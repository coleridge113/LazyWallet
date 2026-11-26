package com.luna.budgetapp.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.domain.repository.FakeAuthRepository
import com.luna.budgetapp.domain.repository.FakeLocalDataRepository
import com.luna.budgetapp.domain.repository.FakeSettingsRepository
import com.luna.budgetapp.domain.usecase.auth.SignOutUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SignOutUseCaseTest {

    private val authFake = FakeAuthRepository()
    private val localFake = FakeLocalDataRepository()

    @Test
    fun accountUser_signsOutThenClearsLocalData() = runTest {
        SignOutUseCase(authFake, FakeSettingsRepository(initialGuest = false), localFake)()

        assertThat(authFake.calls).containsExactly("signOut")
        assertThat(localFake.clearCount).isEqualTo(1)
    }

    @Test
    fun guest_signsOut_keepsLocalData() = runTest {
        SignOutUseCase(authFake, FakeSettingsRepository(initialGuest = true), localFake)()

        assertThat(authFake.calls).containsExactly("signOut")
        assertThat(localFake.clearCount).isEqualTo(0)
    }

    @Test
    fun signOutFails_doesNotClear() = runTest {
        authFake.signOutError = RuntimeException("boom")
        val useCase = SignOutUseCase(authFake, FakeSettingsRepository(initialGuest = false), localFake)

        try {
            useCase()
            throw AssertionError("expected exception")
        } catch (e: RuntimeException) {
            assertThat(e.message).isEqualTo("boom")
        }
        assertThat(localFake.clearCount).isEqualTo(0)
    }
}
