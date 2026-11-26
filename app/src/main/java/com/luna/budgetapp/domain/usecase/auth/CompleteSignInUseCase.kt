package com.luna.budgetapp.domain.usecase.auth

import android.util.Log
import com.luna.budgetapp.domain.repository.CloudSyncRepository
import com.luna.budgetapp.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException

class CompleteSignInUseCase(
    private val cloudSync: CloudSyncRepository,
    private val settings: SettingsRepository
) {
    suspend operator fun invoke(isGuest: Boolean) {
        try {
            cloudSync.downloadCloudData()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w("CompleteSignInUseCase", "downloadCloudData failed", e)
        }
        settings.setGuestMode(isGuest)
    }
}
