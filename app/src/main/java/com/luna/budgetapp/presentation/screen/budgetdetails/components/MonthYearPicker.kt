package com.luna.budgetapp.presentation.screen.budgetdetails.components

import androidx.compose.ui.res.stringResource
import com.luna.budgetapp.R
import java.time.YearMonth
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.ui.theme.LazyWalletTheme

@Composable
fun MonthYearPickerDialog(
    initialYearMonth: YearMonth = YearMonth.now(),
    onMonthYearSelected: (YearMonth) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYear by remember { mutableIntStateOf(initialYearMonth.year) }
    var selectedMonth by remember { mutableIntStateOf(initialYearMonth.monthValue) }

    val months = remember {
        (1..12).map { monthIndex ->
            Month.of(monthIndex)
                .getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { selectedYear-- }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.cd_previous_year)
                    )
                }
                Text(
                    text = "$selectedYear",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { selectedYear++ }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.cd_next_year)
                    )
                }
            }
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(months) { index, monthName ->
                    val monthNumber = index + 1
                    val isCurrentMonth =
                        monthNumber == initialYearMonth.monthValue &&
                        selectedYear == initialYearMonth.year
                    val isSelected = monthNumber == selectedMonth
                    val shape = MaterialTheme.shapes.small
                    val borderColor =
                        if (isCurrentMonth)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Transparent
                    val textColor =
                        if (isSelected)
                            MaterialTheme.colorScheme.onPrimary
                        else if (isCurrentMonth)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface


                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(48.dp)
                            .border(1.dp, borderColor, shape)
                            .clip(shape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { selectedMonth = monthNumber }
                    ) {
                        Text(
                            text = monthName,
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onMonthYearSelected(YearMonth.of(selectedYear, selectedMonth))
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.btn_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Preview(
    device = Devices.PIXEL_7,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_NO
)
@Composable
fun MonthYearPickerDialogPreview() {
    LazyWalletTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            MonthYearPickerDialog(
                onMonthYearSelected = {},
                onDismiss = {}
            )
        }
    }
}
