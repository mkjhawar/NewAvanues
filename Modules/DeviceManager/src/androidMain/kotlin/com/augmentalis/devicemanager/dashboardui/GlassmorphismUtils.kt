/**
 * GlassmorphismUtils.kt - DeviceManager Glass Morphism Theme
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * Refactored: 2026-02-02 (consolidated core classes to Common/UI)
 */
package com.augmentalis.devicemanager.dashboardui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.avanues.ui.GlassMorphismConfig

// Re-export core classes for backward compatibility
typealias GlassMorphismConfig = com.avanues.ui.GlassMorphismConfig
typealias DepthLevel = com.avanues.ui.DepthLevel

/**
 * Device manager color palette
 */
object DeviceColors {
    // Status colors
    val StatusConnected = Color(0xFF4CAF50)     // Green
    val StatusDisconnected = Color(0xFF9E9E9E)  // Gray
    val StatusPairing = Color(0xFF2196F3)       // Blue
    val StatusError = Color(0xFFFF5252)         // Red
    val StatusWarning = Color(0xFFFF9800)       // Orange

    // Device type colors
    val TypePhone = Color(0xFF2196F3)           // Blue
    val TypeTablet = Color(0xFF00BCD4)          // Cyan
    val TypeFoldable = Color(0xFF9C27B0)        // Purple
    val TypeXR = Color(0xFFE91E63)              // Pink
    val TypeWatch = Color(0xFF4CAF50)           // Green
    val TypeTV = Color(0xFFFF5722)              // Deep Orange
    val TypeAuto = Color(0xFF795548)            // Brown

    // Sensor colors
    val SensorAccelerometer = Color(0xFF2196F3) // Blue
    val SensorGyroscope = Color(0xFF4CAF50)     // Green
    val SensorMagnetometer = Color(0xFF9C27B0)  // Purple
    val SensorProximity = Color(0xFFFF9800)     // Orange
    val SensorLight = Color(0xFFFFEB3B)         // Yellow
    val SensorTemperature = Color(0xFFFF5722)   // Deep Orange
    val SensorPressure = Color(0xFF00BCD4)      // Cyan
    val SensorLidar = Color(0xFFE91E63)         // Pink

    // Network colors
    val NetworkWiFi = Color(0xFF2196F3)         // Blue
    val NetworkBluetooth = Color(0xFF3F51B5)    // Indigo
    val NetworkCellular = Color(0xFF4CAF50)     // Green
    val NetworkNFC = Color(0xFF9C27B0)          // Purple
    val NetworkUWB = Color(0xFFFF9800)          // Orange

    // Audio colors
    val AudioSpeaker = Color(0xFF2196F3)        // Blue
    val AudioHeadphone = Color(0xFF4CAF50)      // Green
    val AudioBluetooth = Color(0xFF3F51B5)      // Indigo
    val AudioSpatial = Color(0xFF9C27B0)        // Purple

    // Battery level colors
    val BatteryFull = Color(0xFF4CAF50)         // Green
    val BatteryMedium = Color(0xFFFFEB3B)       // Yellow
    val BatteryLow = Color(0xFFFF9800)          // Orange
    val BatteryCritical = Color(0xFFFF5252)     // Red
}

/**
 * Pre-defined glass morphism configs for device types
 */
object DeviceGlassConfigs {
    val Primary = GlassMorphismConfig(
        tintColor = DeviceColors.TypePhone,
        cornerRadius = 16.dp
    )

    val Hardware = GlassMorphismConfig(
        tintColor = DeviceColors.TypeTablet,
        cornerRadius = 16.dp
    )

    val Sensors = GlassMorphismConfig(
        tintColor = DeviceColors.SensorAccelerometer,
        cornerRadius = 12.dp
    )

    val Network = GlassMorphismConfig(
        tintColor = DeviceColors.NetworkWiFi,
        cornerRadius = 12.dp
    )

    val Audio = GlassMorphismConfig(
        tintColor = DeviceColors.AudioSpeaker,
        cornerRadius = 12.dp
    )

    val XR = GlassMorphismConfig(
        tintColor = DeviceColors.TypeXR,
        cornerRadius = 16.dp,
        tintOpacity = 0.2f
    )

    val Battery = GlassMorphismConfig(
        tintColor = DeviceColors.BatteryFull,
        cornerRadius = 12.dp
    )

    val Status = GlassMorphismConfig(
        tintColor = DeviceColors.StatusConnected,
        cornerRadius = 12.dp
    )

    val Warning = GlassMorphismConfig(
        tintColor = DeviceColors.StatusWarning,
        cornerRadius = 12.dp,
        backgroundOpacity = 0.15f
    )

    val Error = GlassMorphismConfig(
        tintColor = DeviceColors.StatusError,
        cornerRadius = 12.dp,
        backgroundOpacity = 0.15f
    )
}
