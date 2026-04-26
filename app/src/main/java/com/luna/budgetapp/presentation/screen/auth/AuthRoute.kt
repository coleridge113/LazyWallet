package com.luna.budgetapp.presentation.screen.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen
import com.luna.budgetapp.presentation.nav.Routes
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AuthRoute(
    viewModel: AuthViewModel,
    navController: NavController
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
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
            state = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun AuthContent(
    state: UiState,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier
) {

    FirebaseAuthScreen()
}
