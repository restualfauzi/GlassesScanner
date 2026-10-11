package com.glassesscan.app.data.repository

import com.glassesscan.app.data.scanner.BleScannerHelper
import com.glassesscan.app.data.scanner.WifiScannerHelper
import com.glassesscan.app.domain.model.DetectedDevice
import com.glassesscan.app.domain.repository.ScannerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan

class ScannerRepositoryImpl(
    private val bleScannerHelper: BleScannerHelper,
    private val wifiScannerHelper: WifiScannerHelper
) : ScannerRepository {

    override fun scanDevices(): Flow<List<DetectedDevice>> {
        val bleFlow = bleScannerHelper.startScanning()
        val wifiFlow = wifiScannerHelper.startScanning()

        return merge(bleFlow, wifiFlow)
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
