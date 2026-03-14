package com.luna.budgetapp.presentation.screen.expensepreset

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MainDispatcherRule
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.domain.repository.FakeCategoryRepository
import com.luna.budgetapp.domain.repository.FakeExpensePresetRepository
import com.luna.budgetapp.domain.repository.FakeExpenseRepository
import com.luna.budgetapp.domain.usecase.UseCases
import com.luna.budgetapp.domain.usecase.category.InitializeCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.expense.AddExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.DeleteExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.DeleteLatestExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.GetTotalAmountByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.AddExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.DeleteExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.GetAllExpensePresetsUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensePresetViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeExpensePresetRepo: FakeExpensePresetRepository
    private lateinit var fakeExpenseRepo: FakeExpenseRepository
    private lateinit var fakeCategoryRepo: FakeCategoryRepository


    val defaultCategories = listOf(
        Category.FOOD,
        Category.DATE,
        Category.BEVERAGE,
        Category.COMMUTE,
        Category.OTHERS,
        Category.FITNESS
    )

    val dummyPreset = ExpensePreset(
        id = 1,
        amount = 10.0,
        category = "Food",
        type = "Lunch"
    )

    val dummyExpense = Expense(
        amount = 10.0,
        category = "Food",
        type = "Lunch"
    )

    private lateinit var viewModel: ExpensePresetViewModel

    @Before
    fun setup() {
        fakeExpensePresetRepo = FakeExpensePresetRepository()
        fakeExpenseRepo = FakeExpenseRepository()

        val initializeCategoryProfile = mockk<InitializeCategoryProfileUseCase>()
        coEvery { initializeCategoryProfile() } just Runs

        val useCases = UseCases(
            getToken = mockk(),
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
            getAllExpensePresets = GetAllExpensePresetsUseCase(fakeExpensePresetRepo),
            addExpensePreset = AddExpensePresetUseCase(fakeExpensePresetRepo),
            deleteExpensePreset = DeleteExpensePresetUseCase(fakeExpensePresetRepo),
            getCategoryProfile = mockk(),
            getCategoryProfiles = mockk(),
            saveCategoryProfile = mockk(),
            deleteCategoryProfile = mockk(),
            initializeCategoryProfile = initializeCategoryProfile,
            getActiveCategoryProfile = mockk(),
            setActiveCategoryProfile = mockk(),
            getActiveDateFilter = mockk(),
            setActiveDateFilter = mockk()
        )

        viewModel = ExpensePresetViewModel(useCases)
        runTest {
            advanceUntilIdle()
        }
    }

    @Test
    fun `clicking a preset adds an expense`() = runTest {
        val expensePreset = ExpensePreset(
            amount = 10.0,
            category = "Food",
            type = "Lunch",
        )

        viewModel.onEvent(Event.AddExpense(expensePreset))

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.totalAmount).isAtLeast(1.0)
    }

    @Test
    fun `dialog shows when clicking add expense preset`() = runTest {
        val initial = viewModel.uiState.value
        assertThat(initial.dialogState).isNull()

        viewModel.onEvent(Event.ShowExpenseForm(null))

        advanceUntilIdle()

        val final = viewModel.uiState.value
        assertThat(final.dialogState).isNotNull()
    }

    @Test
    fun `confirming expense form adds an expense preset`() = runTest {
        viewModel.onEvent(Event.ShowExpenseForm(null))
        advanceUntilIdle()
        viewModel.onEvent(
            Event.ConfirmDialog(
                Category.FOOD,
                dummyPreset.type,
                dummyPreset.amount.toString()
            )
        )

        val state = viewModel.uiState.value

        assertEquals(1, state.expensePresets.size)
    }

    @Test
    fun `deleting an expense preset deletes it`() = runTest {
        fakeExpensePresetRepo.addExpensePreset(dummyPreset)
        advanceUntilIdle()

        val initial = viewModel.uiState.value
        assertThat(initial.expensePresets.size).isEqualTo(1)

        dummyPreset.id?.let {
            viewModel.onEvent(Event.DeleteExpensePreset(it))
        }

        val final = viewModel.uiState.value
        assertThat(final.expensePresets.size).isEqualTo(0)
    }

    @Test
    fun `adding custom expenses updates total amount`() = runTest {
        val custom = dummyPreset.copy(
            type = "Dinner",
            amount = 20.0
        )

        viewModel.onEvent(
            Event.AddExpense(
                dummyPreset,
                custom.amount,
                custom.amount.toString()
            )
        )
        advanceUntilIdle()

        val final = viewModel.uiState.value
        assertThat(final.totalAmount).isEqualTo(custom.amount)
    }

    @Test
    fun `deleting latest expense deletes it`() = runTest {
        viewModel.onEvent(Event.AddExpense(dummyPreset))
        advanceUntilIdle()
        val initial = viewModel.uiState.value
        assertThat(initial.totalAmount).isEqualTo(dummyPreset.amount)

        viewModel.onEvent(Event.DeleteLatestExpense)
        advanceUntilIdle()

        val final = viewModel.uiState.value
        assertThat(final.totalAmount).isEqualTo(0.0)
    }
}