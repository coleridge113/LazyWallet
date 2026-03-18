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
}
