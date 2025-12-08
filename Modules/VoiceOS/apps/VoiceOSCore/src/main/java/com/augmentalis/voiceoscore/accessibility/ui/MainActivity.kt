/**
 * MainActivity.kt - Main application entry point for VoiceOS Accessibility
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-01-28
 */
package com.augmentalis.voiceoscore.accessibility.ui

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.augmentalis.voiceoscore.accessibility.config.ServiceConfiguration
import com.augmentalis.voiceoscore.accessibility.ui.theme.AccessibilityTheme
import com.augmentalis.voiceoscore.accessibility.ui.utils.DepthLevel
import com.augmentalis.voiceoscore.accessibility.ui.utils.GlassMorphismConfig
import com.augmentalis.voiceoscore.accessibility.ui.utils.glassMorphism
import com.augmentalis.voiceoscore.accessibility.viewmodel.MainViewModel
import com.augmentalis.voiceoscore.ui.components.FloatingEngineSelector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    
    // Activity launcher for accessibility settings
    private val accessibilitySettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Check service status after returning from settings
        viewModel.checkServiceStatus()
    }
    
    // Activity launcher for system overlay permission
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Check overlay permission after returning from settings
        viewModel.checkOverlayPermission()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ViewModel
        viewModel.initialize(this)
        
        setContent {
            AccessibilityTheme {
                MainScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = ::navigateToAccessibilitySettings,
                    onNavigateToTesting = ::navigateToTesting,
                    onRequestOverlayPermission = ::requestOverlayPermission
                )
            }
        }

        microphonePermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        
        // Check permissions and service status
        lifecycleScope.launch {
            delay(500) // Allow UI to initialize
            viewModel.checkAllPermissions()
        }
    }
    
    private fun navigateToAccessibilitySettings() {
        val intent = Intent(this, AccessibilitySettings::class.java)
        startActivity(intent)
    }
    
    private fun navigateToTesting() {
        // Navigate to command testing within this activity
        // For now, this can be expanded to show a testing dialog or navigate to a testing screen
        // Implementation can be added later as needed
    }
    
    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        overlayPermissionLauncher.launch(intent)
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        accessibilitySettingsLauncher.launch(intent)
    }



    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted — proceed with microphone use
        } else {
            // Permission denied — show a message or disable voice features
        }
    }
}

/**
 * Adaptive spacing values based on orientation
 */
data class AdaptiveSpacing(
    val outerPadding: Dp,
    val cardPadding: Dp,
    val itemSpacing: Dp,
    val iconSize: Dp,
    val smallIconSize: Dp
)

/**
 * Get adaptive spacing based on orientation
 */
