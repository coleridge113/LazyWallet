package com.luna.budgetapp.presentation.screen.analysis

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.luna.budgetapp.presentation.screen.analysis.components.DailyExpenseBarChart
import com.luna.budgetapp.presentation.screen.analysis.components.ExpenseTable
import com.luna.budgetapp.presentation.screen.components.CategoryProfileSelectorDropdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisRoute(
    navController: NavController,
    viewModel: AnalysisViewModel
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                    if (uiState is UiState.Success) {
                        val successState = uiState as UiState.Success
                        CategoryProfileSelectorDropdown(
                            modifier = Modifier.padding(end = 12.dp),
                            selectedProfile = successState.categoryProfileState.activeProfile,
                            profileList = successState.categoryProfileState.profileList,
                            onSelectedChange = {
                                viewModel.onEvent(Event.SelectCategoryProfile(it))
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

        }
        when (val state = uiState) {
            is UiState.Loading -> {}
            is UiState.Error -> {}
            is UiState.Success -> {
                MainContent(
                    modifier = Modifier.padding(innerPadding),
                    uiState = state,
                    onEvent = viewModel::onEvent,
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
