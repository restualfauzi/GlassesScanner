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
    val wifiDirectActive: Boolean = false,
    val metaGlassesDetected: Boolean = false,
    val zoneSecurityMode: Boolean = false,
    val selectedDevice: DetectedDevice? = null,
    val errorMessage: String? = null
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
    
    val deviceAngles: Map<String, Float> get() = _deviceAngles
    val deviceDisplayRssi: Map<String, Float> get() = _deviceDisplayRssi

    init {
        viewModelScope.launch {
            settingsManager.zoneSecurityMode.collect { mode ->
                _uiState.update { it.copy(zoneSecurityMode = mode) }
            }
        }
    }

    private var scanJob: Job? = null

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    fun startScanning() {
        if (_uiState.value.isScanning) return

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
                    
                    val processedDevices = devices.map { device ->
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

                    val hasMeta = processedDevices.any { it.isMetaGlasses }

                    processedDevices.forEach { device ->
                        if (canSaveToHistory(device.address, now)) {
                            viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    val location = locationHelper.getLastLocation()
                                    val deviceWithLoc = device.copy(
                                        latitude = location?.latitude,
                                        longitude = location?.longitude,
                                        isZoneAlert = _uiState.value.zoneSecurityMode && device.isMetaGlasses
                                    )
                                    historyManager.saveDetection(deviceWithLoc)
                                    
                                    withContext(Dispatchers.Main) {
                                        lastSavedMap[device.address] = now
                                        if (shouldVibrate && device.isMetaGlasses) {
                                            triggerVibration()
                                        }
                                    }
                                } catch (e: Exception) {}
                            }
                        }
                    }

                    _uiState.update { 
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

    fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
        _uiState.update { it.copy(isScanning = false) }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
