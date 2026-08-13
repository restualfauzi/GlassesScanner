package com.glassesscan.app.ui.audit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AuditStep(val title: String, val desc: String, var isDone: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditScreen(onBack: () -> Unit) {
    val steps = remember {
        mutableStateListOf(
            AuditStep("Environmental Radar Scan", "Run a 60-second real-time scan to identify surrounding BLE signals."),
            AuditStep("Visual LED Inspection", "Use AI Scan to check for tiny recording indicators on suspicious frames."),
            AuditStep("Network Sniffing", "Check Wi-Fi Direct activity for high-bandwidth data transmission."),
            AuditStep("Proximity Audit", "Identify the exact distance level of the strongest signal."),
            AuditStep("Tag Known Devices", "Filter out friendly devices to isolate potential threats.")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Audit Checklist", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            item {
                Text(
                    "Follow these expert steps when entering a new sensitive area to ensure maximum privacy protection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            itemsIndexed(steps) { index, step ->
                AuditCard(step) {
                    steps[index] = step.copy(isDone = !step.isDone)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
                val progress = steps.count { it.isDone }.toFloat() / steps.size
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color(0xFF34C759),
                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                )
                Text(
                    text = "Audit Progress: ${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp),
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun AuditCard(step: AuditStep, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (step.isDone) Color(0xFF34C759).copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        ),
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (step.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (step.isDone) Color(0xFF34C759) else Color.Gray
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (step.isDone) Color(0xFF34C759) else MaterialTheme.colorScheme.onSurface
                )
                Text(text = step.desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
