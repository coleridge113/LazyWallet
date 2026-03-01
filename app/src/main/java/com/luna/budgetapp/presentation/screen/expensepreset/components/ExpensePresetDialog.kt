package com.luna.budgetapp.presentation.screen.expensepreset.components

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
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.luna.budgetapp.domain.model.ExpensePreset
import com.luna.budgetapp.domain.model.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensePresetDialog(
    selectedPreset: ExpensePreset?,
    onDismissRequest: () -> Unit,
    onConfirm: (Category, String, String) -> Unit,
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
        val typeState = rememberTextFieldState(selectedPreset?.type ?: "")
        val amountState = rememberTextFieldState("")
        val isLocked = selectedPreset != null

        LaunchedEffect(Unit) {
            selectedPreset?.let {
                selectedOption = options.firstOrNull { option ->
                    option.displayName.equals(it.category, ignoreCase = true)
                } ?: options.first()
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
                    text = if (selectedPreset == null) "Add Expense Preset" else "Add Custom Expense",
                    style = MaterialTheme.typography.headlineSmall
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!isLocked) expanded = !expanded }
                ) {
                    TextField(
                        value = selectedOption.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = if (!isLocked) {{ Text("Select category") }} else null,
                        trailingIcon = {
                            if (!isLocked) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null
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
                                text = { Text(option.displayName) },
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
                    label = { Text("Type") }
                )

                OutlinedTextField(
                    state = amountState,
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    inputTransformation = InputTransformation {
                        val text = asCharSequence().toString()
                        if (!text.matches(Regex("^\\d*\\.?\\d*$"))) {
                            revertAllChanges()
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
                        onConfirm(
                            selectedOption,
                            typeState.text.toString(),
                            amountState.text.toString()
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
    Spacer(modifier = Modifier.height(50.dp))
    Box(modifier = Modifier.fillMaxSize()){
        ExpensePresetDialog(
            selectedPreset = null,
            onDismissRequest = {},
            onConfirm = { _, _, _ -> },
            isSaving = false,
        )
    }
}
