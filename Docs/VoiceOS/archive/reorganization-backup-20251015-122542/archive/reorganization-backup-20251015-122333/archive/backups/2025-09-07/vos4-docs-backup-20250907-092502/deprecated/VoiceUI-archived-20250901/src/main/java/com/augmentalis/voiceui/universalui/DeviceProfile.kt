/**
 * DeviceProfile.kt - Unified device profile data model for adaptive UI
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-08-24
 * 
 * Merged from multiple definitions to preserve all functionality
 */
package com.augmentalis.voiceui.universalui

import android.os.Build

/**
 * Unified device profile containing all device information for adaptive UI rendering
 * Combines properties from both AdaptiveVoiceUI and standalone DeviceProfile
 * 
 * @property type The type of device (phone, tablet, TV, etc.)
 * @property manufacturer Device manufacturer (Samsung, Google, etc.)
 * @property model Device model name
 * @property capabilities Set of device capability strings
 * @property os The operating system name (Android, iOS, etc.)
 * @property osVersion The OS version number
 * @property screenWidth Screen width in pixels
 * @property screenHeight Screen height in pixels
 * @property density Screen density (DPI)
 * @property isPortrait Whether device is in portrait orientation
 * @property supportsMultiTouch Whether device supports multi-touch
 * @property supportsHaptics Whether device supports haptic feedback
 * @property hasPhysicalKeyboard Whether device has physical keyboard
 * @property hasPointer Whether device has pointer/mouse input
 */
data class DeviceProfile(
    val type: DeviceType,
    val manufacturer: String = Build.MANUFACTURER,
    val model: String = Build.MODEL,
    val capabilities: Set<String> = emptySet(),
    val os: String = "Android",
    val osVersion: Int = Build.VERSION.SDK_INT,
    val screenWidth: Int = 0,
    val screenHeight: Int = 0,
    val density: Float = 1.0f,
    val isPortrait: Boolean = true,
    val supportsMultiTouch: Boolean = true,
    val supportsHaptics: Boolean = true,
    val hasPhysicalKeyboard: Boolean = false,
    val hasPointer: Boolean = false,
    // Additional fields from DeviceType.kt to maintain 100% functionality
    val screenSizeDp: Int = 360,
    val densityDpi: Int = 420,
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
) {
    
    /**
     * Check if device is running Material 3 capable OS
     */
    val isMaterial3Capable: Boolean
        get() = os.contains("Android", ignoreCase = true) && osVersion >= 31
    
    /**
     * Check if device is a mobile device (phone or tablet)
     */
    val isMobile: Boolean
        get() = type == DeviceType.PHONE || type == DeviceType.TABLET
    
    /**
     * Check if device is an XR device (AR/VR)
     */
    val isXR: Boolean
        get() = type == DeviceType.SMART_GLASSES || type == DeviceType.VR_DEVICE
    
    /**
     * Get recommended UI scale factor for device
     */
    val uiScaleFactor: Float
        get() = when (type) {
            DeviceType.TV -> 1.5f
            DeviceType.WATCH -> 0.75f
            DeviceType.VR_DEVICE -> 1.2f
            else -> 1.0f
        }
    
    companion object {
        /**
         * Create a default device profile for current device
         */
        fun default(): DeviceProfile {
            return DeviceProfile(
                type = DeviceType.PHONE,
                os = "Android",
                osVersion = android.os.Build.VERSION.SDK_INT
            )
        }
        
        /**
         * Create device profile for phone
         */
        fun phone(): DeviceProfile {
            return DeviceProfile(
                type = DeviceType.PHONE,
                supportsMultiTouch = true,
                supportsHaptics = true
            )
        }
        
        /**
         * Create device profile for tablet
         */
        fun tablet(): DeviceProfile {
            return DeviceProfile(
                type = DeviceType.TABLET,
                screenWidth = 1920,
                screenHeight = 1080,
                supportsMultiTouch = true
            )
        }
        
        /**
         * Create device profile for smart glasses
         */
        fun smartGlasses(): DeviceProfile {
            return DeviceProfile(
                type = DeviceType.SMART_GLASSES,
                supportsMultiTouch = false,
                supportsHaptics = false,
                hasPointer = true
            )
        }
        
        /**
         * Create device profile for VR device
         */
        fun vrDevice(): DeviceProfile {
            return DeviceProfile(
                type = DeviceType.VR_DEVICE,
                supportsMultiTouch = false,
                supportsHaptics = true,
                hasPointer = true
            )
        }
    }
}
