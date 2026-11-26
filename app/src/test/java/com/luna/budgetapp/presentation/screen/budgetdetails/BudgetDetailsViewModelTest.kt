package com.luna.budgetapp.presentation.screen.budgetdetails

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MainDispatcherRule
import com.luna.budgetapp.domain.model.Budget
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.repository.FakeBudgetRepository
import com.luna.budgetapp.domain.repository.FakeExpenseRepository
import com.luna.budgetapp.domain.usecase.BudgetUseCases
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.usecase.budget.GetBudgetByIdUseCase
import com.luna.budgetapp.domain.usecase.expense.GetExpensesByDateRangeUseCase
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetDetailsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val budgetRepo = FakeBudgetRepository()
    private val expenseRepo = FakeExpenseRepository()

    private fun viewModel(extra: Map<String, Any?> = emptyMap()): BudgetDetailsViewModel {
        val budgetUseCases = BudgetUseCases(
            saveBudget = mockk(),
            saveBudgetForm = mockk(),
            getBudgetById = GetBudgetByIdUseCase(budgetRepo),
            updateBudget = mockk(),
            getAllBudget = mockk(),
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
            editExpense = mockk(),
        )
        return BudgetDetailsViewModel(
            SavedStateHandle(mapOf("budgetId" to 1L) + extra),
            budgetUseCases,
            expenseUseCases
        )
    }

    @Test
    fun `a failing read gives Error and Retry after the fault clears gives Success`() = runTest {
        // The budget does not exist yet, so the read flow throws.
        val vm = viewModel()
        val job = launch { vm.uiState.collect {} }
        runCurrent()
        assertThat(vm.uiState.value).isInstanceOf(UiState.Error::class.java)

        budgetRepo.insertBudget(Budget(name = "Food", limit = 1000L, frequency = DateFilter.Monthly, interactors = emptyList(), startDate = java.time.LocalDate.now()))
        vm.onEvent(Event.Retry)
        runCurrent()

        assertThat(vm.uiState.value).isInstanceOf(UiState.Success::class.java)
        job.cancel()
    }

    @Test
    fun `date range is restored from saved state`() = runTest {
        budgetRepo.insertBudget(Budget(name = "Food", limit = 1000L, frequency = DateFilter.Monthly, interactors = listOf(Category.FOOD), startDate = java.time.LocalDate.now()))
        expenseRepo.addExpense(Expense(amount = 100L, category = Category.FOOD, type = "Jan", date = java.time.LocalDateTime.of(2026, 1, 15, 12, 0)))
        val vm = viewModel(
            mapOf("range_start" to "2026-01-01T00:00", "range_end" to "2026-01-31T23:59:59.999999999")
        )
        val job = launch { vm.uiState.collect {} }
        runCurrent()
        val state = vm.uiState.value as UiState.Success
        assertThat(state.expenses.map { it.type }).containsExactly("Jan")
        job.cancel()
    }
}
