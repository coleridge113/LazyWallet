package com.luna.budgetapp.presentation.screen.budget.components

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luna.budgetapp.R
import com.luna.budgetapp.domain.model.DailyTotal
import com.luna.budgetapp.presentation.screen.budget.model.DayKind
import com.luna.budgetapp.presentation.screen.budget.model.MonthOutlook
import com.luna.budgetapp.presentation.screen.budget.model.OutlookDay
import com.luna.budgetapp.presentation.screen.utils.toWholePeso
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CELL = 36.dp
private val GAP = 6.dp
private val CELL_SHAPE = RoundedCornerShape(8.dp)
private val GRID_WIDTH = CELL * 7 + GAP * 6
private val WEEKDAYS = listOf("S", "M", "T", "W", "T", "F", "S")

@Composable
fun BudgetHeatGridCard(
    outlook: MonthOutlook,
    selectedDay: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    onCollapse: () -> Unit,
    onOpenOutlook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = Locale.getDefault()
    // A selected date outside this month (month rolled over before the Route cleared it) counts as collapsed.
    val selectedDetailsDay = selectedDay?.let { sel -> outlook.days.firstOrNull { it.date == sel } }
    val monthName = outlook.today.format(DateTimeFormatter.ofPattern("MMMM", locale))
    val noBudgets = stringResource(R.string.outlook_no_budgets)
    val over = outlook.remaining < 0
    val suffix = stringResource(
        if (over) R.string.outlook_over_this_month else R.string.outlook_left_this_month
    )
    val amount = outlook.remaining.toWholePeso()
    val heroCd = if (outlook.hasBudgets) {
        stringResource(R.string.outlook_hero_cd, amount, suffix, monthName)
    } else {
        stringResource(R.string.outlook_hero_no_budgets_cd, noBudgets, monthName)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(16.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onOpenOutlook)
                .semantics { contentDescription = heroCd },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (outlook.hasBudgets) {
                Text(
                    text = amount,
                    fontSize = 36.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = if (over) budgetProgressColor(1.5f) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.alignByBaseline().clearAndSetSemantics { },
                )
                Text(
                    text = suffix,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline().clearAndSetSemantics { },
                )
            } else {
                Text(
                    text = noBudgets,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        HeatGrid(
            outlook = outlook,
            selectedDay = selectedDetailsDay?.date,
            onDayClick = onDayClick,
            locale = locale,
        )

        if (selectedDetailsDay != null) {
            DayDetails(
                day = selectedDetailsDay,
                outlook = outlook,
                column = (outlook.leadingBlanks + selectedDetailsDay.date.dayOfMonth - 1) % 7,
                onCollapse = onCollapse,
                locale = locale,
            )
        }
    }
}

@Composable
private fun HeatGrid(
    outlook: MonthOutlook,
    selectedDay: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    locale: Locale,
) {
    val cells: List<OutlookDay?> = List(outlook.leadingBlanks) { null } + outlook.days
    val weeks = cells.chunked(7)
    Column(
        modifier = Modifier.width(GRID_WIDTH),
        verticalArrangement = Arrangement.spacedBy(GAP),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
            WEEKDAYS.forEach {
                Text(
                    text = it,
                    modifier = Modifier
                        .width(CELL)
                        .clearAndSetSemantics { },
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        weeks.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(Modifier.size(CELL))
                    } else {
                        DayCell(
                            day = day,
                            selected = day.date == selectedDay,
                            onClick = { onDayClick(day.date) },
                            locale = locale,
                        )
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.size(CELL)) }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: OutlookDay,
    selected: Boolean,
    onClick: () -> Unit,
    locale: Locale,
) {
    val scheme = MaterialTheme.colorScheme
    val date = day.date.format(DateTimeFormatter.ofPattern("d MMMM", locale))
    if (day.kind == DayKind.Future) {
        val dashColor = scheme.onSurfaceVariant
        val cd = stringResource(R.string.outlook_cell_upcoming_cd, date)
        Box(
            modifier = Modifier
                .size(CELL)
                .alpha(0.4f)
                .clearAndSetSemantics { contentDescription = cd }
                .drawBehind {
                    val w = 1.dp.toPx()
                    drawRoundRect(
                        color = dashColor,
                        topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
                        size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                        cornerRadius = CornerRadius(8.dp.toPx()),
                        style = Stroke(
                            width = w,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                        ),
                    )
                }
        )
        return
    }

    val hasSpend = day.spent > 0
    val isToday = day.kind == DayKind.Today
    val base = if (hasSpend) stringResource(R.string.outlook_cell_cd, date, day.spent.toWholePeso())
    else stringResource(R.string.outlook_cell_no_spend_cd, date)
    val todaySuffix = if (isToday) stringResource(R.string.outlook_cell_today_suffix) else ""
    val selectedSuffix = if (selected) stringResource(R.string.outlook_cell_selected_suffix) else ""
    val cd = base + todaySuffix + selectedSuffix
    val borderModifier = when {
        selected || isToday -> Modifier.border(2.dp, scheme.onSurface, CELL_SHAPE)
        !hasSpend -> Modifier.border(
            BorderStroke(1.dp, scheme.onSurfaceVariant.copy(alpha = 0.5f)), CELL_SHAPE
        )
        else -> Modifier
    }

    Box(
        modifier = Modifier
            .size(CELL)
            .clip(CELL_SHAPE)
            .then(
                if (day.intensity != null)
                    Modifier.background(scheme.primary.copy(alpha = day.intensity))
                else Modifier
            )
            .then(borderModifier)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = cd
                role = Role.Button
                this.selected = selected
                onClick { onClick(); true }
            }
    )
}

@Composable
private fun DayDetails(
    day: OutlookDay,
    outlook: MonthOutlook,
    column: Int,
    onCollapse: () -> Unit,
    locale: Locale,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val centerInGrid = (CELL + GAP) * column + CELL / 2
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(12.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clearAndSetSemantics { }
        ) {
            val gridLeft = (size.width - GRID_WIDTH.toPx()) / 2
            val cx = gridLeft + centerInGrid.toPx()
            val y = 8.5.dp.toPx()
            val half = 8.dp.toPx()
            val path = Path().apply {
                moveTo(0f, y)
                lineTo(cx - half, y)
                lineTo(cx, 0.5.dp.toPx())
                lineTo(cx + half, y)
                lineTo(size.width, y)
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Miter),
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = day.date.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale)),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.1.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = day.spent.toWholePeso(),
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onCollapse, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Filled.ExpandLess,
                    contentDescription = stringResource(R.string.outlook_collapse_day),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DetailStat(
                label = stringResource(R.string.outlook_running_total),
                value = outlook.runningTotal(day.date).toWholePeso(),
                muted = muted,
                modifier = Modifier.weight(1f),
            )
            DetailStat(
                label = stringResource(R.string.outlook_projected_month),
                value = outlook.projected?.toWholePeso() ?: stringResource(R.string.outlook_too_early),
                muted = muted,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String, muted: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp, color = muted)
        Text(text = value, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp)
    }
}

