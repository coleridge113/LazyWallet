package com.luna.budgetapp.data.local.repository

import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.luna.budgetapp.domain.model.AuthException
import kotlinx.coroutines.CancellationException
import org.junit.Test

class AuthErrorMapperTest {

    @Test
    fun invalidEmail() {
        val e = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_EMAIL", "bad")
        assertThat(e.toAuthException()).isEqualTo(AuthException.InvalidEmail)
    }

    @Test
    fun invalidCredential() {
        listOf("ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD").forEach {
            val e = FirebaseAuthInvalidCredentialsException(it, "bad")
            assertThat(e.toAuthException()).isEqualTo(AuthException.InvalidCredentials)
        }
    }

    @Test
    fun userNotFound() {
        val e = FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "none")
        assertThat(e.toAuthException()).isEqualTo(AuthException.UserNotFound)
    }

    @Test
    fun userDisabled() {
        val e = FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "off")
        assertThat(e.toAuthException()).isInstanceOf(AuthException.Unknown::class.java)
    }

    @Test
    fun weakPassword() {
        val e = FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "weak", "too short")
        assertThat(e.toAuthException()).isInstanceOf(AuthException.Unknown::class.java)
    }

    @Test
    fun cancellation_passesThrough() {
        val e = CancellationException("cancelled")
        assertThat(e.toAuthException()).isSameInstanceAs(e)
    }

    @Test
    fun illegalArgument_passesThrough() {
        val e = IllegalArgumentException("empty")
        assertThat(e.toAuthException()).isSameInstanceAs(e)
    }

    @Test
    fun other() {
        val e = RuntimeException("x")
        val mapped = e.toAuthException()
        assertThat(mapped).isInstanceOf(AuthException.Unknown::class.java)
        assertThat(mapped.cause).isSameInstanceAs(e)
    }
}
