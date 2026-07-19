package com.luna.budgetapp.data.local.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.luna.budgetapp.data.firebase.toFirestoreModel
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
import java.util.UUID

class BudgetRepositoryImpl(
    private val dao: BudgetDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
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
        val userId = auth.currentUser?.uid
        val remoteId = UUID.randomUUID().toString()

        val generatedId = dao.insertBudget(budget.toEntity().copy(remoteId = remoteId))
        val interactionEntities = budget.interactors.map { category ->
            BudgetInteractorCategoryEntity(
                budgetId = generatedId,
                category = category
            )
        }
        dao.insertInteractors(interactionEntities)

        if (userId != null) {
            try {
                val budgetWithInteractors = dao.getBudgetWithInteractorsByIdOnce(generatedId)
                val firestoreModel = budgetWithInteractors?.budget?.toFirestoreModel(budget.interactors)
                if (firestoreModel != null) {
                    firestore.collection("users").document(userId)
                        .collection("budgets").document(remoteId)
                        .set(firestoreModel)
                }
            } catch (e: Exception) {
                Log.e("Sync", "Failed to sync added budget", e)
            }
        }
    }

    override suspend fun updateBudget(budget: Budget) {
        val oldWithChildren = dao.getBudgetWithInteractorsByIdOnce(budget.id)
            ?: throw IllegalArgumentException("Budget not found")

        val remoteId = oldWithChildren.budget.remoteId
        val userId = auth.currentUser?.uid

        val updatedParent = BudgetEntity(
            id = budget.id,
            remoteId = remoteId,
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

        if (userId != null && remoteId != null) {
            try {
                val firestoreModel = updatedParent.toFirestoreModel(budget.interactors)
                firestore.collection("users").document(userId)
                    .collection("budgets").document(remoteId)
                    .set(firestoreModel)
            } catch (e: Exception) {
                Log.e("Sync", "Failed to sync updated budget", e)
            }
        }
    }

    override suspend fun deleteBudget(budget: Budget) {
        val userId = auth.currentUser?.uid
        val entity = dao.getBudgetByIdOnce(budget.id) ?: return
        val remoteId = entity.remoteId

        dao.deleteBudget(budget.toEntity().copy(remoteId = remoteId))

        if (userId != null && remoteId != null) {
            try {
                firestore.collection("users").document(userId)
                    .collection("budgets").document(remoteId)
                    .delete()
            } catch (e: Exception) {
                Log.e("Sync", "Failed to sync deleted budget", e)
            }
        }
    }

    override suspend fun updateBudgetWithHistory(currentBudget: Budget, newLimit: Double) {
        val oldWithChildren = dao.getBudgetWithInteractorsByIdOnce(currentBudget.id)
            ?: throw IllegalArgumentException("Budget not found")

        val userId = auth.currentUser?.uid
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val oldRemoteId = oldWithChildren.budget.remoteId
        val newRemoteId = UUID.randomUUID().toString()

        val oldEntity = BudgetEntity(
            id = currentBudget.id,
            remoteId = oldRemoteId,
            limit = currentBudget.limit,
            name = currentBudget.name,
            frequency = currentBudget.frequency,
            startDate = currentBudget.startDate,
            endDate = yesterday
        )

        val newEntity = BudgetEntity(
            id = 0,
            remoteId = newRemoteId,
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

        if (userId != null) {
            try {
                if (oldRemoteId != null) {
                    val oldFirestoreModel = oldEntity.toFirestoreModel(currentBudget.interactors)
                    firestore.collection("users").document(userId)
                        .collection("budgets").document(oldRemoteId)
                        .set(oldFirestoreModel)
                }

                val newFirestoreModel = newEntity.toFirestoreModel(currentBudget.interactors)
                firestore.collection("users").document(userId)
                    .collection("budgets").document(newRemoteId)
                    .set(newFirestoreModel)
            } catch (e: Exception) {
                Log.e("Sync", "Failed to sync budget versioning", e)
            }
        }
    }
}
