package com.luna.budgetapp.presentation.screen.expenselist.components

import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.presentation.model.CategoryOptions
import com.luna.budgetapp.presentation.screen.utils.formatToDisplay
import com.luna.budgetapp.presentation.screen.utils.getIconForCategory
import com.luna.budgetapp.presentation.screen.utils.toCurrency
import kotlin.math.roundToInt

@Composable
fun ExpenseTable(
    modifier: Modifier = Modifier,
    expenses: LazyPagingItems<Expense>,
    onClick: (Expense) -> Unit,
    onLongClick: (Expense) -> Unit,
    onDelete: (Expense) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(expenses.itemCount) { index ->
            expenses[index]?.let { expense ->
                SwipeableExpenseItem(
                    item = expense,
                    icon = getIconForCategory(expense.category),
                    onClick = {},
                    onLongClick = {},
                    onDelete = { onDelete(expense) }, 
                )
            }
        }
    }
}

enum class DragValue { Closed, Open }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableExpenseItem(
    item: Expense,
    icon: ImageVector,
    onDelete: (Expense) -> Unit,
    onClick: (Expense) -> Unit,
    onLongClick: (Expense) -> Unit
) {

    val density = LocalDensity.current
    val actionWidth = 80.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }

    val anchors = remember(actionWidthPx) {
        DraggableAnchors {
            DragValue.Closed at 0f
            DragValue.Open at -actionWidthPx
        }
    }

    val state = remember {
        AnchoredDraggableState(
            initialValue = DragValue.Closed,
            anchors = anchors
        )
    }

    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = state,
        positionalThreshold = { distance -> distance * 0.5f },
        animationSpec = spring()
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RectangleShape)
    ) {

        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.End
        ) {

            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.error)
                    .clickable { onDelete(item) },

                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()

                .offset {
                    IntOffset(
                        x = state.offset.roundToInt(),
                        y = 0
                    )
                }

                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Horizontal,
                    flingBehavior = flingBehavior
                )

                .background(MaterialTheme.colorScheme.surface)
        ) {

            ExpenseItem(
                item = item,
                icon = icon,
                onClick = onClick,
                onLongClick = onLongClick
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
        modifier = Modifier
            .fillMaxWidth()
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
                text = item.amount.toCurrency(),
                modifier = Modifier
                    .widthIn(min = 64.dp)
                    .wrapContentWidth(Alignment.End)
            )
        }
    }
}

@Preview
@Composable
fun SwipeableExpenseItemPreview() {
    val expense = Expense(
        category = "Beverage",
        type = "Coffee",
        amount = 140.0,
    )

    SwipeableExpenseItem(
        item = expense,
        icon = CategoryOptions.BEVERAGE.icon,
        onClick = {},
        onLongClick = {},
        onDelete = {},
    )
}
