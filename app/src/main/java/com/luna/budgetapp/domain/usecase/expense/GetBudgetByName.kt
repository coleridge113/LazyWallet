package com.luna.budgetapp.domain.usecase.expense

import com.luna.budgetapp.domain.model.Budget
import com.luna.budgetapp.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow

class GetBudgetByName(
    private val repository: BudgetRepository
) {
    operator fun invoke(name: String): Flow<Budget> {
        return repository.getBudgetByName(name)
    }
}
