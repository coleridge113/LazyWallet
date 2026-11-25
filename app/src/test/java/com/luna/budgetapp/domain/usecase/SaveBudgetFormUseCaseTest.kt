package com.luna.budgetapp.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MutableClock
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.repository.FakeBudgetRepository
import com.luna.budgetapp.domain.usecase.budget.SaveBudgetFormUseCase
import com.luna.budgetapp.domain.usecase.budget.SaveBudgetUseCase
import com.luna.budgetapp.domain.usecase.budget.UpdateBudgetUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class SaveBudgetFormUseCaseTest {
    private val repo = FakeBudgetRepository()
    private val clock = MutableClock(Instant.parse("2026-03-10T12:00:00Z"))
    private val useCase = SaveBudgetFormUseCase(SaveBudgetUseCase(repo), UpdateBudgetUseCase(repo), clock)
    private val categories = mapOf(Category.FOOD to true, Category.BILLS to false)

    @Test
    fun `build converts amount to cents, keeps selected categories and uses clock date`() {
        val budget = useCase.build(0, "Food", "10", DateFilter.Monthly, categories)

        assertThat(budget.limit).isEqualTo(1000L)
        assertThat(budget.interactors).containsExactly(Category.FOOD)
        assertThat(budget.startDate).isEqualTo(LocalDate.of(2026, 3, 10))
    }

    @Test
    fun `id 0 inserts and non-zero id updates`() = runTest {
        useCase(0, "Food", "10", DateFilter.Monthly, categories)
        val saved = repo.getBudgets().first().single()
        assertThat(saved.id).isEqualTo(1L)

        useCase(saved.id, "Food2", "20", DateFilter.Weekly, categories)
        val updated = repo.getBudgets().first().single()
        assertThat(updated.name).isEqualTo("Food2")
        assertThat(updated.limit).isEqualTo(2000L)
    }
}
