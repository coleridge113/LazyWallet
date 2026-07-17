package com.luna.budgetapp.presentation.screen.budget

import com.luna.budgetapp.domain.model.Budget
import com.luna.budgetapp.domain.model.Expense

sealed interface UiState {
    data object Loading : UiState
    data class Error(val message: String? = null) : UiState
    data class Success(
        val budgets: List<Budget>
    ) : UiState
}

data class ExpenseState(
    val expenses: List<Expense>
) {
    val spent: Double = expenses.sumOf { it.amount }
}

sealed interface Event {}

sealed interface DialogState {}
