package com.luna.budgetapp.domain.usecase.auth

import com.luna.budgetapp.domain.repository.AuthRepository

class SignUpUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String) {
        repository.signUp(email, password)
    }
}
