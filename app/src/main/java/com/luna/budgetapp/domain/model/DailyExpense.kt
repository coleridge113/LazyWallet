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
