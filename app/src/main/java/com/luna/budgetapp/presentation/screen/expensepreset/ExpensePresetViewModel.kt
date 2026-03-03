package com.luna.budgetapp.presentation.screen.expensepreset

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.usecase.UseCases
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensePresetViewModel(
    private val useCases: UseCases,
): ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _navigation = Channel<Navigation>()
    val navigation = _navigation.receiveAsFlow()

    init {
        initializeCategoryFilterIfNeeded()
        observeTotalAmount()
        observeExpensePresets()
    }

    fun onEvent(event: Event) {
        when (event) {
            Event.GotoExpenseRoute -> gotoExpenseRoute()
            Event.DismissDialog -> dismissDialog()
            Event.ShowDeleteConfirmationDialog -> showExpenseDeleteConfirmationDialog()
            Event.DeleteLatestExpense -> deleteLatestExpense()
            is Event.AddExpense -> addExpense(event.expensePreset, event.customAmount, event.customType)
            is Event.ShowExpenseForm -> showExpenseForm(event.selectedPreset)
            is Event.ShowConfirmationDialog -> showPresetDeleteConfirmationDialog(event.expensePresetId)
            is Event.AddCustomExpense -> showExpenseForm(event.selectedPreset)
            is Event.DeleteExpensePreset -> deleteExpensePreset(event.expensePresetId)
            is Event.ConfirmDialog -> saveExpensePreset(event.category, event.type, event.amount)
        }
    }

    private fun observeTotalAmount() {
        viewModelScope.launch {
            _uiState
                .map { it.selectedRange to it.selectedCategoryMap }
                .distinctUntilChanged()
                .flatMapLatest { (filter, categoryMap) ->
                    val range = filter.resolve()

                    val selectedCategories =
                        categoryMap
                            .filterValues { it }
                            .keys
                            .map { it.name }

                    Log.d("ExpensePreset", "selected categories: $selectedCategories")

                    useCases.getTotalAmountByDateRange(
                        start = range.start,
                        end = range.end,
                        categories = selectedCategories
                    )
                }
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isExpensesLoading = false,
                            error = error.localizedMessage
                        )
                    }
                }
                .collect { totalAmount ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            isExpensesLoading = false,
                            error = null,
                            totalAmount = totalAmount
                        )
                    }
                }
        }
    }

    private fun observeExpensePresets() {
        viewModelScope.launch {
            useCases.getAllExpensePresets()
                .onStart {
                    _uiState.update {
                        it.copy(
                            isPresetsLoading = true,
                        )
                    }
                }
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isPresetsLoading = false,
                            error = error.localizedMessage
                        )
                    }
                }
                .collect { expensePresets ->
                    _uiState.update {
                        it.copy(
                            isPresetsLoading = false,
                            error = null,
                            expensePresets = expensePresets
                        )
                    }
                }
        }
    }

    private fun showExpenseForm(selectedPreset: ExpensePreset?) {
        _uiState.update { currentState ->
            currentState.copy(
                dialogState = DialogState.ExpenseForm(
                    selectedPreset = selectedPreset,
                    isSaving = false
                )
            )
        }
    }

    private fun dismissDialog() {
        _uiState.update { currentState ->
            currentState.copy(
                 dialogState = null
            )
        }
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
                useCases.addExpensePreset(expensePreset)
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
        customAmount: Double?,
        customType: String?
    ) {
        val state = _uiState.value

        viewModelScope.launch {
            val expense = Expense(
                category = expensePreset.category,
                type = customType ?: expensePreset.type,
                amount = customAmount ?: expensePreset.amount
            )
            useCases.addExpense(expense)

            if (state.dialogState != null) {
                _uiState.update { currentState ->
                    currentState.copy(
                        dialogState = null
                    )
                }
            }
        }
    }

    private fun deleteLatestExpense() {
        _uiState.update { currentState ->
            currentState.copy(
                dialogState = null
            )
        }
        viewModelScope.launch {
            useCases.deleteLatestExpense()
        }
    }

    private fun deleteExpensePreset(expensePresetId: Long) {
        viewModelScope.launch {
            try {
                useCases.deleteExpensePreset(expensePresetId)
                _uiState.update { currentState ->
                    currentState.copy(
                        dialogState = null
                    )
                }
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
        _uiState.update { currentState ->
            currentState.copy(
                dialogState = DialogState.ConfirmDeleteExpense
            )
        }
    }

    private fun gotoExpenseRoute() {
        viewModelScope.launch {
            _navigation.send(Navigation.GotoExpenseRoute)
        }
    }

    private fun initializeCategoryFilterIfNeeded() {
        viewModelScope.launch {
            useCases.initializeCategoryProfile()
        }
    }
}