@Composable
fun getAdaptiveSpacing(): AdaptiveSpacing {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    return if (isLandscape) {
        AdaptiveSpacing(
            outerPadding = 8.dp,
            cardPadding = 8.dp,
            itemSpacing = 6.dp,
            iconSize = 20.dp,
            smallIconSize = 14.dp
        )
    } else {
        AdaptiveSpacing(
            outerPadding = 16.dp,
            cardPadding = 16.dp,
            itemSpacing = 12.dp,
            iconSize = 32.dp,
            smallIconSize = 18.dp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToTesting: () -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    val context = LocalContext.current
    val serviceEnabled by viewModel.serviceEnabled.observeAsState(false)
    val overlayPermissionGranted by viewModel.overlayPermissionGranted.observeAsState(false)
    val configuration by viewModel.configuration.observeAsState(ServiceConfiguration.createDefault())
    val selectedEngine by viewModel.selectedEngine.observeAsState("vivoka")
    val isRecognizing by viewModel.isRecognizing.observeAsState(false)

    val localConfig = LocalConfiguration.current
    val isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
    val spacing = getAdaptiveSpacing()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        if (isLandscape) {
            // LANDSCAPE: Two-column layout for maximum space usage
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.outerPadding),
                horizontalArrangement = Arrangement.spacedBy(spacing.itemSpacing)
            ) {
                // Left column: Header + Permissions
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(spacing.itemSpacing)
                ) {
                    HeaderSectionCompact(spacing)
                    ServiceStatusCardCompact(
                        serviceEnabled = serviceEnabled,
                        overlayPermissionGranted = overlayPermissionGranted,
                        onEnableService = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        onRequestOverlay = onRequestOverlayPermission,
                        spacing = spacing
                    )
                }

                // Right column: Stats + Navigation + Footer
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(spacing.itemSpacing)
                ) {
                    QuickStatsCardCompact(configuration = configuration, spacing = spacing)
                    NavigationCardsCompact(
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToTesting = onNavigateToTesting,
                        serviceEnabled = serviceEnabled,
                        spacing = spacing
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    FooterSectionCompact()
                }
            }
        } else {
            // PORTRAIT: Original vertical layout with reduced spacing
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.outerPadding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.itemSpacing)
            ) {
                HeaderSection(spacing)
                ServiceStatusCard(
                    serviceEnabled = serviceEnabled,
                    overlayPermissionGranted = overlayPermissionGranted,
                    onEnableService = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    onRequestOverlay = onRequestOverlayPermission,
                    spacing = spacing
                )
                QuickStatsCard(configuration = configuration, spacing = spacing)
                NavigationCards(
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToTesting = onNavigateToTesting,
                    serviceEnabled = serviceEnabled,
                    spacing = spacing
                )
                Spacer(modifier = Modifier.weight(1f))
                FooterSection()
            }
        }

        // Floating Engine Selector for testing
        FloatingEngineSelector(
            selectedEngine = selectedEngine,
            onEngineSelected = { engine ->
                viewModel.selectEngine(engine)
            },
            onInitiate = { engine ->
                if (isRecognizing) {
                    viewModel.stopRecognition()
                } else {
                    viewModel.startRecognitionWithEngine(engine)
                }
            },
            isRecognizing = isRecognizing
        )
    }
}

@Composable
fun HeaderSection(spacing: AdaptiveSpacing) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 12.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.15f,
                    borderWidth = 0.5.dp,
                    tintColor = Color(0xFF4285F4),
                    tintOpacity = 0.1f
                ),
                depth = DepthLevel(0.8f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Accessibility,
                contentDescription = "VoiceOS Accessibility",
                modifier = Modifier.size(spacing.iconSize),
                tint = Color(0xFF4285F4)
            )

            Spacer(modifier = Modifier.width(spacing.itemSpacing))

            Column {
                Text(
                    text = "VoiceOS Accessibility",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Voice-controlled device interaction",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Compact header for landscape mode - inline layout
 */
@Composable
fun HeaderSectionCompact(spacing: AdaptiveSpacing) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 8.dp,
                    backgroundOpacity = 0.08f,
                    borderOpacity = 0.1f,
                    borderWidth = 0.5.dp,
                    tintColor = Color(0xFF4285F4),
                    tintOpacity = 0.08f
                ),
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Accessibility,
                contentDescription = "VoiceOS",
                modifier = Modifier.size(spacing.iconSize),
                tint = Color(0xFF4285F4)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "VoiceOS",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Voice control",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun ServiceStatusCard(
    serviceEnabled: Boolean,
    overlayPermissionGranted: Boolean,
    onEnableService: () -> Unit,
    onRequestOverlay: () -> Unit,
    spacing: AdaptiveSpacing
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 12.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.15f,
                    borderWidth = 0.5.dp,
                    tintColor = if (serviceEnabled && overlayPermissionGranted) Color(0xFF00C853) else Color(0xFFFF5722),
                    tintOpacity = 0.1f
                ),
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(spacing.cardPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Service Status",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                StatusIndicator(isActive = serviceEnabled && overlayPermissionGranted, spacing = spacing)
            }

            Spacer(modifier = Modifier.height(spacing.itemSpacing))

            PermissionRow(
                icon = Icons.Default.Accessibility,
                title = "Accessibility Service",
                isGranted = serviceEnabled,
                onClick = if (!serviceEnabled) onEnableService else null,
                spacing = spacing
            )

            Spacer(modifier = Modifier.height(spacing.itemSpacing / 2))

            PermissionRow(
                icon = Icons.Default.Layers,
                title = "System Overlay",
                isGranted = overlayPermissionGranted,
                onClick = if (!overlayPermissionGranted) onRequestOverlay else null,
                spacing = spacing
            )
        }
    }
}

