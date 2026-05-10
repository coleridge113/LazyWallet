package com.luna.budgetapp.data.firebase.migration

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.luna.budgetapp.data.firebase.toFirestoreModel
import com.luna.budgetapp.data.local.dao.CategoryFilterDao
import com.luna.budgetapp.data.local.dao.ExpenseDao
import kotlinx.coroutines.tasks.await

class DataMigrationRepository(
    private val expenseDao: ExpenseDao,
    private val categoryFilterDao: CategoryFilterDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun performMigration() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        // 1. Fetch from Room
        val localExpenses = expenseDao.getAllExpensesOnce()
        val localFilters = categoryFilterDao.getAllFiltersOnce() // Add your filter DAO call

        if (localExpenses.isEmpty() && localFilters.isEmpty()) return

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

        try {
            // 4. Fire the single atomic network request
            batch.commit().await()

            // 5. Success! Clear Room
            expenseDao.deleteAll()
            categoryFilterDao.deleteAll()

            Log.d("Migration", "Successfully moved expenses and filters to the cloud.")
        } catch (e: Exception) {
            Log.e("Migration", "Cloud sync failed.", e)
        }
    }
}
