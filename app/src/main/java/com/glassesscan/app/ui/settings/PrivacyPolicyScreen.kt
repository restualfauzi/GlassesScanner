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
                text = stringResource(R.string.privacy_policy_main_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.privacy_last_updated),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            PrivacyContentSection(
                stringResource(R.string.privacy_section_1_title),
                stringResource(R.string.privacy_section_1_desc)
            )
            
            PrivacyContentSection(
                stringResource(R.string.privacy_section_2_title),
                stringResource(R.string.privacy_section_2_desc)
            )
            
            PrivacyContentSection(
                stringResource(R.string.privacy_section_3_title),
                stringResource(R.string.privacy_section_3_desc)
            )
            
            PrivacyContentSection(
                stringResource(R.string.privacy_section_4_title),
                stringResource(R.string.privacy_section_4_desc)
            )
            
            PrivacyContentSection(
                stringResource(R.string.privacy_section_5_title),
                stringResource(R.string.privacy_section_5_desc)
            )
            
            PrivacyContentSection(
                stringResource(R.string.privacy_section_6_title),
                stringResource(R.string.privacy_section_6_desc)
            )
            
            PrivacyContentSection(
                stringResource(R.string.privacy_section_7_title),
                stringResource(R.string.privacy_section_7_desc)
            )

            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = stringResource(R.string.privacy_copyright),
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
