package com.luna.budgetapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    @ColumnInfo(name =  "name") val name: String?,
    @ColumnInfo(name =  "amount") val amount: Double,
    @ColumnInfo(name =  "category") val category: String,
    @ColumnInfo(name =  "type") val type: String,
    @ColumnInfo(name =  "date") val date: LocalDateTime
)
