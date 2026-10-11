package com.glassesscan.app.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import com.glassesscan.app.R
import com.glassesscan.app.domain.model.DetectedDevice
import com.glassesscan.app.ui.history.HistoryViewModel
import com.glassesscan.app.ui.scanner.ScannerViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    historyViewModel: HistoryViewModel,
    scannerViewModel: ScannerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val history by historyViewModel.history.collectAsState()
    val scannerState by scannerViewModel.uiState.collectAsState()

    var showHeatmap by remember { mutableStateOf(false) }

    // Initialize osmdroid configuration once
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        val basePath = File(context.cacheDir, "osmdroid")
        val tileCache = File(basePath, "tiles")
        Configuration.getInstance().osmdroidBasePath = basePath
        Configuration.getInstance().osmdroidTileCache = tileCache
    }

    // Historical Locations
    val historyLocations = remember(history) {
        history.filter { it.latitude != null && it.longitude != null }
    }

    // Active (Real-time) Locations
    val activeDevices = scannerState.detectedDevices.filter { it.isMetaGlasses }

    val defaultPos = remember(historyLocations) {
        if (historyLocations.isNotEmpty()) {
            GeoPoint(historyLocations[0].latitude!!, historyLocations[0].longitude!!)
        } else {
            GeoPoint(40.7580, -73.9855) // Times Square, New York, USA default
        }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
            controller.setZoom(15.0)
            controller.setCenter(defaultPos)
        }
    }

    // Retain MyLocation overlay so it isn't recreated on every Compose frame
    val myLocationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context.applicationContext), mapView).apply {
            enableMyLocation()
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        if (!mapView.overlays.contains(myLocationOverlay)) {
            mapView.overlays.add(myLocationOverlay)
        }
        onDispose {
            try {
                myLocationOverlay.disableMyLocation()
            } catch (_: Exception) {}
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_map), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val center = mapView.mapCenter
                        scannerViewModel.setAutoZone(center.latitude, center.longitude)
                        android.widget.Toast.makeText(context, "Auto-Zone Security (100m) Set!", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Set Auto-Zone",
                            tint = androidx.compose.ui.graphics.Color(0xFF34C759)
                        )
                    }
                    IconButton(onClick = { showHeatmap = !showHeatmap }) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = stringResource(R.string.heatmap),
                            tint = if (showHeatmap) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clipToBounds(),
            update = { view ->
                // Retain myLocationOverlay and clear only marker/polygon overlays
                view.overlays.retainAll(listOf(myLocationOverlay))

                if (showHeatmap) {
                    // Render Heatmap (Circles around historical points)
                    historyLocations.forEach { device ->
                        val center = GeoPoint(device.latitude!!, device.longitude!!)
                        val circlePoints = Polygon.pointsAsCircle(center, 150.0)
                        val polygon = Polygon().apply {
                            points = circlePoints
                            fillColor = AndroidColor.argb(80, 255, 0, 0)
                            strokeColor = AndroidColor.argb(120, 255, 50, 50)
                            strokeWidth = 2f
                        }
                        view.overlays.add(polygon)
                    }
                } else {
                    // Render Historical Detections (Red Markers)
                    historyLocations.forEach { device ->
                        val pos = GeoPoint(device.latitude!!, device.longitude!!)
                        val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(device.timestamp))
                        val marker = Marker(view).apply {
                            position = pos
                            title = device.customTag ?: device.name ?: view.context.getString(R.string.meta_detected)
                            snippet = view.context.getString(R.string.last_seen, dateStr)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = createColoredMarkerDrawable(view.context, AndroidColor.RED)
                        }
                        view.overlays.add(marker)
                    }
                }

                // Render Global Threats (Orange Markers in New York, USA)
                val globalThreats = listOf(
                    GeoPoint(40.7580, -73.9855), // Times Square, NY
                    GeoPoint(40.7128, -74.0060), // Financial District, NY
                    GeoPoint(40.7829, -73.9654)  // Central Park, NY
                )
                globalThreats.forEachIndexed { index, pos ->
                    val marker = Marker(view).apply {
                        position = pos
                        title = view.context.getString(R.string.global_threat_label, index)
                        snippet = view.context.getString(R.string.global_threat_desc)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        alpha = 0.7f
                        icon = createColoredMarkerDrawable(view.context, AndroidColor.rgb(255, 140, 0))
                    }
                    view.overlays.add(marker)
                }

                // Render Active Real-time Detections (Blue Markers)
                if (activeDevices.isNotEmpty()) {
                    val currentCenter = (view.mapCenter as? GeoPoint) ?: defaultPos
                    activeDevices.forEach { device ->
                        val marker = Marker(view).apply {
                            position = currentCenter
                            title = view.context.getString(R.string.live_label, device.customTag ?: device.name ?: view.context.getString(R.string.unknown_device))
                            snippet = view.context.getString(R.string.rssi_proximity_label, device.rssi, device.proximity)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            alpha = 0.85f
                            icon = createColoredMarkerDrawable(view.context, AndroidColor.rgb(0, 122, 255))
                        }
                        view.overlays.add(marker)
                    }
                }

                view.postInvalidate()
            }
        )
    }
}

private fun createColoredMarkerDrawable(context: Context, colorInt: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (48 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val scale = size / 48f

    // 1. Outer Location Drop Pin
    val pinPath = Path().apply {
        moveTo(24f * scale, 2f * scale)
        cubicTo(14.06f * scale, 2f * scale, 6f * scale, 10.06f * scale, 6f * scale, 20f * scale)
        cubicTo(6f * scale, 31.5f * scale, 24f * scale, 46f * scale, 24f * scale, 46f * scale)
        cubicTo(24f * scale, 46f * scale, 42f * scale, 31.5f * scale, 42f * scale, 20f * scale)
        cubicTo(42f * scale, 10.06f * scale, 33.94f * scale, 2f * scale, 24f * scale, 2f * scale)
        close()
    }
    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt
        style = Paint.Style.FILL
    }
    canvas.drawPath(pinPath, pinPaint)

    // White Border line for pin definition
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * scale
    }
    canvas.drawPath(pinPath, borderPaint)

    // 2. White Circle Badge inside Pin Head
    val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(24f * scale, 19f * scale, 10.5f * scale, circlePaint)

    // 3. Smart Glasses Icon on White Circle
    val glassesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#1E1E1E")
        style = Paint.Style.STROKE
        strokeWidth = 2f * scale
        strokeCap = Paint.Cap.ROUND
    }

    // Left Lens
    val leftRect = RectF(15.5f * scale, 15.5f * scale, 22f * scale, 21.5f * scale)
    canvas.drawRoundRect(leftRect, 2.5f * scale, 2.5f * scale, glassesPaint)

    // Right Lens
    val rightRect = RectF(26f * scale, 15.5f * scale, 32.5f * scale, 21.5f * scale)
    canvas.drawRoundRect(rightRect, 2.5f * scale, 2.5f * scale, glassesPaint)

    // Bridge
    canvas.drawLine(22f * scale, 18f * scale, 26f * scale, 18f * scale, glassesPaint)

    // Left & Right Temples
    canvas.drawLine(15.5f * scale, 17f * scale, 12.5f * scale, 16f * scale, glassesPaint)
    canvas.drawLine(32.5f * scale, 17f * scale, 35.5f * scale, 16f * scale, glassesPaint)

    return BitmapDrawable(context.resources, bitmap)
}
