package com.luna.budgetapp.domain.usecase.expense

import com.luna.budgetapp.domain.model.Granularity
import com.luna.budgetapp.domain.model.SpendingHistory
import com.luna.budgetapp.domain.repository.ExpenseRepository
import com.luna.budgetapp.domain.utils.rollUp
import com.luna.budgetapp.domain.utils.todayFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock

class ObserveSpendingHistoryUseCase(
    private val repository: ExpenseRepository,
    private val clock: Clock
) {
    operator fun invoke(categories: List<String>, granularity: Granularity): Flow<SpendingHistory> =
        combine(repository.getDailyTotals(categories), todayFlow(clock)) { daily, today ->
            SpendingHistory(today, rollUp(daily, granularity, today))
        }
}
