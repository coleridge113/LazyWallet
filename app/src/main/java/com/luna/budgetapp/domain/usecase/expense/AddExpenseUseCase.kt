package com.luna.budgetapp.domain.usecase.expense

import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.repository.ExpenseRepository
import com.luna.budgetapp.domain.utils.parseAmountExpression

class AddExpenseUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(
        category: String,
        type: String,
        amount: String
    ) {
        val expense = Expense(
            category = category,
            type = type,
            amount = parseAmountExpression(amount)
        )
        repository.addExpense(expense)
    }
}
