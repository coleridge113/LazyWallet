package com.luna.budgetapp.presentation.screen.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.domain.model.Expense
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
        _expenses
    ) { budgets, expenses ->
        UiState.Success(
            budgets = budgets,
            expenses = expenses
        )
    }

    val uiState: StateFlow<UiState> = _successState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

}
