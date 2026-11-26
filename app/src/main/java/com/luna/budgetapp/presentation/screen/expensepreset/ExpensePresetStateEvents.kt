package com.luna.budgetapp.presentation.screen.expensepreset

import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.presentation.screen.expensepreset.components.ExpenseFormAction

sealed interface UiState {
    data object Loading : UiState
    data class Error(val message: String? = null) : UiState
    data class Success(
        val dialogState: DialogState? = null,
        val dateState: DateState = DateState(),
        val expensesState: ExpensesState = ExpensesState(),
        val userMessage: UserMessage? = null
    ) : UiState
}

data class ExpensesState(
    val expensePresets: List<ExpensePreset> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val sparkline: List<Long> = List(7) { 0L },
    val recent: List<Expense> = emptyList(),
    val lastLogged: Expense? = null
) {
    val totalAmount = expenses.sumOf { it.amount }
}

sealed interface UserMessage {
    /** The id makes two removals of identical expenses distinct, so the snackbar re-fires. */
    data class Removed(val expenseId: Long, val type: String, val amount: Long) : UserMessage
}

data class DateState(
    val dateFilter: DateFilter = DateFilter.Daily
)

sealed interface DialogState {
    data object ConfirmLogout : DialogState
    data class ConfirmDeleteExpensePreset(val expensePresetId: Long) : DialogState
    data class ExpenseForm(
        val selectedPreset: ExpensePreset? = null,
        val isSaving: Boolean = false,
        val action: ExpenseFormAction = ExpenseFormAction.ADD
    ) : DialogState
}

sealed interface Event {
    data object Retry : Event
    data object DismissDialog : Event
    data object ShowSignOutDialog : Event
    data object UndoRemove : Event
    data object UserMessageShown : Event
    data object ClearUndo : Event
    data class RemoveExpense(val expense: Expense) : Event
    data object SignOut : Event
    data class ShowConfirmationDialog(val expensePresetId: Long) : Event
    data class ConfirmExpenseFormDialog(
        val id: Long?,
        val category: Category,
        val type: String,
        val amount: String,
        val date: java.time.LocalDateTime? = null
    ) : Event
    data class ReorderPresets(val orderedIds: List<Long>) : Event
    data class DeleteExpensePreset(val expensePresetId: Long) : Event
    data class AddExpense(val expensePreset: ExpensePreset, val customAmount: String? = null, val customType: String? = null, val customDate: java.time.LocalDateTime? = null) : Event
    data class AddCustomExpense(
        val selectedPreset: ExpensePreset,
        val action: ExpenseFormAction
    ) : Event
    data class AddExpensePreset(
        val selectedPreset: ExpensePreset? = null,
        val action: ExpenseFormAction
    ) : Event
    data class EditExpensePreset(
        val selectedPreset: ExpensePreset,
        val action: ExpenseFormAction
    ) : Event
}

