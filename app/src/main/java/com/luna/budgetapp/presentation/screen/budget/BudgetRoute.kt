package com.luna.budgetapp.presentation.screen.budget

import com.luna.budgetapp.presentation.screen.components.LoadingContent
import com.luna.budgetapp.presentation.screen.components.ErrorContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.time.LocalDate
import com.luna.budgetapp.presentation.screen.budget.components.BudgetHeatGridCard
import com.luna.budgetapp.presentation.screen.budget.components.OutlookSheet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luna.budgetapp.R
import com.luna.budgetapp.presentation.screen.budget.components.BudgetCard
import com.luna.budgetapp.presentation.screen.budget.components.BudgetDialog
import com.luna.budgetapp.presentation.screen.components.ConfirmationDialog
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BudgetRoute(
    onOpenBudgetDetails: (BudgetId) -> Unit,
    viewModel: BudgetViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is UiState.Loading -> LoadingContent()
        is UiState.Error -> ErrorContent(
            onRetry = { viewModel.onEvent(Event.Retry) }
        )
        is UiState.Success -> {
            BudgetContent(
                uiState = state,
                onEvent = viewModel::onEvent,
                onOpenBudgetDetails = onOpenBudgetDetails,
                modifier = Modifier
            )
        }
    }
}

@Composable
fun BudgetContent(
    uiState: UiState.Success,
    modifier: Modifier = Modifier,
    onOpenBudgetDetails: (BudgetId) -> Unit,
    onEvent: (Event) -> Unit
) {
    val (budgets, expenses, outlook, dialog) = uiState
    // Plain remember: the tab back stack restores saved state, so expanded day details would reappear.
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var showOutlookSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(outlook.today.year, outlook.today.month) { selectedDay = null }

    if (showOutlookSheet) {
        OutlookSheet(outlook = outlook, onDismiss = { showOutlookSheet = false })
    }

    Box(
        modifier = modifier.fillMaxSize()
            .padding(horizontal = 16.dp)
    ){
        when (val dialog = dialog) {
            is DialogState.DeleteDialog -> {
                ConfirmationDialog(
                    message = stringResource(R.string.confirm_delete_budget),
                    isDestructive = true,
                    onDismiss = { onEvent(Event.DismissDialog) },
                    onConfirm = { onEvent(Event.ConfirmDeleteBudget(dialog.budget)) }
                )
            }
            is DialogState.BudgetDialog -> {
                BudgetDialog(
                    budget = dialog.budget,
                    onDismissRequest = { onEvent(Event.DismissDialog) },
                    onSave = { id, name, amount, frequency, categoryMap ->
                        onEvent(
                            Event.ConfirmBudgetFormDialog(
                                id = id,
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
            item(key = "outlook") {
                BudgetHeatGridCard(
                    modifier = Modifier.padding(4.dp),
                    outlook = outlook,
                    selectedDay = selectedDay,
                    onDayClick = { selectedDay = if (selectedDay == it) null else it },
                    onCollapse = { selectedDay = null },
                    onOpenOutlook = {
                        selectedDay = null
                        showOutlookSheet = true
                    }
                )
            }
            items(budgets, key = { it.id }) { budget ->
                val spent = expenses[budget.id]?.sumOf { it.amount }
                BudgetCard(
                    modifier = Modifier.padding(4.dp),
                    budget = budget,
                    spent = spent ?: 0L,
                    onEdit = { onEvent(Event.ShowBudgetFormDialog(it)) },
                    onDelete = { onEvent(Event.ShowDeleteDialog(it)) },
                    onClick = { onOpenBudgetDetails(budget.id) }
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
                onClick = { onEvent(Event.ShowBudgetFormDialog()) }
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = stringResource(R.string.cd_add_budget),
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}
