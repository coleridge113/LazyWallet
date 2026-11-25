package com.luna.budgetapp.presentation.screen.auth

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MainDispatcherRule
import com.luna.budgetapp.data.datastore.SettingsDataStore
import com.luna.budgetapp.data.firebase.migration.DataMigrationRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val migrationRepository = mockk<DataMigrationRepository>()
    private val settings = mockk<SettingsDataStore>()

    private fun createViewModel() = AuthViewModel(
        authUseCases = mockk(relaxed = true),
        migrationRepository = migrationRepository,
        settings = settings
    )

    @Test
    fun signInSuccess_setsSignedIn_andSavesGuestMode() = runTest {
        coEvery { migrationRepository.syncFromCloud() } just Runs
        coEvery { settings.setGuestMode(true) } just Runs
        val vm = createViewModel()
        assertThat(vm.signedIn.value).isFalse()

        vm.onEvent(Event.HandleSignInSuccess(isGuest = true))
        advanceUntilIdle()

        assertThat(vm.signedIn.value).isTrue()
        coVerify { settings.setGuestMode(true) }

        vm.onSignedInHandled()
        assertThat(vm.signedIn.value).isFalse()
    }

    @Test
    fun signInSuccess_whenSyncThrows_stillSetsSignedIn() = runTest {
        coEvery { migrationRepository.syncFromCloud() } throws RuntimeException("boom")
        coEvery { settings.setGuestMode(any()) } just Runs
        val vm = createViewModel()

        vm.onEvent(Event.HandleSignInSuccess(isGuest = false))
        advanceUntilIdle()

        assertThat(vm.signedIn.value).isTrue()
        coVerify { settings.setGuestMode(false) }
    }
}
