package com.luna.budgetapp.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Wallet

enum class NavOptions(
    val icon: ImageVector
) {
    HOME(Icons.Default.Home),
    LIST(Icons.AutoMirrored.Filled.FormatListBulleted),
    ANALYSIS(Icons.Default.BarChart),
    BUDGET(Icons.Default.Wallet)
}
