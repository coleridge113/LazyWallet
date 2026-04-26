package com.luna.budgetapp.presentation.screen.auth

import com.firebase.ui.auth.AuthState

sealed interface UiState {
    data object Loading : UiState
    data class Error(val message: String) : UiState
    data class Success(
        val authState: AuthState = AuthState.Idle
    ) : UiState
}

sealed interface Event {
    data object GotoAddExpenseRoute : Event
}

sealed class Navigation {
    data object GotoAddExpenseRoute : Navigation()
}
