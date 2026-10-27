package com.luna.budgetapp.expenselist

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MainDispatcherRule
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.repository.FakeCategoryRepository
import com.luna.budgetapp.domain.repository.FakeExpensePresetRepository
import com.luna.budgetapp.domain.repository.FakeExpenseRepository
import com.luna.budgetapp.domain.repository.FakeSettingsRepository
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.usecase.ProfileUseCases
import com.luna.budgetapp.domain.usecase.category.DeleteCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.category.GetCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.category.GetCategoryProfilesUseCase
import com.luna.budgetapp.domain.usecase.category.InitializeCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.category.SaveCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.expense.AddExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.DeleteExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.DeleteLatestExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.EditExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.GetTotalAmountByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.settings.GetActiveCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.settings.SetActiveCategoryProfileUseCase
import com.luna.budgetapp.presentation.screen.expenselist.DialogState
import com.luna.budgetapp.presentation.screen.expenselist.Event
import com.luna.budgetapp.presentation.screen.expenselist.ExpenseListViewModel
import com.luna.budgetapp.presentation.screen.expenselist.UiState
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeExpenseRepo: FakeExpenseRepository
    private lateinit var fakeSettingsRepo: FakeSettingsRepository
    private lateinit var fakeCategoryRepo: FakeCategoryRepository
    private lateinit var viewModel: ExpenseListViewModel

    val dummyExpense = Expense(
        id = 1L,
        amount = 1000L,
        category = Category.FOOD,
        type = "Lunch",
    )

    @Before
    fun setup() = runTest {
        fakeExpenseRepo = FakeExpenseRepository()
        fakeSettingsRepo = FakeSettingsRepository()
        fakeCategoryRepo = FakeCategoryRepository()

        val expenseUseCases = ExpenseUseCases(
            addExpense = AddExpenseUseCase(fakeExpenseRepo),
            deleteExpense = DeleteExpenseUseCase(fakeExpenseRepo),
            deleteLatestExpense = DeleteLatestExpenseUseCase(fakeExpenseRepo),
            getAllExpenses = mockk(),
            getExpensesByCategory = mockk(),
            getExpensesByDateRange = mockk(),
            getTotalAmountByDateRange = GetTotalAmountByDateRangeUseCase(fakeExpenseRepo),
            getCategoryTotalsByDateRange = mockk(relaxed = true) {
                io.mockk.coEvery { this@mockk.invoke(any(), any(), any()) } returns flowOf(emptyList())
            },
            getPagingExpensesByDateRange = mockk(relaxed = true),
            getExpensesByType = mockk(),
            editExpense = EditExpenseUseCase(fakeExpenseRepo),
        )
        val profileUseCases = ProfileUseCases(
            getCategoryProfile = GetCategoryProfileUseCase(fakeCategoryRepo),
            getCategoryProfiles = GetCategoryProfilesUseCase(fakeCategoryRepo),
            saveCategoryProfile = SaveCategoryProfileUseCase(fakeCategoryRepo),
            deleteCategoryProfile = DeleteCategoryProfileUseCase(fakeCategoryRepo),
            initializeCategoryProfile = InitializeCategoryProfileUseCase(fakeCategoryRepo),
            getActiveCategoryProfile = GetActiveCategoryProfileUseCase(fakeSettingsRepo),
            setActiveCategoryProfile = SetActiveCategoryProfileUseCase(fakeSettingsRepo),
        )

        fakeCategoryRepo.initializeIfNeeded()
        fakeSettingsRepo.setActiveProfile("All")
        viewModel = ExpenseListViewModel(
            expenseUseCases,
            profileUseCases
        )

        advanceUntilIdle()
    }

    @Test
    fun `expense list is empty by default`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value as UiState.Success
        assertThat(state.expensesState.totalAmount).isEqualTo(0L)
    }

    @Test
    fun `adding an expense updates the total amount`() = runTest {
        val job = launch {
            viewModel.uiState.collect {}
        }

        fakeExpenseRepo.addExpense(dummyExpense)
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertThat(state.expensesState.totalAmount).isEqualTo(dummyExpense.amount)

        job.cancel()
    }

    @Test
    fun `deleting an expense by its id deletes it`() = runTest {
        val job = launch {
            viewModel.uiState.collect {}
        }

        fakeExpenseRepo.addExpense(dummyExpense)
        advanceUntilIdle()
        var state = viewModel.uiState.value as UiState.Success
        assertThat(state.expensesState.totalAmount).isEqualTo(dummyExpense.amount)

        viewModel.onEvent(Event.DeleteExpense(dummyExpense.id!!))
        advanceUntilIdle()

        state = viewModel.uiState.value as UiState.Success
        assertThat(state.expensesState.totalAmount).isEqualTo(0L)

        job.cancel()
    }

    @Test
    fun `invoking long press on an expense opens the delete confirmation dialog`() = runTest {
        val job = launch {
            viewModel.uiState.collect {}
        }

        fakeExpenseRepo.addExpense(dummyExpense)
        advanceUntilIdle()

        viewModel.onEvent(Event.ShowDeleteConfirmationDialog(dummyExpense.id!!))
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertThat(state.dialogState).isInstanceOf(DialogState.ConfirmDeleteExpense::class.java)

        job.cancel()
    }

    @Test
    fun `invoking show calendar form opens the calendar form`() = runTest {
        val job = launch {
            viewModel.uiState.collect {}
        }

        viewModel.onEvent(Event.ShowCalendarForm)
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertThat(state.dialogState).isInstanceOf(DialogState.CalendarForm::class.java)

        job.cancel()
    }

    @Test
    fun `invoking show category filter dialog opens the category filter dialog`() = runTest {
        val job = launch {
            viewModel.uiState.collect {}
        }

        viewModel.onEvent(Event.ShowCategoryFilterDialog)
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertThat(state.dialogState).isInstanceOf(DialogState.CategoryFilterForm::class.java)

        job.cancel()
    }

    @Test
    fun `invoking reset category filters resets the category filters`() = runTest {
        val job = launch {
            viewModel.uiState.collect {}
        }

        viewModel.onEvent(Event.ResetCategoryFilters)
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertThat(state.categoryProfileState.activeProfile).isEqualTo("All")

        job.cancel()
    }

    @Test
    fun `applying category filter updates active profile and subsequent show category filter dialog includes all categories`() = runTest {
        val job = launch {
            viewModel.uiState.collect {}
        }

        val customFilters = mapOf(Category.FOOD to true, Category.BEVERAGE to false)
        viewModel.onEvent(Event.ApplyCategoryFilters("Custom", customFilters))
        advanceUntilIdle()

        var state = viewModel.uiState.value as UiState.Success
        assertThat(state.categoryProfileState.activeProfile).isEqualTo("Custom")

        viewModel.onEvent(Event.ShowCategoryFilterDialog)
        advanceUntilIdle()

        state = viewModel.uiState.value as UiState.Success
        val dialog = state.dialogState as DialogState.CategoryFilterForm
        assertThat(dialog.filteredCategories.keys).containsAtLeastElementsIn(Category.entries)

        job.cancel()
    }
}
