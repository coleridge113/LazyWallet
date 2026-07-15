package com.luna.budgetapp.domain.usecase.budget

import com.luna.budgetapp.domain.model.Budget
import com.luna.budgetapp.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow

class GetBudgetByIdUseCase(
    val repository: BudgetRepository
) {
    operator fun invoke(
        budgetId: Long
    ): Flow<Budget> {
        return repository.getBudgetById(budgetId)
    }
}
