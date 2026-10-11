package com.glassesscan.app.ui.scanner

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassesscan.app.R
import androidx.compose.material.icons.filled.Warning
import com.glassesscan.app.ui.scanner.components.ArcGaugeComponent
import com.glassesscan.app.ui.scanner.components.DeviceItem
import com.glassesscan.app.ui.scanner.components.MetaAlertCard
import com.glassesscan.app.ui.scanner.components.RadarComponent
import com.glassesscan.app.ui.scanner.components.SignalChartComponent
import com.glassesscan.app.util.AdMobHelper
import com.glassesscan.app.util.findActivity
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import com.glassesscan.app.domain.model.DetectedDevice
import com.glassesscan.app.service.ScannerService
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(viewModel: ScannerViewModel, isPremium: Boolean) {
    val uiState by viewModel.uiState.collectAsState()
    val emergencyAnnouncement by com.glassesscan.app.util.RealtimeDbManager.emergencyAnnouncement.collectAsState()
    val context = LocalContext.current
    
    var showTagDialog by remember { mutableStateOf<String?>(null) }
    var tagText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isScanning) {
        val intent = Intent(context, ScannerService::class.java)
        if (uiState.isScanning) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            context.stopService(intent)
        }
    }

    val proximityColor = remember(uiState.detectedDevices) {
        val closest = uiState.detectedDevices.minByOrNull { it.rssi }
        when (closest?.proximity) {
            DetectedDevice.Proximity.IMMEDIATE -> Color(0xFFFF3B30)
            DetectedDevice.Proximity.NEAR -> Color(0xFF007AFF)
            else -> null
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 12.dp)) {
                        SafetyScoreHeader(uiState.safetyScore, uiState.safetyStatus)
                    }
                },
                title = { 
                    Text(
                        stringResource(R.string.nav_scanner),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = { viewModel.toggleSoundAlert() }) {
                        Icon(
                            imageVector = if (uiState.soundAlertEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = stringResource(R.string.sound_alert),
                            tint = if (uiState.soundAlertEnabled) Color(0xFF007AFF) else Color.Gray
                        )
                    }
                    val zoneDesc = stringResource(R.string.benefit_zone)
                    IconButton(onClick = { 
                        if (isPremium) viewModel.toggleZoneSecurity() 
                        else {
                            Toast.makeText(context, zoneDesc, Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.zoneSecurityMode) Icons.Default.Shield else Icons.Default.ShieldMoon,
                                contentDescription = stringResource(R.string.zone_security),
                                tint = if (uiState.zoneSecurityMode) Color(0xFF34C759) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!isPremium) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp).align(Alignment.BottomEnd),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            AdMobHelper.BannerAdView(isPremium = isPremium)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (!emergencyAnnouncement.isNullOrBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFF3B30).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFFFF3B30).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF3B30))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = emergencyAnnouncement!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFF3B30),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Dashboard Card (iOS Style)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Live Signal Arc Gauge Speedometer
                        val highestMetaRssi = uiState.detectedDevices
                            .filter { it.isMetaGlasses && !uiState.ignoredDevices.contains(it.address) }
                            .maxOfOrNull { it.rssi }
                        ArcGaugeComponent(highestRssi = highestMetaRssi)
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // Radar Container
                        Box(
                            modifier = Modifier.size(240.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            RadarComponent(
                                modifier = Modifier.fillMaxSize(),
                                isScanning = uiState.isScanning,
                                detectedDevices = uiState.detectedDevices,
                                deviceAngles = viewModel.deviceAngles,
                                deviceDisplayRssi = viewModel.deviceDisplayRssi,
                                proximityColor = proximityColor
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))

                        // Control Button - Modern Pill
                        Button(
                            onClick = { 
                                if (uiState.isScanning) {
                                    // Show ad when stopping scan (high profit moment)
                                    if (!isPremium) {
                                        context.findActivity()?.let { act ->
                                            AdMobHelper.showInterstitial(act, minIntervalMs = 120_000L) {
                                                viewModel.stopScanning()
                                            }
                                        } ?: viewModel.stopScanning()
                                    } else {
                                        viewModel.stopScanning()
                                    }
                                } else {
                                    viewModel.startScanning()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(56.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isScanning) Color(0xFFFF3B30) else Color(0xFF007AFF),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 8.dp,
                                hoveredElevation = 6.dp
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.isScanning) Icons.Default.Stop else Icons.Default.Radar,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (uiState.isScanning) stringResource(R.string.stop_scan) else stringResource(R.string.start_scan),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatItemV2("BLE", uiState.detectedDevices.size.toString())
                            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFE5E5EA)))
                            StatItemV2("Wi-Fi", if (uiState.wifiDirectActive) stringResource(R.string.active) else stringResource(R.string.none))
                            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFE5E5EA)))
                            StatItemStat("META", uiState.detectedDevices.count { it.isMetaGlasses }.toString())
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.detections),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            items(uiState.detectedDevices, key = { it.address }) { device ->
                DeviceItem(
                    device = device,
                    onClick = { viewModel.selectDevice(device) }
                )
            }
        }
    }

    // Selected Device Analysis POPUP Dialog
    uiState.selectedDevice?.let { device ->
        AlertDialog(
            onDismissRequest = { viewModel.selectDevice(null) },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = (device.customTag ?: device.name ?: stringResource(R.string.unknown_device)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { 
                        showTagDialog = device.address
                        tagText = device.customTag ?: ""
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.save), modifier = Modifier.size(20.dp))
                    }
                }
            },
            text = {
                Column {
                    Text(device.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.signal_trend), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    
                    val safeRssiHistory = if (device.rssiHistory.size < 2) {
                        listOf(device.rssi - 5, device.rssi)
                    } else device.rssiHistory

                    if (isPremium) {
                        SignalChartComponent(
                            rssiHistory = safeRssiHistory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(vertical = 12.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .padding(vertical = 12.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.premium_feature_chart), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Signal: ${device.rssi} dBm", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("Proximity: ${device.proximity}", style = MaterialTheme.typography.bodySmall, color = if (device.proximity == DetectedDevice.Proximity.IMMEDIATE) Color(0xFFFF3B30) else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isIgnored = uiState.ignoredDevices.contains(device.address)
                        var reported by remember { mutableStateOf(false) }
                        val context = LocalContext.current
                        
                        if (!device.isMetaGlasses && !reported) {
                            TextButton(
                                onClick = { 
                                    viewModel.reportAsSmartGlasses(device)
                                    reported = true
                                    Toast.makeText(context, context.getString(R.string.report_sent), Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Report as Smart Glasses", color = Color(0xFF007AFF), style = MaterialTheme.typography.labelSmall)
                            }
                        } else if (device.isMetaGlasses && !reported) {
                            TextButton(
                                onClick = { 
                                    viewModel.reportFalsePositive(device)
                                    reported = true
                                    Toast.makeText(context, context.getString(R.string.report_sent), Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(stringResource(R.string.not_a_glasses), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        TextButton(
                            onClick = { viewModel.toggleIgnoreDevice(device.address) }
                        ) {
                            Text(if (isIgnored) stringResource(R.string.unignore) else stringResource(R.string.ignore), color = if (isIgnored) Color.Gray else Color(0xFFFF3B30))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.selectDevice(null) }) {
                    Text(stringResource(R.string.close), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Stalker Alert Dialog
    uiState.stalkerDetected?.let { stalker ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissStalkerAlert() },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF3B30)) },
            title = { Text(stringResource(R.string.stalker_alert_title)) },
            text = { 
                Text(stringResource(R.string.stalker_alert_msg, stalker.customTag ?: stalker.name ?: "Unknown", stalker.address))
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissStalkerAlert() }) {
                    Text(stringResource(R.string.i_understand))
                }
            }
        )
    }

    if (showTagDialog != null) {
        AlertDialog(
            onDismissRequest = { showTagDialog = null },
            title = { Text(stringResource(R.string.identify_device)) },
            text = {
                OutlinedTextField(
                    value = tagText,
                    onValueChange = { tagText = it },
                    label = { Text(stringResource(R.string.custom_tag_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateDeviceTag(showTagDialog!!, tagText)
                    showTagDialog = null
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun SafetyScoreHeader(score: Int, status: String) {
    val statusText = when (status) {
        "THREAT DETECTED" -> stringResource(R.string.status_threat)
        "CAUTION" -> stringResource(R.string.status_caution)
        else -> stringResource(R.string.status_safe)
    }
    
    val color = when (status) {
        "THREAT DETECTED" -> Color(0xFFFF3B30)
        "CAUTION" -> Color(0xFFFFCC00)
        else -> Color(0xFF34C759)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$statusText ($score%)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun StatItemV2(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF007AFF)
        )
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            color = Color.Gray,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun StatItemStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF007AFF)
        )
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            color = Color.Gray,
            letterSpacing = 1.sp
        )
    }
}
