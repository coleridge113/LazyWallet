package com.luna.budgetapp.domain.usecase.expense

import com.luna.budgetapp.domain.model.DailyTotal
import com.luna.budgetapp.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow

class ObserveAllDailyTotalsUseCase(
    private val repository: ExpenseRepository
) {
    operator fun invoke(): Flow<List<DailyTotal>> = repository.getAllDailyTotals()
}
