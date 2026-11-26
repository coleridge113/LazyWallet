package com.luna.budgetapp.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MutableClock
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.Granularity
import com.luna.budgetapp.domain.repository.FakeExpenseRepository
import com.luna.budgetapp.domain.usecase.expense.ObserveSpendingHistoryUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class ObserveSpendingHistoryUseCaseTest {
    private val clock = MutableClock(Instant.parse("2026-11-26T10:00:00Z"))
    private val repo = FakeExpenseRepository()
    private val useCase = ObserveSpendingHistoryUseCase(repo, clock)
    private val now = LocalDateTime.of(2026, 11, 26, 9, 0)

    private fun expense(amount: Long, category: Category, date: LocalDateTime) =
        Expense(amount = amount, category = category, type = "t", date = date)

    @Test
    fun filtersByCategory() = runTest {
        repo.addExpense(expense(100, Category.FOOD, now))
        repo.addExpense(expense(50, Category.COMMUTE, now))
        val h = useCase(listOf("FOOD"), Granularity.DAY).first()
        assertThat(h.today).isEqualTo(LocalDate.of(2026, 11, 26))
        assertThat(h.periods.last().total).isEqualTo(100L)
    }

    @Test
    fun emptyCategories_allZero() = runTest {
        repo.addExpense(expense(100, Category.FOOD, now))
        repo.addExpense(expense(50, Category.COMMUTE, now))
        val h = useCase(emptyList(), Granularity.DAY).first()
        assertThat(h.periods.map { it.total }.all { it == 0L }).isTrue()
    }

    @Test
    fun groupsByLocalDate() = runTest {
        repo.addExpense(expense(100, Category.FOOD, LocalDateTime.of(2026, 11, 25, 8, 0)))
        repo.addExpense(expense(40, Category.FOOD, LocalDateTime.of(2026, 11, 25, 23, 30)))
        val h = useCase(listOf("FOOD"), Granularity.DAY).first()
        val periods = h.periods.filter { it.start == LocalDate.of(2026, 11, 25) }
        assertThat(periods).hasSize(1)
        assertThat(periods.single().total).isEqualTo(140L)
    }
}
