package com.luna.budgetapp.presentation.screen.analysis

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.domain.model.Granularity
import com.luna.budgetapp.domain.model.PeriodTotal
import com.luna.budgetapp.presentation.screen.analysis.components.SummaryLabel
import com.luna.budgetapp.presentation.screen.analysis.components.barLabel
import com.luna.budgetapp.presentation.screen.analysis.components.summaryLabel
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class PeriodLabelsTest {
    private val locale = Locale.US
    private val today = LocalDate.of(2026, 11, 26)

    private fun p(s: String, e: String) = PeriodTotal(LocalDate.parse(s), LocalDate.parse(e), 0)

    @Test
    fun barLabel_day() {
        assertThat(barLabel(LocalDate.of(2026, 11, 19), Granularity.DAY, locale)).isEqualTo("Thu 19")
    }

    @Test
    fun barLabel_week() {
        assertThat(barLabel(LocalDate.of(2026, 11, 16), Granularity.WEEK, locale)).isEqualTo("Nov 16")
    }

    @Test
    fun barLabel_month_january_showsYear() {
        assertThat(barLabel(LocalDate.of(2026, 1, 1), Granularity.MONTH, locale)).isEqualTo("Jan '26")
        assertThat(barLabel(LocalDate.of(2026, 11, 1), Granularity.MONTH, locale)).isEqualTo("Nov")
    }

    @Test
    fun summary_currentPeriods() {
        assertThat(summaryLabel(p("2026-11-26", "2026-11-26"), Granularity.DAY, today, locale)).isEqualTo(SummaryLabel.Today)
        assertThat(summaryLabel(p("2026-11-23", "2026-11-29"), Granularity.WEEK, today, locale)).isEqualTo(SummaryLabel.ThisWeek)
        assertThat(summaryLabel(p("2026-11-01", "2026-11-30"), Granularity.MONTH, today, locale)).isEqualTo(SummaryLabel.ThisMonth)
    }

    @Test
    fun summary_pastDay() {
        assertThat(summaryLabel(p("2026-11-19", "2026-11-19"), Granularity.DAY, today, locale))
            .isEqualTo(SummaryLabel.Text("Thu, Nov 19"))
    }

    @Test
    fun summary_pastWeek_sameMonth() {
        assertThat(summaryLabel(p("2026-11-09", "2026-11-15"), Granularity.WEEK, today, locale))
            .isEqualTo(SummaryLabel.Text("Nov 9 – 15"))
    }

    @Test
    fun summary_pastWeek_acrossMonths() {
        assertThat(summaryLabel(p("2025-12-29", "2026-01-04"), Granularity.WEEK, today, locale))
            .isEqualTo(SummaryLabel.Text("Dec 29 – Jan 4"))
    }

    @Test
    fun summary_pastMonth() {
        assertThat(summaryLabel(p("2026-10-01", "2026-10-31"), Granularity.MONTH, today, locale))
            .isEqualTo(SummaryLabel.Text("October 2026"))
    }
}
