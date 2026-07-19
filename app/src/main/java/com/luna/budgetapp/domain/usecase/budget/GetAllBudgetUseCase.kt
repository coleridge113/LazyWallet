package com.luna.budgetapp.domain.usecase.budget

import com.luna.budgetapp.domain.model.Budget
import com.luna.budgetapp.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow

class GetAllBudgetUseCase(
    private val repository: BudgetRepository
) {
    operator fun invoke(): Flow<List<Budget>> {
        return repository.getBudgets()
    }
}
