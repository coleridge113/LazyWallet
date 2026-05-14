package com.luna.budgetapp.domain.usecase

import com.luna.budgetapp.domain.usecase.settings.GetActiveDateFilterUseCase
import com.luna.budgetapp.domain.usecase.settings.SetActiveDateFilterUseCase
import com.luna.budgetapp.domain.usecase.settings.GetMigrationStatusUseCase
import com.luna.budgetapp.domain.usecase.settings.SetMigrationCompleteUseCase

data class SettingsUseCases(
    val getActiveDateFilter: GetActiveDateFilterUseCase,
    val setActiveDateFilter: SetActiveDateFilterUseCase,
    val getMigrationStatus: GetMigrationStatusUseCase,
    val setMigrationComplete: SetMigrationCompleteUseCase
)
