package com.luna.budgetapp.domain.usecase.auth

import com.luna.budgetapp.domain.repository.AuthRepository

class IsSignedInUseCase(
    private val repository: AuthRepository
) {
    operator fun invoke(): Boolean = repository.isSignedIn()
}
