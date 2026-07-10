package com.luna.budgetapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.luna.budgetapp.data.local.entity.BudgetEntity

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budget")
    suspend fun getBudgets(): List<BudgetEntity>

    @Query("SELECT * FROM budget where name = :name")
    suspend fun getBudgetByName(name: String): BudgetEntity

    @Query("SELECT * FROM budget where id = :budgetId")
    suspend fun getBudgetById(budgetId: Long): BudgetEntity

    @Insert
    suspend fun insertBudget(budget: BudgetEntity)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Transaction
    suspend fun updateBudgetVersion(oldBudget: BudgetEntity, newBudget: BudgetEntity) {
        updateBudget(oldBudget)
        insertBudget(newBudget)
    }
}
