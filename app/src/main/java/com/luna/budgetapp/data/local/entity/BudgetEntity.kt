package com.luna.budgetapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.luna.budgetapp.domain.model.BudgetFrequency
import java.time.LocalDate

@Entity(tableName = "budget")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    @ColumnInfo(name = "limit") val limit: Double,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "frequency") val frequency: BudgetFrequency,
    @ColumnInfo(name = "start_date") val startDate: LocalDate,
    @ColumnInfo(name = "end_date") val endDate: LocalDate? = null
)
