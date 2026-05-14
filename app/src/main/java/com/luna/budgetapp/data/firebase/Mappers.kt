package com.luna.budgetapp.data.firebase

import com.luna.budgetapp.data.firebase.models.CategoryFilter
import com.luna.budgetapp.data.firebase.models.Expense
import com.luna.budgetapp.data.firebase.models.ExpensePreset
import com.luna.budgetapp.data.local.entity.CategoryFilterEntity
import com.luna.budgetapp.data.local.entity.ExpenseEntity
import com.luna.budgetapp.data.local.entity.ExpensePresetEntity
import com.luna.budgetapp.domain.model.Category
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

fun LocalDateTime.toDate(): Date {
    return Date.from(this.atZone(ZoneId.systemDefault()).toInstant())
}

fun Date.toLocalDateTime(): LocalDateTime {
    return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
}

fun ExpenseEntity.toFirestoreModel(): Expense {
    return Expense(
        name = this.name,
        amount = this.amount,
        category = this.category,
        type = this.type,
        date = this.date.toDate() 
    )
}

fun Expense.toEntity(): ExpenseEntity {
    return ExpenseEntity(
        remoteId = this.id.ifBlank { null },
        name = this.name,
        amount = this.amount,
        category = this.category,
        type = this.type,
        date = this.date.toLocalDateTime()
    )
}

fun ExpensePresetEntity.toFirestoreModel(): ExpensePreset {
    return ExpensePreset(
        amount = this.amount,
        category = this.category,
        type = this.type,
        createdAt = this.createdAt.toDate()
    )
}

fun ExpensePreset.toEntity(): ExpensePresetEntity {
    return ExpensePresetEntity(
        id = 0,
        remoteId = this.id.ifBlank { null },
        amount = this.amount,
        category = this.category,
        type = this.type,
        createdAt = this.createdAt.toLocalDateTime()
    )
}

fun CategoryFilterEntity.toFirestoreModel(): CategoryFilter {
    return CategoryFilter(
        profileName = this.profileName,
        category = this.category.displayName,
        active = this.isActive
    )
}

fun CategoryFilter.toEntity(): CategoryFilterEntity {
    return CategoryFilterEntity(
        profileName = this.profileName,
        category = Category.entries.first { it.displayName == this.category },
        isActive = this.active
    )
}
