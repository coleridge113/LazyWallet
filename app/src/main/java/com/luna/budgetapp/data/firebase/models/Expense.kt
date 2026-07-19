package com.luna.budgetapp.data.firebase.models

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class Expense(
    @DocumentId val id: String = "",
    val name: String? = null,
    val amount: Long = 0L,
    val category: String = "",
    val type: String = "",
    val date: Date = Date()
)

