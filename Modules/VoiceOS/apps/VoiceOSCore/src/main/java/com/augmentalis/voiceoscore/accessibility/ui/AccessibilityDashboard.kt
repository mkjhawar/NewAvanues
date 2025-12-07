/**
 * AccessibilityDashboard.kt - Main dashboard composable for VoiceOS Accessibility
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-08-28
 */
package com.augmentalis.voiceoscore.accessibility.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.voiceoscore.accessibility.config.ServiceConfiguration
import com.augmentalis.voiceoscore.accessibility.ui.utils.glassMorphism
import com.augmentalis.voiceoscore.accessibility.ui.utils.GlassMorphismConfig
import com.augmentalis.voiceoscore.accessibility.ui.utils.DepthLevel
import com.augmentalis.voiceoscore.accessibility.ui.utils.ThemeUtils

/**
 * Main accessibility dashboard with glassmorphism design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilityDashboard(
    serviceEnabled: Boolean,
    configuration: ServiceConfiguration,
    onNavigateToSettings: () -> Unit,
    onNavigateToTesting: () -> Unit,
    onRequestPermission: () -> Unit,
    commandsExecuted: Int = 0,
    successRate: Float = 0.0f,
    performanceMode: String = "Balanced"
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)) // Dark theme background
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section
        item {
            DashboardHeader()
        }
        
        // Service Status Card
        item {
            ServiceStatusCard(
                serviceEnabled = serviceEnabled,
                onRequestPermission = onRequestPermission
            )
        }
        
        // Quick Statistics
        item {
            QuickStatsSection(
                commandsExecuted = commandsExecuted,
                successRate = successRate,
                handlersActive = getActiveHandlersCount(configuration)
            )
        }
        
        // Performance Mode Indicator
        item {
            PerformanceModeCard(performanceMode = performanceMode)
        }
        
        // Handler Status Overview
        item {
            HandlerStatusOverview(configuration = configuration)
        }
        
        // Navigation Section
        item {
            DashboardNavigation(
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToTesting = onNavigateToTesting,
                serviceEnabled = serviceEnabled
            )
        }
        
        // System Information
        item {
            SystemInfoCard(configuration = configuration)
        }
    }
}

/**
 * Dashboard header with app branding
 */
@Composable
fun DashboardHeader() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 20.dp,
                    backgroundOpacity = 0.15f,
                    borderOpacity = 0.25f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFF4285F4),
                    tintOpacity = 0.2f
                ),
                depth = DepthLevel(1.0f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Accessibility,
                contentDescription = "VoiceOS Accessibility",
                modifier = Modifier.size(40.dp),
                tint = Color(0xFF4285F4)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "VoiceOS Accessibility",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ThemeUtils.getTextColor(),
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Voice Control Dashboard",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ThemeUtils.getSecondaryTextColor()
                )
            }
        }
    }
}

/**
 * Service status indicator with action button
 */
@Composable
fun ServiceStatusCard(
    serviceEnabled: Boolean,
    onRequestPermission: () -> Unit
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
                    tintColor = if (serviceEnabled) Color(0xFF00C853) else Color(0xFFFF5722),
                    tintOpacity = 0.18f
                ),
                depth = DepthLevel(0.8f)
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
                Column {
                    Text(
                        text = "Service Status",
                        style = MaterialTheme.typography.titleLarge,
                        color = ThemeUtils.getTextColor(),
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = if (serviceEnabled) "Accessibility service is running" else "Accessibility service disabled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeUtils.getSecondaryTextColor()
                    )
                }
                
                StatusIndicatorBadge(isActive = serviceEnabled)
            }
            
            if (!serviceEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassMorphism(
                            config = GlassMorphismConfig(
                                cornerRadius = 8.dp,
                                backgroundOpacity = 0.15f,
                                borderOpacity = 0.3f,
                                borderWidth = 1.dp,
                                tintColor = Color(0xFF4CAF50),
                                tintOpacity = 0.2f
                            ),
                            depth = DepthLevel(0.4f)
                        ),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Enable",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable Accessibility Service")
                }
            }
        }
    }
}

/**
 * Quick statistics overview
 */
@Composable
fun QuickStatsSection(
    commandsExecuted: Int,
    successRate: Float,
    handlersActive: Int
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
                text = "Quick Statistics",
                style = MaterialTheme.typography.titleLarge,
                color = ThemeUtils.getTextColor(),
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    value = commandsExecuted.toString(),
                    label = "Commands",
                    icon = Icons.Default.PlayArrow,
                    color = Color(0xFF2196F3)
                )
                
                StatisticItem(
                    value = "${(successRate * 100).toInt()}%",
                    label = "Success Rate",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50)
                )
                
                StatisticItem(
                    value = "$handlersActive",
                    label = "Handlers",
                    icon = Icons.Default.Extension,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

/**
 * Individual statistic item
 */
@Composable
fun StatisticItem(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = color
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = ThemeUtils.getTextColor(),
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ThemeUtils.getSecondaryTextColor()
        )
    }
}

/**
 * Performance mode indicator
 */
