package com.luna.budgetapp.data.firebasemodels

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class ExpensePreset(
    @DocumentId val id: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val type: String = "",
    val createdAt: Date = Date()
)
