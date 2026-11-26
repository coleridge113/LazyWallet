package com.luna.budgetapp.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.domain.repository.FakeCloudSyncRepository
import com.luna.budgetapp.domain.repository.FakeSettingsRepository
import com.luna.budgetapp.domain.usecase.auth.CompleteSignInUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CompleteSignInUseCaseTest {

    private val cloudSync = FakeCloudSyncRepository()

    @Test
    fun setsGuestMode_afterSync() = runTest {
        val settings = FakeSettingsRepository()
        CompleteSignInUseCase(cloudSync, settings)(isGuest = true)

        assertThat(cloudSync.calls).containsExactly("download")
        assertThat(settings.isGuestFlow.first()).isTrue()
    }

    @Test
    fun syncFails_stillSetsGuestMode() = runTest {
        val settings = FakeSettingsRepository(initialGuest = true)
        cloudSync.downloadError = RuntimeException("boom")

        CompleteSignInUseCase(cloudSync, settings)(false)

        assertThat(settings.isGuestFlow.first()).isFalse()
    }

    @Test
    fun cancellation_propagates_andSkipsGuestMode() = runTest {
        val settings = FakeSettingsRepository(initialGuest = true)
        cloudSync.downloadError = CancellationException()

        val result = runCatching { CompleteSignInUseCase(cloudSync, settings)(false) }

        assertThat(result.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
        assertThat(settings.isGuestFlow.first()).isTrue()
    }
}
