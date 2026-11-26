package com.luna.budgetapp.presentation.screen.expensepreset.model

import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.PeriodTotal
import java.time.Duration
import java.time.LocalDateTime

private const val SPARKLINE_DAYS = 7

/** Exactly seven daily totals, oldest first, ending with the last period; left-padded with 0. */
fun lastSevenDays(periods: List<PeriodTotal>): List<Long> {
    val totals = periods.takeLast(SPARKLINE_DAYS).map { it.total }
    return List(SPARKLINE_DAYS - totals.size) { 0L } + totals
}

/** The expense with the highest non-null id, or null. */
fun lastLogged(todayExpenses: List<Expense>): Expense? =
    todayExpenses.filter { it.id != null }.maxByOrNull { it.id!! }

/** Sorted by id descending; expenses without an id go last. */
fun recentFirst(todayExpenses: List<Expense>): List<Expense> =
    todayExpenses.sortedWith(compareByDescending<Expense> { it.id != null }.thenByDescending { it.id ?: 0L })

sealed interface RelativeTime {
    data object JustNow : RelativeTime
    data class Minutes(val n: Long) : RelativeTime
    data class Hours(val n: Long) : RelativeTime
    data object None : RelativeTime
}

fun relativeTime(added: LocalDateTime, now: LocalDateTime): RelativeTime {
    val gap = Duration.between(added, now)
    return when {
        gap.seconds < 60 -> RelativeTime.JustNow
        gap.toMinutes() < 60 -> RelativeTime.Minutes(gap.toMinutes())
        gap.toHours() < 24 -> RelativeTime.Hours(gap.toHours())
        else -> RelativeTime.None
    }
}
