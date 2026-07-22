package com.luna.budgetapp.presentation.nav

import kotlinx.serialization.Serializable

@Serializable
sealed interface Routes {

    @Serializable
    data object AuthRoute : Routes

    @Serializable
    data object MigrationRoute : Routes

    @Serializable
    data object ExpensePresetRoute : Routes

    @Serializable
    data object ExpensesRoute : Routes

    @Serializable
    data object AnalysisRoute : Routes

    @Serializable
    data object BudgetRoute : Routes

    @Serializable
    data object BudgetDetailsRoute : Routes
}
