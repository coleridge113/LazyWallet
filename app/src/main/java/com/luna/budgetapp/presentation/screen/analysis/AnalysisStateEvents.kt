package com.luna.budgetapp.presentation.screen.analysis

import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.Granularity
import com.luna.budgetapp.domain.model.PeriodChange
import com.luna.budgetapp.domain.model.PeriodTotal
import java.time.LocalDate

sealed interface UiState {
    data object Loading : UiState
    data class Error(val message: String? = null) : UiState
    data class Success(
        val historyState: HistoryState = HistoryState(),
        val expensesState: ExpensesState = ExpensesState(),
        val categoryProfileState: CategoryProfileState = CategoryProfileState()
    ) : UiState
}

data class ExpensesState(
    val filteredExpenses: List<Expense> = emptyList()
)

/** [periods] are oldest first; [selected] is the highlighted period and [change] is versus the one before it. */
data class HistoryState(
    val granularity: Granularity = Granularity.DAY,
    val periods: List<PeriodTotal> = emptyList(),
    val selected: PeriodTotal? = null,
    val change: PeriodChange? = null,
    val today: LocalDate? = null,
)

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
    data object Retry : Event
    data class SelectGranularity(val granularity: Granularity) : Event
    data class SelectPeriod(val start: LocalDate) : Event
    data class SelectCategoryProfile(val profileName: String) : Event
}
