package com.luna.budgetapp.presentation.screen.analysis

import androidx.lifecycle.SavedStateHandle
import com.luna.budgetapp.domain.usecase.expense.GetExpensesByDateRangeUseCase
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
import com.luna.budgetapp.presentation.screen.analysis.UiState
import io.mockk.mockk
import java.time.LocalDate
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
class AnalysisViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `selected date is restored from saved state`() = runTest {
        val expenseRepo = FakeExpenseRepository()
        val settingsRepo = FakeSettingsRepository()
        val categoryRepo = FakeCategoryRepository()
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
        val profileUseCases = ProfileUseCases(
            getCategoryProfile = GetCategoryProfileUseCase(categoryRepo),
            getCategoryProfiles = GetCategoryProfilesUseCase(categoryRepo),
            saveCategoryProfile = SaveCategoryProfileUseCase(categoryRepo),
            deleteCategoryProfile = DeleteCategoryProfileUseCase(categoryRepo),
            initializeCategoryProfile = InitializeCategoryProfileUseCase(categoryRepo),
            getActiveCategoryProfile = GetActiveCategoryProfileUseCase(settingsRepo),
            setActiveCategoryProfile = SetActiveCategoryProfileUseCase(settingsRepo),
            observeCategoryProfileState = ObserveCategoryProfileStateUseCase(
                GetActiveCategoryProfileUseCase(settingsRepo),
                GetCategoryProfileUseCase(categoryRepo),
                GetCategoryProfilesUseCase(categoryRepo)
            ),
        )
        categoryRepo.initializeIfNeeded()
        settingsRepo.setActiveProfile("All")

        val date = LocalDate.now().minusDays(2)
        val vm = AnalysisViewModel(
            SavedStateHandle(mapOf("selected_date" to date.toEpochDay())),
            expenseUseCases,
            profileUseCases
        )
        val job = launch { vm.uiState.collect {} }
        runCurrent()
        val state = vm.uiState.value as UiState.Success
        assertThat(state.dateState.selectedDate).isEqualTo(date)
        job.cancel()
    }
}
