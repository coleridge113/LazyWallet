package com.luna.budgetapp.domain.model

import java.time.LocalDate

data class Budget(
    val id: Long = 0,
    val limit: Long,
    val name: String,
    val frequency: DateFilter,
    val interactors: List<Category>,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
)
