package com.luna.budgetapp.presentation.screen.auth

import android.content.Context
import androidx.credentials.GetCredentialRequest
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.PasswordRule
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.theme.AuthUIAsset
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.common.io.Resources.getResource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.luna.budgetapp.BuildConfig
import com.luna.budgetapp.R
import com.luna.budgetapp.presentation.nav.Routes
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
fun AuthRoute(
    viewModel: AuthViewModel,
    navController: NavController
) {
    val lifecycleOwner = rememberLifecycleOwner()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentContext = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val configuration = remember(currentContext) {
        authUIConfiguration {
            context = currentContext
            logo = AuthUIAsset.Resource(R.drawable.ic_lazywallet_no_bg)
            providers {
                provider(AuthProvider.Email(
                    emailLinkActionCodeSettings = null,
                    passwordValidationRules = listOf(
                        PasswordRule.MinimumLength(8),
                        PasswordRule.RequireUppercase,
                        PasswordRule.RequireLowercase,
                        PasswordRule.RequireDigit,
                        PasswordRule.RequireSpecialCharacter
                    )
                ))
                provider(AuthProvider.Google(
                    scopes = listOf("profile", "email"),
                    serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
                ))
            }
        }
    }


    LaunchedEffect(Unit) {
        viewModel.navigation.collectLatest { navigation ->
                when (navigation) {
                    Navigation.GotoAddExpenseRoute -> {
                        navController.navigate(Routes.AddExpensesRoute) {
                            popUpTo(Routes.AuthRoute) { inclusive = true }
                        }
                    }
                    Navigation.GotoMigrationRoute -> {
                        navController.navigate(Routes.MigrationRoute) {
                            popUpTo(Routes.AuthRoute) { inclusive = true }
                        }
                    }
                }
            }
    }

    Scaffold { innerPadding ->
        AuthContent(
            context = currentContext,
            state = state,
            auth = auth,
            configuration = configuration,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(innerPadding),
            handleGoogleSignIn = {
                lifecycleOwner.lifecycleScope.launch {
                    launchCredentialManager(
                        context = currentContext,
                        auth = auth,
                        onSuccess = { viewModel.onEvent(Event.HandleSignInSuccess) }
                    )
                }
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
fun AuthContent(
    context: Context,
    state: UiState,
    auth: FirebaseAuth,
    handleGoogleSignIn: () -> Unit,
    configuration: AuthUIConfiguration,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    if (auth.currentUser != null) {
        LaunchedEffect(Unit) {
            onEvent(Event.HandleSignInSuccess)
        }
    } else {
        val usernameState = rememberTextFieldState()
        var passwordString by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                state = usernameState,
                label = { Text("Username") },
            )
            OutlinedTextField(
                value = passwordString,
                onValueChange = { passwordString = it },
                label = { Text("Password") },
                visualTransformation =
                    if (passwordVisible)
                        PasswordVisualTransformation()
                    else
                        VisualTransformation.None
            )

            Button(
                onClick = {
                    auth.signInWithEmailAndPassword(
                        usernameState.text.toString(),
                        passwordString
                    ).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            onEvent(Event.HandleSignInSuccess)
                        }
                    }
                },
                modifier = Modifier.padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Sign In"
                )
            }

            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Or continue with",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = handleGoogleSignIn
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.google_logo),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Sign in with Google"
                )
            }
        }
    }
}

private suspend fun launchCredentialManager(
    context: Context,
    auth: FirebaseAuth,
    onSuccess: () -> Unit
) {
    val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        .setFilterByAuthorizedAccounts(false)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    val credentialManager: CredentialManager = CredentialManager.create(context)

    try {
        val result = credentialManager.getCredential(context, request)
        handleSignIn(
            credential = result.credential,
            auth = auth,
            onSuccess = onSuccess
        )
    } catch(e: Exception) {
        Log.e("GoogleSignIn", "Error signing in: ${e.message}")
    }
}

private fun handleSignIn(
    credential: Credential,
    auth: FirebaseAuth,
    onSuccess: () -> Unit
) {
    if (credential is CustomCredential &&
        credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        Log.d("GoogleSignIn", "Processing credential")
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        firebaseAuthWithGoogle(
            idToken = googleIdTokenCredential.idToken,
            auth = auth,
            onSuccess = onSuccess
        )
    } else {
        Log.e("GoogleSignIn", "Credentials not recognized")
    }
}

private fun firebaseAuthWithGoogle(
    idToken: String,
    auth: FirebaseAuth,
    onSuccess: () -> Unit
) {
    val credential = GoogleAuthProvider.getCredential(idToken, null)
    auth.signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("GoogleSignIn", "Task Completed")
                onSuccess()
            } else {
                Log.e("GoogleSignIn", "Task did not complete")
            }
        }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Preview(
    showSystemUi = true,
    showBackground = true,
    device = Devices.PIXEL_7,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_NO
)
@Composable
fun LoginScreenPreview() {
    val context = LocalContext.current
    LazyWalletTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            AuthContent(
                context = context,
                state = UiState.Success(),
                auth = FirebaseAuth.getInstance(),
                configuration = authUIConfiguration {
                    this.context = context
                    providers {
                        provider(
                            AuthProvider.Email(
                                emailLinkActionCodeSettings = null,
                                passwordValidationRules = emptyList()
                            )
                        )
                    }
                },
                onEvent = {},
                modifier = Modifier,
                handleGoogleSignIn = {}
            )
        }
    }
}
