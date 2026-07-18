package com.luna.budgetapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.luna.budgetapp.data.local.entity.BudgetEntity
import com.luna.budgetapp.data.local.entity.BudgetInteractorCategoryEntity
import com.luna.budgetapp.data.local.entity.BudgetWithInteractors
import com.luna.budgetapp.domain.model.Category
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budget")
    fun getBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budget where name = :name")
    fun getBudgetByName(name: String): Flow<BudgetEntity>

    @Query("SELECT * FROM budget where id = :budgetId")
    fun getBudgetById(budgetId: Long): Flow<BudgetEntity>

    @Query("SELECT * FROM budget where id = :budgetId")
    suspend fun getBudgetByIdOnce(budgetId: Long): BudgetEntity?

    @Transaction
    @Query("SELECT * FROM budget")
    fun getBudgetsWithInteractors(): Flow<List<BudgetWithInteractors>>

    @Transaction
    @Query("SELECT * FROM budget where id = :budgetId")
    fun getBudgetWithInteractorsById(budgetId: Long): Flow<BudgetWithInteractors?>

    @Transaction
    @Query("SELECT * FROM budget where id = :budgetId")
    suspend fun getBudgetWithInteractorsByIdOnce(budgetId: Long): BudgetWithInteractors?

    @Transaction
    @Query("SELECT * FROM budget where name = :name")
    fun getBudgetWithInteractorsByName(name: String): Flow<BudgetWithInteractors?>

    @Insert
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Transaction
    suspend fun updateBudgetVersion(oldBudget: BudgetEntity, newBudget: BudgetEntity) {
        updateBudget(oldBudget)
        insertBudget(newBudget)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteractors(interactors: List<BudgetInteractorCategoryEntity>)

    @Query("DELETE FROM budget_interactor_categories WHERE budgetId = :budgetId")
    suspend fun clearInteractors(budgetId: Long)

    @Transaction
    suspend fun updateBudgetVersionWithInteractors(
        oldBudget: BudgetEntity,
        newBudget: BudgetEntity,
        newInteractors: List<Category>
    ) {
        updateBudget(oldBudget)
        val newId = insertBudget(newBudget)
        val childEntities = newInteractors.map { category ->
            BudgetInteractorCategoryEntity(budgetId = newId, category = category)
        }
        insertInteractors(childEntities)
    }

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0.0) 
        FROM expenses e
        WHERE e.date = :targetDate
          AND e.category IN (
              SELECT category 
              FROM budget_interactor_categories 
              WHERE budgetId = :budgetId
          )
    """)
    fun getSpentAmountForBudgetOnDate(budgetId: Long, targetDate: LocalDate): Flow<Double>
}
