package com.luna.budgetapp.presentation.screen.migration

import androidx.compose.ui.res.stringResource
import com.luna.budgetapp.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.luna.budgetapp.presentation.nav.Routes
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MigrationRoute(
    viewModel: MigrationViewModel,
    navController: NavController
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigation.collectLatest { navigation ->
            when (navigation) {
                MigrationNavigation.GotoAddExpenseRoute -> {
                    navController.navigate(Routes.ExpensePresetRoute) {
                        popUpTo(Routes.AuthRoute) { inclusive = true }
                    }
                }
            }
        }
    }

    MigrationScreen(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun MigrationScreen(
    state: MigrationUiState,
    onEvent: (MigrationEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.migration_title),
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.migration_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (state.isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stringResource(R.string.migration_in_progress))
        } else {
            Button(
                onClick = { onEvent(MigrationEvent.StartMigration) }
            ) {
                Text(text = stringResource(R.string.migration_start))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = { onEvent(MigrationEvent.SkipMigration) }
            ) {
                Text(text = stringResource(R.string.migration_skip))
            }
        }

        state.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
