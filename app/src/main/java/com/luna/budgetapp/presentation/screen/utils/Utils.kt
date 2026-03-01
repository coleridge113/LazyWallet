package com.luna.budgetapp.presentation.screen.utils

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import com.luna.budgetapp.presentation.model.CategoryOptions
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

fun getIconForCategory(category: String): ImageVector {
    return CategoryOptions.entries
        .firstOrNull { it.name == category }
        ?.icon
        ?: Icons.Default.AttachMoney
}

fun Double.formatToCurrency(): String {
    return "%,.2f".format(this)
}

fun Double.formatToPercentage(): String {
    val percent = (this * 100).roundToInt()
    return "${percent.coerceAtLeast(0)}%"
}

fun LocalDateTime.formatToDisplay(): String {
    val formatter = DateTimeFormatter.ofPattern(
        "dd MMMM yyyy",
        Locale.getDefault()
    )
    return this.format(formatter)
}

fun Modifier.singleClick(
    interval: Long = 600L,
    onClick: () -> Unit
): Modifier = composed {
    var lastClickTime by remember { mutableLongStateOf(0L) }

    clickable {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > interval) {
            lastClickTime = currentTime
            onClick()
        }
    }
}
