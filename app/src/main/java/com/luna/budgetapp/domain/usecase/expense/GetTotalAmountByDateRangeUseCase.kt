package com.luna.budgetapp.domain.usecase.expense

import com.luna.budgetapp.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

class GetTotalAmountByDateRangeUseCase(
    private val repository: ExpenseRepository
) {
    operator fun invoke(
        categories: List<String>? = null,
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<Long> {
        return if (categories == null) {
            repository.getTotalAmountByDateRange(start, end)
        } else {
            repository.getTotalAmountByCategories(categories, start, end)
        }
    }
}
