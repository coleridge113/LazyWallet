package com.luna.budgetapp.domain.repository

import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.CategoryFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeCategoryRepository : CategoryRepository {

    private val profilesFlow =
        MutableStateFlow<Map<String, List<CategoryFilter>>>(emptyMap())

    override fun getProfiles(): Flow<List<String>> {
        return profilesFlow.map { it.keys.toList() }
    }

    override fun getProfile(profileName: String): Flow<List<CategoryFilter>> {
        return profilesFlow.map { profiles ->
            profiles[profileName] ?: emptyList()
        }
    }

    override suspend fun saveProfile(items: List<CategoryFilter>) {
        if (items.isEmpty()) return

        val profileName = items.first().profileName

        profilesFlow.update { profiles ->
            profiles + (profileName to items)
        }
    }

    override suspend fun deleteProfile(profileName: String) {
        profilesFlow.update { profiles ->
            profiles - profileName
        }
    }

    override suspend fun hasAny(): Boolean {
        return profilesFlow.value.isNotEmpty()
    }

    override suspend fun initializeIfNeeded() {
        if (profilesFlow.value.isNotEmpty()) return

        val profileName = "All"

        val items = Category.entries.map { category ->
            CategoryFilter(
                profileName = profileName,
                category = category,
                isActive = true
            )
        }

        profilesFlow.value = mapOf(profileName to items)
    }
}
