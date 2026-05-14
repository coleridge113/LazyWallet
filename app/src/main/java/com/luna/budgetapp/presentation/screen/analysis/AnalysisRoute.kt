package com.luna.budgetapp.presentation.screen.analysis

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.luna.budgetapp.presentation.screen.analysis.components.DailyExpenseBarChart
import com.luna.budgetapp.presentation.screen.analysis.components.ExpenseTable
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.presentation.screen.components.CategoryProfileSelectorDropdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisRoute(
    navController: NavController,
    viewModel: AnalysisViewModel
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val filteredExpenses by viewModel.filteredExpenses.collectAsStateWithLifecycle()
    val profileList by viewModel.profileList.collectAsStateWithLifecycle()

    Scaffold(
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
                    CategoryProfileSelectorDropdown(
                        modifier = Modifier.padding(end = 12.dp),
                        selectedProfile = uiState.activeProfile,
                        profileList = profileList,
                        onSelectedChange = {
                            viewModel.onEvent(Event.SelectCategoryProfile(it))
                        }
                    )
                }
            )
        }
    ) { innerPadding ->

        MainContent(
            modifier = Modifier.padding(innerPadding),
            uiState = uiState,
            expenses = expenses,
            filteredExpenses = filteredExpenses,
            onEvent = viewModel::onEvent,
        )
    }
}

@Composable
fun MainContent(
    modifier: Modifier,
    expenses: List<Expense>,
    filteredExpenses: List<Expense>,
    uiState: UiState,
    onEvent: (Event) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize()
            .padding(16.dp)
    ) {
        DailyExpenseBarChart(
            modifier = Modifier,
            expenses = expenses,
            selectedDate = uiState.selectedDate,
            onClickBar = { date ->
                onEvent(Event.SelectBar(date)) 
            }
        )

        ExpenseTable(
            modifier = Modifier,
            expenses = filteredExpenses
        )
    }
}
