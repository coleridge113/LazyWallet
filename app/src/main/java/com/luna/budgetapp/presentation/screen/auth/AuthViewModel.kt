package com.luna.budgetapp.presentation.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.luna.budgetapp.domain.usecase.AuthUseCases
import com.luna.budgetapp.domain.usecase.SettingsUseCases
import com.luna.budgetapp.data.firebase.migration.DataMigrationRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.flow.update

class AuthViewModel(
    private val authUseCases: AuthUseCases,
    private val settingsUseCases: SettingsUseCases,
    private val migrationRepository: DataMigrationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState.Success())
    val uiState = _uiState.asStateFlow()

    private val _navigation = Channel<Navigation>()
    val navigation = _navigation.receiveAsFlow()

    private val _dialogState = MutableStateFlow<DialogState?>(null)
    val dialogState = _dialogState.asStateFlow()

    fun onEvent(event: Event) {
        when (event) {
            is Event.HandleSignInSuccess -> handleSignInSuccess()
            is Event.SignInGoogle -> signInGoogle(event.credential)
            is Event.SignInEmailPassword -> signInEmailPassword(event.email, event.password)
            is Event.SignUp -> signUp(event.email, event.password)
            is Event.DismissDialog -> dismissDialog()
        }
    }

    private fun handleSignInSuccess() {
        viewModelScope.launch {
            try {
                migrationRepository.syncFromCloud()
            } catch (_: Exception) { }
            _navigation.send(Navigation.GotoAddExpenseRoute)
        }
    }

    private fun signInGoogle(
        credential: Credential
    ) {
        viewModelScope.launch {
            if (credential is CustomCredential &&
                credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                authUseCases.signInGoogle(
                    idToken = googleIdTokenCredential.idToken,
                    onSuccess = { handleSignInSuccess() },
                    onFailure = { error ->
                        showError(error)
                    }
                )
            }
        }
    }

    private fun signInEmailPassword(
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            authUseCases.signInEmailPassword(
                email = email,
                password = password,
                onSuccess = { handleSignInSuccess() },
                onFailure = { error ->
                    showError(error)
                }
            )
        }
    }

    private fun signUp(
        email: String,
        password: String
    ) {
    }

    private fun showError(error: Exception) {
        _dialogState.update {
            DialogState.ErrorMessage(error.message)
        }
    }

    private fun dismissDialog() {
        _dialogState.update { null }
    }
}
