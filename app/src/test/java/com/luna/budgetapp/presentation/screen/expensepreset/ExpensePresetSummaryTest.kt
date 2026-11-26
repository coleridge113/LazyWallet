package com.luna.budgetapp.presentation.screen.expensepreset

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.PeriodTotal
import com.luna.budgetapp.presentation.screen.expensepreset.model.RelativeTime
import com.luna.budgetapp.presentation.screen.expensepreset.model.lastLogged
import com.luna.budgetapp.presentation.screen.expensepreset.model.lastSevenDays
import com.luna.budgetapp.presentation.screen.expensepreset.model.recentFirst
import com.luna.budgetapp.presentation.screen.expensepreset.model.relativeTime
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ExpensePresetSummaryTest {
    private fun periods(vararg totals: Long): List<PeriodTotal> {
        val base = LocalDate.of(2026, 1, 1)
        return totals.mapIndexed { i, t -> PeriodTotal(base.plusDays(i.toLong()), base.plusDays(i.toLong()), t) }
    }

    private fun expense(id: Long?) =
        Expense(id = id, amount = 100L, category = Category.FOOD, type = "x")

    @Test
    fun `lastSevenDays pads a short history with zeros`() {
        assertThat(lastSevenDays(periods(500, 0, 300)))
            .containsExactly(0L, 0L, 0L, 0L, 500L, 0L, 300L).inOrder()
    }

    @Test
    fun `lastSevenDays keeps only the newest seven`() {
        assertThat(lastSevenDays(periods(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
            .containsExactly(4L, 5L, 6L, 7L, 8L, 9L, 10L).inOrder()
    }

    @Test
    fun `lastSevenDays of empty is seven zeros`() {
        assertThat(lastSevenDays(emptyList())).containsExactly(0L, 0L, 0L, 0L, 0L, 0L, 0L)
    }

    @Test
    fun `lastLogged picks the highest id and skips null ids`() {
        assertThat(lastLogged(listOf(expense(3), expense(null), expense(7), expense(5)))?.id).isEqualTo(7L)
        assertThat(lastLogged(listOf(expense(null), expense(null)))).isNull()
    }

    @Test
    fun `recentFirst orders by id descending`() {
        assertThat(recentFirst(listOf(expense(3), expense(7), expense(5))).map { it.id })
            .containsExactly(7L, 5L, 3L).inOrder()
    }

    @Test
    fun `recentFirst puts null ids last`() {
        assertThat(recentFirst(listOf(expense(null), expense(2), expense(9))).map { it.id })
            .containsExactly(9L, 2L, null).inOrder()
    }

    @Test
    fun `relativeTime buckets`() {
        val now = LocalDateTime.of(2026, 11, 26, 18, 0, 0)
        assertThat(relativeTime(now.minusSeconds(30), now)).isEqualTo(RelativeTime.JustNow)
        assertThat(relativeTime(now.minusSeconds(59), now)).isEqualTo(RelativeTime.JustNow)
        assertThat(relativeTime(now.minusSeconds(60), now)).isEqualTo(RelativeTime.Minutes(1))
        assertThat(relativeTime(now.minusMinutes(59), now)).isEqualTo(RelativeTime.Minutes(59))
        assertThat(relativeTime(now.minusMinutes(60), now)).isEqualTo(RelativeTime.Hours(1))
        assertThat(relativeTime(now.minusHours(23), now)).isEqualTo(RelativeTime.Hours(23))
        assertThat(relativeTime(now.minusHours(24), now)).isEqualTo(RelativeTime.None)
        assertThat(relativeTime(now.plusSeconds(10), now)).isEqualTo(RelativeTime.JustNow)
    }
}
