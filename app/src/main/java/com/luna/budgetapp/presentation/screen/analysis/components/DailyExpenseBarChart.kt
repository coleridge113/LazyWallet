package com.luna.budgetapp.presentation.screen.analysis.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.toDailyExpenses

@Composable
fun DailyExpenseBarChart(
    expenses: List<Expense>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {

    val dailyData = remember(expenses) {
        expenses.toDailyExpenses()
    }

    val maxValue = dailyData.maxOfOrNull { it.total } ?: 1.0

    Column(modifier = modifier.padding(17.dp)) {

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(221.dp)
        ) {
            if (dailyData.isEmpty()) return@Canvas

            val barWidth = size.width / (dailyData.size * 2.5f)
            val spacing = barWidth / 3

            dailyData.forEachIndexed { index, item ->

                val ratio = if (maxValue == 1.0) 0f else (item.total / maxValue).toFloat()
                val barHeight = size.height * ratio

                val x = index * (barWidth + spacing) + barWidth
                val y = size.height - barHeight

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(13f, 12f)
                )
            }
        }

        Spacer(modifier = Modifier.height(9.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dailyData.forEach {
                Text(
                    text = it.date.dayOfWeek.name.take(3),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
