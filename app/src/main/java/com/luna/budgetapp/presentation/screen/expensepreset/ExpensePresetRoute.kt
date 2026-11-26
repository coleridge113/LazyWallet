package com.luna.budgetapp.presentation.screen.expensepreset

import androidx.compose.material3.IconButton
import com.luna.budgetapp.presentation.screen.components.LoadingContent
import com.luna.budgetapp.presentation.screen.components.ErrorContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luna.budgetapp.R
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.presentation.screen.components.ConfirmationDialog
import com.luna.budgetapp.presentation.screen.expensepreset.components.ExpenseFormAction
import com.luna.budgetapp.presentation.screen.expensepreset.components.ExpensePresetDialog
import com.luna.budgetapp.presentation.screen.expensepreset.components.ExpensePresetHero
import com.luna.budgetapp.presentation.screen.expensepreset.components.LastLoggedStrip
import com.luna.budgetapp.presentation.screen.expensepreset.components.PresetTileGrid
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExpensePresetRoute(
    onSignedOut: () -> Unit,
    viewModel: ExpensePresetViewModel = koinViewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onEvent = viewModel::onEvent

    val signedOut by viewModel.signedOut.collectAsStateWithLifecycle()
    LaunchedEffect(signedOut) {
        if (signedOut) {
            onSignedOut()
            viewModel.onSignedOutHandled()
        }
    }

    when (val state = uiState) {
        is UiState.Loading -> LoadingContent()
        is UiState.Error -> ErrorContent(
            onRetry = { onEvent(Event.Retry) }
        )
        is UiState.Success -> {
            ExpensePresetContent(
                uiState = state,
                modifier = Modifier,
                onEvent = onEvent
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensePresetContent(
    modifier: Modifier = Modifier,
    uiState: UiState.Success,
    onEvent: (Event) -> Unit
) {
    val expensePresets = uiState.expensesState.expensePresets
    val expensesState = uiState.expensesState
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalDateTime.now()
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
            .padding(horizontal = 16.dp)
    ){
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                modifier = Modifier,
                title = {},
                windowInsets = WindowInsets(0, 0, 0, 0),
                actions = {
                    IconButton(onClick = { onEvent(Event.ShowSignOutDialog) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.cd_sign_out)
                        )
                    }
                }
            )

            ExpensePresetHero(
                total = expensesState.totalAmount,
                sparkline = expensesState.sparkline
            )

            expensesState.lastLogged?.let { last ->
                LastLoggedStrip(
                    expense = last,
                    now = now,
                    onOpen = { /* Task 4 opens the recently added sheet */ },
                    onUndo = { onEvent(Event.RemoveExpense(last)) }
                )
            }

            PresetTileGrid(
                presets = expensePresets,
                onLog = { onEvent(Event.AddExpense(it)) },
                onCustom = { onEvent(Event.AddExpensePreset(it, ExpenseFormAction.CUSTOM)) },
                onEdit = { onEvent(Event.AddExpensePreset(it, ExpenseFormAction.EDIT)) },
                onDelete = { onEvent(Event.ShowConfirmationDialog(it)) },
                onNewPreset = { onEvent(Event.AddExpensePreset(null, ExpenseFormAction.ADD)) },
                modifier = Modifier.weight(1f)
            )

            when (val dialog = uiState.dialogState) {
                DialogState.ConfirmLogout -> {
                    ConfirmationDialog(
                        message = stringResource(R.string.confirm_sign_out),
                        confirmText = stringResource(R.string.btn_yes),
                        isDestructive = true,
                        onDismiss = { onEvent(Event.DismissDialog) },
                        onConfirm = { onEvent(Event.SignOut) }
                    )
                }
                is DialogState.ExpenseForm -> {
                    ExpensePresetDialog(
                        selectedPreset = dialog.selectedPreset,
                        onDismissRequest = { onEvent(Event.DismissDialog) },
                        isSaving = dialog.isSaving,
                        action = dialog.action,
                        onConfirm = { id, category, type, amount, date ->
                            when (dialog.action) {
                                ExpenseFormAction.ADD ->
                                    onEvent(
                                        Event.ConfirmExpenseFormDialog(
                                            null, category, type, amount, date
                                        )
                                    )
                                ExpenseFormAction.EDIT ->
                                    onEvent(
                                        Event.ConfirmExpenseFormDialog(
                                            id, category, type, amount, date
                                        )
                                    )
                                else ->
                                    onEvent(Event.AddExpense(dialog.selectedPreset!!, amount, type, date))
                            }
                        }
                    )
                }
                is DialogState.ConfirmDeleteExpensePreset -> {
                    ConfirmationDialog(
                        message = stringResource(R.string.confirm_delete_item),
                        confirmText = stringResource(R.string.btn_delete),
                        isDestructive = true,
                        onDismiss = { onEvent(Event.DismissDialog) },
                        onConfirm = { onEvent(Event.DeleteExpensePreset(dialog.expensePresetId)) }
                    )
                }
                else -> {}
            }
        }
    }
}

@Preview(
    showSystemUi = true,
    device = Devices.PIXEL_7,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_NO
)
@Composable
fun ExpenseRoutePreviewLight() {
    val expensesState = ExpensesState(
        expensePresets = listOf(
            ExpensePreset(amount = 10000, category = Category.FOOD, type = "Lunch"),
            ExpensePreset(amount = 14000, category = Category.BEVERAGE, type = "Coffee")
        ),
        expenses = listOf(
            Expense(id = 1, amount = 5000, category = Category.FOOD, type = "Lunch"),
            Expense(id = 2, amount = 1500, category = Category.BEVERAGE, type = "Coffee"),
            Expense(id = 3, amount = 20000, category = Category.LEISURE, type = "Groceries")
        ),
        sparkline = listOf(21000, 3000, 51000, 23000, 17000, 20000, 26500),
        lastLogged = Expense(id = 3, amount = 20000, category = Category.LEISURE, type = "Groceries")
    )

    val uiState = UiState.Success(
        expensesState = expensesState
    )

    LazyWalletTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ExpensePresetContent(
                uiState = uiState,
                onEvent = {}
            )
        }
    }
}

@Preview(
    showSystemUi = true,
    device = Devices.PIXEL_7,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
fun ExpenseRoutePreviewDark() {
    val uiState = UiState.Success()

    LazyWalletTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            ExpensePresetContent(
                uiState = uiState,
                onEvent = {}
            )
        }
    }
}

@Preview(
    showSystemUi = true,
    device = Devices.PIXEL_7,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
fun ExpenseRoutePreviewDialog() {
    val expenseFormDialog = DialogState.ExpenseForm()
    val uiState = UiState.Success(
        dialogState = expenseFormDialog
    )

    LazyWalletTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            ExpensePresetContent(
                uiState = uiState,
                onEvent = {}
            )
        }
    }
}
