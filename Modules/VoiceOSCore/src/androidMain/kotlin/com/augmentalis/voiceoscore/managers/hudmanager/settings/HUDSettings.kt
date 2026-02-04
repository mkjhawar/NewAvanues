/**
 * HUDSettings.kt
 * Path: /managers/HUDManager/src/main/java/com/augmentalis/hudmanager/settings/HUDSettings.kt
 * 
 * Created: 2025-01-24
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: User preferences and configuration for HUD display system
 * Provides comprehensive control over AR/smart glasses display elements
 */

package com.augmentalis.voiceoscore.managers.hudmanager.settings

import kotlinx.serialization.Serializable

/**
 * Master HUD configuration settings
 * Provides full control over what and how information is displayed
 */
@Serializable
data class HUDSettings(
    // Master Controls
    val hudEnabled: Boolean = true,
    val displayMode: HUDDisplayMode = HUDDisplayMode.CONTEXTUAL,
    
    // Display Elements (toggleable)
    val displayElements: DisplayElements = DisplayElements(),
    
    // Positioning & Layout
    val positioning: PositioningSettings = PositioningSettings(),
    
    // Visual Preferences
    val visual: VisualSettings = VisualSettings(),
    
    // Privacy & Security
    val privacy: PrivacySettings = PrivacySettings(),
    
    // Performance
    val performance: PerformanceSettings = PerformanceSettings(),
    
    // Accessibility
    val accessibility: AccessibilitySettings = AccessibilitySettings()
) {
    companion object {
        val DEFAULT = HUDSettings()
        
        // Preset configurations
        val MINIMAL = HUDSettings(
            displayMode = HUDDisplayMode.MINIMAL,
            displayElements = DisplayElements(
                batteryStatus = true,
                time = true,
                notifications = false,
                voiceCommands = false,
                gazeTarget = false,
                contextualInfo = false
            )
        )
        
        val DRIVING = HUDSettings(
            displayMode = HUDDisplayMode.CUSTOM,
            displayElements = DisplayElements(
                navigationHints = true,
                speedInfo = true,
                voiceCommands = true,
                notifications = false
            ),
            visual = VisualSettings(
                brightness = 1.2f,
                contrast = 1.3f
            )
        )
        
        val PRIVACY = HUDSettings(
            privacy = PrivacySettings(
                hideInPublic = true,
                blurSensitiveContent = true,
                disableInMeetings = true,
                disableScreenshots = true
            )
        )
    }
}

/**
 * HUD display modes for different use cases
 */
@Serializable
enum class HUDDisplayMode {
    OFF,           // Completely disabled
    MINIMAL,       // Only critical info (battery, time)
    CONTEXTUAL,    // Show based on context/activity
    FULL,          // All available information
    CUSTOM,        // User-defined elements
    DRIVING,       // Optimized for driving
    WORK,          // Optimized for work environment
    FITNESS,       // Optimized for exercise
    PRIVACY        // Privacy-focused mode
}

/**
 * Individual display elements that can be toggled
 */
@Serializable
data class DisplayElements(
    // System Information
    val batteryStatus: Boolean = true,
    val time: Boolean = true,
    val date: Boolean = false,
    val networkStatus: Boolean = false,
    
    // Interaction Elements
    val voiceCommands: Boolean = true,
    val gazeTarget: Boolean = true,
    val gestureHints: Boolean = false,
    
    // Notifications & Messages
    val notifications: Boolean = true,
    val messages: Boolean = true,
    val calls: Boolean = true,
    val alerts: Boolean = true,
    
    // Contextual Information
    val contextualInfo: Boolean = true,
    val appSuggestions: Boolean = false,
    val smartReplies: Boolean = false,
    
    // Navigation & Location
    val navigationHints: Boolean = false,
    val compass: Boolean = false,
    val speedInfo: Boolean = false,
    val miniMap: Boolean = false,
    
    // Advanced
    val systemDiagnostics: Boolean = false,
    val developerInfo: Boolean = false,
    val performanceMetrics: Boolean = false
)

/**
 * Positioning and layout configuration
 */
@Serializable
data class PositioningSettings(
    val hudDistance: Float = 2.0f,        // Meters from eyes
    val verticalOffset: Float = 0f,       // Degrees up/down
    val horizontalOffset: Float = 0f,     // Degrees left/right
    val textSize: TextSize = TextSize.MEDIUM,
    val iconSize: IconSize = IconSize.MEDIUM,
    val layout: LayoutStyle = LayoutStyle.CENTERED,
    val anchorPoint: AnchorPoint = AnchorPoint.CENTER,
    val autoAdjust: Boolean = true        // Auto-adjust based on head position
)

