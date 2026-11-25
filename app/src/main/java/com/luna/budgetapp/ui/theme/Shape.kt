package com.luna.budgetapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Values equal the M3 defaults; set explicitly so the app's shapes are defined in one place.
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Preset row pill (24dp has no M3 slot; extraLarge stays 28dp for dialogs/date pickers).
val PresetPillShape = RoundedCornerShape(24.dp)
