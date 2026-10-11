package com.glassesscan.app.ui.scanner.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glassesscan.app.R
import com.glassesscan.app.domain.model.DetectedDevice

@Composable
fun DeviceItem(device: DetectedDevice, onClick: () -> Unit = {}) {
    val imageRes = device.getDrawableRes()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Devices Icon / Image Thumbnail
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (imageRes != null) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = device.name,
                        modifier = Modifier.size(44.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = if (device.isMetaGlasses) Icons.Default.Face else Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = if (device.isMetaGlasses) MaterialTheme.colorScheme.onSurface else Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.customTag ?: device.name ?: stringResource(R.string.unknown_device),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = device.getCategoryLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Signal Indicator (Blue bars)
                SignalIndicator(rssi = device.rssi)
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SignalIndicator(rssi: Int) {
    val level = when {
        rssi > -60 -> 4
        rssi > -70 -> 3
        rssi > -85 -> 2
        else -> 1
    }
    
    Row(
        modifier = Modifier.height(16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 1..4) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((4 * i).dp)
                    .background(
                        if (i <= level) Color(0xFF007AFF) else Color.LightGray.copy(alpha = 0.5f),
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}
