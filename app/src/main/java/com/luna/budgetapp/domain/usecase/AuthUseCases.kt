package com.luna.budgetapp.domain.usecase

import com.luna.budgetapp.domain.usecase.auth.GetTokenUseCase
import com.luna.budgetapp.domain.usecase.auth.SignInGoogleUseCase
import com.luna.budgetapp.domain.usecase.auth.SignInEmailPasswordUseCase
import com.luna.budgetapp.domain.usecase.auth.SignUpUseCase

data class AuthUseCases(
    val getToken: GetTokenUseCase,
    val signInGoogle: SignInGoogleUseCase,
    val signInEmailPassword: SignInEmailPasswordUseCase,
    val signUp: SignUpUseCase
)
