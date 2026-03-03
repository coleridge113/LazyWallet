package com.luna.budgetapp.domain.repository

import com.luna.budgetapp.domain.model.CategoryFilter
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun getProfiles(): Flow<List<String>>

    fun getProfile(profileName: String): Flow<List<CategoryFilter>>

    suspend fun saveProfile(items: List<CategoryFilter>)

    suspend fun deleteProfile(profileName: String)

    suspend fun hasAny(): Boolean

    suspend fun initializeIfNeeded()
}
