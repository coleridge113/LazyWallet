package com.luna.budgetapp.domain.repository

import com.luna.budgetapp.domain.model.DateFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(
    initialProfile: String = "All",
    initialDateFilter: DateFilter = DateFilter.Daily
) : SettingsRepository {

    private val _activeProfileFlow = MutableStateFlow(initialProfile)
    override val activeProfileFlow: Flow<String> = _activeProfileFlow

    private val _activeDateFilterFlow = MutableStateFlow(initialDateFilter)
    override val activeDateFilterFlow: Flow<DateFilter> = _activeDateFilterFlow

    override suspend fun setActiveProfile(profileName: String) {
        _activeProfileFlow.value = profileName
    }

    override suspend fun setActiveDateFilter(dateFilter: DateFilter) {
        _activeDateFilterFlow.value = dateFilter
    }
}
