package com.luna.budgetapp.presentation.screen.expensepreset.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.luna.budgetapp.presentation.screen.utils.toCurrency

@Composable
fun ExpenseAmountDisplay(
    modifier: Modifier = Modifier,
    totalAmount: Double,
) {
    val displayAmount = if (totalAmount == 0.0) "-" else totalAmount.toCurrency()
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "PHP",
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier.weight(3f),
                text = displayAmount,
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(
    showBackground = true,
    heightDp = 200
)
@Composable
fun ExpenseAmountDisplayPreview() {
    ExpenseAmountDisplay(
        modifier = Modifier.fillMaxSize(),
        totalAmount = 1234.0,
    )
}

