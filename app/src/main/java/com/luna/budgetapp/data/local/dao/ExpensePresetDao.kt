package com.luna.budgetapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import com.luna.budgetapp.data.local.entity.ExpensePresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpensePresetDao {
    
    @Query("SELECT * FROM expense_presets")
    fun getAllExpensePresets(): Flow<List<ExpensePresetEntity>>

    @Insert(onConflict = REPLACE)
    suspend fun addExpensePresets(expensePresets: List<ExpensePresetEntity>)

    @Insert(onConflict = REPLACE)
    suspend fun addExpensePreset(expensePreset: ExpensePresetEntity)

    @Query("DELETE FROM expense_presets WHERE id = :expensePresetId")
    suspend fun deleteExpensePreset(expensePresetId: Long)
    @Query("SELECT * FROM expense_presets")
    suspend fun getAllExpensePresetsOnce(): List<ExpensePresetEntity>

    @Query("DELETE FROM expense_presets")
    suspend fun deleteAll()
}
