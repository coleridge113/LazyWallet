package com.luna.budgetapp.expenselist

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MainDispatcherRule
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.DateFilter
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
import com.luna.budgetapp.domain.usecase.category.ObserveCategoryProfileStateUseCase
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
import kotlinx.coroutines.test.runCurrent
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
            observeCategoryProfileState = ObserveCategoryProfileStateUseCase(
                GetActiveCategoryProfileUseCase(fakeSettingsRepo),
                GetCategoryProfileUseCase(fakeCategoryRepo),
                GetCategoryProfilesUseCase(fakeCategoryRepo)
            ),
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
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        val state = viewModel.uiState.value as UiState.Success
        assertThat(state.expensesState.totalAmount).isEqualTo(0L)
        job.cancel()
    }

    @Test
    fun `uiState becomes Error when the expense flow throws`() = runTest {
        fakeExpenseRepo.shouldThrow = true
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(UiState.Error::class.java)
        job.cancel()
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
    fun `applying category filter updates active profile and exposes the full category map in ui state`() = runTest {
        val job = launch {
            viewModel.uiState.collect {}
        }
        runCurrent()

        val customFilters = mapOf(Category.FOOD to true, Category.BEVERAGE to false)
        viewModel.onEvent(Event.ApplyCategoryFilters("Custom", customFilters))
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertThat(state.categoryProfileState.activeProfile).isEqualTo("Custom")
        assertThat(state.categoryProfileState.selectedCategoryMap.keys)
            .containsAtLeastElementsIn(Category.entries)

        job.cancel()
    }

    private suspend fun saveWorkProfile() {
        fakeCategoryRepo.saveProfile(
            Category.entries.map {
                com.luna.budgetapp.domain.model.CategoryFilter("Work", it, it == Category.FOOD)
            }
        )
    }

    @Test
    fun `previewing a profile exposes its map and keeps the active profile`() = runTest {
        saveWorkProfile()
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onEvent(Event.PreviewCategoryProfile("Work"))
        runCurrent()

        val state = viewModel.uiState.value as UiState.Success
        assertThat(state.categoryPreview?.profileName).isEqualTo("Work")
        assertThat(state.categoryPreview?.categoryMap?.get(Category.FOOD)).isTrue()
        assertThat(state.categoryPreview?.categoryMap?.get(Category.BEVERAGE)).isFalse()
        assertThat(state.categoryProfileState.activeProfile).isEqualTo("All")
        assertThat(state.categoryProfileState.selectedCategoryMap.values.all { it }).isTrue()

        job.cancel()
    }

    @Test
    fun `applying after a preview activates the previewed profile and clears the preview`() = runTest {
        saveWorkProfile()
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onEvent(Event.PreviewCategoryProfile("Work"))
        runCurrent()
        val previewMap = (viewModel.uiState.value as UiState.Success).categoryPreview!!.categoryMap
        viewModel.onEvent(Event.ApplyCategoryFilters("Work", previewMap))
        runCurrent()

        val state = viewModel.uiState.value as UiState.Success
        assertThat(state.categoryProfileState.activeProfile).isEqualTo("Work")
        assertThat(state.categoryPreview).isNull()

        job.cancel()
    }

    @Test
    fun `clearing the preview leaves the active profile unchanged`() = runTest {
        saveWorkProfile()
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onEvent(Event.PreviewCategoryProfile("Work"))
        runCurrent()
        viewModel.onEvent(Event.ClearCategoryPreview)
        runCurrent()

        val state = viewModel.uiState.value as UiState.Success
        assertThat(state.categoryPreview).isNull()
        assertThat(state.categoryProfileState.activeProfile).isEqualTo("All")

        job.cancel()
    }

    @Test
    fun `selecting a custom date range updates the date filter`() = runTest {
        val job = launch {
            viewModel.uiState.collect {}
        }
        runCurrent()

        val custom = DateFilter.Custom(start = 1_000L, end = 2_000L)
        viewModel.onEvent(Event.SelectDateRange(custom))
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertThat(state.dateState.dateFilter).isEqualTo(custom)

        job.cancel()
    }
}
