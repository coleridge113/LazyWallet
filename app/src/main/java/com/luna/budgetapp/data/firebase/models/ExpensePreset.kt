package com.luna.budgetapp.data.firebase.models

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class ExpensePreset(
    @DocumentId val id: String = "",
    val amount: Long = 0L,
    val category: String = "",
    val type: String = "",
    val createdAt: Date = Date()
)