@Composable
fun PerformanceModeCard(performanceMode: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 12.dp,
                    backgroundOpacity = 0.08f,
                    borderOpacity = 0.15f,
                    borderWidth = 1.dp,
                    tintColor = getPerformanceModeColor(performanceMode),
                    tintOpacity = 0.12f
                ),
                depth = DepthLevel(0.4f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getPerformanceModeIcon(performanceMode),
                contentDescription = "Performance Mode",
                modifier = Modifier.size(20.dp),
                tint = getPerformanceModeColor(performanceMode)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = "Performance Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeUtils.getSecondaryTextColor()
                )
                
                Text(
                    text = performanceMode,
                    style = MaterialTheme.typography.titleMedium,
                    color = ThemeUtils.getTextColor(),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Handler status overview grid
 */
@Composable
fun HandlerStatusOverview(configuration: ServiceConfiguration) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 16.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.2f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFF009688),
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
                text = "Handler Status",
                style = MaterialTheme.typography.titleLarge,
                color = ThemeUtils.getTextColor(),
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val handlers = listOf(
                Pair("App Handler", configuration.handlersEnabled && configuration.appLaunchingEnabled),
                Pair("Navigation Handler", configuration.handlersEnabled),
                Pair("System Handler", configuration.handlersEnabled),
                Pair("UI Handler", configuration.handlersEnabled),
                Pair("Device Handler", configuration.handlersEnabled),
                Pair("Input Handler", configuration.handlersEnabled),
                Pair("Action Handler", true) // Always enabled as base
            )
            
            handlers.chunked(2).forEach { handlerPair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    handlerPair.forEach { (name, enabled) ->
                        HandlerStatusItem(
                            modifier = Modifier.weight(1f),
                            name = name,
                            enabled = enabled
                        )
                    }
                    // Fill remaining space if odd number
                    if (handlerPair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Individual handler status item
 */
@Composable
fun HandlerStatusItem(
    modifier: Modifier = Modifier,
    name: String,
    enabled: Boolean
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (enabled) Color(0xFF00C853) else Color(0xFFFF5722),
                    shape = RoundedCornerShape(50)
                )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = name.replace(" Handler", ""),
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled) ThemeUtils.getTextColor() else ThemeUtils.getDisabledTextColor(),
            fontSize = 12.sp
        )
    }
}

/**
 * Dashboard navigation cards
 */
@Composable
fun DashboardNavigation(
    onNavigateToSettings: () -> Unit,
    onNavigateToTesting: () -> Unit,
    serviceEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DashboardNavigationCard(
            modifier = Modifier.weight(1f),
            title = "Settings",
            description = "Configure handlers and options",
            icon = Icons.Default.Settings,
            color = Color(0xFF2196F3),
            onClick = onNavigateToSettings
        )
        
        DashboardNavigationCard(
            modifier = Modifier.weight(1f),
            title = "Testing",
            description = "Test voice commands",
            icon = Icons.Default.PlayArrow,
            color = Color(0xFF4CAF50),
            enabled = serviceEnabled,
            onClick = onNavigateToTesting
        )
    }
}

/**
 * Individual navigation card
 */
@Composable
fun DashboardNavigationCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(enabled = enabled) { onClick() }
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 12.dp,
                    backgroundOpacity = if (enabled) 0.1f else 0.05f,
                    borderOpacity = if (enabled) 0.2f else 0.1f,
                    borderWidth = 1.dp,
                    tintColor = if (enabled) color else Color.Gray,
                    tintOpacity = if (enabled) 0.15f else 0.08f
                ),
                depth = DepthLevel(0.4f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = if (enabled) color else Color.Gray
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) ThemeUtils.getTextColor() else ThemeUtils.getDisabledTextColor(),
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) ThemeUtils.getSecondaryTextColor() else ThemeUtils.getDisabledTextColor(),
                fontSize = 11.sp
            )
        }
    }
}

/**
 * System information card
 */
@Composable
fun SystemInfoCard(configuration: ServiceConfiguration) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 12.dp,
                    backgroundOpacity = 0.08f,
                    borderOpacity = 0.15f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFF607D8B),
                    tintOpacity = 0.12f
                ),
                depth = DepthLevel(0.3f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "System Configuration",
                style = MaterialTheme.typography.titleMedium,
                color = ThemeUtils.getTextColor(),
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SystemInfoItem("Cache Size", "${configuration.maxCacheSize} items")
                SystemInfoItem("Timeout", "${configuration.commandTimeout}ms")
                SystemInfoItem("Cursor", if (configuration.cursorEnabled) "ON" else "OFF")
            }
        }
    }
}

/**
 * Individual system info item
 */
@Composable
fun SystemInfoItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = ThemeUtils.getTextColor(),
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ThemeUtils.getSecondaryTextColor(),
            fontSize = 10.sp
        )
    }
}

/**
 * Status indicator badge
 */
@Composable
fun StatusIndicatorBadge(isActive: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = (if (isActive) Color(0xFF00C853) else Color(0xFFFF5722)).copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (isActive) Color(0xFF00C853) else Color(0xFFFF5722),
                    shape = RoundedCornerShape(50)
                )
        )
        
        Spacer(modifier = Modifier.width(6.dp))
        
        Text(
            text = if (isActive) "Active" else "Inactive",
            style = MaterialTheme.typography.bodySmall,
            color = if (isActive) Color(0xFF00C853) else Color(0xFFFF5722),
            fontWeight = FontWeight.Medium
        )
    }
}

// Helper functions
private fun getActiveHandlersCount(configuration: ServiceConfiguration): Int {
    var count = 1 // ActionHandler is always enabled
    if (configuration.handlersEnabled) {
        count += 5 // App, Navigation, System, UI, Device handlers
        if (configuration.appLaunchingEnabled) count++ // Input handler
    }
    return count
}

private fun getPerformanceModeColor(mode: String): Color {
    return when (mode) {
        "High Performance" -> Color(0xFFFF5722)
        "Power Saver" -> Color(0xFF4CAF50)
        else -> Color(0xFF2196F3) // Balanced
    }
}

private fun getPerformanceModeIcon(mode: String): ImageVector {
    return when (mode) {
        "High Performance" -> Icons.Default.Speed
        "Power Saver" -> Icons.Default.BatteryChargingFull
        else -> Icons.Default.Balance // Balanced
    }
}