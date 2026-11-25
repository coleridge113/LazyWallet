package com.luna.budgetapp.domain.repository

import com.luna.budgetapp.domain.model.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeBudgetRepository : BudgetRepository {

    private val budgetsFlow = MutableStateFlow<List<Budget>>(emptyList())
    private var nextId = 1L

    override fun getBudgets(): Flow<List<Budget>> = budgetsFlow

    override fun getBudgetByName(name: String): Flow<Budget> =
        budgetsFlow.map { list -> list.first { it.name == name } }

    override fun getBudgetById(budgetId: Long): Flow<Budget> =
        budgetsFlow.map { list -> list.first { it.id == budgetId } }

    override suspend fun insertBudget(budget: Budget) {
        budgetsFlow.update { it + budget.copy(id = nextId++) }
    }

    override suspend fun updateBudget(budget: Budget) {
        budgetsFlow.update { list -> list.map { if (it.id == budget.id) budget else it } }
    }

    override suspend fun deleteBudget(budget: Budget) {
        budgetsFlow.update { list -> list.filter { it.id != budget.id } }
    }

    override suspend fun updateBudgetWithHistory(currentBudget: Budget, newLimit: Long) {
        updateBudget(currentBudget.copy(limit = newLimit))
    }
}
