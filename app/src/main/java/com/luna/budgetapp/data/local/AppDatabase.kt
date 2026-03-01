package com.luna.budgetapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.luna.budgetapp.data.local.dao.ExpenseDao
import com.luna.budgetapp.data.local.dao.ExpensePresetDao
import com.luna.budgetapp.data.local.dao.CategoryFilterDao
import com.luna.budgetapp.data.local.entity.CategoryFilterEntity
import com.luna.budgetapp.data.local.entity.ExpenseEntity
import com.luna.budgetapp.data.local.entity.ExpensePresetEntity

@Database(
    entities =  [
        ExpenseEntity::class, 
        ExpensePresetEntity::class,
        CategoryFilterEntity::class
    ], 
    version =  1, exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun expensePresetDao(): ExpensePresetDao
    abstract fun categoryFilterDao(): CategoryFilterDao
}
