package com.glassesscan.app

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.glassesscan.app.ui.history.HistoryScreen
import com.glassesscan.app.ui.history.HistoryViewModel
import com.glassesscan.app.ui.history.HistoryViewModelFactory
import com.glassesscan.app.ui.map.MapScreen
import com.glassesscan.app.ui.navigation.Screen
import com.glassesscan.app.ui.permissions.PermissionsHandler
import com.glassesscan.app.ui.scanner.ScannerScreen
import com.glassesscan.app.ui.scanner.ScannerViewModel
import com.glassesscan.app.ui.scanner.ScannerViewModelFactory
import com.glassesscan.app.ui.visual.VisualAIScreen
import com.glassesscan.app.ui.onboarding.WelcomeScreen
import com.glassesscan.app.ui.audit.AuditScreen
import com.glassesscan.app.ui.settings.*
import com.glassesscan.app.ui.theme.GlassesScanTheme
import com.glassesscan.app.util.*
import com.google.android.play.core.review.ReviewManagerFactory

class MainActivity : ComponentActivity() {
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Allow screenshots & screen recording
        SecurityGuard.allowScreenCapture(window)
        if (SecurityGuard.isPackageTampered(this) || SecurityGuard.isHookedOrTampered()) {
            finishAffinity()
            return
        }
        
        val settingsManager = com.glassesscan.app.data.settings.SettingsManager(this)
        billingManager = BillingManager(this, settingsManager)
        
        // Real-Time Control Listeners for Firebase
        RealtimeDbManager.listenForUserPremiumControl(settingsManager)
        RealtimeDbManager.listenForGlobalAppConfig(settingsManager)
        
        // Show App Open Ad
        (application as? GlassesScanApp)?.showAppOpenAd(this)
        
        // Load Interstitial and Rewarded for later use
        AdMobHelper.loadInterstitial(this)
        AdMobHelper.loadRewarded(this)

        // Google Play Review API
        val reviewManager = ReviewManagerFactory.create(this)
        
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(this, billingManager),
            )
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
            val isPremium by settingsViewModel.isPremium.collectAsState()
            val hasCompletedOnboarding by settingsViewModel.hasCompletedOnboarding.collectAsState()

            val networkObserver = remember { NetworkObserver(applicationContext) }
            val isConnected by networkObserver.isConnected.collectAsState(initial = true)

            LaunchedEffect(Unit) {
                // Request review flow safely
                try {
                    val request = reviewManager.requestReviewFlow()
                    request.addOnCompleteListener { task ->
                        if ((task.isSuccessful) && (task.result != null)) {
                            try {
                                reviewManager.launchReviewFlow(this@MainActivity, task.result)
                            } catch (_: Exception) {
                                // Ignore review flow error
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Ignore review flow error
                }
            }

            val themeDark = isDarkMode
            GlassesScanTheme(darkTheme = themeDark) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (!hasCompletedOnboarding) {
                            WelcomeScreen {
                                settingsViewModel.setOnboardingCompleted(completed = true)
                            }
                        } else {
                            PermissionsHandler {
                                Box(modifier = Modifier.safeDrawingPadding()) {
                                    MainNavigation(settingsViewModel, isPremium, intent)
                                }
                            }
                        }
                    }

                    // 100% Online Internet Monitoring Overlay
                    NoInternetOverlay(visible = !isConnected)
                }
            }
        }
    }
}

