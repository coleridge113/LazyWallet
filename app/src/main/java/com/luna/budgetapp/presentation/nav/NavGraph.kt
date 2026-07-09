package com.luna.budgetapp.presentation.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
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

@ExperimentalMaterial3Api
@ExperimentalSharedTransitionApi
@Composable
fun NavGraphSetup(
    navController: NavHostController,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val selectedOption = when {
        currentDestination?.hasRoute<Routes.ExpensePresetRoute>() == true -> NavOptions.HOME
        currentDestination?.hasRoute<Routes.ExpensesRoute>() == true -> NavOptions.LIST
        currentDestination?.hasRoute<Routes.AnalysisRoute>() == true -> NavOptions.ANALYSIS
        else -> null
    }

    SharedTransitionLayout {
        Scaffold(
            bottomBar = {
                if (selectedOption != null) {
                    BottomNavBar(selectedItem = selectedOption) { option ->
                        val route = when (option) {
                            NavOptions.HOME -> Routes.ExpensePresetRoute
                            NavOptions.LIST -> Routes.ExpensesRoute
                            NavOptions.ANALYSIS -> Routes.AnalysisRoute
                        }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
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
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
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
            }
        }
    }
}
