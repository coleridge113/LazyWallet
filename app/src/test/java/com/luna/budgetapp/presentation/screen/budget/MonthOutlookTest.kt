package com.luna.budgetapp.presentation.screen.budget

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.domain.model.DailyTotal
import com.luna.budgetapp.presentation.screen.budget.model.DayKind
import com.luna.budgetapp.presentation.screen.budget.model.MonthOutlook
import com.luna.budgetapp.presentation.screen.utils.toWholePeso
import org.junit.Test
import java.time.LocalDate

class MonthOutlookTest {
    private fun day(d: Int, pesos: Int) = DailyTotal(LocalDate.of(2026, 11, d), pesos * 100L)
    private val nov26 = LocalDate.of(2026, 11, 26)

    @Test fun `mockup figures`() {
        // 25 days of 400 pesos = 10,000 + day 26 of 2,300 = 12,300
        val totals = (1..25).map { day(it, 400) } + day(26, 2300)
        val o = MonthOutlook.from(nov26, 2_800_000, totals)
        assertThat(o.spentSoFar).isEqualTo(1_230_000)
        assertThat(o.remaining).isEqualTo(1_570_000)
        assertThat(o.projected).isEqualTo(1_419_231)
        assertThat(o.avgPerDay).isEqualTo(47_308)
        assertThat(o.daysLeft).isEqualTo(4)
        assertThat(o.daysInMonth).isEqualTo(30)
        assertThat(o.projected!!.toWholePeso()).isEqualTo("₱14,192")
        assertThat(o.remaining.toWholePeso()).isEqualTo("₱15,700")
    }

    @Test fun `projected is null on day 1 and 2 and present on day 3`() {
        val t = listOf(day(1, 100), day(2, 100), day(3, 100))
        assertThat(MonthOutlook.from(LocalDate.of(2026, 11, 1), 1000, t).projected).isNull()
        assertThat(MonthOutlook.from(LocalDate.of(2026, 11, 2), 1000, t).projected).isNull()
        assertThat(MonthOutlook.from(LocalDate.of(2026, 11, 3), 1000, t).projected).isNotNull()
    }

    @Test fun `intensity is 1 for the biggest day, at least 0_38 for the smallest spend day, null for zero days`() {
        val o = MonthOutlook.from(nov26, 0, listOf(day(1, 1), day(2, 1000), day(3, 500)))
        assertThat(o.days[1].intensity).isEqualTo(1f)
        assertThat(o.days[0].intensity!!).isAtLeast(0.38f)
        assertThat(o.days[3].intensity).isNull()
        assertThat(o.days[27].intensity).isNull()
    }

    @Test fun `future and previous-month totals are ignored`() {
        val t = listOf(
            day(5, 100),
            day(28, 999),
            DailyTotal(LocalDate.of(2026, 10, 31), 99_900),
        )
        val o = MonthOutlook.from(nov26, 0, t)
        assertThat(o.spentSoFar).isEqualTo(10_000)
        assertThat(o.days[27].spent).isEqualTo(0)
        assertThat(o.days[27].kind).isEqualTo(DayKind.Future)
        assertThat(o.days[25].kind).isEqualTo(DayKind.Today)
        assertThat(o.days[0].kind).isEqualTo(DayKind.Past)
    }

    @Test fun `running total includes the given day`() {
        val t = (1..26).map { day(it, it) }
        val o = MonthOutlook.from(nov26, 0, t)
        assertThat(o.runningTotal(LocalDate.of(2026, 11, 18))).isEqualTo((1..18).sum() * 100L)
        assertThat(o.runningTotal(LocalDate.of(2026, 11, 29))).isEqualTo(o.spentSoFar)
    }

    @Test fun `leading blanks are Sunday-first`() {
        assertThat(MonthOutlook.from(nov26, 0, emptyList()).leadingBlanks).isEqualTo(0)
        assertThat(MonthOutlook.from(LocalDate.of(2026, 2, 10), 0, emptyList()).leadingBlanks).isEqualTo(0)
        assertThat(MonthOutlook.from(LocalDate.of(2026, 12, 10), 0, emptyList()).leadingBlanks).isEqualTo(2)
    }

    @Test fun `month lengths`() {
        assertThat(MonthOutlook.from(LocalDate.of(2026, 2, 10), 0, emptyList()).days).hasSize(28)
        assertThat(MonthOutlook.from(LocalDate.of(2026, 12, 10), 0, emptyList()).days).hasSize(31)
    }

    @Test fun `over budget gives negative remaining and chartMax covers projected`() {
        val today = LocalDate.of(2026, 11, 10)
        val o = MonthOutlook.from(today, 100_000, listOf(day(10, 1500)))
        assertThat(o.remaining).isEqualTo(-50_000)
        assertThat(o.projected).isEqualTo(450_000)
        assertThat(o.chartMax).isEqualTo(450_000)
    }

    @Test fun `no budgets`() {
        val o = MonthOutlook.from(nov26, 0, listOf(day(3, 100)))
        assertThat(o.hasBudgets).isFalse()
        assertThat(o.chartMax).isEqualTo(maxOf(o.projected!!, o.spentSoFar))
    }

    @Test fun `highest tie picks the earliest day and is null with no spend`() {
        val o = MonthOutlook.from(nov26, 0, listOf(day(4, 50), day(9, 50), day(2, 10)))
        assertThat(o.highest!!.date).isEqualTo(LocalDate.of(2026, 11, 4))
        assertThat(MonthOutlook.from(nov26, 0, emptyList()).highest).isNull()
    }

    @Test fun `toWholePeso rounds half up and drops the sign`() {
        assertThat(1_419_231L.toWholePeso()).isEqualTo("₱14,192")
        assertThat(50L.toWholePeso()).isEqualTo("₱1")
        assertThat((-123_400L).toWholePeso()).isEqualTo("₱1,234")
    }
}
