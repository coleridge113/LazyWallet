package com.luna.budgetapp.presentation.screen.budget.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import com.luna.budgetapp.presentation.screen.utils.toCurrency
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import java.time.LocalDate
import java.util.Locale

@Composable
fun BudgetCard(
    modifier: Modifier,
    budget: Budget,
    spent: Long,
    onEdit: (Budget) -> Unit,
    onDelete: (Budget) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
            .height(200.dp),
    ) {
        val remaining = budget.limit - spent

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header sections
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = budget.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .singleClick { expanded = true }
                    )
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

            // Financials section
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .align(Alignment.CenterHorizontally)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Frequency:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = budget.frequency.getFriendlyName(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Amount: ",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Php ${budget.limit.toCurrency()}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Spent: ",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Php ${spent.toCurrency()}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Remaining: ",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Php ${remaining.toCurrency()}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.weight(1f),
                        color =
                            if (remaining > 0)
                                MaterialTheme.colorScheme.secondary
                            else
                                MaterialTheme.colorScheme.error
                    )
                }
                Spacer(
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Categories section
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth(0.80f)
                    .align(Alignment.CenterHorizontally)
            ) {
                items(budget.interactors) { category ->
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
                onDelete = {}
            )
        }
    }
}
