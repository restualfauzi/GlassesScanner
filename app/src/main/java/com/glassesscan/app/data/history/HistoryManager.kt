package com.glassesscan.app.data.history

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.glassesscan.app.domain.model.DetectedDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "history")

class HistoryManager(private val context: Context) {
    private val HISTORY_KEY = stringPreferencesKey("detection_history")

    val history: Flow<List<DetectedDevice>> = context.dataStore.data.map { preferences ->
        val json = preferences[HISTORY_KEY] ?: "[]"
        try {
            val list = Json.decodeFromString<List<DetectedDevice>>(json)
            list.distinctBy { it.address }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveDetection(device: DetectedDevice) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[HISTORY_KEY] ?: "[]"
            val currentList = try {
                Json.decodeFromString<List<DetectedDevice>>(currentJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            // Deduplicate: Remove previous entry with the same MAC address
            currentList.removeAll { it.address == device.address }
            
            // Add latest detection at top of list
            currentList.add(0, device)
            
            // Limit history to 50 items
            val limitedList = currentList.take(50)
            preferences[HISTORY_KEY] = Json.encodeToString(limitedList)
        }
    }

    suspend fun updateDeviceTag(address: String, tag: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[HISTORY_KEY] ?: "[]"
            val currentList = try {
                Json.decodeFromString<List<DetectedDevice>>(currentJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            val updatedList = currentList.map {
                if (it.address == address) it.copy(customTag = if (tag.isBlank()) null else tag) else it
            }
            preferences[HISTORY_KEY] = Json.encodeToString(updatedList)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { it.clear() }
    }
}
