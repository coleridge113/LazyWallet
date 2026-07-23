package com.luna.budgetapp.presentation.screen.expensepreset.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.common.Constants.EMPTY
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.presentation.screen.utils.formatToDisplay
import com.luna.budgetapp.presentation.screen.utils.toCurrency
import com.luna.budgetapp.ui.theme.LazyWalletTheme
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

typealias FormInputs = (Long?, Category, String, String, LocalDateTime) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensePresetDialog(
    selectedPreset: ExpensePreset?,
    onDismissRequest: () -> Unit,
    onConfirm: FormInputs,
    isSaving: Boolean,
    action: ExpenseFormAction,
    modifier: Modifier = Modifier
) {

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        val options = remember { Category.entries }
        var expanded by remember { mutableStateOf(false) }
        var selectedOption by remember { mutableStateOf(options.first()) }
        val typeState = rememberTextFieldState(selectedPreset?.type ?: EMPTY)
        val amountState = rememberTextFieldState(
            selectedPreset?.amount?.toCurrency() ?: EMPTY
        )
        val isLocked = selectedPreset != null

        var showDatePicker by remember { mutableStateOf(false) }
        var selectedDate by remember {
            mutableStateOf(selectedPreset?.createdAt ?: LocalDateTime.now())
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toInstant(ZoneOffset.UTC).toEpochMilli()
        )

        LaunchedEffect(Unit) {
            selectedPreset?.let {
                selectedOption = options.firstOrNull { option ->
                    option.getDisplayName().equals(it.category, ignoreCase = true)
                } ?: options.first()
            }
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
                    text = when (action) {
                        ExpenseFormAction.EDIT -> "Edit Expense Preset"
                        ExpenseFormAction.ADD -> "Add Expense Preset"
                        else -> "Add Custom Expense"
                    },
                    style = MaterialTheme.typography.headlineSmall
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!isLocked) expanded = !expanded }
                ) {
                    TextField(
                        value = selectedOption.getDisplayName(),
                        onValueChange = {},
                        readOnly = true,
                        label = if (!isLocked) {{ Text("Select category") }} else null,
                        trailingIcon = {
                            if (!isLocked) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Dropdown Arrow"
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock"
                                )
                            }
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
                        Text(selectedPreset?.type ?: EMPTY)
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
                        Text(
                            selectedPreset?.amount?.toCurrency() ?: EMPTY
                        )
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
                    Button(
                        onClick = {
                            val type =
                                typeState.text.ifBlank {
                                    selectedPreset?.type
                                }.toString()
                            val amount =
                                amountState.text.ifBlank {
                                    selectedPreset?.amount
                                }.toString()
                            onConfirm(selectedPreset?.id, selectedOption, type, amount, selectedDate)
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

enum class ExpenseFormAction {
    EDIT,
    ADD,
    CUSTOM
}

@Preview(
    device = Devices.PIXEL_7,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
fun ExpensePresetDialogPreview() {
    LazyWalletTheme {
        Surface (
            modifier = Modifier.fillMaxSize()
        ) {
            ExpensePresetDialog(
                selectedPreset = null,
                onDismissRequest = {},
                onConfirm = { _, _, _, _, _ -> },
                isSaving = false,
                action = ExpenseFormAction.ADD
            )
        }
    }
}
