package com.luna.budgetapp.presentation.screen.auth

import android.util.Log
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

    val isUserSignedIn: Boolean = authUseCases.isSignedIn()

    private val _signedIn = MutableStateFlow(false)
    val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()

    private val _dialogState = MutableStateFlow<DialogState?>(null)
    val dialogState = _dialogState.asStateFlow()

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
        _signedIn.value = false
    }

    private fun handleSignInSuccess(isGuest: Boolean) {
        viewModelScope.launch { completeSignIn(isGuest) }
    }

    private suspend fun completeSignIn(isGuest: Boolean) {
        authUseCases.completeSignIn(isGuest)
        _signedIn.value = true
    }

    private fun signInGoogle(
        credential: Credential?
    ) {
        viewModelScope.launch {
            try {
                if (credential is CustomCredential &&
                    credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    authUseCases.signInGoogle(googleIdTokenCredential.idToken)
                    completeSignIn(false)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                showError(e)
            }
        }
    }

    private fun signInEmailPassword(
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            try {
                authUseCases.signInEmailPassword(email, password)
                completeSignIn(false)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                showError(e)
            }
        }
    }

    private fun signUp(
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            try {
                authUseCases.signUp(email, password)
                completeSignIn(false)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                showError(e)
            }
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

        _dialogState.update {
            DialogState.ErrorMessage(message)
        }
    }

    private fun dismissDialog() {
        _dialogState.update { null }
    }
}
