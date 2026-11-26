package com.luna.budgetapp.domain.model

sealed class AuthException : Exception() {
    data object InvalidEmail : AuthException()
    data object UserNotFound : AuthException()
    data object InvalidCredentials : AuthException()
    class Unknown(override val cause: Throwable?) : AuthException()
}
