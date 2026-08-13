package com.glassesscan.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.sp
import com.glassesscan.app.R
import com.glassesscan.app.ui.navigation.Screen
import com.glassesscan.app.util.BannerAdView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigate: (Screen) -> Unit
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val vibrateOnDetect by viewModel.vibrateOnDetect.collectAsState()
    val zoneSecurityMode by viewModel.zoneSecurityMode.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val premiumPrice by viewModel.premiumPrice.collectAsState()
    val currentLang by viewModel.languageCode.collectAsState()
    val scanSensitivity by viewModel.scanSensitivity.collectAsState()
    val context = LocalContext.current
    val noEmailError = stringResource(R.string.error_no_email)

    var showLangDialog by remember { mutableStateOf(false) }

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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            BannerAdView(isPremium = isPremium)
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
                                val activity = context as? android.app.Activity
                                Button(
                                    onClick = { activity?.let { viewModel.buyPremium(it) } },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF007AFF)),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val buttonText = if (premiumPrice != null) {
                                        "${stringResource(R.string.remove_ads)} ($premiumPrice)"
                                    } else {
                                        stringResource(R.string.remove_ads)
                                    }
                                    Text(text = buttonText.uppercase(), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Preferences
            item { SectionHeader("PREFERENCES") }
            item {
                iOSSettingsItem(
                    title = stringResource(R.string.dark_mode),
                    icon = Icons.Default.DarkMode,
                    iconColor = Color(0xFF5856D6),
                    trailing = {
                        Switch(checked = isDarkMode, onCheckedChange = { viewModel.setDarkMode(it) })
                    }
                )
            }
            item {
                iOSSettingsItem(
                    title = stringResource(R.string.vibrate_on_detect),
                    icon = Icons.Default.Vibration,
                    iconColor = Color(0xFF8E8E93),
                    trailing = {
                        Switch(checked = vibrateOnDetect, onCheckedChange = { viewModel.setVibrate(it) })
                    }
                )
            }
            // Zone Security Toggle (Premium Gated)
            item {
                iOSSettingsItem(
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
                iOSSettingsItem(
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
            item {
                iOSSettingsItem(
                    title = stringResource(R.string.language),
                    icon = Icons.Default.Language,
                    iconColor = Color(0xFF007AFF),
                    subtitle = getLanguageName(currentLang),
                    onClick = { showLangDialog = true }
                )
            }

            // Support
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item { SectionHeader("SUPPORT") }
            item {
                iOSSettingsItem(
                    title = stringResource(R.string.device_library),
                    icon = Icons.Default.AutoStories,
                    iconColor = Color(0xFF5856D6),
                    subtitle = "Learn about smart eyewear",
                    onClick = { onNavigate(Screen.DeviceLibrary) }
                )
            }
            item {
                iOSSettingsItem(
                    title = "Rate App",
                    icon = Icons.Default.Star,
                    iconColor = Color(0xFFFFCC00),
                    subtitle = "Give us 5 stars on Play Store",
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("market://details?id=com.glassesscan.app")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://play.google.com/store/apps/details?id=com.glassesscan.app")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(webIntent)
                            } catch (e2: Exception) {
                                android.widget.Toast.makeText(context, "Could not open Play Store", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            item {
                iOSSettingsItem(
                    title = "Privacy Audit",
                    icon = Icons.Default.AssignmentTurnedIn,
                    iconColor = Color(0xFF34C759),
                    subtitle = "Complete your security check",
                    onClick = { onNavigate(Screen.Audit) }
                )
            }
            item {
                iOSSettingsItem(
                    title = stringResource(R.string.guide_title),
                    icon = Icons.Default.HelpOutline,
                    iconColor = Color(0xFF34C759),
                    onClick = { onNavigate(Screen.Guide) }
                )
            }
            item {
                iOSSettingsItem(
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
                        } catch (e: Exception) {
                            try {
                                val fallbackIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:support@circlemesh.site")
                                    putExtra(Intent.EXTRA_SUBJECT, "Glasses Scanner Support")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(fallbackIntent)
                            } catch (e2: Exception) {
                                android.widget.Toast.makeText(context, noEmailError, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            item {
                iOSSettingsItem(
                    title = stringResource(R.string.privacy_title),
                    icon = Icons.Default.PrivacyTip,
                    iconColor = Color(0xFF8E8E93),
                    onClick = { onNavigate(Screen.PrivacyPolicy) }
                )
            }
            item {
                iOSSettingsItem(
                    title = "Terms of Service",
                    icon = Icons.Default.Gavel,
                    iconColor = Color(0xFF8E8E93),
                    onClick = { onNavigate(Screen.TermsOfService) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("VERSION 1.0.2", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("© 2026 Circle Mesh Limited", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }
    }

    if (showLangDialog) {
        AlertDialog(
            onDismissRequest = { showLangDialog = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column {
                    LanguageOption("en", "English", currentLang) { viewModel.setLanguage("en"); showLangDialog = false }
                    LanguageOption("id", "Indonesia", currentLang) { viewModel.setLanguage("id"); showLangDialog = false }
                    LanguageOption("zh", "Chinese", currentLang) { viewModel.setLanguage("zh"); showLangDialog = false }
                    LanguageOption("es", "Español", currentLang) { viewModel.setLanguage("es"); showLangDialog = false }
                    LanguageOption("fr", "Français", currentLang) { viewModel.setLanguage("fr"); showLangDialog = false }
                    LanguageOption("de", "Deutsch", currentLang) { viewModel.setLanguage("de"); showLangDialog = false }
                    LanguageOption("ja", "日本語", currentLang) { viewModel.setLanguage("ja"); showLangDialog = false }
                }
            },
            confirmButton = {}
        )
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
fun iOSSettingsItem(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = MaterialTheme.shapes.small,
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

@Composable
fun LanguageOption(code: String, name: String, current: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = code == current, onClick = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = name)
    }
}

private fun getLanguageName(code: String) = when(code) {
    "en" -> "English"
    "id" -> "Indonesia"
    "zh" -> "Chinese"
    "es" -> "Español"
    "fr" -> "Français"
    "de" -> "Deutsch"
    "ja" -> "日本語"
    else -> "English"
}
