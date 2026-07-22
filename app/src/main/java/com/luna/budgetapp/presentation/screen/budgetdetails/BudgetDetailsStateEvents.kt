package com.luna.budgetapp.presentation.screen.budgetdetails

import com.luna.budgetapp.domain.model.Budget
import com.luna.budgetapp.domain.model.Expense

sealed interface UiState {
    data object Loading : UiState
    data class Error(val message: String? = null) : UiState
    data class Success(
        val budget: Budget,
        val expenses: List<Expense>
    ) : UiState
}

sealed interface Event {}