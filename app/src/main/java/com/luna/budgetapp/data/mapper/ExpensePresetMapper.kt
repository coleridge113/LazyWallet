package com.luna.budgetapp.data.mapper

import com.luna.budgetapp.data.local.entity.ExpensePresetEntity
import com.luna.budgetapp.data.remote.dto.ExpensePresetDto
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.domain.model.toCategory

fun ExpensePresetDto.toEntity(): ExpensePresetEntity {
    return ExpensePresetEntity(
        id = this.id,
        amount = this.amount,
        category = this.category.toCategory(),
        type = this.type,
        createdAt = this.createdAt
    )
}

fun ExpensePresetEntity.toModel(): ExpensePreset {
    return ExpensePreset(
        id = this.id,
        amount = this.amount,
        category = this.category,
        type = this.type,
        createdAt = this.createdAt,
        position = this.position
    )
}

fun ExpensePreset.toEntity(): ExpensePresetEntity {
    return ExpensePresetEntity(
        id = this.id ?: 0,
        amount = this.amount,
        category = this.category,
        type = this.type,
        createdAt = this.createdAt,
        position = this.position
    )
}
