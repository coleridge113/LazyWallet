package com.luna.budgetapp.domain.repository

import androidx.paging.PagingData
import com.luna.budgetapp.common.Resource
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.CategoryTotalProjection
import com.luna.budgetapp.domain.model.DailyTotal
import com.luna.budgetapp.domain.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime

class FakeExpenseRepository : ExpenseRepository {
    private val expensesFlow = MutableStateFlow<List<Expense>>(emptyList())

    /** When true, the date-range / category read flows throw on collection. */
    var shouldThrow = false

    override fun getAllExpenses(): Flow<PagingData<Expense>> {
        return expensesFlow.map { PagingData.from(it) }
    }

    override fun getExpensesByCategory(category: String): Flow<Resource<List<Expense>>> {
        return expensesFlow.map { expenses ->
            Resource.Success(expenses.filter { it.category.name.equals(category, ignoreCase = true) })
        }
    }

    override fun getExpensesByType(type: String): Flow<Resource<List<Expense>>> {
        return expensesFlow.map { expenses ->
            Resource.Success(expenses.filter { it.type == type })
        }
    }

    override fun getExpensesByDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<Expense>> {
        return expensesFlow.throwIfRequested().map { expenses ->
            expenses.filter { it.date in start..end }
        }
    }

    override fun getExpensesByCategories(
        categories: List<String>,
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<Expense>> {
        return expensesFlow.throwIfRequested().map { expenses ->
            expenses.filter { it.category.name in categories && it.date in start..end }
        }
    }

    override fun getPagingExpensesByDateRange(
        start: LocalDateTime,
        end: LocalDateTime,
        query: String
    ): Flow<PagingData<Expense>> {
        return expensesFlow.map { expenses ->
            val filtered = expenses.filter {
                it.date in start..end && it.matches(query)
            }
            PagingData.from(filtered)
        }
    }

    override fun getPagingExpensesByCategories(
        categories: List<String>,
        start: LocalDateTime,
        end: LocalDateTime,
        query: String
    ): Flow<PagingData<Expense>> {
        return expensesFlow.map { expenses ->
            val filtered = expenses.filter {
                it.category.name in categories && it.date in start..end && it.matches(query)
            }
            PagingData.from(filtered)
        }
    }

    private fun Expense.matches(query: String): Boolean {
        val q = query.trim()
        return q.isEmpty() ||
            type.contains(q, ignoreCase = true) ||
            name?.contains(q, ignoreCase = true) == true ||
            category.name.contains(q, ignoreCase = true)
    }

    override fun getTotalAmountByDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<Long> {
        return expensesFlow.throwIfRequested().map { expenses ->
            println("Total recalculated: ${expenses.sumOf { e -> e.amount }}")
            expenses.filter { it.date in start..end }.sumOf { it.amount }
        }
    }

    override fun getCategoryTotalsByDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<CategoryTotalProjection>> {
        return expensesFlow.map { expenses ->
            expenses
                .filter { it.date in start..end }
                .groupBy { it.category }
                .map { (category, expenses) ->

                    CategoryTotalProjection(
                        category = category,
                        total = expenses.sumOf { it.amount }
                    )
                }
        }
    }

    override fun getCategoryTotalsByCategory(
        categories: List<String>,
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<CategoryTotalProjection>> {
        return expensesFlow.map { expenses ->
            expenses
                .filter { it.category.name in categories && it.date in start..end }
                .groupBy { it.category }
                .map { (category, expenses) ->
                    CategoryTotalProjection(
                        category = category,
                        total = expenses.sumOf { it.amount }
                    )
                }
        }
    }

    override fun getDailyTotals(categories: List<String>): Flow<List<DailyTotal>> {
        return expensesFlow.throwIfRequested().map { expenses ->
            expenses
                .filter { it.category.name in categories }
                .groupBy { it.date.toLocalDate() }
                .map { (date, list) -> DailyTotal(date, list.sumOf { it.amount }) }
                .sortedBy { it.date }
        }
    }

    override fun getTotalAmountByCategories(
        categories: List<String>,
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<Long> {
        return expensesFlow.throwIfRequested().map { expenses ->
            expenses
                .filter { it.category.name in categories && it.date in start..end }
                .sumOf { it.amount }
        }
    }

    override suspend fun addExpense(expense: Expense) {
        expensesFlow.update {
            it + expense
        }
    }

    override suspend fun addExpenses(expenses: List<Expense>) {
        expensesFlow.update {
            it + expenses
        }
    }

    override suspend fun updateExpense(expense: Expense) {
        expensesFlow.update { list ->
            list.map {
                if (it.id == expense.id) expense else it
            }
        }
    }

    override suspend fun deleteExpenseById(expenseId: Long) {
        expensesFlow.update {
            it.filter { e -> e.id != expenseId }
        }
    }

    override suspend fun deleteLatestExpense() {
        expensesFlow.update {
            it.dropLast(1)
        }
    }

    override suspend fun editExpenseById(
        expenseId: Long,
        amount: Long,
        type: String,
        date: LocalDateTime
    ) {
        expensesFlow.update {
            it.map { e ->
                if (e.id == expenseId) e.copy(amount = amount, type = type, date = date) else e
            }
        }
    }

    private fun <T> Flow<T>.throwIfRequested(): Flow<T> = flow {
        if (shouldThrow) throw IllegalStateException("fake failure")
        emitAll(this@throwIfRequested)
    }
}
