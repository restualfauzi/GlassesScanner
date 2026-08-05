package com.glassesscan.app.ui.scanner.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

import com.glassesscan.app.domain.model.DetectedDevice
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarComponent(
    modifier: Modifier = Modifier,
    isScanning: Boolean = true,
    detectedDevices: List<DetectedDevice> = emptyList(),
    deviceAngles: Map<String, Float> = emptyMap(),
    deviceDisplayRssi: Map<String, Float> = emptyMap(),
    proximityColor: Color? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val color = proximityColor ?: Color(0xFF007AFF)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f
            if (maxRadius <= 0f) return@Canvas

            // Circular grid - Blueish
            for (i in 1..4) {
                drawCircle(
                    color = Color(0xFFE3F2FD),
                    radius = maxRadius * (i / 4f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Sweep animation
            if (isScanning) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to color.copy(alpha = 0f),
                        0.5f to color.copy(alpha = 0.2f),
                        1f to color.copy(alpha = 0f),
                        center = centerOffset
                    ),
                    startAngle = rotation,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset(centerOffset.x - maxRadius, centerOffset.y - maxRadius),
                    size = androidx.compose.ui.geometry.Size(maxRadius * 2, maxRadius * 2)
                )
            }

            // Center anchor
            drawCircle(
                color = color,
                radius = 6.dp.toPx(),
                center = centerOffset
            )
            
            // Draw Real-time Targets (All Detected Devices)
            detectedDevices.forEach { device ->
                val angle = deviceAngles[device.address] ?: 0f
                val rssi = deviceDisplayRssi[device.address] ?: device.rssi.toFloat()
                
                // Map RSSI (-100 to -40) to distance ratio (0.1 to 0.9)
                val rssiRatio = ((rssi.coerceIn(-100f, -40f) + 100) / 60f)
                val distanceRatio = (1.0f - rssiRatio).coerceIn(0.1f, 0.9f)
                val radius = maxRadius * distanceRatio
                
                val rad = Math.toRadians(angle.toDouble())
                val x = centerOffset.x + (radius * cos(rad)).toFloat()
                val y = centerOffset.y + (radius * sin(rad)).toFloat()
                
                drawCircle(
                    color = if (device.isMetaGlasses) color else Color.LightGray,
                    radius = 5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}
