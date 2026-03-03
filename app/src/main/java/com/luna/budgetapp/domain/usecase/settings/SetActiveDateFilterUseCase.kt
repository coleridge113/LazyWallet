package com.luna.budgetapp.domain.usecase.settings

import com.luna.budgetapp.domain.repository.SettingsRepository
import com.luna.budgetapp.domain.model.DateFilter

class SetActiveDateFilterUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(dateFilter: DateFilter) {
        repository.setActiveDateFilter(dateFilter)
    }
}
