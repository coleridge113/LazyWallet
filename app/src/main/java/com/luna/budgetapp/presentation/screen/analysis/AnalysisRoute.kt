package com.luna.budgetapp.presentation.screen.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luna.budgetapp.R
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.Granularity
import com.luna.budgetapp.domain.model.PeriodChange
import com.luna.budgetapp.domain.model.PeriodTotal
import com.luna.budgetapp.presentation.screen.analysis.components.SpendingHistoryChart
import com.luna.budgetapp.presentation.screen.analysis.components.SummaryLabel
import com.luna.budgetapp.presentation.screen.analysis.components.summaryLabel
import com.luna.budgetapp.presentation.screen.components.CategoryProfileSelectorDropdown
import com.luna.budgetapp.presentation.screen.components.ErrorContent
import com.luna.budgetapp.presentation.screen.components.ExpenseTable
import com.luna.budgetapp.presentation.screen.components.LoadingContent
import com.luna.budgetapp.presentation.screen.utils.toPeso
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisRoute(
    viewModel: AnalysisViewModel
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onEvent = viewModel::onEvent

    when (val state = uiState) {
        is UiState.Loading -> LoadingContent()
        is UiState.Error -> ErrorContent(
            onRetry = { onEvent(Event.Retry) }
        )
        is UiState.Success -> {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    modifier = Modifier,
                    title = {},
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    actions = {
                        CategoryProfileSelectorDropdown(
                            selectedProfile = state.categoryProfileState.activeProfile,
                            profileList = state.categoryProfileState.profileList,
                            onSelectedChange = {
                                onEvent(Event.SelectCategoryProfile(it))
                            }
                        )
                    }
                )

                AnalysisContent(
                    modifier = Modifier,
                    uiState = state,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@Composable
fun AnalysisContent(
    modifier: Modifier,
    uiState: UiState.Success,
    onEvent: (Event) -> Unit,
) {
    val history = uiState.historyState
    val selected = history.selected

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GranularityPicker(
            selected = history.granularity,
            onSelect = { onEvent(Event.SelectGranularity(it)) }
        )

        if (selected != null && history.today != null) {
            SpendingSummary(
                period = selected,
                granularity = history.granularity,
                today = history.today,
                change = history.change
            )
        }

        SpendingHistoryChart(
            periods = history.periods,
            granularity = history.granularity,
            selectedStart = selected?.start,
            onSelect = { onEvent(Event.SelectPeriod(it)) },
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.expensesState.filteredExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.analysis_no_spending),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            ExpenseTable(
                modifier = Modifier.fillMaxWidth(),
                expenses = uiState.expensesState.filteredExpenses
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GranularityPicker(
    selected: Granularity,
    onSelect: (Granularity) -> Unit,
) {
    val options = listOf(
        Granularity.DAY to R.string.analysis_day,
        Granularity.WEEK to R.string.analysis_week,
        Granularity.MONTH to R.string.analysis_month,
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (granularity, label) ->
            SegmentedButton(
                selected = granularity == selected,
                onClick = { onSelect(granularity) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size)
            ) {
                Text(stringResource(label))
            }
        }
    }
}

@Composable
private fun SpendingSummary(
    period: PeriodTotal,
    granularity: Granularity,
    today: LocalDate,
    change: PeriodChange?,
) {
    val locale = LocalConfiguration.current.locales[0]
    val label = when (val l = summaryLabel(period, granularity, today, locale)) {
        SummaryLabel.Today -> stringResource(R.string.analysis_today)
        SummaryLabel.ThisWeek -> stringResource(R.string.analysis_this_week)
        SummaryLabel.ThisMonth -> stringResource(R.string.analysis_this_month)
        is SummaryLabel.Text -> l.value
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = period.total.toPeso(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        if (change != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = changeText(change, stringResource(R.string.analysis_vs_previous)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** "▲ ₱120.00 (+15%) vs previous"; the percentage is omitted when there is nothing to compare against. */
private fun changeText(change: PeriodChange, suffix: String): String {
    val arrow = if (change.amount >= 0) "▲" else "▼"
    val percent = change.percent?.let {
        val rounded = it.roundToInt()
        val sign = if (rounded >= 0) "+" else "-"
        " ($sign${abs(rounded)}%)"
    }.orEmpty()
    return "$arrow ${abs(change.amount).toPeso()}$percent $suffix"
}

private fun previewState(): UiState.Success {
    val today = LocalDate.of(2026, 11, 26)
    val periods = (6 downTo 0).map { i ->
        val d = today.minusDays(i.toLong())
        PeriodTotal(d, d, if (i % 3 == 0) 0L else 15000L * (7 - i))
    }
    val expenses = listOf(
        Expense(1L, "Coffee", 9000, Category.FOOD, "Expense", today.atTime(9, 0)),
        Expense(2L, "Lunch", 15000, Category.FOOD, "Expense", today.atTime(12, 0)),
    )
    return UiState.Success(
        historyState = HistoryState(
            periods = periods,
            selected = periods.last(),
            change = PeriodChange(12000, 15.0),
            today = today
        ),
        expensesState = ExpensesState(filteredExpenses = expenses)
    )
}

@Preview(
    showSystemUi = true,
    device = Devices.PIXEL_7,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_NO
)
@Composable
private fun AnalysisContentPreviewLight() {
    LazyWalletTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AnalysisContent(modifier = Modifier, uiState = previewState(), onEvent = {})
        }
    }
}

@Preview(
    showSystemUi = true,
    device = Devices.PIXEL_7,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
private fun AnalysisContentPreviewDark() {
    LazyWalletTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AnalysisContent(modifier = Modifier, uiState = previewState(), onEvent = {})
        }
    }
}
