package com.luna.budgetapp.data.local.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.luna.budgetapp.data.datastore.SettingsDataStore
import com.luna.budgetapp.data.firebase.toFirestoreModel
import com.luna.budgetapp.data.local.dao.ExpensePresetDao
import com.luna.budgetapp.data.mapper.toEntity
import com.luna.budgetapp.data.mapper.toModel
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.domain.repository.ExpensePresetRepository
import com.luna.budgetapp.network.ExpenseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.util.UUID

class ExpensePresetRepositoryImpl(
    private val dao: ExpensePresetDao,
    private val api: ExpenseService,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val settingsDataStore: SettingsDataStore
): ExpensePresetRepository {
    
    override fun getAllExpensePresets(): Flow<List<ExpensePreset>> =
        dao.getAllExpensePresets()
            .map { local ->
                local.map { it.toModel() }
            }
            .flowOn(Dispatchers.IO)

    override suspend fun addExpensePresets(expensePresets: List<ExpensePreset>) {
        dao.addExpensePresets(expensePresets.map { it.toEntity() })
    }

    override suspend fun addExpensePreset(expensePreset: ExpensePreset) {
        val userId = auth.currentUser?.uid
        val remoteId = UUID.randomUUID().toString()

        val entity = expensePreset.toEntity().copy(remoteId = remoteId)
        dao.addExpensePreset(entity)

        if (userId != null) {
            try {
                val firestoreModel = entity.toFirestoreModel()
                firestore.collection("users").document(userId)
                    .collection("expense_presets").document(remoteId)
                    .set(firestoreModel)
            } catch (e: Exception) {
                Log.e("Sync", "Failed to sync added expense preset", e)
            }
        }
    }

    override suspend fun updateExpensePreset(expensePreset: ExpensePreset) {
        val userId = auth.currentUser?.uid
        val existingEntity = expensePreset.id?. let { dao.getExpensePresetByIdOnce(it) }
        val remoteId = existingEntity?.remoteId
        val entity = expensePreset.toEntity().copy(remoteId = remoteId)
        dao.addExpensePreset(entity)

        if (userId != null && remoteId != null) {
            try {
                val firestoreModel = entity.toFirestoreModel()
                firestore.collection("users").document(userId)
                    .collection("expense_presets").document(remoteId)
                    .set(firestoreModel)
            } catch (e: Exception) {
                Log.e("Sync", "Failed to sync updated expense preset", e)
            }
        }
    }

    override suspend fun deleteExpensePreset(expensePresetId: Long) {
        val preset = dao.getExpensePresetByIdOnce(expensePresetId)
        val remoteId = preset?.remoteId
        val userId = auth.currentUser?.uid

        dao.deleteExpensePreset(expensePresetId)

        if (remoteId != null && userId != null) {
            try {
                firestore.collection("users").document(userId)
                    .collection("expense_presets").document(remoteId)
                    .delete()
            } catch (e: Exception) {
                Log.e("Sync", "Failed to sync deleted expense preset", e)
            }
        }
    }
}
