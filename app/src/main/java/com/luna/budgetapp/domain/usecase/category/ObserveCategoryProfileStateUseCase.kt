package com.luna.budgetapp.domain.usecase.category

import com.luna.budgetapp.domain.model.CategoryProfileSnapshot
import com.luna.budgetapp.domain.usecase.settings.GetActiveCategoryProfileUseCase
import com.luna.budgetapp.domain.utils.toCategoryMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Observes the active category profile with its per-category active flags.
 * Categories missing from the profile default to active. When [includeProfileList] is false the
 * profile-names flow is not observed and [CategoryProfileSnapshot.profileList] is empty.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveCategoryProfileStateUseCase(
    private val getActiveCategoryProfile: GetActiveCategoryProfileUseCase,
    private val getCategoryProfile: GetCategoryProfileUseCase,
    private val getCategoryProfiles: GetCategoryProfilesUseCase
) {
    operator fun invoke(includeProfileList: Boolean = true): Flow<CategoryProfileSnapshot> {
        val activeAndMap = getActiveCategoryProfile()
            .flatMapLatest { activeProfile ->
                getCategoryProfile(activeProfile).map { filters ->
                    activeProfile to filters.toCategoryMap()
                }
            }
        if (!includeProfileList) {
            return activeAndMap.map { (active, map) -> CategoryProfileSnapshot(active, map, emptyList()) }
        }
        return activeAndMap.combine(getCategoryProfiles()) { (active, map), profileList ->
            CategoryProfileSnapshot(active, map, profileList)
        }
    }
}
