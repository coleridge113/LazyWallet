package com.luna.budgetapp.data.local.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.luna.budgetapp.data.datastore.SettingsDataStore
import com.luna.budgetapp.domain.repository.SettingsRepository
import com.luna.budgetapp.domain.model.DateFilter

class SettingsRepositoryImpl(
    private val local: SettingsDataStore
) : SettingsRepository {

    override val activeProfileFlow: Flow<String> = local.activeProfileFlow

    override val activeDateFilterFlow: Flow<DateFilter> = local.activeDateFilterFlow

    override suspend fun setActiveProfile(profileName: String) {
        local.setActiveProfile(profileName)
    }

    override suspend fun setActiveDateFilter(dateFilter: DateFilter) {
        local.setActiveDateFilter(dateFilter)
    }
}
