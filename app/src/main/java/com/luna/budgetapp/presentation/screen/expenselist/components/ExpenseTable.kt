package com.luna.budgetapp.presentation.screen.expenselist.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.presentation.screen.components.SwipeableTableItem
import com.luna.budgetapp.presentation.screen.utils.formatToDisplay
import com.luna.budgetapp.presentation.screen.utils.getIconForCategory
import com.luna.budgetapp.presentation.screen.utils.toCurrency
import com.luna.budgetapp.ui.theme.LazyWalletTheme

@Composable
fun ExpenseTable(
    modifier: Modifier = Modifier,
    expenses: LazyPagingItems<Expense>,
    onEdit: (Expense) -> Unit,
    onDelete: (Expense) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(expenses.itemCount) { index ->
            expenses[index]?.let { expense ->
                SwipeableTableItem(
                    onClickEdit = { onEdit(expense) },
                    onClickDelete = { onDelete(expense) }
                ) {
                    ExpenseItem(
                        item = expense,
                        icon = getIconForCategory(expense.category)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseTableContent(
    modifier: Modifier = Modifier,
    expenses: List<Expense>,
    onEdit: (Expense) -> Unit,
    onDelete: (Expense) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(expenses) { expense ->
            SwipeableTableItem(
                onClickEdit = { onEdit(expense) },
                onClickDelete = { onDelete(expense) }
            ) {
                ExpenseItem(
                    item = expense,
                    icon = getIconForCategory(expense.category)
                )
            }
        }
    }
}

@Composable
fun ExpenseItem(
    item: Expense,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
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
                text = item.amount.toCurrency(),
                modifier = Modifier
                    .widthIn(min = 64.dp)
                    .wrapContentWidth(Alignment.End)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseTablePreview() {
    val expenses = listOf(
        Expense(id = 1, amount = 100.0, category = "FOOD", type = "Lunch"),
        Expense(id = 2, amount = 50.0, category = "COMMUTE", type = "Taxi"),
        Expense(id = 3, amount = 200.0, category = "BILLS", type = "Electricity")
    )

    LazyWalletTheme {
        Surface {
            ExpenseTableContent(
                modifier = Modifier
                    .padding(16.dp),
                expenses = expenses,
                onEdit = {},
                onDelete = {}
            )
        }
    }
}
