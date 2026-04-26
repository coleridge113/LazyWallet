package com.luna.budgetapp.presentation.screen.expensepreset

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MainDispatcherRule
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.domain.repository.FakeExpensePresetRepository
import com.luna.budgetapp.domain.repository.FakeExpenseRepository
import com.luna.budgetapp.domain.usecase.category.InitializeCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.expense.AddExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.DeleteExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.DeleteLatestExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.GetTotalAmountByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.AddExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.DeleteExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.GetAllExpensePresetsUseCase
import com.luna.budgetapp.domain.usecase.PresetUseCases
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.usecase.ProfileUseCases
import com.luna.budgetapp.domain.usecase.SettingsUseCases
import com.luna.budgetapp.domain.usecase.expense.EditExpenseUseCase
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

    val dummyPreset = ExpensePreset(
        id = 1,
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
            editExpense = EditExpenseUseCase(fakeExpenseRepo),
        )
        val profileUseCases = ProfileUseCases(
            getCategoryProfile = mockk(),
            getCategoryProfiles = mockk(),
            saveCategoryProfile = mockk(),
            deleteCategoryProfile = mockk(),
            initializeCategoryProfile = initializeCategoryProfile,
            getActiveCategoryProfile = mockk(),
            setActiveCategoryProfile = mockk(),
        )
        val settingsUseCases = SettingsUseCases(
            getActiveDateFilter = mockk(),
            setActiveDateFilter = mockk()
        )

        viewModel = ExpensePresetViewModel(
            presetUseCases,
            expenseUseCases,
            profileUseCases
        )

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

        assertThat(viewModel.totalAmount).isEqualTo(expensePreset.amount)
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
        viewModel.onEvent(
            Event.ConfirmExpenseFormDialog(
                Category.FOOD,
                dummyPreset.type,
                dummyPreset.amount.toString()
            )
        )

        assertEquals(1, viewModel.expensePresets.value.size)
    }

    @Test
    fun `deleting an expense preset deletes it`() = runTest {
        fakeExpensePresetRepo.addExpensePreset(dummyPreset)
        advanceUntilIdle()

        assertThat(viewModel.expensePresets.value.size).isEqualTo(1)

        dummyPreset.id?.let {
            viewModel.onEvent(Event.DeleteExpensePreset(it))
        }

        assertThat(viewModel.expensePresets.value.size).isEqualTo(0)
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
                custom.amount.toString(),
                custom.amount.toString()
            )
        )
        advanceUntilIdle()

        assertThat(viewModel.totalAmount).isEqualTo(custom.amount)
    }

    @Test
    fun `invoking the undo button deletes the latest expense`() = runTest {
        viewModel.onEvent(Event.AddExpense(dummyPreset))
        advanceUntilIdle()
        assertThat(viewModel.totalAmount).isEqualTo(dummyPreset.amount)

        viewModel.onEvent(Event.DeleteLatestExpense)
        advanceUntilIdle()

        assertThat(viewModel.totalAmount).isEqualTo(0.0)
    }

    @Test
    fun `confirming preset delete dialog deletes the preset with the target id`() = runTest {
        fakeExpensePresetRepo.addExpensePreset(dummyPreset)
        advanceUntilIdle()
        assertThat(viewModel.expensePresets.value.size).isEqualTo(1)

        viewModel.onEvent(Event.DeleteExpensePreset(dummyPreset.id!!))
        advanceUntilIdle()

        val final = viewModel.uiState.value
        assertThat(viewModel.expensePresets.value.size).isEqualTo(0)
    }
}
