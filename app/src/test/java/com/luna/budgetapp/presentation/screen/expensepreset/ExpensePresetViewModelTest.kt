package com.luna.budgetapp.presentation.screen.expensepreset

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.MainDispatcherRule
import com.luna.budgetapp.MutableClock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.domain.repository.FakeCategoryRepository
import com.luna.budgetapp.domain.repository.FakeExpensePresetRepository
import com.luna.budgetapp.domain.repository.FakeExpenseRepository
import com.luna.budgetapp.domain.repository.FakeSettingsRepository
import com.luna.budgetapp.domain.usecase.AuthUseCases
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.usecase.PresetUseCases
import com.luna.budgetapp.domain.usecase.ProfileUseCases
import com.luna.budgetapp.domain.usecase.SettingsUseCases
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
import com.luna.budgetapp.domain.usecase.expense.GetExpensesByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expense.GetTotalAmountByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.AddExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.DeleteExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.GetAllExpensePresetsUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.UpdateExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.settings.GetActiveCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.settings.SetActiveCategoryProfileUseCase
import com.luna.budgetapp.presentation.screen.expensepreset.components.ExpenseFormAction
import com.luna.budgetapp.domain.usecase.auth.SignOutUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
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
    private val clock = MutableClock(Instant.now())
    private val signOut = mockk<SignOutUseCase>()

    val dummyPreset = ExpensePreset(
        id = 1,
        amount = 1000L,
        category = Category.FOOD,
        type = "Lunch"
    )


    private val success: UiState.Success
        get() = viewModel.uiState.value as UiState.Success

    private val totalAmount: Long get() = success.expensesState.totalAmount
    private val presets: List<ExpensePreset> get() = success.expensesState.expensePresets

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
            updateExpensePreset = UpdateExpensePresetUseCase(fakeExpensePresetRepo),
        )
        val expenseUseCases = ExpenseUseCases(
            addExpense = AddExpenseUseCase(fakeExpenseRepo),
            deleteExpense = DeleteExpenseUseCase(fakeExpenseRepo),
            deleteLatestExpense = DeleteLatestExpenseUseCase(fakeExpenseRepo),
            getAllExpenses = mockk(),
            getExpensesByCategory = mockk(),
            getExpensesByDateRange = GetExpensesByDateRangeUseCase(fakeExpenseRepo),
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
            observeCategoryProfileState = ObserveCategoryProfileStateUseCase(
                GetActiveCategoryProfileUseCase(fakeSettingsRepo),
                GetCategoryProfileUseCase(fakeCategoryRepo),
                GetCategoryProfilesUseCase(fakeCategoryRepo)
            ),
        )
        val settingsUseCases = SettingsUseCases(
            getActiveDateFilter = mockk(),
            setActiveDateFilter = mockk(),
            getMigrationStatus = mockk(),
            setMigrationComplete = mockk()
        )

        fakeCategoryRepo.initializeIfNeeded()
        fakeSettingsRepo.setActiveProfile("All")
        viewModel = ExpensePresetViewModel(
            presetUseCases,
            expenseUseCases,
            profileUseCases,
            AuthUseCases(
                getToken = mockk(),
                signInGoogle = mockk(),
                signInEmailPassword = mockk(),
                signUp = mockk(),
                signOut = signOut,
                isSignedIn = mockk(),
                completeSignIn = mockk()
            ),
            clock
        )

        runCurrent()
    }

    @Test
    fun `signOut setsSignedOut and handled resets it`() = runTest {
        coEvery { signOut(any()) } just Runs
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onEvent(Event.SignOut(mockk(relaxed = true)))
        runCurrent()
        assertThat(viewModel.signedOut.value).isTrue()

        viewModel.onSignedOutHandled()
        assertThat(viewModel.signedOut.value).isFalse()
        job.cancel()
    }

    @Test
    fun `signOut when signOut throws does not set signedOut`() = runTest {
        coEvery { signOut(any()) } throws RuntimeException("boom")
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onEvent(Event.SignOut(mockk(relaxed = true)))
        runCurrent()
        assertThat(viewModel.signedOut.value).isFalse()
        job.cancel()
    }

    @Test
    fun `Retry after the fault clears restores Success`() = runTest {
        fakeExpenseRepo.shouldThrow = true
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()
        assertThat(viewModel.uiState.value).isInstanceOf(UiState.Error::class.java)

        fakeExpenseRepo.shouldThrow = false
        viewModel.onEvent(Event.Retry)
        runCurrent()

        assertThat(viewModel.uiState.value).isInstanceOf(UiState.Success::class.java)
        job.cancel()
    }

    @Test
    fun `uiState becomes Error when the expense flow throws`() = runTest {
        fakeExpenseRepo.shouldThrow = true
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(UiState.Error::class.java)
        assertThat((state as UiState.Error).message).isEqualTo("fake failure")
        job.cancel()
    }

    @Test
    fun `clicking a preset adds an expense`() = runTest {
        val job = launch {
            viewModel.uiState.collect {}
        }

        viewModel.onEvent(Event.AddExpense(dummyPreset))

        runCurrent()

        assertThat(totalAmount).isEqualTo(dummyPreset.amount)

        job.cancel()
    }

    @Test
    fun `dialog shows when clicking add expense preset`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()
        assertThat(success.dialogState).isNull()

        viewModel.onEvent(Event.AddExpensePreset(null, ExpenseFormAction.ADD))

        runCurrent()

        assertThat(success.dialogState).isNotNull()
        job.cancel()
    }

    @Test
    fun `confirming expense form adds an expense preset`() = runTest {
        val job = launch {
            viewModel.uiState.collect {}
        }

        viewModel.onEvent(Event.AddExpensePreset(null, ExpenseFormAction.ADD))
        runCurrent()

        viewModel.onEvent(
            Event.ConfirmExpenseFormDialog(
                id = null,
                category = Category.FOOD,
                type = dummyPreset.type,
                amount = "10"
            )
        )

        runCurrent()
        assertEquals(1, presets.size)
        job.cancel()
    }

    @Test
    fun `deleting an expense preset deletes it`() = runTest {
        val job = launch {
            viewModel.uiState.collect {}
        }

        fakeExpensePresetRepo.addExpensePreset(dummyPreset)
        runCurrent()

        assertThat(presets.size).isEqualTo(1)

        dummyPreset.id?.let {
            viewModel.onEvent(Event.DeleteExpensePreset(it))
        }

        assertThat(presets.size).isEqualTo(0)

        job.cancel()
    }

    @Test
    fun `adding custom expenses updates total amount`() = runTest {
        val job = launch {
            viewModel.uiState.collect {}
        }

        val custom = dummyPreset.copy(
            type = "Dinner",
            amount = 2000L
        )

        viewModel.onEvent(
            Event.AddExpense(
                dummyPreset,
                "20",
                custom.type
            )
        )
        runCurrent()

        assertThat(totalAmount).isEqualTo(custom.amount)
        job.cancel()
    }

    @Test
    fun `invoking the undo button deletes the latest expense`() = runTest {
        val job = launch {
            viewModel.uiState.collect {}
        }
        viewModel.onEvent(Event.AddExpense(dummyPreset))
        runCurrent()
        assertEquals(totalAmount, dummyPreset.amount)

        viewModel.onEvent(Event.DeleteLatestExpense)
        runCurrent()

        assertEquals(0L, totalAmount)
        job.cancel()
    }

    @Test
    fun `confirming preset delete dialog deletes the preset with the target id`() = runTest {
        val job = launch {
            viewModel.uiState.collect {}
        }
        fakeExpensePresetRepo.addExpensePreset(dummyPreset)
        runCurrent()
        assertEquals(1, presets.size)

        viewModel.onEvent(Event.DeleteExpensePreset(dummyPreset.id!!))
        runCurrent()

        assertEquals(0, presets.size)
        job.cancel()
    }

    @Test
    fun `expense list range follows the clock past midnight`() = runTest {
        clock.current = Instant.parse("2026-03-10T23:59:00Z")
        val yesterday = LocalDateTime.of(2026, 3, 10, 12, 0)
        val today = LocalDateTime.of(2026, 3, 11, 12, 0)
        fakeExpenseRepo.addExpense(Expense(amount = 1000L, category = Category.FOOD, type = "Old", date = yesterday))
        fakeExpenseRepo.addExpense(Expense(amount = 500L, category = Category.FOOD, type = "New", date = today))

        val job = launch { viewModel.uiState.collect {} }
        advanceTimeBy(1)
        assertThat(totalAmount).isEqualTo(1000L)

        clock.current = Instant.parse("2026-03-11T00:00:30Z")
        advanceTimeBy(61_000)
        assertThat(totalAmount).isEqualTo(500L)
        job.cancel()
    }
}
