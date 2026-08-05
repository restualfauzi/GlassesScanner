package com.glassesscan.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.glassesscan.app.data.settings.SettingsManager
import com.glassesscan.app.util.BillingManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsManager: SettingsManager,
    private val billingManager: BillingManager
) : ViewModel() {
    val isDarkMode: StateFlow<Boolean> = settingsManager.isDarkMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val vibrateOnDetect: StateFlow<Boolean> = settingsManager.vibrateOnDetect.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val zoneSecurityMode: StateFlow<Boolean> = settingsManager.zoneSecurityMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val isPremium: StateFlow<Boolean> = settingsManager.isPremium.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val languageCode: StateFlow<String> = settingsManager.languageCode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "en"
    )

    val premiumPrice: StateFlow<String?> = billingManager.premiumPrice.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setDarkMode(enabled)
        }
    }

    fun setVibrate(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setVibrate(enabled)
        }
    }

    fun setZoneSecurityMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setZoneSecurityMode(enabled)
        }
    }

    fun setPremium(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setPremium(enabled)
        }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch {
            settingsManager.setLanguage(code)
        }
    }

    fun buyPremium(activity: android.app.Activity) {
        billingManager.launchPurchaseFlow(activity)
    }
}

class SettingsViewModelFactory(
    private val context: Context,
    private val billingManager: BillingManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val settingsManager = SettingsManager(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsManager, billingManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
