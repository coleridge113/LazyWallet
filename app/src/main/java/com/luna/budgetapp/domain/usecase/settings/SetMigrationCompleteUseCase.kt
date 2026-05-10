package com.luna.budgetapp.domain.usecase.settings

import com.luna.budgetapp.domain.repository.SettingsRepository

class SetMigrationCompleteUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke() = repository.setMigrationComplete()
}
