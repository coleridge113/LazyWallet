package com.luna.budgetapp.presentation.screen.budgetdetails

import androidx.compose.ui.res.stringResource
import com.luna.budgetapp.R
import com.luna.budgetapp.presentation.screen.budgetdetails.components.BudgetDetailsCard
import com.luna.budgetapp.presentation.screen.budgetdetails.components.MonthYearPickerDialog
import com.luna.budgetapp.presentation.screen.components.ErrorContent
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.luna.budgetapp.domain.model.DateFilter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailsRoute(
    onBack: () -> Unit,
    viewModel: BudgetDetailsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onEvent = viewModel::onEvent
    // Picker visibility is pure UI state; the ViewModel only receives the picked result.
    var showPicker by rememberSaveable { mutableStateOf(false) }

    when (val state = uiState) {
        UiState.Loading -> {}
        is UiState.Error -> ErrorContent(
            onRetry = { onEvent(Event.Retry) }
        )
        is UiState.Success -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    onBack()
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
                                    if (state.budget.frequency == DateFilter.Daily ||
                                        state.budget.frequency == DateFilter.Monthly
                                    ) {
                                        showPicker = true
                                    }
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
                    onEvent = viewModel::onEvent,
                    showPicker = showPicker,
                    onDismissPicker = { showPicker = false }
                )
            }
        }
    }
}

@Composable
private fun BudgetDetailsContent(
    modifier: Modifier,
    state: UiState.Success,
    onEvent: (Event) -> Unit,
    showPicker: Boolean,
    onDismissPicker: () -> Unit
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

        if (showPicker) {
            when (budget.frequency) {
            DateFilter.Daily -> {
                val datePickerState = rememberDatePickerState()

                DatePickerDialog(
                    onDismissRequest = onDismissPicker,
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
                                onDismissPicker()
                            }
                        ) {
                            Text(stringResource(R.string.btn_ok))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = onDismissPicker
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
            DateFilter.Monthly -> {
                MonthYearPickerDialog(
                    onMonthYearSelected = {
                        onEvent(Event.ConfirmYearMonth(it))
                        onDismissPicker()
                    },
                    onDismiss = onDismissPicker
                )
            }
            else -> {}
            }
        }

        when (dialog) {
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
