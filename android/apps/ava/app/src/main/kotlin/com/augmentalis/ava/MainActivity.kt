// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/MainActivity.kt
// created: 2025-11-02 15:32:00 -0800
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.augmentalis.ava.crashreporting.CrashReporter
import com.augmentalis.ava.features.chat.ui.ChatScreen
import com.augmentalis.ava.features.chat.ui.ChatViewModel
import com.augmentalis.ava.features.teach.TeachAvaScreen
import com.augmentalis.ava.features.teach.TeachAvaViewModel
import com.augmentalis.ava.preferences.ThemeMode
import com.augmentalis.ava.ui.components.StoragePermissionBanner
import com.augmentalis.ava.ui.components.StoragePermissionDialog
import com.augmentalis.ava.ui.settings.ModelDownloadScreen
import com.augmentalis.ava.ui.settings.SettingsScreenResponsive
import com.augmentalis.ava.ui.settings.SettingsViewModel
import com.augmentalis.ava.ui.testing.TestLauncherScreen
import com.augmentalis.ava.ui.testing.TestLauncherViewModel
import com.augmentalis.ava.ui.theme.AvaTheme
import com.augmentalis.ava.ui.components.AvaCommandOverlayWrapper
import com.augmentalis.ava.util.StoragePermissionHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * Main Activity for AVA AI application
 *
 * Entry point for the standalone Android app, providing:
 * - Bottom navigation between Chat, Teach AVA, and Settings
 * - Integration of all feature modules
 * - Material 3 design system
 *
 * @author Manoj Jhawar
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        /** Broadcast intent action for wake word detection */
        const val ACTION_WAKE_WORD_DETECTED = "com.augmentalis.ava.WAKE_WORD_DETECTED"

        /** SharedFlow for wake word events - observed by ChatViewModel */
        val wakeWordEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)

        /** StateFlow for storage permission status */
        private val _storagePermissionGranted = MutableStateFlow(false)
        val storagePermissionGranted: StateFlow<Boolean> = _storagePermissionGranted
    }

    /** Handler for storage permission requests */
    private lateinit var storagePermissionHandler: StoragePermissionHandler

    /**
     * BroadcastReceiver for wake word detection from WakeWordService.
     * When wake word is detected, emits event to wakeWordEvents flow
     * which ChatViewModel observes to trigger voice input.
     */
    private val wakeWordReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_WAKE_WORD_DETECTED) {
                val keyword = intent.getStringExtra("keyword") ?: "AVA"
                Timber.i("Wake word detected: $keyword - triggering voice input")
                wakeWordEvents.tryEmit(keyword)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        installSplashScreen()

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize storage permission handler (must be before setContent)
        storagePermissionHandler = StoragePermissionHandler(this) { granted ->
            _storagePermissionGranted.value = granted
            Timber.i("Storage permission ${if (granted) "granted" else "denied"}")
        }

        // Check initial permission state
        _storagePermissionGranted.value = storagePermissionHandler.hasStoragePermission()

        // Register wake word broadcast receiver
        registerWakeWordReceiver()

        Timber.d("MainActivity created, storage permission: ${_storagePermissionGranted.value}")

        // Initialize crash reporting (skip in tests)
        val app = application as? AvaApplication
        if (app != null) {
            val crashReportingEnabled = runBlocking {
                app.userPreferences.crashReportingEnabled.first()
            }
            CrashReporter.initialize(this, crashReportingEnabled)
        } else {
            // Running in test environment with HiltTestApplication
            Timber.d("Running in test mode, skipping crash reporter initialization")
        }

        setContent {
            // Observe theme preferences from UserPreferences (or use default in tests)
            val themeMode by if (app != null) {
                app.userPreferences.themeMode.collectAsStateWithLifecycle(initialValue = "auto")
            } else {
                // Test mode: use system default
                remember { mutableStateOf("auto") }
            }

            val useDynamicColor by if (app != null) {
                app.userPreferences.useDynamicColor.collectAsStateWithLifecycle(initialValue = false)
            } else {
                remember { mutableStateOf(false) }
            }

            val systemInDarkTheme = isSystemInDarkTheme()

            // Determine dark theme based on preference
            val darkTheme = when (ThemeMode.fromString(themeMode)) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AUTO -> systemInDarkTheme
            }

            Timber.v("Theme mode: $themeMode, darkTheme: $darkTheme, dynamicColor: $useDynamicColor, system: $systemInDarkTheme")

            AvaTheme(
                darkTheme = darkTheme,
                dynamicColor = useDynamicColor
            ) {
                AvaApp(
                    hasStoragePermission = _storagePermissionGranted.collectAsStateWithLifecycle().value,
                    onRequestStoragePermission = { storagePermissionHandler.requestStoragePermission() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister wake word receiver to prevent memory leaks
        try {
            unregisterReceiver(wakeWordReceiver)
            Timber.d("Wake word receiver unregistered")
        } catch (e: IllegalArgumentException) {
            // Receiver not registered - ignore
            Timber.w("Wake word receiver was not registered")
        }
    }

    /**
     * Register broadcast receiver for wake word detection.
     * Uses RECEIVER_NOT_EXPORTED flag on Android 13+ for security.
     */
    private fun registerWakeWordReceiver() {
        val filter = IntentFilter(ACTION_WAKE_WORD_DETECTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wakeWordReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(wakeWordReceiver, filter)
        }
        Timber.d("Wake word receiver registered for action: $ACTION_WAKE_WORD_DETECTED")
    }
}

@Composable
fun AvaApp(
    hasStoragePermission: Boolean = true,
    onRequestStoragePermission: () -> Unit = {}
) {
    val navController = rememberNavController()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // State for showing permission dialog
    var showPermissionDialog by remember { mutableStateOf(!hasStoragePermission) }

    // Update dialog state when permission changes
    LaunchedEffect(hasStoragePermission) {
        if (hasStoragePermission) {
            showPermissionDialog = false
        }
    }

    // Ocean Blue theme colors - WebAvanue style
    val OceanGradientStart = Color(0xFF0A1929)
    val OceanGradientMid = Color(0xFF0F172A)
    val OceanGradientEnd = Color(0xFF1E293B)
    val GlassBackground = Color.White.copy(alpha = 0.08f)
    val GlassBorder = Color.White.copy(alpha = 0.15f)
    val IconColor = Color.White.copy(alpha = 0.9f)
    val SelectedIconColor = Color.White

    // Navigation state
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Navigation item click handler
    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Show permission dialog on first launch
    if (showPermissionDialog) {
        StoragePermissionDialog(
            onRequestPermission = {
                showPermissionDialog = false
                onRequestStoragePermission()
            },
            onDismiss = { showPermissionDialog = false }
        )
    }

    // Ocean gradient background for entire app
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(OceanGradientStart, OceanGradientMid, OceanGradientEnd)
                )
            )
    ) {
        // Wrap entire app with command overlay
        // Pass current route so FAB can be hidden on chat screen (mic is in input row)
        AvaCommandOverlayWrapper(
            navController = navController,
            currentRoute = currentDestination?.route
        ) { onTriggerVoiceOverlay ->
            if (isLandscape) {
                // LANDSCAPE: WebAvanue-style glass card navigation on left
                Row(modifier = Modifier.fillMaxSize()) {
                    // Navigation Column - transparent, centered vertically
                    Column(
                        modifier = Modifier
                            .width(56.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Navigation items - compact, no background
                        GlassNavItem(
                            icon = Icons.Outlined.Chat,
                            label = "Chat",
                            selected = currentDestination?.hierarchy?.any { it.route == "chat" } == true,
                            onClick = { navigateTo("chat") },
                            glassBackground = Color.Transparent,
                            glassBorder = Color.Transparent,
                            iconColor = IconColor,
                            selectedIconColor = SelectedIconColor
                        )

                        Spacer(Modifier.height(12.dp))

                        GlassNavItem(
                            icon = Icons.Outlined.School,
                            label = "Teach",
                            selected = currentDestination?.hierarchy?.any { it.route == "teach" } == true,
                            onClick = { navigateTo("teach") },
                            glassBackground = Color.Transparent,
                            glassBorder = Color.Transparent,
                            iconColor = IconColor,
                            selectedIconColor = SelectedIconColor
                        )

                        Spacer(Modifier.height(12.dp))

                        GlassNavItem(
                            icon = Icons.Outlined.Settings,
                            label = "Settings",
                            selected = currentDestination?.hierarchy?.any { it.route == "settings" } == true,
                            onClick = { navigateTo("settings") },
                            glassBackground = Color.Transparent,
                            glassBorder = Color.Transparent,
                            iconColor = IconColor,
                            selectedIconColor = SelectedIconColor
                        )
                    }

                    // Content area
                    NavHost(
                        navController = navController,
                        startDestination = "chat",
                        modifier = Modifier.weight(1f)
                    ) {
                        composable("chat") { ChatScreenWrapper(onVoiceInput = onTriggerVoiceOverlay) }
                        composable("teach") { TeachAvaScreenWrapper() }
                        composable("settings") {
                            SettingsScreenWrapper(
                                onNavigateToModelDownload = { navController.navigate("model_download") },
                                onNavigateToTestLauncher = { navController.navigate("test_launcher") }
                            )
                        }
                        composable("model_download") {
                            ModelDownloadScreenWrapper(onNavigateBack = { navController.popBackStack() })
                        }
                        composable("test_launcher") {
                            TestLauncherScreenWrapper(onNavigateBack = { navController.popBackStack() })
                        }
                    }
                }
            } else {
                // PORTRAIT: Glass-style NavigationBar at bottom
                Scaffold(
                    containerColor = Color.Transparent, // Transparent to show Ocean gradient
                    bottomBar = {
                        // Glass navigation bar - WebAvanue style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            GlassNavItem(
                                icon = Icons.Outlined.Chat,
                                label = "Chat",
                                selected = currentDestination?.hierarchy?.any { it.route == "chat" } == true,
                                onClick = { navigateTo("chat") },
                                glassBackground = GlassBackground,
                                glassBorder = GlassBorder,
                                iconColor = IconColor,
                                selectedIconColor = SelectedIconColor
                            )

                            GlassNavItem(
                                icon = Icons.Outlined.School,
                                label = "Teach",
                                selected = currentDestination?.hierarchy?.any { it.route == "teach" } == true,
                                onClick = { navigateTo("teach") },
                                glassBackground = GlassBackground,
                                glassBorder = GlassBorder,
                                iconColor = IconColor,
                                selectedIconColor = SelectedIconColor
                            )

                            GlassNavItem(
                                icon = Icons.Outlined.Settings,
                                label = "Settings",
                                selected = currentDestination?.hierarchy?.any { it.route == "settings" } == true,
                                onClick = { navigateTo("settings") },
                                glassBackground = GlassBackground,
                                glassBorder = GlassBorder,
                                iconColor = IconColor,
                                selectedIconColor = SelectedIconColor
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "chat",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("chat") { ChatScreenWrapper(onVoiceInput = onTriggerVoiceOverlay) }
                        composable("teach") { TeachAvaScreenWrapper() }
                        composable("settings") {
                            SettingsScreenWrapper(
                                onNavigateToModelDownload = { navController.navigate("model_download") },
                                onNavigateToTestLauncher = { navController.navigate("test_launcher") }
                            )
                        }
                        composable("model_download") {
                            ModelDownloadScreenWrapper(onNavigateBack = { navController.popBackStack() })
                        }
                        composable("test_launcher") {
                            TestLauncherScreenWrapper(onNavigateBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact Navigation Item - minimal, transparent
 */
@Composable
private fun GlassNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    glassBackground: Color,
    glassBorder: Color,
    iconColor: Color,
    selectedIconColor: Color,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) selectedIconColor else iconColor

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
fun ChatScreenWrapper(
    onVoiceInput: (() -> Unit)? = null
) {
    // Get ChatViewModel via Hilt
    val viewModel: ChatViewModel = hiltViewModel()

    // Use real ChatScreen from features:chat module
    // Pass voice input callback to enable mic button in input row
    ChatScreen(
        viewModel = viewModel,
        modifier = Modifier.fillMaxSize(),
        onVoiceInput = onVoiceInput
    )
}

@Composable
fun TeachAvaScreenWrapper() {
    // Get TeachAvaViewModel via Hilt
    val viewModel: TeachAvaViewModel = hiltViewModel()

    // Use real TeachAvaScreen from features:teach module
    TeachAvaScreen(
        viewModel = viewModel,
        onNavigateBack = { /* No-op: Bottom nav handles navigation */ },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun SettingsScreenWrapper(
    onNavigateToModelDownload: () -> Unit = {},
    onNavigateToTestLauncher: () -> Unit = {}
) {
    // Get SettingsViewModel via Hilt
    val viewModel: SettingsViewModel = hiltViewModel()

    // Use responsive SettingsScreen
    SettingsScreenResponsive(
        viewModel = viewModel,
        onNavigateToModelDownload = onNavigateToModelDownload,
        onNavigateToTestLauncher = onNavigateToTestLauncher,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ModelDownloadScreenWrapper(
    onNavigateBack: () -> Unit
) {
    // Get SettingsViewModel via Hilt for model downloads
    val viewModel: SettingsViewModel = hiltViewModel()

    // Use ModelDownloadScreen
    ModelDownloadScreen(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun TestLauncherScreenWrapper(
    onNavigateBack: () -> Unit
) {
    // Create TestLauncherViewModel
    val viewModel = remember {
        TestLauncherViewModel()
    }

    // Use TestLauncherScreen
    TestLauncherScreen(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        modifier = Modifier.fillMaxSize()
    )
}
