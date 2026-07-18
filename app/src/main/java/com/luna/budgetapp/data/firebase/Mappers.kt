package com.luna.budgetapp.data.firebase

import com.luna.budgetapp.data.firebase.models.Budget
import com.luna.budgetapp.data.firebase.models.CategoryFilter
import com.luna.budgetapp.data.firebase.models.Expense
import com.luna.budgetapp.data.firebase.models.ExpensePreset
import com.luna.budgetapp.data.local.entity.BudgetEntity
import com.luna.budgetapp.data.local.entity.CategoryFilterEntity
import com.luna.budgetapp.data.local.entity.ExpenseEntity
import com.luna.budgetapp.data.local.entity.ExpensePresetEntity
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.DateFilter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

fun LocalDateTime.toDate(): Date {
    return Date.from(this.atZone(ZoneId.systemDefault()).toInstant())
}

fun Date.toLocalDateTime(): LocalDateTime {
    return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
}

fun LocalDate.toDate(): Date {
    return Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
}

fun Date.toLocalDate(): LocalDate {
    return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}

fun ExpenseEntity.toFirestoreModel(): Expense {
    return Expense(
        name = this.name,
        amount = this.amount,
        category = this.category,
        type = this.type,
        date = this.date.toDate() 
    )
}

fun Expense.toEntity(): ExpenseEntity {
    return ExpenseEntity(
        remoteId = this.id.ifBlank { null },
        name = this.name,
        amount = this.amount,
        category = this.category,
        type = this.type,
        date = this.date.toLocalDateTime()
    )
}

fun ExpensePresetEntity.toFirestoreModel(): ExpensePreset {
    return ExpensePreset(
        amount = this.amount,
        category = this.category,
        type = this.type,
        createdAt = this.createdAt.toDate()
    )
}

fun ExpensePreset.toEntity(): ExpensePresetEntity {
    return ExpensePresetEntity(
        id = 0,
        remoteId = this.id.ifBlank { null },
        amount = this.amount,
        category = this.category,
        type = this.type,
        createdAt = this.createdAt.toLocalDateTime()
    )
}

fun CategoryFilterEntity.toFirestoreModel(): CategoryFilter {
    return CategoryFilter(
        profileName = this.profileName,
        category = this.category.getDisplayName(),
        active = this.isActive
    )
}

fun CategoryFilter.toEntity(): CategoryFilterEntity {
    return CategoryFilterEntity(
        profileName = this.profileName,
        category = Category.entries.first { it.getDisplayName() == this.category },
        isActive = this.active
    )
}

fun BudgetEntity.toFirestoreModel(interactors: List<Category>): Budget {
    val frequencyString = when (val filter = this.frequency) {
        DateFilter.Daily -> "DAILY"
        DateFilter.Weekly -> "WEEKLY"
        DateFilter.BiWeekly -> "BI_WEEKLY"
        DateFilter.Monthly -> "MONTHLY"
        DateFilter.Quarterly -> "QUARTERLY"
        DateFilter.BiYearly -> "BI_YEARLY"
        DateFilter.Yearly -> "YEARLY"
        DateFilter.Last7Days -> "LAST_7_DAYS"
        is DateFilter.Custom -> "CUSTOM|${filter.start}|${filter.end}"
    }

    return Budget(
        limit = this.limit,
        name = this.name,
        frequency = frequencyString,
        interactors = interactors,
        startDate = this.startDate.toDate(),
        endDate = this.endDate?.toDate()
    )
}

fun Budget.toEntity(): BudgetEntity {
    val frequencyFilter = when (val freq = this.frequency) {
        is String -> {
            val parts = freq.split("|")
            when (parts[0]) {
                "DAILY" -> DateFilter.Daily
                "WEEKLY" -> DateFilter.Weekly
                "BI_WEEKLY" -> DateFilter.BiWeekly
                "MONTHLY" -> DateFilter.Monthly
                "QUARTERLY" -> DateFilter.Quarterly
                "BI_YEARLY" -> DateFilter.BiYearly
                "YEARLY" -> DateFilter.Yearly
                "LAST_7_DAYS" -> DateFilter.Last7Days
                "CUSTOM" -> DateFilter.Custom(
                    start = parts.getOrNull(1)?.toLongOrNull() ?: 0L,
                    end = parts.getOrNull(2)?.toLongOrNull()
                )
                else -> DateFilter.Monthly
            }
        }
        is Map<*, *> -> {
            // Smart Recovery for legacy Map data
            when {
                freq.containsKey("start") -> {
                    DateFilter.Custom(
                        start = (freq["start"] as? Number)?.toLong() ?: 0L,
                        end = (freq["end"] as? Number)?.toLong()
                    )
                }
                this.name.contains("Daily", ignoreCase = true) -> DateFilter.Daily
                this.name.contains("Weekly", ignoreCase = true) -> DateFilter.Weekly
                this.name.contains("Groceries", ignoreCase = true) || 
                this.name.contains("Bi-Weekly", ignoreCase = true) -> DateFilter.BiWeekly
                this.name.contains("Yearly", ignoreCase = true) -> DateFilter.Yearly
                else -> DateFilter.Monthly
            }
        }
        else -> DateFilter.Monthly
    }

    return BudgetEntity(
        remoteId = this.id.ifBlank { null },
        limit = this.limit,
        name = this.name,
        frequency = frequencyFilter,
        startDate = this.startDate.toLocalDate(),
        endDate = this.endDate?.toLocalDate()
    )
}
