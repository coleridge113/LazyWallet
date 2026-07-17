package com.luna.budgetapp.presentation.screen.budget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.luna.budgetapp.domain.model.Budget
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import com.luna.budgetapp.presentation.screen.budget.components.BudgetCard
import com.luna.budgetapp.presentation.screen.expensepreset.components.BudgetDialog
import java.time.LocalDate

@Composable
fun BudgetRoute(
    viewModel: BudgetViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    when (val state = uiState) {
        is UiState.Loading -> {}
        is UiState.Error -> {}
        is UiState.Success -> {
            MainContent(
                uiState = state,
                onEvent = viewModel::onEvent,
                modifier = Modifier
            )
        }
    }
}

@Composable
fun MainContent(
    uiState: UiState.Success,
    modifier: Modifier = Modifier,
    onEvent: (Event) -> Unit
) {
    val (budgets, expenses, dialog) = uiState

    Box(
        modifier = modifier.fillMaxSize()
            .padding(horizontal = 16.dp)
    ){
        when (val dialog = dialog) {
            DialogState.BudgetDialog -> {
                BudgetDialog(
                    onDismissRequest = { onEvent(Event.DismissDialog) },
                    onSave = { name, amount, frequency, categoryMap ->
                        onEvent(
                            Event.ConfirmBudgetFormDialog(
                                name = name,
                                amount = amount,
                                frequency = frequency,
                                categoryMap = categoryMap
                            )
                        )
                    }
                )
            }
            else -> {}
        }

        LazyColumn(modifier = modifier) {
            items(budgets) { budget ->
                val spent = expenses[budget.id]?.sumOf { it.amount }
                BudgetCard(
                    modifier = Modifier.padding(4.dp),
                    budget = budget,
                    spent = spent ?: 0.0,
                    onEdit = { }
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 72.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            val iconSize = 24.dp
            val noElevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp
            )

            FloatingActionButton(
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.secondary,
                elevation = noElevation,
                onClick = { onEvent(Event.ShowBudgetDialog) }
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = "Menu",
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}