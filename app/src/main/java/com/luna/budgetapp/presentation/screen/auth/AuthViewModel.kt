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

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private val _navigation = Channel<Navigation>()
    val navigation = _navigation.receiveAsFlow()

    fun onEvent(event: Event) {
        when (event) {
            Event.FetchToken -> { fetchToken() }
            Event.GotoAddExpenseRoute -> { gotoAddExpenseRoute() }
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
            _navigation.send(Navigation.GotoAddExpenseRoute)
        }
    }
}
