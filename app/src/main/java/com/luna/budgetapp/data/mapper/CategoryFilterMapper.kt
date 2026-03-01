package com.luna.budgetapp.data.mapper

import com.luna.budgetapp.domain.model.CategoryFilter
import com.luna.budgetapp.data.local.entity.CategoryFilterEntity

fun CategoryFilterEntity.toModel(): CategoryFilter {
    return CategoryFilter(
        profileName = this.profileName,
        category = this.category,
        isActive = this.isActive
    )
}

fun CategoryFilter.toEntity(): CategoryFilterEntity {
    return CategoryFilterEntity(
        profileName = this.profileName,
        category = this.category,
        isActive = this.isActive
    )
}
