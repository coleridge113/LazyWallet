package com.luna.budgetapp.presentation.screen.expensepreset

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.presentation.nav.Routes
import com.luna.budgetapp.presentation.screen.components.ConfirmationDialog
import com.luna.budgetapp.presentation.screen.expensepreset.components.ExpenseAmountDisplay
import com.luna.budgetapp.presentation.screen.expensepreset.components.ExpenseFormAction
import com.luna.budgetapp.presentation.screen.expensepreset.components.ExpensePresetDialog
import com.luna.budgetapp.presentation.screen.expensepreset.components.ExpensePresetTable
import com.luna.budgetapp.presentation.screen.utils.singleClick
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
    var isMenuExpanded by remember { mutableStateOf(false) }

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
                onClickIcon = { onEvent(Event.AddExpensePreset(it, ExpenseFormAction.CUSTOM)) },
                onClickItem = { onEvent(Event.AddExpense(it)) },
                onEdit = { onEvent(Event.AddExpensePreset(it, ExpenseFormAction.EDIT)) },
                onDelete = { onEvent(Event.ShowConfirmationDialog(it)) },
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
                        isSaving = dialog.isSaving,
                        action = dialog.action,
                        onConfirm = { id, category, type, amount ->
                            when (dialog.action) {
                                ExpenseFormAction.ADD ->
                                    onEvent(
                                        Event.ConfirmExpenseFormDialog(
                                            null, category, type, amount
                                        )
                                    )
                                ExpenseFormAction.EDIT ->
                                    onEvent(
                                        Event.ConfirmExpenseFormDialog(
                                            id, category, type, amount
                                        )
                                    )
                                else ->
                                    onEvent(Event.AddExpense(dialog.selectedPreset!!, amount, type))
                            }
                        }
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

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 72.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            val iconSize = 24.dp
            val fabSize = 56.dp
            val offset = fabSize / 2
            val noElevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp
            )

            AnimatedVisibility(
                visible = isMenuExpanded,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy
                    )
                ) { it },
                exit = slideOutVertically { it } + shrinkVertically(),
                modifier = Modifier.padding(bottom = offset)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(top = 18.dp, bottom = 28.dp)
                ) {
                    FloatingActionButton(
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.secondary,
                        elevation = noElevation,
                        onClick = {
                            onEvent(Event.AddExpensePreset(action = ExpenseFormAction.ADD))
                            isMenuExpanded = false
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Add",
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    FloatingActionButton(
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.secondary,
                        elevation = noElevation,
                        onClick = {
                            onEvent(Event.ShowDeleteConfirmationDialog)
                            isMenuExpanded = false
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    FloatingActionButton(
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.secondary,
                        elevation = noElevation,
                        onClick = {
                            onEvent(Event.Logout)
                            isMenuExpanded = false
                        },
                        modifier = Modifier.padding(bottom = 18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            }

            FloatingActionButton(
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.secondary,
                elevation = noElevation,
                onClick = { isMenuExpanded = !isMenuExpanded }
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    modifier = Modifier.size(iconSize)
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
    val expensesState = ExpensesState(
        totalAmount = totalAmount,
        expensePresets = listOf(
            ExpensePreset(
                amount = 100.0,
                category = "FOOD",
                type = "Lunch"
            ),
            ExpensePreset(
                amount = 140.0,
                category = "BEVERAGE",
                type = "Coffee"
            ),
            ExpensePreset(
                amount = 140.0,
                category = "BEVERAGE",
                type = "Coffee"
            ),
            ExpensePreset(
                amount = 140.0,
                category = "BEVERAGE",
                type = "Coffee"
            ),
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
