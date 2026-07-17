package com.luna.budgetapp.presentation.screen.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.domain.usecase.BudgetUseCases
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class BudgetViewModel(
    private val budgetUseCases: BudgetUseCases,
    private val expenseUseCases: ExpenseUseCases
): ViewModel() {
    private val _errorState = MutableStateFlow<String?>(null)
    private val _dialogState = MutableStateFlow<DialogState?>(null)
    private val _budgets = budgetUseCases.getAllBudget()

    private val _expenses = _budgets
        .map { budgets ->
        }
} 
