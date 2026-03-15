package com.luna.budgetapp.domain.usecase

import com.luna.budgetapp.domain.usecase.category.DeleteCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.category.GetCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.category.GetCategoryProfilesUseCase
import com.luna.budgetapp.domain.usecase.category.InitializeCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.category.SaveCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.settings.GetActiveCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.settings.SetActiveCategoryProfileUseCase

data class ProfileUseCases(
    val getCategoryProfile: GetCategoryProfileUseCase,
    val getCategoryProfiles: GetCategoryProfilesUseCase,
    val saveCategoryProfile: SaveCategoryProfileUseCase,
    val deleteCategoryProfile: DeleteCategoryProfileUseCase,
    val initializeCategoryProfile: InitializeCategoryProfileUseCase,
    val getActiveCategoryProfile: GetActiveCategoryProfileUseCase,
    val setActiveCategoryProfile: SetActiveCategoryProfileUseCase,
)
