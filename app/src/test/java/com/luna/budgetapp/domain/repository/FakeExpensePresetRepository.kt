package com.luna.budgetapp.domain.repository

import com.luna.budgetapp.domain.model.ExpensePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeExpensePresetRepository : ExpensePresetRepository {
    private val presetFlow = MutableStateFlow<List<ExpensePreset>>(emptyList())

    override fun getAllExpensePresets(): Flow<List<ExpensePreset>> = presetFlow

    override suspend fun addExpensePresets(expensePresets: List<ExpensePreset>) {
        presetFlow.update { it + expensePresets }
    }

    override suspend fun updateExpensePreset(expensePreset: ExpensePreset) {
        presetFlow.update { list ->
            list.map { if (it.id == expensePreset.id) expensePreset.copy(position = it.position) else it }
        }
    }

    override suspend fun addExpensePreset(expensePreset: ExpensePreset) {
        presetFlow.update {
            val next = (it.maxOfOrNull { p -> p.position } ?: -1) + 1
            it + expensePreset.copy(position = next)
        }
    }

    override suspend fun deleteExpensePreset(expensePresetId: Long) {
        presetFlow.update { presetList ->
            presetList.filter { it.id != expensePresetId }
        }
    }

    override suspend fun reorderExpensePresets(orderedIds: List<Long>) {
        presetFlow.update { list ->
            val byId = list.associateBy { it.id }
            val listed = orderedIds.mapNotNull { byId[it] }
            val listedIds = listed.map { it.id }.toSet()
            val rest = list.filter { it.id !in listedIds }
            (listed + rest).mapIndexed { index, p -> p.copy(position = index) }
        }
    }
}
