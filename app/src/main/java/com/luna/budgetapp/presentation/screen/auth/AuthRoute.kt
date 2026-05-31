package com.luna.budgetapp.presentation.screen.auth

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.luna.budgetapp.R
import com.luna.budgetapp.presentation.nav.Routes
import com.luna.budgetapp.presentation.screen.components.ErrorDialog
import com.luna.budgetapp.presentation.screen.components.PrimaryButton
import com.luna.budgetapp.presentation.screen.components.SecondaryButton
import com.luna.budgetapp.presentation.screen.utils.launchCredentialManager
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import kotlinx.coroutines.flow.collectLatest

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
fun AuthRoute(
    viewModel: AuthViewModel,
    navController: NavController
) {
    val lifecycleOwner = rememberLifecycleOwner()
    val currentContext = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val dialog by viewModel.dialogState.collectAsStateWithLifecycle()

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
            isUserSignedIn = auth.currentUser != null,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(innerPadding),
            dialog = dialog,
            handleGoogleSignIn = {
                launchCredentialManager(
                    context = currentContext,
                    scope = lifecycleOwner.lifecycleScope
                ) { credential ->
                    viewModel.onEvent(Event.SignInGoogle(credential))
                }
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
fun AuthContent(
    isUserSignedIn: Boolean,
    onEvent: (Event) -> Unit,
    dialog: DialogState?,
    handleGoogleSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (val dialog = dialog) {
        is DialogState.ErrorMessage -> ErrorDialog(dialog.message) {
            onEvent(Event.DismissDialog)
        }
        null -> {}
    }
    if (isUserSignedIn) {
        LaunchedEffect(Unit) {
            onEvent(Event.HandleSignInSuccess)
        }
    } else {
        val emailState = rememberTextFieldState()
        var passwordString by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                state = emailState,
                label = { Text("Email") },
            )
            OutlinedTextField(
                value = passwordString,
                onValueChange = { passwordString = it },
                label = { Text("Password") },
                trailingIcon = {
                    Icon(
                        imageVector =
                            if (passwordVisible)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.clickable {
                            passwordVisible = !passwordVisible
                        }
                    )
                },
                visualTransformation =
                    if (passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation()
            )

            Row {
                SecondaryButton(
                    onClick = {
                        onEvent(
                            Event.SignUp(
                                email = emailState.text.toString().trim(),
                                password = passwordString.trim()
                            )
                        )
                    },
                    text = "Sign Up",
                    modifier = Modifier.padding(16.dp)
                )
                PrimaryButton(
                    onClick = {
                        onEvent(
                            Event.SignInEmailPassword(
                                email = emailState.text.toString().trim(),
                                password = passwordString.trim()
                            )
                        )
                    },
                    text = "Sign In",
                    modifier = Modifier.padding(16.dp)
                )
            }

            Row(
                modifier = Modifier.padding(
                    horizontal = 32.dp,
                    vertical = 16.dp
                ),
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

            Column(
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Button(
                    onClick = handleGoogleSignIn
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.google_logo),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Sign in with Google"
                    )
                }
            }
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
fun LoginScreenPreviewLight() {
    LazyWalletTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            AuthContent(
                isUserSignedIn = false,
                onEvent = {},
                modifier = Modifier,
                dialog = null,
                handleGoogleSignIn = {}
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Preview(
    showSystemUi = true,
    showBackground = true,
    device = Devices.PIXEL_7,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
fun LoginScreenPreviewDark() {
    LazyWalletTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            AuthContent(
                isUserSignedIn = false,
                onEvent = {},
                modifier = Modifier,
                dialog = null,
                handleGoogleSignIn = {}
            )
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
fun LoginScreenPreviewDialog() {
    LazyWalletTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            val dialog = DialogState.ErrorMessage("Something went wrong")
            AuthContent(
                isUserSignedIn = false,
                onEvent = {},
                modifier = Modifier,
                dialog = dialog,
                handleGoogleSignIn = {}
            )
        }
    }
}
