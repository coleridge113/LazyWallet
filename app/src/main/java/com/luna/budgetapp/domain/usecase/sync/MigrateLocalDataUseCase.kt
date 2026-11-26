package com.luna.budgetapp.domain.usecase.sync

import com.luna.budgetapp.domain.repository.CloudSyncRepository
import com.luna.budgetapp.domain.repository.SettingsRepository

class MigrateLocalDataUseCase(
    private val cloudSync: CloudSyncRepository,
    private val settings: SettingsRepository
) {
    suspend operator fun invoke() {
        cloudSync.uploadLocalData()
        cloudSync.downloadCloudData()
        settings.setMigrationComplete()
    }
}
