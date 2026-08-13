package com.glassesscan.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Scanner", Icons.Default.Home)
    object Settings : Screen("settings", "Pengaturan", Icons.Default.Settings)
    object History : Screen("history", "Riwayat", Icons.Default.History)
    object Map : Screen("map", "Peta", Icons.Default.Map)
    object VisualAI : Screen("visual_ai", "AI Scanner", Icons.Default.RemoveRedEye)
    object Guide : Screen("guide", "Panduan")
    object PrivacyPolicy : Screen("privacy", "Kebijakan Privasi")
    object TermsOfService : Screen("tos", "Ketentuan Layanan")
    object DeviceLibrary : Screen("device_library", "Device Library", Icons.Default.AutoStories)
    object Audit : Screen("audit", "Privacy Audit", Icons.Default.AssignmentTurnedIn)
}
