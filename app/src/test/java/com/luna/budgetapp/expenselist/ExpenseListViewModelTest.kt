package com.luna.budgetapp.expenselist

import androidx.paging.PagingData
import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MainDispatcherRule
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.domain.repository.FakeExpensePresetRepository
import com.luna.budgetapp.domain.repository.FakeExpenseRepository
import com.luna.budgetapp.domain.usecase.UseCases
import com.luna.budgetapp.domain.usecase.category.InitializeCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.expense.AddExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.DeleteExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.DeleteLatestExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.GetCategoryTotalsByDateRange
import com.luna.budgetapp.domain.usecase.expense.GetPagingExpensesByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expense.GetTotalAmountByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.AddExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.DeleteExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.GetAllExpensePresetsUseCase
import com.luna.budgetapp.presentation.screen.expenselist.Event
import com.luna.budgetapp.presentation.screen.expenselist.ExpenseListViewModel
import com.luna.budgetapp.presentation.screen.expensepreset.ExpensePresetViewModel
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeExpensePresetRepo: FakeExpensePresetRepository
    private lateinit var fakeExpenseRepo: FakeExpenseRepository
    private lateinit var viewModel: ExpenseListViewModel
    private lateinit var useCases: UseCases

    val dummyPreset = ExpensePreset(
        id = 1,
        amount = 10.0,
        category = "Food",
        type = "Lunch"
    )

    val dummyExpense = Expense(
        id = 1,
        amount = 10.0,
        category = "Food",
        type = "Lunch",
    )

    @Before
    fun setup() {
        fakeExpensePresetRepo = FakeExpensePresetRepository()
        fakeExpenseRepo = FakeExpenseRepository()

        useCases = UseCases(
            getToken = mockk(), // not used
            addExpense = AddExpenseUseCase(fakeExpenseRepo),
            deleteExpense = DeleteExpenseUseCase(fakeExpenseRepo),
            deleteLatestExpense = DeleteLatestExpenseUseCase(fakeExpenseRepo),
            getAllExpenses = mockk(),
            getExpensesByCategory = mockk(),
            getExpensesByDateRange = mockk(),
            getTotalAmountByDateRange = GetTotalAmountByDateRangeUseCase(fakeExpenseRepo),
            getCategoryTotalsByDateRange = GetCategoryTotalsByDateRange(fakeExpenseRepo),
            getPagingExpensesByDateRange = GetPagingExpensesByDateRangeUseCase(fakeExpenseRepo),
            getExpensesByType = mockk(),
            getAllExpensePresets = GetAllExpensePresetsUseCase(fakeExpensePresetRepo),
            addExpensePreset = AddExpensePresetUseCase(fakeExpensePresetRepo),
            deleteExpensePreset = DeleteExpensePresetUseCase(fakeExpensePresetRepo),
            getCategoryProfile = mockk(),
            getCategoryProfiles = mockk(),
            saveCategoryProfile = mockk(),
            deleteCategoryProfile = mockk(),
            initializeCategoryProfile = mockk(),
            getActiveCategoryProfile = mockk(),
            setActiveCategoryProfile = mockk(),
            getActiveDateFilter = mockk(),
            setActiveDateFilter = mockk()
        )

        coEvery { useCases.getActiveCategoryProfile() } returns flowOf("")
        coEvery { useCases.getCategoryProfile(any()) } returns flowOf(emptyList())
        coEvery { useCases.getCategoryProfiles() } returns flowOf(emptyList())

        viewModel = ExpenseListViewModel(useCases)
    }

    @Test
    fun `expense list is empty by default`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertThat(state.totalAmount).isEqualTo(0.0)
        assertThat(state.expenses).isEmpty()
    }

    @Test
    fun `adding an expense updates the total amount`() = runTest {
        fakeExpenseRepo.addExpense(dummyExpense)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.totalAmount).isEqualTo(dummyExpense.amount)
    }

    @Test
    fun `deleting an expense by its id deletes it`() = runTest {
        fakeExpenseRepo.addExpense(dummyExpense)
        advanceUntilIdle()
        val initial = viewModel.uiState.value
        assertThat(initial.totalAmount).isEqualTo(dummyExpense.amount)

        viewModel.onEvent(Event.DeleteExpense(dummyExpense.id!!))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.totalAmount).isEqualTo(0.0)
    }
}