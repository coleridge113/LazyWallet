package com.luna.budgetapp.presentation.screen.auth

import com.luna.budgetapp.R
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.luna.budgetapp.domain.usecase.AuthUseCases
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.luna.budgetapp.domain.model.AuthException
import kotlinx.coroutines.flow.update

class AuthViewModel(
    private val authUseCases: AuthUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(isUserSignedIn = authUseCases.isSignedIn())
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEvent(event: Event) {
        when (event) {
            is Event.HandleSignInSuccess -> handleSignInSuccess(event.isGuest)
            is Event.SignInGoogle -> signInGoogle(event.credential)
            is Event.SignInEmailPassword -> signInEmailPassword(event.email, event.password)
            is Event.SignUp -> signUp(event.email, event.password)
            is Event.DismissDialog -> dismissDialog()
            is Event.HandleError -> showError(event.error)
        }
    }

    fun onSignedInHandled() {
        _uiState.update { it.copy(signedIn = false, isLoading = false) }
    }

    /** Returns false (and does nothing) if a sign-in is already in flight. */
    private fun startLoading(): Boolean {
        var started = false
        _uiState.update {
            if (it.isLoading) it else {
                started = true
                it.copy(isLoading = true)
            }
        }
        return started
    }

    private fun handleSignInSuccess(isGuest: Boolean) {
        if (!startLoading()) return
        viewModelScope.launch { completeSignIn(isGuest) }
    }

    private suspend fun completeSignIn(isGuest: Boolean) {
        try {
            authUseCases.completeSignIn(isGuest)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        _uiState.update { it.copy(signedIn = true) }
    }

    private fun signInGoogle(
        credential: Credential?
    ) {
        if (credential !is CustomCredential ||
            credential.type != TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) return
        if (!startLoading()) return
        viewModelScope.launch {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                authUseCases.signInGoogle(googleIdTokenCredential.idToken)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError(e)
                return@launch
            }
            completeSignIn(false)
        }
    }

    private fun signInEmailPassword(
        email: String,
        password: String
    ) = launchSignIn { authUseCases.signInEmailPassword(email, password) }

    private fun signUp(
        email: String,
        password: String
    ) = launchSignIn { authUseCases.signUp(email, password) }

    private fun launchSignIn(signIn: suspend () -> Unit) {
        if (!startLoading()) return
        viewModelScope.launch {
            try {
                signIn()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError(e)
                return@launch
            }
            completeSignIn(false)
        }
    }

    private fun showError(error: Exception) {
        val message =
            when (error) {
                is NoCredentialException ->
                    R.string.auth_error_no_google_account

                is IllegalArgumentException ->
                    R.string.auth_error_invalid_input

                is AuthException.InvalidEmail ->
                    R.string.auth_error_invalid_email

                is AuthException.UserNotFound ->
                    R.string.auth_error_user_not_found

                is AuthException.InvalidCredentials ->
                    R.string.auth_error_invalid_credentials

                else -> R.string.auth_error_generic
            }

        _uiState.update {
            it.copy(isLoading = false, dialog = DialogState.ErrorMessage(message))
        }
    }

    private fun dismissDialog() {
        _uiState.update { it.copy(dialog = null) }
    }
}
