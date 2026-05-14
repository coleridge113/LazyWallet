package com.luna.budgetapp.presentation.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.domain.usecase.AuthUseCases
import com.luna.budgetapp.domain.usecase.SettingsUseCases
import com.luna.budgetapp.data.firebase.migration.DataMigrationRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow

class AuthViewModel(
    private val authUseCases: AuthUseCases,
    private val settingsUseCases: SettingsUseCases,
    private val migrationRepository: DataMigrationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState.Success())
    val uiState = _uiState.asStateFlow()

    private val _navigation = Channel<Navigation>()
    val navigation = _navigation.receiveAsFlow()

    fun onEvent(event: Event) {
        when (event) {
            Event.HandleSignInSuccess -> { handleSignInSuccess() }
        }
    }

    private fun handleSignInSuccess() {
        viewModelScope.launch {
            val isMigrated = settingsUseCases.getMigrationStatus().first()
            if (isMigrated) {
                try {
                    migrationRepository.syncFromCloud()
                } catch (_: Exception) { }
                _navigation.send(Navigation.GotoAddExpenseRoute)
            } else {
                _navigation.send(Navigation.GotoMigrationRoute)
            }
        }
    }
}
