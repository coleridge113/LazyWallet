package com.luna.budgetapp.domain.usecase.expense

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.DailyTotal
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.repository.FakeExpenseRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ObserveAllDailyTotalsUseCaseTest {
    private val repo = FakeExpenseRepository()

    private fun expense(amount: Long, category: Category, date: LocalDateTime) =
        Expense(amount = amount, category = category, type = "t", date = date)

    @Test
    fun `sums every expense per local day regardless of category`() = runTest {
        repo.addExpense(expense(10_000L, Category.FOOD, LocalDateTime.of(2026, 11, 18, 10, 0)))
        repo.addExpense(expense(5_000L, Category.BEVERAGE, LocalDateTime.of(2026, 11, 18, 10, 0)))
        repo.addExpense(expense(3_000L, Category.FOOD, LocalDateTime.of(2026, 11, 19, 9, 0)))

        val totals = ObserveAllDailyTotalsUseCase(repo)().first()

        assertThat(totals).containsExactly(
            DailyTotal(LocalDate.of(2026, 11, 18), 15_000L),
            DailyTotal(LocalDate.of(2026, 11, 19), 3_000L),
        ).inOrder()
    }

    @Test
    fun `throws when the repository read fails`() = runTest {
        repo.shouldThrow = true
        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { ObserveAllDailyTotalsUseCase(repo)().first() }
        }
    }
}
