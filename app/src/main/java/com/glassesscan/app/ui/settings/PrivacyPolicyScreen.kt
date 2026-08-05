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
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
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
                text = "Privacy Policy for Glasses Scanner",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Last updated: August 5, 2026",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            PrivacyContentSection(
                "1. Data Collection",
                "Glasses Scanner is designed with a 'Privacy-First' philosophy. We do not collect, store, or transmit any personal identification information (PII) to external servers. All scanning data remains strictly on your local device."
            )
            
            PrivacyContentSection(
                "2. Bluetooth & Wi-Fi Usage",
                "The app requires Bluetooth and Wi-Fi permissions to perform real-time signal analysis. This data is used solely to identify the proximity of smart glasses and hidden recording devices. Signal data is never shared with third parties."
            )
            
            PrivacyContentSection(
                "3. Location Data",
                "Android requires 'Fine Location' access to perform Bluetooth Low Energy (BLE) scanning. While the app records the coordinates of detections for your personal 'Map' feature, this data is stored locally in an encrypted format and is never uploaded or tracked by Circle Mesh Limited."
            )
            
            PrivacyContentSection(
                "4. Camera Access",
                "Camera access is only required for the 'AI Visual Scan' feature to help you manually detect recording LEDs. The app does not record, save, or transmit any images or videos from your camera."
            )
            
            PrivacyContentSection(
                "5. Third-Party Services",
                "We use Google AdMob for monetization. AdMob may collect device identifiers to serve personalized ads (unless you are a Premium user). Please refer to Google\\'s Privacy Policy for more details."
            )
            
            PrivacyContentSection(
                "6. Contact Us",
                "If you have any questions regarding this policy or our privacy practices, please contact us at support@circlemesh.site. Developed by Circle Mesh Limited."
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
fun PrivacyContentSection(title: String, body: String) {
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
