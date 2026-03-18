package com.luna.budgetapp.domain.model

import java.time.LocalDate

data class DailyExpense(
    val date: LocalDate,
    val total: Double
)

fun List<Expense>.toDailyExpenses(): List<DailyExpense> {
    return this
        .groupBy { it.date.toLocalDate() }
        .map { (date, expenses) ->
            DailyExpense(
                date = date,
                total = expenses.sumOf { it.amount }
            )
        }
        .sortedBy { it.date }
}

fun List<Expense>.toLast7DaysExpenses(): List<DailyExpense> {
    val today = LocalDate.now()

    val grouped = this.groupBy { it.date.toLocalDate() }

    return (6 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        val expenses = grouped[date].orEmpty()

        DailyExpense(
            date = date,
            total = expenses.sumOf { it.amount }
        )
    }
}
