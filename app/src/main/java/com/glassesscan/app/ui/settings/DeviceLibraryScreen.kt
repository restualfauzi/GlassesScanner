package com.glassesscan.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassesscan.app.R
import androidx.compose.ui.res.stringResource

data class SmartDevice(
    val name: String,
    val brand: String,
    val description: String,
    val specs: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceLibraryScreen(onBack: () -> Unit) {
    val devices = listOf(
        SmartDevice(
            "Ray-Ban Meta (Gen 2)",
            "Meta / Ray-Ban",
            "The latest smart glasses with ultra-wide 12MP camera, open-ear audio, and AI integration.",
            "BT 5.3, Wi-Fi 6, 12MP Camera"
        ),
        SmartDevice(
            "Ray-Ban Stories",
            "Meta / Ray-Ban",
            "First generation smart glasses for capturing photos and videos directly to social media.",
            "BT 5.0, 5MP Dual Camera"
        ),
        SmartDevice(
            "XREAL Air 2 Ultra",
            "XREAL (Nreal)",
            "High-end AR glasses with spatial computing and 6DoF tracking.",
            "USB-C, Dual 3D Cameras, AR Display"
        ),
        SmartDevice(
            "Viture One",
            "VITURE",
            "XR glasses designed for cloud gaming and premium entertainment experience.",
            "120Hz OLED, Electrochromic film"
        ),
        SmartDevice(
            "Amazon Echo Frames",
            "Amazon",
            "Smart audio eyewear with hands-free Alexa and discreet notifications.",
            "BT 5.2, 4 Micro-speakers"
        ),
        SmartDevice(
            "Snap Spectacles 4",
            "Snapchat",
            "True AR glasses with dual 3D waveguides and built-in spatial audio.",
            "BT 5.0, 2 Cameras, Waveguide display"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.device_library), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "Learn about the most common smart glasses detected by our scanner.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            items(devices) { device ->
                DeviceInfoCard(device)
            }
        }
    }
}

@Composable
fun DeviceInfoCard(device: SmartDevice) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = device.brand.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = device.description,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = device.specs, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }
    }
}
