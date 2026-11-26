package com.luna.budgetapp.data.mapper

import com.luna.budgetapp.data.firebase.toEntity
import com.luna.budgetapp.data.local.entity.ExpensePresetEntity
import com.luna.budgetapp.data.firebase.models.ExpensePreset as FirestorePreset

/** New position = index in [orderedIds]; returns only ids whose position changes. Unknown ids are ignored. */
fun reorderedPositions(orderedIds: List<Long>, current: Map<Long, Int>): Map<Long, Int> {
    val result = LinkedHashMap<Long, Int>()
    orderedIds.forEachIndexed { index, id ->
        if (id in current && current[id] != index) result[id] = index
    }
    return result
}

/** Positioned presets first (by position), then unpositioned by createdAt; position = final index. */
fun normalizeCloudPresetOrder(presets: List<FirestorePreset>): List<ExpensePresetEntity> {
    val positioned = presets.filter { it.position != null }.sortedBy { it.position }
    val unpositioned = presets.filter { it.position == null }.sortedBy { it.createdAt }
    return (positioned + unpositioned).mapIndexed { index, preset ->
        preset.toEntity().copy(position = index)
    }
}
