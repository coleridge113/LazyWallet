package com.luna.budgetapp.presentation.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Routes.AuthRoute,
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
            composable<Routes.AddExpensesRoute> {
                val viewModel: ExpensePresetViewModel = koinViewModel()
                ExpensePresetRoute(
                    navController = navController,
                    viewModel = viewModel
                )
            }
            composable<Routes.ExpensesRoute> {
                val viewModel: ExpenseListViewModel = koinViewModel()
                ExpenseListRoute(
                    navController = navController,
                    viewModel = viewModel
                )
            }
            composable<Routes.AnalysisRoute> {
                val viewModel: AnalysisViewModel = koinViewModel()
                AnalysisRoute(
                    navController = navController,
                    viewModel = viewModel
                ) 
            }
        }
    }
}
