package com.luna.budgetapp.presentation.screen.expensepreset.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luna.budgetapp.R
import com.luna.budgetapp.presentation.screen.utils.toPeso
import com.luna.budgetapp.ui.theme.LazyWalletTheme

@Composable
fun ExpensePresetHero(
    total: Long,
    sparkline: List<Long>,
    modifier: Modifier = Modifier
) {
    val totalText = total.toPeso()
    val description = stringResource(R.string.cd_expense_preset_today_total, totalText)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.clearAndSetSemantics { contentDescription = description }
        ) {
            Text(
                text = stringResource(R.string.expense_preset_today),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = totalText,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Sparkline(
            values = sparkline,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun Sparkline(values: List<Long>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 120.dp, height = 40.dp)) {
        if (values.isEmpty()) return@Canvas
        val inset = 4.dp.toPx()
        val w = size.width - 2 * inset
        val h = size.height - 2 * inset
        val max = values.max()
        val stepX = if (values.size > 1) w / (values.size - 1) else 0f
        val points = values.mapIndexed { i, v ->
            val ratio = if (max > 0L) v.toFloat() / max else 0f
            Offset(inset + i * stepX, inset + h * (1f - ratio))
        }
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(color = color, radius = 3.5.dp.toPx(), center = points.last())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF282828)
@Composable
private fun ExpensePresetHeroPreview() {
    LazyWalletTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ExpensePresetHero(
                total = 45000,
                sparkline = listOf(21000, 3000, 51000, 23000, 17000, 20000, 25000)
            )
        }
    }
}
