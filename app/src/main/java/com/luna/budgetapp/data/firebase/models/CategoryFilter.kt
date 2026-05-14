package com.luna.budgetapp.data.firebase.models

import com.google.firebase.firestore.PropertyName

data class CategoryFilter(
    val profileName: String = "",
    val category: String = "",
    @get:PropertyName("active")
    @set:PropertyName("active")
    var active: Boolean = false
)
