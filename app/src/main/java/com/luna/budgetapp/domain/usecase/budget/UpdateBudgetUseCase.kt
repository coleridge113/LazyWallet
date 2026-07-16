package com.luna.budgetapp.domain.usecase.budget

import com.luna.budgetapp.domain.model.Budget
import com.luna.budgetapp.domain.repository.BudgetRepository

class UpdateBudgetUseCase(
    val repository: BudgetRepository
) {
    suspend operator fun invoke(
        budget: Budget
    ) {
        repository.updateBudget(budget)
    }
}
