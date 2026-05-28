package com.luna.budgetapp.domain.usecase.auth

import com.luna.budgetapp.domain.repository.AuthRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult

class SignInGoogleUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        idToken: String,
        onSuccess: (Task<AuthResult>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        repository.signInGoogle(
            idToken = idToken,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }
}
