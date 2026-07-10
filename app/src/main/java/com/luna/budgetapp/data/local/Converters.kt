package com.luna.budgetapp.data.local

import androidx.room.TypeConverter
import com.luna.budgetapp.domain.model.BudgetFrequency
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
    fun fromBudgetFrequency(frequency: BudgetFrequency): String = frequency.name

    @TypeConverter
    fun toBudgetFrequency(value: String): BudgetFrequency = BudgetFrequency.valueOf(value)
}
