package com.glassesscan.app.ui.map

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.glassesscan.app.ui.history.HistoryViewModel
import com.glassesscan.app.ui.scanner.ScannerViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.compose.ui.res.stringResource
import com.glassesscan.app.R

import com.google.maps.android.heatmaps.HeatmapTileProvider
import androidx.compose.material.icons.filled.Layers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    historyViewModel: HistoryViewModel,
    scannerViewModel: ScannerViewModel,
    onBack: () -> Unit
) {
    val history by historyViewModel.history.collectAsState()
    val scannerState by scannerViewModel.uiState.collectAsState()
    
    var showHeatmap by remember { mutableStateOf(false) }

    // Historical Locations
    val historyLocations = remember(history) {
        history.filter { it.latitude != null && it.longitude != null }
    }

    val heatmapProvider = remember(historyLocations) {
        if (historyLocations.isNotEmpty()) {
            val points = historyLocations.map { LatLng(it.latitude!!, it.longitude!!) }
            HeatmapTileProvider.Builder()
                .data(points)
                .radius(50)
                .build()
        } else null
    }

    // Active (Real-time) Locations - Based on current phone position
    val activeDevices = scannerState.detectedDevices.filter { it.isMetaGlasses }

    val defaultPos = if (historyLocations.isNotEmpty()) {
        LatLng(historyLocations[0].latitude!!, historyLocations[0].longitude!!)
    } else {
        LatLng(-6.200000, 106.816666) // Jakarta default
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPos, 15f)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.nav_map), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHeatmap = !showHeatmap }) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Heatmap",
                            tint = if (showHeatmap) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        GoogleMap(
            modifier = Modifier.fillMaxSize().padding(padding),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true)
        ) {
            if (showHeatmap && heatmapProvider != null) {
                TileOverlay(tileProvider = heatmapProvider)
            } else {
                // Render Historical Detections (Red Markers)
                historyLocations.forEach { device ->
                    key(device.address + device.timestamp) {
                        val pos = LatLng(device.latitude!!, device.longitude!!)
                        Marker(
                            state = rememberMarkerState(position = pos),
                            title = device.customTag ?: device.name ?: "Meta Glasses",
                            snippet = "Last seen: ${java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(device.timestamp))}",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                        )
                    }
                }
            }

            // Render Global Threats (Simulated aggregated data from other users)
            val globalThreats = listOf(
                LatLng(-6.2146, 106.8451),
                LatLng(-6.1751, 106.8272),
                LatLng(-6.2088, 106.8456)
            )
            globalThreats.forEachIndexed { index, pos ->
                Marker(
                    state = rememberMarkerState(position = pos),
                    title = "GLOBAL THREAT #$index",
                    snippet = "Detected by community in last 24h",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                    alpha = 0.6f
                )
            }

            // Render Active Real-time Detections
            if (activeDevices.isNotEmpty()) {
                val currentLoc = cameraPositionState.position.target
                activeDevices.forEach { device ->
                    key("live_" + device.address) {
                        Marker(
                            state = rememberMarkerState(position = currentLoc),
                            title = "LIVE: ${device.customTag ?: device.name}",
                            snippet = "RSSI: ${device.rssi} dBm (${device.proximity})",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            alpha = 0.8f
                        )
                    }
                }
            }
        }
    }
}
