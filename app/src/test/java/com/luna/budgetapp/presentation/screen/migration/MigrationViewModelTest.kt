package com.luna.budgetapp.presentation.screen.migration

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MainDispatcherRule
import com.luna.budgetapp.R
import com.luna.budgetapp.data.firebase.migration.DataMigrationRepository
import com.luna.budgetapp.domain.usecase.SettingsUseCases
import com.luna.budgetapp.domain.usecase.settings.SetMigrationCompleteUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class MigrationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val migrationRepository = mockk<DataMigrationRepository>()
    private val setMigrationComplete = mockk<SetMigrationCompleteUseCase>()

    private fun createViewModel() = MigrationViewModel(
        migrationRepository = migrationRepository,
        settingsUseCases = SettingsUseCases(
            getActiveDateFilter = mockk(),
            setActiveDateFilter = mockk(),
            getMigrationStatus = mockk(),
            setMigrationComplete = setMigrationComplete
        )
    )

    @Test
    fun skip_setsDone() {
        coEvery { setMigrationComplete() } just Runs
        val vm = createViewModel()

        vm.onEvent(MigrationEvent.SkipMigration)
        assertThat(vm.uiState.value.isDone).isTrue()

        vm.onDoneHandled()
        assertThat(vm.uiState.value.isDone).isFalse()
    }

    @Test
    fun startMigration_success_setsDone() {
        coEvery { setMigrationComplete() } just Runs
        coEvery { migrationRepository.performMigration() } just Runs
        coEvery { migrationRepository.syncFromCloud() } just Runs
        val vm = createViewModel()

        vm.onEvent(MigrationEvent.StartMigration)

        assertThat(vm.uiState.value.isDone).isTrue()
        assertThat(vm.uiState.value.isLoading).isFalse()
    }

    @Test
    fun startMigration_failure_doesNotSetDone() {
        coEvery { migrationRepository.performMigration() } throws RuntimeException("offline")
        val vm = createViewModel()

        vm.onEvent(MigrationEvent.StartMigration)

        assertThat(vm.uiState.value.isDone).isFalse()
        assertThat(vm.uiState.value.error).isEqualTo(R.string.migration_error)
        assertThat(vm.uiState.value.isLoading).isFalse()
    }
}
