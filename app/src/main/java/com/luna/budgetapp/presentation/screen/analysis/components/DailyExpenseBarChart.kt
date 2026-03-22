package com.luna.budgetapp.presentation.screen.analysis.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.toLast7DaysExpenses
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun DailyExpenseBarChart(
    modifier: Modifier = Modifier,
    expenses: List<Expense>,
    dBarColor: Color = MaterialTheme.colorScheme.primary,
    selectedDate: LocalDate,
    onClickBar: (LocalDate) -> Unit,
) {

    val dailyData = remember(expenses) {
        expenses.toLast7DaysExpenses()
    }

    val maxValue = dailyData.maxOfOrNull { it.total } ?: 1.0

    var animationTarget by remember { mutableStateOf(0f) }
    val animationProgress by animateFloatAsState(
        targetValue = animationTarget,
        animationSpec = tween(
            durationMillis = 700,
            easing = FastOutSlowInEasing
        ),
        label = "bar-rise"
    )

    val animatedColors = dailyData.map { item ->

        val isSelected = item.date == selectedDate

        animateColorAsState(
            targetValue =
            if (isSelected) Color(0xFFE53935)
            else dBarColor,
            animationSpec = tween(250),
            label = "bar-color"
        ).value
    }

    LaunchedEffect(Unit) {
        animationTarget = 0f
        animationTarget = 1f
    }

    val barPositions = remember { mutableStateListOf<Pair<Rect, LocalDate>>() }
    barPositions.clear()

    Column(modifier = modifier.padding(16.dp)) {

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .pointerInput(dailyData) {
                    detectTapGestures { offset ->
                        barPositions.forEach { (rect, date) ->
                            if (rect.contains(offset)) {
                                onClickBar(date)
                            }
                        }
                    }
                }
        ) {

            if (dailyData.isEmpty()) return@Canvas

            val barWidth = size.width / (dailyData.size * 1.65f)
            val spacing = barWidth / 2

            dailyData.forEachIndexed { index, item ->

                val ratio = (item.total / maxValue).toFloat()

                val barHeight =
                    (size.height * 0.85f) *
                            ratio *
                            animationProgress   // 👈 animation here

                val x = index * (barWidth + spacing) + barWidth
                val y = size.height - barHeight

                val rect = Rect(
                    offset = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )

                barPositions.add(rect to item.date)

                val barColor = animatedColors[index]
                drawRoundRect(
                    color = barColor,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    cornerRadius = CornerRadius(14f, 14f)
                )

                drawContext.canvas.nativeCanvas.drawText(
                    "₱${item.total.toInt()}",
                    x + barWidth / 2,
                    y - 10,
                    android.graphics.Paint().apply {
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 28f
                        isAntiAlias = true
                        color = Color.White.toArgb()
                    }
                )
            }

            drawLine(
                color = Color.LightGray,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 2f
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewContent() {

    val now = LocalDateTime.of(2024, 1, 7, 12, 0)

    val dummyExpenses = listOf(
        Expense(1, "Coffee", 90.0, "Food", "Expense", now.minusDays(6)),
        Expense(2, "Lunch", 150.0, "Food", "Expense", now.minusDays(6)),
        Expense(3, "Grab", 200.0, "Transport", "Expense", now.minusDays(5)),
        Expense(4, "Dinner", 180.0, "Food", "Expense", now.minusDays(4)),
        Expense(5, "Snacks", 70.0, "Food", "Expense", now.minusDays(4)),
        Expense(6, "Groceries", 500.0, "Groceries", "Expense", now.minusDays(3)),
        Expense(7, "Coffee", 95.0, "Food", "Expense", now.minusDays(2)),
        Expense(8, "Taxi", 180.0, "Transport", "Expense", now.minusDays(2)),
        Expense(9, "Lunch", 160.0, "Food", "Expense", now.minusDays(1)),
        Expense(10, "Breakfast", 80.0, "Food", "Expense", now),
        Expense(11, "Dinner", 200.0, "Food", "Expense", now)
    )

    MaterialTheme {
        DailyExpenseBarChart(
            modifier = Modifier,
            expenses = dummyExpenses,
            selectedDate = LocalDate.now(),
            onClickBar = {}
        )
    }
}
