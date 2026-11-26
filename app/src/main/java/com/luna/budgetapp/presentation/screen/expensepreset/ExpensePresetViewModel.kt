package com.luna.budgetapp.presentation.screen.expensepreset

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.domain.model.Granularity
import com.luna.budgetapp.domain.usecase.AuthUseCases
import com.luna.budgetapp.domain.usecase.BudgetUseCases
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.usecase.PresetUseCases
import com.luna.budgetapp.domain.usecase.ProfileUseCases
import com.luna.budgetapp.domain.utils.parseAmountExpression
import com.luna.budgetapp.domain.utils.todayFlow
import com.luna.budgetapp.presentation.screen.expensepreset.components.ExpenseFormAction
import com.luna.budgetapp.presentation.screen.expensepreset.model.lastLogged
import com.luna.budgetapp.presentation.screen.expensepreset.model.lastSevenDays
import com.luna.budgetapp.presentation.screen.expensepreset.model.recentFirst
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

private const val TAG = "ExpensePresetVM"

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensePresetViewModel(
    private val presetUseCases: PresetUseCases,
    private val expenseUseCases: ExpenseUseCases,
    private val profileUseCases: ProfileUseCases,
    private val authUseCases: AuthUseCases,
    private val clock: Clock
): ViewModel() {

    /** Bumped by [Event.Retry]; each bump restarts the load pipeline. */
    private val _retryTrigger = MutableStateFlow(0)
    private val _dialogState = MutableStateFlow<DialogState?>(null)
    private val _dateState = MutableStateFlow(DateState())
    private val _userMessage = MutableStateFlow<UserMessage?>(null)
    private var lastRemoved: Expense? = null
    private val allCategoryNames = Category.entries.map { it.name }

    private val _expensesState = combine(
        _dateState,
        presetUseCases.getAllExpensePresets(),
        expenseUseCases.observeSpendingHistory(allCategoryNames, Granularity.DAY)
            .map { lastSevenDays(it.periods) },
        todayFlow(clock)
    ) { dateState, expensePresets, sparkline, today ->
        listOf(dateState, expensePresets, sparkline, today)
    }
        .flatMapLatest { values ->
            val dateState = values[0] as DateState
            @Suppress("UNCHECKED_CAST")
            val expensePresets = values[1] as List<ExpensePreset>
            @Suppress("UNCHECKED_CAST")
            val sparkline = values[2] as List<Long>
            val today = values[3] as LocalDate
            val (start, end) = dateState.dateFilter.resolve(today)
            // The ExpensePreset screen ignores the category filter: null = every category.
            expenseUseCases.getExpensesByDateRange(
                start = start,
                end = end,
                categories = null
            ).map { expenses ->
                ExpensesState(
                    expensePresets = expensePresets,
                    expenses = expenses,
                    sparkline = sparkline,
                    recent = recentFirst(expenses),
                    lastLogged = lastLogged(expenses)
                )
            }
        }

    private val _successState = combine(
        _dateState,
        combine(_dialogState, _userMessage) { dialog, message -> dialog to message },
        _expensesState
    ) { dateState, (dialogState, userMessage), expensesState ->
        UiState.Success(
            dialogState = dialogState,
            dateState = dateState,
            expensesState = expensesState,
            userMessage = userMessage
        )
    }

    val uiState: StateFlow<UiState> = _retryTrigger
        .flatMapLatest {
            _successState
                .map<UiState.Success, UiState> { it }
                .onStart { profileUseCases.initializeCategoryProfile() }
                .catch { e ->
                    if (e is CancellationException) throw e
                    emit(UiState.Error(e.message))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

    private val _signedOut = MutableStateFlow(false)
    val signedOut: StateFlow<Boolean> = _signedOut.asStateFlow()

    fun onSignedOutHandled() {
        _signedOut.value = false
    }

    fun onEvent(event: Event) {
        when (event) {
            Event.ShowSignOutDialog -> showSignOutDialog()
            Event.DismissDialog -> dismissDialog()
            Event.Retry -> _retryTrigger.update { it + 1 }
            Event.UndoRemove -> undoRemove()
            Event.UserMessageShown -> _userMessage.value = null
            Event.ClearUndo -> {
                lastRemoved = null
                _userMessage.value = null
            }
            is Event.RemoveExpense -> removeExpense(event.expense)
            Event.SignOut -> signOutUser()
            is Event.AddExpense -> addExpense(event.expensePreset, event.customAmount, event.customType, event.customDate)
            is Event.AddExpensePreset -> showExpenseForm(event.selectedPreset, event.action)
            is Event.AddCustomExpense -> showExpenseForm(event.selectedPreset, event.action)
            is Event.EditExpensePreset -> showExpenseForm(event.selectedPreset, event.action)
            is Event.ShowConfirmationDialog -> showPresetDeleteConfirmationDialog(event.expensePresetId)
            is Event.DeleteExpensePreset -> deleteExpensePreset(event.expensePresetId)
            is Event.ConfirmExpenseFormDialog -> saveExpensePreset(
                event.id, event.category, event.type, event.amount, event.date
            )
        }
    }

    private fun showExpenseForm(
        selectedPreset: ExpensePreset?,
        action: ExpenseFormAction
    ) {
        _dialogState.update {
            DialogState.ExpenseForm(
                selectedPreset = selectedPreset,
                isSaving = false,
                action = action
            )
        }
    }

    private fun dismissDialog() {
        _dialogState.update { null }
    }

    private fun saveExpensePreset(id: Long?, category: Category, type: String, amount: String, date: LocalDateTime?) {
        val dialog = _dialogState.value

        if (dialog !is DialogState.ExpenseForm || dialog.isSaving) return

        val expensePreset = ExpensePreset(
            id = id,
            amount = parseAmountExpression(amount),
            category = category,
            type = type.ifEmpty { category.getDisplayName() }.trim(),
            createdAt = date ?: LocalDateTime.now(clock)
        )

        viewModelScope.launch {
            try {
                if (id == null) {
                    presetUseCases.addExpensePreset(expensePreset)
                } else {
                    presetUseCases.updateExpensePreset(expensePreset)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Failed to save expense preset", e)
            } finally {
                dismissDialog()
            }
        }
    }

    private fun addExpense(
        expensePreset: ExpensePreset, 
        customAmount: String?,
        customType: String?,
        customDate: LocalDateTime?
    ) {
        val amount = customAmount?.let { parseAmountExpression(it) } ?: expensePreset.amount
        viewModelScope.launch {
            expenseUseCases.addExpense(
                category = expensePreset.category.name,
                type = customType ?: expensePreset.type,
                amount = amount,
                date = customDate ?: LocalDateTime.now(clock)
            )

            if (_dialogState.value != null) {
                dismissDialog()
            }
        }
    }

    private fun removeExpense(expense: Expense) {
        val id = expense.id ?: return
        lastRemoved = expense
        viewModelScope.launch {
            expenseUseCases.deleteExpense(id)
            _userMessage.value = UserMessage.Removed(id, expense.type, expense.amount)
        }
    }

    private fun undoRemove() {
        val expense = lastRemoved ?: return
        lastRemoved = null
        _userMessage.value = null
        viewModelScope.launch {
            expenseUseCases.addExpense(
                category = expense.category.name,
                type = expense.type,
                amount = expense.amount,
                date = expense.date
            )
        }
    }

    private fun deleteExpensePreset(expensePresetId: Long) {
        viewModelScope.launch {
            try {
                presetUseCases.deleteExpensePreset(expensePresetId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Failed to delete expense preset", e)
            } finally {
                dismissDialog()
            }
        }
    }

    private fun showPresetDeleteConfirmationDialog(expensePresetId: Long) {
        _dialogState.update {
            DialogState.ConfirmDeleteExpensePreset(expensePresetId)
        }
    }

    private fun showSignOutDialog() {
        _dialogState.update {
            DialogState.ConfirmLogout
        }
    }

    private fun signOutUser() {
        viewModelScope.launch {
            try {
                authUseCases.signOut()
                _signedOut.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Sign out failed", e)
            }
        }
    }
}
