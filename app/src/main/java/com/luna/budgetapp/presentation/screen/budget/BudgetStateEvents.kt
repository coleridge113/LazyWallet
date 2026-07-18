package com.luna.budgetapp.presentation.screen.budget

import com.luna.budgetapp.domain.model.Budget
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.model.Expense

typealias BudgetId = Long

sealed interface UiState {
    data object Loading : UiState
    data class Error(val message: String? = null) : UiState
    data class Success(
        val budgets: List<Budget>,
        val expenses: Map<BudgetId, List<Expense>>,
        val dialog: DialogState? = null
    ) : UiState
}

sealed interface Event {
    data object DismissDialog : Event
    data class ShowBudgetDialog(val budget: Budget? = null) : Event
    data class ConfirmBudgetFormDialog(
        val id: Long,
        val name: String,
        val amount: String,
        val frequency: DateFilter,
        val categoryMap: Map<Category, Boolean>
    ) : Event
}

sealed interface DialogState {
    data class BudgetDialog(val budget: Budget?) : DialogState
}
