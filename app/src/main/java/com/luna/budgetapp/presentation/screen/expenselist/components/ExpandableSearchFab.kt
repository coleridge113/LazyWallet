package com.luna.budgetapp.presentation.screen.expenselist.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.R
import com.luna.budgetapp.ui.theme.LazyWalletTheme

private val FabSize = 56.dp

@Composable
fun ExpandableSearchFab(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPositioned: (LayoutCoordinates) -> Unit = {}
) {
    // Starts collapsed; an active query is signalled by the badge on the FAB.
    var expanded by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Local field value so the cursor can be placed at the end on re-expand.
    // The field owns its text while composed; it is only seeded from `query`. Re-syncing from
    // `query` on every recomposition would race the ViewModel round-trip and drop keystrokes.
    var fieldValue by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }

    // Previous focus state of the text field, so the initial unfocused report
    // right after expanding is not mistaken for a blur.
    var wasFocused by remember { mutableStateOf(false) }

    fun close() {
        fieldValue = TextFieldValue("")
        onQueryChange("")
        wasFocused = false
        expanded = false
        focusManager.clearFocus()
    }

    BackHandler(enabled = expanded) { close() }

    // Only focus (and raise the keyboard) when the user taps the FAB,
    // not when the field is restored expanded after returning to the tab.
    var focusOnExpand by remember { mutableStateOf(false) }
    LaunchedEffect(focusOnExpand) {
        if (focusOnExpand) {
            focusRequester.requestFocus()
            focusOnExpand = false
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        val width by animateDpAsState(
            targetValue = if (expanded) maxWidth else FabSize,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
            label = "searchFabWidth"
        )

        Surface(
            modifier = Modifier
                .width(width)
                .height(FabSize)
                .onGloballyPositioned(onPositioned),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            onClick = {
                if (!expanded) {
                    wasFocused = false
                    fieldValue = fieldValue.copy(selection = TextRange(fieldValue.text.length))
                    expanded = true
                    focusOnExpand = true
                }
            },
            enabled = !expanded
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(FabSize),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_expenses),
                        modifier = Modifier.size(24.dp)
                    )
                }

                AnimatedVisibility(
                    visible = expanded,
                    modifier = Modifier.weight(1f),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    BasicTextField(
                        value = fieldValue,
                        onValueChange = {
                            fieldValue = it
                            if (it.text != query) onQueryChange(it.text)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                // Collapse on focus loss but keep the query.
                                if (wasFocused && !it.isFocused) expanded = false
                                wasFocused = it.isFocused
                            },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSecondary
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSecondary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.search_expenses_placeholder),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = { close() },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.search_clear)
                        )
                    }
                }
            }
        }

        if (!expanded && query.isNotEmpty()) {
            Badge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(14.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpandableSearchFabCollapsedPreview() {
    LazyWalletTheme {
        Box(Modifier.padding(16.dp)) {
            ExpandableSearchFab(query = "", onQueryChange = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpandableSearchFabCollapsedActiveQueryPreview() {
    LazyWalletTheme {
        Box(Modifier.padding(16.dp)) {
            ExpandableSearchFab(query = "food", onQueryChange = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpandableSearchFabExpandedPreview() {
    LazyWalletTheme {
        Box(Modifier.padding(16.dp)) {
            ExpandableSearchFab(query = "food", onQueryChange = {})
        }
    }
}
