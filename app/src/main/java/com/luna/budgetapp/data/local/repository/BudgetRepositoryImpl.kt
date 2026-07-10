package com.luna.budgetapp.data.local.repository

import com.luna.budgetapp.data.local.dao.BudgetDao
import com.luna.budgetapp.data.local.entity.BudgetEntity
import com.luna.budgetapp.data.mapper.toEntity
import com.luna.budgetapp.data.mapper.toModel
import com.luna.budgetapp.domain.model.Budget
import kotlinx.coroutines.flow.Flow
import com.luna.budgetapp.domain.repository.BudgetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate

class BudgetRepositoryImpl(
    val dao: BudgetDao
) : BudgetRepository {

    override fun getBudgets(): Flow<List<Budget>> {
        return flow<List<Budget>> {
            try {
                dao.getBudgets().forEach { it.toModel() }
            } catch (e: Exception) {
                emptyList<Budget>()
                throw e
            }
        }
            .flowOn(Dispatchers.IO)
    }

    override fun getBudgetByName(name: String): Flow<Budget> {
        return flow<Budget> {
            dao.getBudgetByName(name).toModel()
        }
            .flowOn(Dispatchers.IO)
    }

    override fun getBudgetById(budgetId: Long): Flow<Budget> {
        return flow<Budget> {
            dao.getBudgetById(budgetId)
        }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun insertBudget(budget: Budget) {
        try {
            val entity = budget.toEntity()
            dao.insertBudget(entity)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun updateBudget(budget: Budget) {
        val old = dao.getBudgetById(budget.id)
        val new = BudgetEntity(
            id = budget.id,
            remoteId = old.remoteId,
            limit = budget.limit,
            name = budget.name,
            frequency = budget.frequency,
            startDate = budget.startDate,
            endDate = budget.endDate
        )
        dao.insertBudget(new)
    }

    override suspend fun deleteBudget(budget: Budget) {
        try {
            dao.deleteBudget(budget.toEntity())
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun updateBudgetWithHistory(currentBudget: Budget, newLimit: Double) {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val oldEntity = BudgetEntity(
            id = currentBudget.id,
            remoteId = currentBudget.id.toString(),
            limit = currentBudget.limit,
            name = currentBudget.name,
            frequency = currentBudget.frequency,
            startDate = currentBudget.startDate,
            endDate = yesterday
        )

        val newEntity = BudgetEntity(
            id = 0,
            remoteId = null,
            limit = newLimit,
            name = currentBudget.name,
            frequency = currentBudget.frequency,
            startDate = today,
            endDate = null
        )
        dao.updateBudget(oldEntity)
        dao.insertBudget(newEntity)
    }
}
