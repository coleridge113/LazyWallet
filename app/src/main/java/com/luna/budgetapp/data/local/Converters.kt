package com.luna.budgetapp.data.local

import androidx.room.TypeConverter
import com.luna.budgetapp.domain.model.DateFilter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import com.luna.budgetapp.domain.model.Category
import java.time.LocalDate

object Converters {

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): Long? {
        return value
            ?.atZone(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()
    }

    @TypeConverter
    fun toLocalDateTime(value: Long?): LocalDateTime? {
        return value?.let {
            Instant.ofEpochMilli(it)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        }
    }

    @TypeConverter
    fun fromCategory(category: Category): String = category.name

    @TypeConverter
    fun toCategory(name: String): Category = Category.valueOf(name)

    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun dateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromDateFilter(filter: DateFilter): String {
        return when (filter) {
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
    }

    @TypeConverter
    fun toDateFilter(value: String): DateFilter {
        val parts = value.split("|")
        return when (parts[0]) {
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
            else -> DateFilter.Daily
        }
    }
}
