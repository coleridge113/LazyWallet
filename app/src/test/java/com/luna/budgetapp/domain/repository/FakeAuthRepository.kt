package com.luna.budgetapp.domain.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult

class FakeAuthRepository(var signedIn: Boolean = false) : AuthRepository {

    override suspend fun refreshJwtToken(): String = ""

    override suspend fun fetchJwtToken(): String? = null

    override suspend fun signUp(
        email: String,
        password: String,
        onSuccess: (Task<AuthResult>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = Unit

    override suspend fun signInEmailPassword(
        email: String,
        password: String,
        onSuccess: (Task<AuthResult>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = Unit

    override suspend fun signInGoogle(
        idToken: String,
        onSuccess: (Task<AuthResult>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = Unit

    val calls = mutableListOf<String>()
    var signOutError: Exception? = null

    override suspend fun signOut() {
        signOutError?.let { throw it }
        calls.add("signOut")
    }

    override fun isSignedIn(): Boolean = signedIn
}
