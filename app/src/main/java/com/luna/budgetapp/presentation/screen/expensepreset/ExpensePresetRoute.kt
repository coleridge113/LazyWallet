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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.luna.budgetapp.presentation.screen.expensepreset.components.RecentlyAddedSheet
import com.luna.budgetapp.presentation.screen.utils.toPeso
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import androidx.compose.ui.platform.LocalAccessibilityManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExpensePresetRoute(
    onSignedOut: () -> Unit,
    onOpenExpenses: () -> Unit,
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
                onOpenExpenses = onOpenExpenses,
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
    onOpenExpenses: () -> Unit = {},
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

    var showRecent by rememberSaveable { mutableStateOf(false) }
    val recent = expensesState.recent
    LaunchedEffect(recent.isEmpty()) {
        if (recent.isEmpty()) showRecent = false
    }

    val sheetShown = showRecent && recent.isNotEmpty()
    val sheetShownState by rememberUpdatedState(sheetShown)
    val snackbarHostState = remember { SnackbarHostState() }
    // The snackbar is dismissed whenever the sheet opens or closes (not on first composition).
    LaunchedEffect(snackbarHostState) {
        snapshotFlow { sheetShownState }.drop(1).collect {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
    val userMessage = uiState.userMessage
    val context = LocalContext.current
    val undoLabel = stringResource(R.string.expense_preset_undo)
    DisposableEffect(Unit) {
        onDispose { onEvent(Event.ClearUndo) }
    }
    val accessibilityManager = LocalAccessibilityManager.current
    LaunchedEffect(userMessage) {
        if (userMessage is UserMessage.Removed) {
            val timeoutMs = accessibilityManager?.calculateRecommendedTimeoutMillis(
                4000L,
                containsIcons = true,
                containsText = true,
                containsControls = true
            ) ?: 4000L
            val result = coroutineScope {
                // The Route owns the timeout: the snackbar is dismissed when the
                // sheet opens or closes, so it never moves between the two hosts.
                val timer = launch {
                    delay(timeoutMs)
                    snackbarHostState.currentSnackbarData?.dismiss()
                }
                val r = snackbarHostState.showSnackbar(
                    message = context.getString(
                        R.string.expense_preset_removed,
                        userMessage.type,
                        userMessage.amount.toPeso()
                    ),
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Indefinite
                )
                timer.cancel()
                r
            }
            // Only reached on normal return: a newer message cancels this effect,
            // and the old one must not clear the new message.
            if (result == SnackbarResult.ActionPerformed) onEvent(Event.UndoRemove)
            onEvent(Event.UserMessageShown)
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
                    onOpen = { showRecent = true },
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

        if (!sheetShown) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (sheetShown) {
            RecentlyAddedSheet(
                recent = recent,
                total = expensesState.totalAmount,
                now = now,
                onRemove = { onEvent(Event.RemoveExpense(it)) },
                onSeeAll = {
                    showRecent = false
                    onOpenExpenses()
                },
                onDismiss = { showRecent = false },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            )
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
