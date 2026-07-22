package com.luna.budgetapp.presentation.nav

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.luna.budgetapp.presentation.model.NavOptions
import com.luna.budgetapp.presentation.screen.components.BottomNavBar
import com.luna.budgetapp.presentation.screen.expensepreset.ExpensePresetViewModel
import com.luna.budgetapp.presentation.screen.expenselist.ExpenseListViewModel
import org.koin.compose.viewmodel.koinViewModel
import com.luna.budgetapp.presentation.screen.expensepreset.ExpensePresetRoute
import com.luna.budgetapp.presentation.screen.auth.AuthViewModel
import com.luna.budgetapp.presentation.screen.auth.AuthRoute
import com.luna.budgetapp.presentation.screen.expenselist.ExpenseListRoute
import com.luna.budgetapp.presentation.screen.analysis.AnalysisViewModel
import com.luna.budgetapp.presentation.screen.analysis.AnalysisRoute
import com.luna.budgetapp.presentation.screen.budget.BudgetRoute
import com.luna.budgetapp.presentation.screen.budget.BudgetViewModel
import com.luna.budgetapp.presentation.screen.budgetdetails.BudgetDetailsRoute
import com.luna.budgetapp.presentation.screen.budgetdetails.BudgetDetailsViewModel

@ExperimentalMaterial3Api
@ExperimentalSharedTransitionApi
@Composable
fun NavGraphSetup(
    navController: NavHostController,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val selectedOption = when {
        currentRoute?.contains("ExpensePresetRoute") == true -> NavOptions.HOME
        currentRoute?.contains("ExpensesRoute") == true -> NavOptions.LIST
        currentRoute?.contains("AnalysisRoute") == true -> NavOptions.ANALYSIS
        currentRoute?.contains("BudgetRoute") == true -> NavOptions.BUDGET
        else -> null
    }

    SharedTransitionLayout {
        Scaffold(
            bottomBar = {
                if (selectedOption != null) {
                    BottomNavBar(selectedItem = selectedOption) { option ->
                        if (option == selectedOption) return@BottomNavBar

                        val route = when (option) {
                            NavOptions.HOME -> Routes.ExpensePresetRoute
                            NavOptions.LIST -> Routes.ExpensesRoute
                            NavOptions.ANALYSIS -> Routes.AnalysisRoute
                            NavOptions.BUDGET -> Routes.BudgetRoute
                        }
                        navController.navigate(route) {
                            popUpTo<Routes.ExpensePresetRoute> {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.AuthRoute,
                modifier = Modifier.padding(innerPadding),

                enterTransition = {
                    val srcIndex = getTabIndex(initialState.destination)
                    val dstIndex = getTabIndex(targetState.destination)
                    val offsetMultiplier = if (srcIndex != -1 && dstIndex != -1 && dstIndex < srcIndex) -1 else 1

                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth * offsetMultiplier },
                        animationSpec = tween(300)
                    )
                },
                exitTransition = {
                    val srcIndex = getTabIndex(initialState.destination)
                    val dstIndex = getTabIndex(targetState.destination)
                    val offsetMultiplier = if (srcIndex != -1 && dstIndex != -1 && dstIndex < srcIndex) 1 else -1

                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth * offsetMultiplier },
                        animationSpec = tween(300)
                    )
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(300)
                    )
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(300)
                    )
                }
            ) {
                composable<Routes.AuthRoute> {
                    val viewModel: AuthViewModel = koinViewModel()
                    AuthRoute(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
                composable<Routes.ExpensePresetRoute> {
                    val viewModel: ExpensePresetViewModel = koinViewModel()
                    ExpensePresetRoute(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
                composable<Routes.ExpensesRoute> {
                    val viewModel: ExpenseListViewModel = koinViewModel()
                    ExpenseListRoute(
                        viewModel = viewModel
                    )
                }
                composable<Routes.AnalysisRoute> {
                    val viewModel: AnalysisViewModel = koinViewModel()
                    AnalysisRoute(
                        viewModel = viewModel
                    )
                }
                composable<Routes.BudgetRoute> {
                    val viewModel: BudgetViewModel = koinViewModel()
                    BudgetRoute(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
                composable<Routes.BudgetDetailsRoute> {
                    val viewModel: BudgetDetailsViewModel = koinViewModel()
                    BudgetDetailsRoute(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

private fun getTabIndex(destination: NavDestination?): Int {
    return when {
        destination?.hasRoute<Routes.ExpensePresetRoute>() == true -> 0
        destination?.hasRoute<Routes.ExpensesRoute>() == true -> 1
        destination?.hasRoute<Routes.AnalysisRoute>() == true -> 2
        destination?.hasRoute<Routes.BudgetRoute>() == true -> 3
        else -> -1
    }
}
