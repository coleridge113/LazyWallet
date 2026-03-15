package com.luna.budgetapp.domain.usecase

import com.luna.budgetapp.domain.usecase.settings.GetActiveDateFilterUseCase
import com.luna.budgetapp.domain.usecase.settings.SetActiveDateFilterUseCase

data class SettingsUseCases(
    val getActiveDateFilter: GetActiveDateFilterUseCase,
    val setActiveDateFilter: SetActiveDateFilterUseCase
)
