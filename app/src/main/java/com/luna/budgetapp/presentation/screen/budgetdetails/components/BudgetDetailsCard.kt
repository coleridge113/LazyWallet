package com.luna.budgetapp.presentation.screen.budgetdetails.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.domain.model.Budget
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.presentation.screen.budget.model.OutlookDetails
import com.luna.budgetapp.presentation.screen.utils.getIconForCategory
import com.luna.budgetapp.presentation.screen.utils.toCurrency
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import java.time.LocalDate

@Composable
fun BudgetDetailsCard(
    modifier: Modifier = Modifier,
    budget: Budget,
    spent: Long
) {
    val remaining = budget.limit - spent

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val status = if (remaining >= 0) "Remaining" else "Exceeded"
            Text(
                text = status,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = remaining.toCurrency(),
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(Modifier.height(24.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Spent",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = spent.toCurrency()
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Limit",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = budget.limit.toCurrency()
                )
            }
        }
        Spacer(Modifier.padding(vertical = 12.dp))
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

@Preview(
    device = Devices.PIXEL_7,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_NO
)
@Composable
fun BudgetDetailsCardPreview() {
    val budget = Budget(
        limit = 50000,
        name = "Daily Budget",
        frequency = DateFilter.Daily,
        interactors = Category.entries,
        startDate = LocalDate.now()
    )

    LazyWalletTheme {
        Surface(
            color = MaterialTheme.colorScheme.surface
        ) {
            BudgetDetailsCard(
                budget = budget,
                spent = 43200
            )
        }
    }
}