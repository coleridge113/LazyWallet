package com.luna.budgetapp.domain.usecase.expense

import com.luna.budgetapp.domain.repository.CategoryRepository
import com.luna.budgetapp.domain.model.CategoryFilter
import kotlinx.coroutines.flow.Flow

class DeleteCategoryProfileUseCase(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(profileName: String) {
        repository.deleteProfile(profileName)
    }
}
