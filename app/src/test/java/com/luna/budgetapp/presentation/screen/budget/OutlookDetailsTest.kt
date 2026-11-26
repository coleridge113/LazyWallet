package com.luna.budgetapp.presentation.screen.budget

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.presentation.screen.budget.model.OutlookDetails
import com.luna.budgetapp.presentation.screen.budget.model.paceFraction
import java.time.LocalDate
import org.junit.Test

class OutlookDetailsTest {
    @Test
    fun usedFraction_underTarget() {
        assertThat(OutlookDetails(0, 28_000_00, 12_300_00).usedFraction!!)
            .isWithin(0.0001f).of(0.4393f)
    }

    @Test
    fun usedFraction_atTarget() {
        assertThat(OutlookDetails(0, 100, 100).usedFraction).isEqualTo(1f)
    }

    @Test
    fun usedFraction_overspent_isUnclamped() {
        assertThat(OutlookDetails(0, 100, 120).usedFraction).isEqualTo(1.2f)
    }

    @Test
    fun usedFraction_noBudgets_isNull() {
        assertThat(OutlookDetails(0, 0, 500).usedFraction).isNull()
    }

    @Test
    fun paceFraction_firstOfMonth() {
        assertThat(paceFraction(LocalDate.of(2026, 11, 1))).isEqualTo(1f / 30)
    }

    @Test
    fun paceFraction_midMonth() {
        assertThat(paceFraction(LocalDate.of(2026, 11, 15))).isEqualTo(0.5f)
    }

    @Test
    fun paceFraction_lastDay() {
        assertThat(paceFraction(LocalDate.of(2026, 11, 30))).isEqualTo(1f)
    }

    @Test
    fun paceFraction_leapFebruary() {
        assertThat(paceFraction(LocalDate.of(2028, 2, 29))).isEqualTo(1f)
        assertThat(paceFraction(LocalDate.of(2028, 2, 15))).isEqualTo(15f / 29)
    }
}
