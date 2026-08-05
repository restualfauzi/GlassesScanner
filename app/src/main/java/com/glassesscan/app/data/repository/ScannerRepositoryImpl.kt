package com.glassesscan.app.data.repository

import com.glassesscan.app.data.scanner.BleScannerHelper
import com.glassesscan.app.domain.model.DetectedDevice
import com.glassesscan.app.domain.repository.ScannerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

class ScannerRepositoryImpl(
    private val bleScannerHelper: BleScannerHelper
) : ScannerRepository {

    // Store devices in a map to update RSSI/Timestamp and avoid duplicates
    private val deviceCache = mutableMapOf<String, DetectedDevice>()

    override fun scanDevices(): Flow<List<DetectedDevice>> {
        return bleScannerHelper.startScanning()
            .scan(emptyMap<String, DetectedDevice>()) { acc, device ->
                // Update or add device
                val updatedMap = acc.toMutableMap()
                updatedMap[device.address] = device
                
                // Cleanup old devices (e.g., not seen for more than 10 seconds)
                val now = System.currentTimeMillis()
                updatedMap.filterValues { now - it.timestamp < 10000 }
            }
            .map { it.values.toList().sortedByDescending { device -> device.rssi } }
    }
}
