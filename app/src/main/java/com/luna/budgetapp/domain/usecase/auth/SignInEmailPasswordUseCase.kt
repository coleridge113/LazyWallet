package com.luna.budgetapp.domain.usecase.auth

import com.luna.budgetapp.domain.repository.AuthRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult

class SignInEmailPasswordUseCase(
    private val repository: AuthRepository
) {
    operator suspend fun invoke(
        email: String,
        password: String,
        onSuccess: (Task<AuthResult>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        repository.signInEmailPassword(
            email = email,
            password = password,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }
}
