package com.luna.budgetapp.data.local.repository.dev

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.luna.budgetapp.data.datastore.SettingsDataStore
import com.luna.budgetapp.data.firebase.toFirestoreModel
import com.luna.budgetapp.data.local.dao.CategoryFilterDao
import com.luna.budgetapp.data.local.entity.CategoryFilterEntity
import com.luna.budgetapp.data.mapper.toModel
import com.luna.budgetapp.data.mapper.toEntity
import com.luna.budgetapp.domain.repository.CategoryRepository
import com.luna.budgetapp.domain.model.CategoryFilter
import com.luna.budgetapp.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CategoryFilterRepositoryImplDev(
    private val dao: CategoryFilterDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val settingsDataStore: SettingsDataStore
) : CategoryRepository {

    override fun getProfiles(): Flow<List<String>> {
        return dao.getProfiles()
    }

    override fun getProfile(profileName: String): Flow<List<CategoryFilter>> =
        dao.getProfile(profileName)
            .map { entities -> entities.map(CategoryFilterEntity::toModel) }

    override suspend fun saveProfile(items: List<CategoryFilter>) {
        val entities = items.map(CategoryFilter::toEntity)
        dao.upsertAll(entities)
        val userId = auth.currentUser?.uid

        if (userId != null) {
            try {
                val batch = firestore.batch()
                val userRef = firestore.collection("users").document(userId)
                
                entities.forEach { entity ->
                    val customDocId = "${entity.profileName}_${entity.category.name}"
                    val docRef = userRef.collection("category_filters").document(customDocId)
                    batch.set(docRef, entity.toFirestoreModel())
                }
                
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("Sync", "Failed to sync category profile", e)
            }
        }
    }

    override suspend fun deleteProfile(profileName: String) {
        val filtersToDelete = dao.getAllFiltersOnce().filter { it.profileName == profileName }
        dao.deleteProfile(profileName)
        val userId = auth.currentUser?.uid

        if (userId != null) {
            try {
                val batch = firestore.batch()
                val userRef = firestore.collection("users").document(userId)
                
                filtersToDelete.forEach { entity ->
                    val customDocId = "${entity.profileName}_${entity.category.name}"
                    val docRef = userRef.collection("category_filters").document(customDocId)
                    batch.delete(docRef)
                }
                
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("Sync", "Failed to sync deleted category profile", e)
            }
        }
    }

    override suspend fun hasAny(): Boolean {
        return dao.hasAny()
    }

    override suspend fun initializeIfNeeded() {
        if (dao.hasAny()) return

        val profileName = "All"

        val items = Category.entries.map { category ->
            CategoryFilter(
                profileName = profileName,
                category = category,
                isActive = true
            )
        }

        saveProfile(items) // Use saveProfile to trigger sync
    }
}
