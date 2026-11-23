package com.luna.budgetapp.domain.usecase.expense

import androidx.paging.PagingData
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

class GetPagingExpensesByDateRangeUseCase(
    private val repository: ExpenseRepository
) {
    operator fun invoke(
        categories: List<String>? = null,
        start: LocalDateTime,
        end: LocalDateTime,
        query: String = ""
    ): Flow<PagingData<Expense>> {
        return if (categories == null) { 
            repository.getPagingExpensesByDateRange(start, end, query)
        } else {
            repository.getPagingExpensesByCategories(categories, start, end, query)
        }
    }
}
