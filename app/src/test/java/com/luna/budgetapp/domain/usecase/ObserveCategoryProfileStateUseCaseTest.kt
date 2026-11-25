package com.luna.budgetapp.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.CategoryFilter
import com.luna.budgetapp.domain.repository.FakeCategoryRepository
import com.luna.budgetapp.domain.repository.FakeSettingsRepository
import com.luna.budgetapp.domain.usecase.category.GetCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.category.GetCategoryProfilesUseCase
import com.luna.budgetapp.domain.usecase.category.ObserveCategoryProfileStateUseCase
import com.luna.budgetapp.domain.usecase.settings.GetActiveCategoryProfileUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveCategoryProfileStateUseCaseTest {
    private val categoryRepo = FakeCategoryRepository()
    private val settingsRepo = FakeSettingsRepository()
    private val useCase = ObserveCategoryProfileStateUseCase(
        GetActiveCategoryProfileUseCase(settingsRepo),
        GetCategoryProfileUseCase(categoryRepo),
        GetCategoryProfilesUseCase(categoryRepo)
    )

    @Test
    fun `merges profile filters over all-active default and lists profiles`() = runTest {
        categoryRepo.saveProfile(
            listOf(CategoryFilter(profileName = "Work", category = Category.FOOD, isActive = false))
        )
        settingsRepo.setActiveProfile("Work")

        val snapshot = useCase().first()

        assertThat(snapshot.activeProfile).isEqualTo("Work")
        assertThat(snapshot.profileList).containsExactly("Work")
        assertThat(snapshot.categoryMap[Category.FOOD]).isFalse()
        assertThat(snapshot.categoryMap.filterKeys { it != Category.FOOD }.values).doesNotContain(false)
    }

    @Test
    fun `omits profile list when not requested`() = runTest {
        categoryRepo.initializeIfNeeded()
        settingsRepo.setActiveProfile("All")

        val snapshot = useCase(includeProfileList = false).first()

        assertThat(snapshot.profileList).isEmpty()
        assertThat(snapshot.categoryMap.values).doesNotContain(false)
    }
}
