package com.luna.budgetapp.presentation.screen.expensepreset.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.BasicAlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Devices.PIXEL_7
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.domain.model.BudgetFrequency
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.presentation.screen.components.CategoryFilter
import com.luna.budgetapp.ui.theme.LazyWalletTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDialog(
    onDismissRequest: () -> Unit,
    onSave: (String, String, BudgetFrequency, Map<Category, Boolean>) -> Unit,
    modifier: Modifier = Modifier
) {
    val nameState = rememberTextFieldState()
    val amountState = rememberTextFieldState()
    val frequencyOptions = BudgetFrequency.entries
    var selectedOption by remember { mutableStateOf(frequencyOptions.first()) }
    var expanded by remember { mutableStateOf(false) }
    var tempMap by remember {
        mutableStateOf(Category.entries.associateWith { false })
    }

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
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
                    text = "Create Budget",
                    style = MaterialTheme.typography.headlineSmall
                )

                OutlinedTextField(
                    state = nameState,
                    label = { Text("Name") },
                    placeholder = { Text("My Daily Budget") },
                    modifier = Modifier.onFocusChanged {
                        if (it.isFocused) nameState.clearText()
                    }
                )

                OutlinedTextField(
                    state = amountState,
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    placeholder = {
                        Text("500.00")
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

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = selectedOption.getFriendlyName(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select frequency") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Dropdown Arrow"
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
                        frequencyOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.getFriendlyName()) },
                                onClick = {
                                    selectedOption = option
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Text(
                    text = "Scope",
                    modifier = Modifier.padding(top = 8.dp)
                )
                CategoryFilter(
                    selectedCategoryMap = tempMap,
                    onCheckedChange = { category, isChecked ->
                        tempMap = tempMap + (category to isChecked)
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onDismissRequest() }
                    ) {
                        Text("Cancel")
                    }

                    TextButton(
                        onClick = {
                            onSave(
                                nameState.text.toString(),
                                amountState.text.toString(),
                                selectedOption,
                                tempMap
                            )
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Preview(
    device = PIXEL_7,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
fun BudgetDialogPreview() {
    LazyWalletTheme {
        Surface (
            modifier = Modifier.fillMaxSize()
        ) {
            BudgetDialog(
                onDismissRequest = {},
                onSave = { _, _, _, _ -> },
                modifier = Modifier
            )
        }
    }
}
