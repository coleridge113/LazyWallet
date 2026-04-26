package com.luna.budgetapp.presentation.screen.auth

data class UiState(
    val isLoading: Boolean = true,
    val error: String = "",
    val success: Boolean = false
) 

sealed interface Event {
    data object FetchToken : Event
    data object GotoAddExpenseRoute : Event
}

sealed class Navigation {
    data object GotoAddExpenseRoute : Navigation()
}
