package com.luna.budgetapp.domain.usecase

import com.luna.budgetapp.domain.usecase.auth.GetTokenUseCase
import com.luna.budgetapp.domain.usecase.auth.SignInGoogleUseCase

data class AuthUseCases(
    val getToken: GetTokenUseCase,
    val signInGoogle: SignInGoogleUseCase,
)
