package com.luna.budgetapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import com.luna.budgetapp.data.local.entity.ExpensePresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpensePresetDao {
    
    @Query("SELECT * FROM expense_presets ORDER BY position, id")
    fun getAllExpensePresets(): Flow<List<ExpensePresetEntity>>

    @Insert(onConflict = REPLACE)
    suspend fun addExpensePresets(expensePresets: List<ExpensePresetEntity>)

    @Insert(onConflict = REPLACE)
    suspend fun addExpensePreset(expensePreset: ExpensePresetEntity)

    @Query("DELETE FROM expense_presets WHERE id = :expensePresetId")
    suspend fun deleteExpensePreset(expensePresetId: Long)
    @Query("SELECT * FROM expense_presets ORDER BY position, id")
    suspend fun getAllExpensePresetsOnce(): List<ExpensePresetEntity>

    @Query("SELECT * FROM expense_presets WHERE id = :id")
    suspend fun getExpensePresetByIdOnce(id: Long): ExpensePresetEntity?

    @Query("DELETE FROM expense_presets")
    suspend fun deleteAll()

    @Query("SELECT MAX(position) FROM expense_presets")
    suspend fun getMaxPosition(): Int?

    @Query("UPDATE expense_presets SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    @Transaction
    suspend fun updatePositions(positions: Map<Long, Int>) {
        positions.forEach { (id, position) -> updatePosition(id, position) }
    }
}
