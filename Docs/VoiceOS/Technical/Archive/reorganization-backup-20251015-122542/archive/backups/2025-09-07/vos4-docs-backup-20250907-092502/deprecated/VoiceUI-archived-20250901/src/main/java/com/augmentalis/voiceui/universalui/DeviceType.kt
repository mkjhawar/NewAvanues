/**
 * DeviceType.kt - Unified device type enumeration for adaptive UI
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-08-24
 * 
 * Merged all DeviceType values to maintain 100% functional equivalency
 */

package com.augmentalis.voiceui.universalui

/**
 * Unified device types for adaptive UI system
 * Combines all values from multiple definitions - no values removed
 */
enum class DeviceType {
    PHONE,           // Smartphones (< 7 inches)
    TABLET,          // Tablets (7-12 inches)
    DESKTOP,         // Desktop computers
    TV,              // Television screens
    WATCH,           // Smartwatches
    AR_GLASSES,      // AR/VR headsets and glasses (kept separate from AR_DEVICE)
    SMART_GLASSES,   // Smart glasses (lighter AR)
    AR_DEVICE,       // AR devices (from AdaptiveVoiceUI)
    VR_DEVICE,       // Virtual Reality headsets
    CAR,             // Car displays (same as AUTO)
    AUTO,            // Automotive displays (alternative to CAR)
    AUTOMOTIVE,      // From AdaptiveVoiceUI (another automotive variant)
    FOLDABLE,        // Foldable devices
    WEARABLE,        // Other wearable devices
    IOT,             // IoT devices with displays
    NEURAL_INTERFACE,// Neural interface devices (from AdaptiveVoiceUI)
    KIOSK,           // Kiosk displays
    UNKNOWN,         // Unknown device type (from AdaptiveVoiceUI)
    CUSTOM,          // Custom device types
    OTHER            // Other unspecified devices
}

// DeviceProfile has been moved to DeviceProfile.kt to avoid duplication
// The duplicate DeviceProfile definition below has been removed

/*
data class DeviceProfile(
    val type: DeviceType,
    val screenSizeDp: Int,
    val densityDpi: Int,
    val supportsTouch: Boolean = true,
    val supportsVoice: Boolean = true,
    val supportsGesture: Boolean = false,
    val supportsEyeGaze: Boolean = false,
    val supportsHandTracking: Boolean = false,
    val supportsControllers: Boolean = false,
    val supports6DOF: Boolean = false,
    val supportsDPad: Boolean = false,
    val supportsRemoteControl: Boolean = false,
    val supportsDigitalCrown: Boolean = false,
    val supportsSwipeGestures: Boolean = true,
    val minHeight: Float = 48f,
    val minWidth: Float = 48f,
    val focusedBorderWidth: Float = 2f,
    val focusedBorderColor: Long = 0xFF000000,
    val gazeDwellTime: Long = 1000L
)
*/

/**
 * Helper functions for device detection
 * Note: DeviceProfile is imported from DeviceProfile.kt
 */
object DeviceTypeDetector {
    fun detectDeviceType(screenSizeDp: Int, densityDpi: Int): DeviceType {
        return when {
            screenSizeDp < 360 -> DeviceType.WATCH
            screenSizeDp < 600 -> DeviceType.PHONE
            screenSizeDp < 840 -> DeviceType.TABLET
            screenSizeDp < 1280 -> DeviceType.DESKTOP
            else -> DeviceType.TV
        }
    }
    
    fun getDefaultProfile(type: DeviceType): DeviceProfile {
        return when (type) {
            DeviceType.PHONE -> DeviceProfile(
                type = DeviceType.PHONE,
                screenSizeDp = 360,
                densityDpi = 420,
                supportsTouch = true,
                supportsVoice = true
            )
            DeviceType.TABLET -> DeviceProfile(
                type = DeviceType.TABLET,
                screenSizeDp = 768,
                densityDpi = 320,
                supportsTouch = true,
                supportsVoice = true
            )
            DeviceType.DESKTOP -> DeviceProfile(
                type = DeviceType.DESKTOP,
                screenSizeDp = 1920,
                densityDpi = 96,
                supportsTouch = false,
                supportsVoice = true
            )
            DeviceType.TV -> DeviceProfile(
                type = DeviceType.TV,
                screenSizeDp = 1920,
                densityDpi = 72,
                supportsTouch = false,
                supportsVoice = true,
                supportsDPad = true,
                supportsRemoteControl = true,
                minHeight = 56f,
                minWidth = 56f
            )
            DeviceType.WATCH -> DeviceProfile(
                type = DeviceType.WATCH,
                screenSizeDp = 280,
                densityDpi = 320,
                supportsTouch = true,
                supportsVoice = true,
                supportsDigitalCrown = true,
                minHeight = 40f,
                minWidth = 40f
            )
            DeviceType.AR_GLASSES -> DeviceProfile(
                type = DeviceType.AR_GLASSES,
                screenSizeDp = 1280,
                densityDpi = 72,
                supportsTouch = false,
                supportsVoice = true,
                supportsGesture = true,
                supportsEyeGaze = true,
                supportsHandTracking = true,
                supports6DOF = true
            )
            else -> DeviceProfile(
                type = type,
                screenSizeDp = 360,
                densityDpi = 420
            )
        }
    }
}
