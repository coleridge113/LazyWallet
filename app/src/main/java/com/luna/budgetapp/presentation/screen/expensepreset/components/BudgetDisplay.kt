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
import com.luna.budgetapp.domain.model.Budget

//@Composable
//fun BudgetAmountDisplay(
//    modifier: Modifier = Modifier,
//    budget: Budget,
//) {
//    val displayAmount = budget.remaining
//    Box(
//        modifier = modifier,
//        contentAlignment = Alignment.Center
//    ) {
//        Row(
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text(
//                modifier = Modifier.weight(1f),
//                text = "PHP",
//                textAlign = TextAlign.Center
//            )
//            Text(
//                modifier = Modifier.weight(3f),
//                text = displayAmount.toString(),
//                style = MaterialTheme.typography.displayMedium,
//                textAlign = TextAlign.Center
//            )
//        }
//    }
//}
//
//@Preview(
//    showBackground = true,
//    heightDp = 200
//)
//@Composable
//fun BudgetAmountDisplayPreview() {
//
//    BudgetAmountDisplay(
//        modifier = Modifier.fillMaxSize(),
//        budget = budget,
//    )
//}
//
