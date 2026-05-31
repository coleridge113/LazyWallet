package com.luna.budgetapp.presentation.screen.expensepreset.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.presentation.screen.components.SwipeableTableItem
import com.luna.budgetapp.presentation.screen.utils.getIconForCategory
import com.luna.budgetapp.ui.icons.CoffeeIcon
import com.luna.budgetapp.ui.theme.LazyWalletTheme

@Composable
fun ExpensePresetTable(
    expensePresets: List<ExpensePreset>,
    modifier: Modifier = Modifier,
    onClickIcon: (ExpensePreset) -> Unit,
    onClickItem: (ExpensePreset) -> Unit,
    onDelete: (Long) -> Unit,
    onEdit: (ExpensePreset) -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items (expensePresets) { expensePreset ->
                SwipeableTableItem(
                    onClickEdit = { onEdit(expensePreset) },
                    onClickDelete = { onDelete(expensePreset.id!!) }
                ) {
                    ExpensePresetItem(
                        item = expensePreset,
                        icon = getIconForCategory(expensePreset.category),
                        onClickIcon = { onClickIcon(expensePreset) },
                        onLongClickIcon = { onDelete(expensePreset.id!!) },
                        onClickItem = { onClickItem(expensePreset) }
                    )
                }
            }
        }
    } 
}

@Composable
fun ExpensePresetItem(
    item: ExpensePreset,
    icon: ImageVector,
    onClickIcon: (ExpensePreset) -> Unit,
    onLongClickIcon: (Long) -> Unit,
    onClickItem: (ExpensePreset) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                .combinedClickable(
                    onClick = { onClickIcon(item) },
                    onLongClick = { onLongClickIcon(item.id!!) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null)
        }
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier.weight(3f)
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                .clickable { onClickItem(item) },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "${item.type} - P${item.amount}")
        }
    }
}

@Preview(
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
fun ExpensePresetItemPreview() {
    val item = ExpensePreset(
            id = 1L,
            amount = 4.50,
            category = "Coffee",
            type = "Food & Drink"
        )

    LazyWalletTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            ExpensePresetItem(
                item = item,
                icon = CoffeeIcon,
                onClickIcon = {},
                onLongClickIcon = {},
                onClickItem = {}
            )
        }
    }
}

@Preview(
    uiMode = AndroidUiModes.UI_MODE_NIGHT_NO
)
@Composable
fun ExpensePresetTablePreview() {
    val expensePresets = listOf(
        ExpensePreset(
            id = 1L,
            amount = 4.50,
            category = "BEVERAGE",
            type = "Coffee"
        ),
        ExpensePreset(
            id = 2L,
            amount = 12.00,
            category = "FOOD",
            type = "Lunch"
        ),
        ExpensePreset(
            id = 3L,
            amount = 65.00,
            category = "COMMUTE",
            type = "Angkas"
        ),
    )
    LazyWalletTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            ExpensePresetTable(
                expensePresets = expensePresets,
                onClickItem = {},
                onClickIcon = {},
                onEdit = {},
                onDelete = {}
            )
        }
    }
}

