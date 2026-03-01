package com.luna.budgetapp.presentation.screen.expenselist.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.presentation.model.ChartData
import com.luna.budgetapp.presentation.screen.utils.formatToPercentage
import com.luna.budgetapp.ui.theme.OthersChartColor

@Composable
fun ExpenseChart(
    modifier: Modifier = Modifier,
    chartDataList: List<ChartData>,
    totalAmount: Double,
    showDialog: () -> Unit,
    onClickCenter: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExpenseDonutChart(
            chartDataList = chartDataList,
            totalAmount = totalAmount,
            onClickCenter = onClickCenter,
            modifier = Modifier.fillMaxWidth(0.6f)
                .offset(x = 16.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        ExpenseChartLegends(
            chartDataList = chartDataList,
            totalAmount = totalAmount,
            showDialog = showDialog
        )
    }

}

@Composable
fun ExpenseDonutChart(
    modifier: Modifier = Modifier,
    totalAmount: Double,
    chartDataList: List<ChartData>,
    onClickCenter: () -> Unit
) {
    val animationProgress = remember(chartDataList) { Animatable(0f) }
    val SLICE_GAP = 2f

    LaunchedEffect(chartDataList) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing
            )
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.aspectRatio(1f)
    ) {

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            var startAngle = -90f
            val gapAngle = if (chartDataList.size > 1) SLICE_GAP else 0f

            if (totalAmount == 0.0) {
                val sweepAngle =
                    (360f * animationProgress.value)
                drawArc(
                    color = OthersChartColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 80f)
                )
                return@Canvas
            }

            chartDataList.forEach { slice ->
                val rawSweep =
                    ((slice.subtotal / totalAmount) * 360f * animationProgress.value).toFloat()

                val sweepAngle = (rawSweep - gapAngle).coerceAtLeast(0f)

                drawArc(
                    color = slice.color,
                    startAngle = startAngle + gapAngle / SLICE_GAP,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 80f)
                )

                startAngle += rawSweep
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onClickCenter() }
        ) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "₱%,.2f".format(totalAmount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ExpenseChartLegends(
    modifier: Modifier = Modifier,
    chartDataList: List<ChartData>,
    totalAmount: Double,
    showDialog: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.clickable { showDialog() }
    ) {
        items(chartDataList) { item ->
            val portion = item.subtotal / totalAmount
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = item.color,
                            shape = CircleShape
                        )
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "${portion.formatToPercentage()} ${item.category.displayName}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

        }
    }
}

@Preview
@Composable
fun ExpenseDonutChartPreview() {
   val totalAmount = 240.0
   val chartDataList = listOf(
       ChartData(
           category = Category.FOOD,
           subtotal = 100.0
       ),
       ChartData(
           category = Category.BEVERAGE,
           subtotal = 140.0
       ),
       ChartData(
           category = Category.COMMUTE,
           subtotal = 140.0
       ),
       ChartData(
           category = Category.BILLS,
           subtotal = 140.0
       ),
   )
   Column(
       verticalArrangement = Arrangement.spacedBy(32.dp),
   ) {
       ExpenseDonutChart(
           totalAmount = totalAmount,
           chartDataList = chartDataList,
           onClickCenter = {}
       )

       ExpenseChartLegends(
           totalAmount = totalAmount,
           chartDataList = chartDataList,
           showDialog = {}
       )
   }
}
