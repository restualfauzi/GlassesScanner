package com.glassesscan.app.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.pow

@Serializable
data class DetectedDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isMetaGlasses: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val customTag: String? = null,
    val rssiHistory: List<Int> = emptyList(),
    val isZoneAlert: Boolean = false
) {
    val proximity: Proximity
        get() = calculateProximity(rssi)

    val estimatedDistance: Double
        get() = calculateDistance(rssi)

    @Serializable
    enum class Proximity {
        IMMEDIATE, // Very close
        NEAR,      // Within a few meters
        FAR        // Signal is weak
    }

    private fun calculateDistance(rssi: Int): Double {
        // Simple log-distance path loss model
        // d = 10^((Measured Power - RSSI) / (10 * N))
        // Measured Power is the RSSI at 1 meter. N is path-loss exponent (typically 2-4)
        val measuredPower = -59 // common average
        val n = 2.5
        return 10.0.pow((measuredPower - rssi) / (10.0 * n))
    }

    private fun calculateProximity(rssi: Int): Proximity {
        return when {
            rssi > -60 -> Proximity.IMMEDIATE
            rssi > -80 -> Proximity.NEAR
            else -> Proximity.FAR
        }
    }
}
