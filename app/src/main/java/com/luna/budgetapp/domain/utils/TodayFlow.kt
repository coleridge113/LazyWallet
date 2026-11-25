package com.luna.budgetapp.domain.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Emits the current date from [clock] on collection, then again each time the date changes
 * (at midnight), so date-range based queries do not go stale while the app stays open.
 */
fun todayFlow(clock: Clock): Flow<LocalDate> = flow {
    while (true) {
        val now = LocalDateTime.now(clock)
        emit(now.toLocalDate())
        val untilMidnight = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay())
        delay(untilMidnight.toMillis().coerceAtLeast(1L))
    }
}
