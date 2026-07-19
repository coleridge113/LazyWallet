package com.luna.budgetapp.data.firebase.models

import com.google.firebase.firestore.DocumentId
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.DateFilter
import java.util.Date

data class Budget(
    @DocumentId val id: String = "",
    val limit: Double = 0.0,
    val name: String = "",
    val frequency: String = "Monthly",
    val interactors: List<Category> = emptyList(),
    val startDate: Date = Date(),
    val endDate: Date? = null,
)
