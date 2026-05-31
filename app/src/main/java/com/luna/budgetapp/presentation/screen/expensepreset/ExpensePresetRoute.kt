package com.luna.budgetapp.presentation.screen.expensepreset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.presentation.nav.Routes
import com.luna.budgetapp.presentation.screen.components.ConfirmationDialog
import com.luna.budgetapp.presentation.screen.expensepreset.components.ExpenseAmountDisplay
import com.luna.budgetapp.presentation.screen.expensepreset.components.ExpensePresetDialog
import com.luna.budgetapp.presentation.screen.expensepreset.components.ExpensePresetTable
import com.luna.budgetapp.presentation.screen.utils.singleClick
import com.luna.budgetapp.ui.icons.CirclePlusIcon
import com.luna.budgetapp.ui.icons.UndoIcon
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExpensePresetRoute(
    navController: NavController,
    viewModel: ExpensePresetViewModel = koinViewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val auth = remember { FirebaseAuth.getInstance() }

    LaunchedEffect(Unit) {
        viewModel.navigation.collectLatest { navigation ->
            when (navigation) {
                Navigation.GotoExpenseRoute -> {
                    navController.navigate(Routes.ExpensesRoute) {
                        launchSingleTop = true
                    }
                }
                Navigation.Logout -> {
                    auth.signOut()
                    navController.navigate(Routes.AuthRoute) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    when (val state = uiState) {
        is UiState.Loading -> {}
        is UiState.Error -> {}
        is UiState.Success -> {
            Scaffold(
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                MainContent(
                    uiState = state,
                    modifier = Modifier.padding(innerPadding),
                    onEvent = viewModel::onEvent,
                )
            }
        }
    }
}

@Composable
fun MainContent(
    uiState: UiState.Success,
    modifier: Modifier = Modifier,
    onEvent: (Event) -> Unit
) {
    val (expensePresets, totalAmount) = uiState.expensesState
    Box(
        modifier = modifier.fillMaxSize()
            .padding(16.dp)
    ){
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            ExpenseAmountDisplay(
                totalAmount = totalAmount,
                modifier = Modifier.weight(1f)
                    .fillMaxWidth()
                    .singleClick { onEvent(Event.GotoExpenseRoute) }
            )

            ExpensePresetTable(
                expensePresets = expensePresets,
                onClickIcon = { onEvent(Event.ShowExpenseForm(it)) },
                onLongClickIcon = { onEvent(Event.ShowConfirmationDialog(it)) },
                onClickItem = { onEvent(Event.AddExpense(it)) },
                modifier = Modifier.weight(3f)
            )

            when (val dialog = uiState.dialogState) {
                DialogState.ConfirmDeleteExpense -> {
                    ConfirmationDialog(
                        message = "Delete the last expense?",
                        confirmText = "Delete",
                        isDestructive = true,
                        onDismiss = { onEvent(Event.DismissDialog) },
                        onConfirm = { onEvent(Event.DeleteLatestExpense) }
                    )
                }
                is DialogState.ExpenseForm -> {
                    ExpensePresetDialog(
                        selectedPreset = dialog.selectedPreset,
                        onDismissRequest = { onEvent(Event.DismissDialog) },
                        onConfirm = { category, type, amount ->
                            if (dialog.selectedPreset == null) {
                                onEvent(Event.ConfirmExpenseFormDialog(category, type, amount))
                            } else {
                                onEvent(Event.AddExpense(dialog.selectedPreset, amount, type))
                            }
                        },
                        isSaving = dialog.isSaving
                    )
                }
                is DialogState.ConfirmDeleteExpensePreset -> {
                    ConfirmationDialog(
                        message = "Delete this item?",
                        confirmText = "Delete",
                        isDestructive = true,
                        onDismiss = { onEvent(Event.DismissDialog) },
                        onConfirm = { onEvent(Event.DeleteExpensePreset(dialog.expensePresetId)) }
                    )
                }
                else -> {}
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            FloatingActionButton(
                onClick = { onEvent(Event.ShowExpenseForm()) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(
                    imageVector = CirclePlusIcon,
                    contentDescription = null
                )
            }
            FloatingActionButton(
                onClick = { onEvent(Event.ShowDeleteConfirmationDialog) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(
                    imageVector = UndoIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            FloatingActionButton(
                onClick = { onEvent(Event.Logout) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
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
    val totalAmount = 1234.56
    val expensePresetFood = ExpensePreset(
        amount = 100.0,
        category = "FOOD",
        type = "Lunch"
    )
    val expensePresetBeverage = ExpensePreset(
        amount = 140.0,
        category = "BEVERAGE",
        type = "Coffee"
    )
    val expensesState = ExpensesState(
        totalAmount = totalAmount,
        expensePresets = listOf(
            expensePresetFood,
            expensePresetBeverage
        )
    )
    val uiState = UiState.Success(
        expensesState = expensesState
    )

    LazyWalletTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            MainContent(
                uiState = uiState,
                modifier = Modifier.fillMaxSize(),
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
            MainContent(
                uiState = uiState,
                modifier = Modifier.fillMaxSize(),
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
            MainContent(
                uiState = uiState,
                modifier = Modifier.fillMaxSize(),
                onEvent = {}
            )
        }
    }
}
