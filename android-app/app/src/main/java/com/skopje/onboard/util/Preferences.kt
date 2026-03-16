package com.skopje.onboard.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferencesKeys {
    val STATION_NAME = stringPreferencesKey("station_name")
    val SURVEYOR_ID = stringPreferencesKey("surveyor_id")
    val LANGUAGE = stringPreferencesKey("language") // "mk" or "en"
    val THEME = stringPreferencesKey("theme") // "light", "dark", "system"
}

class Preferences(private val context: Context) {

    val stationName: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.STATION_NAME] }
    val surveyorId: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.SURVEYOR_ID] }
    val language: Flow<String> = context.dataStore.data.map { it[PreferencesKeys.LANGUAGE] ?: "mk" }
    val theme: Flow<String> = context.dataStore.data.map { it[PreferencesKeys.THEME] ?: "system" }

    suspend fun setStationName(value: String) {
        context.dataStore.edit { it[PreferencesKeys.STATION_NAME] = value }
    }

    suspend fun setSurveyorId(value: String) {
        context.dataStore.edit { it[PreferencesKeys.SURVEYOR_ID] = value }
    }

    suspend fun setLanguage(value: String) {
        context.dataStore.edit { it[PreferencesKeys.LANGUAGE] = value }
    }

    suspend fun setTheme(value: String) {
        context.dataStore.edit { it[PreferencesKeys.THEME] = value }
    }

    suspend fun getStationName(): String? = stationName.first()
    suspend fun getSurveyorId(): String? = surveyorId.first()
}
