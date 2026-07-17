package com.luna.budgetapp.presentation.screen.budget

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import org.koin.compose.viewmodel.koinViewModel
import com.luna.budgetapp.domain.model.Budget
import com.luna.budgetapp.presentation.screen.budget.components.BudgetCard

@Composable
fun BudgetRoute(
    navController: NavController,
    viewModel: BudgetViewModel = koinViewModel()
) {

}

@Composable
fun MainContent(
    budgets: List<Budget>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(budgets) { budget ->
            BudgetCard(
                modifier = Modifier,
                budget = budget,
                spent = 0.0,
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

}