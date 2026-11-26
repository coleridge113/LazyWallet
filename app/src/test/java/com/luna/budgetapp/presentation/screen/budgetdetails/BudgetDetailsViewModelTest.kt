package com.luna.budgetapp.presentation.screen.budgetdetails

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MainDispatcherRule
import com.luna.budgetapp.domain.model.Budget
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

    private fun viewModel(): BudgetDetailsViewModel {
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
            editExpense = mockk(),
        )
        return BudgetDetailsViewModel(
            SavedStateHandle(mapOf("budgetId" to 1L)),
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
}
