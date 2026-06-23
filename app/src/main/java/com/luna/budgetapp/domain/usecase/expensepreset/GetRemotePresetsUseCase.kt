package com.luna.budgetapp.domain.usecase.expensepreset

import com.luna.budgetapp.domain.repository.ExpensePresetRepository

class GetRemotePresetsUseCase(
    private val repository: ExpensePresetRepository
) {
    suspend operator fun invoke() {
        repository.getAllPresetsOnce()
    }
}