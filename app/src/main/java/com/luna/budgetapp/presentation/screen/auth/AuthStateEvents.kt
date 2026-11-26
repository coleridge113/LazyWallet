package com.luna.budgetapp.presentation.screen.auth

import androidx.annotation.StringRes
import androidx.credentials.Credential

sealed interface Event {
    data object DismissDialog : Event
    data class HandleSignInSuccess(val isGuest: Boolean) : Event
    data class SignInGoogle(val credential: Credential?) : Event
    data class SignInEmailPassword(val email: String, val password: String) : Event
    data class SignUp(val email: String, val password: String) : Event
    data class HandleError(val error: Exception) : Event
}

sealed interface DialogState {
    data class ErrorMessage(@StringRes val messageRes: Int) : DialogState
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val dialog: DialogState? = null,
    val isUserSignedIn: Boolean = false,
    val signedIn: Boolean = false
)
