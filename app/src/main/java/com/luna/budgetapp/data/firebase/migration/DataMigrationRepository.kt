package com.luna.budgetapp.data.firebase.migration

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.luna.budgetapp.data.firebase.models.CategoryFilter
import com.luna.budgetapp.data.firebase.models.Expense
import com.luna.budgetapp.data.firebase.models.ExpensePreset
import com.luna.budgetapp.data.firebase.toEntity
import com.luna.budgetapp.data.firebase.toFirestoreModel
import com.luna.budgetapp.data.local.dao.CategoryFilterDao
import com.luna.budgetapp.data.local.dao.ExpenseDao
import com.luna.budgetapp.data.local.dao.ExpensePresetDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DataMigrationRepository(
    private val expenseDao: ExpenseDao,
    private val expensePresetDao: ExpensePresetDao,
    private val categoryFilterDao: CategoryFilterDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun performMigration() = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        val userId = user.uid

        // 1. Fetch from Room
        val localExpenses = expenseDao.getAllExpensesOnce()
        val localFilters = categoryFilterDao.getAllFiltersOnce()
        val localPresets = expensePresetDao.getAllExpensePresetsOnce()

        if (localExpenses.isEmpty() && localFilters.isEmpty() && localPresets.isEmpty()) return@withContext

        val batch = firestore.batch()
        val userRef = firestore.collection("users").document(userId)

        // 2. Queue the Expenses
        localExpenses.forEach { entity ->
            val firestoreModel = entity.toFirestoreModel()
            val docRef = userRef.collection("expenses").document() // Random ID
            batch.set(docRef, firestoreModel)
        }

        // 3. Queue the Profile Filters
        localFilters.forEach { entity ->
            val firestoreModel = entity.toFirestoreModel()
            // Create the composite ID: e.g., "Work_Food"
            val customDocId = "${entity.profileName}_${entity.category.name}"

            val docRef = userRef.collection("category_filters").document(customDocId)
            batch.set(docRef, firestoreModel)
        }

        // 4. Queue the Expense Presets
        localPresets.forEach { entity ->
            val firestoreModel = entity.toFirestoreModel()
            val docRef = userRef.collection("expense_presets").document() // Random ID
            batch.set(docRef, firestoreModel)
        }

        try {
            // 5. Fire the single atomic network request
            batch.commit().await()

            // 6. Success! Clear Room
            expenseDao.deleteAll()
            categoryFilterDao.deleteAll()
            expensePresetDao.deleteAll()

            Log.d("Migration", "Successfully moved expenses, filters, and presets to the cloud.")
        } catch (e: Exception) {
            Log.e("Migration", "Cloud sync failed.", e)
            throw e
        }
    }

    suspend fun syncFromCloud() = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        val userId = user.uid
        val userRef = firestore.collection("users").document(userId)

        try {
            val expenses = userRef.collection("expenses").get().await()
                .toObjects(Expense::class.java)
            
            val presets = userRef.collection("expense_presets").get().await()
                .toObjects(ExpensePreset::class.java)
            
            val filters = userRef.collection("category_filters").get().await()
                .toObjects(CategoryFilter::class.java)

            expenseDao.deleteAll()
            expensePresetDao.deleteAll()
            categoryFilterDao.deleteAll()

            expenseDao.addExpenses(expenses.map { it.toEntity() })
            expensePresetDao.addExpensePresets(presets.map { it.toEntity() })
            categoryFilterDao.upsertAll(filters.map { it.toEntity() })

            Log.d("Sync", "Successfully synced data from cloud to local.")
        } catch (e: Exception) {
            Log.e("Sync", "Cloud fetch failed.", e)
            throw e
        }
    }
}
