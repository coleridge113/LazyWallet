package com.luna.budgetapp.domain.usecase.category

import com.luna.budgetapp.domain.repository.CategoryRepository

class DeleteCategoryProfileUseCase(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(profileName: String) {
        repository.deleteProfile(profileName)
    }
}
