package com.luna.budgetapp.presentation.screen.budgetdetails

import androidx.compose.ui.res.stringResource
import com.luna.budgetapp.R
import com.luna.budgetapp.presentation.screen.budgetdetails.components.BudgetDetailsCard
import com.luna.budgetapp.presentation.screen.budgetdetails.components.MonthYearPickerDialog
import com.luna.budgetapp.presentation.screen.components.ExpenseTable
import com.luna.budgetapp.presentation.screen.components.ConfirmationDialog
import com.luna.budgetapp.presentation.screen.expenselist.components.ExpenseForm
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailsRoute(
    navController: NavController,
    viewModel: BudgetDetailsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onEvent = viewModel::onEvent

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
                        actions = {
                            IconButton(
                                onClick = {
                                    onEvent(Event.ClickCalendar(state.budget.frequency))
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
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
                BudgetDetailsContent(
                    modifier = Modifier.padding(innerPadding),
                    state = state,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}

@Composable
private fun BudgetDetailsContent(
    modifier: Modifier,
    state: UiState.Success,
    onEvent: (Event) -> Unit
) {
    val (budget, expenses, dialog) = state

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
                expenses = expenses,
                onEdit = { onEvent(Event.ShowExpenseForm(it)) },
                onDelete = { onEvent(Event.ShowDeleteConfirmationDialog(it)) }
            )
        }

        when (dialog) {
            DialogState.DatePicker -> {
                val datePickerState = rememberDatePickerState()

                DatePickerDialog(
                    onDismissRequest = { onEvent(Event.DismissDialog) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onEvent(
                                    Event.ConfirmDate(
                                        datePickerState.selectedDateMillis?.let {
                                            Instant.ofEpochMilli(it)
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDate()
                                        } ?: LocalDate.now()
                                    )
                                )
                            }
                        ) {
                            Text(stringResource(R.string.btn_ok))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { onEvent(Event.DismissDialog) }
                        ) {
                            Text(stringResource(R.string.btn_cancel))
                        }
                    }
                ) {
                    DatePicker(
                        state = datePickerState,
                        showModeToggle = false,
                        title = null,
                        headline = null
                    )
                }
            }
            DialogState.YearMonthPicker -> {
                MonthYearPickerDialog(
                    onMonthYearSelected = { onEvent(Event.ConfirmYearMonth(it)) },
                    onDismiss = { onEvent(Event.DismissDialog) }
                )
            }
            is DialogState.ExpenseForm ->
                ExpenseForm(
                    selectedExpense = dialog.selectedExpense,
                    onDismissRequest = { onEvent(Event.DismissDialog) },
                    onConfirm = { expenseId, type, amount, date ->
                        onEvent(Event.EditExpense(expenseId, type, amount, date))
                    },
                    isSaving = dialog.isSaving
                )
            is DialogState.DeleteConfirmation ->
                ConfirmationDialog(
                    message = stringResource(R.string.confirm_delete_expense),
                    confirmText = stringResource(R.string.btn_delete),
                    isDestructive = true,
                    onDismiss = { onEvent(Event.DismissDialog) },
                    onConfirm = { onEvent(Event.DeleteExpense(dialog.expenseId)) }
                )
            else -> {}
        }
    }
}
