package com.glassesscan.app.ui.history

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassesscan.app.R
import com.glassesscan.app.domain.model.DetectedDevice
import com.glassesscan.app.ui.scanner.components.SignalIndicator
import com.glassesscan.app.util.AdMobHelper
import java.text.SimpleDateFormat
import java.util.*

enum class HistoryFilter {
    ALL,
    SMART_GLASSES,
    SUSPICIOUS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel, isPremium: Boolean) {
    val history by viewModel.history.collectAsState()
    val context = LocalContext.current
    val premiumRequiredMsg = stringResource(R.string.premium_required_export)
    
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var showTagDialog by remember { mutableStateOf<String?>(null) }
    var tagText by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }

    val smartGlassesCount = remember(history) {
        history.count { it.isMetaGlasses || it.getDrawableRes() != null || it.getCategoryLabel() != "BLE Device" }
    }

    val suspiciousCount = remember(history) {
        history.count { it.isZoneAlert || it.isMetaGlasses || it.proximity == DetectedDevice.Proximity.IMMEDIATE || it.rssi > -70 }
    }

    val filteredHistory = remember(history, selectedFilter) {
        when (selectedFilter) {
            HistoryFilter.ALL -> history
            HistoryFilter.SMART_GLASSES -> history.filter { 
                it.isMetaGlasses || it.getDrawableRes() != null || it.getCategoryLabel() != "BLE Device" 
            }
            HistoryFilter.SUSPICIOUS -> history.filter { 
                it.isZoneAlert || it.isMetaGlasses || it.proximity == DetectedDevice.Proximity.IMMEDIATE || it.rssi > -70 
            }
        }
    }

    val groupedHistory = remember(filteredHistory) {
        filteredHistory.groupBy { 
            val date = Date(it.timestamp)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.format(date)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.nav_history),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            AdMobHelper.BannerAdView(isPremium = isPremium)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Top Header with Clear All & Export
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.last_meta_detection),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        letterSpacing = 0.5.sp
                    )
                    
                    if (history.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(
                                onClick = { 
                                    if (isPremium) viewModel.exportHistoryToCsv(context)
                                    else {
                                        Toast.makeText(context, premiumRequiredMsg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isPremium) Icons.Default.FileDownload else Icons.Default.Lock, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.export), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))

                            FilledTonalButton(
                                onClick = { showClearConfirm = true },
                                shape = CircleShape,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.clear_all), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // iOS Style Segmented Control Filter Row with Dynamic Counts
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(9.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val filters = listOf(
                            HistoryFilter.ALL to "${stringResource(R.string.filter_all)} (${history.size})",
                            HistoryFilter.SMART_GLASSES to "${stringResource(R.string.filter_smart_glasses)} ($smartGlassesCount)",
                            HistoryFilter.SUSPICIOUS to "${stringResource(R.string.filter_suspicious)} ($suspiciousCount)"
                        )

                        filters.forEach { (filter, label) ->
                            val isSelected = selectedFilter == filter
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clickable { selectedFilter = filter },
                                shape = RoundedCornerShape(7.dp),
                                color = if (isSelected) {
                                    if (filter == HistoryFilter.SUSPICIOUS) Color(0xFFFF3B30) else MaterialTheme.colorScheme.surface
                                } else Color.Transparent,
                                shadowElevation = if (isSelected) 2.dp else 0.dp
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        if (filter == HistoryFilter.SUSPICIOUS) {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = if (isSelected) Color.White else Color(0xFFFF3B30)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        } else if (filter == HistoryFilter.SMART_GLASSES) {
                                            Icon(
                                                Icons.Default.Face,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) {
                                                if (filter == HistoryFilter.SUSPICIOUS) Color.White else MaterialTheme.colorScheme.onSurface
                                            } else Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Analytics Dashboard Section
            item {
                if (history.isNotEmpty()) {
                    val totalDetections = history.size
                    val totalMeta = history.count { it.isMetaGlasses }
                    
                    // Most active hour
                    val hourCounts = history.groupBy { 
                        val cal = java.util.Calendar.getInstance()
                        cal.timeInMillis = it.timestamp
                        cal.get(java.util.Calendar.HOUR_OF_DAY)
                    }.mapValues { it.value.size }
                    
                    val peakHour = hourCounts.maxByOrNull { it.value }?.key ?: 0
                    val peakHourStr = String.format("%02d:00", peakHour)
                    
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) {
                        Text(
                            text = "Analytics Dashboard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Total Scan", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                    Text("$totalDetections", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Smart Glasses", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                    Text("$totalMeta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFFF3B30))
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Peak Time", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                    Text(peakHourStr, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                if (!isPremium) {
                    AdMobHelper.NativeAdView(isPremium = false)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (filteredHistory.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(bottom = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val emptyMsg = when (selectedFilter) {
                            HistoryFilter.SMART_GLASSES -> "No smart glasses in history"
                            HistoryFilter.SUSPICIOUS -> "No suspicious threats in history"
                            else -> stringResource(R.string.no_history)
                        }
                        Text(text = emptyMsg, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                groupedHistory.forEach { (dateKey, devices) ->
                    item {
                        Text(
                            text = formatDateHeader(dateKey, context),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                        )
                    }
                    
                    items(devices) { device ->
                        HistoryItem(
                            device = device,
                            onClick = {
                                showTagDialog = device.address
                                tagText = device.customTag ?: ""
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Footer Info
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Info, 
                        contentDescription = null, 
                        tint = Color.Gray, 
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.history_local_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF3B30)) },
            title = { Text(stringResource(R.string.clear_history_title)) },
            text = { Text(stringResource(R.string.clear_history_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF3B30))
                ) {
                    Text(stringResource(R.string.clear_all), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.cancel))
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
fun HistoryItem(device: DetectedDevice, onClick: () -> Unit) {
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
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(device.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
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

private fun formatDateHeader(dateKey: String, context: Context): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val date = sdf.parse(dateKey) ?: return dateKey
    val now = Calendar.getInstance()
    val headerDate = Calendar.getInstance().apply { time = date }
    
    return when {
        now.get(Calendar.YEAR) == headerDate.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == headerDate.get(Calendar.DAY_OF_YEAR) -> context.getString(R.string.today) + ", " + SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date)
        
        now.get(Calendar.YEAR) == headerDate.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) - 1 == headerDate.get(Calendar.DAY_OF_YEAR) -> context.getString(R.string.yesterday) + ", " + SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date)
        
        else -> SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date)
    }
}