@Composable
fun NoInternetOverlay(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(250)) + scaleOut(targetScale = 0.92f, animationSpec = tween(250))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .pointerInput(Unit) { /* Block touches */ },
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .padding(28.dp)
                    .fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(28.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFF3B30).copy(alpha = 0.12f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFFF3B30), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "OFFLINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF3B30)
                            )
                        }
                    }

                    // Icon Container
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "No Internet Connection",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Glasses Scanner requires an active internet connection to detect signatures and update threat maps.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Reconnecting Progress Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Waiting for network...",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainNavigation(settingsViewModel: SettingsViewModel, isPremium: Boolean, intent: android.content.Intent?) {
    val navController = rememberNavController()
    val viewModel: ScannerViewModel = viewModel(
        factory = ScannerViewModelFactory(androidx.compose.ui.platform.LocalContext.current),
    )
    
    // Handle App Shortcuts
    LaunchedEffect(intent) {
        intent?.data?.let { data ->
            when (data.toString()) {
                "shortcut://radar" -> {
                    navController.navigate(Screen.Home.route)
                    viewModel.startScanning()
                }
                "shortcut://visual" -> {
                    navController.navigate(Screen.VisualAI.route)
                }
            }
        }
    }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val items = listOf(
        Screen.Home,
        Screen.History,
        Screen.Map,
        Screen.VisualAI,
        Screen.Settings,
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            val showBottomBar = currentDestination?.route in listOf(Screen.Home.route, Screen.History.route, Screen.Map.route, Screen.VisualAI.route, Screen.Settings.route)
            
            if (showBottomBar) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 0.dp,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                    ) {
                        items.forEach { screen ->
                            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            NavigationBarItem(
                                icon = { 
                                    Box {
                                        screen.icon?.let { Icon(it, contentDescription = null) }
                                        val isMap = screen == Screen.Map
                                        val notPremium = !isPremium
                                        if (isMap && notPremium) {
                                            Icon(
                                                Icons.Default.Lock, 
                                                contentDescription = null,
                                                modifier = Modifier.size(10.dp).align(Alignment.BottomEnd),
                                                tint = Color(0xFF007AFF)
                                            )
                                        }
                                    }
                                },
                                label = { Text(
                                    text = when(screen) {
                                        Screen.Home -> stringResource(R.string.nav_scanner)
                                        Screen.History -> stringResource(R.string.nav_history)
                                        Screen.Map -> stringResource(R.string.nav_map)
                                        Screen.VisualAI -> stringResource(R.string.nav_visual)
                                        Screen.Settings -> stringResource(R.string.nav_settings)
                                        else -> ""
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ) },
                                selected = isSelected,
                                onClick = {
                                    val isMapScreen = screen == Screen.Map
                                    val needsPremium = !isPremium
                                    
                                    if (isMapScreen && needsPremium) {
                                        activity?.let { act ->
                                            AlertDialog.Builder(act)
                                                .setTitle(act.getString(R.string.premium_pkg))
                                                .setMessage("Watch a short video to unlock the Map feature for this session?")
                                                .setPositiveButton("Watch Ad") { _, _ ->
                                                    AdMobHelper.showRewarded(act) {
                                                        navController.navigate(Screen.Map.route)
                                                    }
                                                }
                                                .setNegativeButton("Upgrade") { _, _ ->
                                                    navController.navigate(Screen.Settings.route)
                                                }
                                                .show()
                                        }
                                        return@NavigationBarItem
                                    }
                                    
                                    val isFreeUser = !isPremium
                                    if (isFreeUser) {
                                        activity?.let {
                                            AdMobHelper.showInterstitialOnTabSwitch(it) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        } ?: run {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    } else {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF007AFF),
                                    unselectedIconColor = Color(0xFF8E8E93),
                                    selectedTextColor = Color(0xFF007AFF),
                                    unselectedTextColor = Color(0xFF8E8E93),
                                    indicatorColor = Color.Transparent,
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val startDest = if (com.glassesscan.app.util.AuthManager.isUserLoggedIn()) Screen.Home.route else Screen.Login.route

        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                ScannerScreen(viewModel = viewModel, isPremium = isPremium)
            }
            composable(Screen.History.route) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModelFactory(context)
                )
                HistoryScreen(viewModel = historyViewModel, isPremium = isPremium)
            }
            composable(Screen.Map.route) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModelFactory(context)
                )
                MapScreen(
                    historyViewModel = historyViewModel,
                    scannerViewModel = viewModel,
                ) {
                    navController.popBackStack()
                }
            }
            composable(Screen.VisualAI.route) {
                VisualAIScreen(onBack = { navController.popBackStack() }, isPremium = isPremium)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                ) { screen ->
                    navController.navigate(screen.route)
                }
            }
            composable(Screen.Guide.route) {
                GuideScreen { navController.popBackStack() }
            }
            composable(Screen.PrivacyPolicy.route) {
                PrivacyPolicyScreen { navController.popBackStack() }
            }
            composable(Screen.TermsOfService.route) {
                TermsOfServiceScreen { navController.popBackStack() }
            }
            composable(Screen.DeviceLibrary.route) {
                DeviceLibraryScreen(onBack = { navController.popBackStack() }, isPremium = isPremium)
            }
            composable(Screen.Audit.route) {
                AuditScreen { navController.popBackStack() }
            }
            composable(Screen.SafetyTips.route) {
                SafetyTipsScreen { navController.popBackStack() }
            }
            composable(Screen.Login.route) {
                val isNewUser = !com.glassesscan.app.util.AuthManager.isUserLoggedIn()
                com.glassesscan.app.ui.auth.LoginScreen(
                    showBackButton = !isNewUser,
                    onBack = { 
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    },
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
