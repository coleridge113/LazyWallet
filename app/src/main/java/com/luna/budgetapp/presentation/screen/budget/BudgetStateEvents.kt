package com.luna.budgetapp.presentation.screen.budget

import com.luna.budgetapp.domain.model.Budget
import com.luna.budgetapp.domain.model.Expense

typealias BudgetId = Long

sealed interface UiState {
    data object Loading : UiState
    data class Error(val message: String? = null) : UiState
    data class Success(
        val budgets: List<Budget>,
        val expenses: Map<BudgetId, List<Expense>>
    ) : UiState
}

sealed interface Event {}

sealed interface DialogState {}
