package com.luna.budgetapp.domain.usecase.auth

import com.luna.budgetapp.domain.repository.AuthRepository
import com.luna.budgetapp.domain.repository.LocalDataRepository
import com.luna.budgetapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class SignOutUseCase(
    private val auth: AuthRepository,
    private val settings: SettingsRepository,
    private val localData: LocalDataRepository
) {
    suspend operator fun invoke() {
        auth.signOut()
        if (!settings.isGuestFlow.first()) {
            localData.clearAll()
        }
    }
}
