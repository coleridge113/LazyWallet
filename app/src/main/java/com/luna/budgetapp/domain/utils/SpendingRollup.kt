package com.luna.budgetapp.domain.utils

import com.luna.budgetapp.domain.model.DailyTotal
import com.luna.budgetapp.domain.model.Granularity
import com.luna.budgetapp.domain.model.PeriodChange
import com.luna.budgetapp.domain.model.PeriodTotal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

fun LocalDate.periodStart(granularity: Granularity): LocalDate = when (granularity) {
    Granularity.DAY -> this
    Granularity.WEEK -> with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Granularity.MONTH -> withDayOfMonth(1)
}

fun periodEnd(start: LocalDate, granularity: Granularity): LocalDate = when (granularity) {
    Granularity.DAY -> start
    Granularity.WEEK -> start.plusDays(6)
    Granularity.MONTH -> start.with(TemporalAdjusters.lastDayOfMonth())
}

private fun nextStart(start: LocalDate, granularity: Granularity): LocalDate = when (granularity) {
    Granularity.DAY -> start.plusDays(1)
    Granularity.WEEK -> start.plusWeeks(1)
    Granularity.MONTH -> start.plusMonths(1)
}

/** Oldest first; every period from the one holding the earliest day through the one holding [today]. */
fun rollUp(daily: List<DailyTotal>, granularity: Granularity, today: LocalDate): List<PeriodTotal> {
    val totals = HashMap<LocalDate, Long>()
    daily.forEach { d ->
        val key = d.date.periodStart(granularity)
        totals[key] = (totals[key] ?: 0L) + d.total
    }
    val first = (daily.minOfOrNull { it.date } ?: today).periodStart(granularity)
    val last = today.periodStart(granularity)
    val result = ArrayList<PeriodTotal>()
    var start = first
    while (!start.isAfter(last)) {
        result += PeriodTotal(start, periodEnd(start, granularity), totals[start] ?: 0L)
        start = nextStart(start, granularity)
    }
    return result
}

fun change(selected: PeriodTotal, previous: PeriodTotal?): PeriodChange {
    val amount = selected.total - (previous?.total ?: 0L)
    val percent = if (previous != null && previous.total != 0L) amount * 100.0 / previous.total else null
    return PeriodChange(amount, percent)
}
