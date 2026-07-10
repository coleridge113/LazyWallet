package com.luna.budgetapp.domain.model

import java.time.LocalDate

data class Budget(
    val id: Long = 0,
    val limit: Double,
    val name: String,
    val frequency: BudgetFrequency,
    val interactors: List<Category>,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
)

enum class BudgetFrequency {
    DAILY,
    WEEKLY,
    BI_WEEKLY,
    MONTHLY,
    QUARTERLY,
    BI_YEARLY,
    YEARLY,
    CUSTOM;

    fun getFriendlyName(): String {
        return when (this) {
            DAILY, WEEKLY, MONTHLY,
            QUARTERLY, YEARLY, CUSTOM ->
                this.name.lowercase().replaceFirstChar { it.uppercase() }

            BI_WEEKLY -> "Every 2 weeks"
            BI_YEARLY -> "Every 6 months"
        }
    }
}