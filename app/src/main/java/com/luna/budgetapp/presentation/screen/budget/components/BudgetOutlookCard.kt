package com.luna.budgetapp.presentation.screen.budget.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.presentation.screen.budget.model.OutlookDetails
import com.luna.budgetapp.presentation.screen.utils.toCurrency
import com.luna.budgetapp.ui.theme.LazyWalletTheme

@Composable
fun BudgetOutlookCard(
    modifier: Modifier = Modifier,
    details: OutlookDetails
) {
    Surface(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Total Remaining",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = details.remaining.toCurrency(),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(
                modifier = Modifier.height(24.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = details.income.toCurrency(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Projected",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = details.projectedSpend.toCurrency(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Actual",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = details.actualSpend.toCurrency(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun BudgetOutlookCardPreview() {
    val details = OutlookDetails(
        income = 3_000_000L,
        projectedSpend = 2_800_000L,
        actualSpend = 1_230_000L
    )
    LazyWalletTheme {
        BudgetOutlookCard(
            modifier = Modifier,
            details = details
        )
    }
}
