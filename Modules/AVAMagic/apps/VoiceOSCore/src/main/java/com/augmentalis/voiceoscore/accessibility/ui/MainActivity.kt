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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0xFF000000) // Dark background for glassmorphism
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            HeaderSection()
            
            // Service Status Card
            ServiceStatusCard(
                serviceEnabled = serviceEnabled,
                overlayPermissionGranted = overlayPermissionGranted,
                onEnableService = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                },
                onRequestOverlay = onRequestOverlayPermission
            )
            
            // Quick Stats
            QuickStatsCard(configuration = configuration)
            
            // Navigation Cards
            NavigationCards(
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToTesting = onNavigateToTesting,
                serviceEnabled = serviceEnabled
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Footer
            FooterSection()
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
fun HeaderSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 16.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.2f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFF4285F4),
                    tintOpacity = 0.15f
                ),
                depth = DepthLevel(0.8f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Accessibility,
                contentDescription = "VoiceOS Accessibility",
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF4285F4)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "VoiceOS Accessibility",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Voice-controlled device interaction",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ServiceStatusCard(
    serviceEnabled: Boolean,
    overlayPermissionGranted: Boolean,
    onEnableService: () -> Unit,
    onRequestOverlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 16.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.2f,
                    borderWidth = 1.dp,
                    tintColor = if (serviceEnabled && overlayPermissionGranted) Color(0xFF00C853) else Color(0xFFFF5722),
                    tintOpacity = 0.15f
                ),
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Service Status",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                
                StatusIndicator(
                    isActive = serviceEnabled && overlayPermissionGranted
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Accessibility Service Status
            PermissionRow(
                icon = Icons.Default.Accessibility,
                title = "Accessibility Service",
                isGranted = serviceEnabled,
                onClick = if (!serviceEnabled) onEnableService else null
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // System Overlay Permission
            PermissionRow(
                icon = Icons.Default.Layers,
                title = "System Overlay",
                isGranted = overlayPermissionGranted,
                onClick = if (!overlayPermissionGranted) onRequestOverlay else null
            )
        }
    }
}

@Composable
fun PermissionRow(
    icon: ImageVector,
    title: String,
    isGranted: Boolean,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(20.dp),
            tint = if (isGranted) Color(0xFF00C853) else Color(0xFFFF5722)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = if (isGranted) "Granted" else "Required",
            modifier = Modifier.size(18.dp),
            tint = if (isGranted) Color(0xFF00C853) else Color(0xFFFF5722)
        )
        
        if (onClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Configure",
                modifier = Modifier.size(16.dp),
                tint = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun StatusIndicator(isActive: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (isActive) Color(0xFF00C853) else Color(0xFFFF5722),
                    shape = RoundedCornerShape(50)
                )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = if (isActive) "Active" else "Inactive",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isActive) Color(0xFF00C853) else Color(0xFFFF5722),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun QuickStatsCard(configuration: ServiceConfiguration) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 16.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.2f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFF673AB7),
                    tintOpacity = 0.15f
                ),
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Configuration",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Handlers",
                    value = if (configuration.handlersEnabled) "7 Active" else "Disabled"
                )
                
                StatItem(
                    label = "Cursor",
                    value = if (configuration.cursorEnabled) "Enabled" else "Disabled"
                )
                
                StatItem(
                    label = "Cache",
                    value = "${configuration.maxCacheSize} cmds"
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
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun NavigationCards(
    onNavigateToSettings: () -> Unit,
    onNavigateToTesting: () -> Unit,
    serviceEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Settings Card
        NavigationCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Settings,
            title = "Settings",
            description = "Configure service options",
            tintColor = Color(0xFF2196F3),
            onClick = onNavigateToSettings
        )
        
        // Testing Card
        NavigationCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.PlayArrow,
            title = "Testing",
            description = "Test voice commands",
            tintColor = Color(0xFF4CAF50),
            enabled = serviceEnabled,
            onClick = onNavigateToTesting
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
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(enabled = enabled) { onClick() }
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 16.dp,
                    backgroundOpacity = if (enabled) 0.1f else 0.05f,
                    borderOpacity = if (enabled) 0.2f else 0.1f,
                    borderWidth = 1.dp,
                    tintColor = if (enabled) tintColor else Color.Gray,
                    tintOpacity = if (enabled) 0.15f else 0.08f
                ),
                depth = DepthLevel(0.4f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = if (enabled) tintColor else Color.Gray
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) Color.White else Color.Gray,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) Color.White.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun FooterSection() {
    Text(
        text = "VOS4 • Intelligent Devices LLC",
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    )
}