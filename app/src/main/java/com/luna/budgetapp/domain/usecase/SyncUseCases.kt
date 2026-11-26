package com.luna.budgetapp.domain.usecase

import com.luna.budgetapp.domain.usecase.sync.MigrateLocalDataUseCase

data class SyncUseCases(
    val migrateLocalData: MigrateLocalDataUseCase
)
