package com.luna.budgetapp.presentation.screen.expensepreset.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.R
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.presentation.screen.utils.getIconForCategory
import com.luna.budgetapp.presentation.screen.utils.toCurrency
import com.luna.budgetapp.ui.theme.LazyWalletTheme

@Composable
fun PresetTileGrid(
    presets: List<ExpensePreset>,
    onLog: (ExpensePreset) -> Unit,
    onCustom: (ExpensePreset) -> Unit,
    onEdit: (ExpensePreset) -> Unit,
    onDelete: (Long) -> Unit,
    onNewPreset: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(presets, key = { it.id ?: it.hashCode() }) { preset ->
            PresetTile(
                preset = preset,
                onLog = { onLog(preset) },
                onCustom = { onCustom(preset) },
                onEdit = { onEdit(preset) },
                onDelete = { preset.id?.let(onDelete) }
            )
        }
        item(key = "new_preset") {
            NewPresetTile(onClick = onNewPreset)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PresetTile(
    preset: ExpensePreset,
    onLog: () -> Unit,
    onCustom: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = preset.category.tileColor()
    val amount = preset.amount.toCurrency()
    var menuOpen by remember { mutableStateOf(false) }
    val logDescription = stringResource(R.string.cd_log_preset, preset.type, amount)
    val customDescription = stringResource(R.string.cd_custom_preset, preset.type)
    val menuLabel = stringResource(R.string.cd_edit_or_delete)
    val shape = RoundedCornerShape(20.dp)

    Box(modifier = modifier.aspectRatio(1f)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(color.copy(alpha = 0.18f))
                .combinedClickable(
                    onClick = onLog,
                    onLongClick = { menuOpen = true },
                    onLongClickLabel = menuLabel
                )
                .clearAndSetSemantics { contentDescription = logDescription }
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = preset.type,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.35f))
                .clickable(onClick = onCustom)
                .clearAndSetSemantics {
                    contentDescription = customDescription
                    role = Role.Button
                    onClick(label = null) { onCustom(); true }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getIconForCategory(preset.category),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.btn_edit)) },
                onClick = { menuOpen = false; onEdit() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.btn_delete)) },
                onClick = { menuOpen = false; onDelete() }
            )
        }
    }
}

@Composable
fun NewPresetTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .clickable(onClick = onClick)
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = color,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(20.dp.toPx() - stroke / 2),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                    )
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = color)
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.expense_preset_new_preset),
            style = MaterialTheme.typography.labelLarge,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF282828, widthDp = 412)
@Composable
private fun PresetTileGridPreview() {
    LazyWalletTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PresetTileGrid(
                presets = listOf(
                    ExpensePreset(1, 14000, Category.BEVERAGE, "Coffee"),
                    ExpensePreset(2, 25000, Category.FOOD, "Lunch"),
                    ExpensePreset(3, 6000, Category.COMMUTE, "Jeepney"),
                    ExpensePreset(4, 150000, Category.GROCERY, "Groceries"),
                    ExpensePreset(5, 280000, Category.BILLS, "Electricity"),
                    ExpensePreset(6, 30000, Category.FITNESS, "Gym"),
                    ExpensePreset(7, 120000, Category.DATE, "Date night")
                ),
                onLog = {}, onCustom = {}, onEdit = {}, onDelete = {}, onNewPreset = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF282828)
@Composable
private fun NewPresetTilePreview() {
    LazyWalletTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            NewPresetTile(onClick = {}, modifier = Modifier.size(118.dp))
        }
    }
}
