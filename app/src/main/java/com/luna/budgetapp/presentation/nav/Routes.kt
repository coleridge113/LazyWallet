package com.luna.budgetapp.presentation.nav

import kotlinx.serialization.Serializable

@Serializable
sealed interface Routes {

    @Serializable
    data object AuthRoute : Routes

    @Serializable
    data object AddExpensesRoute : Routes

    @Serializable
    data object ExpensesRoute : Routes

    @Serializable
    data object AnalysisRoute : Routes
}
