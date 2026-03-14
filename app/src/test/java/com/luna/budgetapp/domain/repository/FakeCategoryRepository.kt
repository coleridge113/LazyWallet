package com.luna.budgetapp.domain.repository

import com.luna.budgetapp.domain.model.CategoryFilter
import kotlinx.coroutines.flow.Flow

class FakeCategoryRepository : CategoryRepository {
    override fun getProfiles(): Flow<List<String>> {
        TODO("Not yet implemented")
    }

    override fun getProfile(profileName: String): Flow<List<CategoryFilter>> {
        TODO("Not yet implemented")
    }

    override suspend fun saveProfile(items: List<CategoryFilter>) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteProfile(profileName: String) {
        TODO("Not yet implemented")
    }

    override suspend fun hasAny(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun initializeIfNeeded() {
        TODO("Not yet implemented")
    }
}