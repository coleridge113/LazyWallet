package com.luna.budgetapp.domain.usecase.settings

import com.luna.budgetapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class GetActiveCategoryProfileUseCase(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<String> = repository.activeProfileFlow
}
