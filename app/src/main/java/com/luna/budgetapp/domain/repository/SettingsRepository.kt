package com.luna.budgetapp.domain.repository

import kotlinx.coroutines.flow.Flow
import com.luna.budgetapp.domain.model.DateFilter

interface SettingsRepository {

    val activeProfileFlow: Flow<String>

    val activeDateFilterFlow: Flow<DateFilter>

    val isMigratedFlow: Flow<Boolean>

    val isGuestFlow: Flow<Boolean>

    suspend fun setActiveProfile(profileName: String)

    suspend fun setActiveDateFilter(dateFilter: DateFilter)

    suspend fun setMigrationComplete()

    suspend fun setGuestMode(isGuest: Boolean)

}