// ---- Previews ----

private fun previewOutlook(
    today: LocalDate = LocalDate.of(2026, 11, 26),
    budget: Long = 28_000_00L,
): MonthOutlook {
    val amounts = listOf(
        1 to 450_00L, 2 to 0L, 3 to 820_00L, 5 to 300_00L, 6 to 1_900_00L, 8 to 560_00L,
        9 to 240_00L, 11 to 700_00L, 12 to 380_00L, 14 to 1_050_00L, 15 to 610_00L,
        17 to 290_00L, 18 to 1_150_00L, 20 to 430_00L, 21 to 880_00L, 23 to 520_00L,
        24 to 340_00L, 25 to 650_00L, 26 to 200_00L,
    )
    val totals = amounts
        .filter { it.first <= today.dayOfMonth }
        .map { DailyTotal(today.withDayOfMonth(it.first), it.second) }
    return MonthOutlook.from(today, budget, totals)
}

@Composable
private fun HeatGridPreview(outlook: MonthOutlook, selected: LocalDate? = null) {
    LazyWalletTheme {
        Box(Modifier.padding(16.dp)) {
            BudgetHeatGridCard(
                outlook = outlook,
                selectedDay = selected,
                onDayClick = {},
                onCollapse = {},
                onOpenOutlook = {},
            )
        }
    }
}

@Preview(name = "Collapsed", heightDp = 420)
@Preview(name = "Collapsed dark", heightDp = 420, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollapsedPreview() = HeatGridPreview(previewOutlook())

@Preview(name = "Expanded 18 Nov", heightDp = 560)
@Preview(name = "Expanded 18 Nov dark", heightDp = 560, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExpandedPreview() = HeatGridPreview(previewOutlook(), LocalDate.of(2026, 11, 18))

// 1 Nov 2026 is a Sunday (column 0); 7 Nov is a Saturday (column 6).
@Preview(name = "Expanded Sunday", heightDp = 560)
@Preview(name = "Expanded Sunday dark", heightDp = 560, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExpandedSundayPreview() = HeatGridPreview(previewOutlook(), LocalDate.of(2026, 11, 1))

@Preview(name = "Expanded Saturday", heightDp = 560)
@Preview(name = "Expanded Saturday dark", heightDp = 560, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExpandedSaturdayPreview() = HeatGridPreview(previewOutlook(), LocalDate.of(2026, 11, 7))

@Preview(name = "Overspent", heightDp = 420)
@Preview(name = "Overspent dark", heightDp = 420, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OverspentPreview() = HeatGridPreview(previewOutlook(budget = 8_000_00L))

@Preview(name = "No budgets", heightDp = 420)
@Preview(name = "No budgets dark", heightDp = 420, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NoBudgetsPreview() = HeatGridPreview(previewOutlook(budget = 0L))

@Preview(name = "Day 2", heightDp = 560)
@Preview(name = "Day 2 dark", heightDp = 560, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Day2Preview() {
    val today = LocalDate.of(2026, 11, 2)
    HeatGridPreview(previewOutlook(today = today), today)
}
