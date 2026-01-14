package com.augmentalis.voiceoscoreng

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.augmentalis.voiceoscoreng.handlers.VoiceOSCoreNG
import com.augmentalis.voiceoscoreng.features.LearnAppConfig
import com.augmentalis.voiceoscoreng.features.LearnAppDevToggle
import com.augmentalis.voiceoscoreng.features.DeveloperSettingsScreen
import com.augmentalis.voiceoscoreng.features.ScanningCallbacks
import com.augmentalis.voiceoscoreng.service.OverlayService
import com.augmentalis.voiceoscoreng.service.VoiceOSAccessibilityService
import com.augmentalis.voiceoscoreng.ui.theme.VoiceOSCoreNGTheme
import kotlinx.coroutines.launch

/**
 * Main Activity for VoiceOSCoreNG Test App.
 *
 * Provides a testing interface for all VoiceOSCoreNG features including:
 * - Test Mode FAB for quick feature testing
 * - Developer Settings for configuration
 * - Real-time configuration display
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoiceOSCoreNGTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var showDeveloperSettings by remember { mutableStateOf(false) }
    var configSummary by remember {
        mutableStateOf(LearnAppConfig.getSummary())
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Listen for config changes
    DisposableEffect(Unit) {
        val listener: (LearnAppConfig.VariantConfig) -> Unit = {
            configSummary = LearnAppConfig.getSummary()
        }
        LearnAppConfig.addConfigChangeListener(listener)

        onDispose {
            LearnAppConfig.removeConfigChangeListener(listener)
        }
    }

    // Developer Settings Bottom Sheet
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    if (showDeveloperSettings) {
        ModalBottomSheet(
            onDismissRequest = { showDeveloperSettings = false },
            sheetState = sheetState,
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            DeveloperSettingsScreen(
                onDismiss = { showDeveloperSettings = false },
                scanningCallbacks = ScanningCallbacks(
                    onSetContinuousMonitoring = { enabled ->
                        VoiceOSAccessibilityService.setContinuousMonitoring(enabled)
                    },
                    onRescanCurrentApp = {
                        VoiceOSAccessibilityService.rescanCurrentApp()
                    },
                    onRescanEverything = {
                        VoiceOSAccessibilityService.rescanEverything()
                    }
                )
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                DrawerContent(
                    configSummary = configSummary,
                    onOpenSettings = {
                        scope.launch { drawerState.close() }
                        showDeveloperSettings = true
                    },
                    onCloseDrawer = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "VoiceOSCoreNG",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "v${VoiceOSCoreNG.getVersion()} • Test App",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        // Tier indicator
                        val tier = VoiceOSCoreNG.getCurrentTier()
                        AssistChip(
                            onClick = { VoiceOSCoreNG.toggle() },
                            label = { Text(tier.name) },
                            leadingIcon = {
                                Icon(
                                    if (tier == LearnAppDevToggle.Tier.DEV)
                                        Icons.Default.Code
                                    else
                                        Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Scanner Control Card (Primary action)
                ScannerControlCard()

                // Status Card
                StatusCard(configSummary = configSummary)

                // Instructions Card
                InstructionsCard()

                // Feature Status Card
                FeatureStatusCard()
            }
        }
    }
}

/**
 * Drawer content with all testing and settings options.
 */
