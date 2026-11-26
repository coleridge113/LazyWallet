package com.luna.budgetapp.presentation.screen.analysis

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MainDispatcherRule
import com.luna.budgetapp.MutableClock
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.CategoryFilter
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.Granularity
import com.luna.budgetapp.domain.model.PeriodChange
import com.luna.budgetapp.domain.repository.FakeCategoryRepository
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
import com.luna.budgetapp.domain.usecase.expense.GetExpensesByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expense.ObserveSpendingHistoryUseCase
import com.luna.budgetapp.domain.usecase.settings.GetActiveCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.settings.SetActiveCategoryProfileUseCase
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val expenseRepo = FakeExpenseRepository()
    private val settingsRepo = FakeSettingsRepository()
    private val categoryRepo = FakeCategoryRepository()
    private val clock = MutableClock(Instant.parse("2026-11-26T10:00:00Z"))

    private fun d(day: Int, month: Int = 11) = LocalDate.of(2026, month, day)

    private fun expense(id: Long, amount: Long, date: LocalDate, category: Category = Category.FOOD) =
        Expense(id = id, amount = amount, category = category, type = "t$id", date = date.atTime(12, 0))

    private suspend fun seed(vararg expenses: Expense) = expenses.forEach { expenseRepo.addExpense(it) }

    private suspend fun newViewModel(saved: SavedStateHandle = SavedStateHandle()): AnalysisViewModel {
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
            observeSpendingHistory = ObserveSpendingHistoryUseCase(expenseRepo, clock),
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
        return AnalysisViewModel(saved, expenseUseCases, profileUseCases, clock)
    }

    private fun TestScope.success(vm: AnalysisViewModel): UiState.Success {
        runCurrent()
        return vm.uiState.value as UiState.Success
    }

    @Test
    fun `defaults dayGranularity currentPeriodSelected`() = runTest {
        seed(expense(1, 4000, d(24)))
        val vm = newViewModel()
        val job = launch { vm.uiState.collect {} }
        val state = success(vm)
        assertThat(state.historyState.granularity).isEqualTo(Granularity.DAY)
        assertThat(state.historyState.selected?.start).isEqualTo(d(26))
        job.cancel()
    }

    @Test
    fun `selectPeriod selectsAndLoadsItsExpenses`() = runTest {
        seed(
            expense(1, 4000, d(23)),
            expense(2, 10000, d(24)),
            expense(3, 7000, d(26)),
        )
        val vm = newViewModel()
        val job = launch { vm.uiState.collect {} }
        runCurrent()
        vm.onEvent(Event.SelectPeriod(d(24)))
        val state = success(vm)
        assertThat(state.historyState.selected?.start).isEqualTo(d(24))
        assertThat(state.expensesState.filteredExpenses.map { it.id }).containsExactly(2L)
        assertThat(state.historyState.change).isEqualTo(PeriodChange(6000, 150.0))
        job.cancel()
    }

    @Test
    fun `selectGranularity resetsToCurrentPeriod`() = runTest {
        seed(expense(1, 10000, d(24)), expense(2, 7000, d(26)))
        val handle = SavedStateHandle()
        val vm = newViewModel(handle)
        val job = launch { vm.uiState.collect {} }
        runCurrent()
        vm.onEvent(Event.SelectPeriod(d(24)))
        runCurrent()
        vm.onEvent(Event.SelectGranularity(Granularity.WEEK))
        val state = success(vm)
        assertThat(state.historyState.granularity).isEqualTo(Granularity.WEEK)
        assertThat(state.historyState.selected?.start).isEqualTo(d(23))
        assertThat(state.historyState.selected?.end).isEqualTo(d(29))
        assertThat(handle.get<String>("granularity")).isEqualTo("WEEK")
        assertThat(handle.get<Long>("selected_start")).isNull()
        job.cancel()
    }

    @Test
    fun `weekSelection listsWholeWeek`() = runTest {
        seed(
            expense(1, 100, d(15)),
            expense(2, 200, d(16)),
            expense(3, 300, d(22)),
            expense(4, 400, d(23)),
        )
        val vm = newViewModel(SavedStateHandle(mapOf("granularity" to "WEEK")))
        val job = launch { vm.uiState.collect {} }
        runCurrent()
        vm.onEvent(Event.SelectPeriod(d(16)))
        val state = success(vm)
        assertThat(state.expensesState.filteredExpenses.map { it.id }).containsExactly(2L, 3L)
        job.cancel()
    }

    @Test
    fun `emptyProfile showsZeroBars`() = runTest {
        seed(expense(1, 4000, d(24)), expense(2, 5000, d(26)))
        val vm = newViewModel()
        categoryRepo.saveProfile(
            Category.entries.map { CategoryFilter(profileName = "Off", category = it, isActive = false) }
        )
        settingsRepo.setActiveProfile("Off")
        val job = launch { vm.uiState.collect {} }
        val state = success(vm)
        assertThat(state.historyState.periods).isNotEmpty()
        assertThat(state.historyState.periods.map { it.total }.distinct()).containsExactly(0L)
        assertThat(state.expensesState.filteredExpenses).isEmpty()
        job.cancel()
    }

    @Test
    fun `restore normalisesSelectionToPeriodStart`() = runTest {
        seed(expense(1, 4000, d(12)))
        val vm = newViewModel(
            SavedStateHandle(mapOf("granularity" to "WEEK", "selected_start" to d(18).toEpochDay()))
        )
        val job = launch { vm.uiState.collect {} }
        val state = success(vm)
        assertThat(state.historyState.selected?.start).isEqualTo(d(16))
        job.cancel()
    }

    @Test
    fun `error then retry restores success`() = runTest {
        expenseRepo.shouldThrow = true
        val vm = newViewModel()
        val job = launch { vm.uiState.collect {} }
        runCurrent()
        assertThat(vm.uiState.value).isInstanceOf(UiState.Error::class.java)

        expenseRepo.shouldThrow = false
        vm.onEvent(Event.Retry)
        runCurrent()
        assertThat(vm.uiState.value).isInstanceOf(UiState.Success::class.java)
        job.cancel()
    }
}
