package com.luna.budgetapp.presentation.screen.auth

import androidx.credentials.Credential
import com.firebase.ui.auth.AuthState

sealed interface UiState {
    data object Loading : UiState
    data class Success(
        val authState: AuthState = AuthState.Idle
    ) : UiState
}

sealed interface Event {
    data object DismissDialog : Event
    data object HandleSignInSuccess : Event
    data class SignInGoogle(val credential: Credential) : Event
    data class SignInEmailPassword(val email: String, val password: String) : Event
    data class SignUp(val email: String, val password: String) : Event
}

sealed class Navigation {
    data object GotoAddExpenseRoute : Navigation()
    data object GotoMigrationRoute : Navigation()
}

sealed interface DialogState {
    data class ErrorMessage(val message: String?) : DialogState
}