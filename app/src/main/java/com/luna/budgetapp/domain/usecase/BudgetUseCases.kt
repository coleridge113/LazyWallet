package com.luna.budgetapp.domain.usecase

import com.luna.budgetapp.domain.usecase.budget.GetAllBudgetUseCase
import com.luna.budgetapp.domain.usecase.budget.GetBudgetByIdUseCase
import com.luna.budgetapp.domain.usecase.budget.SaveBudgetUseCase
import com.luna.budgetapp.domain.usecase.budget.UpdateBudgetUseCase
import com.luna.budgetapp.domain.usecase.budget.DeleteBudgetUseCase

data class BudgetUseCases(
    val saveBudget: SaveBudgetUseCase,
    val getBudgetById: GetBudgetByIdUseCase,
    val updateBudget: UpdateBudgetUseCase,
    val getAllBudget: GetAllBudgetUseCase,
    val deleteBudget: DeleteBudgetUseCase,
)
