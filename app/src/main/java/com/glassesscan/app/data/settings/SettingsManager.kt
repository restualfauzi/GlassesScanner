package com.glassesscan.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    private val VIBRATE_KEY = booleanPreferencesKey("vibrate_on_detect")
    private val ZONE_SECURITY_KEY = booleanPreferencesKey("zone_security_mode")
    private val PREMIUM_KEY = booleanPreferencesKey("is_premium")
    private val LANGUAGE_KEY = stringPreferencesKey("language_code")
    private val DEVICE_TAGS_KEY = stringPreferencesKey("device_tags")

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: false
    }

    val vibrateOnDetect: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VIBRATE_KEY] ?: true
    }

    val zoneSecurityMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ZONE_SECURITY_KEY] ?: false
    }

    val deviceTags: Flow<Map<String, String>> = context.dataStore.data.map { preferences ->
        val json = preferences[DEVICE_TAGS_KEY] ?: "{}"
        try {
            Json.decodeFromString<Map<String, String>>(json)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    val isPremium: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PREMIUM_KEY] ?: false
    }

    val languageCode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: "en"
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    suspend fun setVibrate(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIBRATE_KEY] = enabled
        }
    }

    suspend fun setZoneSecurityMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ZONE_SECURITY_KEY] = enabled
        }
    }

    suspend fun setDeviceTag(address: String, tag: String) {
        context.dataStore.edit { preferences ->
            val json = preferences[DEVICE_TAGS_KEY] ?: "{}"
            val tags = try {
                Json.decodeFromString<Map<String, String>>(json).toMutableMap()
            } catch (e: Exception) {
                mutableMapOf()
            }
            tags[address] = tag
            preferences[DEVICE_TAGS_KEY] = Json.encodeToString(tags)
        }
    }

    suspend fun setPremium(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PREMIUM_KEY] = enabled
        }
    }

    suspend fun setLanguage(code: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = code
        }
    }
}
