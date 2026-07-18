package com.luna.budgetapp.presentation.screen.expensepreset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.domain.usecase.BudgetUseCases
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.usecase.PresetUseCases
import com.luna.budgetapp.domain.usecase.ProfileUseCases
import com.luna.budgetapp.domain.utils.parseAmountExpression
import com.luna.budgetapp.presentation.screen.expensepreset.components.ExpenseFormAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensePresetViewModel(
    private val presetUseCases: PresetUseCases,
    private val expenseUseCases: ExpenseUseCases,
    private val profileUseCases: ProfileUseCases,
    private val budgetUseCases: BudgetUseCases
): ViewModel() {

    private val _errorState = MutableStateFlow<String?>(null)
    private val _dialogState = MutableStateFlow<DialogState?>(null)
    private val _dateState = MutableStateFlow(DateState())
    private val _categoryProfileState =
        profileUseCases.getActiveCategoryProfile()
            .flatMapLatest { activeProfile ->
                profileUseCases.getCategoryProfile(activeProfile)
                    .map { filters ->
                        val categoryMap = filters.associate { it.category to it.isActive }

                        CategoryProfileState(
                            selectedCategoryMap = categoryMap
                        )
                    }
            }

    private val _expensesState = combine(
        _dateState,
        presetUseCases.getAllExpensePresets(),
        _categoryProfileState
    ) { dateState, expensePresets, categoryProfileState ->
        Triple(dateState, expensePresets, categoryProfileState)
    }
        .flatMapLatest { (dateState, expensePresets, categoryProfileState) ->
            val (start, end) = dateState.dateRange
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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

    private val _navigation = Channel<Navigation>()
    val navigation = _navigation.receiveAsFlow()

    init {
        initializeCategoryFilterIfNeeded()
    }

    fun onEvent(event: Event) {
        when (event) {
            Event.SignOut -> signOutUser()
            Event.ShowSignOutDialog -> showSignOutDialog()
            Event.GotoExpenseRoute -> gotoExpenseRoute(Navigation.GotoExpenseRoute)
            Event.GotoAnalysisRoute -> gotoExpenseRoute(Navigation.GotoAnalysisRoute)
            Event.DismissDialog -> dismissDialog()
            Event.ShowDeleteConfirmationDialog -> showExpenseDeleteConfirmationDialog()
            Event.DeleteLatestExpense -> deleteLatestExpense()
            is Event.AddExpense -> addExpense(event.expensePreset, event.customAmount, event.customType)
            is Event.AddExpensePreset -> showExpenseForm(event.selectedPreset, event.action)
            is Event.AddCustomExpense -> showExpenseForm(event.selectedPreset, event.action)
            is Event.EditExpensePreset -> showExpenseForm(event.selectedPreset, event.action)
            is Event.ShowConfirmationDialog -> showPresetDeleteConfirmationDialog(event.expensePresetId)
            is Event.DeleteExpensePreset -> deleteExpensePreset(event.expensePresetId)
            is Event.ConfirmExpenseFormDialog -> saveExpensePreset(
                event.id, event.category, event.type, event.amount
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

    private fun saveExpensePreset(id: Long?, category: Category, type: String, amount: String) {
        val dialog = _dialogState.value

        if (dialog !is DialogState.ExpenseForm || dialog.isSaving) return

        val expensePreset = ExpensePreset(
            id = id,
            amount = amount.toDoubleOrNull() ?: 0.0,
            category = category.name,
            type = type.ifEmpty { category.getDisplayName() }.trim()
        )

        viewModelScope.launch {
            try {
                if (id == null) {
                    presetUseCases.addExpensePreset(expensePreset)
                } else {
                    presetUseCases.updateExpensePreset(expensePreset)
                }
            } catch (_: Exception) {

            } finally {
                dismissDialog()
            }
        }
    }

    private fun addExpense(
        expensePreset: ExpensePreset, 
        customAmount: String?,
        customType: String?
    ) {
        val amount =
            parseAmountExpression(customAmount ?: expensePreset.amount.toString())
        viewModelScope.launch {
            expenseUseCases.addExpense(
                category = expensePreset.category,
                type = customType ?: expensePreset.type,
                amount = amount
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
            } catch (_: Exception) {
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

    private fun initializeCategoryFilterIfNeeded() {
        viewModelScope.launch {
            profileUseCases.initializeCategoryProfile()
        }
    }

    private fun showSignOutDialog() {
        _dialogState.update {
            DialogState.ConfirmLogout
        }
    }

    private fun signOutUser() {
        viewModelScope.launch {
            _navigation.send(Navigation.Logout)
        }
    }
}
