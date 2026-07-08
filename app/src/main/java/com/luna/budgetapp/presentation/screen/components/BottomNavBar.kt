package com.luna.budgetapp.presentation.screen.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import com.luna.budgetapp.presentation.model.NavOptions

@Composable
fun BottomNavBar(
    selectedItem: NavOptions,
    navCallback: (NavOptions) -> Unit
) {
    val items = NavOptions.entries

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.name) },
                selected = selectedItem == item,
                onClick = {
                    if (selectedItem == item) return@NavigationBarItem
                    navCallback(item)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }

}
