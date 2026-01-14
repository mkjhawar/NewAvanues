/**
 * VoiceUIConfig.kt - Configuration for VoiceUI module
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-08-24
 */
package com.augmentalis.voiceui.config

import com.augmentalis.voiceui.universalui.DeviceType

/**
 * VoiceUI configuration
 */
data class VoiceUIConfig(
    // Performance settings
    val enableHotReload: Boolean = false,
    val enableCaching: Boolean = true,
    val maxCacheSize: Int = 100,
    val preWarmCache: Boolean = true,
    
    // Voice settings
    val enableVoiceCommands: Boolean = true,
    val voiceLanguage: String = "en-US",
    val voiceTimeout: Long = 5000L,
    val enableVoiceFeedback: Boolean = true,
    val voiceConfidenceThreshold: Float = 0.7f,
    
    // Gesture settings
    val enableGestures: Boolean = true,
    val enableMultiTouch: Boolean = true,
    val enableSpatialGestures: Boolean = false,
    val gestureDebounce: Long = 300L,
    
    // Theme settings
    val defaultTheme: String = "material",
    val enableDarkMode: Boolean = true,
    val enableAdaptiveThemes: Boolean = true,
    
    // Accessibility settings
    val enableAccessibility: Boolean = true,
    val enableScreenReader: Boolean = true,
    val enableKeyboardNavigation: Boolean = true,
    val minimumTouchTarget: Float = 48f,
    
    // Performance optimization
    val targetFPS: Int = 60,
    val enableGPUAcceleration: Boolean = true,
    val enableParallelProcessing: Boolean = true,
    val maxThreads: Int = 4,
    
    // Device adaptation
    val enableAutoAdaptation: Boolean = true,
    val deviceOverrides: Map<DeviceType, DeviceConfig> = emptyMap(),
    
    // AI features
    val enableAIAssistance: Boolean = true,
    val enableSmartSuggestions: Boolean = true,
    val enableContextAwareness: Boolean = true,
    
    // Developer options
    val debugMode: Boolean = false,
    val enableLogging: Boolean = true,
    val logLevel: LogLevel = LogLevel.INFO,
    val enablePerformanceMonitoring: Boolean = false
) {
    
    companion object {
        /**
         * Default configuration
         */
        fun default() = VoiceUIConfig()
        
        /**
         * Performance-optimized configuration
         */
        fun performance() = VoiceUIConfig(
            enableCaching = true,
            maxCacheSize = 200,
            preWarmCache = true,
            enableGPUAcceleration = true,
            enableParallelProcessing = true,
            targetFPS = 120,
            enableVoiceFeedback = false,
            enableLogging = false
        )
        
        /**
         * Battery-optimized configuration
         */
        fun battery() = VoiceUIConfig(
            targetFPS = 30,
            enableGPUAcceleration = false,
            enableParallelProcessing = false,
            maxThreads = 2,
            enableVoiceFeedback = false,
            enablePerformanceMonitoring = false
        )
        
        /**
         * Accessibility-focused configuration
         */
        fun accessibility() = VoiceUIConfig(
            enableAccessibility = true,
            enableScreenReader = true,
            enableKeyboardNavigation = true,
            minimumTouchTarget = 56f,
            enableVoiceFeedback = true,
            voiceTimeout = 10000L,
            gestureDebounce = 500L
        )
        
        /**
         * Developer configuration
         */
        fun development() = VoiceUIConfig(
            debugMode = true,
            enableLogging = true,
            logLevel = LogLevel.DEBUG,
            enablePerformanceMonitoring = true,
            enableHotReload = true
        )
        
        /**
         * Smart glasses configuration
         */
        fun smartGlasses() = VoiceUIConfig(
            enableSpatialGestures = true,
            enableVoiceCommands = true,
            enableMultiTouch = false,
            targetFPS = 90,
            enableGPUAcceleration = true,
            defaultTheme = "arvision"
        )
        
        /**
         * VR/AR configuration
         */
        fun xr() = VoiceUIConfig(
            enableSpatialGestures = true,
            targetFPS = 120,
            enableGPUAcceleration = true,
            enableParallelProcessing = true,
            defaultTheme = "spatial"
        )
    }
    
    /**
     * Merge with device-specific config
     */
    fun withDeviceConfig(deviceType: DeviceType): VoiceUIConfig {
        val deviceConfig = deviceOverrides[deviceType] ?: return this
        
        return copy(
            targetFPS = deviceConfig.targetFPS ?: targetFPS,
            enableGestures = deviceConfig.enableGestures ?: enableGestures,
            defaultTheme = deviceConfig.theme ?: defaultTheme
        )
    }
    
    /**
     * Validate configuration
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (targetFPS < 30 || targetFPS > 240) {
            errors.add("Target FPS must be between 30 and 240")
        }
        
        if (maxCacheSize < 0) {
            errors.add("Max cache size cannot be negative")
        }
        
        if (voiceConfidenceThreshold < 0 || voiceConfidenceThreshold > 1) {
            errors.add("Voice confidence threshold must be between 0 and 1")
        }
        
        if (minimumTouchTarget < 44) {
            errors.add("Minimum touch target should be at least 44dp for accessibility")
        }
        
        return errors
    }
}

/**
 * Device-specific configuration overrides
 */
data class DeviceConfig(
    val targetFPS: Int? = null,
    val enableGestures: Boolean? = null,
    val theme: String? = null,
    val customSettings: Map<String, Any> = emptyMap()
)

/**
 * Log levels
 */
enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    NONE
}
