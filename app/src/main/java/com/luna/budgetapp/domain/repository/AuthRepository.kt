package com.luna.budgetapp.domain.repository

interface AuthRepository {

    suspend fun refreshJwtToken(): String 

    suspend fun fetchJwtToken(): String?

    suspend fun signUp(email: String, password: String)

    suspend fun signInEmailPassword(email: String, password: String)

    suspend fun signInGoogle(idToken: String)

    suspend fun signOut()

    fun isSignedIn(): Boolean
}
