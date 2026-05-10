package com.luna.budgetapp.data.firebasemodels

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class Expense(
    @DocumentId val id: String = "",
    val name: String? = null,
    val amount: Double = 0.0,
    val category: String = "",
    val type: String = "",
    val date: Date = Date()
)

