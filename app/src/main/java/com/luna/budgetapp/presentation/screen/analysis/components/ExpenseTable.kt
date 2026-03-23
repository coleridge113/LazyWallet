package com.luna.budgetapp.presentation.screen.analysis.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.presentation.screen.utils.formatToDisplay
import com.luna.budgetapp.presentation.screen.utils.getIconForCategory

@Composable
fun ExpenseTable(
    modifier: Modifier = Modifier,
    expenses: List<Expense>
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(expenses) { expense ->
            ExpenseItem(
                item = expense,
                icon = getIconForCategory(expense.category),
                onClick = {},
                onLongClick = {}
            )
        }
    }
}

@Composable
fun ExpenseItem(
    item: Expense,
    icon: ImageVector,
    onClick: (Expense) -> Unit,
    onLongClick: (Expense) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .combinedClickable(
                onClick = { onClick(item) },
                onLongClick = { onLongClick(item) }
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            Icon(imageVector = icon, contentDescription = null)
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = item.type,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = item.date.formatToDisplay(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "₱",
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = "%,.2f".format(item.amount),
                modifier = Modifier
                    .widthIn(min = 64.dp)
                    .wrapContentWidth(Alignment.End)
            )
        }
    }
}

@Preview
@Composable
fun ExpenseItemPreview() {
    val expense = Expense(
        category = "BEVERAGE",
        type = "Coffee",
        amount = 100.0
    )
    ExpenseItem(
        item = expense,
        icon = Icons.Default.AttachMoney,
        onClick = {},
        onLongClick = {}
    )
}

