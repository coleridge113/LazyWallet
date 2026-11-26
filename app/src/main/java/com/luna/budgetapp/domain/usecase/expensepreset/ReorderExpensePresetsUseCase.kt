package com.luna.budgetapp.domain.usecase.expensepreset

import com.luna.budgetapp.domain.repository.ExpensePresetRepository

class ReorderExpensePresetsUseCase(
    private val repository: ExpensePresetRepository
) {
    suspend operator fun invoke(orderedIds: List<Long>) {
        repository.reorderExpensePresets(orderedIds)
    }
}