/**
 * Visual appearance settings
 */
@Serializable
data class VisualSettings(
    val transparency: Float = 0.8f,       // 0.0 = invisible, 1.0 = opaque
    val brightness: Float = 1.0f,         // Display brightness multiplier
    val contrast: Float = 1.0f,           // Contrast adjustment
    val colorTheme: ColorTheme = ColorTheme.AUTO,
    val accentColor: String = "#2196F3",  // Hex color for accents
    val fontSize: Float = 1.0f,           // Font size multiplier
    val animations: Boolean = true,
    val smoothTransitions: Boolean = true,
    val antiAliasing: Boolean = true,
    val nightMode: Boolean = false,       // Force night mode
    val highContrast: Boolean = false     // High contrast for visibility
)

/**
 * Privacy and security settings
 */
@Serializable
data class PrivacySettings(
    val hideInPublic: Boolean = false,           // Auto-hide in public spaces
    val blurSensitiveContent: Boolean = true,    // Blur passwords, credit cards
    val disableInMeetings: Boolean = true,       // Auto-disable during meetings
    val disableScreenshots: Boolean = false,     // Prevent screenshots
    val incognitoMode: Boolean = false,          // No history/logging
    val hiddenApps: List<String> = emptyList(),  // Apps to never show
    val trustedNetworks: List<String> = emptyList(), // Networks where full display is OK
    val requireAuthentication: Boolean = false    // Require auth to show HUD
)

/**
 * Performance optimization settings
 */
@Serializable
data class PerformanceSettings(
    val targetFps: Int = 60,                     // Target frame rate
    val batteryOptimization: Boolean = true,     // Reduce features when battery low
    val adaptiveQuality: Boolean = true,         // Adjust quality based on performance
    val maxRenderDistance: Float = 10f,          // Maximum render distance in meters
    val particleEffects: Boolean = true,         // Enable particle effects
    val shadowQuality: ShadowQuality = ShadowQuality.MEDIUM,
    val textureQuality: TextureQuality = TextureQuality.HIGH
)

/**
 * Accessibility settings for users with different needs
 */
@Serializable
data class AccessibilitySettings(
    val voiceAnnouncements: Boolean = false,     // Announce HUD changes
    val hapticFeedback: Boolean = true,          // Vibration feedback
    val colorBlindMode: ColorBlindMode = ColorBlindMode.OFF,
    val largeText: Boolean = false,              // Force large text
    val boldText: Boolean = false,               // Bold all text
    val reduceMotion: Boolean = false,           // Reduce animations
    val screenReader: Boolean = false,           // TalkBack integration
    val magnification: Float = 1.0f,             // Display magnification
    val highContrastText: Boolean = false        // High contrast text
)

// Enums for settings

@Serializable
enum class TextSize {
    TINY,
    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE
}

@Serializable
enum class IconSize {
    SMALL,
    MEDIUM,
    LARGE
}

@Serializable
enum class LayoutStyle {
    CENTERED,      // Center of view
    TOP_LEFT,      // Top-left corner
    TOP_RIGHT,     // Top-right corner
    BOTTOM,        // Bottom bar
    FLOATING,      // Float around gaze
    PERIPHERAL,    // Peripheral vision
    CUSTOM         // User-defined positions
}

@Serializable
enum class AnchorPoint {
    CENTER,
    HEAD_LOCKED,   // Follows head movement
    WORLD_LOCKED,  // Stays in world position
    GAZE_LOCKED    // Follows eye gaze
}

@Serializable
enum class ColorTheme {
    AUTO,          // System default
    LIGHT,
    DARK,
    HIGH_CONTRAST,
    BLUE_LIGHT_FILTER,
    CUSTOM
}

@Serializable
enum class ShadowQuality {
    OFF,
    LOW,
    MEDIUM,
    HIGH,
    ULTRA
}

@Serializable
enum class TextureQuality {
    LOW,
    MEDIUM,
    HIGH,
    ULTRA
}

@Serializable
enum class ColorBlindMode {
    OFF,
    PROTANOPIA,    // Red-blind
    DEUTERANOPIA,  // Green-blind
    TRITANOPIA,    // Blue-blind
    MONOCHROME     // Complete color blindness
}