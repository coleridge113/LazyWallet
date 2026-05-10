package com.luna.budgetapp.data.firebase

import com.luna.budgetapp.data.firebase.models.CategoryFilter
import com.luna.budgetapp.data.firebase.models.Expense
import com.luna.budgetapp.data.firebase.models.ExpensePreset
import com.luna.budgetapp.data.local.entity.CategoryFilterEntity
import com.luna.budgetapp.data.local.entity.ExpenseEntity
import com.luna.budgetapp.data.local.entity.ExpensePresetEntity
import java.time.ZoneId
import java.util.Date

fun java.time.LocalDateTime.toDate(): Date {
    return Date.from(this.atZone(ZoneId.systemDefault()).toInstant())
}

fun ExpenseEntity.toFirestoreModel(): Expense {
    return Expense(
        // We leave the ID blank so Firestore auto-generates a String ID
        name = this.name,
        amount = this.amount,
        category = this.category,
        type = this.type,
        date = this.date.toDate() 
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

fun CategoryFilterEntity.toFirestoreModel(): CategoryFilter {
    return CategoryFilter(
        profileName = this.profileName,
        category = this.category.displayName,
        isActive = this.isActive
    )
}