package com.luna.budgetapp.presentation.screen.budget.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.R
import com.luna.budgetapp.presentation.screen.budget.model.OutlookDetails
import com.luna.budgetapp.presentation.screen.utils.toPeso
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val START_ANGLE = 150f
private const val SWEEP_ANGLE = 240f

@Composable
fun BudgetOutlookCard(
    modifier: Modifier = Modifier,
    details: OutlookDetails
) {
    val used = details.usedFraction
    val hasBudgets = used != null
    val actual = details.actualSpend.toPeso()
    val projected = details.projectedSpend.toPeso()
    val remaining = abs(details.remaining).toPeso()
    val suffix = stringResource(
        if (details.remaining > 0) R.string.budget_suffix_left else R.string.budget_suffix_overspent
    )
    val noBudgets = stringResource(R.string.outlook_no_budgets)
    val description = if (hasBudgets) {
        stringResource(R.string.outlook_cd, actual, projected, remaining, suffix)
    } else {
        noBudgets
    }

    val fillColor = budgetProgressColor(used ?: 0f)
    val trackColor = if (hasBudgets) {
        fillColor.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    }
    val tickColor = MaterialTheme.colorScheme.onSurface
    val animatedFill by animateFloatAsState(
        targetValue = min(used ?: 0f, 1f).coerceAtLeast(0f),
        label = "outlookFill"
    )
    val pace = details.paceFraction

    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val strokePx = 12.dp.toPx()
                    val inset = strokePx * 1.5f
                    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                    val topLeft = Offset(inset, inset)
                    val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)
                    drawArc(
                        color = trackColor,
                        startAngle = START_ANGLE,
                        sweepAngle = SWEEP_ANGLE,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )
                    if (hasBudgets) {
                        if (animatedFill > 0f) {
                            drawArc(
                                color = fillColor,
                                startAngle = START_ANGLE,
                                sweepAngle = SWEEP_ANGLE * animatedFill,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = stroke
                            )
                        }
                        val radius = arcSize.width / 2
                        val center = Offset(size.width / 2, size.height / 2)
                        val angle = Math.toRadians((START_ANGLE + SWEEP_ANGLE * pace).toDouble())
                        val dir = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                        drawLine(
                            color = tickColor,
                            start = center + dir * (radius - strokePx),
                            end = center + dir * (radius + strokePx),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (hasBudgets) {
                        Text(
                            text = abs(details.remaining).toPeso(),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = suffix,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = noBudgets,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            if (hasBudgets) {
                Text(
                    text = stringResource(R.string.outlook_spent_of, actual, projected),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private operator fun Offset.times(f: Float) = Offset(x * f, y * f)

@Preview(name = "UnderPace")
@Preview(name = "UnderPace dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun UnderPacePreview() = OutlookPreview(
    OutlookDetails(30_000_00L, 28_000_00L, 12_300_00L, 0.867f)
)

@Preview(name = "OverPace")
@Preview(name = "OverPace dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OverPacePreview() = OutlookPreview(
    OutlookDetails(30_000_00L, 28_000_00L, 22_000_00L, 0.4f)
)

@Preview(name = "Overspent")
@Preview(name = "Overspent dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OverspentPreview() = OutlookPreview(
    OutlookDetails(30_000_00L, 28_000_00L, 31_000_00L, 0.9f)
)

@Preview(name = "NoBudgets")
@Preview(name = "NoBudgets dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NoBudgetsPreview() = OutlookPreview(OutlookDetails(0L, 0L, 0L))

@Composable
private fun OutlookPreview(details: OutlookDetails) {
    LazyWalletTheme {
        BudgetOutlookCard(details = details)
    }
}
