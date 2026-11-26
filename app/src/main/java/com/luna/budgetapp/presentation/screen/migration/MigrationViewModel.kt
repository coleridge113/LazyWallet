package com.luna.budgetapp.presentation.screen.migration

import android.util.Log
import androidx.annotation.StringRes
import com.luna.budgetapp.R
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.data.firebase.migration.DataMigrationRepository
import com.luna.budgetapp.domain.usecase.SettingsUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MigrationViewModel(
    private val migrationRepository: DataMigrationRepository,
    private val settingsUseCases: SettingsUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(MigrationUiState())
    val uiState = _uiState.asStateFlow()

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
                _uiState.update { it.copy(isLoading = false, isDone = true) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("MigrationViewModel", "Migration failed", e)
                _uiState.update { it.copy(isLoading = false, error = R.string.migration_error) }
            }
        }
    }

    fun onDoneHandled() {
        _uiState.update { it.copy(isDone = false) }
    }

    private fun skipMigration() {
        viewModelScope.launch {
            settingsUseCases.setMigrationComplete()
            _uiState.update { it.copy(isLoading = false, isDone = true) }
        }
    }
}

data class MigrationUiState(
    val isLoading: Boolean = false,
    @StringRes val error: Int? = null,
    val isDone: Boolean = false
)

sealed interface MigrationEvent {
    data object StartMigration : MigrationEvent
    data object SkipMigration : MigrationEvent
}
