package com.luna.budgetapp.domain.usecase.auth

import com.luna.budgetapp.domain.repository.AuthRepository

class SignInGoogleUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(idToken: String) {
        repository.signInGoogle(idToken)
    }
}
