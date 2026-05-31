package com.luna.budgetapp.presentation.screen.utils

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.luna.budgetapp.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


fun launchCredentialManager(
    context: Context,
    scope: CoroutineScope,
    onResult: (Result<Credential>) -> Unit
) {
    val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        .setFilterByAuthorizedAccounts(false)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    val credentialManager: CredentialManager = CredentialManager.create(context)

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            onResult(Result.success(result.credential))
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }

    }
}

