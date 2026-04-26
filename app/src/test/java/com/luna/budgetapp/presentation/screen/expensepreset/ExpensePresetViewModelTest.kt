package com.luna.budgetapp.presentation.screen.expensepreset

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MainDispatcherRule
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.domain.repository.FakeCategoryRepository
import com.luna.budgetapp.domain.repository.FakeExpensePresetRepository
import com.luna.budgetapp.domain.repository.FakeExpenseRepository
import com.luna.budgetapp.domain.repository.FakeSettingsRepository
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.usecase.PresetUseCases
import com.luna.budgetapp.domain.usecase.ProfileUseCases
import com.luna.budgetapp.domain.usecase.SettingsUseCases
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
import com.luna.budgetapp.domain.usecase.expensepreset.AddExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.DeleteExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.GetAllExpensePresetsUseCase
import com.luna.budgetapp.domain.usecase.settings.GetActiveCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.settings.SetActiveCategoryProfileUseCase
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
    private lateinit var fakeSettingsRepo: FakeSettingsRepository
    private lateinit var fakeCategoryRepo: FakeCategoryRepository
    private lateinit var viewModel: ExpensePresetViewModel

    val dummyPreset = ExpensePreset(
        id = 1,
        amount = 10.0,
        category = "Food",
        type = "Lunch"
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
        val settingsUseCases = SettingsUseCases(
            getActiveDateFilter = mockk(),
            setActiveDateFilter = mockk()
        )

        fakeCategoryRepo.initializeIfNeeded()
        fakeSettingsRepo.setActiveProfile("All")
        viewModel = ExpensePresetViewModel(
            presetUseCases,
            expenseUseCases,
            profileUseCases
        )

        advanceUntilIdle()
    }

    @Test
    fun `clicking a preset adds an expense`() = runTest {
        val job = launch {
            viewModel.totalAmount.collect {}
        }

        viewModel.onEvent(Event.AddExpense(dummyPreset))

        advanceUntilIdle()

        assertThat(viewModel.totalAmount.value).isEqualTo(dummyPreset.amount)

        job.cancel()
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
        val job = launch {
            viewModel.expensePresets.collect {}
        }

        viewModel.onEvent(Event.ShowExpenseForm(null))
        advanceUntilIdle()

        viewModel.onEvent(
            Event.ConfirmExpenseFormDialog(
                Category.FOOD,
                dummyPreset.type,
                dummyPreset.amount.toString()
            )
        )

        advanceUntilIdle()
        assertEquals(1, viewModel.expensePresets.value.size)
        job.cancel()
    }

    @Test
    fun `deleting an expense preset deletes it`() = runTest {
        val job = launch {
            viewModel.expensePresets.collect {}
        }

        fakeExpensePresetRepo.addExpensePreset(dummyPreset)
        advanceUntilIdle()

        assertThat(viewModel.expensePresets.value.size).isEqualTo(1)

        dummyPreset.id?.let {
            viewModel.onEvent(Event.DeleteExpensePreset(it))
        }

        assertThat(viewModel.expensePresets.value.size).isEqualTo(0)

        job.cancel()
    }

    @Test
    fun `adding custom expenses updates total amount`() = runTest {
        val job = launch {
            viewModel.totalAmount.collect {}
        }

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

        assertThat(viewModel.totalAmount.value).isEqualTo(custom.amount)
        job.cancel()
    }

    @Test
    fun `invoking the undo button deletes the latest expense`() = runTest {
        val job = launch {
            viewModel.totalAmount.collect {}
        }
        viewModel.onEvent(Event.AddExpense(dummyPreset))
        advanceUntilIdle()
        assertEquals(viewModel.totalAmount.value, dummyPreset.amount)

        viewModel.onEvent(Event.DeleteLatestExpense)
        advanceUntilIdle()

        assertEquals(viewModel.totalAmount.value, 0.0)
        job.cancel()
    }

    @Test
    fun `confirming preset delete dialog deletes the preset with the target id`() = runTest {
        val job = launch {
            viewModel.expensePresets.collect {  }
        }
        fakeExpensePresetRepo.addExpensePreset(dummyPreset)
        advanceUntilIdle()
        assertEquals(1, viewModel.expensePresets.value.size)

        viewModel.onEvent(Event.DeleteExpensePreset(dummyPreset.id!!))
        advanceUntilIdle()

        val final = viewModel.uiState.value
        assertEquals(0, viewModel.expensePresets.value.size)
        job.cancel()
    }
}
