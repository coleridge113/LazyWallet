package com.luna.budgetapp.domain.model

import java.time.LocalDateTime

data class ExpensePreset(
    val id: Long? = null,
    val amount: Double,
    val category: String,
    val type: String,
    val createdAt: LocalDateTime = LocalDateTime.now()
) : TableItem
