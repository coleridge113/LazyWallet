package com.luna.budgetapp.domain.repository

import com.luna.budgetapp.domain.model.DateFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(
    initialProfile: String = "All",
    initialDateFilter: DateFilter = DateFilter.Daily,
    initialGuest: Boolean = false
) : SettingsRepository {

    private val _activeProfileFlow = MutableStateFlow(initialProfile)
    override val activeProfileFlow: Flow<String> = _activeProfileFlow

    private val _activeDateFilterFlow = MutableStateFlow(initialDateFilter)
    override val activeDateFilterFlow: Flow<DateFilter> = _activeDateFilterFlow

    private val _isMigratedFlow = MutableStateFlow(false)
    override val isMigratedFlow: Flow<Boolean> = _isMigratedFlow

    private val _isGuestFlow = MutableStateFlow(initialGuest)
    override val isGuestFlow: Flow<Boolean> = _isGuestFlow

    override suspend fun setGuestMode(isGuest: Boolean) {
        _isGuestFlow.value = isGuest
    }

    override suspend fun setMigrationComplete() {
        _isMigratedFlow.value = true
    }

    override suspend fun setActiveProfile(profileName: String) {
        _activeProfileFlow.value = profileName
    }

    override suspend fun setActiveDateFilter(dateFilter: DateFilter) {
        _activeDateFilterFlow.value = dateFilter
    }
}
