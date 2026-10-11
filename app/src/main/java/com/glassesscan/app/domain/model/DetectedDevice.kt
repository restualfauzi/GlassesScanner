package com.glassesscan.app.domain.model

import com.glassesscan.app.R
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
    val isZoneAlert: Boolean = false,
    val isHighBandwidth: Boolean = false
) {
    val proximity: Proximity
        get() = calculateProximity(rssi)

    val estimatedDistance: Double
        get() = calculateDistance(rssi)

    fun getCategoryLabel(): String {
        val deviceName = name ?: ""
        val lower = deviceName.lowercase()
        return when {
            lower.contains("ray-ban meta") || lower.contains("meta wayfarer") || lower.contains("meta headliner") || lower.contains("meta view") || lower.contains("orion") -> "Smart Glasses (Ray-Ban Meta)"
            lower.contains("ray-ban stories") -> "Smart Glasses (Ray-Ban)"
            lower.contains("xreal") || lower.contains("nreal") -> "AR Smart Glasses (XREAL)"
            lower.contains("viture") -> "XR Smart Glasses (Viture)"
            lower.contains("rayneo") || lower.contains("tcl") -> "AR Smart Glasses (RayNeo)"
            lower.contains("echo frames") || lower.contains("amazon") -> "Smart Audio Glasses (Echo)"
            lower.contains("solos") || lower.contains("airgo") -> "AI Smart Glasses (Solos)"
            lower.contains("spectacles") || lower.contains("snap") -> "AR Smart Glasses (Snap)"
            lower.contains("even realities") || lower.contains("g1") -> "AI Smart Glasses (Even Realities)"
            lower.contains("rokid") -> "AR Smart Glasses (Rokid)"
            lower.contains("huawei") || lower.contains("gentle") -> "Smart Audio Glasses (Huawei)"
            lower.contains("lucyd") -> "Smart Glasses (Lucyd)"
            lower.contains("bose") || lower.contains("tenor") || lower.contains("soprano") -> "Smart Audio Glasses (Bose)"
            lower.contains("razer") || lower.contains("anzu") -> "Smart Audio Glasses (Razer)"
            lower.contains("frame ai") || lower.contains("brilliant labs") -> "AI Camera Glasses (Frame)"
            lower.contains("inmo") -> "AR Smart Glasses (InMo)"
            lower.contains("spy") || lower.contains("camera glasses") || lower.contains("eyewear cam") -> "Spy Camera Glasses"
            isMetaGlasses -> "Smart Glasses"
            else -> "BLE Device"
        }
    }

    fun getDrawableRes(): Int? {
        val deviceName = name ?: ""
        val lower = deviceName.lowercase()
        return when {
            lower.contains("ray-ban meta") || lower.contains("meta wayfarer") || lower.contains("meta headliner") -> R.drawable.ray_ban_meta
            lower.contains("ray-ban stories") -> R.drawable.ray_ban_stories
            lower.contains("xreal") || lower.contains("nreal") -> R.drawable.xreal_air
            lower.contains("viture") -> R.drawable.viture_one
            lower.contains("echo frames") || lower.contains("amazon") -> R.drawable.echo_frames
            lower.contains("spectacles") || lower.contains("snap") -> R.drawable.snap_spectacles
            lower.contains("even realities") || lower.contains("g1") -> R.drawable.even_realities_g1
            lower.contains("rokid") -> R.drawable.ar_rokid_max
            lower.contains("huawei") || lower.contains("gentle") -> R.drawable.huawei_eyewear
            lower.contains("lucyd") -> R.drawable.lucyd_lite
            lower.contains("razer") || lower.contains("anzu") -> R.drawable.razer_anzu
            lower.contains("frame ai") || lower.contains("brilliant labs") -> R.drawable.frame_ai_glasses
            lower.contains("inmo") -> R.drawable.inmo_air
            lower.contains("spy") || lower.contains("camera glasses") || lower.contains("eyewear cam") -> R.drawable.spy_hd_camera_glasses
            isMetaGlasses || lower.contains("meta") || lower.contains("ray-ban") -> R.drawable.ray_ban_meta
            else -> null
        }
    }

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
