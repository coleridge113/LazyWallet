package com.luna.budgetapp.domain.utils

import kotlin.math.roundToLong

fun parseAmountExpression(input: String): Long {
    val sanitized = input.replace(" ", "")

    val tokens = Regex("([+-]?\\d+(?:\\.\\d+)?)")
        .findAll(sanitized)
        .map { it.value.toDouble() }

    return (tokens.sum() * 100).roundToLong()
}
