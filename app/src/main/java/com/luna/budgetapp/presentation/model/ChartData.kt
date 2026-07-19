package com.luna.budgetapp.presentation.model

import com.luna.budgetapp.ui.theme.OthersChartColor
import com.luna.budgetapp.domain.model.Category

data class ChartData(
    val category: Category,
    val subtotal: Long,
) {
    val color = CategoryOptions.entries
        .find { it.name == category.name }
        ?.chartColor
        ?: OthersChartColor
}
