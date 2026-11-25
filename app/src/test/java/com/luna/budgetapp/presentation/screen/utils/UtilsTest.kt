package com.luna.budgetapp.presentation.screen.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UtilsTest {
    @Test
    fun toPeso_formatsCentsWithSymbolAndGrouping() {
        assertThat(123456L.toPeso()).isEqualTo("₱1,234.56")
    }

    @Test
    fun toPeso_zero() {
        assertThat(0L.toPeso()).isEqualTo("₱0.00")
    }

    @Test
    fun toPeso_negativeInParentheses() {
        assertThat((-5050L).toPeso()).isEqualTo("(₱50.50)")
    }
}
