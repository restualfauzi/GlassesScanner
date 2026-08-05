package com.glassesscan.app.ui.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.glassesscan.app.data.history.HistoryManager
import com.glassesscan.app.data.settings.SettingsManager
import com.glassesscan.app.domain.model.DetectedDevice
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val historyManager: HistoryManager,
    private val settingsManager: SettingsManager
) : ViewModel() {
    val history: StateFlow<List<DetectedDevice>> = historyManager.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun clearHistory() {
        viewModelScope.launch {
            historyManager.clearHistory()
        }
    }

    fun updateDeviceTag(address: String, tag: String) {
        viewModelScope.launch {
            settingsManager.setDeviceTag(address, tag)
        }
    }
}

class HistoryViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            val historyManager = HistoryManager(context.applicationContext)
            val settingsManager = SettingsManager(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(historyManager, settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
