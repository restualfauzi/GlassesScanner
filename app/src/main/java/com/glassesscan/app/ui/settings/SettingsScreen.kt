package com.glassesscan.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glassesscan.app.R
import com.glassesscan.app.util.AdMobHelper
import com.glassesscan.app.util.findActivity
import com.glassesscan.app.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigate: (Screen) -> Unit
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val vibrateOnDetect by viewModel.vibrateOnDetect.collectAsState()
    val soundAlertEnabled by viewModel.soundAlertEnabled.collectAsState()
    val autoSaveHistory by viewModel.autoSaveHistory.collectAsState()
    val ecoScanMode by viewModel.ecoScanMode.collectAsState()
    val zoneSecurityMode by viewModel.zoneSecurityMode.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val premiumPrice by viewModel.premiumPrice.collectAsState()
    val scanSensitivity by viewModel.scanSensitivity.collectAsState()
    val context = LocalContext.current
    val noEmailError = stringResource(R.string.error_no_email)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.pengaturan_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            AdMobHelper.BannerAdView(isPremium = isPremium)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Premium Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF007AFF), Color(0xFF5856D6))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.premium_pkg),
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(R.string.by_circle_mesh),
                                        color = Color.White.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Benefits List
                            PremiumBenefitItem(stringResource(R.string.benefit_ads))
                            PremiumBenefitItem(stringResource(R.string.benefit_background))
                            PremiumBenefitItem(stringResource(R.string.benefit_zone))
                            PremiumBenefitItem(stringResource(R.string.benefit_chart))
                            PremiumBenefitItem(stringResource(R.string.benefit_history))
                            PremiumBenefitItem(stringResource(R.string.benefit_support))
                            PremiumBenefitItem(stringResource(R.string.benefit_priority))
                            PremiumBenefitItem(stringResource(R.string.benefit_export))
                            PremiumBenefitItem(stringResource(R.string.benefit_unlimited))

                            Spacer(modifier = Modifier.height(20.dp))
                            
                            if (isPremium) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.premium_active),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                val activity = context.findActivity()
                                Button(
                                    onClick = { activity?.let { viewModel.buyPremium(it) } },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF007AFF)
                                    ),
                                    shape = CircleShape,
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 3.dp,
                                        pressedElevation = 6.dp
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = Color(0xFF007AFF)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val buttonText = if (premiumPrice != null) {
                                            "${stringResource(R.string.remove_ads)} ($premiumPrice)"
                                        } else {
                                            stringResource(R.string.remove_ads)
                                        }
                                        Text(
                                            text = buttonText,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Preferences
            item { SectionHeader(stringResource(R.string.preferences_header)) }
            item {
                IOSSettingsItem(
                    title = stringResource(R.string.dark_mode),
                    icon = Icons.Default.DarkMode,
                    iconColor = Color(0xFF5856D6),
                    trailing = {
                        Switch(checked = isDarkMode, onCheckedChange = { viewModel.setDarkMode(it) })
                    }
                )
            }
            item {
                IOSSettingsItem(
                    title = stringResource(R.string.sound_alert_setting),
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    iconColor = Color(0xFF007AFF),
                    trailing = {
                        Switch(checked = soundAlertEnabled, onCheckedChange = { viewModel.setSoundAlertEnabled(it) })
                    }
                )
            }
            item {
                IOSSettingsItem(
                    title = stringResource(R.string.vibrate_on_detect),
                    icon = Icons.Default.Vibration,
                    iconColor = Color(0xFF8E8E93),
                    trailing = {
                        Switch(checked = vibrateOnDetect, onCheckedChange = { viewModel.setVibrate(it) })
                    }
                )
            }
            item {
                IOSSettingsItem(
                    title = stringResource(R.string.auto_save_history_setting),
                    icon = Icons.Default.HistoryToggleOff,
                    iconColor = Color(0xFFFF9500),
                    trailing = {
                        Switch(checked = autoSaveHistory, onCheckedChange = { viewModel.setAutoSaveHistory(it) })
                    }
                )
            }
            item {
                IOSSettingsItem(
                    title = "Account & Cloud Sync",
                    icon = Icons.Default.CloudSync,
                    iconColor = Color(0xFF007AFF),
                    subtitle = com.glassesscan.app.util.AuthManager.getUserEmail(),
                    onClick = { onNavigate(Screen.Login) }
                )
            }
            item {
                IOSSettingsItem(
                    title = "Eco Scan Mode (Battery Saver)",
                    icon = Icons.Default.BatterySaver,
                    iconColor = Color(0xFF34C759),
                    subtitle = "Extends battery by adjusting scan frequency when battery < 20%",
                    trailing = {
                        Switch(checked = ecoScanMode, onCheckedChange = { viewModel.setEcoScanMode(it) })
                    }
                )
            }
            // Zone Security Toggle (Premium Gated)
            item {
                IOSSettingsItem(
                    title = stringResource(R.string.zone_security),
                    icon = Icons.Default.Shield,
                    iconColor = Color(0xFF34C759),
                    trailing = {
                        if (isPremium) {
                            Switch(checked = zoneSecurityMode, onCheckedChange = { viewModel.setZoneSecurityMode(it) })
                        } else {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                )
            }
            item {
                IOSSettingsItem(
                    title = stringResource(R.string.scan_sensitivity),
                    icon = Icons.Default.Tune,
                    iconColor = Color(0xFF34C759),
                    trailing = {
                        Text(
                            text = scanSensitivity,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                val next = when(scanSensitivity) {
                                    "LOW" -> "MEDIUM"
                                    "MEDIUM" -> "HIGH"
                                    else -> "LOW"
                                }
                                viewModel.setScanSensitivity(next)
                            }
                        )
                    }
                )
            }

            // Support
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item { SectionHeader(stringResource(R.string.support_header)) }
            item {
                IOSSettingsItem(
                    title = stringResource(R.string.device_library),
                    icon = Icons.Default.AutoStories,
                    iconColor = Color(0xFF5856D6),
                    subtitle = stringResource(R.string.device_library_subtitle),
                    onClick = { onNavigate(Screen.DeviceLibrary) }
                )
            }
            item {
                IOSSettingsItem(
                    title = stringResource(R.string.rate_app_label),
                    icon = Icons.Default.Star,
                    iconColor = Color(0xFFFFCC00),
                    subtitle = stringResource(R.string.rate_app_subtitle),
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("market://details?id=com.glassesscan.app")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            try {
                                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://play.google.com/store/apps/details?id=com.glassesscan.app")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(webIntent)
                            } catch (_: Exception) {
                                android.widget.Toast.makeText(context, "Could not open Play Store", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            item {
                IOSSettingsItem(
                    title = stringResource(R.string.privacy_audit_label),
                    icon = Icons.Default.AssignmentTurnedIn,
                    iconColor = Color(0xFF34C759),
                    subtitle = stringResource(R.string.privacy_audit_subtitle),
                    onClick = { onNavigate(Screen.Audit) }
                )
            }
            item {
                IOSSettingsItem(
                    title = stringResource(R.string.safety_tips_label),
                    icon = Icons.Default.TipsAndUpdates,
                    iconColor = Color(0xFFFFCC00),
                    onClick = { onNavigate(Screen.SafetyTips) }
                )
            }
            item {
                IOSSettingsItem(
                    title = stringResource(R.string.guide_title),
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    iconColor = Color(0xFF34C759),
                    onClick = { onNavigate(Screen.Guide) }
                )
            }
            item {
                IOSSettingsItem(
                    title = stringResource(R.string.contact_support),
                    icon = Icons.Default.Email,
                    iconColor = Color(0xFFFF9500),
                    subtitle = stringResource(R.string.support_email),
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:support@circlemesh.site")
                                putExtra(Intent.EXTRA_SUBJECT, "Glasses Scanner Support")
                                setPackage("com.google.android.gm") // Prioritize Gmail
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            try {
                                val fallbackIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:support@circlemesh.site")
                                    putExtra(Intent.EXTRA_SUBJECT, "Glasses Scanner Support")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(fallbackIntent)
                            } catch (_: Exception) {
                                android.widget.Toast.makeText(context, noEmailError, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            item {
                IOSSettingsItem(
                    title = stringResource(R.string.privacy_title),
                    icon = Icons.Default.PrivacyTip,
                    iconColor = Color(0xFF8E8E93),
                    onClick = { onNavigate(Screen.PrivacyPolicy) }
                )
            }
            item {
                IOSSettingsItem(
                    title = stringResource(R.string.terms_of_service_label),
                    icon = Icons.Default.Gavel,
                    iconColor = Color(0xFF8E8E93),
                    onClick = { onNavigate(Screen.TermsOfService) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.version_info, com.glassesscan.app.BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(stringResource(R.string.privacy_copyright), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun PremiumBenefitItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CheckCircle, 
            contentDescription = null, 
            tint = Color.White, 
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, color = Color.White, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = Color.Gray,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun IOSSettingsItem(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = RoundedCornerShape(7.dp),
                color = iconColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
            }
        }
    }
}


