package com.luna.budgetapp.domain.model


data class CategoryFilter(
    val profileName: String,
    val category: Category,
    val isActive: Boolean
)
