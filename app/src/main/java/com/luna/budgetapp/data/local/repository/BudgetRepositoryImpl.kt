package com.luna.budgetapp.data.local.repository

import com.luna.budgetapp.data.local.dao.BudgetDao
import com.luna.budgetapp.data.local.entity.BudgetEntity
import com.luna.budgetapp.data.local.entity.BudgetInteractorCategoryEntity
import com.luna.budgetapp.data.mapper.toEntity
import com.luna.budgetapp.data.mapper.toModel
import com.luna.budgetapp.domain.model.Budget
import kotlinx.coroutines.flow.Flow
import com.luna.budgetapp.domain.repository.BudgetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import java.time.LocalDate

class BudgetRepositoryImpl(
    val dao: BudgetDao
) : BudgetRepository {

    override fun getBudgets(): Flow<List<Budget>> {
        return dao.getBudgetsWithInteractors().map { list ->
            list.map { it.toModel() }
        }
            .flowOn(Dispatchers.IO)
    }

    override fun getBudgetByName(name: String): Flow<Budget> {
        return dao.getBudgetWithInteractorsByName(name).mapNotNull { compoundObj ->
            compoundObj?.toModel()
        }.flowOn(Dispatchers.IO)
    }

    override fun getBudgetById(budgetId: Long): Flow<Budget> {
        return dao.getBudgetWithInteractorsById(budgetId)
            .mapNotNull { it?.toModel() }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun insertBudget(budget: Budget) {
        val generatedId = dao.insertBudget(budget.toEntity())
        val interactionEntities = budget.interactors.map { category ->
            BudgetInteractorCategoryEntity(
                budgetId = generatedId,
                category = category
            )
        }
        dao.insertInteractors(interactionEntities)
    }

    override suspend fun updateBudget(budget: Budget) {
        val oldWithChildren = dao.getBudgetWithInteractorsByIdOnce(budget.id)
            ?: throw IllegalArgumentException("Budget not found")

        val updatedParent = BudgetEntity(
            id = budget.id,
            remoteId = oldWithChildren.budget.remoteId,
            limit = budget.limit,
            name = budget.name,
            frequency = budget.frequency,
            startDate = budget.startDate,
            endDate = budget.endDate
        )

        dao.updateBudget(updatedParent)

        dao.clearInteractors(budget.id)
        val interactorEntities = budget.interactors.map { category ->
            BudgetInteractorCategoryEntity(budgetId = budget.id, category = category)
        }
        dao.insertInteractors(interactorEntities)
    }

    override suspend fun deleteBudget(budget: Budget) {
        dao.deleteBudget(budget.toEntity())
    }

    override suspend fun updateBudgetWithHistory(currentBudget: Budget, newLimit: Double) {
        val oldWithChildren = dao.getBudgetWithInteractorsByIdOnce(currentBudget.id)
            ?: throw IllegalArgumentException("Budget not found")

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val oldEntity = BudgetEntity(
            id = currentBudget.id,
            remoteId = oldWithChildren.budget.remoteId, // Safely preserved
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

        dao.updateBudgetVersionWithInteractors(
            oldBudget = oldEntity,
            newBudget = newEntity,
            newInteractors = currentBudget.interactors
        )
    }
}