/**
 * Compact service status card for landscape
 */
@Composable
fun ServiceStatusCardCompact(
    serviceEnabled: Boolean,
    overlayPermissionGranted: Boolean,
    onEnableService: () -> Unit,
    onRequestOverlay: () -> Unit,
    spacing: AdaptiveSpacing
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 8.dp,
                    backgroundOpacity = 0.08f,
                    borderOpacity = 0.1f,
                    borderWidth = 0.5.dp,
                    tintColor = if (serviceEnabled && overlayPermissionGranted) Color(0xFF00C853) else Color(0xFFFF5722),
                    tintOpacity = 0.08f
                ),
                depth = DepthLevel(0.5f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(spacing.cardPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Permissions",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                StatusIndicatorCompact(isActive = serviceEnabled && overlayPermissionGranted)
            }

            Spacer(modifier = Modifier.height(4.dp))

            PermissionRowCompact(
                icon = Icons.Default.Accessibility,
                title = "Accessibility",
                isGranted = serviceEnabled,
                onClick = if (!serviceEnabled) onEnableService else null,
                spacing = spacing
            )

            Spacer(modifier = Modifier.height(2.dp))

            PermissionRowCompact(
                icon = Icons.Default.Layers,
                title = "Overlay",
                isGranted = overlayPermissionGranted,
                onClick = if (!overlayPermissionGranted) onRequestOverlay else null,
                spacing = spacing
            )
        }
    }
}

@Composable
fun PermissionRow(
    icon: ImageVector,
    title: String,
    isGranted: Boolean,
    onClick: (() -> Unit)? = null,
    spacing: AdaptiveSpacing
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(spacing.smallIconSize),
            tint = if (isGranted) Color(0xFF00C853) else Color(0xFFFF5722)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = if (isGranted) "Granted" else "Required",
            modifier = Modifier.size(spacing.smallIconSize - 2.dp),
            tint = if (isGranted) Color(0xFF00C853) else Color(0xFFFF5722)
        )

        if (onClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Configure",
                modifier = Modifier.size(spacing.smallIconSize - 4.dp),
                tint = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Compact permission row for landscape
 */
@Composable
fun PermissionRowCompact(
    icon: ImageVector,
    title: String,
    isGranted: Boolean,
    onClick: (() -> Unit)? = null,
    spacing: AdaptiveSpacing
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(12.dp),
            tint = if (isGranted) Color(0xFF00C853) else Color(0xFFFF5722)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = if (isGranted) "OK" else "Req",
            modifier = Modifier.size(10.dp),
            tint = if (isGranted) Color(0xFF00C853) else Color(0xFFFF5722)
        )

        if (onClick != null) {
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Go",
                modifier = Modifier.size(10.dp),
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun StatusIndicator(isActive: Boolean, spacing: AdaptiveSpacing) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    color = if (isActive) Color(0xFF00C853) else Color(0xFFFF5722),
                    shape = RoundedCornerShape(50)
                )
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = if (isActive) "Active" else "Inactive",
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) Color(0xFF00C853) else Color(0xFFFF5722),
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Compact status indicator for landscape
 */
@Composable
fun StatusIndicatorCompact(isActive: Boolean) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(
                color = if (isActive) Color(0xFF00C853) else Color(0xFFFF5722),
                shape = RoundedCornerShape(50)
            )
    )
}

@Composable
fun QuickStatsCard(configuration: ServiceConfiguration, spacing: AdaptiveSpacing) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 12.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.15f,
                    borderWidth = 0.5.dp,
                    tintColor = Color(0xFF673AB7),
                    tintOpacity = 0.1f
                ),
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(spacing.cardPadding)
        ) {
            Text(
                text = "Configuration",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(spacing.itemSpacing))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Handlers",
                    value = if (configuration.handlersEnabled) "7" else "Off"
                )
                StatItem(
                    label = "Cursor",
                    value = if (configuration.cursorEnabled) "On" else "Off"
                )
                StatItem(
                    label = "Cache",
                    value = "${configuration.maxCacheSize}"
                )
            }
        }
    }
}

