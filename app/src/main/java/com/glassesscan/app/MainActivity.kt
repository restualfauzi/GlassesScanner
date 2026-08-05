package com.glassesscan.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
import com.glassesscan.app.ui.settings.*
import com.glassesscan.app.ui.theme.GlassesScanTheme
import com.glassesscan.app.util.*

class MainActivity : ComponentActivity() {
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val settingsManager = com.glassesscan.app.data.settings.SettingsManager(this)
        billingManager = BillingManager(this, settingsManager)
        
        // Show App Open Ad
        (application as? GlassesScanApp)?.showAppOpenAd(this)
        
        // Load Interstitial for later use
        AdMobHelper.loadInterstitial(this)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(this, billingManager)
            )
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
            val languageCode by settingsViewModel.languageCode.collectAsState()

            // Handle language change safely
            val context = androidx.compose.ui.platform.LocalContext.current
            
            // 1. Create a wrapped context that forces the locale for all Compose children
            val localizedContext = remember(languageCode) {
                LocaleUtils.wrap(context, languageCode)
            }

            LaunchedEffect(languageCode) {
                val locale = if (languageCode == "zh") java.util.Locale.CHINESE else java.util.Locale(languageCode)
                
                // Compatibility for API < 24
                val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.resources.configuration.locales[0]
                } else {
                    @Suppress("DEPRECATION")
                    context.resources.configuration.locale
                }
                
                if (currentLocale?.language != locale.language) {
                    java.util.Locale.setDefault(locale)
                    
                    // Update System-wide Application Locales (Android 13+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.getSystemService(android.app.LocaleManager::class.java).applicationLocales = 
                            android.os.LocaleList(locale)
                    }

                    (context as? android.app.Activity)?.recreate()
                }
            }

            PermissionsHandler {
                CompositionLocalProvider(androidx.compose.ui.platform.LocalContext provides localizedContext) {
                    val themeDark = isDarkMode
                    val isPremium by settingsViewModel.isPremium.collectAsState()
                    
                    GlassesScanTheme(darkTheme = themeDark) {
                        Surface(
                            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            MainNavigation(settingsViewModel, isPremium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainNavigation(settingsViewModel: SettingsViewModel, isPremium: Boolean) {
    val navController = rememberNavController()
    val viewModel: ScannerViewModel = viewModel(
        factory = ScannerViewModelFactory(androidx.compose.ui.platform.LocalContext.current)
    )
    
    val context = androidx.compose.ui.platform.LocalContext.current

    val items = listOf(
        Screen.Home,
        Screen.History,
        Screen.Map,
        Screen.VisualAI,
        Screen.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            val showBottomBar = currentDestination?.route in listOf(Screen.Home.route, Screen.History.route, Screen.Map.route, Screen.VisualAI.route, Screen.Settings.route)
            
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp // iOS style flat
                ) {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { 
                                Box {
                                    screen.icon?.let { Icon(it, contentDescription = null) }
                                    if (screen == Screen.Map && !isPremium) {
                                        Icon(
                                            Icons.Default.Lock, 
                                            contentDescription = null,
                                            modifier = Modifier.size(10.dp).align(Alignment.BottomEnd),
                                            tint = MaterialTheme.colorScheme.primary
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
                                fontSize = 10.sp
                            ) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                if (screen == Screen.Map && !isPremium) {
                                    navController.navigate(Screen.Settings.route)
                                    return@NavigationBarItem
                                }
                                
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.secondary,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
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
                HistoryScreen(viewModel = historyViewModel)
            }
            composable(Screen.Map.route) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModelFactory(context)
                )
                MapScreen(
                    historyViewModel = historyViewModel,
                    scannerViewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.VisualAI.route) {
                VisualAIScreen(onBack = { navController.popBackStack() }, isPremium = isPremium)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigate = { screen ->
                        navController.navigate(screen.route)
                    }
                )
            }
            composable(Screen.Guide.route) {
                GuideScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.PrivacyPolicy.route) {
                PrivacyPolicyScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
