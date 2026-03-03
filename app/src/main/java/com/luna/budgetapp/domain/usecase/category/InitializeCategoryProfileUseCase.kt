package com.luna.budgetapp.domain.usecase.category

import com.luna.budgetapp.domain.repository.CategoryRepository

class InitializeCategoryProfileUseCase(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke() {
        repository.initializeIfNeeded()
    }
}
