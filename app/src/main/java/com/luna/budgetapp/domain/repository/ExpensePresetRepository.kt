package com.luna.budgetapp.domain.repository

import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.common.Resource
import kotlinx.coroutines.flow.Flow

interface ExpensePresetRepository {

    fun getAllExpensePresets(): Flow<List<ExpensePreset>>

    suspend fun addExpensePresets(expensePresets: List<ExpensePreset>)

    suspend fun addExpensePreset(expensePreset: ExpensePreset)

    suspend fun updateExpensePreset(expensePreset: ExpensePreset)

    suspend fun deleteExpensePreset(expensePresetId: Long)
}
