package com.luna.budgetapp.presentation.screen.budget.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luna.budgetapp.R
import com.luna.budgetapp.presentation.screen.budget.model.MonthOutlook
import com.luna.budgetapp.presentation.screen.utils.toWholePeso
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OutlookChart(outlook: MonthOutlook, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val onSurface = scheme.onSurface
    val muted = scheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, color = muted)
    val budgetLabel = stringResource(R.string.outlook_budget_label, outlook.budgetTotal.toWholePeso())
    val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    val first = outlook.today.withDayOfMonth(1).format(fmt)
    val last = outlook.today.withDayOfMonth(outlook.daysInMonth).format(fmt)

    Column(modifier = modifier.fillMaxWidth().clearAndSetSemantics { }) {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val w = size.width
            val h = size.height
            val n = outlook.daysInMonth
            fun x(d: Int) = d.toFloat() / n * w
            val max = outlook.chartMax
            fun y(a: Long) = if (max <= 0L) h else h - a.toFloat() / max * h

            drawLine(
                color = onSurface.copy(alpha = 0.3f),
                start = Offset(0f, h), end = Offset(w, h), strokeWidth = 1.dp.toPx(),
            )
            if (max <= 0L) return@Canvas

            if (outlook.hasBudgets) {
                drawLine(
                    color = muted,
                    start = Offset(0f, y(0)), end = Offset(w, y(outlook.budgetTotal)),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                )
                val layout = measurer.measure(budgetLabel, labelStyle)
                val lx = (w - layout.size.width).coerceAtLeast(0f)
                val ly = (y(outlook.budgetTotal) - layout.size.height - 2.dp.toPx())
                    .coerceIn(0f, (h - layout.size.height).coerceAtLeast(0f))
                drawText(layout, topLeft = Offset(lx, ly))
            }

            val points = buildList {
                add(Offset(x(0), y(0)))
                for (d in 1..outlook.daysElapsed) {
                    add(Offset(x(d), y(outlook.runningTotal(outlook.today.withDayOfMonth(d)))))
                }
            }
            val line = Path().apply {
                points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
            }
            val area = Path().apply {
                addPath(line)
                lineTo(points.last().x, h)
                lineTo(points.first().x, h)
                close()
            }
            drawPath(area, primary.copy(alpha = 0.15f))
            drawPath(
                line, primary,
                style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            val todayPoint = points.last()
            outlook.projected?.let { proj ->
                drawLine(
                    color = primary,
                    start = todayPoint, end = Offset(x(n), y(proj)),
                    strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 5.dp.toPx())),
                )
            }
            drawCircle(primary, radius = 4.dp.toPx(), center = todayPoint)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(first, fontSize = 11.sp, lineHeight = 16.sp, color = muted)
            Text(last, fontSize = 11.sp, lineHeight = 16.sp, color = muted)
        }
    }
}
