package com.luna.budgetapp.presentation.screen.migration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.data.firebase.migration.DataMigrationRepository
import com.luna.budgetapp.domain.usecase.SettingsUseCases
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MigrationViewModel(
    private val migrationRepository: DataMigrationRepository,
    private val settingsUseCases: SettingsUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(MigrationUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigation = Channel<MigrationNavigation>()
    val navigation = _navigation.receiveAsFlow()

    fun onEvent(event: MigrationEvent) {
        when (event) {
            MigrationEvent.StartMigration -> startMigration()
            MigrationEvent.SkipMigration -> skipMigration()
        }
    }

    private fun startMigration() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                migrationRepository.performMigration()
                migrationRepository.syncFromCloud()
                settingsUseCases.setMigrationComplete()
                _navigation.send(MigrationNavigation.GotoAddExpenseRoute)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Migration failed") }
            }
        }
    }

    private fun skipMigration() {
        viewModelScope.launch {
            settingsUseCases.setMigrationComplete()
            _navigation.send(MigrationNavigation.GotoAddExpenseRoute)
        }
    }
}

data class MigrationUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface MigrationEvent {
    data object StartMigration : MigrationEvent
    data object SkipMigration : MigrationEvent
}

sealed interface MigrationNavigation {
    data object SetMigrationComplete : MigrationNavigation
    data object GotoAddExpenseRoute : MigrationNavigation
}
