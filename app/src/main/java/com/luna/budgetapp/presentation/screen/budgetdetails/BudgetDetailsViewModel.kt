package com.luna.budgetapp.presentation.screen.budgetdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.domain.usecase.BudgetUseCases
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val budgetUseCases: BudgetUseCases,
    private val expenseUseCases: ExpenseUseCases
): ViewModel() {
    val budgetId: Long = checkNotNull(savedStateHandle["budgetId"])

    private val _budget = budgetUseCases.getBudgetById(budgetId)
    private val _expenses = _budget.flatMapLatest { budget ->
        val (start ,end) = budget.frequency.resolve()
        expenseUseCases.getExpensesByDateRange(
            categories = budget.interactors.map { it.name },
            start = start,
            end = end
        )
    }

    val uiState = combine(_budget, _expenses) { budget, expenses ->
        UiState.Success(
            budget = budget,
            expenses = expenses
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000.milliseconds),
            initialValue = UiState.Loading
        )
}
