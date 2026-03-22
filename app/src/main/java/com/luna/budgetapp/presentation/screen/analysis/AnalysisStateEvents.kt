package com.luna.budgetapp.presentation.screen.analysis

import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.model.Category
import java.time.LocalDate

data class UiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val expenses: List<Expense> = emptyList(),
    val selectedRange: DateFilter = DateFilter.Last7Days,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedCategoryMap: Map<Category, Boolean> = emptyMap(),
    val activeProfile: String = ""
) {
    val filteredExpenses = expenses.filter { expense ->
        expense.date.toLocalDate() == selectedDate
    }
}

sealed interface Event {
    data class SelectBar(val date: LocalDate) : Event
}

sealed interface Navigation {}


