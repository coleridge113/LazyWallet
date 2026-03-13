package com.luna.budgetapp.domain.repository

import com.luna.budgetapp.domain.model.ExpensePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update

class FakeExpensePresetRepository : ExpensePresetRepository {
    private val flow = MutableStateFlow<List<ExpensePreset>>(emptyList())

    override fun getAllExpensePresets(): Flow<List<ExpensePreset>> = flow

    override suspend fun addExpensePresets(expensePresets: List<ExpensePreset>) {
        TODO("Not yet implemented")
    }

    override suspend fun addExpensePreset(expensePreset: ExpensePreset) {
        flow.update {
           it + expensePreset
        }
    }

    override suspend fun deleteExpensePreset(expensePresetId: Long) {
        flow.update { presetList ->
            presetList.filter { it.id != expensePresetId }
        }
    }
}
