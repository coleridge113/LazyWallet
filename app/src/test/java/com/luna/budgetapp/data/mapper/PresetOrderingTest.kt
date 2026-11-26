package com.luna.budgetapp.data.mapper

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.data.firebase.models.ExpensePreset
import org.junit.Test
import java.util.Date

class PresetOrderingTest {

    @Test fun `unchanged order returns no changes`() {
        assertThat(reorderedPositions(listOf(1L, 2L, 3L), mapOf(1L to 0, 2L to 1, 3L to 2))).isEmpty()
    }

    @Test fun `moving last to first returns only changed positions`() {
        assertThat(reorderedPositions(listOf(3L, 1L, 2L), mapOf(1L to 0, 2L to 1, 3L to 2)))
            .containsExactly(3L, 0, 1L, 1, 2L, 2)
    }

    @Test fun `gaps from deletes are compacted only where order changes`() {
        assertThat(reorderedPositions(listOf(1L, 3L), mapOf(1L to 0, 3L to 5))).containsExactly(3L, 1)
    }

    @Test fun `unknown ids are ignored`() {
        assertThat(reorderedPositions(listOf(9L, 1L), mapOf(1L to 0))).containsExactly(1L, 1)
    }

    @Test fun `cloud presets without position go last by createdAt`() {
        val a = ExpensePreset(id = "a", type = "a", category = "FOOD", position = 1, createdAt = Date(5_000))
        val b = ExpensePreset(id = "b", type = "b", category = "FOOD", position = null, createdAt = Date(2_000))
        val c = ExpensePreset(id = "c", type = "c", category = "FOOD", position = 0, createdAt = Date(9_000))
        val d = ExpensePreset(id = "d", type = "d", category = "FOOD", position = null, createdAt = Date(1_000))
        val result = normalizeCloudPresetOrder(listOf(a, b, c, d))
        assertThat(result.map { it.type }).containsExactly("c", "a", "d", "b").inOrder()
        assertThat(result.map { it.position }).containsExactly(0, 1, 2, 3).inOrder()
    }
}
