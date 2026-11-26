package com.luna.budgetapp.presentation.screen.budget.model

import com.luna.budgetapp.domain.model.DailyTotal
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

enum class DayKind { Past, Today, Future }

/** [intensity] null means no fill (zero-spend or future day). */
data class OutlookDay(
    val date: LocalDate,
    val spent: Long,
    val kind: DayKind,
    val intensity: Float?,
)

data class MonthOutlook(
    val today: LocalDate,
    val budgetTotal: Long,
    val days: List<OutlookDay>,
    val spentSoFar: Long,
    val remaining: Long,
    val daysElapsed: Int,
    val daysInMonth: Int,
    val daysLeft: Int,
    val projected: Long?,
    val avgPerDay: Long,
    val highest: OutlookDay?,
    val leadingBlanks: Int,
    val chartMax: Long,
) {
    val hasBudgets: Boolean get() = budgetTotal > 0

    /** Sum of spend from day 1 through [date], clamped to today. */
    fun runningTotal(date: LocalDate): Long {
        val limit = if (date.isAfter(today)) today else date
        return days.filter { !it.date.isAfter(limit) }.sumOf { it.spent }
    }

    companion object {
        private const val MIN_INTENSITY = 0.38f

        fun from(today: LocalDate, budgetTotal: Long, dailyTotals: List<DailyTotal>): MonthOutlook {
            val daysInMonth = today.lengthOfMonth()
            val byDate = dailyTotals
                .filter { it.date.year == today.year && it.date.month == today.month }
                .groupBy { it.date }
                .mapValues { (_, v) -> v.sumOf { it.total } }

            val maxSpent = (1..today.dayOfMonth)
                .maxOfOrNull { byDate[today.withDayOfMonth(it)] ?: 0L } ?: 0L

            val days = (1..daysInMonth).map { d ->
                val date = today.withDayOfMonth(d)
                val kind = when {
                    date.isBefore(today) -> DayKind.Past
                    date == today -> DayKind.Today
                    else -> DayKind.Future
                }
                val spent = if (kind == DayKind.Future) 0L else byDate[date] ?: 0L
                val intensity = if (kind == DayKind.Future || spent <= 0L || maxSpent <= 0L) null
                else MIN_INTENSITY + (1f - MIN_INTENSITY) * (spent.toFloat() / maxSpent)
                OutlookDay(date, spent, kind, intensity)
            }

            val daysElapsed = today.dayOfMonth
            val spentSoFar = days.sumOf { it.spent }
            val projected = if (daysElapsed < 3) null
            else BigDecimal(spentSoFar).multiply(BigDecimal(daysInMonth))
                .divide(BigDecimal(daysElapsed), 0, RoundingMode.HALF_UP).toLong()
            val avgPerDay = BigDecimal(spentSoFar)
                .divide(BigDecimal(daysElapsed), 0, RoundingMode.HALF_UP).toLong()
            val highest = days
                .filter { it.kind != DayKind.Future && it.spent > 0 }
                .fold(null as OutlookDay?) { best, d -> if (best == null || d.spent > best.spent) d else best }

            return MonthOutlook(
                today = today,
                budgetTotal = budgetTotal,
                days = days,
                spentSoFar = spentSoFar,
                remaining = budgetTotal - spentSoFar,
                daysElapsed = daysElapsed,
                daysInMonth = daysInMonth,
                daysLeft = daysInMonth - daysElapsed,
                projected = projected,
                avgPerDay = avgPerDay,
                highest = highest,
                leadingBlanks = today.withDayOfMonth(1).dayOfWeek.value % 7,
                chartMax = maxOf(budgetTotal, projected ?: 0L, spentSoFar),
            )
        }
    }
}
