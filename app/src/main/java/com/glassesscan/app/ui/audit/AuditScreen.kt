package com.glassesscan.app.ui.audit

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.glassesscan.app.R

data class AuditStep(val title: String, val desc: String, var isDone: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditScreen(onBack: () -> Unit) {
    val steps = remember {
        mutableStateListOf<AuditStep>()
    }

    // Initialize steps with localized strings
    val step1Title = stringResource(R.string.audit_step_1_title)
    val step1Desc = stringResource(R.string.audit_step_1_desc)
    val step2Title = stringResource(R.string.audit_step_2_title)
    val step2Desc = stringResource(R.string.audit_step_2_desc)
    val step3Title = stringResource(R.string.audit_step_3_title)
    val step3Desc = stringResource(R.string.audit_step_3_desc)
    val step4Title = stringResource(R.string.audit_step_4_title)
    val step4Desc = stringResource(R.string.audit_step_4_desc)
    val step5Title = stringResource(R.string.audit_step_5_title)
    val step5Desc = stringResource(R.string.audit_step_5_desc)

    LaunchedEffect(Unit) {
        if (steps.isEmpty()) {
            steps.addAll(listOf(
                AuditStep(step1Title, step1Desc),
                AuditStep(step2Title, step2Desc),
                AuditStep(step3Title, step3Desc),
                AuditStep(step4Title, step4Desc),
                AuditStep(step5Title, step5Desc)
            ))
        }
    }

    val progress = if (steps.isEmpty()) 0f else steps.count { it.isDone }.toFloat() / steps.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_audit_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header Section with Progress
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF34C759).copy(alpha = 0.1f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.audit_progress, (progress * 100).toInt()),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = if (progress == 1f) Color(0xFF34C759) else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Color(0xFF34C759),
                        trackColor = Color.LightGray.copy(alpha = 0.2f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.audit_intro),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(steps) { index, step ->
                    MinimalistAuditRow(step) {
                        steps[index] = step.copy(isDone = !step.isDone)
                    }
                    if (index < steps.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp, top = 8.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun MinimalistAuditRow(step: AuditStep, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = if (step.isDone) Color(0xFF34C759) else Color.Gray.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (step.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (step.isDone) Color.White else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (step.isDone) Color(0xFF34C759) else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = step.desc, 
                style = MaterialTheme.typography.bodySmall, 
                color = Color.Gray,
                lineHeight = 18.sp
            )
        }
    }
}
