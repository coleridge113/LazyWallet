package com.luna.budgetapp.presentation.screen.auth
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen
import com.luna.budgetapp.presentation.nav.Routes
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AuthRoute(
    viewModel: AuthViewModel,
    navController: NavController
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentContext = LocalContext.current
    val authUI = FirebaseAuthUI.getInstance()
    val configuration = remember(currentContext) {
        authUIConfiguration {
            context = currentContext
            providers {
                provider(AuthProvider.Google(
                    scopes = listOf("profile", "email"),
                    serverClientId = null
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
                }
            }
    }

    Scaffold { innerPadding ->
        AuthContent(
            context = currentContext,
            state = state,
            authUI = authUI,
            configuration = configuration,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun AuthContent(
    context: Context,
    state: UiState,
    authUI: FirebaseAuthUI,
    configuration: AuthUIConfiguration,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    if (authUI.isSignedIn()) {
        onEvent(Event.GotoAddExpenseRoute)
    } else {
        FirebaseAuthScreen(
            modifier = modifier.wrapContentSize(),
            configuration = configuration,
            onSignInSuccess = {
                Toast.makeText(
                    context,
                    "Signed in successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d("AuthRoute", "Signed in successfully!")
            },
            onSignInFailure = {
                Toast.makeText(
                    context,
                    "Failed to sign in...",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onSignInCancelled = {},
            authenticatedContent = { _, _ ->
                onEvent(Event.GotoAddExpenseRoute)
            }
        )
    }
}
