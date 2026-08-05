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
            Json.decodeFromString<List<DetectedDevice>>(json)
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
            
            // Avoid duplicates of the same address in history if they are recent? 
            // Or just keep adding with timestamp.
            currentList.add(0, device)
            
            // Limit history to 50 items
            val limitedList = currentList.take(50)
            preferences[HISTORY_KEY] = Json.encodeToString(limitedList)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { it.clear() }
    }
}
