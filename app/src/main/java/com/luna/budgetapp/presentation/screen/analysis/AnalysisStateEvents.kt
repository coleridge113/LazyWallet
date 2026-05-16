package com.luna.budgetapp.presentation.screen.analysis

import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.Expense
import java.time.LocalDate

sealed interface UiState {
    data object Loading : UiState
    data class Error(val message: String? = null) : UiState
    data class Success(
        val expensesState: ExpensesState = ExpensesState(),
        val dateState: DateState = DateState(),
        val categoryProfileState: CategoryProfileState = CategoryProfileState()
    ) : UiState
}

data class ExpensesState(
    val expenses: List<Expense> = emptyList(),
    val filteredExpenses: List<Expense> = emptyList()
)

data class DateState(
    val dateFilter: DateFilter = DateFilter.Last7Days,
    val selectedDate: LocalDate = LocalDate.now(),
) {
    val dateRange = dateFilter.resolve()
}

data class CategoryProfileState(
    val selectedCategoryMap: Map<Category, Boolean> = emptyMap(),
    val activeProfile: String = "",
    val profileList: List<String> = emptyList()
) {
    val activeCategories = selectedCategoryMap
        .filterValues { it }
        .keys
        .map { it.name }
}

sealed interface Event {
    data class SelectBar(val date: LocalDate) : Event
    data class SelectCategoryProfile(val profileName: String) : Event
}

sealed interface Navigation {}