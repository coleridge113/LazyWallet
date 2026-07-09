package com.luna.budgetapp.domain.model

data class Budget(
    val amount: Double,
    val consumed: Double,
    val type: BudgetType
) {
    val remaining: Double = amount - consumed
    val isExceeded: Boolean = amount < consumed
}

enum class BudgetType {
    DAILY,
    WEEKLY,
    MONTHLY
}
