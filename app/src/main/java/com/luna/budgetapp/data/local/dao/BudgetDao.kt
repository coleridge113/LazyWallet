package com.luna.budgetapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.luna.budgetapp.data.local.entity.BudgetEntity
import com.luna.budgetapp.domain.model.BudgetType

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budget where type = :type")
    suspend fun getBudgetByType(type: BudgetType)

    @Upsert
    suspend fun insertBudget(budget: BudgetEntity)
}
