package com.luna.budgetapp.domain.utils

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.domain.model.DailyTotal
import com.luna.budgetapp.domain.model.Granularity
import com.luna.budgetapp.domain.model.PeriodChange
import com.luna.budgetapp.domain.model.PeriodTotal
import org.junit.Test
import java.time.LocalDate

class SpendingRollupTest {
    private val today = LocalDate.of(2026, 11, 26)
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    @Test
    fun day_zeroFillsGaps() {
        val r = rollUp(listOf(DailyTotal(d(2026, 11, 23), 500), DailyTotal(d(2026, 11, 25), 300)), Granularity.DAY, today)
        assertThat(r.map { it.start }).containsExactly(d(2026, 11, 23), d(2026, 11, 24), d(2026, 11, 25), d(2026, 11, 26)).inOrder()
        assertThat(r.map { it.total }).containsExactly(500L, 0L, 300L, 0L).inOrder()
    }

    @Test
    fun week_mondayToSunday() {
        val r = rollUp(listOf(DailyTotal(d(2026, 11, 23), 200), DailyTotal(d(2026, 11, 22), 100)), Granularity.WEEK, today)
        assertThat(r).containsExactly(
            PeriodTotal(d(2026, 11, 16), d(2026, 11, 22), 100),
            PeriodTotal(d(2026, 11, 23), d(2026, 11, 29), 200)
        ).inOrder()
    }

    @Test
    fun week_spanningNewYear() {
        val r = rollUp(
            listOf(DailyTotal(d(2025, 12, 29), 10), DailyTotal(d(2026, 1, 2), 20)),
            Granularity.WEEK, d(2026, 1, 2)
        )
        assertThat(r).containsExactly(PeriodTotal(d(2025, 12, 29), d(2026, 1, 4), 30))
    }

    @Test
    fun month_acrossYearBoundary() {
        val r = rollUp(
            listOf(DailyTotal(d(2025, 12, 31), 10), DailyTotal(d(2026, 1, 1), 20)),
            Granularity.MONTH, d(2026, 1, 15)
        )
        assertThat(r).containsExactly(
            PeriodTotal(d(2025, 12, 1), d(2025, 12, 31), 10),
            PeriodTotal(d(2026, 1, 1), d(2026, 1, 31), 20)
        ).inOrder()
    }

    @Test
    fun noExpenses_singleCurrentPeriod() {
        assertThat(rollUp(emptyList(), Granularity.WEEK, today))
            .containsExactly(PeriodTotal(d(2026, 11, 23), d(2026, 11, 29), 0))
    }

    @Test
    fun newestExpenseOld_includesCurrentPeriod() {
        val r = rollUp(listOf(DailyTotal(d(2026, 9, 10), 50)), Granularity.MONTH, today)
        assertThat(r.map { it.start }).containsExactly(d(2026, 9, 1), d(2026, 10, 1), d(2026, 11, 1)).inOrder()
        assertThat(r.map { it.total }).containsExactly(50L, 0L, 0L).inOrder()
    }

    private fun p(total: Long) = PeriodTotal(today, today, total)

    @Test
    fun change_normal() = assertThat(change(p(150), p(100))).isEqualTo(PeriodChange(50, 50.0))

    @Test
    fun change_previousZero() = assertThat(change(p(150), p(0)).percent).isNull()

    @Test
    fun change_noPrevious() = assertThat(change(p(150), null)).isEqualTo(PeriodChange(150, null))

    @Test
    fun change_decrease() = assertThat(change(p(80), p(100))).isEqualTo(PeriodChange(-20, -20.0))
}
