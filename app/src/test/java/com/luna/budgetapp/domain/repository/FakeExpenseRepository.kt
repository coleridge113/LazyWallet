package com.luna.budgetapp.domain.repository

import androidx.paging.PagingData
import com.luna.budgetapp.common.Resource
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
        TODO()
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
        TODO("Not yet implemented")
    }

    override fun getPagingExpensesByDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<PagingData<Expense>> {
        TODO("Not yet implemented")
    }

    override fun getPagingExpensesByCategories(
        categories: List<String>,
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<PagingData<Expense>> {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override fun getCategoryTotalsByCategory(
        categories: List<String>,
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<CategoryTotalProjection>> {
        TODO("Not yet implemented")
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
        println("Adding expense to fake repo")
        expensesFlow.update {
            it + expense
        }
    }

    override suspend fun addExpenses(expenses: List<Expense>) {
        TODO("Not yet implemented")
    }

    override suspend fun updateExpense(expense: Expense) {
        TODO("Not yet implemented")
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