package com.luna.budgetapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.luna.budgetapp.domain.model.Category
import java.time.LocalDateTime

@Entity(tableName = "expense_presets")
data class ExpensePresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    @ColumnInfo(name = "amount") val amount: Long,
    @ColumnInfo(name = "category") val category: Category,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime,
    @ColumnInfo(name = "position", defaultValue = "0") val position: Int = 0
)
