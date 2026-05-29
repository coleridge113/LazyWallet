package com.luna.budgetapp.domain.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult

interface AuthRepository {

    suspend fun refreshJwtToken(): String 

    suspend fun fetchJwtToken(): String?

    suspend fun signInEmailPassword(
        email: String,
        password: String,
        onSuccess: (Task<AuthResult>) -> Unit,
        onFailure: (Exception) -> Unit
    )

    suspend fun signInGoogle(
        idToken: String,
        onSuccess: (Task<AuthResult>) -> Unit,
        onFailure: (Exception) -> Unit
    )
}
