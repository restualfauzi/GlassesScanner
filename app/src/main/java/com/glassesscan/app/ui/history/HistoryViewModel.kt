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

    fun exportHistoryToCsv(context: android.content.Context) {
        val currentHistory = history.value
        if (currentHistory.isEmpty()) return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val csvHeader = "Date,Time,Device Name,MAC Address,Signal (RSSI),Proximity,Is Meta\n"
            val csvData = currentHistory.joinToString("\n") { device ->
                val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(device.timestamp))
                val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(device.timestamp))
                "${date},${time},${device.name ?: "Unknown"},${device.address},${device.rssi},${device.proximity},${device.isMetaGlasses}"
            }
            
            val csvContent = csvHeader + csvData
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                shareCsvFile(context, csvContent)
            }
        }
    }

    private fun shareCsvFile(context: android.content.Context, content: String) {
        val fileName = "Glasses_Scanner_History_${System.currentTimeMillis()}.csv"
        try {
            val file = java.io.File(context.cacheDir, fileName)
            file.writeText(content)
            
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Glasses Scanner Detection Logs")
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(android.content.Intent.createChooser(intent, "Export Audit Logs"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
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
