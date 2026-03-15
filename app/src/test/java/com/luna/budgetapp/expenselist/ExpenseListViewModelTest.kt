package com.luna.budgetapp.expenselist

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MainDispatcherRule
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.domain.repository.FakeCategoryRepository
import com.luna.budgetapp.domain.repository.FakeExpensePresetRepository
import com.luna.budgetapp.domain.repository.FakeExpenseRepository
import com.luna.budgetapp.domain.repository.FakeSettingsRepository
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.usecase.PresetUseCases
import com.luna.budgetapp.domain.usecase.ProfileUseCases
import com.luna.budgetapp.domain.usecase.SettingsUseCases
import com.luna.budgetapp.domain.usecase.UseCases
import com.luna.budgetapp.domain.usecase.category.DeleteCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.category.GetCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.category.GetCategoryProfilesUseCase
import com.luna.budgetapp.domain.usecase.category.InitializeCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.category.SaveCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.expense.AddExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.DeleteExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.DeleteLatestExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.GetCategoryTotalsByDateRange
import com.luna.budgetapp.domain.usecase.expense.GetPagingExpensesByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expense.GetTotalAmountByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.AddExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.DeleteExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.GetAllExpensePresetsUseCase
import com.luna.budgetapp.domain.usecase.settings.GetActiveCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.settings.SetActiveCategoryProfileUseCase
import com.luna.budgetapp.presentation.screen.expenselist.DialogState
import com.luna.budgetapp.presentation.screen.expenselist.Event
import com.luna.budgetapp.presentation.screen.expenselist.ExpenseListViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeExpensePresetRepo: FakeExpensePresetRepository
    private lateinit var fakeExpenseRepo: FakeExpenseRepository
    private lateinit var fakeSettingsRepo: FakeSettingsRepository
    private lateinit var fakeCategoryRepo: FakeCategoryRepository
    private lateinit var viewModel: ExpenseListViewModel

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
    fun setup() = runTest {
        fakeExpensePresetRepo = FakeExpensePresetRepository()
        fakeExpenseRepo = FakeExpenseRepository()
        fakeSettingsRepo = FakeSettingsRepository()
        fakeCategoryRepo = FakeCategoryRepository()

        val presetUseCases = PresetUseCases(
            getAllExpensePresets = GetAllExpensePresetsUseCase(fakeExpensePresetRepo),
            addExpensePreset = AddExpensePresetUseCase(fakeExpensePresetRepo),
            deleteExpensePreset = DeleteExpensePresetUseCase(fakeExpensePresetRepo),
        )
        val expenseUseCases = ExpenseUseCases(
            addExpense = AddExpenseUseCase(fakeExpenseRepo),
            deleteExpense = DeleteExpenseUseCase(fakeExpenseRepo),
            deleteLatestExpense = DeleteLatestExpenseUseCase(fakeExpenseRepo),
            getAllExpenses = mockk(),
            getExpensesByCategory = mockk(),
            getExpensesByDateRange = mockk(),
            getTotalAmountByDateRange = GetTotalAmountByDateRangeUseCase(fakeExpenseRepo),
            getCategoryTotalsByDateRange = mockk(),
            getPagingExpensesByDateRange = mockk(),
            getExpensesByType = mockk(),
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
        val settingsUseCases = SettingsUseCases(
            getActiveDateFilter = mockk(),
            setActiveDateFilter = mockk()
        )

        fakeCategoryRepo.initializeIfNeeded()
        viewModel = ExpenseListViewModel(
            presetUseCases,
            expenseUseCases,
            profileUseCases
        )
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

    @Test
    fun `invoking long press on an expense opens the delete confirmation dialog`() = runTest {
        fakeExpenseRepo.addExpense(dummyExpense)
        advanceUntilIdle()

        val initial = viewModel.uiState.value
        assertThat(initial.totalAmount).isEqualTo(dummyExpense.amount)

        viewModel.onEvent(Event.ShowDeleteConfirmationDialog(dummyExpense.id!!))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.dialogState).isInstanceOf(DialogState.ConfirmDeleteExpense::class.java)
    }

    @Test
    fun `invoking show calendar form opens the calendar form`() = runTest {
        viewModel.onEvent(Event.ShowCalendarForm)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.dialogState).isInstanceOf(DialogState.CalendarForm::class.java)
    }

    @Test
    fun `invoking show category filter dialog opens the category filter dialog`() = runTest {
        viewModel.onEvent(Event.ShowCategoryFilterDialog)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.dialogState).isInstanceOf(DialogState.CategoryFilterForm::class.java)
    }

    @Test
    fun `invoking reset category filters resets the category filters`() = runTest {
        viewModel.onEvent(Event.ResetCategoryFilters)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.activeProfile).isEqualTo("All")

    }
}
