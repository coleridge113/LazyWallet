package com.luna.budgetapp.presentation.screen.budget

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MainDispatcherRule
import com.luna.budgetapp.MutableClock
import com.luna.budgetapp.domain.model.Budget
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
import com.luna.budgetapp.domain.usecase.expense.ObserveAllDailyTotalsUseCase
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
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
            getTotalAmountByDateRange = mockk(),
            getCategoryTotalsByDateRange = mockk(),
            getPagingExpensesByDateRange = mockk(),
            getExpensesByType = mockk(),
            observeSpendingHistory = mockk(),
            observeAllDailyTotals = ObserveAllDailyTotalsUseCase(expenseRepo),
            editExpense = mockk(),
        )
        return BudgetViewModel(budgetUseCases, expenseUseCases, clock)
    }

    @Test
    fun `a failing read gives Error and Retry after the fault clears gives Success`() = runTest {
        expenseRepo.shouldThrow = true
        val vm = viewModel()
        val job = launch { vm.uiState.collect {} }
        runCurrent()
        assertThat(vm.uiState.value).isInstanceOf(UiState.Error::class.java)

        expenseRepo.shouldThrow = false
        vm.onEvent(Event.Retry)
        runCurrent()

        assertThat(vm.uiState.value).isInstanceOf(UiState.Success::class.java)
        job.cancel()
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
    fun `success carries the month outlook from all spending`() = runTest {
        clock.current = Instant.parse("2026-03-20T12:00:00Z")
        expenseRepo.addExpense(
            Expense(amount = 700L, category = Category.FOOD, type = "a", date = LocalDateTime.of(2026, 3, 5, 10, 0))
        )
        expenseRepo.addExpense(
            Expense(amount = 300L, category = Category.COMMUTE, type = "b", date = LocalDateTime.of(2026, 3, 10, 10, 0))
        )
        val vm = viewModel()
        val job = launch { vm.uiState.collect {} }
        runCurrent()

        assertThat((vm.uiState.value as UiState.Success).outlook.spentSoFar).isEqualTo(1000L)
        job.cancel()
    }

    @Test
    fun `outlook switches month at midnight`() = runTest {
        expenseRepo.addExpense(
            Expense(amount = 700L, category = Category.FOOD, type = "Mar", date = LocalDateTime.of(2026, 3, 31, 10, 0))
        )
        val vm = viewModel()
        val job = launch { vm.uiState.collect {} }
        runCurrent()
        assertThat((vm.uiState.value as UiState.Success).outlook.spentSoFar).isEqualTo(700L)

        clock.current = Instant.parse("2026-04-01T00:00:30Z")
        advanceTimeBy(120_000)
        runCurrent()
        val outlook = (vm.uiState.value as UiState.Success).outlook
        assertThat(outlook.today).isEqualTo(LocalDate.of(2026, 4, 1))
        assertThat(outlook.spentSoFar).isEqualTo(0L)
        assertThat(outlook.daysInMonth).isEqualTo(30)
        job.cancel()
    }

    @Test
    fun `budget total is the monthly projection of all budgets`() = runTest {
        budgetRepo.insertBudget(
            Budget(
                limit = 100_000L, name = "Food", frequency = DateFilter.Monthly,
                interactors = listOf(Category.FOOD), startDate = LocalDate.of(2026, 3, 1)
            )
        )
        val vm = viewModel()
        val job = launch { vm.uiState.collect {} }
        runCurrent()

        assertThat((vm.uiState.value as UiState.Success).outlook.budgetTotal).isEqualTo(100_000L)
        job.cancel()
    }
}
