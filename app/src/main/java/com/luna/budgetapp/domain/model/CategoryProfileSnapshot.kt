package com.luna.budgetapp.domain.model

data class CategoryProfileSnapshot(
    val activeProfile: String,
    val categoryMap: Map<Category, Boolean>,
    val profileList: List<String>
)
