package com.glassesscan.app.ui.scanner.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SignalChartComponent(
    rssiHistory: List<Int>,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.primary
    
    Canvas(modifier = modifier) {
        if (rssiHistory.size < 2) return@Canvas
        
        val width = size.width
        val height = size.height
        val maxRssi = -40f
        val minRssi = -100f
        val range = maxRssi - minRssi
        
        val stepX = width / (rssiHistory.size - 1)
        val path = Path()
        
        rssiHistory.forEachIndexed { index, rssi ->
            val x = index * stepX
            val normalizedY = (rssi.coerceIn(-100, -40) - minRssi) / range
            val y = height - (normalizedY * height)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
