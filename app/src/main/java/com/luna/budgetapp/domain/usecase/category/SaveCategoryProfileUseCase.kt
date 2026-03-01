package com.luna.budgetapp.domain.usecase.category

import com.luna.budgetapp.domain.repository.CategoryRepository
import com.luna.budgetapp.domain.model.CategoryFilter

class SaveCategoryProfileUseCase(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(items: List<CategoryFilter>) {
        repository.saveProfile(items)
    }
}
