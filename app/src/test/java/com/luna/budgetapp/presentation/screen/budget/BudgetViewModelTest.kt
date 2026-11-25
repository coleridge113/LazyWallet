package com.luna.budgetapp.presentation.screen.budget

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MainDispatcherRule
import com.luna.budgetapp.MutableClock
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.repository.FakeBudgetRepository
import com.luna.budgetapp.domain.repository.FakeExpenseRepository
import com.luna.budgetapp.domain.usecase.BudgetUseCases
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.usecase.budget.SaveBudgetFormUseCase
import com.luna.budgetapp.domain.usecase.budget.SaveBudgetUseCase
import com.luna.budgetapp.domain.usecase.budget.UpdateBudgetUseCase
import com.luna.budgetapp.domain.usecase.expense.GetExpensesByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expense.GetTotalAmountByDateRangeUseCase
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val budgetRepo = FakeBudgetRepository()
    private val expenseRepo = FakeExpenseRepository()
    private val clock = MutableClock(Instant.parse("2026-03-31T23:59:00Z"))

    private fun viewModel(): BudgetViewModel {
        val budgetUseCases = BudgetUseCases(
            saveBudget = SaveBudgetUseCase(budgetRepo),
            saveBudgetForm = SaveBudgetFormUseCase(
                SaveBudgetUseCase(budgetRepo), UpdateBudgetUseCase(budgetRepo), clock
            ),
            getBudgetById = mockk(),
            updateBudget = UpdateBudgetUseCase(budgetRepo),
            getAllBudget = com.luna.budgetapp.domain.usecase.budget.GetAllBudgetUseCase(budgetRepo),
            deleteBudget = mockk(),
        )
        val expenseUseCases = ExpenseUseCases(
            addExpense = mockk(),
            deleteExpense = mockk(),
            deleteLatestExpense = mockk(),
            getAllExpenses = mockk(),
            getExpensesByCategory = mockk(),
            getExpensesByDateRange = GetExpensesByDateRangeUseCase(expenseRepo),
            getTotalAmountByDateRange = GetTotalAmountByDateRangeUseCase(expenseRepo),
            getCategoryTotalsByDateRange = mockk(),
            getPagingExpensesByDateRange = mockk(),
            getExpensesByType = mockk(),
            editExpense = mockk(),
        )
        return BudgetViewModel(budgetUseCases, expenseUseCases, clock)
    }

    @Test
    fun `confirming the budget form saves a budget built from the form`() = runTest {
        val vm = viewModel()
        val job = launch { vm.uiState.collect {} }
        runCurrent()

        vm.onEvent(
            Event.ConfirmBudgetFormDialog(
                id = 0, name = "Food", amount = "10",
                frequency = DateFilter.Monthly, categoryMap = mapOf(Category.FOOD to true)
            )
        )

        runCurrent()
        val state = vm.uiState.value as UiState.Success
        assertThat(state.budgets.single().limit).isEqualTo(1000L)
        job.cancel()
    }

    @Test
    fun `monthly actual spend rolls over at midnight`() = runTest {
        expenseRepo.addExpense(
            Expense(amount = 700L, category = Category.FOOD, type = "Mar", date = LocalDateTime.of(2026, 3, 31, 10, 0))
        )
        val vm = viewModel()
        val job = launch { vm.uiState.collect {} }
        advanceTimeBy(1)
        assertThat((vm.uiState.value as UiState.Success).monthlyOutlook.actualSpend).isEqualTo(700L)

        clock.current = Instant.parse("2026-04-01T00:00:30Z")
        advanceTimeBy(61_000)
        assertThat((vm.uiState.value as UiState.Success).monthlyOutlook.actualSpend).isEqualTo(0L)
        job.cancel()
    }
}
