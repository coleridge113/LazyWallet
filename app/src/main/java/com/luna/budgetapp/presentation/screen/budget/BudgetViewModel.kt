package com.luna.budgetapp.presentation.screen.budget

import com.luna.budgetapp.domain.utils.parseAmountExpression
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
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(
    private val budgetUseCases: BudgetUseCases,
    private val expenseUseCases: ExpenseUseCases
): ViewModel() {
    private val _dialogState = MutableStateFlow<DialogState?>(null)
    private val _budgets = budgetUseCases.getAllBudget()
    private val _expenses = _budgets
        .flatMapLatest { budgets ->
            if (budgets.isEmpty()) {
                flowOf(emptyMap())
            } else {
                val expenseFlows = budgets.map { budget ->
                    val (start, end) = budget.frequency.resolve()
                    expenseUseCases.getExpensesByDateRange(
                        categories = budget.interactors.map { it.name },
                        start = start,
                        end = end
                    ).map { expenses -> budget.id to expenses }
                }
                combine(expenseFlows) { it.toMap() }
            }
        }

    private val _monthlyOutlook =
        combine(_budgets, _expenses) { budgets, expenses ->
            OutlookDetails(
                income = 0L,
                projectedSpend = budgets.sumOf { it.frequency.projectAmountToMonth(it.limit) },
                actualSpend = expenses.values.flatten().distinctBy { it.id }.sumOf { it.amount },
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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000.milliseconds),
            initialValue = UiState.Loading
        )


    fun onEvent(event: Event) {
        when (event) {
            Event.DismissDialog -> dismissDialog()
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

            val budget = Budget(
                id = id,
                name = name,
                limit = parseAmountExpression(amount),
                frequency = frequency,
                interactors = categoryMap.filter { it.value }.keys.toList(),
                startDate = LocalDate.now()
            )

            if (budget.id != 0L) {
                budgetUseCases.updateBudget(budget)
            } else {
                budgetUseCases.saveBudget(budget)
            }
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
}
