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
    private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
    private val IGNORED_DEVICES_KEY = stringPreferencesKey("ignored_devices")
    private val SCAN_SENSITIVITY_KEY = stringPreferencesKey("scan_sensitivity") // "LOW", "MEDIUM", "HIGH")
    private val STALKER_DETECTION_KEY = booleanPreferencesKey("stalker_detection_enabled")
    private val SOUND_ALERT_KEY = booleanPreferencesKey("sound_alert_enabled")
    private val AUTO_SAVE_HISTORY_KEY = booleanPreferencesKey("auto_save_history")
    private val ECO_SCAN_MODE_KEY = booleanPreferencesKey("eco_scan_mode")

    val ecoScanMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ECO_SCAN_MODE_KEY] ?: false
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: false
    }

    val vibrateOnDetect: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VIBRATE_KEY] ?: true
    }

    val soundAlertEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SOUND_ALERT_KEY] ?: true
    }

    val autoSaveHistory: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_SAVE_HISTORY_KEY] ?: true
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

    val scanSensitivity: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SCAN_SENSITIVITY_KEY] ?: "MEDIUM"
    }

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED_KEY] ?: false
    }

    val ignoredDevices: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        val json = preferences[IGNORED_DEVICES_KEY] ?: "[]"
        try {
            Json.decodeFromString<Set<String>>(json)
        } catch (e: Exception) {
            emptySet()
        }
    }

    val stalkerDetectionEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[STALKER_DETECTION_KEY] ?: true
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

    suspend fun setScanSensitivity(sensitivity: String) {
        context.dataStore.edit { preferences ->
            preferences[SCAN_SENSITIVITY_KEY] = sensitivity
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = completed
        }
    }

    suspend fun setStalkerDetectionEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[STALKER_DETECTION_KEY] = enabled
        }
    }

    suspend fun setSoundAlertEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SOUND_ALERT_KEY] = enabled
        }
    }

    suspend fun setAutoSaveHistory(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_SAVE_HISTORY_KEY] = enabled
        }
    }

    suspend fun setEcoScanMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ECO_SCAN_MODE_KEY] = enabled
        }
    }

    suspend fun toggleIgnoreDevice(address: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[IGNORED_DEVICES_KEY] ?: "[]"
            val currentSet = try {
                Json.decodeFromString<Set<String>>(currentJson).toMutableSet()
            } catch (e: Exception) {
                mutableSetOf()
            }
            
            if (currentSet.contains(address)) {
                currentSet.remove(address)
            } else {
                currentSet.add(address)
            }
            
            preferences[IGNORED_DEVICES_KEY] = Json.encodeToString(currentSet)
        }
    }
}
