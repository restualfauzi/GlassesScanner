package com.glassesscan.app.ui.scanner.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SignalChartComponent(
    rssiHistory: List<Int>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = Color.LightGray.copy(alpha = 0.3f)
    val safeHistory = if (rssiHistory.isEmpty()) {
        listOf(-90, -80)
    } else if (rssiHistory.size == 1) {
        listOf(rssiHistory[0] - 5, rssiHistory[0])
    } else {
        rssiHistory
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val maxRssi = -40f
        val minRssi = -100f
        val range = maxRssi - minRssi

        // 1. Draw horizontal grid lines (-50 dBm, -70 dBm, -90 dBm)
        val gridLevels = listOf(-50f, -70f, -90f)
        gridLevels.forEach { level ->
            val gridY = height - (((level - minRssi) / range) * height)
            drawLine(
                color = gridColor,
                start = Offset(0f, gridY),
                end = Offset(width, gridY),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 2. Build Path for RSSI Signal Curve
        val stepX = width / (safeHistory.size - 1)
        val path = Path()
        val fillPath = Path()

        safeHistory.forEachIndexed { index, rssi ->
            val x = index * stepX
            val normalizedY = (rssi.coerceIn(-100, -40) - minRssi) / range
            val y = height - (normalizedY * height)

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            if (index == safeHistory.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        // 3. Draw Filled Area Under Line
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0.05f))
            )
        )

        // 4. Draw Signal Line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx())
        )

        // 5. Draw Data Dots
        safeHistory.forEachIndexed { index, rssi ->
            val x = index * stepX
            val normalizedY = (rssi.coerceIn(-100, -40) - minRssi) / range
            val y = height - (normalizedY * height)

            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = lineColor,
                radius = 2.5.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}
