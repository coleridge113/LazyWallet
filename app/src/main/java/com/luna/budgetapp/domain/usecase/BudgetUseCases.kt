package com.luna.budgetapp.domain.usecase

import com.luna.budgetapp.domain.usecase.budget.GetAllBudgetUseCase
import com.luna.budgetapp.domain.usecase.budget.GetBudgetByIdUseCase
import com.luna.budgetapp.domain.usecase.budget.SaveBudgetUseCase
import com.luna.budgetapp.domain.usecase.budget.UpdateBudgetUseCase

data class BudgetUseCases(
    val saveBudget: SaveBudgetUseCase,
    val getBudgetById: GetBudgetByIdUseCase,
    val updateBudget: UpdateBudgetUseCase,
    val getAllBudget: GetAllBudgetUseCase
)