@Composable
private fun DrawerContent(
    configSummary: String,
    onOpenSettings: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var testModeEnabled by remember { mutableStateOf(LearnAppConfig.DeveloperSettings.enabled) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "VoiceOSCoreNG",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "v${VoiceOSCoreNG.getVersion()} • ${LearnAppDevToggle.getCurrentTier().name} Mode",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Test Mode Section
        DrawerSection(title = "Test Mode") {
            DrawerToggleItem(
                icon = Icons.Default.Science,
                title = if (testModeEnabled) "Disable Test Mode" else "Enable Test Mode",
                subtitle = if (testModeEnabled) "All features unlocked" else "Unlock all features",
                checked = testModeEnabled,
                onToggle = {
                    if (testModeEnabled) {
                        LearnAppConfig.reset()
                    } else {
                        LearnAppConfig.enableTestMode()
                    }
                    testModeEnabled = !testModeEnabled
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Scanning Section
        var continuousMonitoringEnabled by remember { mutableStateOf(true) }
        DrawerSection(title = "Scanning") {
            DrawerToggleItem(
                icon = Icons.Default.Sync,
                title = "Continuous Monitoring",
                subtitle = if (continuousMonitoringEnabled) "Auto-scan on screen change" else "Manual scanning only",
                checked = continuousMonitoringEnabled,
                onToggle = {
                    continuousMonitoringEnabled = !continuousMonitoringEnabled
                    VoiceOSAccessibilityService.setContinuousMonitoring(continuousMonitoringEnabled)
                }
            )
            DrawerActionItem(
                icon = Icons.Default.Refresh,
                title = "Rescan Current App",
                subtitle = "Clear cache and rescan current app",
                onClick = {
                    VoiceOSAccessibilityService.rescanCurrentApp()
                    onCloseDrawer()
                }
            )
            DrawerActionItem(
                icon = Icons.Default.DeleteSweep,
                title = "Rescan Everything",
                subtitle = "Clear all cached screens",
                onClick = {
                    VoiceOSAccessibilityService.rescanEverything()
                    onCloseDrawer()
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Overlay Section
        DrawerSection(title = "Overlay") {
            DrawerActionItem(
                icon = Icons.Default.PlayArrow,
                title = "Start Scanner Overlay",
                subtitle = "Show floating scanner button",
                onClick = {
                    OverlayService.start(context)
                    (context as? ComponentActivity)?.moveTaskToBack(true)
                    onCloseDrawer()
                }
            )
            DrawerActionItem(
                icon = Icons.Default.Stop,
                title = "Stop Scanner Overlay",
                subtitle = "Remove floating button",
                onClick = {
                    OverlayService.stop(context)
                    onCloseDrawer()
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Settings Section
        DrawerSection(title = "Settings") {
            DrawerActionItem(
                icon = Icons.Default.Settings,
                title = "Developer Settings",
                subtitle = "Advanced configuration options",
                onClick = onOpenSettings
            )
            DrawerActionItem(
                icon = Icons.Default.Accessibility,
                title = "Accessibility Settings",
                subtitle = "System accessibility options",
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    onCloseDrawer()
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Current Configuration
        DrawerSection(title = "Current Configuration") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = configSummary,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DrawerSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun DrawerActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = {
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
    )
}

@Composable
private fun DrawerToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = checked,
                    onCheckedChange = { onToggle() }
                )
            }
        },
        selected = false,
        onClick = onToggle,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
    )
}

@Composable
private fun StatusCard(configSummary: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Current Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                configSummary,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private const val TAG = "VoiceOSCoreNGMain"

/**
 * Check if our accessibility service is enabled at the system level.
 * This is a fallback for when the StateFlow isn't updated (e.g., service enabled via ADB).
 */
private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    // First try AccessibilityManager API
    val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager

    val enabledServices = accessibilityManager?.getEnabledAccessibilityServiceList(
        AccessibilityServiceInfo.FEEDBACK_ALL_MASK
    ) ?: emptyList()

    val ourServiceName = "${context.packageName}/${VoiceOSAccessibilityService::class.java.canonicalName}"

    android.util.Log.d(TAG, "Checking accessibility: ourServiceName=$ourServiceName")
    android.util.Log.d(TAG, "Enabled services count: ${enabledServices.size}")
    enabledServices.forEach { serviceInfo ->
        android.util.Log.d(TAG, "  - Service: id=${serviceInfo.id}, pkg=${serviceInfo.resolveInfo?.serviceInfo?.packageName}")
    }

    val apiEnabled = enabledServices.any { serviceInfo ->
        val serviceId = serviceInfo.id
        val match = serviceId == ourServiceName ||
            serviceId.contains("VoiceOSAccessibilityService") ||
            serviceInfo.resolveInfo?.serviceInfo?.packageName == context.packageName
        if (match) android.util.Log.d(TAG, "Found match: $serviceId")
        match
    }

    // Fallback: check settings directly (for ADB-enabled services)
    val settingsEnabled = try {
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val settingsMatch = enabledServicesSetting.contains(context.packageName)
        android.util.Log.d(TAG, "Settings check: '$enabledServicesSetting', match=$settingsMatch")
        settingsMatch
    } catch (e: Exception) {
        android.util.Log.e(TAG, "Failed to check settings", e)
        false
    }

    val isEnabled = apiEnabled || settingsEnabled
    android.util.Log.d(TAG, "isAccessibilityServiceEnabled result: apiEnabled=$apiEnabled, settingsEnabled=$settingsEnabled, final=$isEnabled")
    return isEnabled
}

@Composable
private fun ScannerControlCard() {
    val context = LocalContext.current
    val isAccessibilityConnectedFromService by VoiceOSAccessibilityService.isConnected.collectAsState()

    // Check if overlay permission is granted (reactive - updates on resume)
    var hasOverlayPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        )
    }

    // Also check system-level accessibility service state (fallback for ADB-enabled services)
    var isAccessibilityEnabledInSystem by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context))
    }

    // Combined check: service reports connected OR system shows it enabled
    val isAccessibilityConnected = isAccessibilityConnectedFromService || isAccessibilityEnabledInSystem

    // Re-check permissions when activity resumes (after returning from settings screens)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else {
                    true
                }
                // Also re-check accessibility state
                isAccessibilityEnabledInSystem = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Scanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "App Scanner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusChip(
                    label = "Accessibility",
                    enabled = isAccessibilityConnected
                )
                StatusChip(
                    label = "Overlay",
                    enabled = hasOverlayPermission
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Enable Accessibility Service button
            if (!isAccessibilityConnected) {
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Accessibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable Accessibility Service")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Enable Overlay Permission button
            if (!hasOverlayPermission) {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable Overlay Permission")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Start Scanner Overlay button
            if (isAccessibilityConnected && hasOverlayPermission) {
                Button(
                    onClick = {
                        OverlayService.start(context)
                        // Minimize the app so user can navigate to other apps
                        (context as? ComponentActivity)?.moveTaskToBack(true)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Scanner Overlay")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Instructions
            Text(
                text = if (!isAccessibilityConnected) {
                    "1. Enable VoiceOSCoreNG in Accessibility Settings\n" +
                    "2. Grant overlay permission\n" +
                    "3. Start the scanner overlay\n" +
                    "4. Navigate to any app and tap the floating button to scan"
                } else if (!hasOverlayPermission) {
                    "Grant overlay permission to show floating scanner button on all apps"
                } else {
                    "Scanner ready! Tap 'Start Scanner Overlay' then navigate to any app to scan its UI elements."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun StatusChip(label: String, enabled: Boolean) {
    AssistChip(
        onClick = { },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = {
            Icon(
                if (enabled) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
    )
}

@Composable
private fun InstructionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Help,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "How to Use",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                """
                1. Enable Accessibility Service in settings
                2. Grant overlay permission for floating FAB
                3. Start Scanner Overlay
                4. Navigate to any app on the device
                5. Tap the floating scanner button to analyze
                6. View VUIDs, hierarchy, duplicates, commands, AVU output

                The scanner tests: deduplication, page hierarchy, command hierarchy, hash generation, and command generation.
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun FeatureStatusCard() {
    val config = LearnAppConfig.getConfig()

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Checklist,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Feature Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Feature list
            val features = listOf(
                "AI" to config.enableAI,
                "NLU" to config.enableNLU,
                "Exploration" to config.enableExploration,
                "Framework Detection" to config.enableFrameworkDetection,
                "Caching" to config.cacheEnabled,
                "Analytics" to config.analyticsEnabled,
                "Debug Overlay" to config.enableDebugOverlay
            )

            features.forEach { (name, enabled) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(
                        if (enabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = if (enabled) "Enabled" else "Disabled",
                        tint = if (enabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
