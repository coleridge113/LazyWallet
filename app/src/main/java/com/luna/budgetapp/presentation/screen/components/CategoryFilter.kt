package com.luna.budgetapp.presentation.screen.components

import com.luna.budgetapp.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.ui.theme.MaterialBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterDialog(
    modifier: Modifier = Modifier,
    selectedCategoryMap: Map<Category, Boolean>,
    onDismiss: () -> Unit,
    onConfirm: (Map<Category, Boolean>) -> Unit,
    onSaveConfirm: (String, Map<Category, Boolean>) -> Unit,
    selectedProfile: String = "Default",
    profileList: List<String>,
    onSelectedChange: (String) -> Unit
) {

    val applySaveText = stringResource(R.string.btn_save)
    val cancelText = stringResource(R.string.btn_cancel)
    val applyText = stringResource(R.string.btn_apply)
    val customText = stringResource(R.string.title_custom)

    var tempMap by remember(selectedCategoryMap) { mutableStateOf(selectedCategoryMap) }
    var textTitle by remember { mutableStateOf(selectedProfile) }
    var isSaveMode by remember { mutableStateOf(false) }
    var profileNameInput by remember { mutableStateOf("") }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {

                if (!isSaveMode) {
                    CategoryProfileSelectorDropdown(
                        selectedProfile = textTitle,
                        profileList = profileList,
                        onSelectedChange = onSelectedChange,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 10.dp
                        ),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    CategoryFilter(
                        selectedCategoryMap = tempMap,
                        onCheckedChange = { category, isChecked ->
                            tempMap = tempMap + (category to isChecked)
                            textTitle = customText
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(cancelText)
                        }
                        TextButton(
                            onClick = { onConfirm(tempMap) }
                        ) {
                            Text(
                                text = applyText,
                                color = MaterialBlue
                            )
                        }

                        if (textTitle == customText) {
                            TextButton(
                                onClick = { isSaveMode = true }
                            ) {
                                Text(
                                    text = applySaveText
                                )
                            }
                        }

                    }
                } else {
                    OutlinedTextField(
                        value = profileNameInput,
                        onValueChange = { profileNameInput = it },
                        label = { Text("Profile Name") },
                        singleLine = true
                    )


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { isSaveMode = false }
                        ) {
                            Text(stringResource(R.string.btn_cancel))
                        }

                        TextButton(
                            onClick = {
                                onSaveConfirm(profileNameInput, tempMap)
                                isSaveMode = false
                            }
                        ) {
                            Text(stringResource(R.string.btn_save))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryFilter(
    selectedCategoryMap: Map<Category, Boolean>,
    onCheckedChange: (Category, Boolean) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.Center
    ) {
        items(Category.entries) { item ->

            val isChecked =
                selectedCategoryMap[item]!!

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    onCheckedChange(item, !isChecked)                    
                }
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { checked ->
                        onCheckedChange(item, checked)
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialBlue
                    )
                )

                Text(text = item.displayName)
            }
        }
    }
}

@Composable
fun CategoryProfileSelectorDropdown(
    modifier: Modifier = Modifier,
    selectedProfile: String,
    profileList: List<String>,
    onSelectedChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier.fillMaxWidth()
            .padding(start = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { expanded = true }
        ) {
            Text(
                text = selectedProfile,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.width(4.dp))

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            profileList.forEach { profile ->
                DropdownMenuItem(
                    text = {
                        Text(profile)
                    },
                    onClick = {
                        onSelectedChange(profile)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun CategoryFilterPreview() {

    val selectedCategoryMap = mapOf(
        Category.FOOD to true,
        Category.BEVERAGE to true,
        Category.DATE to false,
        Category.HOUSE to false,
        Category.COMMUTE to true,
        Category.BILLS to false,
        Category.GROCERY to false,
        Category.OTHERS to true,
        Category.FITNESS to false,
        Category.PERSONAL to true,
    )

    Surface(
        color = Color.White
    ) {
        CategoryFilterDialog(
            selectedCategoryMap = selectedCategoryMap,
            onDismiss = {},
            onConfirm = { _ -> },
            onSelectedChange = {},
            profileList = listOf("Default", "Profile 1"),
            onSaveConfirm = {_, _ ->}
        )
    }
}
