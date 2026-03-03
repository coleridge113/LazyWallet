package com.luna.budgetapp.domain.usecase.settings

import com.luna.budgetapp.domain.repository.SettingsRepository
import com.luna.budgetapp.domain.model.DateFilter

class GetActiveDateFilterUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke() = repository.activeDateFilterFlow
}
