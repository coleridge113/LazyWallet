package com.luna.budgetapp.domain.usecase.expensepreset

import com.luna.budgetapp.domain.repository.ExpensePresetRepository
import com.luna.budgetapp.domain.model.ExpensePreset
import kotlinx.coroutines.flow.Flow

class GetAllExpensePresetsUseCase(
    private val repository: ExpensePresetRepository
) {
    operator fun invoke(): Flow<List<ExpensePreset>> {
        return repository.getAllExpensePresets()
    }
}
