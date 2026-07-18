package com.luna.budgetapp.presentation.screen.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.domain.model.Budget
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.usecase.BudgetUseCases
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

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

    private val _successState = combine(
        _budgets,
        _expenses,
        _dialogState
    ) { budgets, expenses, dialog ->
        UiState.Success(
            budgets = budgets,
            expenses = expenses,
            dialog = dialog
        )
    }

    val uiState: StateFlow<UiState> = _successState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )


    fun onEvent(event: Event) {
        when (event) {
            Event.DismissDialog -> dismissDialog()
            is Event.ShowBudgetDialog -> showBudgetDialog(event.budget)
            is Event.ConfirmBudgetFormDialog -> saveBudget(
                event.name, event.amount, event.frequency, event.categoryMap
            )
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
        name: String,
        amount: String,
        frequency: DateFilter,
        categoryMap: Map<Category, Boolean>
    ) {
        viewModelScope.launch {
            dismissDialog()

            val budget = Budget(
                name = name,
                limit = amount.toDoubleOrNull() ?: 0.0,
                frequency = frequency,
                interactors = categoryMap.filter { it.value }.keys.toList(),
                startDate = LocalDate.now()
            )

            budgetUseCases.saveBudget(budget)
        }
    }
}
