package com.luna.budgetapp.presentation.screen.expenselist.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.presentation.screen.utils.formatToDisplay
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseForm(
    selectedExpense: Expense,
    onDismissRequest: () -> Unit,
    onConfirm: (Long, String, String, LocalDateTime) -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier
) {

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        val options = remember { Category.entries }
        var expanded by remember { mutableStateOf(false) }
        var selectedOption by remember { mutableStateOf(options.first()) }
        val typeState = rememberTextFieldState(selectedExpense.type)
        val amountState = rememberTextFieldState(
            "%.2f".format(selectedExpense.amount / 100.0)
        )

        var showDatePicker by remember { mutableStateOf(false) }
        var selectedDate by remember { mutableStateOf(selectedExpense.date) }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toInstant(ZoneOffset.UTC).toEpochMilli()
        )

        LaunchedEffect(Unit) {
            selectedOption = options.firstOrNull { option ->
                option.getDisplayName().equals(selectedExpense.category, ignoreCase = true)
            } ?: options.first()
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = Instant.ofEpochMilli(it)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false,
                    title = null,
                    headline = null
                )
            }
        }

        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Expense",
                    style = MaterialTheme.typography.headlineSmall
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {}
                ) {
                    TextField(
                        value = selectedOption.getDisplayName(),
                        onValueChange = {},
                        readOnly = true,
                        label = null,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null
                            )
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false},
                        modifier = Modifier.heightIn(max = 226.dp)
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.getDisplayName()) },
                                onClick = {
                                    selectedOption = option
                                    expanded = false
                                    typeState.clearText()
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                OutlinedTextField(
                    state = typeState,
                    label = { Text("Type") },
                    placeholder = {
                        Text(selectedExpense.type)
                    },
                    modifier = Modifier.onFocusChanged {
                        if (it.isFocused) typeState.clearText()
                    }
                )

                OutlinedTextField(
                    state = amountState,
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    placeholder = {
                        Text("%.2f".format(selectedExpense.amount / 100.0))
                    },
                    inputTransformation = InputTransformation {
                        val text = asCharSequence().toString()

                        val validExpression =
                            Regex("^\\s*-?\\d*(\\.\\d*)?(?:[+-]\\d*(\\.\\d*)?)*\\s*$")

                        if (!validExpression.matches(text)) {
                            revertAllChanges()
                        }
                    },
                    modifier = Modifier.onFocusChanged {
                        if (it.isFocused) amountState.clearText()
                    }
                )

                OutlinedTextField(
                    value = selectedDate.formatToDisplay(),
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Select Date",
                            modifier = Modifier.clickable { showDatePicker = true }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            if (it.isFocused) {
                                showDatePicker = true
                            }
                        }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val type =
                            typeState.text.ifBlank {
                                selectedExpense.type
                            }.toString()
                        val amount =
                            amountState.text.ifBlank {
                                "%.2f".format(selectedExpense.amount / 100.0)
                            }.toString()
                        onConfirm(
                            selectedExpense.id!!,
                            type,
                            amount,
                            selectedDate
                        )
                    },
                        enabled = !isSaving
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_7
)
@Composable
fun ExpensePresetDialogPreview() {
    val dummyExpense = Expense(
        id = 1L,
        amount = 14000,
        category = "Food",
        type = "Lunch",
    )

    LazyWalletTheme {
        Spacer(modifier = Modifier.height(50.dp))
        Box(modifier = Modifier.fillMaxSize()){
            ExpenseForm(
                selectedExpense = dummyExpense,
                onDismissRequest = {},
                onConfirm = { _, _, _, _ -> },
                isSaving = false,
            )
        }
    }
}
