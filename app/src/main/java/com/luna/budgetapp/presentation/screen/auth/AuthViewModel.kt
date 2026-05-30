package com.luna.budgetapp.presentation.screen.auth

import android.util.Log
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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
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
                try {
                    authUseCases.signInGoogle(
                        idToken = googleIdTokenCredential.idToken,
                        onSuccess = { task ->
                            if (task.isSuccessful) {
                                handleSignInSuccess()
                            }
                        },
                        onFailure = { error ->
                            showError(error)
                        }
                    )
                } catch (e: Exception) {
                    showError(e)
                }
            }
        }
    }

    private fun signInEmailPassword(
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            try {
                authUseCases.signInEmailPassword(
                    email = email,
                    password = password,
                    onSuccess = { task ->
                        if (task.isSuccessful) {
                            handleSignInSuccess()
                        }
                    },
                    onFailure = { error ->
                        showError(error)
                    }
                )
            } catch (e: Exception) {
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
                authUseCases.signUp(
                    email = email,
                    password = password,
                    onSuccess = { task ->
                        if (task.isSuccessful) {
                            handleSignInSuccess()
                        }
                    },
                    onFailure = { error ->
                        showError(error)
                    }
                )
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    private fun showError(error: Exception) {
        val message =
            when (error) {
                is IllegalArgumentException ->
                    "Please enter a valid email or password."

                is FirebaseAuthInvalidCredentialsException ->
                    resolveInvalidAuthMessage(error.errorCode)

                else -> "Something went wrong."
            }

        _dialogState.update {
            DialogState.ErrorMessage(message)
        }
    }

    private fun dismissDialog() {
        _dialogState.update { null }
    }

    private fun resolveInvalidAuthMessage(errorCode: String): String {
        return when (errorCode) {
            "ERROR_INVALID_EMAIL" -> "Invalid email format."
            "ERROR_USER_NOT_FUND" -> "User not found."
            else -> "Invalid credentials"
        }
    }
}
