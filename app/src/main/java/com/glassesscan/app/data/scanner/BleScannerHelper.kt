package com.glassesscan.app.data.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.glassesscan.app.domain.model.DetectedDevice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class BleScannerHelper(private val context: Context) {

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
        val deviceName = name ?: ""
        
        val smartGlassesKeywords = listOf(
            // Meta / Ray-Ban
            "Ray-Ban Meta", "Meta Glasses", "Ray-Ban Stories", "Meta View", "Ray-Ban", "Meta Wayfarer", "Meta Headliner",
            // AR & Display Glasses
            "XREAL", "Nreal", "Air 2", "Air Pro", "Viture", "Rokid", "Mad Gaze", "INMO", "InMo Air", "RayNeo",
            // Snap
            "Snap Spectacles", "Spectacles", "Spectacles 3", "Spectacles 4",
            // Amazon & Bose
            "Echo Frames", "Amazon Eyewear", "Bose Frames", "Bose Tenor", "Bose Soprano",
            // Huawei & Razer & Lenovo
            "Huawei Eyewear", "Huawei Gentle", "Gentle Monster", "Razer Anzu", "Lenovo Legion", "Lenovo Glasses",
            // AI & Next Gen Glasses
            "Even Realities", "G1 Glasses", "Brilliant Labs", "Frame AI", "Solos", "AirGo", "Lucyd", "Lucyd Lyte", "Superlight",
            // Generic Spy / Camera Glasses
            "Camera Glasses", "Spy Glasses", "Eyewear Cam", "Glasses Cam", "HD Glasses", "Smart Eyewear", "CamGlasses",
            "Action Glasses", "DV Glasses", "BT Glasses", "BT Eyewear", "iVision", "Smart Frame", "Google Glass"
        )
        val containsKeyword = if (deviceName.isNotEmpty()) {
            smartGlassesKeywords.any { deviceName.contains(it, ignoreCase = true) }
        } else false
        
        // Manufacturer Specific Data (IDs registered with Bluetooth SIG)
        val manufacturerData = result.scanRecord?.manufacturerSpecificData
        var hasMatchingManufacturerId = false
        if (manufacturerData != null) {
            for (i in 0 until manufacturerData.size()) {
                val key = manufacturerData.keyAt(i)
                if (key == 0x031B || // Meta Platforms, Inc.
                    key == 0x05B4 || // Nreal / XREAL
                    key == 0x0171 || // Amazon.com Services LLC (Echo Frames)
                    key == 0x009E || // Bose Corporation (Bose Frames)
                    key == 0x0059 || // Apple (Vision Wearable detection)
                    key == 0x004C || // Apple Inc.
                    key == 0x02D3 || // Snap Inc. (Spectacles)
                    key == 0x052B || // Lenovo
                    key == 0x027D || // Huawei Technologies Co., Ltd.
                    key == 0x0006 || // Microsoft (HoloLens)
                    key == 0x01C2 || // Razer Inc.
                    key == 0x0220 || // TCL Communication Ltd. (RayNeo)
                    key == 0x00E0    // Google LLC
                ) {
                    hasMatchingManufacturerId = true
                    break
                }
            }
        }

        return containsKeyword || hasMatchingManufacturerId
    }
}
