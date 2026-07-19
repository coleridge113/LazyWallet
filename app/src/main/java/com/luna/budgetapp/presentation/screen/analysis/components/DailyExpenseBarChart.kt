package com.luna.budgetapp.presentation.screen.analysis.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.toLast7DaysExpenses
import com.luna.budgetapp.presentation.screen.utils.formatToDay
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun DailyExpenseBarChart(
    modifier: Modifier = Modifier,
    expenses: List<Expense>,
    dBarColor: Color = MaterialTheme.colorScheme.primary,
    selectedDate: LocalDate?,
    onClickBar: (LocalDate) -> Unit,
) {

    val dailyData = remember(expenses) {
        expenses.toLast7DaysExpenses()
    }

    val maxValue = dailyData.maxOfOrNull { it.total } ?: 1.0

    var animationTarget by remember { mutableFloatStateOf(0f) }
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
                if (isSelected)
                    Color(0xFFE53935)
                else
                    dBarColor,
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

    val labelColor = MaterialTheme.colorScheme.onBackground.toArgb()

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

            val bottomPadding = 40f
            val topPadding = 60f
            val usableHeight = size.height - bottomPadding - topPadding

            val barWidth = size.width / (dailyData.size * 1.65f)
            val spacing = barWidth / 2

            dailyData.forEachIndexed { index, item ->

                val ratio = (item.total / maxValue).toFloat()

                val barHeight = usableHeight * ratio * animationProgress

                val x = index * (barWidth + spacing) + barWidth
                val y = size.height - bottomPadding - barHeight

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
                    y - 15f,
                    android.graphics.Paint().apply {
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 28f
                        isAntiAlias = true
                        color = labelColor
                    }
                )

                drawContext.canvas.nativeCanvas.drawText(
                    item.date.formatToDay(),
                    x + barWidth / 2,
                    size.height - 5f,
                    android.graphics.Paint().apply {
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 28f
                        isAntiAlias = true
                        color = labelColor
                    }
                )
            }

            drawLine(
                color = Color.LightGray,
                start = Offset(0f, size.height - bottomPadding),
                end = Offset(size.width, size.height - bottomPadding),
                strokeWidth = 2f
            )
        }
    }
}

@Preview(
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
private fun DailyExpenseBarChartPreview() {
    LazyWalletTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            DailyExpenseBarChart(
                expenses = listOf(
                    Expense(amount = 100.0, category = "Food", type = "Lunch", date = LocalDateTime.now()),
                    Expense(amount = 50.0, category = "Transport", type = "Bus", date = LocalDateTime.now().minusDays(1)),
                    Expense(amount = 200.0, category = "Shopping", type = "Clothes", date = LocalDateTime.now().minusDays(2)),
                    Expense(amount = 150.0, category = "Food", type = "Dinner", date = LocalDateTime.now().minusDays(3)),
                    Expense(amount = 80.0, category = "Transport", type = "Taxi", date = LocalDateTime.now().minusDays(4)),
                    Expense(amount = 120.0, category = "Entertainment", type = "Movie", date = LocalDateTime.now().minusDays(5)),
                    Expense(amount = 300.0, category = "Bills", type = "Rent", date = LocalDateTime.now().minusDays(6))
                ),
                selectedDate = LocalDate.now(),
                onClickBar = {}
            )
        }
    }
}
