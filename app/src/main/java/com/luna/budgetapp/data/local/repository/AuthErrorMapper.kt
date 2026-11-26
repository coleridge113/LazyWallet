package com.luna.budgetapp.data.local.repository

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.luna.budgetapp.domain.model.AuthException
import kotlinx.coroutines.CancellationException

/**
 * Maps a Firebase failure to a domain exception. Cancellation and
 * IllegalArgumentException (empty input) come back unchanged.
 */
internal fun Exception.toAuthException(): Exception = when (this) {
    is CancellationException -> this
    is IllegalArgumentException -> this
    is FirebaseAuthWeakPasswordException -> AuthException.Unknown(this)
    is FirebaseAuthInvalidCredentialsException ->
        if (errorCode == "ERROR_INVALID_EMAIL") AuthException.InvalidEmail
        else AuthException.InvalidCredentials
    is FirebaseAuthInvalidUserException ->
        if (errorCode == "ERROR_USER_NOT_FOUND") AuthException.UserNotFound
        else AuthException.Unknown(this)
    else -> AuthException.Unknown(this)
}
