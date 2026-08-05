package com.glassesscan.app.ui.scanner

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.glassesscan.app.data.history.HistoryManager
import com.glassesscan.app.data.repository.ScannerRepositoryImpl
import com.glassesscan.app.data.scanner.BleScannerHelper

import com.glassesscan.app.data.settings.SettingsManager
import com.glassesscan.app.util.LocationHelper

class ScannerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScannerViewModel::class.java)) {
            val bleScannerHelper = BleScannerHelper(context.applicationContext)
            val repository = ScannerRepositoryImpl(bleScannerHelper)
            val historyManager = HistoryManager(context.applicationContext)
            val settingsManager = SettingsManager(context.applicationContext)
            val locationHelper = LocationHelper(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return ScannerViewModel(
                context.applicationContext,
                repository,
                historyManager,
                settingsManager,
                locationHelper
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
