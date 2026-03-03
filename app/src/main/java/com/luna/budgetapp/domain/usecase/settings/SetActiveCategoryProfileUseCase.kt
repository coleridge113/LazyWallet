package com.luna.budgetapp.domain.usecase.settings

import com.luna.budgetapp.domain.repository.SettingsRepository

class SetActiveCategoryProfileUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(profileName: String) {
        repository.setActiveProfile(profileName)
    }
}
