package com.luna.budgetapp.presentation.screen.expensepreset

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MainDispatcherRule
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.domain.repository.FakeExpensePresetRepository
import com.luna.budgetapp.domain.repository.FakeExpenseRepository
import com.luna.budgetapp.domain.usecase.UseCases
import com.luna.budgetapp.domain.usecase.expense.AddExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.GetTotalAmountByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.AddExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.DeleteExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.GetAllExpensePresetsUseCase
import io.mockk.mockk
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

    val defaultCategories = listOf(
        Category.FOOD,
        Category.DATE,
        Category.BEVERAGE,
        Category.COMMUTE,
        Category.OTHERS,
        Category.FITNESS
    )

    private lateinit var viewModel: ExpensePresetViewModel

    @Before
    fun setup() {
        val fakeExpensePresetRepo = FakeExpensePresetRepository()
        val fakeExpenseRepo = FakeExpenseRepository()

        val useCases = UseCases(
            getToken = mockk(),
            addExpense = AddExpenseUseCase(fakeExpenseRepo),
            deleteExpense = mockk(),
            deleteLatestExpense = mockk(),
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
            initializeCategoryProfile = mockk(),
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
}