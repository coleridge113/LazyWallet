package com.luna.budgetapp.presentation.screen.budget.model

import java.time.LocalDate

data class OutlookDetails(
    val income: Long,
    val projectedSpend: Long,
    val actualSpend: Long,
    val paceFraction: Float = 0f
) {
    val remaining = projectedSpend - actualSpend

    /** Unclamped actual/projected; null when there are no budgets. */
    val usedFraction: Float? =
        if (projectedSpend > 0) actualSpend.toFloat() / projectedSpend else null
}

/** How far through the month [today] is (dayOfMonth / lengthOfMonth). */
fun paceFraction(today: LocalDate): Float =
    today.dayOfMonth.toFloat() / today.lengthOfMonth()
