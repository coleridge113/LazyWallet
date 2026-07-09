package com.luna.budgetapp.presentation.screen.analysis

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.presentation.screen.analysis.components.DailyExpenseBarChart
import com.luna.budgetapp.presentation.screen.analysis.components.ExpenseTable
import com.luna.budgetapp.presentation.screen.components.CategoryProfileSelectorDropdown
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisRoute(
    viewModel: AnalysisViewModel
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onEvent = viewModel::onEvent

    when (val state = uiState) {
        is UiState.Loading -> {}
        is UiState.Error -> {}
        is UiState.Success -> {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    modifier = Modifier,
                    title = {},
                    actions = {
                        CategoryProfileSelectorDropdown(
                            selectedProfile = state.categoryProfileState.activeProfile,
                            profileList = state.categoryProfileState.profileList,
                            onSelectedChange = {
                                onEvent(Event.SelectCategoryProfile(it))
                            }
                        )
                    }
                )

                MainContent(
                    modifier = Modifier,
                    uiState = state,
                    onEvent = onEvent,
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
) {
    Column(
        modifier = modifier.fillMaxSize()
            .padding(16.dp)
    ) {
        DailyExpenseBarChart(
            modifier = Modifier,
            expenses = uiState.expensesState.expenses,
            selectedDate = uiState.dateState.selectedDate,
            onClickBar = { date ->
                onEvent(Event.SelectBar(date)) 
            }
        )

        ExpenseTable(
            modifier = Modifier,
            expenses = uiState.expensesState.filteredExpenses
        )
    }
}

@Preview(
    showSystemUi = true,
    device = Devices.PIXEL_7,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_NO
)
@Composable
private fun MainContentPreviewLight() {
    val now = LocalDateTime.now()
    val dummyExpenses = listOf(
        Expense(1L, "Coffee", 90.0, "Food", "Expense", now.minusDays(6)),
        Expense(2L, "Lunch", 150.0, "Food", "Expense", now.minusDays(6)),
        Expense(3L, "Grab", 200.0, "Transport", "Expense", now.minusDays(5)),
        Expense(4L, "Dinner", 180.0, "Food", "Expense", now.minusDays(4)),
        Expense(5L, "Snacks", 70.0, "Food", "Expense", now.minusDays(4)),
        Expense(6L, "Groceries", 500.0, "Groceries", "Expense", now.minusDays(3)),
        Expense(7L, "Coffee", 95.0, "Food", "Expense", now.minusDays(2)),
        Expense(8L, "Taxi", 180.0, "Transport", "Expense", now.minusDays(2)),
        Expense(9L, "Lunch", 160.0, "Food", "Expense", now.minusDays(1)),
        Expense(10L, "Breakfast", 80.0, "Food", "Expense", now),
        Expense(11L, "Dinner", 200.0, "Food", "Expense", now)
    )

    val successState = UiState.Success(
        expensesState = ExpensesState(
            expenses = dummyExpenses,
            filteredExpenses = dummyExpenses
        )
    )

    LazyWalletTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            MainContent(
                modifier = Modifier,
                uiState = successState,
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
private fun MainContentPreviewDark() {
    val now = LocalDateTime.now()
    val dummyExpenses = listOf(
        Expense(1L, "Coffee", 90.0, "Food", "Expense", now.minusDays(6)),
        Expense(2L, "Lunch", 150.0, "Food", "Expense", now.minusDays(6)),
        Expense(3L, "Grab", 200.0, "Transport", "Expense", now.minusDays(5)),
        Expense(4L, "Dinner", 180.0, "Food", "Expense", now.minusDays(4)),
        Expense(5L, "Snacks", 70.0, "Food", "Expense", now.minusDays(4)),
        Expense(6L, "Groceries", 500.0, "Groceries", "Expense", now.minusDays(3)),
        Expense(7L, "Coffee", 95.0, "Food", "Expense", now.minusDays(2)),
        Expense(8L, "Taxi", 180.0, "Transport", "Expense", now.minusDays(2)),
        Expense(9L, "Lunch", 160.0, "Food", "Expense", now.minusDays(1)),
        Expense(10L, "Breakfast", 80.0, "Food", "Expense", now),
        Expense(11L, "Dinner", 200.0, "Food", "Expense", now)
    )

    val successState = UiState.Success(
        expensesState = ExpensesState(
            expenses = dummyExpenses,
            filteredExpenses = dummyExpenses
        )
    )

    LazyWalletTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            MainContent(
                modifier = Modifier,
                uiState = successState,
                onEvent = {}
            )
        }
    }
}
