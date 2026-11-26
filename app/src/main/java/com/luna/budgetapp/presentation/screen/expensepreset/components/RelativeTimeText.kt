package com.luna.budgetapp.presentation.screen.expensepreset.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.luna.budgetapp.R
import com.luna.budgetapp.presentation.screen.expensepreset.model.RelativeTime

/** Localised label for a relative time, or null when nothing should be shown. */
@Composable
fun RelativeTime.label(): String? = when (this) {
    RelativeTime.JustNow -> stringResource(R.string.expense_preset_just_now)
    is RelativeTime.Minutes -> stringResource(R.string.expense_preset_minutes_ago, n.toInt())
    is RelativeTime.Hours -> stringResource(R.string.expense_preset_hours_ago, n.toInt())
    RelativeTime.None -> null
}
