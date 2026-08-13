package com.glassesscan.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassesscan.app.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms of Service", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Terms of Service for Glasses Scanner",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Effective Date: August 5, 2026",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LegalSection(
                "1. Acceptance of Terms",
                "By downloading or using Glasses Scanner, you agree to be bound by these terms. If you do not agree, do not use the application."
            )
            
            LegalSection(
                "2. License & Use",
                "We grant you a personal, non-transferable license to use the app for personal, non-commercial purposes. You may not decompile, reverse engineer, or attempt to extract the source code of the app."
            )
            
            LegalSection(
                "3. Disclaimer of Warranties",
                "Glasses Scanner is provided 'AS IS' and 'AS AVAILABLE'. We do not guarantee 100% accuracy in detecting smart glasses. Environmental factors, signal interference, and hardware limitations may affect performance. Use this tool as a secondary aid only."
            )
            
            LegalSection(
                "4. Limitation of Liability",
                "Circle Mesh Limited shall not be liable for any indirect, incidental, or consequential damages resulting from the use or inability to use the application, including but not limited to privacy breaches occurring despite the use of the app."
            )
            
            LegalSection(
                "5. User Responsibility",
                "You are responsible for complying with local laws regarding privacy and surveillance. Glasses Scanner should not be used to harass, stalk, or infringe upon the legal rights of others."
            )
            
            LegalSection(
                "6. Intellectual Property",
                "All app designs, logic, branding, and graphics are the exclusive property of Circle Mesh Limited."
            )
            
            LegalSection(
                "7. Subscription & Payments",
                "Subscriptions are managed through Google Play. Prices are localized and subject to change with notice. You can cancel through your Google Account settings at any time."
            )

            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "© 2026 Circle Mesh Limited. All rights reserved.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
fun LegalSection(title: String, body: String) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}
