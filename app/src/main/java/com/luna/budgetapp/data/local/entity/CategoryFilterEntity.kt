package com.luna.budgetapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.luna.budgetapp.domain.model.Category

@Entity(
    tableName = "category_filter",
    primaryKeys = ["profile_name", "category"]
)
data class CategoryFilterEntity(
    @ColumnInfo(name = "profile_name") 
    val profileName: String,
    val category: Category,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean
)
