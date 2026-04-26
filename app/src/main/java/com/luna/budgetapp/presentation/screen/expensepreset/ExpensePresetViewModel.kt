package com.luna.budgetapp.presentation.screen.expensepreset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.usecase.PresetUseCases
import com.luna.budgetapp.domain.usecase.ProfileUseCases
import com.luna.budgetapp.presentation.screen.utils.filterDataByState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    private val profileUseCases: ProfileUseCases
): ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _navigation = Channel<Navigation>()
    val navigation = _navigation.receiveAsFlow()

    init {
        initializeCategoryFilterIfNeeded()
        observeActiveProfileAndCategories()
    }

    fun onEvent(event: Event) {
        when (event) {
            Event.Logout -> logoutUser()
            Event.GotoExpenseRoute -> gotoExpenseRoute()
            Event.DismissDialog -> dismissDialog()
            Event.ShowDeleteConfirmationDialog -> showExpenseDeleteConfirmationDialog()
            Event.DeleteLatestExpense -> deleteLatestExpense()
            is Event.AddExpense -> addExpense(event.expensePreset, event.customAmount, event.customType)
            is Event.ShowExpenseForm -> showExpenseForm(event.selectedPreset)
            is Event.ShowConfirmationDialog -> showPresetDeleteConfirmationDialog(event.expensePresetId)
            is Event.AddCustomExpense -> showExpenseForm(event.selectedPreset)
            is Event.DeleteExpensePreset -> deleteExpensePreset(event.expensePresetId)
            is Event.ConfirmExpenseFormDialog -> saveExpensePreset(event.category, event.type, event.amount)
        }
    }
    
    val expensePresets: StateFlow<List<ExpensePreset>> =
        presetUseCases.getAllExpensePresets()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val totalAmount: StateFlow<Double> =
        _uiState.filterDataByState(
            dateFilterSelector = UiState::dateFilter,
            categorySelector = UiState::selectedCategories
        ) { categories, start, end ->
            expenseUseCases.getTotalAmountByDateRange(
                categories = categories,
                start = start,
                end = end
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0.0
            )


    private fun showExpenseForm(selectedPreset: ExpensePreset?) {
        updateDialogState(
            dialogState = DialogState.ExpenseForm(
                selectedPreset = selectedPreset,
                isSaving = false
            )
        )
    }

    private fun dismissDialog() {
        updateDialogState(null)
    }

    private fun saveExpensePreset(category: Category, type: String, amount: String) {
        val dialog = _uiState.value.dialogState

        if (dialog !is DialogState.ExpenseForm || dialog.isSaving) return 

        val expensePreset = ExpensePreset(
            amount = amount.toDoubleOrNull() ?: 0.0,
            category = category.name,
            type = type.ifEmpty { category.displayName }.trim()
        )

        _uiState.update { currentState ->
            currentState.copy(
                 dialogState = dialog.copy(isSaving = true)
            )
        }

        viewModelScope.launch {
            try {
                presetUseCases.addExpensePreset(expensePreset)
                _uiState.update {
                    it.copy(
                        dialogState = null
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        dialogState = dialog.copy(isSaving = false),
                        error = "Error saving preset..."
                    )
                }
            }
        }
    }

    private fun addExpense(
        expensePreset: ExpensePreset, 
        customAmount: String?,
        customType: String?
    ) {
        val state = _uiState.value

        viewModelScope.launch {
            expenseUseCases.addExpense(
                category = expensePreset.category,
                type = customType ?: expensePreset.type,
                amount = customAmount ?: expensePreset.amount.toString()
            )

            if (state.dialogState != null) {
                updateDialogState(null)
            }
        }
    }

    private fun deleteLatestExpense() {
        updateDialogState(null)
        viewModelScope.launch {
            expenseUseCases.deleteLatestExpense()
        }
    }

    private fun deleteExpensePreset(expensePresetId: Long) {
        viewModelScope.launch {
            try {
                presetUseCases.deleteExpensePreset(expensePresetId)
                updateDialogState(null)
            } catch (_: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        error = "Failed to delete expense preset..."
                    )
                }
            }
        }
    }

    private fun showPresetDeleteConfirmationDialog(expensePresetId: Long) {
        _uiState.update { currentState ->
            currentState.copy(
                dialogState = DialogState.ConfirmDeleteExpensePreset(expensePresetId)
            )
        }
    }

    private fun showExpenseDeleteConfirmationDialog() {
        updateDialogState(DialogState.ConfirmDeleteExpense)
    }

    private fun gotoExpenseRoute() {
        viewModelScope.launch {
            _navigation.send(Navigation.GotoExpenseRoute)
        }
    }

    private fun initializeCategoryFilterIfNeeded() {
        viewModelScope.launch {
            profileUseCases.initializeCategoryProfile()
        }
    }

    private fun observeActiveProfileAndCategories() {
        viewModelScope.launch {
            profileUseCases.getActiveCategoryProfile()
                .flatMapLatest { profile ->
                    profileUseCases.getCategoryProfile(profile)
                        .map { filters ->
                            profile to filters
                        }
                }
                .collectLatest { (profile, filters) ->

                    val categoryMap = filters.associate {
                        it.category to it.isActive
                    }

                    _uiState.update { currentState ->
                        currentState.copy(
                            selectedCategories =
                                categoryMap.ifEmpty { currentState.selectedCategories }
                        )
                    }
                }
        }
    }

    private fun updateDialogState(dialogState: DialogState?) {
        _uiState.update { currentState ->
            currentState.copy(dialogState = dialogState)
        }
    }

    private fun logoutUser() {
        viewModelScope.launch {
            _navigation.send(Navigation.Logout)
        }
    }
}
