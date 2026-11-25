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
import com.luna.budgetapp.presentation.screen.budget.model.OutlookDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalTime
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(
    private val budgetUseCases: BudgetUseCases,
    private val expenseUseCases: ExpenseUseCases,
    private val clock: Clock
): ViewModel() {

    private val _navigation = Channel<Navigation>()
    val navigation = _navigation.receiveAsFlow()
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

    private val _totalAmount = _today.flatMapLatest { today ->
        expenseUseCases.getTotalAmountByDateRange(
            start = today.withDayOfMonth(1).atStartOfDay(),
            end = today.atTime(LocalTime.MAX)
        )
    }

    private val _monthlyOutlook = combine(
        _budgets, 
        _totalAmount
    ) { budgets, totalAmount ->
        OutlookDetails(
            income = 0L,
            projectedSpend = budgets.sumOf { it.frequency.projectAmountToMonth(it.limit) },
            actualSpend = totalAmount
        )
    }

    private val _successState = combine(
        _budgets,
        _expenses,
        _monthlyOutlook,
        _dialogState
    ) { budgets, expenses, monthlyOutlook, dialog ->
        UiState.Success(
            budgets = budgets,
            expenses = expenses,
            monthlyOutlook = monthlyOutlook,
            dialog = dialog
        )
    }

    val uiState: StateFlow<UiState> = _successState
        .map<UiState.Success, UiState> { it }
        .catch { e ->
            if (e is CancellationException) throw e
            emit(UiState.Error(e.message ?: "Something went wrong"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000.milliseconds),
            initialValue = UiState.Loading
        )


    fun onEvent(event: Event) {
        when (event) {
            Event.DismissDialog -> dismissDialog()
            is Event.GotoBudgetDetails -> gotoBudgetDetails(event.budgetId)
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

    fun showDeleteConfirmationDialog(budget: Budget) {
        _dialogState.update {
            DialogState.DeleteDialog(budget)
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            dismissDialog()
            budgetUseCases.deleteBudget(budget)
        }
    }

    fun gotoBudgetDetails(budgetId: BudgetId) {
        viewModelScope.launch {
            _navigation.send(Navigation.GotoBudgetDetails(budgetId))
        }
    }
}
