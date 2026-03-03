package com.luna.budgetapp.domain.repository

import com.luna.budgetapp.domain.model.ExpensePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeExpensePresetRepository : ExpensePresetRepository {
    private val presets = mutableListOf<ExpensePreset>()
    private val flow = MutableStateFlow<List<ExpensePreset>>(emptyList())

    override fun getAllExpensePresets(): Flow<List<ExpensePreset>> = flowOf(presets)

    override suspend fun addExpensePresets(expensePresets: List<ExpensePreset>) {
        TODO("Not yet implemented")
    }

    override suspend fun addExpensePreset(expensePreset: ExpensePreset) {
        presets.add(expensePreset)
        flow.value = presets
    }

    override suspend fun deleteExpensePreset(expensePresetId: Long) {
        TODO("Not yet implemented")
    }
}
