package com.luna.budgetapp.domain.repository

class FakeAuthRepository(var signedIn: Boolean = false) : AuthRepository {

    override suspend fun refreshJwtToken(): String = ""

    override suspend fun fetchJwtToken(): String? = null

    var error: Exception? = null

    override suspend fun signUp(email: String, password: String) {
        calls.add("signUp")
        error?.let { throw it }
    }

    override suspend fun signInEmailPassword(email: String, password: String) {
        calls.add("signInEmail")
        error?.let { throw it }
    }

    override suspend fun signInGoogle(idToken: String) {
        calls.add("signInGoogle")
        error?.let { throw it }
    }

    val calls = mutableListOf<String>()
    var signOutError: Exception? = null

    override suspend fun signOut() {
        signOutError?.let { throw it }
        calls.add("signOut")
    }

    override fun isSignedIn(): Boolean = signedIn
}
