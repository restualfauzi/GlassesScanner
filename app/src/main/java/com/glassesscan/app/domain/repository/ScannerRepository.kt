package com.glassesscan.app.domain.repository

import com.glassesscan.app.domain.model.DetectedDevice
import kotlinx.coroutines.flow.Flow

interface ScannerRepository {
    fun scanDevices(): Flow<List<DetectedDevice>>
}
