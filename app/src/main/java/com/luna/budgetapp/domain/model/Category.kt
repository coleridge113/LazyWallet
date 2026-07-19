package com.luna.budgetapp.domain.model

enum class Category {
    FOOD,
    DATE,
    BEVERAGE,
    HOUSE,
    COMMUTE,
    BILLS,
    OTHERS,
    GROCERY,
    FITNESS,
    PERSONAL;

    fun getDisplayName(): String {
        return name.lowercase().replaceFirstChar { it.uppercase() }
    }
}
