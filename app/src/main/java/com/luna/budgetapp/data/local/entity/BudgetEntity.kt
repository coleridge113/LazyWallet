package com.luna.budgetapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "consumed") val consumed: Double,
    @ColumnInfo(name = "remaining") val remaining: Double,
    @ColumnInfo(name = "isExceeded") val isExceeded: Boolean,
    @ColumnInfo(name = "type") val type: String
)
