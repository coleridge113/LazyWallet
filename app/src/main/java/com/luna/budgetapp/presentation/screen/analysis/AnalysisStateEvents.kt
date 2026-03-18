package com.luna.budgetapp.presentation.screen.analysis

import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.model.Category

data class UiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val expenses: List<Expense> = emptyList(),
    val selectedRange: DateFilter = DateFilter.Weekly,
    val selectedCategoryMap: Map<Category, Boolean> = emptyMap(),
    val activeProfile: String = ""
)

sealed interface Event {

}

sealed interface Navigation {}


