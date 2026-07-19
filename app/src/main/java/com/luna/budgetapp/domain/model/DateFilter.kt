package com.luna.budgetapp.domain.model

import java.time.Instant
import java.time.temporal.WeekFields
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

sealed class DateFilter {

    abstract fun resolve(
        now: LocalDate = LocalDate.now(),
        locale: Locale = Locale.getDefault()
    ): DateRange

    fun getFriendlyName(): String {
        return when (this) {
            Daily -> "Daily"
            Weekly -> "Weekly"
            BiWeekly -> "Every 2 weeks"
            Monthly -> "Monthly"
            Quarterly -> "Quarterly"
            BiYearly -> "Every 6 months"
            Yearly -> "Yearly"
            Last7Days -> "Last 7 days"
            is Custom -> "Custom"
        }
    }

    data object Daily : DateFilter() {
        override fun resolve(now: LocalDate, locale: Locale) =
            DateRange(
                start = now.atStartOfDay(),
                end = now.atTime(LocalTime.MAX)
            )
    }

    data object Weekly : DateFilter() {
        override fun resolve(now: LocalDate, locale: Locale): DateRange {
            val weekFields = WeekFields.of(locale)

            val start = now.with(weekFields.dayOfWeek(), 1)
            val end = now.with(weekFields.dayOfWeek(), 7)

            return DateRange(
                start = start.atStartOfDay(),
                end = end.atTime(LocalTime.MAX)
            )
        }
    }

    data object BiWeekly : DateFilter() {
        override fun resolve(now: LocalDate, locale: Locale): DateRange {
            val weekFields = WeekFields.of(locale)
            val weekOfYear = now.get(weekFields.weekOfWeekBasedYear())
            
            val isFirstWeekOfPeriod = (weekOfYear - 1) % 2 == 0
            
            val start = if (isFirstWeekOfPeriod) {
                now.with(weekFields.dayOfWeek(), 1)
            } else {
                now.minusWeeks(1).with(weekFields.dayOfWeek(), 1)
            }
            
            val end = if (isFirstWeekOfPeriod) {
                now.plusWeeks(1).with(weekFields.dayOfWeek(), 7)
            } else {
                now.with(weekFields.dayOfWeek(), 7)
            }

            return DateRange(
                start = start.atStartOfDay(),
                end = end.atTime(LocalTime.MAX)
            )
        }
    }

    data object Last7Days : DateFilter() {
        override fun resolve(now: LocalDate, locale: Locale): DateRange {

            val end = now
            val start = now.minusDays(6)

            return DateRange(
                start = start.atStartOfDay(),
                end = end.atTime(LocalTime.MAX)
            )
        }
    }

    data object Monthly : DateFilter() {
        override fun resolve(now: LocalDate, locale: Locale) =
            DateRange(
                start = now.withDayOfMonth(1).atStartOfDay(),
                end = now.withDayOfMonth(now.lengthOfMonth()).atTime(LocalTime.MAX)
            )
    }

    data object Quarterly : DateFilter() {
        override fun resolve(now: LocalDate, locale: Locale): DateRange {
            val quarter = (now.monthValue - 1) / 3
            val startMonth = quarter * 3 + 1
            val start = now.withMonth(startMonth).withDayOfMonth(1)
            val end = start.plusMonths(2).let { 
                it.withDayOfMonth(it.lengthOfMonth())
            }
            return DateRange(
                start = start.atStartOfDay(),
                end = end.atTime(LocalTime.MAX)
            )
        }
    }

    data object BiYearly : DateFilter() {
        override fun resolve(now: LocalDate, locale: Locale): DateRange {
            val half = (now.monthValue - 1) / 6
            val startMonth = half * 6 + 1
            val start = now.withMonth(startMonth).withDayOfMonth(1)
            val end = start.plusMonths(5).let {
                it.withDayOfMonth(it.lengthOfMonth())
            }
            return DateRange(
                start = start.atStartOfDay(),
                end = end.atTime(LocalTime.MAX)
            )
        }
    }

    data object Yearly : DateFilter() {
        override fun resolve(now: LocalDate, locale: Locale): DateRange {
            val start = now.withDayOfYear(1)
            val end = now.withDayOfYear(now.lengthOfYear())

            return DateRange(
                start = start.atStartOfDay(),
                end = end.atTime(LocalTime.MAX)
            )
        }
    }

    data class Custom(
        val start: Long = 0L,
        val end: Long? = 0L
    ) : DateFilter() {

        override fun resolve(now: LocalDate, locale: Locale): DateRange {

            val zone = ZoneId.systemDefault()

            val startDateTime = Instant.ofEpochMilli(start)
                .atZone(zone)
                .toLocalDate()
                .atStartOfDay()

            val endDateTime = if (end != null) {
                    Instant.ofEpochMilli(end)
                        .atZone(zone)
                        .toLocalDate()
                        .atTime(LocalTime.MAX)
                } else {
                    Instant.ofEpochMilli(start)
                        .atZone(zone)
                        .toLocalDate()
                        .atTime(LocalTime.MAX)

                }

            return DateRange(
                start = startDateTime,
                end = endDateTime
            )
        }
    }

    companion object {
        val budgetFrequencies by lazy {
            listOf(
                Daily,
                Weekly,
                BiWeekly,
                Monthly,
                Quarterly,
                BiYearly,
                Yearly,
                Custom()
            )
        }
    }
}

fun String.getDateFilter(): DateFilter {
    return when (this) {
        "Daily" -> DateFilter.Daily
        "Weekly" -> DateFilter.Weekly
        "Every 2 weeks", "BiWeekly" -> DateFilter.BiWeekly
        "Monthly" -> DateFilter.Monthly
        "Quarterly" -> DateFilter.Quarterly
        "Every 6 months", "BiYearly" -> DateFilter.BiYearly
        "Yearly" -> DateFilter.Yearly
        "Last 7 days" -> DateFilter.Last7Days
        "Custom" -> DateFilter.Custom()
        else -> DateFilter.Monthly
    }
}
