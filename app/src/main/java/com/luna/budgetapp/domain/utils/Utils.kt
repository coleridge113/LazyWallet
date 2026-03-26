package com.luna.budgetapp.domain.utils

fun parseAmountExpression(input: String): Double {
    val sanitized = input.replace(" ", "")

    val tokens = Regex("([+-]?\\d+(?:\\.\\d+)?)")
        .findAll(sanitized)
        .map { it.value.toDouble() }

    return tokens.sum()
}
