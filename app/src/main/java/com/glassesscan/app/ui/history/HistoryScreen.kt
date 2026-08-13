package com.glassesscan.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassesscan.app.R
import com.glassesscan.app.domain.model.DetectedDevice
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.platform.LocalContext
import com.glassesscan.app.ui.scanner.components.SignalIndicator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel, isPremium: Boolean) {
    val history by viewModel.history.collectAsState()
    val context = LocalContext.current
    
    var showTagDialog by remember { mutableStateOf<String?>(null) }
    var tagText by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }

    val groupedHistory = remember(history) {
        history.groupBy { 
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
                        "History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
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
            // Top Header with Clear All
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LAST META DETECTIONS",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        letterSpacing = 0.5.sp
                    )
                    
                    if (history.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { 
                                    if (isPremium) viewModel.exportHistoryToCsv(context)
                                    else {
                                        // Show premium required toast or similar
                                        android.widget.Toast.makeText(context, "Premium Required for Export", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isPremium) Icons.Default.FileDownload else Icons.Default.Lock, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))

                            TextButton(
                                onClick = { showClearConfirm = true },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear All", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }

            if (history.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.no_history), color = Color.Gray)
                    }
                }
            } else {
                groupedHistory.forEach { (dateKey, devices) ->
                    item {
                        Text(
                            text = formatDateHeader(dateKey),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
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
                        text = "History is stored locally on your device.\nIt will be cleared if you uninstall the app.",
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
            title = { Text("Clear History?") },
            text = { Text("Are you sure you want to delete all detection records? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF3B30))
                ) {
                    Text("Clear All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
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
fun HistoryItem(device: DetectedDevice, onClick: () -> Unit) {
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
                Icon(
                    imageVector = if (device.isMetaGlasses) Icons.Default.Face else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = if (device.isMetaGlasses) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
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
                    text = device.address,
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

private fun formatDateHeader(dateKey: String): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val date = sdf.parse(dateKey) ?: return dateKey
    val now = Calendar.getInstance()
    val headerDate = Calendar.getInstance().apply { time = date }
    
    return when {
        now.get(Calendar.YEAR) == headerDate.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == headerDate.get(Calendar.DAY_OF_YEAR) -> "Today, " + SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date)
        
        now.get(Calendar.YEAR) == headerDate.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) - 1 == headerDate.get(Calendar.DAY_OF_YEAR) -> "Yesterday, " + SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date)
        
        else -> SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date)
    }
}
