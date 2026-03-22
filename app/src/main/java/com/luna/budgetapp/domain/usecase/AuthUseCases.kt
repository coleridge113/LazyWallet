package com.luna.budgetapp.domain.usecase

import com.luna.budgetapp.domain.usecase.auth.GetTokenUseCase

data class AuthUseCases(
    val getToken: GetTokenUseCase,
)
