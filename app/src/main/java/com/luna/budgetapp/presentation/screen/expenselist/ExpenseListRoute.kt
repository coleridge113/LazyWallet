package com.luna.budgetapp.presentation.screen.expenselist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.presentation.nav.Routes
import com.luna.budgetapp.presentation.screen.components.CategoryFilterDialog
import com.luna.budgetapp.presentation.screen.components.ConfirmationDialog
import com.luna.budgetapp.presentation.screen.components.DateRangePickerDialog
import com.luna.budgetapp.presentation.screen.components.DateRangeSelectorDropdown
import com.luna.budgetapp.presentation.screen.expenselist.components.ExpenseChart
import com.luna.budgetapp.presentation.screen.expenselist.components.ExpenseForm
import com.luna.budgetapp.presentation.screen.expenselist.components.ExpenseTable
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListRoute(
    navController: NavController,
    viewModel: ExpenseListViewModel
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val expenses = viewModel.expensesPagingFlow.collectAsLazyPagingItems()

    LaunchedEffect(Unit) {
        viewModel.navigation.collectLatest { navigation ->
            when (navigation) {
                Navigation.GotoAnalysisRoute -> {
                    navController.navigate(Routes.AnalysisRoute) {
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
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        modifier = Modifier,
                        title = {},
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    navController.popBackStack()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBackIosNew,
                                    contentDescription = null
                                )
                            }
                        },
                        actions = {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                modifier = Modifier.clickable {
                                    viewModel.onEvent(Event.GotoBarGraph)
                                }
                            )
                            DateRangeSelectorDropdown(
                                selected = state.dateState.dateFilter,
                                onSelectedChange = {
                                    when (it) {
                                        DateFilter.Daily,
                                        DateFilter.Weekly,
                                        DateFilter.Monthly -> viewModel.onEvent(Event.SelectDateRange(it))
                                        else -> viewModel.onEvent(Event.ShowCalendarForm)
                                    }
                                }
                            )
                        }
                    )
                }
            ) { innerPadding ->
                MainContent(
                    modifier = Modifier.padding(innerPadding),
                    uiState = state,
                    onEvent = viewModel::onEvent,
                    totalAmount = state.expensesState.totalAmount,
                    expenses = expenses
                )
            }
        }
    }
}

@Composable
fun MainContent(
    modifier: Modifier,
    uiState: UiState.Success,
    onEvent: (Event) -> Unit,
    totalAmount: Double,
    expenses: LazyPagingItems<Expense>
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
            .padding(16.dp)
    ) {
        ExpenseChart(
            chartDataList = uiState.chartDataState.chartDataList,
            totalAmount = totalAmount,
            showDialog = { onEvent(Event.ShowCategoryFilterDialog) },
            onClickCenter = { onEvent(Event.ResetCategoryFilters) }
        )
        Spacer(modifier = Modifier.height(48.dp))
        when {
            expenses.itemCount <= 0 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "No expenses for the filtered range!",
                        modifier = Modifier.padding(top = 48.dp)
                    )
                }
            }
            else -> {
                ExpenseTable(
                    modifier = Modifier,
                    expenses = expenses,
                    onEdit = { onEvent(Event.ShowExpenseForm(it)) },
                    onDelete = { onEvent(Event.ShowDeleteConfirmationDialog(it.id!!)) },
                )
            }
        }

        when (val dialog = uiState.dialogState) {
            DialogState.CalendarForm ->
                DateRangePickerDialog(
                    onDismiss = { onEvent(Event.DismissDialog) },
                    onConfirm = { start, end ->
                        when {
                            start == null -> onEvent(Event.DismissDialog)
                            else -> onEvent(Event.SelectDateRange(DateFilter.Custom(start, end)))
                        }
                    }
                )
            is DialogState.CategoryFilterForm ->
                CategoryFilterDialog(
                    selectedCategoryMap = dialog.filteredCategories,
                    selectedProfile = uiState.categoryProfileState.activeProfile,
                    profileList = uiState.categoryProfileState.profileList,
                    onDismiss = { onEvent(Event.DismissDialog) },
                    onApply = { profileName, filters ->
                        onEvent(Event.ApplyCategoryFilters(profileName, filters))
                    },
                    onSelectedChange = { profileName ->
                        onEvent(Event.SelectCategoryProfile(profileName))
                    },
                    onSave = { newProfileName, filters ->
                        onEvent(Event.SaveCategoryProfile(newProfileName, filters))
                    },
                    onDelete = { profileName ->
                        onEvent(Event.DeleteCategoryProfile(profileName))
                    }
                )

            is DialogState.ConfirmDeleteExpense ->
                ConfirmationDialog(
                    message = "Delete this expense?",
                    confirmText = "Delete",
                    isDestructive = true,
                    onDismiss = { onEvent(Event.DismissDialog) },
                    onConfirm = { onEvent(Event.DeleteExpense(dialog.expenseId)) }
                )

            is DialogState.ExpenseForm ->
                ExpenseForm(
                    selectedExpense = dialog.selectedExpense,
                    onDismissRequest = { onEvent(Event.DismissDialog) },
                    onConfirm = { expenseId, type, amount ->
                        onEvent(Event.EditExpense(expenseId, type, amount))
                    },
                    isSaving = dialog.isSaving
                )

            else -> {}
        }
    }
}
