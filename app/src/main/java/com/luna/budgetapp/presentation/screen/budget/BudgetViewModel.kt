package com.luna.budgetapp.presentation.screen.budget

import com.luna.budgetapp.domain.utils.todayFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.domain.model.Budget
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.model.projectAmountToMonth
import com.luna.budgetapp.domain.usecase.BudgetUseCases
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.presentation.screen.budget.model.MonthOutlook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(
    private val budgetUseCases: BudgetUseCases,
    private val expenseUseCases: ExpenseUseCases,
    private val clock: Clock
): ViewModel() {

    private val _dialogState = MutableStateFlow<DialogState?>(null)
    private val _budgets = budgetUseCases.getAllBudget()
    private val _today = todayFlow(clock)
    private val _expenses = combine(_budgets, _today) { budgets, today -> budgets to today }
        .flatMapLatest { (budgets, today) ->
            if (budgets.isEmpty()) {
                flowOf(emptyMap())
            } else {
                val expenseFlows = budgets.map { budget ->
                    val (start, end) = budget.frequency.resolve(today)
                    expenseUseCases.getExpensesByDateRange(
                        categories = budget.interactors.map { it.name },
                        start = start,
                        end = end
                    ).map { expenses -> budget.id to expenses }
                }
                combine(expenseFlows) { it.toMap() }
            }
        }

    private val _outlook = combine(
        _budgets,
        expenseUseCases.observeAllDailyTotals(),
        _today
    ) { budgets, dailyTotals, today ->
        MonthOutlook.from(
            today = today,
            budgetTotal = budgets.sumOf { it.frequency.projectAmountToMonth(it.limit) },
            dailyTotals = dailyTotals
        )
    }

    private val _successState = combine(
        _budgets,
        _expenses,
        _outlook,
        _dialogState
    ) { budgets, expenses, outlook, dialog ->
        UiState.Success(
            budgets = budgets,
            expenses = expenses,
            outlook = outlook,
            dialog = dialog
        )
    }

    /** Bumped by [Event.Retry]; each bump restarts the load pipeline below. */
    private val _retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<UiState> = _retryTrigger
        .flatMapLatest {
            _successState
                .map<UiState.Success, UiState> { it }
                .catch { e ->
                    if (e is CancellationException) throw e
                    emit(UiState.Error(e.message))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000.milliseconds),
            initialValue = UiState.Loading
        )


    fun onEvent(event: Event) {
        when (event) {
            Event.DismissDialog -> dismissDialog()
            Event.Retry -> _retryTrigger.update { it + 1 }
            is Event.ShowDeleteDialog -> showDeleteConfirmationDialog(event.budget)
            is Event.ShowBudgetFormDialog -> showBudgetDialog(event.budget)
            is Event.ConfirmBudgetFormDialog -> saveBudget(
                event.id, event.name, event.amount, event.frequency, event.categoryMap
            )
            is Event.ConfirmDeleteBudget -> deleteBudget(event.budget)
        }
    }

    private fun dismissDialog() {
        _dialogState.update { null }
    }

    private fun showBudgetDialog(budget: Budget?) {
        _dialogState.update {
            DialogState.BudgetDialog(budget)
        }
    }

    private fun saveBudget(
        id: Long = 0,
        name: String,
        amount: String,
        frequency: DateFilter,
        categoryMap: Map<Category, Boolean>
    ) {
        viewModelScope.launch {
            dismissDialog()

            budgetUseCases.saveBudgetForm(id, name, amount, frequency, categoryMap)
        }
    }

    private fun showDeleteConfirmationDialog(budget: Budget) {
        _dialogState.update {
            DialogState.DeleteDialog(budget)
        }
    }

    private fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            dismissDialog()
            budgetUseCases.deleteBudget(budget)
        }
    }
}
