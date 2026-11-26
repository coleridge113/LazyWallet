package com.luna.budgetapp.presentation.screen.budget.components

import com.luna.budgetapp.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.IconButton
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.Budget
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.presentation.screen.utils.getIconForCategory
import com.luna.budgetapp.presentation.screen.utils.singleClick
import com.luna.budgetapp.presentation.screen.utils.toPeso
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import java.time.LocalDate
import java.util.Locale

@Composable
private fun BudgetCardMenu(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onClick: (Options) -> Unit
) {
    val options = Options.entries

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.getDisplayName()) },
                onClick = {
                    onClick(option)
                }
            )
        }
    }
}

private enum class Options {
    EDIT,
    DELETE;

    fun getDisplayName(): String {
        return name.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetCard(
    modifier: Modifier,
    budget: Budget,
    spent: Long,
    onEdit: (Budget) -> Unit,
    onDelete: (Budget) -> Unit,
    onClick: (Budget) -> Unit
) {

    val remaining = budget.limit - spent

    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
            .heightIn(min = 160.dp)
            .singleClick { onClick(budget) },
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header sections
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = budget.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "(${budget.frequency})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.cd_more_options),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    BudgetCardMenu(
                        modifier = Modifier
                            .width(90.dp),
                        expanded = expanded,
                        onDismiss = { expanded = false },
                        onClick = { option ->
                            when (option) {
                                Options.EDIT -> onEdit(budget)
                                Options.DELETE -> onDelete(budget)
                            }
                            expanded = false
                        }
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val suffix = stringResource(
                    if (remaining >= 0) R.string.budget_suffix_left else R.string.budget_suffix_overspent
                )
                Text(
                    text = "${remaining.toPeso()} $suffix",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                BudgetProgressBar(
                    spent = spent,
                    limit = budget.limit,
                    modifier = Modifier.height(16.dp)
                        .fillMaxWidth()
                )
            }
            Spacer(
                modifier = Modifier.height(24.dp),
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                budget.interactors.forEach { category ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getIconForCategory(category),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = category.getDisplayName(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun budgetProgressColor(progress: Float): Color = when {
    progress < 0.75f -> MaterialTheme.colorScheme.primary
    progress < 0.90f -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}

@Composable
fun BudgetProgressBar(
    spent: Long,
    limit: Long,
    modifier: Modifier = Modifier
) {
    val rawProgress = if (limit > 0f) spent.toFloat() / limit.toFloat() else 0f
    val progress = rawProgress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "progress_animation"
    )

    val progressColor = budgetProgressColor(progress)

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier,
        color = progressColor,
        trackColor = progressColor.copy(alpha = 0.2f),
        strokeCap = StrokeCap.Round
    )
}

@Preview(
    device = Devices.PIXEL_7,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_NO
)
@Composable
fun BudgetCardPreview() {
    val budget = Budget(
        limit = 50000,
        name = "Daily Budget",
        frequency = DateFilter.Daily,
        interactors = Category.entries,
        startDate = LocalDate.now()
    )

    LazyWalletTheme {
        Surface(
            modifier = Modifier
        ) {
            BudgetCard(
                modifier = Modifier,
                budget = budget,
                spent = 43200,
                onEdit = {},
                onDelete = {},
                onClick = {}
            )
        }
    }
}
