package com.luna.budgetapp.data.local.repository

import com.luna.budgetapp.data.local.dao.CategoryFilterDao
import com.luna.budgetapp.data.local.entity.CategoryFilterEntity
import com.luna.budgetapp.data.mapper.toModel
import com.luna.budgetapp.data.mapper.toEntity
import com.luna.budgetapp.domain.repository.CategoryRepository
import com.luna.budgetapp.domain.model.CategoryFilter
import com.luna.budgetapp.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryFilterRepositoryImpl(
    private val dao: CategoryFilterDao
) : CategoryRepository {

    override fun getProfiles(): Flow<List<String>> {
        return dao.getProfiles()
    }

    override fun getProfile(profileName: String): Flow<List<CategoryFilter>> =
        dao.getProfile(profileName)
            .map { entities -> entities.map(CategoryFilterEntity::toModel) }

    override suspend fun saveProfile(items: List<CategoryFilter>) {
        dao.upsertAll(items.map(CategoryFilter::toEntity))
    }

    override suspend fun deleteProfile(profileName: String) {
        dao.deleteProfile(profileName)
    }

    override suspend fun hasAny(): Boolean {
        return dao.hasAny()
    }

    override suspend fun initializeIfNeeded() {
        if (dao.hasAny()) return

        val profileName = "Default"
        val defaultCategories = setOf(
            Category.FOOD,
            Category.DATE,
            Category.BEVERAGE,
            Category.COMMUTE,
            Category.OTHERS,
            Category.FITNESS
        )

        val items = Category.entries.map { category ->
            CategoryFilter(
                profileName = profileName,
                category = category,
                isActive = category in defaultCategories
            )
        }

        dao.upsertAll(items.map(CategoryFilter::toEntity))
    }
}
