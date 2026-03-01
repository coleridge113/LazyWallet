package com.luna.budgetapp.domain.usecase.expense

import com.luna.budgetapp.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

class GetCategoryProfilesUseCase(
    private val repository: CategoryRepository
) {
    operator fun invoke(): Flow<List<String>> {
        return repository.getProfiles()
    }
}
