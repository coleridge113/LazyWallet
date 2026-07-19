package com.luna.budgetapp.data.mapper

import com.luna.budgetapp.data.local.entity.BudgetEntity
import com.luna.budgetapp.data.local.entity.BudgetInteractorCategoryEntity
import com.luna.budgetapp.data.local.entity.BudgetWithInteractors
import com.luna.budgetapp.domain.model.Budget

// 1. Domain to Flat Entity (Used for initial parent insertion setup)
fun Budget.toEntity(): BudgetEntity {
    return BudgetEntity(
        id = this.id,
        remoteId = null,
        limit = this.limit,
        name = this.name,
        frequency = this.frequency,
        startDate = this.startDate,
        endDate = this.endDate
    )
}

fun BudgetWithInteractors.toModel(): Budget {
    return Budget(
        id = this.budget.id,
        limit = this.budget.limit,
        name = this.budget.name,
        frequency = this.budget.frequency,
        startDate = this.budget.startDate,
        endDate = this.budget.endDate,
        interactors = this.interactors.map { it.category }
    )
}
