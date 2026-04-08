package com.luna.budgetapp.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Update
import com.luna.budgetapp.data.local.entity.ExpenseEntity
import com.luna.budgetapp.domain.model.CategoryTotalProjection
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): PagingSource<Int, ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE category = :category")
    fun getExpensesByCategory(category: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE type = :type")
    fun getExpensesByType(type: String): Flow<List<ExpenseEntity>>

    @Query("""
        SELECT * FROM expenses
        WHERE date BETWEEN :start AND :end
        ORDER BY date DESC
    """)
    fun getExpensesByDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<ExpenseEntity>>

    @Query("""
        SELECT * FROM expenses
        WHERE date BETWEEN :start AND :end
        ORDER BY date DESC
    """)
    fun getPagingExpensesByDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): PagingSource<Int, ExpenseEntity>

    @Query("""
        SELECT * FROM expenses
        WHERE date BETWEEN :start AND :end
        AND (category IN (:categories))
        ORDER BY date DESC
    """)
    fun getPagingExpensesByCategories(
        categories: List<String>,
        start: LocalDateTime,
        end: LocalDateTime
    ): PagingSource<Int, ExpenseEntity>

    @Query("""
        SELECT SUM(amount)
        FROM expenses
        WHERE date BETWEEN :start AND :end
    """)
    fun getTotalAmountByDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM expenses
        WHERE date BETWEEN :start AND :end
        AND (category IN (:categories))
    """)
    fun getTotalAmountByCategories(
        categories: List<String>,
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<Double>

    @Query("""
        SELECT category, SUM(amount) AS total
        FROM expenses
        WHERE date BETWEEN :start AND :end
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getCategoryTotalsByDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<CategoryTotalProjection>>

    @Query("""
        SELECT category, SUM(amount) AS total
        FROM expenses
        WHERE date BETWEEN :start AND :end
        AND (category IN (:categories))
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getCategoryTotalsByCategory(
        categories: List<String>,
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<CategoryTotalProjection>>

    @Insert
    suspend fun addExpense(expense: ExpenseEntity)

    @Insert(onConflict = REPLACE)
    suspend fun addExpenses(expenses: List<ExpenseEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: Long)

    @Query("""
        DELETE FROM expenses
        WHERE id = (SELECT MAX(id) FROM expenses)
    """)
    suspend fun deleteLatestExpense()

    @Delete
    suspend fun deleteExpenses(expenses: List<ExpenseEntity>)

    @Query("""
        UPDATE expenses
        SET amount = :amount,
            type = :type
        WHERE id = :expenseId
    """)
    suspend fun editExpenseById(
        expenseId: Long,
        amount: Double,
        type: String
    )

}
