package com.luna.budgetapp.domain.usecase.expense

import com.luna.budgetapp.domain.repository.ExpenseRepository

class EditExpenseUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(
        id: Long,
        amount: Long,
        type: String
    ) {
        repository.editExpenseById(id, amount, type)
    }
}
