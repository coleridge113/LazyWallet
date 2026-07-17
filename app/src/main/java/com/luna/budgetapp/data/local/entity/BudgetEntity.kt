package com.luna.budgetapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.model.Category
import java.time.LocalDate

@Entity(tableName = "budget")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    @ColumnInfo(name = "limit") val limit: Double,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "frequency") val frequency: DateFilter,
    @ColumnInfo(name = "start_date") val startDate: LocalDate,
    @ColumnInfo(name = "end_date") val endDate: LocalDate? = null
)

@Entity(
    tableName = "budget_interactor_categories",
    primaryKeys = ["budgetId", "category"],
    foreignKeys = [
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budgetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["budgetId"])]
)
data class BudgetInteractorCategoryEntity(
    val budgetId: Long,
    val category: Category
)

data class BudgetWithInteractors(
    @Embedded val budget: BudgetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "budgetId"
    )
    val interactors: List<BudgetInteractorCategoryEntity>
)