package com.luna.budgetapp.presentation.screen.budgetdetails

import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.luna.budgetapp.presentation.screen.budgetdetails.components.BudgetDetailsCard
import com.luna.budgetapp.presentation.screen.components.ExpenseTable
import com.luna.budgetapp.presentation.screen.utils.singleClick
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailsRoute(
    navController: NavController,
    viewModel: BudgetDetailsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        UiState.Loading -> {}
        is UiState.Error -> {}
        is UiState.Success -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    navController.navigateUp()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null
                                )
                            }
                        },
                        title = {
                            Row (
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = state.budget.name)
                                Text(
                                    text = "(${state.budget.frequency})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    )
                }
            ) { innerPadding ->
                MainContent(
                    modifier = Modifier.padding(innerPadding),
                    state = state
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    modifier: Modifier,
    state: UiState.Success
) {
    val (budget, expenses) = state

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            BudgetDetailsCard(
                budget = budget,
                spent = expenses.sumOf { it.amount }
            )
            HorizontalDivider(Modifier.padding(vertical = 24.dp))
            ExpenseTable(
                modifier = Modifier,
                expenses = expenses
            )
        }
    }
}
