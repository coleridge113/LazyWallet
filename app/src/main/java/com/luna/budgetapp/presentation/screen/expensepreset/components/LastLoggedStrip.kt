package com.luna.budgetapp.presentation.screen.expensepreset.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.R
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.presentation.model.CategoryOptions
import com.luna.budgetapp.presentation.screen.expensepreset.model.relativeTime
import com.luna.budgetapp.presentation.screen.utils.getIconForCategory
import com.luna.budgetapp.presentation.screen.utils.toPeso
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import java.time.LocalDateTime

internal fun Category.tileColor() =
    CategoryOptions.entries.first { it.name == name }.chartColor

@Composable
fun LastLoggedStrip(
    expense: Expense,
    now: LocalDateTime,
    onOpen: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = expense.category.tileColor()
    val amount = expense.amount.toPeso()
    val label = relativeTime(expense.date, now).label()
    val openDescription = stringResource(
        R.string.cd_last_added, expense.type, amount, label.orEmpty()
    )
    val undoDescription = stringResource(R.string.cd_undo_expense, expense.type, amount)
    val shape = RoundedCornerShape(24.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .height(52.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(shape)
                .clickable(onClick = onOpen)
                .clearAndSetSemantics { contentDescription = openDescription }
                .padding(start = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForCategory(expense.category),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${expense.type} $amount",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (label != null) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "· $label",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ExpandLess,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(
            onClick = onUndo,
            modifier = Modifier
                .padding(end = 6.dp)
                .clearAndSetSemantics {
                    contentDescription = undoDescription
                    role = Role.Button
                    onClick(label = null) { onUndo(); true }
                }
        ) {
            Text(
                text = stringResource(R.string.expense_preset_undo),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF282828)
@Composable
private fun LastLoggedStripPreview() {
    val now = LocalDateTime.of(2026, 11, 26, 12, 0)
    LazyWalletTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            LastLoggedStrip(
                expense = Expense(
                    id = 1, amount = 6000, category = Category.COMMUTE,
                    type = "Jeepney", date = now
                ),
                now = now,
                onOpen = {},
                onUndo = {}
            )
        }
    }
}
