package com.luna.budgetapp.presentation.screen.analysis.components

import com.luna.budgetapp.domain.model.Granularity
import com.luna.budgetapp.domain.model.PeriodTotal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface SummaryLabel {
    data object Today : SummaryLabel
    data object ThisWeek : SummaryLabel
    data object ThisMonth : SummaryLabel
    data class Text(val value: String) : SummaryLabel
}

private fun fmt(pattern: String, locale: Locale) = DateTimeFormatter.ofPattern(pattern, locale)

fun barLabel(start: LocalDate, granularity: Granularity, locale: Locale): String =
    when (granularity) {
        Granularity.DAY -> start.format(fmt("EEE d", locale))
        Granularity.WEEK -> start.format(fmt("MMM d", locale))
        Granularity.MONTH ->
            if (start.monthValue == 1) start.format(fmt("MMM ''yy", locale))
            else start.format(fmt("MMM", locale))
    }

fun summaryLabel(
    period: PeriodTotal,
    granularity: Granularity,
    today: LocalDate,
    locale: Locale,
): SummaryLabel {
    if (today in period.start..period.end) {
        return when (granularity) {
            Granularity.DAY -> SummaryLabel.Today
            Granularity.WEEK -> SummaryLabel.ThisWeek
            Granularity.MONTH -> SummaryLabel.ThisMonth
        }
    }
    val text = when (granularity) {
        Granularity.DAY -> period.start.format(fmt("EEE, MMM d", locale))
        Granularity.WEEK ->
            if (period.start.month == period.end.month && period.start.year == period.end.year) {
                "${period.start.format(fmt("MMM d", locale))} – ${period.end.dayOfMonth}"
            } else {
                "${period.start.format(fmt("MMM d", locale))} – ${period.end.format(fmt("MMM d", locale))}"
            }
        Granularity.MONTH -> period.start.format(fmt("MMMM yyyy", locale))
    }
    return SummaryLabel.Text(text)
}
