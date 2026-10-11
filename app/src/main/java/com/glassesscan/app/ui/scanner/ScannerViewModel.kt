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
    val soundAlertEnabled: Boolean = true,
    val vibrateOnDetect: Boolean = true,
    val autoSaveHistory: Boolean = true,
    val safetyScore: Int = 100,
    val safetyStatus: String = "SAFE"
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

    fun injectSimulatedDevice(device: DetectedDevice) {
        _uiState.update { state ->
            val updated = state.detectedDevices.toMutableList()
            updated.removeAll { it.address == device.address }
            updated.add(0, device)
            state.copy(
                detectedDevices = updated, 
                metaGlassesDetected = true,
                isScanning = true
            )
        }
        viewModelScope.launch {
            try {
                historyManager.saveDetection(device)
            } catch (_: Exception) {}
        }
    }

    fun injectPlayStoreDemoDevices() {
        val demoDevices = listOf(
            DetectedDevice(
                address = "00:11:22:33:44:55",
                name = "Ray-Ban Meta (Gen 2)",
                rssi = -52,
                isMetaGlasses = true,
                latitude = 40.7580, // Times Square, NY, USA
                longitude = -73.9855,
                rssiHistory = listOf(-75, -68, -60, -55, -52)
            ),
            DetectedDevice(
                address = "AA:BB:CC:DD:EE:FF",
                name = "Spy HD Camera Glasses",
                rssi = -46,
                isMetaGlasses = true,
                isZoneAlert = true,
                latitude = 40.7614, // 5th Ave / MoMA, NY, USA
                longitude = -73.9776,
                rssiHistory = listOf(-65, -58, -50, -48, -46)
            ),
            DetectedDevice(
                address = "11:22:33:44:55:66",
                name = "XREAL Air 2 Ultra",
                rssi = -64,
                isMetaGlasses = true,
                latitude = 40.7527, // Grand Central, NY, USA
                longitude = -73.9772,
                rssiHistory = listOf(-80, -75, -70, -68, -64)
            ),
            DetectedDevice(
                address = "22:33:44:55:66:77",
                name = "Even Realities G1",
                rssi = -72,
                isMetaGlasses = true,
                latitude = 40.7484, // Empire State Building, NY, USA
                longitude = -73.9857,
                rssiHistory = listOf(-88, -82, -78, -75, -72)
            ),
            DetectedDevice(
                address = "33:44:55:66:77:88",
                name = "Snap Spectacles 4",
                rssi = -78,
                isMetaGlasses = true,
                latitude = 40.7648, // Central Park, NY, USA
                longitude = -73.9735,
                rssiHistory = listOf(-92, -88, -85, -80, -78)
            )
        )

        // Preset radar angles for clean screenshot visualization
        _deviceAngles["00:11:22:33:44:55"] = 45f
        _deviceAngles["AA:BB:CC:DD:EE:FF"] = 135f
        _deviceAngles["11:22:33:44:55:66"] = 210f
        _deviceAngles["22:33:44:55:66:77"] = 300f
        _deviceAngles["33:44:55:66:77:88"] = 340f

        demoDevices.forEach { device ->
            _deviceDisplayRssi[device.address] = device.rssi.toFloat()
        }

        _uiState.update { state ->
            state.copy(
                detectedDevices = demoDevices,
                metaGlassesDetected = true,
                isScanning = true,
                safetyScore = 42,
                safetyStatus = "THREAT DETECTED"
            )
        }

        viewModelScope.launch {
            demoDevices.forEach { device ->
                try {
                    historyManager.saveDetection(device)
                } catch (_: Exception) {}
            }
        }
    }

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
        viewModelScope.launch {
            settingsManager.vibrateOnDetect.collect { vibrate ->
                _uiState.update { it.copy(vibrateOnDetect = vibrate) }
            }
        }
        viewModelScope.launch {
            settingsManager.soundAlertEnabled.collect { sound ->
                _uiState.update { it.copy(soundAlertEnabled = sound) }
                if (!sound) {
                    soundJob?.cancel()
                    soundJob = null
                }
            }
        }
        viewModelScope.launch {
            settingsManager.autoSaveHistory.collect { autoSave ->
                _uiState.update { it.copy(autoSaveHistory = autoSave) }
            }
        }
        viewModelScope.launch {
            settingsManager.autoZoneLat.combine(settingsManager.autoZoneLng) { lat, lng ->
                if (lat != null && lng != null) Pair(lat, lng) else null
            }.collect { zone ->
                autoZone = zone
            }
        }
    }

    private var scanJob: Job? = null
    private var soundJob: Job? = null
    private var autoZone: Pair<Float, Float>? = null

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
                        processedDevices.filter { it.isMetaGlasses && !dismissedStalkers.contains(it.address) }.forEach { device ->
                            val historyTimes = deviceDiscoveryHistory.getOrPut(device.address) { mutableListOf() }
                            historyTimes.add(now)
                            
                            // Remove history older than 30 mins
                            historyTimes.removeAll { now - it > 30 * 60 * 1000 }
                            
                            // Stalker Defense Logic: Seen over 15 minutes OR multiple times.
                            // Real app would also check distance > 1km using LocationHelper here
                            if (historyTimes.size > 5 && (now - historyTimes.first() > 15 * 60 * 1000)) {
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
                                    
                                    // Check Auto-Zone
                                    var isInAutoZone = false
                                    if (location != null && autoZone != null) {
                                        val results = FloatArray(1)
                                        android.location.Location.distanceBetween(
                                            location.latitude, location.longitude,
                                            autoZone!!.first.toDouble(), autoZone!!.second.toDouble(),
                                            results
                                        )
                                        if (results[0] <= 100f) { // 100 meters
                                            isInAutoZone = true
                                        }
                                    }

                                    val isZoneMode = state.zoneSecurityMode || isInAutoZone
                                    val isThreat = device.isMetaGlasses || isZoneMode
                                    
                                    val deviceWithLoc = device.copy(
                                        latitude = location?.latitude,
                                        longitude = location?.longitude,
                                        isZoneAlert = isZoneMode && device.isMetaGlasses
                                    )
                                    if (state.autoSaveHistory) {
                                        historyManager.saveDetection(deviceWithLoc)
                                        com.glassesscan.app.util.RealtimeDbManager.syncDetectionToCloud(deviceWithLoc)
                                    }
                                    
                                    if (state.vibrateOnDetect && isThreat) {
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
                        
                        val score = when {
                            it.stalkerDetected != null -> 20
                            metaCount > 0 -> (100 - (metaCount * 15)).coerceIn(30, 70)
                            processedDevices.isNotEmpty() -> 90
                            else -> 100
                        }
                        
                        val status = when {
                            score <= 30 -> "THREAT DETECTED"
                            score <= 75 -> "CAUTION"
                            else -> "SAFE"
                        }

                        if (it.isScanning) {
                            com.glassesscan.app.service.GlassesScannerWidget.updateAllWidgets(
                                context,
                                status,
                                if (metaCount > 0) "$metaCount glasses nearby" else "Environment Safe"
                            )
                        }
                        
                        it.copy(
                            detectedDevices = processedDevices,
                            metaGlassesDetected = hasMeta,
                            safetyScore = score,
                            safetyStatus = status,
                            wifiDirectActive = processedDevices.any { d -> 
                                val name = d.name ?: ""
                                name.contains("DIRECT-", ignoreCase = true) ||
                                name.contains("Meta-", ignoreCase = true) ||
                                name.contains("Cam", ignoreCase = true) ||
                                name.contains("P2P", ignoreCase = true) ||
                                name.contains("WiFi", ignoreCase = true)
                            },
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
        viewModelScope.launch {
            settingsManager.setSoundAlertEnabled(newState)
        }
    }
    
    fun setAutoZone(lat: Double, lng: Double) {
        viewModelScope.launch {
            settingsManager.setAutoZone(lat.toFloat(), lng.toFloat())
        }
    }
    
    fun clearAutoZone() {
        viewModelScope.launch {
            settingsManager.clearAutoZone()
        }
    }

    private fun startSoundJob() {
        if (!_uiState.value.soundAlertEnabled) return
        soundJob?.cancel()
        soundJob = viewModelScope.launch {
            while (isActive && _uiState.value.soundAlertEnabled) {
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
            historyManager.updateDeviceTag(address, tag)
            val updatedTag = if (tag.isBlank()) null else tag
            _uiState.update { state ->
                val updatedDevices = state.detectedDevices.map { 
                    if (it.address == address) it.copy(customTag = updatedTag) else it
                }
                val updatedSelected = state.selectedDevice?.let {
                    if (it.address == address) it.copy(customTag = updatedTag) else it
                }
                state.copy(
                    detectedDevices = updatedDevices,
                    selectedDevice = updatedSelected
                )
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

    private val dismissedStalkers = mutableSetOf<String>()

    fun dismissStalkerAlert() {
        val currentStalker = _uiState.value.stalkerDetected
        if (currentStalker != null) {
            dismissedStalkers.add(currentStalker.address)
            deviceDiscoveryHistory.remove(currentStalker.address)
        }
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

    fun reportAsSmartGlasses(device: DetectedDevice) {
        val dbRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("crowdsource_reports")
        val report = mapOf(
            "address" to device.address,
            "name" to (device.name ?: "Unknown"),
            "timestamp" to System.currentTimeMillis()
        )
        dbRef.child(device.address.replace(":", "")).setValue(report)
        
        val params = android.os.Bundle().apply {
            putString("device_address", device.address)
            putString("device_name", device.name ?: "Unknown")
        }
        com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
            .logEvent("crowdsource_smart_glasses", params)
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
