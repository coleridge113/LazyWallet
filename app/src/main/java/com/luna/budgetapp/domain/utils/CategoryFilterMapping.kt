package com.luna.budgetapp.domain.utils

import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.CategoryFilter

/** Full per-category map for a profile's filters; categories missing from the profile default to active. */
fun List<CategoryFilter>.toCategoryMap(): Map<Category, Boolean> =
    Category.entries.associateWith { true } + associate { it.category to it.isActive }
