package com.luna.budgetapp.presentation.screen.expensepreset.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.R
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.presentation.screen.expensepreset.model.relativeTime
import com.luna.budgetapp.presentation.screen.utils.getIconForCategory
import com.luna.budgetapp.presentation.screen.utils.toPeso
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyAddedSheet(
    recent: List<Expense>,
    total: Long,
    now: LocalDateTime,
    onRemove: (Expense) -> Unit,
    onSeeAll: () -> Unit,
    onDismiss: () -> Unit,
    snackbarHost: @Composable () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)
            .compositeOver(MaterialTheme.colorScheme.background)
    ) {
        RecentlyAddedContent(recent, total, now, onRemove, onSeeAll, snackbarHost)
    }
}

@Composable
private fun RecentlyAddedContent(
    recent: List<Expense>,
    total: Long,
    now: LocalDateTime,
    onRemove: (Expense) -> Unit,
    onSeeAll: () -> Unit,
    snackbarHost: @Composable () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = stringResource(R.string.expense_preset_recently_added),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.expense_preset_today_total, total.toPeso()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(top = 12.dp, start = 12.dp, end = 12.dp)
        ) {
            itemsIndexed(recent, key = { index, e -> e.id ?: "idx$index" }) { index, expense ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                RecentRow(
                    expense = expense,
                    now = now,
                    isFirst = index == 0,
                    onRemove = { onRemove(expense) }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onSeeAll) {
                Text(stringResource(R.string.expense_preset_see_all_expenses))
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        snackbarHost()
    }
}

@Composable
private fun RecentRow(
    expense: Expense,
    now: LocalDateTime,
    isFirst: Boolean,
    onRemove: () -> Unit
) {
    val color = expense.category.tileColor()
    val amount = expense.amount.toPeso()
    val time = expense.date.format(TimeFormatter)
    val label = if (isFirst) relativeTime(expense.date, now).label() else null
    val subtitle = if (label != null) "$time · $label" else time
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (isFirst) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                else Modifier
            )
            .padding(start = 12.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getIconForCategory(expense.category),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = expense.type,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(text = amount, style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = stringResource(R.string.cd_remove_expense, expense.type, amount),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF282828)
@Composable
private fun RecentlyAddedSheetPreview() {
    val now = LocalDateTime.of(2026, 11, 26, 18, 6)
    LazyWalletTheme(darkTheme = true) {
        RecentlyAddedContent(
            recent = listOf(
                Expense(id = 3, amount = 6000, category = Category.COMMUTE, type = "Jeepney",
                    date = now.withHour(18).withMinute(5)),
                Expense(id = 2, amount = 25000, category = Category.FOOD, type = "Lunch",
                    date = now.withHour(12).withMinute(30)),
                Expense(id = 1, amount = 14000, category = Category.BEVERAGE, type = "Coffee",
                    date = now.withHour(8).withMinute(15))
            ),
            total = 45000,
            now = now,
            onRemove = {},
            onSeeAll = {}
        )
    }
}
