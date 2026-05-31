package com.luna.budgetapp.presentation.screen.expensepreset

import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.domain.model.DateFilter

sealed interface UiState {
    data object Loading : UiState
    data class Error(val message: String? = null) : UiState
    data class Success(
        val dialogState: DialogState? = null,
        val dateState: DateState = DateState(),
        val categoryProfileState: CategoryProfileState = CategoryProfileState(),
        val expensesState: ExpensesState = ExpensesState()
    ) : UiState
}

data class ExpensesState(
    val expensePresets: List<ExpensePreset> = emptyList(),
    val totalAmount: Double = 0.0
)

data class DateState(
    val dateFilter: DateFilter = DateFilter.Daily
) {
    val dateRange = dateFilter.resolve()
}

data class CategoryProfileState(
    val selectedCategoryMap: Map<Category, Boolean> = emptyMap()
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
    data object Logout : Event
    data object DeleteLatestExpense : Event
    data class ShowExpenseForm(val selectedPreset: ExpensePreset? = null) : Event
    data class ShowConfirmationDialog(val expensePresetId: Long) : Event
    data class ConfirmExpenseFormDialog(val category: Category, val type: String, val amount: String) : Event
    data class AddExpense(val expensePreset: ExpensePreset, val customAmount: String? = null, val customType: String? = null) : Event
    data class AddCustomExpense(val selectedPreset: ExpensePreset) : Event
    data class DeleteExpensePreset(val expensePresetId: Long) : Event
    data class EditExpensePreset(
        val id: Long,
        val category: Category,
        val type: String,
        val amount: String
    ) : Event
}

sealed interface Navigation {
    data object GotoExpenseRoute : Navigation
    data object Logout : Navigation
}
