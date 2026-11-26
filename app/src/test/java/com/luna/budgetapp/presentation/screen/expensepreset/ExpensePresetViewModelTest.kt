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
import com.luna.budgetapp.domain.repository.FakeAuthRepository
import com.luna.budgetapp.domain.repository.FakeCategoryRepository
import com.luna.budgetapp.domain.repository.FakeLocalDataRepository
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
import com.luna.budgetapp.domain.usecase.expense.ObserveSpendingHistoryUseCase
import com.luna.budgetapp.domain.model.CategoryFilter
import com.luna.budgetapp.domain.usecase.expense.GetTotalAmountByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.AddExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.DeleteExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.GetAllExpensePresetsUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.ReorderExpensePresetsUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.UpdateExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.settings.GetActiveCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.settings.SetActiveCategoryProfileUseCase
import com.luna.budgetapp.presentation.screen.expensepreset.components.ExpenseFormAction
import com.luna.budgetapp.domain.usecase.auth.SignOutUseCase
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
    private val clock = MutableClock(Instant.parse("2026-11-26T18:05:00Z"))
    private lateinit var fakeAuthRepo: FakeAuthRepository

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
        fakeAuthRepo = FakeAuthRepository()

        val presetUseCases = PresetUseCases(
            getAllExpensePresets = GetAllExpensePresetsUseCase(fakeExpensePresetRepo),
            addExpensePreset = AddExpensePresetUseCase(fakeExpensePresetRepo),
            deleteExpensePreset = DeleteExpensePresetUseCase(fakeExpensePresetRepo),
            updateExpensePreset = UpdateExpensePresetUseCase(fakeExpensePresetRepo),
            reorderExpensePresets = ReorderExpensePresetsUseCase(fakeExpensePresetRepo),
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
            observeSpendingHistory = ObserveSpendingHistoryUseCase(fakeExpenseRepo, clock),
            observeAllDailyTotals = mockk(),
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
                signOut = SignOutUseCase(fakeAuthRepo, fakeSettingsRepo, FakeLocalDataRepository()),
                isSignedIn = mockk(),
                completeSignIn = mockk()
            ),
            clock
        )

        runCurrent()
    }

    @Test
    fun `signOut setsSignedOut and handled resets it`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onEvent(Event.SignOut)
        runCurrent()
        assertThat(viewModel.signedOut.value).isTrue()

        viewModel.onSignedOutHandled()
        assertThat(viewModel.signedOut.value).isFalse()
        job.cancel()
    }

    @Test
    fun `signOut when signOut throws does not set signedOut`() = runTest {
        fakeAuthRepo.signOutError = RuntimeException("boom")
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onEvent(Event.SignOut)
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

    private suspend fun seedThreeAndReorder() {
        listOf(1L, 2L, 3L).forEach {
            fakeExpensePresetRepo.addExpensePreset(dummyPreset.copy(id = it, type = "P$it"))
        }
        viewModel.onEvent(Event.ReorderPresets(listOf(3L, 1L, 2L)))
    }

    @Test
    fun `reorder presets updates the preset order`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        seedThreeAndReorder()
        runCurrent()

        assertThat(presets.map { it.id }).containsExactly(3L, 1L, 2L).inOrder()
        job.cancel()
    }

    @Test
    fun `new preset is added last`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        seedThreeAndReorder()
        runCurrent()

        viewModel.onEvent(Event.AddExpensePreset(null, ExpenseFormAction.ADD))
        runCurrent()
        viewModel.onEvent(
            Event.ConfirmExpenseFormDialog(id = null, category = Category.FOOD, type = "New", amount = "10")
        )
        runCurrent()

        assertThat(presets.map { it.type }).containsExactly("P3", "P1", "P2", "New").inOrder()
        job.cancel()
    }

    @Test
    fun `edit keeps preset position`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        seedThreeAndReorder()
        runCurrent()

        val first = presets.first { it.id == 1L }
        viewModel.onEvent(Event.EditExpensePreset(first, ExpenseFormAction.EDIT))
        runCurrent()
        viewModel.onEvent(
            Event.ConfirmExpenseFormDialog(id = 1L, category = Category.FOOD, type = "Edited", amount = "20")
        )
        runCurrent()

        assertThat(presets.map { it.id }).containsExactly(3L, 1L, 2L).inOrder()
        assertThat(presets.first { it.id == 1L }.type).isEqualTo("Edited")
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

    private val todayAt = LocalDateTime.of(2026, 11, 26, 18, 5)

    private suspend fun seedToday(type: String, amount: Long, category: Category = Category.FOOD): Expense {
        fakeExpenseRepo.addExpense(Expense(amount = amount, category = category, type = type, date = todayAt))
        return fakeExpenseRepo.getExpensesByDateRange(todayAt.minusHours(1), todayAt.plusHours(1))
            .first().first { it.type == type }
    }

    private val todayExpenses get() = success.expensesState.expenses

    @Test
    fun `removing an expense deletes exactly that id and posts a Removed message`() = runTest {
        val lunch = seedToday("Lunch", 25000L)
        val jeepney = seedToday("Jeepney", 6000L, Category.COMMUTE)
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onEvent(Event.RemoveExpense(lunch))
        runCurrent()

        assertThat(todayExpenses.map { it.id }).containsExactly(jeepney.id)
        assertThat(success.userMessage).isEqualTo(UserMessage.Removed(lunch.id!!, "Lunch", 25000L))
        job.cancel()
    }

    @Test
    fun `undo re-adds the removed expense with the same fields`() = runTest {
        seedToday("Lunch", 25000L)
        val jeepney = seedToday("Jeepney", 6000L, Category.COMMUTE)
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onEvent(Event.RemoveExpense(jeepney))
        runCurrent()
        viewModel.onEvent(Event.UndoRemove)
        runCurrent()

        val back = todayExpenses.single { it.type == "Jeepney" }
        assertThat(back.category).isEqualTo(Category.COMMUTE)
        assertThat(back.amount).isEqualTo(6000L)
        assertThat(back.date).isEqualTo(todayAt)
        assertThat(success.userMessage).isNull()
        job.cancel()
    }

    @Test
    fun `undo after two removals restores only the second`() = runTest {
        val a = seedToday("A", 100L)
        val b = seedToday("B", 200L)
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onEvent(Event.RemoveExpense(a))
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        viewModel.onEvent(Event.RemoveExpense(b))
        runCurrent()
        viewModel.onEvent(Event.UndoRemove)
        runCurrent()

        assertThat(todayExpenses.map { it.type }).containsExactly("B")
        job.cancel()
    }

    @Test
    fun `undo after the message was shown still restores`() = runTest {
        val a = seedToday("A", 100L)
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onEvent(Event.RemoveExpense(a))
        runCurrent()
        viewModel.onEvent(Event.UserMessageShown)
        runCurrent()
        assertThat(success.userMessage).isNull()
        viewModel.onEvent(Event.UndoRemove)
        runCurrent()

        assertThat(todayExpenses.map { it.type }).containsExactly("A")
        job.cancel()
    }

    @Test
    fun `clear undo drops the message and makes undo a no-op`() = runTest {
        val a = seedToday("A", 100L)
        seedToday("B", 200L)
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onEvent(Event.RemoveExpense(a))
        runCurrent()
        viewModel.onEvent(Event.ClearUndo)
        runCurrent()
        assertThat(success.userMessage).isNull()
        viewModel.onEvent(Event.UndoRemove)
        runCurrent()

        assertThat(todayExpenses.map { it.type }).containsExactly("B")
        job.cancel()
    }

    @Test
    fun `undo with nothing removed does nothing`() = runTest {
        seedToday("A", 100L)
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onEvent(Event.UndoRemove)
        runCurrent()

        assertThat(todayExpenses.map { it.type }).containsExactly("A")
        job.cancel()
    }

    @Test
    fun `failed delete posts no message and a following undo adds nothing`() = runTest {
        val a = seedToday("A", 100L)
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()
        fakeExpenseRepo.shouldThrowOnDelete = true

        viewModel.onEvent(Event.RemoveExpense(a))
        runCurrent()
        assertThat(success.userMessage).isNull()
        viewModel.onEvent(Event.UndoRemove)
        runCurrent()

        assertThat(todayExpenses.map { it.type }).containsExactly("A")
        job.cancel()
    }

    @Test
    fun `second remove inside the lock window is ignored until it passes`() = runTest {
        val a = seedToday("A", 100L)
        val b = seedToday("B", 200L)
        val c = seedToday("C", 300L)
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onEvent(Event.RemoveExpense(a))
        viewModel.onEvent(Event.RemoveExpense(b))
        runCurrent()
        assertThat(todayExpenses.map { it.type }).containsExactly("B", "C")

        advanceTimeBy(500)
        runCurrent()
        viewModel.onEvent(Event.RemoveExpense(c))
        runCurrent()
        assertThat(todayExpenses.map { it.type }).containsExactly("B")
        job.cancel()
    }

    @Test
    fun `removing the only expense of today clears lastLogged`() = runTest {
        val a = seedToday("A", 100L)
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()
        assertThat(lastLogged).isNotNull()

        viewModel.onEvent(Event.RemoveExpense(a))
        runCurrent()

        assertThat(lastLogged).isNull()
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

    private val lastLogged get() = success.expensesState.lastLogged
    private val recent get() = success.expensesState.recent
    private val sparkline get() = success.expensesState.sparkline

    @Test
    fun `expense preset total counts categories the active filter excludes`() = runTest {
        fakeCategoryRepo.saveProfile(
            Category.entries.map { CategoryFilter("Work", it, it != Category.FOOD) }
        )
        fakeSettingsRepo.setActiveProfile("Work")
        val food = Expense(amount = 700L, category = Category.FOOD, type = "Lunch", date = LocalDateTime.of(2026, 11, 26, 12, 0))
        fakeExpenseRepo.addExpense(food)

        val job = launch { viewModel.uiState.collect {} }
        runCurrent()

        assertThat(totalAmount).isEqualTo(700L)
        assertThat(lastLogged?.type).isEqualTo("Lunch")
        job.cancel()
    }

    @Test
    fun `lastLogged is today's highest id and ignores yesterday`() = runTest {
        fakeExpenseRepo.addExpense(Expense(amount = 100L, category = Category.FOOD, type = "Yesterday", date = LocalDateTime.of(2026, 11, 25, 12, 0)))
        fakeExpenseRepo.addExpense(Expense(amount = 200L, category = Category.FOOD, type = "First", date = LocalDateTime.of(2026, 11, 26, 9, 0)))
        fakeExpenseRepo.addExpense(Expense(amount = 300L, category = Category.FOOD, type = "Second", date = LocalDateTime.of(2026, 11, 26, 10, 0)))

        val job = launch { viewModel.uiState.collect {} }
        runCurrent()

        assertThat(lastLogged?.type).isEqualTo("Second")
        assertThat(recent.map { it.type }).containsExactly("Second", "First").inOrder()
        job.cancel()
    }

    @Test
    fun `lastLogged is null when today has no expenses`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()

        assertThat(lastLogged).isNull()
        assertThat(recent).isEmpty()
        job.cancel()
    }

    @Test
    fun `sparkline has seven days ending today`() = runTest {
        fakeExpenseRepo.addExpense(Expense(amount = 600L, category = Category.FOOD, type = "A", date = LocalDateTime.of(2026, 11, 24, 12, 0)))
        fakeExpenseRepo.addExpense(Expense(amount = 450L, category = Category.FOOD, type = "B", date = LocalDateTime.of(2026, 11, 26, 12, 0)))

        val job = launch { viewModel.uiState.collect {} }
        runCurrent()

        assertThat(sparkline).containsExactly(0L, 0L, 0L, 0L, 600L, 0L, 450L).inOrder()
        job.cancel()
    }

    @Test
    fun `sparkline and strip roll over at midnight`() = runTest {
        fakeExpenseRepo.addExpense(Expense(amount = 600L, category = Category.FOOD, type = "A", date = LocalDateTime.of(2026, 11, 24, 12, 0)))
        fakeExpenseRepo.addExpense(Expense(amount = 450L, category = Category.FOOD, type = "B", date = LocalDateTime.of(2026, 11, 26, 12, 0)))

        val job = launch { viewModel.uiState.collect {} }
        runCurrent()
        assertThat(lastLogged).isNotNull()

        clock.current = Instant.parse("2026-11-27T00:00:30Z")
        advanceTimeBy(6 * 60 * 60 * 1000L)
        runCurrent()

        assertThat(sparkline.takeLast(4)).containsExactly(600L, 0L, 450L, 0L).inOrder()
        assertThat(lastLogged).isNull()
        assertThat(recent).isEmpty()
        job.cancel()
    }
}
