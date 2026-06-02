package com.luna.budgetapp.presentation.screen.expensepreset.components

import android.graphics.drawable.shapes.Shape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
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
    onEdit: (ExpensePreset) -> Unit,
    onDelete: (Long) -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2)
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
    onClickItem: (ExpensePreset) -> Unit
) {
    val size = 72.dp
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                .clickable { onClickIcon(item) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Expense Preset Icon",
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .weight(1.5f)
                .height(size)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                .clickable { onClickItem(item) },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.type,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${item.amount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
            amount = 140.00,
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
            amount = 3500.00,
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

