package com.luna.budgetapp.domain.usecase.expense

import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.repository.ExpenseRepository

class AddExpenseUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(
        category: String,
        type: String,
        amount: Long
    ) {
        val expense = Expense(
            category = category,
            type = type,
            amount = amount
        )
        repository.addExpense(expense)
    }
}
