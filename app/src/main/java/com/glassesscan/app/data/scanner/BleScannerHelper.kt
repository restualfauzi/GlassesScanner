package com.glassesscan.app.data.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.glassesscan.app.domain.model.DetectedDevice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class BleScannerHelper(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter = bluetoothManager.adapter

    @SuppressLint("MissingPermission")
    fun startScanning(): Flow<DetectedDevice> = callbackFlow {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        
        if (adapter == null || !adapter.isEnabled) {
            channel.close(java.lang.RuntimeException("BLE_NOT_AVAILABLE"))
            return@callbackFlow
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            channel.close(java.lang.RuntimeException("BLE_NOT_AVAILABLE"))
            return@callbackFlow
        }

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val rssi = result.rssi
                val name = device.name ?: result.scanRecord?.deviceName
                
                // Filtering Logic for Meta / Ray-Ban
                val isMeta = isMetaGlasses(name, result)

                trySend(
                    DetectedDevice(
                        address = device.address,
                        name = name,
                        rssi = rssi,
                        isMetaGlasses = isMeta
                    )
                )
            }

            override fun onScanFailed(errorCode: Int) {
                close(Exception("Scan failed with error code: $errorCode"))
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // We could add filters here, but for a "Radar" feel, we scan all and filter in logic
        try {
            scanner.startScan(null, settings, scanCallback)
        } catch (e: Exception) {
            channel.close(e)
            return@callbackFlow
        }

        awaitClose {
            try {
                scanner.stopScan(scanCallback)
            } catch (e: Exception) {}
        }
    }

    private fun isMetaGlasses(name: String?, result: ScanResult): Boolean {
        if (name == null) return false
        
        val metaKeywords = listOf("Ray-Ban Meta", "Meta Glasses", "Ray-Ban Stories")
        val containsKeyword = metaKeywords.any { name.contains(it, ignoreCase = true) }
        
        // Manufacturer Specific Data for Meta (Example ID: 0x0059 is Apple, 0x031B is Meta Platforms Inc)
        // Meta Platforms, Inc. Manufacturer ID is 0x031B (795 decimal)
        val manufacturerData = result.scanRecord?.manufacturerSpecificData
        var hasMetaManufacturerId = false
        if (manufacturerData != null) {
            for (i in 0 until manufacturerData.size()) {
                if (manufacturerData.keyAt(i) == 0x031B) {
                    hasMetaManufacturerId = true
                    break
                }
            }
        }

        return containsKeyword || hasMetaManufacturerId
    }
}
