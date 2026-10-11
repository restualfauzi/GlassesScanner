package com.glassesscan.app.ui.settings

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassesscan.app.R
import com.glassesscan.app.util.AdMobHelper

data class SmartDevice(
    val name: String,
    val brand: String,
    val description: String,
    val specs: String,
    val imageRes: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceLibraryScreen(onBack: () -> Unit, isPremium: Boolean = false) {
    val devices = listOf(
        SmartDevice(
            stringResource(R.string.device_1_name),
            stringResource(R.string.device_1_brand),
            stringResource(R.string.device_1_desc),
            stringResource(R.string.device_1_specs),
            R.drawable.ray_ban_meta
        ),
        SmartDevice(
            stringResource(R.string.device_2_name),
            stringResource(R.string.device_2_brand),
            stringResource(R.string.device_2_desc),
            stringResource(R.string.device_2_specs),
            R.drawable.ray_ban_stories
        ),
        SmartDevice(
            stringResource(R.string.device_3_name),
            stringResource(R.string.device_3_brand),
            stringResource(R.string.device_3_desc),
            stringResource(R.string.device_3_specs),
            R.drawable.xreal_air
        ),
        SmartDevice(
            stringResource(R.string.device_4_name),
            stringResource(R.string.device_4_brand),
            stringResource(R.string.device_4_desc),
            stringResource(R.string.device_4_specs),
            R.drawable.viture_one
        ),
        SmartDevice(
            stringResource(R.string.device_5_name),
            stringResource(R.string.device_5_brand),
            stringResource(R.string.device_5_desc),
            stringResource(R.string.device_5_specs),
            R.drawable.echo_frames
        ),
        SmartDevice(
            stringResource(R.string.device_6_name),
            stringResource(R.string.device_6_brand),
            stringResource(R.string.device_6_desc),
            stringResource(R.string.device_6_specs),
            R.drawable.snap_spectacles
        ),
        SmartDevice(
            stringResource(R.string.device_7_name),
            stringResource(R.string.device_7_brand),
            stringResource(R.string.device_7_desc),
            stringResource(R.string.device_7_specs),
            R.drawable.even_realities_g1
        ),
        SmartDevice(
            stringResource(R.string.device_8_name),
            stringResource(R.string.device_8_brand),
            stringResource(R.string.device_8_desc),
            stringResource(R.string.device_8_specs),
            R.drawable.ar_rokid_max
        ),
        SmartDevice(
            stringResource(R.string.device_9_name),
            stringResource(R.string.device_9_brand),
            stringResource(R.string.device_9_desc),
            stringResource(R.string.device_9_specs),
            R.drawable.huawei_eyewear
        ),
        SmartDevice(
            stringResource(R.string.device_10_name),
            stringResource(R.string.device_10_brand),
            stringResource(R.string.device_10_desc),
            stringResource(R.string.device_10_specs),
            R.drawable.lucyd_lite
        ),
        SmartDevice(
            stringResource(R.string.device_11_name),
            stringResource(R.string.device_11_brand),
            stringResource(R.string.device_11_desc),
            stringResource(R.string.device_11_specs),
            R.drawable.razer_anzu
        ),
        SmartDevice(
            stringResource(R.string.device_12_name),
            stringResource(R.string.device_12_brand),
            stringResource(R.string.device_12_desc),
            stringResource(R.string.device_12_specs),
            R.drawable.frame_ai_glasses
        ),
        SmartDevice(
            stringResource(R.string.device_13_name),
            stringResource(R.string.device_13_brand),
            stringResource(R.string.device_13_desc),
            stringResource(R.string.device_13_specs),
            R.drawable.inmo_air
        ),
        SmartDevice(
            stringResource(R.string.device_14_name),
            stringResource(R.string.device_14_brand),
            stringResource(R.string.device_14_desc),
            stringResource(R.string.device_14_specs),
            R.drawable.spy_hd_camera_glasses
        ),
        SmartDevice(
            stringResource(R.string.device_15_name),
            stringResource(R.string.device_15_brand),
            stringResource(R.string.device_15_desc),
            stringResource(R.string.device_15_specs),
            R.drawable.xreal_air
        ),
        SmartDevice(
            stringResource(R.string.device_16_name),
            stringResource(R.string.device_16_brand),
            stringResource(R.string.device_16_desc),
            stringResource(R.string.device_16_specs),
            R.drawable.echo_frames
        ),
        SmartDevice(
            stringResource(R.string.device_17_name),
            stringResource(R.string.device_17_brand),
            stringResource(R.string.device_17_desc),
            stringResource(R.string.device_17_specs),
            R.drawable.huawei_eyewear
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
        },
        bottomBar = {
            AdMobHelper.BannerAdView(isPremium = isPremium)
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
                    text = stringResource(R.string.device_library_intro),
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
            device.imageRes?.let { imgRes ->
                Image(
                    painter = painterResource(id = imgRes),
                    contentDescription = device.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

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
