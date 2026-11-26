package com.luna.budgetapp.presentation.screen.budget.components

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luna.budgetapp.R
import com.luna.budgetapp.domain.model.DailyTotal
import com.luna.budgetapp.presentation.screen.budget.model.MonthOutlook
import com.luna.budgetapp.presentation.screen.utils.toWholePeso
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@Composable
private fun sheetContainerColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return scheme.primary.copy(alpha = 0.11f).compositeOver(scheme.surface)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlookSheet(outlook: MonthOutlook, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetContainerColor(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        scrimColor = Color.Black.copy(alpha = 0.32f),
        dragHandle = { DragHandle() },
    ) {
        OutlookSheetContent(outlook)
    }
}

@Composable
private fun DragHandle() {
    Box(
        modifier = Modifier
            .padding(top = 16.dp, bottom = 8.dp)
            .size(width = 32.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant)
    )
}

@Composable
private fun OutlookSheetContent(outlook: MonthOutlook) {
    val locale = LocalLocale.current.platformLocale
    val monthName = outlook.today.format(DateTimeFormatter.ofPattern("MMMM", locale))
    val dayFmt = DateTimeFormatter.ofPattern("d MMM", locale)
    val dash = "—"
    val projectedDate = outlook.today.withDayOfMonth(outlook.daysInMonth).format(dayFmt)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.outlook_sheet_title, monthName),
            fontSize = 22.sp,
            lineHeight = 28.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile(
                label = stringResource(R.string.outlook_spent_so_far),
                value = outlook.spentSoFar.toWholePeso(),
                caption = pluralStringResource(
                    R.plurals.outlook_days_count, outlook.daysElapsed, outlook.daysElapsed
                ),
            )
            Tile(
                label = stringResource(R.string.outlook_projected),
                value = outlook.projected?.toWholePeso() ?: dash,
                caption = if (outlook.projected != null)
                    stringResource(R.string.outlook_by_date, projectedDate)
                else stringResource(R.string.outlook_too_early_to_project),
            )
        }

        OutlookChart(outlook)

        val highest = outlook.highest
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat(
                icon = Icons.AutoMirrored.Filled.TrendingFlat,
                value = outlook.avgPerDay.toWholePeso(),
                caption = stringResource(R.string.outlook_avg_day),
            )
            Stat(
                icon = Icons.Filled.ArrowUpward,
                value = highest?.spent?.toWholePeso() ?: dash,
                caption = if (highest != null)
                    stringResource(R.string.outlook_highest_on, highest.date.format(dayFmt))
                else stringResource(R.string.outlook_highest),
            )
            Stat(
                icon = Icons.Filled.Event,
                value = outlook.daysLeft.toString(),
                caption = stringResource(R.string.outlook_days_left),
            )
        }
    }
}

@Composable
private fun RowScope.Tile(label: String, value: String, caption: String) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(12.dp),
    ) {
        Text(label, fontSize = 12.sp, lineHeight = 16.sp, color = muted)
        Text(
            value,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(caption, fontSize = 12.sp, lineHeight = 16.sp, color = muted)
    }
}

@Composable
private fun RowScope.Stat(icon: ImageVector, value: String, caption: String) {
    Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                value,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        Text(
            caption,
            modifier = Modifier.padding(top = 2.dp),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---- Previews ----

private fun previewOutlook(
    today: LocalDate = LocalDate.of(2026, 11, 26),
    budget: Long = 28_000_00L,
    amounts: List<Pair<Int, Long>> = listOf(
        1 to 450_00L, 3 to 820_00L, 5 to 300_00L, 6 to 1_900_00L, 8 to 560_00L,
        9 to 240_00L, 11 to 700_00L, 12 to 380_00L, 14 to 1_050_00L, 15 to 610_00L,
        17 to 290_00L, 18 to 1_150_00L, 20 to 430_00L, 21 to 880_00L, 23 to 520_00L,
        24 to 340_00L, 25 to 650_00L, 26 to 200_00L,
    ),
): MonthOutlook = MonthOutlook.from(
    today, budget,
    amounts.filter { it.first <= today.dayOfMonth }
        .map { DailyTotal(today.withDayOfMonth(it.first), it.second) },
)

@Composable
private fun SheetPreview(outlook: MonthOutlook) {
    LazyWalletTheme {
        Surface(color = sheetContainerColor()) { OutlookSheetContent(outlook) }
    }
}

@Composable
private fun ChartPreview(outlook: MonthOutlook) {
    LazyWalletTheme {
        Surface(color = sheetContainerColor()) {
            OutlookChart(outlook, Modifier.padding(24.dp))
        }
    }
}

private val overspent get() = previewOutlook(budget = 8_000_00L)
private val noBudgets get() = previewOutlook(budget = 0L)
private val day2 get() = previewOutlook(today = LocalDate.of(2026, 11, 2))
private val nothing get() = previewOutlook(amounts = emptyList())

@Preview(name = "Sheet mockup", heightDp = 560)
@Preview(name = "Sheet mockup dark", heightDp = 560, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SheetMockupPreview() = SheetPreview(previewOutlook())

@Preview(name = "Sheet overspent", heightDp = 560)
@Preview(name = "Sheet overspent dark", heightDp = 560, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SheetOverspentPreview() = SheetPreview(overspent)

@Preview(name = "Sheet no budgets", heightDp = 560)
@Preview(name = "Sheet no budgets dark", heightDp = 560, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SheetNoBudgetsPreview() = SheetPreview(noBudgets)

@Preview(name = "Sheet day 2", heightDp = 560)
@Preview(name = "Sheet day 2 dark", heightDp = 560, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SheetDay2Preview() = SheetPreview(day2)

@Preview(name = "Sheet nothing spent", heightDp = 560)
@Preview(name = "Sheet nothing spent dark", heightDp = 560, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SheetNothingPreview() = SheetPreview(nothing)

@Preview(name = "Chart mockup", heightDp = 240)
@Preview(name = "Chart mockup dark", heightDp = 240, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChartMockupPreview() = ChartPreview(previewOutlook())

@Preview(name = "Chart overspent", heightDp = 240)
@Preview(name = "Chart overspent dark", heightDp = 240, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChartOverspentPreview() = ChartPreview(overspent)

@Preview(name = "Chart no budgets", heightDp = 240)
@Preview(name = "Chart no budgets dark", heightDp = 240, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChartNoBudgetsPreview() = ChartPreview(noBudgets)

@Preview(name = "Chart day 2", heightDp = 240)
@Preview(name = "Chart day 2 dark", heightDp = 240, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChartDay2Preview() = ChartPreview(day2)

@Preview(name = "Chart nothing spent", heightDp = 240)
@Preview(name = "Chart nothing spent dark", heightDp = 240, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChartNothingPreview() = ChartPreview(nothing)
