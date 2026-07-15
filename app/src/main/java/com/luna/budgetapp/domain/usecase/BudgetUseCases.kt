package com.luna.budgetapp.domain.usecase

import com.luna.budgetapp.domain.usecase.budget.GetBudgetByIdUseCase
import com.luna.budgetapp.domain.usecase.budget.SaveBudgetUseCase

data class BudgetUseCases(
    val saveBudget: SaveBudgetUseCase,
    val getBudgetById: GetBudgetByIdUseCase,
)
