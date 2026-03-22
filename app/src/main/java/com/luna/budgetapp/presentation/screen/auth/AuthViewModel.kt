package com.luna.budgetapp.presentation.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.domain.usecase.AuthUseCases
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

class AuthViewModel(
    private val authUseCases: AuthUseCases
) : ViewModel() {

    init {
        fetchToken()
    }

    private val _state = MutableStateFlow(ViewModelStateEvents.UiState())
    val state = _state.asStateFlow()

    private val _navigation = Channel<ViewModelStateEvents.Navigation>()
    val navigation = _navigation.receiveAsFlow()

    fun onEvent(event: ViewModelStateEvents.Event) {
        when (event) {
            ViewModelStateEvents.Event.FetchToken -> { fetchToken() }
            ViewModelStateEvents.Event.GotoAddExpenseRoute -> { gotoAddExpenseRoute() }
        }
    }

    private fun fetchToken() {
        viewModelScope.launch {
            try {
                authUseCases.getToken()
                _state.update { curr ->
                    curr.copy(
                        isLoading = false,
                        success = true
                    )
                }
            } catch (e: IllegalStateException) {
               _state.update { curr ->
                   curr.copy(
                       isLoading = false,
                       error = e.message ?: "Unknown error occurred...",
                       success = false
                   )
               } 
            }
        }
    }

    private fun gotoAddExpenseRoute() {
        viewModelScope.launch {
            _navigation.send(ViewModelStateEvents.Navigation.GotoAddExpenseRoute)
        }
    }
}

object ViewModelStateEvents {
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
}
