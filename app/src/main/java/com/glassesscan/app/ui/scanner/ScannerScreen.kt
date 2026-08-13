package com.glassesscan.app.ui.scanner

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.glassesscan.app.ui.scanner.components.DeviceItem
import com.glassesscan.app.ui.scanner.components.MetaAlertCard
import com.glassesscan.app.ui.scanner.components.RadarComponent
import com.glassesscan.app.ui.scanner.components.SignalChartComponent
import com.glassesscan.app.util.AdMobHelper
import com.glassesscan.app.util.BannerAdView

import android.content.Intent
import com.glassesscan.app.domain.model.DetectedDevice
import com.glassesscan.app.service.ScannerService
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(viewModel: ScannerViewModel, isPremium: Boolean) {
    val uiState by viewModel.uiState.collectAsState()
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
                title = { 
                    Text(
                        "Scanner",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = { viewModel.toggleSoundAlert() }) {
                        Icon(
                            imageVector = if (uiState.soundAlertEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Sound Alert",
                            tint = if (uiState.soundAlertEnabled) Color(0xFF007AFF) else Color.Gray
                        )
                    }
                    IconButton(onClick = { 
                        if (isPremium) viewModel.toggleZoneSecurity() 
                        else {
                            // Toast or redirect
                        }
                    }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.zoneSecurityMode) Icons.Default.Shield else Icons.Default.ShieldMoon,
                                contentDescription = "Zone Security",
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
            BannerAdView(isPremium = isPremium)
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

                        // Control Button - Red Pill
                        Button(
                            onClick = { 
                                if (uiState.isScanning) {
                                    // Show ad when stopping scan (high profit moment)
                                    if (!isPremium) {
                                        (context as? android.app.Activity)?.let { act ->
                                            AdMobHelper.showInterstitial(act) {
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
                                .fillMaxWidth(0.8f)
                                .height(56.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isScanning) Color(0xFFFF3B30) else Color(0xFF007AFF),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(Color.White)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (uiState.isScanning) "STOP" else "START",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
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
                            StatItemV2("Wi-Fi", if (uiState.wifiDirectActive) "ACTIVE" else "NONE")
                            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFE5E5EA)))
                            StatItemStat("META", uiState.detectedDevices.count { it.isMetaGlasses }.toString())
                        }
                    }
                }
            }

            item {
                Text(
                    text = "DETECTIONS",
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
            
            // Selected Device Analysis Card (Premium Gated)
            uiState.selectedDevice?.let { device ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = (device.customTag ?: device.name ?: "Unknown").uppercase(),
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
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.signal_trend), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            
                            if (isPremium) {
                                SignalChartComponent(
                                    rssiHistory = device.rssiHistory,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .padding(vertical = 12.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .padding(vertical = 12.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Premium Feature: Signal Chart", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                }
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Current: ${device.rssi} dBm", style = MaterialTheme.typography.bodySmall)
                                Text("Proximity: ${device.proximity}", style = MaterialTheme.typography.bodySmall, color = if (device.proximity == DetectedDevice.Proximity.IMMEDIATE) Color.Red else Color.Gray)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isIgnored = uiState.ignoredDevices.contains(device.address)
                                var reported by remember { mutableStateOf(false) }
                                
                                if (!reported) {
                                    TextButton(
                                        onClick = { 
                                            viewModel.reportFalsePositive(device)
                                            reported = true
                                        }
                                    ) {
                                        Text(stringResource(R.string.not_a_glasses), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                TextButton(
                                    onClick = { viewModel.toggleIgnoreDevice(device.address) }
                                ) {
                                    Text(if (isIgnored) "UNIGNORE" else "IGNORE", color = if (isIgnored) Color.Gray else Color(0xFFFF3B30))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = { viewModel.selectDevice(null) }
                                ) {
                                    Text("CLOSE")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Stalker Alert Dialog
    uiState.stalkerDetected?.let { stalker ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissStalkerAlert() },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF3B30)) },
            title = { Text("STALKER ALERT") },
            text = { 
                Text("Device '${stalker.customTag ?: stalker.name ?: "Unknown"}' (${stalker.address}) has been following you for more than 5 minutes. Exercise caution.")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissStalkerAlert() }) {
                    Text("I UNDERSTAND")
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
                    label = { Text("Custom Name/Tag") },
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
