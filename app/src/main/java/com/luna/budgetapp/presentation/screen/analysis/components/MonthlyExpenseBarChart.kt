package com.luna.budgetapp.presentation.screen.analysis.components

import android.graphics.Paint
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
import com.luna.budgetapp.domain.model.DateRange
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.toLast7DaysExpenses
import com.luna.budgetapp.domain.model.toLast7MonthsExpenses
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun MonthlyExpenseBarChart(
    modifier: Modifier = Modifier,
    expenses: List<Expense>,
    dBarColor: Color = MaterialTheme.colorScheme.primary,
    selectedMonth: DateRange,
    onClickBar: (LocalDate) -> Unit
) {
    val monthlyData = remember(expenses) {
        expenses.toLast7MonthsExpenses()
    }
    val maxValue = monthlyData.maxOfOrNull { it.total } ?: 1.0
    var animationTarget by remember { mutableFloatStateOf(0f) }
    val animationProgress by animateFloatAsState(
        targetValue = animationTarget,
        animationSpec = tween(
            durationMillis = 700,
            easing = FastOutSlowInEasing
        ),
        label = "bar-rise"
    )
    val animatedColors = monthlyData.map { item ->
        val isSelected =
            item.date >= selectedMonth.start.toLocalDate() &&
            item.date <= selectedMonth.end.toLocalDate()
        
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
                .pointerInput(monthlyData) {
                    detectTapGestures { offset ->
                        barPositions.forEach { (rect, date) ->
                            if (rect.contains(offset)) {
                                onClickBar(date)
                            }
                        }
                    }
                }
        ) {
            if (monthlyData.isEmpty()) return@Canvas

            val barWidth = size.width / (monthlyData.size * 1.65f)
            val spacing = barWidth / 2

            monthlyData.forEachIndexed { index, item ->
                val ratio = (item.total / maxValue).toFloat()
                val barHeight = 
                    (size.height * 0.85f) * ratio * animationProgress

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
                    Paint().apply {
                        textAlign = Paint.Align.CENTER
                        textSize = 28f
                        isAntiAlias = true
                        color = labelColor
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
