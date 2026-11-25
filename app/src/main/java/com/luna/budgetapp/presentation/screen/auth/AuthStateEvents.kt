package com.luna.budgetapp.presentation.screen.auth

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
    data class ErrorMessage(val message: String?) : DialogState
}
