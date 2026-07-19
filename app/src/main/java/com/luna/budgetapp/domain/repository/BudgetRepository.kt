package com.luna.budgetapp.domain.repository

import com.luna.budgetapp.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {

    fun getBudgets(): Flow<List<Budget>>

    fun getBudgetByName(name: String): Flow<Budget>

    fun getBudgetById(budgetId: Long): Flow<Budget>

    suspend fun insertBudget(budget: Budget)

    suspend fun updateBudget(budget: Budget)

    suspend fun deleteBudget(budget: Budget)

    suspend fun updateBudgetWithHistory(
        currentBudget: Budget,
        newLimit: Long
    )
}
