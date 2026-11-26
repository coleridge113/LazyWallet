package com.luna.budgetapp.domain.model

import java.time.LocalDate

enum class Granularity { DAY, WEEK, MONTH }

data class DailyTotal(val date: LocalDate, val total: Long)

/** [end] is inclusive. */
data class PeriodTotal(val start: LocalDate, val end: LocalDate, val total: Long)

data class PeriodChange(val amount: Long, val percent: Double?)

data class SpendingHistory(val today: LocalDate, val periods: List<PeriodTotal>)
