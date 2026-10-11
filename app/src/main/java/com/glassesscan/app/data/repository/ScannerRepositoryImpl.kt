package com.glassesscan.app.data.repository

import com.glassesscan.app.data.scanner.BleScannerHelper
import com.glassesscan.app.data.scanner.WifiScannerHelper
import com.glassesscan.app.domain.model.DetectedDevice
import com.glassesscan.app.domain.repository.ScannerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan

import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class ScannerRepositoryImpl(
    private val bleScannerHelper: BleScannerHelper,
    private val wifiScannerHelper: WifiScannerHelper,
    private val settingsManager: com.glassesscan.app.data.settings.SettingsManager
) : ScannerRepository {

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    override fun scanDevices(): Flow<List<DetectedDevice>> {
        return settingsManager.ecoScanMode.flatMapLatest { isEcoMode ->
            val bleFlow = bleScannerHelper.startScanning(isEcoMode)
            val wifiFlow = wifiScannerHelper.startScanning(isEcoMode)

            merge(bleFlow, wifiFlow)
                .scan(emptyMap<String, DetectedDevice>()) { acc, device ->
                    val updatedMap = acc.toMutableMap()
                    updatedMap[device.address] = device
                    
                    // Cleanup old devices not seen for 12 seconds
                    val now = System.currentTimeMillis()
                    updatedMap.filterValues { now - it.timestamp < 12000 }
                }
                .map { it.values.toList().sortedByDescending { device -> device.rssi } }
        }
    }
}
