package com.luna.budgetapp.domain.repository

import androidx.paging.PagingData
import com.luna.budgetapp.common.Resource
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.CategoryTotalProjection
import com.luna.budgetapp.domain.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime

class FakeExpenseRepository : ExpenseRepository {
    private val expensesFlow = MutableStateFlow<List<Expense>>(emptyList())

    override fun getAllExpenses(): Flow<PagingData<Expense>> {
        return expensesFlow.map { PagingData.from(it) }
    }

    override fun getExpensesByCategory(category: String): Flow<Resource<List<Expense>>> {
        TODO("Not yet implemented")
    }

    override fun getExpensesByType(type: String): Flow<Resource<List<Expense>>> {
        TODO("Not yet implemented")
    }

    override fun getExpensesByDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<Expense>> {
        return expensesFlow.map { expenses ->
            expenses.filter { it.date in start..end }
        }
    }

    override fun getPagingExpensesByDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<PagingData<Expense>> {
        return expensesFlow.map { expenses ->
            val filtered = expenses.filter {
                it.date in start..end
            }
            PagingData.from(filtered)
        }
    }

    override fun getPagingExpensesByCategories(
        categories: List<String>,
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<PagingData<Expense>> {
        return expensesFlow.map { expenses ->
            val filtered = expenses.filter {
                it.category in categories && it.date in start..end
            }
            PagingData.from(filtered)
        }
    }

    override fun getTotalAmountByDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<Double> {
        return expensesFlow.map { expenses ->
            println("Total recalculated: ${expenses.sumOf { e -> e.amount }}")
            expenses.sumOf { it.amount }
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
                    val match = Category.entries.find { it.displayName == category }

                    CategoryTotalProjection(
                        category = match!!,
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
                .filter { it.category in categories && it.date in start..end }
                .groupBy { it.category }
                .map { (category, expenses) ->

                    val match = Category.entries.find { it.displayName == category }
                    CategoryTotalProjection(
                        category = match!!,
                        total = expenses.sumOf { it.amount }
                    )
                }
        }
    }

    override fun getTotalAmountByCategories(
        categories: List<String>,
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<Double> {
        return expensesFlow.map { expenses ->
            expenses.sumOf { it.amount }
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
}