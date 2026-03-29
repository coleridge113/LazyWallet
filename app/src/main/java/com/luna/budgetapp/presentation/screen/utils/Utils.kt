package com.luna.budgetapp.presentation.screen.utils

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.presentation.model.CategoryOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

fun getIconForCategory(category: String): ImageVector {
    return CategoryOptions.entries
        .firstOrNull { it.name == category }
        ?.icon
        ?: Icons.Default.AttachMoney
}

fun Double.toCurrency(): String {
    return if (this >= 0)
        "%,.2f".format(this)
    else
        "(%,.2f)".format(abs(this))
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

@OptIn(ExperimentalCoroutinesApi::class)
fun <STATE, T> Flow<STATE>.filterDataByState(
    dateFilterSelector: (STATE) -> DateFilter,
    categorySelector: (STATE) -> Map<Category, Boolean>,
    useCase: (categories: List<String>, start: LocalDateTime, end: LocalDateTime) -> Flow<T>
): Flow<T> =
    map { state ->
        dateFilterSelector(state) to categorySelector(state)
    }
        .distinctUntilChanged()
        .flatMapLatest { (dateFilter, categoryMap) ->

            val range = dateFilter.resolve()

            val activeCategories =
                categoryMap
                    .filterValues { it }
                    .keys
                    .map { it.name }

            useCase(activeCategories, range.start, range.end)
        }
