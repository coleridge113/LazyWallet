package com.luna.budgetapp.presentation.screen.expenselist

import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.presentation.model.ChartData
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.Expense

sealed interface UiState {
    data object Loading : UiState
    data class Error(val message: String? = null) : UiState
    data class Success(
        val dialogState: DialogState? = null,
        val dateState: DateState = DateState(),
        val categoryProfileState: CategoryProfileState = CategoryProfileState(),
        val chartDataState: ChartDataState = ChartDataState(),
        val expensesState: ExpensesState = ExpensesState()
    ) : UiState
}

data class ExpensesState(
    val totalAmount: Long = 0L
)

data class DateState(
    val dateFilter: DateFilter = DateFilter.Daily
) {
    val dateRange = dateFilter.resolve()
}

data class CategoryProfileState(
    val selectedCategoryMap: Map<Category, Boolean> = emptyMap(),
    val activeProfile: String = "",
    val profileList: List<String> = emptyList()
) {
    val activeCategories: List<String> = selectedCategoryMap
        .filterValues { it }
        .keys
        .map { it.name }
}

data class ChartDataState(
    val chartDataList: List<ChartData> = emptyList()
)

sealed interface DialogState {
    data object CalendarForm : DialogState
    data class CategoryFilterForm(val filteredCategories: Map<Category, Boolean>) : DialogState
    data class ConfirmDeleteExpense(val expenseId: Long) : DialogState
    data class ExpenseForm(
        val selectedExpense: Expense,
        val isSaving: Boolean = false
    ) : DialogState
}

sealed interface Event {
    data object ShowCalendarForm : Event
    data object ShowCategoryFilterDialog : Event
    data object DismissDialog : Event
    data object ResetCategoryFilters : Event
    data object GotoBarGraph : Event
    data class ShowDeleteConfirmationDialog(val expenseId: Long) : Event
    data class DeleteExpense(val expenseId: Long) : Event
    data class SelectDateRange(val selectedRange: DateFilter) : Event
    data class SelectCategoryProfile(val profileName: String) : Event
    data class DeleteCategoryProfile(val profileName: String) : Event
    data class ShowExpenseForm(val selectedExpense: Expense) : Event
    data class EditExpense(
        val expenseId: Long,
        val type: String,
        val amount: String
    ) : Event
    data class SaveCategoryProfile(
        val profileName: String,
        val selectedCategoryMap: Map<Category, Boolean>
    ) : Event
    data class ApplyCategoryFilters(
        val profileName: String,
        val selectedCategoryMap: Map<Category, Boolean>
    ) : Event
}

sealed interface Navigation {
    data object GotoAnalysisRoute : Navigation
}
