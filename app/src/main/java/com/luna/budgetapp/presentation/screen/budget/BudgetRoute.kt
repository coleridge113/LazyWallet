package com.luna.budgetapp.presentation.screen.budget

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.luna.budgetapp.domain.model.Budget
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import com.luna.budgetapp.presentation.screen.budget.components.BudgetCard
import java.time.LocalDate

@Composable
fun BudgetRoute(
    viewModel: BudgetViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    when (val state = uiState) {
        is UiState.Loading -> {}
        is UiState.Error -> {}
        is UiState.Success -> {
            MainContent(
                budgets = state.budgets,
                expenses = state.expenses,
                modifier = Modifier
            )
        }
    }
}

@Composable
fun MainContent(
    budgets: List<Budget>,
    expenses: Map<BudgetId, List<Expense>>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(budgets) { budget ->
            val spent = expenses[budget.id]?.sumOf { it.amount }
            BudgetCard(
                modifier = Modifier.padding(4.dp),
                budget = budget,
                spent = spent ?: 0.0,
                onEdit = { }
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
fun BudgetRoutePreview() {
    LazyWalletTheme {
        val budgets = listOf(
            Budget(
                id = 1,
                name = "Monthly Grocery",
                limit = 500.0,
                frequency = DateFilter.Monthly,
                interactors = listOf(Category.GROCERY),
                startDate = LocalDate.now().withDayOfMonth(1)
            ),
            Budget(
                id = 2,
                name = "Daily Food",
                limit = 30.0,
                frequency = DateFilter.Daily,
                interactors = listOf(Category.FOOD),
                startDate = LocalDate.now()
            )
        )
        val expenses = mapOf(
            1L to listOf(
                Expense(amount = 50.0, category = Category.GROCERY.name, type = "DEBIT"),
                Expense(amount = 25.0, category = Category.GROCERY.name, type = "DEBIT")
            ),
            2L to listOf(
                Expense(amount = 15.0, category = Category.FOOD.name, type = "DEBIT")
            )
        )
        MainContent(
            budgets = budgets,
            expenses = expenses
        )
    }
}