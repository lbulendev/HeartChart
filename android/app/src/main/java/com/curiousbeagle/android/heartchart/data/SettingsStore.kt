package com.curiousbeagle.android.heartchart.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Persists the two things HeartChart remembers: the user's age (drives the
 * zone lines) and the paired sensor address — the DataStore analog of the
 * iOS app's @AppStorage/UserDefaults usage.
 */
class SettingsStore(private val dataStore: DataStore<Preferences>) {

    /** 0 = not set; the UI prompts until a valid age is saved. */
    val age: Flow<Int> = dataStore.data.map { it[AGE_KEY] ?: 0 }

    suspend fun setAge(age: Int) {
        dataStore.edit { it[AGE_KEY] = age }
    }

    suspend fun pairedSensorAddress(): String? =
        dataStore.data.first()[PAIRED_SENSOR_KEY]

    suspend fun setPairedSensorAddress(address: String?) {
        dataStore.edit { preferences ->
            if (address == null) {
                preferences.remove(PAIRED_SENSOR_KEY)
            } else {
                preferences[PAIRED_SENSOR_KEY] = address
            }
        }
    }

    private companion object {
        val AGE_KEY = intPreferencesKey("userAge")
        val PAIRED_SENSOR_KEY = stringPreferencesKey("pairedSensorAddress")
    }
}
