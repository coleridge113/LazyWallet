package com.luna.budgetapp.presentation.screen.analysis

data class UiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface Event {

}

sealed interface Navigation {}


