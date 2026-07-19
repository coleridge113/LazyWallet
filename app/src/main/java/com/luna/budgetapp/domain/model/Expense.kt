package com.luna.budgetapp.domain.model

import java.time.LocalDateTime

data class Expense(
    val id: Long? = null,
    val name: String? = null,
    val amount: Long,
    val category: String,
    val type: String,
    val date: LocalDateTime = LocalDateTime.now()
) : TableItem
