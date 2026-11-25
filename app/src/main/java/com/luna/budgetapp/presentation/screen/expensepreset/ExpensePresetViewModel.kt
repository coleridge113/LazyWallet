package com.luna.budgetapp.presentation.screen.expensepreset

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.domain.usecase.AuthUseCases
import com.luna.budgetapp.domain.usecase.BudgetUseCases
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.usecase.PresetUseCases
import com.luna.budgetapp.domain.usecase.ProfileUseCases
import com.luna.budgetapp.domain.utils.parseAmountExpression
import com.luna.budgetapp.domain.utils.todayFlow
import com.luna.budgetapp.presentation.screen.expensepreset.components.ExpenseFormAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
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

    private val _errorState = MutableStateFlow<String?>(null)
    private val _dialogState = MutableStateFlow<DialogState?>(null)
    private val _dateState = MutableStateFlow(DateState())
    private val _categoryProfileState =
        profileUseCases.observeCategoryProfileState(includeProfileList = false)
            .map { snapshot ->
                CategoryProfileState(selectedCategoryMap = snapshot.categoryMap)
            }

    private val _expensesState = combine(
        _dateState,
        presetUseCases.getAllExpensePresets(),
        _categoryProfileState,
        todayFlow(clock)
    ) { dateState, expensePresets, categoryProfileState, today ->
        listOf(dateState, expensePresets, categoryProfileState, today)
    }
        .flatMapLatest { values ->
            val dateState = values[0] as DateState
            @Suppress("UNCHECKED_CAST")
            val expensePresets = values[1] as List<ExpensePreset>
            val categoryProfileState = values[2] as CategoryProfileState
            val today = values[3] as LocalDate
            val (start, end) = dateState.dateFilter.resolve(today)
            expenseUseCases.getExpensesByDateRange(
                start = start,
                end = end,
                categories = categoryProfileState.selectedCategoryMap
                    .filter { it.value }
                    .keys
                    .map { it.name }
                    .toList()
            ).map { expenses ->
                ExpensesState(
                    expensePresets = expensePresets,
                    expenses = expenses
                )
            }
        }

    private val _successState = combine(
        _dateState,
        _dialogState,
        _categoryProfileState,
        _expensesState
    ) { dateState, dialogState, categoryProfileState, expensesState ->
        UiState.Success(
            dialogState = dialogState,
            dateState = dateState,
            categoryProfileState = categoryProfileState,
            expensesState = expensesState
        )
    }

    val uiState: StateFlow<UiState> = combine(
        _errorState,
        _successState
    ) { error, success ->
        if (error != null) {
            UiState.Error(error)
        } else {
            success
        }
    }
        .onStart { profileUseCases.initializeCategoryProfile() }
        .catch { e ->
            if (e is CancellationException) throw e
            emit(UiState.Error(e.message ?: "Something went wrong"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

    private val _navigation = Channel<Navigation>()
    val navigation = _navigation.receiveAsFlow()

    fun onEvent(event: Event) {
        when (event) {
            Event.ShowSignOutDialog -> showSignOutDialog()
            Event.GotoExpenseRoute -> gotoExpenseRoute(Navigation.GotoExpenseRoute)
            Event.GotoAnalysisRoute -> gotoExpenseRoute(Navigation.GotoAnalysisRoute)
            Event.DismissDialog -> dismissDialog()
            Event.ShowDeleteConfirmationDialog -> showExpenseDeleteConfirmationDialog()
            Event.DeleteLatestExpense -> deleteLatestExpense()
            is Event.SignOut -> signOutUser(event.auth)
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

    private fun deleteLatestExpense() {
        dismissDialog()
        viewModelScope.launch {
            expenseUseCases.deleteLatestExpense()
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

    private fun showExpenseDeleteConfirmationDialog() {
        _dialogState.update {
            DialogState.ConfirmDeleteExpense
        }
    }

    private fun gotoExpenseRoute(navigation: Navigation) {
        viewModelScope.launch {
            _navigation.send(navigation)
        }
    }

    private fun showSignOutDialog() {
        _dialogState.update {
            DialogState.ConfirmLogout
        }
    }

    private fun signOutUser(auth: FirebaseAuth) {
        viewModelScope.launch {
            authUseCases.signOut(auth)
            _navigation.send(Navigation.Logout)
        }
    }
}
