package com.luna.budgetapp.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.MutablePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.luna.budgetapp.domain.model.DateFilter

class SettingsDataStore(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val activeProfile = stringPreferencesKey("active_profile")
        val dateFilterType = stringPreferencesKey("date_filter_type")
        val customStart = longPreferencesKey("custom_start")
        val customEnd = longPreferencesKey("custom_end")
    }

    /* ------------------------------
       Active Profile
    ------------------------------ */

    val activeProfileFlow: Flow<String> =
        dataStore.data.map { prefs ->
            prefs[Keys.activeProfile] ?: "Default"
        }

    suspend fun setActiveProfile(profileName: String) {
        dataStore.edit { prefs ->
            prefs[Keys.activeProfile] = profileName
        }
    }

    /* ------------------------------
       Date Filter
    ------------------------------ */

    val activeDateFilterFlow: Flow<DateFilter> =
        dataStore.data.map { prefs ->
            when (prefs[Keys.dateFilterType]) {

                "DAILY" -> DateFilter.Daily
                "WEEKLY" -> DateFilter.Weekly
                "MONTHLY" -> DateFilter.Monthly

                "CUSTOM" -> {
                    val start = prefs[Keys.customStart] ?: 0L
                    val end = prefs[Keys.customEnd]
                    DateFilter.Custom(start, end)
                }

                else -> DateFilter.Daily
            }
        }

    suspend fun setActiveDateFilter(filter: DateFilter) {
        dataStore.edit { prefs ->

            when (filter) {

                DateFilter.Daily -> {
                    prefs[Keys.dateFilterType] = "DAILY"
                    clearCustom(prefs)
                }

                DateFilter.Weekly -> {
                    prefs[Keys.dateFilterType] = "WEEKLY"
                    clearCustom(prefs)
                }

                DateFilter.Monthly -> {
                    prefs[Keys.dateFilterType] = "MONTHLY"
                    clearCustom(prefs)
                }

                is DateFilter.Custom -> {
                    prefs[Keys.dateFilterType] = "CUSTOM"
                    prefs[Keys.customStart] = filter.start
                    filter.end?.let { prefs[Keys.customEnd] = it }
                }
            }
        }
    }

    private fun clearCustom(prefs: MutablePreferences) {
        prefs.remove(Keys.customStart)
        prefs.remove(Keys.customEnd)
    }
}
