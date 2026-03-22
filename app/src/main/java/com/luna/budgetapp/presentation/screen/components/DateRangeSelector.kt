package com.luna.budgetapp.presentation.screen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import com.luna.budgetapp.domain.model.DateFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSelectorDropdown(
    selected: DateFilter,
    onSelectedChange: (DateFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    var expanded by remember { mutableStateOf(false) }
    val selectableDateFilters = listOf(
        DateFilter.Daily,
        DateFilter.Weekly,
        DateFilter.Monthly,
        DateFilter.Custom()
    )

    Box(modifier = modifier.wrapContentSize()) {
        Surface(
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                expanded = true
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selected.displayName(),
                    style = MaterialTheme.typography.labelLarge
                )

                Spacer(Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = (-11).dp, y = 0.dp),
            modifier = Modifier.width(90.dp)
        ) {
            selectableDateFilters.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Box(
                            contentAlignment = Alignment.CenterEnd,
                            modifier = Modifier.fillMaxWidth()
                                .padding(end = 12.dp)
                        ) {
                            Text(text = option.displayName())
                        }
                    },
                    onClick = {
                        onSelectedChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun DateRangeSelectorDropdownPreview() {
    DateRangeSelectorDropdown(
        selected = DateFilter.Daily,
        onSelectedChange = {}
    )
}

fun DateFilter.displayName() =
    when (this) {
        DateFilter.Daily -> "Today"
        DateFilter.Weekly -> "Week"
        DateFilter.Monthly -> "Month"
        DateFilter.Last7Days -> ""
        is DateFilter.Custom -> "Custom"
    }
