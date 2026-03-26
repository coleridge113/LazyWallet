package com.luna.budgetapp.presentation.screen.expensepreset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.luna.budgetapp.presentation.nav.Routes
import com.luna.budgetapp.presentation.screen.components.ConfirmationDialog
import com.luna.budgetapp.presentation.screen.expensepreset.components.ExpenseAmountDisplay
import com.luna.budgetapp.presentation.screen.expensepreset.components.ExpensePresetDialog
import com.luna.budgetapp.presentation.screen.expensepreset.components.ExpensePresetTable
import com.luna.budgetapp.presentation.screen.utils.singleClick
import com.luna.budgetapp.ui.icons.CirclePlusIcon
import com.luna.budgetapp.ui.icons.UndoIcon
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExpensePresetRoute(
    navController: NavController,
    viewModel: ExpensePresetViewModel = koinViewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigation.collectLatest { navigation ->
            when (navigation) {
                Navigation.GotoExpenseRoute -> {
                    navController.navigate(Routes.ExpensesRoute) {
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        MainContent(
            uiState = uiState,
            modifier = Modifier.padding(innerPadding),
            onEvent = viewModel::onEvent,
        )
    }
}

@Composable
fun MainContent(
    uiState: UiState,
    modifier: Modifier = Modifier,
    onEvent: (Event) -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize()
            .padding(16.dp)
    ){
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            ExpenseAmountDisplay(
                totalAmount = uiState.totalAmount,
                modifier = Modifier.weight(1f)
                    .fillMaxWidth()
                    .singleClick { onEvent(Event.GotoExpenseRoute) }
            )

            ExpensePresetTable(
                expensePresets = uiState.expensePresets,
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
                                onEvent(Event.ConfirmDialog(category, type, amount))
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
                shape = CircleShape
            ) {
                Icon(
                    imageVector = CirclePlusIcon,
                    contentDescription = null
                )
            }
            FloatingActionButton(
                onClick = { onEvent(Event.ShowDeleteConfirmationDialog) },
                shape = CircleShape
            ) {
                Icon(
                    imageVector = UndoIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
