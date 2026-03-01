package com.luna.budgetapp.domain.usecase.expense

import com.luna.budgetapp.domain.repository.CategoryRepository
import com.luna.budgetapp.domain.model.CategoryFilter
import kotlinx.coroutines.flow.Flow

class GetCategoryProfileUseCase(
    private val repository: CategoryRepository
) {
    operator fun invoke(profileName: String): Flow<List<CategoryFilter>> {
        return repository.getProfile(profileName)
    }
}
