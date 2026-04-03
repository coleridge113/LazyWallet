package com.luna.budgetapp.presentation.screen.expensepreset

import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.domain.model.DateFilter

data class UiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val dialogState: DialogState? = null,
    val dateFilter: DateFilter = DateFilter.Daily,
    val selectedCategories: Map<Category, Boolean> = emptyMap()
)

sealed interface DialogState {
    data object ConfirmDeleteExpense : DialogState
    data class ConfirmDeleteExpensePreset(val expensePresetId: Long) : DialogState
    data class ExpenseForm(
        val selectedPreset: ExpensePreset? = null,
        val isSaving: Boolean = false
    ) : DialogState
}

sealed interface Event {
    data object GotoExpenseRoute : Event
    data object DismissDialog : Event
    data object ShowDeleteConfirmationDialog : Event
    data object DeleteLatestExpense : Event
    data class ShowExpenseForm(val selectedPreset: ExpensePreset? = null) : Event
    data class ShowConfirmationDialog(val expensePresetId: Long) : Event
    data class ConfirmExpenseFormDialog(val category: Category, val type: String, val amount: String) : Event
    data class AddExpense(val expensePreset: ExpensePreset, val customAmount: String? = null, val customType: String? = null) : Event
    data class AddCustomExpense(val selectedPreset: ExpensePreset) : Event
    data class DeleteExpensePreset(val expensePresetId: Long) : Event
}

sealed interface Navigation {
    data object GotoExpenseRoute : Navigation
}
