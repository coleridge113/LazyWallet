package com.luna.budgetapp.presentation.screen.analysis.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luna.budgetapp.domain.model.Granularity
import com.luna.budgetapp.domain.model.PeriodTotal
import com.luna.budgetapp.presentation.screen.utils.toPeso
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import java.time.LocalDate
import java.util.Locale

private val ChartHeight = 160.dp
private val LabelHeight = 20.dp

private fun barsInView(granularity: Granularity): Int = when (granularity) {
    Granularity.DAY -> 7
    Granularity.WEEK -> 6
    Granularity.MONTH -> 6
}

/** [periods] are oldest first; the row is reversed so the newest bar sits at the right edge. */
@Composable
fun SpendingHistoryChart(
    periods: List<PeriodTotal>,
    granularity: Granularity,
    selectedStart: LocalDate?,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reversed = remember(periods) { periods.asReversed() }
    val listState = rememberLazyListState()
    val locale = LocalConfiguration.current.locales[0]

    LaunchedEffect(granularity, reversed.firstOrNull()?.start) { listState.scrollToItem(0) }

    val visibleTotals by remember(reversed) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.mapNotNull { reversed.getOrNull(it.index)?.total }
        }
    }
    val visibleMax = visibleTotals.maxOrNull() ?: 0L
    val visibleAverage = if (visibleTotals.isEmpty()) 0.0 else visibleTotals.average()

    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedColor = MaterialTheme.colorScheme.primary
    val mutedColor = MaterialTheme.colorScheme.secondaryContainer

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val barSlot = maxWidth / barsInView(granularity)
        LazyRow(
            state = listState,
            reverseLayout = true,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ChartHeight + LabelHeight)
                .drawWithContent {
                    drawContent()
                    if (visibleMax > 0L && visibleAverage > 0.0) {
                        val chartPx = ChartHeight.toPx()
                        val y = chartPx - chartPx * (visibleAverage / visibleMax).toFloat()
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                        )
                    }
                },
        ) {
            items(reversed, key = { it.start.toEpochDay() }) { period ->
                Bar(
                    period = period,
                    granularity = granularity,
                    width = barSlot,
                    maxTotal = visibleMax,
                    selected = period.start == selectedStart,
                    selectedColor = selectedColor,
                    mutedColor = mutedColor,
                    onClick = { onSelect(period.start) },
                    locale = locale,
                )
            }
        }
    }
}

@Composable
private fun Bar(
    period: PeriodTotal,
    granularity: Granularity,
    width: Dp,
    maxTotal: Long,
    selected: Boolean,
    selectedColor: Color,
    mutedColor: Color,
    onClick: () -> Unit,
    locale: Locale,
) {
    val label = barLabel(period.start, granularity, locale)
    val amount = period.total.toPeso()
    val fraction = if (maxTotal > 0L) (period.total.toFloat() / maxTotal).coerceIn(0f, 1f) else 0f
    val barHeight by animateDpAsState(
        targetValue = if (period.total > 0L) (ChartHeight * fraction).coerceAtLeast(2.dp) else 2.dp,
        label = "barHeight",
    )
    Column(
        modifier = Modifier
            .width(width)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = "$label, $amount"
                this.selected = selected
                role = Role.Button
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.height(ChartHeight),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier
                    .width(width * 0.6f)
                    .height(barHeight),
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                color = if (selected) selectedColor else mutedColor,
            ) {}
        }
        Text(
            text = label,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            color = if (selected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .heightIn(min = LabelHeight)
                .padding(top = 4.dp),
        )
    }
}

private fun previewPeriods(count: Int, granularity: Granularity): List<PeriodTotal> {
    val end = LocalDate.of(2026, 11, 26)
    return (count - 1 downTo 0).map { i ->
        val start = when (granularity) {
            Granularity.DAY -> end.minusDays(i.toLong())
            Granularity.WEEK -> end.minusWeeks(i.toLong())
            Granularity.MONTH -> end.withDayOfMonth(1).minusMonths(i.toLong())
        }
        val total = if (i % 4 == 3) 0L else ((i * 37) % 23 + 3) * 10_000L
        PeriodTotal(start, start, total)
    }
}

@Preview(showBackground = true)
@Composable
private fun SpendingHistoryChartDayPreview() {
    LazyWalletTheme(darkTheme = false) {
        val p = previewPeriods(30, Granularity.DAY)
        SpendingHistoryChart(p, Granularity.DAY, p.last().start, {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SpendingHistoryChartMonthDarkPreview() {
    LazyWalletTheme(darkTheme = true) {
        val p = previewPeriods(14, Granularity.MONTH)
        SpendingHistoryChart(p, Granularity.MONTH, p.last().start, {})
    }
}
