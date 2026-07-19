package com.luna.budgetapp.presentation.screen.budget.model

data class OutlookDetails(
    val income: Long,
    val projectedSpend: Long,
    val actualSpend: Long
) {
    val remaining = projectedSpend - actualSpend
}
