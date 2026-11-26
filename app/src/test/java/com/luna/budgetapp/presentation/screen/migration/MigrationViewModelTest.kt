package com.luna.budgetapp.presentation.screen.migration

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MainDispatcherRule
import com.luna.budgetapp.R
import com.luna.budgetapp.domain.repository.FakeCloudSyncRepository
import com.luna.budgetapp.domain.repository.FakeSettingsRepository
import com.luna.budgetapp.domain.usecase.SettingsUseCases
import com.luna.budgetapp.domain.usecase.SyncUseCases
import com.luna.budgetapp.domain.usecase.settings.GetActiveDateFilterUseCase
import com.luna.budgetapp.domain.usecase.settings.GetMigrationStatusUseCase
import com.luna.budgetapp.domain.usecase.settings.SetActiveDateFilterUseCase
import com.luna.budgetapp.domain.usecase.settings.SetMigrationCompleteUseCase
import com.luna.budgetapp.domain.usecase.sync.MigrateLocalDataUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class MigrationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val cloudSync = FakeCloudSyncRepository()
    private val settings = FakeSettingsRepository()

    private fun createViewModel() = MigrationViewModel(
        syncUseCases = SyncUseCases(MigrateLocalDataUseCase(cloudSync, settings)),
        settingsUseCases = SettingsUseCases(
            getActiveDateFilter = GetActiveDateFilterUseCase(settings),
            setActiveDateFilter = SetActiveDateFilterUseCase(settings),
            getMigrationStatus = GetMigrationStatusUseCase(settings),
            setMigrationComplete = SetMigrationCompleteUseCase(settings)
        )
    )

    @Test
    fun skip_setsDone() = runTest {
        val vm = createViewModel()

        vm.onEvent(MigrationEvent.SkipMigration)
        assertThat(vm.uiState.value.isDone).isTrue()
        assertThat(settings.isMigratedFlow.first()).isTrue()

        vm.onDoneHandled()
        assertThat(vm.uiState.value.isDone).isFalse()
    }

    @Test
    fun startMigration_success_setsDone() = runTest {
        val vm = createViewModel()

        vm.onEvent(MigrationEvent.StartMigration)

        assertThat(vm.uiState.value.isDone).isTrue()
        assertThat(vm.uiState.value.isLoading).isFalse()
        assertThat(settings.isMigratedFlow.first()).isTrue()
    }

    @Test
    fun startMigration_failure_doesNotSetDone() = runTest {
        cloudSync.uploadError = RuntimeException("offline")
        val vm = createViewModel()

        vm.onEvent(MigrationEvent.StartMigration)

        assertThat(vm.uiState.value.isDone).isFalse()
        assertThat(vm.uiState.value.error).isEqualTo(R.string.migration_error)
        assertThat(vm.uiState.value.isLoading).isFalse()
        assertThat(settings.isMigratedFlow.first()).isFalse()
    }
}
