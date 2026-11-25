package com.luna.budgetapp.presentation.screen.components

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import com.luna.budgetapp.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.IconButton
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.domain.model.TableItem
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeableTableItem(
    onClickEdit: () -> Unit,
    onClickDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val actionWidth = 96.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val iconSize = 32.dp

    val anchors = remember (actionWidthPx) {
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
    val scope = rememberCoroutineScope()
    val editLabel = stringResource(R.string.btn_edit)
    val deleteLabel = stringResource(R.string.btn_delete)

    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = state,
        positionalThreshold = { distance -> distance * 0.5f },
        animationSpec = spring()
    )

    Box {
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .matchParentSize(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(actionWidth / 2)
                    .fillMaxHeight()
            ) {
                IconButton(
                    onClick = {
                        onClickEdit()
                        scope.launch {
                            state.animateTo(DragValue.Closed)
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(iconSize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.btn_edit),
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(actionWidth / 2)
                    .fillMaxHeight()
            ) {
                IconButton(
                    onClick = {
                        onClickDelete()
                        scope.launch {
                            state.animateTo(DragValue.Closed)
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(iconSize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.btn_delete),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
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
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(editLabel) {
                            onClickEdit()
                            true
                        },
                        CustomAccessibilityAction(deleteLabel) {
                            onClickDelete()
                            true
                        }
                    )
                }
        ) {
            content()
        }
    }
}

enum class DragValue { Closed, Open }
