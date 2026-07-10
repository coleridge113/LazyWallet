package com.luna.budgetapp.data.mapper

import com.luna.budgetapp.data.local.entity.BudgetEntity
import com.luna.budgetapp.domain.model.Budget

fun Budget.toEntity(): BudgetEntity {
    return BudgetEntity(
        id = this.id,
        limit = this.limit,
        name = this.name,
        frequency = this.frequency,
        startDate = this.startDate,
        endDate = this.endDate
    )
}

fun BudgetEntity.toModel(): Budget {
    return Budget(
        id = this.id,
        limit = this.limit,
        name = this.name,
        frequency = this.frequency,
        startDate = this.startDate,
        endDate = this.endDate
    )
}