/**
 * Compact quick stats for landscape
 */
@Composable
fun QuickStatsCardCompact(configuration: ServiceConfiguration, spacing: AdaptiveSpacing) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 8.dp,
                    backgroundOpacity = 0.08f,
                    borderOpacity = 0.1f,
                    borderWidth = 0.5.dp,
                    tintColor = Color(0xFF673AB7),
                    tintOpacity = 0.08f
                ),
                depth = DepthLevel(0.5f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(spacing.cardPadding)
        ) {
            Text(
                text = "Config",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItemCompact(
                    label = "Handlers",
                    value = if (configuration.handlersEnabled) "7" else "Off"
                )
                StatItemCompact(
                    label = "Cursor",
                    value = if (configuration.cursorEnabled) "On" else "Off"
                )
                StatItemCompact(
                    label = "Cache",
                    value = "${configuration.maxCacheSize}"
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

/**
 * Compact stat item for landscape
 */
@Composable
fun StatItemCompact(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun NavigationCards(
    onNavigateToSettings: () -> Unit,
    onNavigateToTesting: () -> Unit,
    serviceEnabled: Boolean,
    spacing: AdaptiveSpacing
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.itemSpacing)
    ) {
        NavigationCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Settings,
            title = "Settings",
            description = "Configure options",
            tintColor = Color(0xFF2196F3),
            onClick = onNavigateToSettings,
            spacing = spacing
        )
        NavigationCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.PlayArrow,
            title = "Testing",
            description = "Test commands",
            tintColor = Color(0xFF4CAF50),
            enabled = serviceEnabled,
            onClick = onNavigateToTesting,
            spacing = spacing
        )
    }
}

/**
 * Compact navigation cards for landscape
 */
@Composable
fun NavigationCardsCompact(
    onNavigateToSettings: () -> Unit,
    onNavigateToTesting: () -> Unit,
    serviceEnabled: Boolean,
    spacing: AdaptiveSpacing
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        NavigationCardCompact(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Settings,
            title = "Settings",
            tintColor = Color(0xFF2196F3),
            onClick = onNavigateToSettings,
            spacing = spacing
        )
        NavigationCardCompact(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.PlayArrow,
            title = "Test",
            tintColor = Color(0xFF4CAF50),
            enabled = serviceEnabled,
            onClick = onNavigateToTesting,
            spacing = spacing
        )
    }
}

@Composable
fun NavigationCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    tintColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
    spacing: AdaptiveSpacing
) {
    Card(
        modifier = modifier
            .clickable(enabled = enabled) { onClick() }
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 12.dp,
                    backgroundOpacity = if (enabled) 0.1f else 0.05f,
                    borderOpacity = if (enabled) 0.15f else 0.08f,
                    borderWidth = 0.5.dp,
                    tintColor = if (enabled) tintColor else Color.Gray,
                    tintOpacity = if (enabled) 0.1f else 0.05f
                ),
                depth = DepthLevel(0.4f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(spacing.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(spacing.iconSize),
                tint = if (enabled) tintColor else Color.Gray
            )

            Spacer(modifier = Modifier.height(spacing.itemSpacing / 2))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) Color.White else Color.Gray,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) Color.White.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * Compact navigation card for landscape
 */
@Composable
fun NavigationCardCompact(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    tintColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
    spacing: AdaptiveSpacing
) {
    Card(
        modifier = modifier
            .clickable(enabled = enabled) { onClick() }
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 8.dp,
                    backgroundOpacity = if (enabled) 0.08f else 0.04f,
                    borderOpacity = if (enabled) 0.1f else 0.05f,
                    borderWidth = 0.5.dp,
                    tintColor = if (enabled) tintColor else Color.Gray,
                    tintOpacity = if (enabled) 0.08f else 0.04f
                ),
                depth = DepthLevel(0.3f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(spacing.iconSize),
                tint = if (enabled) tintColor else Color.Gray
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) Color.White else Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun FooterSection() {
    Text(
        text = "VOS4 • Intelligent Devices LLC",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Compact footer for landscape
 */
@Composable
fun FooterSectionCompact() {
    Text(
        text = "VOS4",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.3f)
    )
}