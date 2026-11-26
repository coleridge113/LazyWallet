package com.luna.budgetapp.presentation.screen.auth

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MainDispatcherRule
import com.luna.budgetapp.R
import com.luna.budgetapp.domain.model.AuthException
import com.luna.budgetapp.domain.repository.FakeAuthRepository
import com.luna.budgetapp.domain.repository.FakeCloudSyncRepository
import com.luna.budgetapp.domain.repository.FakeLocalDataRepository
import com.luna.budgetapp.domain.repository.FakeSettingsRepository
import com.luna.budgetapp.domain.usecase.AuthUseCases
import com.luna.budgetapp.domain.usecase.auth.CompleteSignInUseCase
import com.luna.budgetapp.domain.usecase.auth.GetTokenUseCase
import com.luna.budgetapp.domain.usecase.auth.IsSignedInUseCase
import com.luna.budgetapp.domain.usecase.auth.SignInEmailPasswordUseCase
import com.luna.budgetapp.domain.usecase.auth.SignInGoogleUseCase
import com.luna.budgetapp.domain.usecase.auth.SignOutUseCase
import com.luna.budgetapp.domain.usecase.auth.SignUpUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepo = FakeAuthRepository()
    private val cloudSync = FakeCloudSyncRepository()
    private val settings = FakeSettingsRepository(initialGuest = true)

    private fun createViewModel() = AuthViewModel(
        AuthUseCases(
            getToken = GetTokenUseCase(authRepo),
            signInGoogle = SignInGoogleUseCase(authRepo),
            signInEmailPassword = SignInEmailPasswordUseCase(authRepo),
            signUp = SignUpUseCase(authRepo),
            signOut = SignOutUseCase(authRepo, settings, FakeLocalDataRepository()),
            isSignedIn = IsSignedInUseCase(authRepo),
            completeSignIn = CompleteSignInUseCase(cloudSync, settings)
        )
    )

    private fun errorDialog(res: Int) = DialogState.ErrorMessage(res)

    @Test
    fun signInSuccess_setsSignedIn_andSavesGuestMode() = runTest {
        val vm = createViewModel()
        assertThat(vm.uiState.value.signedIn).isFalse()

        vm.onEvent(Event.HandleSignInSuccess(isGuest = true))
        advanceUntilIdle()

        assertThat(vm.uiState.value.signedIn).isTrue()
        assertThat(settings.isGuestFlow.first()).isTrue()

        vm.onSignedInHandled()
        assertThat(vm.uiState.value.signedIn).isFalse()
    }

    @Test
    fun signInSuccess_whenSyncThrows_stillSetsSignedIn() = runTest {
        cloudSync.downloadError = RuntimeException("boom")
        val vm = createViewModel()

        vm.onEvent(Event.HandleSignInSuccess(isGuest = false))
        advanceUntilIdle()

        assertThat(vm.uiState.value.signedIn).isTrue()
        assertThat(settings.isGuestFlow.first()).isFalse()
    }

    @Test
    fun emailSignIn_success_setsSignedIn() = runTest {
        val vm = createViewModel()

        vm.onEvent(Event.SignInEmailPassword("a@b.c", "pw"))
        advanceUntilIdle()

        assertThat(authRepo.calls).containsExactly("signInEmail")
        assertThat(vm.uiState.value.signedIn).isTrue()
        assertThat(settings.isGuestFlow.first()).isFalse()
    }

    @Test
    fun emailSignIn_invalidCredentials_showsInvalidCredentialsDialog() = runTest {
        authRepo.error = AuthException.InvalidCredentials
        val vm = createViewModel()

        vm.onEvent(Event.SignInEmailPassword("a@b.c", "pw"))
        advanceUntilIdle()

        assertThat(vm.uiState.value.dialog)
            .isEqualTo(errorDialog(R.string.auth_error_invalid_credentials))
        assertThat(vm.uiState.value.signedIn).isFalse()
    }

    @Test
    fun emailSignIn_userNotFound_showsUserNotFound() = runTest {
        authRepo.error = AuthException.UserNotFound
        val vm = createViewModel()

        vm.onEvent(Event.SignInEmailPassword("a@b.c", "pw"))
        advanceUntilIdle()

        assertThat(vm.uiState.value.dialog).isEqualTo(errorDialog(R.string.auth_error_user_not_found))
    }

    @Test
    fun emailSignIn_invalidEmail_showsInvalidEmail() = runTest {
        authRepo.error = AuthException.InvalidEmail
        val vm = createViewModel()

        vm.onEvent(Event.SignInEmailPassword("bad", "pw"))
        advanceUntilIdle()

        assertThat(vm.uiState.value.dialog).isEqualTo(errorDialog(R.string.auth_error_invalid_email))
    }

    @Test
    fun signUp_unknownError_showsGeneric() = runTest {
        authRepo.error = AuthException.Unknown(null)
        val vm = createViewModel()

        vm.onEvent(Event.SignUp("a@b.c", "pw"))
        advanceUntilIdle()

        assertThat(vm.uiState.value.dialog).isEqualTo(errorDialog(R.string.auth_error_generic))
    }

    @Test
    fun emailSignIn_illegalArgument_showsInvalidInput() = runTest {
        authRepo.error = IllegalArgumentException("empty")
        val vm = createViewModel()

        vm.onEvent(Event.SignInEmailPassword("", ""))
        advanceUntilIdle()

        assertThat(vm.uiState.value.dialog).isEqualTo(errorDialog(R.string.auth_error_invalid_input))
    }

    @Test
    fun isUserSignedIn_readOnceAtConstruction() {
        val repo = FakeAuthRepository(signedIn = true)
        val useCases = AuthUseCases(
            getToken = GetTokenUseCase(repo),
            signInGoogle = SignInGoogleUseCase(repo),
            signInEmailPassword = SignInEmailPasswordUseCase(repo),
            signUp = SignUpUseCase(repo),
            signOut = SignOutUseCase(repo, settings, FakeLocalDataRepository()),
            isSignedIn = IsSignedInUseCase(repo),
            completeSignIn = CompleteSignInUseCase(cloudSync, settings)
        )
        val vm = AuthViewModel(useCases)
        repo.signedIn = false

        assertThat(vm.uiState.value.isUserSignedIn).isTrue()
    }

    @Test
    fun signIn_isLoadingWhileInFlight() = runTest {
        authRepo.gate = CompletableDeferred()
        val vm = createViewModel()

        vm.onEvent(Event.SignInEmailPassword("a@b.c", "pw"))
        advanceUntilIdle()
        assertThat(vm.uiState.value.isLoading).isTrue()
        assertThat(vm.uiState.value.signedIn).isFalse()

        authRepo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertThat(vm.uiState.value.isLoading).isTrue()
        assertThat(vm.uiState.value.signedIn).isTrue()
    }

    @Test
    fun signIn_error_clearsLoading_andShowsDialog() = runTest {
        authRepo.error = AuthException.InvalidCredentials
        val vm = createViewModel()

        vm.onEvent(Event.SignInEmailPassword("a@b.c", "pw"))
        advanceUntilIdle()

        assertThat(vm.uiState.value.isLoading).isFalse()
        assertThat(vm.uiState.value.dialog)
            .isEqualTo(errorDialog(R.string.auth_error_invalid_credentials))
    }

    @Test
    fun secondSignIn_whileLoading_makesNoSecondCall() = runTest {
        authRepo.gate = CompletableDeferred()
        val vm = createViewModel()

        vm.onEvent(Event.SignInEmailPassword("a@b.c", "pw"))
        advanceUntilIdle()
        vm.onEvent(Event.SignInEmailPassword("a@b.c", "pw"))
        vm.onEvent(Event.SignUp("a@b.c", "pw"))
        vm.onEvent(Event.HandleSignInSuccess(isGuest = true))
        advanceUntilIdle()

        assertThat(authRepo.calls).containsExactly("signInEmail")
    }

    @Test
    fun onSignedInHandled_clearsSignedInAndLoading() = runTest {
        val vm = createViewModel()
        vm.onEvent(Event.SignInEmailPassword("a@b.c", "pw"))
        advanceUntilIdle()
        assertThat(vm.uiState.value.isLoading).isTrue()

        vm.onSignedInHandled()

        assertThat(vm.uiState.value.signedIn).isFalse()
        assertThat(vm.uiState.value.isLoading).isFalse()
    }
}
