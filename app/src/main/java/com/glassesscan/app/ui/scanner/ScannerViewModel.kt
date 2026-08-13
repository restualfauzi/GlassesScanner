package com.glassesscan.app.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glassesscan.app.domain.model.DetectedDevice
import com.glassesscan.app.domain.repository.ScannerRepository
import com.glassesscan.app.data.history.HistoryManager
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.content.Context
import com.glassesscan.app.util.LocationHelper
import com.glassesscan.app.data.settings.SettingsManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ScannerUiState(
    val isScanning: Boolean = false,
    val detectedDevices: List<DetectedDevice> = emptyList(),
    val ignoredDevices: Set<String> = emptySet(),
    val wifiDirectActive: Boolean = false,
    val metaGlassesDetected: Boolean = false,
    val zoneSecurityMode: Boolean = false,
    val scanSensitivity: String = "MEDIUM",
    val stalkerDetected: DetectedDevice? = null,
    val selectedDevice: DetectedDevice? = null,
    val errorMessage: String? = null,
    val soundAlertEnabled: Boolean = false
)

class ScannerViewModel(
    private val context: Context,
    private val repository: ScannerRepository,
    private val historyManager: HistoryManager,
    private val settingsManager: SettingsManager,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()
    
    private val deviceRssiHistory = mutableMapOf<String, MutableList<Int>>()
    private val _deviceAngles = mutableMapOf<String, Float>()
    private val _deviceDisplayRssi = mutableMapOf<String, Float>()
    private val deviceDiscoveryHistory = mutableMapOf<String, MutableList<Long>>() // MAC -> List of discovery timestamps
    
    val deviceAngles: Map<String, Float> get() = _deviceAngles
    val deviceDisplayRssi: Map<String, Float> get() = _deviceDisplayRssi

    private val toneGenerator = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)

    init {
        viewModelScope.launch {
            settingsManager.zoneSecurityMode.collect { mode ->
                _uiState.update { it.copy(zoneSecurityMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsManager.ignoredDevices.collect { ignored ->
                _uiState.update { it.copy(ignoredDevices = ignored) }
            }
        }
        viewModelScope.launch {
            settingsManager.scanSensitivity.collect { sensitivity ->
                _uiState.update { it.copy(scanSensitivity = sensitivity) }
            }
        }
    }

    private var scanJob: Job? = null
    private var soundJob: Job? = null

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    fun startScanning() {
        if (_uiState.value.isScanning) return
        
        // Start sound job if enabled
        if (_uiState.value.soundAlertEnabled) {
            startSoundJob()
        }

        // 1. Pre-check Bluetooth
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        val adapter = bluetoothManager.adapter
        if (adapter == null || !adapter.isEnabled) {
            _uiState.update { it.copy(isScanning = false, errorMessage = "Please enable Bluetooth") }
            return
        }

        // 2. Pre-check Location Services (GPS) - Required for BLE scanning on most Androids
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        if (!isGpsEnabled && !isNetworkEnabled) {
            _uiState.update { it.copy(isScanning = false, errorMessage = "Please enable Location Services") }
            return
        }

        _uiState.update { it.copy(isScanning = true, errorMessage = null) }

        scanJob = viewModelScope.launch {
            val tags = settingsManager.deviceTags.first()
            
            repository.scanDevices()
                .catch { e ->
                    val msg = if (e.message == "BLE_NOT_AVAILABLE") "Bluetooth OFF" else e.message
                    _uiState.update { it.copy(isScanning = false, errorMessage = msg) }
                }
                .collect { devices ->
                    val now = System.currentTimeMillis()
                    val shouldVibrate = settingsManager.vibrateOnDetect.first()
                    val isStalkerDetectionEnabled = settingsManager.stalkerDetectionEnabled.first()
                    val sensitivity = _uiState.value.scanSensitivity
                    
                    val filteredBySensitivity = devices.filter { device ->
                        when (sensitivity) {
                            "LOW" -> device.rssi > -70
                            "MEDIUM" -> device.rssi > -85
                            else -> true // HIGH
                        }
                    }

                    val processedDevices = filteredBySensitivity.map { device ->
                        if (!_deviceAngles.containsKey(device.address)) {
                            _deviceAngles[device.address] = (0..360).random().toFloat()
                        }
                        
                        // Low Pass Filter for stable radar display
                        val currentDisplay = _deviceDisplayRssi[device.address] ?: device.rssi.toFloat()
                        val filteredRssi = (currentDisplay * 0.85f) + (device.rssi.toFloat() * 0.15f)
                        _deviceDisplayRssi[device.address] = filteredRssi

                        val history = deviceRssiHistory.getOrPut(device.address) { mutableListOf() }
                        history.add(device.rssi)
                        if (history.size > 20) history.removeAt(0)
                        
                        device.copy(
                            customTag = tags[device.address],
                            rssiHistory = history.toList()
                        )
                    }

                    // Stalker Detection Logic
                    if (isStalkerDetectionEnabled) {
                        processedDevices.filter { it.isMetaGlasses }.forEach { device ->
                            val historyTimes = deviceDiscoveryHistory.getOrPut(device.address) { mutableListOf() }
                            historyTimes.add(now)
                            
                            // Remove history older than 30 mins
                            historyTimes.removeAll { now - it > 30 * 60 * 1000 }
                            
                            // If seen in at least 3 distinct scan cycles over more than 5 minutes
                            if (historyTimes.size > 3 && (now - historyTimes.first() > 5 * 60 * 1000)) {
                                _uiState.update { it.copy(stalkerDetected = device) }
                            }
                        }
                    }

                    val state = _uiState.value
                    val hasMeta = processedDevices.any { it.isMetaGlasses && !state.ignoredDevices.contains(it.address) }

                    processedDevices.forEach { device ->
                        val isIgnored = state.ignoredDevices.contains(device.address)
                        if (!isIgnored && canSaveToHistory(device.address, now)) {
                            // Update last saved time immediately to prevent double triggers
                            lastSavedMap[device.address] = now
                            
                            viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    val location = locationHelper.getLastLocation()
                                    val isZoneMode = _uiState.value.zoneSecurityMode
                                    val isThreat = device.isMetaGlasses || isZoneMode
                                    
                                    val deviceWithLoc = device.copy(
                                        latitude = location?.latitude,
                                        longitude = location?.longitude,
                                        isZoneAlert = isZoneMode && device.isMetaGlasses
                                    )
                                    historyManager.saveDetection(deviceWithLoc)
                                    
                                    if (shouldVibrate && isThreat) {
                                        withContext(Dispatchers.Main) {
                                            triggerVibration()
                                        }
                                    }
                                } catch (e: Exception) {}
                            }
                        }
                    }

                    _uiState.update { 
                        val metaCount = processedDevices.count { d -> d.isMetaGlasses && !it.ignoredDevices.contains(d.address) }
                        if (it.isScanning) {
                            com.glassesscan.app.service.GlassesScannerWidget.updateAllWidgets(
                                context,
                                if (metaCount > 0) "THREAT DETECTED" else "SYSTEM ACTIVE",
                                if (metaCount > 0) "$metaCount glasses nearby" else "Environment Safe"
                            )
                        }
                        
                        it.copy(
                            detectedDevices = processedDevices,
                            metaGlassesDetected = hasMeta,
                            wifiDirectActive = processedDevices.any { d -> d.name?.contains("DIRECT-", ignoreCase = true) == true },
                            selectedDevice = processedDevices.find { d -> d.address == it.selectedDevice?.address }
                        ) 
                    }
                }
        }
    }

    fun selectDevice(device: DetectedDevice?) {
        _uiState.update { it.copy(selectedDevice = device) }
    }

    fun toggleZoneSecurity() {
        viewModelScope.launch {
            settingsManager.setZoneSecurityMode(!_uiState.value.zoneSecurityMode)
        }
    }

    fun toggleSoundAlert() {
        val newState = !_uiState.value.soundAlertEnabled
        _uiState.update { it.copy(soundAlertEnabled = newState) }
        if (newState && _uiState.value.isScanning) {
            startSoundJob()
        } else {
            soundJob?.cancel()
            soundJob = null
        }
    }

    private fun startSoundJob() {
        soundJob?.cancel()
        soundJob = viewModelScope.launch {
            while (isActive) {
                val devices = _uiState.value.detectedDevices
                val metaDevices = devices.filter { it.isMetaGlasses && !_uiState.value.ignoredDevices.contains(it.address) }
                
                if (metaDevices.isNotEmpty()) {
                    val bestRssi = metaDevices.maxOf { it.rssi }
                    // Map RSSI (-100 to -40) to delay (1000ms to 100ms)
                    val delayMs = ((bestRssi.coerceIn(-100, -40) + 100) / 60f * -900 + 1000).toLong()
                    
                    toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 50)
                    delay(delayMs)
                } else {
                    delay(1000)
                }
            }
        }
    }

    fun updateDeviceTag(address: String, tag: String) {
        viewModelScope.launch {
            settingsManager.setDeviceTag(address, tag)
            val tags = settingsManager.deviceTags.first()
            _uiState.update { state ->
                state.copy(detectedDevices = state.detectedDevices.map { 
                    if (it.address == address) it.copy(customTag = tags[address]) else it
                })
            }
        }
    }

    private fun triggerVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    private val lastSavedMap = mutableMapOf<String, Long>()
    private fun canSaveToHistory(address: String, now: Long): Boolean {
        val lastSaved = lastSavedMap[address] ?: 0L
        return now - lastSaved > 30000 // 30 seconds gap
    }

    fun toggleIgnoreDevice(address: String) {
        viewModelScope.launch {
            settingsManager.toggleIgnoreDevice(address)
        }
    }

    fun dismissStalkerAlert() {
        _uiState.update { it.copy(stalkerDetected = null) }
    }

    fun reportFalsePositive(device: DetectedDevice) {
        // Send to Firebase Analytics
        val params = android.os.Bundle().apply {
            putString("device_address", device.address)
            putString("device_name", device.name ?: "Unknown")
            putInt("rssi", device.rssi)
        }
        com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
            .logEvent("false_positive_report", params)
    }

    fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
        soundJob?.cancel()
        soundJob = null
        com.glassesscan.app.service.GlassesScannerWidget.updateAllWidgets(context, "SCANNER IDLE", "Safe Environment")
        _uiState.update { it.copy(isScanning = false) }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
