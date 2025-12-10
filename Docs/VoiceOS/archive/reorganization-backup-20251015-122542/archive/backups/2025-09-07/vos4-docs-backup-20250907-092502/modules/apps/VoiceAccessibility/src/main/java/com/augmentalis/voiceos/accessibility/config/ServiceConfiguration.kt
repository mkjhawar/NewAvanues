/**
 * ServiceConfiguration.kt - Configuration for VoiceOS Accessibility Service
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-26
 * 
 * Configuration system following SR6-HYBRID patterns:
 * - Complete API (createDefault, fromMap, toMap, mergeWith, isEquivalentTo)
 * - Version migration support
 * - Full serialization/deserialization
 */
package com.augmentalis.voiceos.accessibility.config

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Configuration for VoiceOS Accessibility Service
 * Direct implementation - no interfaces (VOS4 compliance)
 */
data class ServiceConfiguration(
    // Core settings
    val isEnabled: Boolean = true,
    val verboseLogging: Boolean = false,
    val showToasts: Boolean = true,
    
    // Feature flags
    val handlersEnabled: Boolean = true,
    val appLaunchingEnabled: Boolean = true,
    val dynamicCommandsEnabled: Boolean = true,
    val cursorEnabled: Boolean = true,
    val uiScrapingEnabled: Boolean = false,  // Advanced feature, off by default
    val fingerprintGesturesEnabled: Boolean = false,
    val commandCachingEnabled: Boolean = true,
    
    // Performance settings
    val maxCacheSize: Int = 100,
    val commandTimeout: Long = 100L,  // milliseconds
    val initTimeout: Long = 1000L,    // milliseconds
    
    // Cursor settings
    val cursorSize: Float = 48f,
    val cursorColor: Int = 0xFF4285F4.toInt(),  // Google Blue
    val cursorSpeed: Float = 1.0f,
    
    // Voice Recognition settings
    val voiceRecognitionEnabled: Boolean = true,
    val voiceAutoStart: Boolean = false,           // Whether to auto-start recognition on service connect
    val voiceEngine: String = "google",            // Preferred recognition engine
    val voiceLanguage: String = "en-US",           // Recognition language
    val voiceMinConfidence: Float = 0.7f,          // Minimum confidence to accept commands
    val voiceCommandFeedback: Boolean = true,      // Show feedback for voice commands
    val showPartialResults: Boolean = false,       // Show partial recognition results
    
    // Version for migration
    val configVersion: Int = 1
) {
    
    companion object {
        private const val TAG = "ServiceConfiguration"
        private const val PREFS_NAME = "voiceos_accessibility_prefs"
        private const val CURRENT_VERSION = 1
        
        /**
         * Create default configuration
         * SR6-HYBRID pattern: createDefault()
         */
        @JvmStatic
        fun createDefault(): ServiceConfiguration {
            return ServiceConfiguration()
        }
        
        /**
         * Load configuration from SharedPreferences
         */
        @JvmStatic
        fun loadFromPreferences(context: Context): ServiceConfiguration {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            return ServiceConfiguration(
                isEnabled = prefs.getBoolean("enabled", true),
                verboseLogging = prefs.getBoolean("verbose_logging", false),
                showToasts = prefs.getBoolean("show_toasts", true),
                
                handlersEnabled = prefs.getBoolean("handlers_enabled", true),
                appLaunchingEnabled = prefs.getBoolean("app_launching", true),
                dynamicCommandsEnabled = prefs.getBoolean("dynamic_commands", true),
                cursorEnabled = prefs.getBoolean("cursor_enabled", true),
                uiScrapingEnabled = prefs.getBoolean("ui_scraping", false),
                fingerprintGesturesEnabled = prefs.getBoolean("fingerprint_gestures", false),
                commandCachingEnabled = prefs.getBoolean("command_caching", true),
                
                maxCacheSize = prefs.getInt("max_cache_size", 100),
                commandTimeout = prefs.getLong("command_timeout", 100L),
                initTimeout = prefs.getLong("init_timeout", 1000L),
                
                cursorSize = prefs.getFloat("cursor_size", 48f),
                cursorColor = prefs.getInt("cursor_color", 0xFF4285F4.toInt()),
                cursorSpeed = prefs.getFloat("cursor_speed", 1.0f),
                
                voiceRecognitionEnabled = prefs.getBoolean("voice_recognition_enabled", true),
                voiceAutoStart = prefs.getBoolean("voice_auto_start", false),
                voiceEngine = prefs.getString("voice_engine", "google") ?: "google",
                voiceLanguage = prefs.getString("voice_language", "en-US") ?: "en-US",
                voiceMinConfidence = prefs.getFloat("voice_min_confidence", 0.7f),
                voiceCommandFeedback = prefs.getBoolean("voice_command_feedback", true),
                showPartialResults = prefs.getBoolean("show_partial_results", false),
                
                configVersion = prefs.getInt("config_version", CURRENT_VERSION)
            ).migrateIfNeeded()
        }
        
        /**
         * Create configuration from map
         * SR6-HYBRID pattern: fromMap()
         */
        @JvmStatic
        fun fromMap(map: Map<String, Any>): ServiceConfiguration {
            return ServiceConfiguration(
                isEnabled = map["enabled"] as? Boolean ?: true,
                verboseLogging = map["verbose_logging"] as? Boolean ?: false,
                showToasts = map["show_toasts"] as? Boolean ?: true,
                
                handlersEnabled = map["handlers_enabled"] as? Boolean ?: true,
                appLaunchingEnabled = map["app_launching"] as? Boolean ?: true,
                dynamicCommandsEnabled = map["dynamic_commands"] as? Boolean ?: true,
                cursorEnabled = map["cursor_enabled"] as? Boolean ?: true,
                uiScrapingEnabled = map["ui_scraping"] as? Boolean ?: false,
                fingerprintGesturesEnabled = map["fingerprint_gestures"] as? Boolean ?: false,
                commandCachingEnabled = map["command_caching"] as? Boolean ?: true,
                
                maxCacheSize = (map["max_cache_size"] as? Number)?.toInt() ?: 100,
                commandTimeout = (map["command_timeout"] as? Number)?.toLong() ?: 100L,
                initTimeout = (map["init_timeout"] as? Number)?.toLong() ?: 1000L,
                
                cursorSize = (map["cursor_size"] as? Number)?.toFloat() ?: 48f,
                cursorColor = (map["cursor_color"] as? Number)?.toInt() ?: 0xFF4285F4.toInt(),
                cursorSpeed = (map["cursor_speed"] as? Number)?.toFloat() ?: 1.0f,
                
                voiceRecognitionEnabled = map["voice_recognition_enabled"] as? Boolean ?: true,
                voiceAutoStart = map["voice_auto_start"] as? Boolean ?: false,
                voiceEngine = map["voice_engine"] as? String ?: "google",
                voiceLanguage = map["voice_language"] as? String ?: "en-US",
                voiceMinConfidence = (map["voice_min_confidence"] as? Number)?.toFloat() ?: 0.7f,
                voiceCommandFeedback = map["voice_command_feedback"] as? Boolean ?: true,
                showPartialResults = map["show_partial_results"] as? Boolean ?: false,
                
                configVersion = (map["config_version"] as? Number)?.toInt() ?: CURRENT_VERSION
            )
        }
    }
    
    /**
     * Save configuration to SharedPreferences
     */
    fun saveToPreferences(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("enabled", isEnabled)
            putBoolean("verbose_logging", verboseLogging)
            putBoolean("show_toasts", showToasts)
            
            putBoolean("handlers_enabled", handlersEnabled)
            putBoolean("app_launching", appLaunchingEnabled)
            putBoolean("dynamic_commands", dynamicCommandsEnabled)
            putBoolean("cursor_enabled", cursorEnabled)
            putBoolean("ui_scraping", uiScrapingEnabled)
            putBoolean("fingerprint_gestures", fingerprintGesturesEnabled)
            putBoolean("command_caching", commandCachingEnabled)
            
            putInt("max_cache_size", maxCacheSize)
            putLong("command_timeout", commandTimeout)
            putLong("init_timeout", initTimeout)
            
            putFloat("cursor_size", cursorSize)
            putInt("cursor_color", cursorColor)
            putFloat("cursor_speed", cursorSpeed)
            
            putBoolean("voice_recognition_enabled", voiceRecognitionEnabled)
            putBoolean("voice_auto_start", voiceAutoStart)
            putString("voice_engine", voiceEngine)
            putString("voice_language", voiceLanguage)
            putFloat("voice_min_confidence", voiceMinConfidence)
            putBoolean("voice_command_feedback", voiceCommandFeedback)
            putBoolean("show_partial_results", showPartialResults)
            
            putInt("config_version", configVersion)
            
            apply()
        }
    }
    
    /**
     * Convert configuration to map
     * SR6-HYBRID pattern: toMap()
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "enabled" to isEnabled,
            "verbose_logging" to verboseLogging,
            "show_toasts" to showToasts,
            
            "handlers_enabled" to handlersEnabled,
            "app_launching" to appLaunchingEnabled,
            "dynamic_commands" to dynamicCommandsEnabled,
            "cursor_enabled" to cursorEnabled,
            "ui_scraping" to uiScrapingEnabled,
            "fingerprint_gestures" to fingerprintGesturesEnabled,
            "command_caching" to commandCachingEnabled,
            
            "max_cache_size" to maxCacheSize,
            "command_timeout" to commandTimeout,
            "init_timeout" to initTimeout,
            
            "cursor_size" to cursorSize,
            "cursor_color" to cursorColor,
            "cursor_speed" to cursorSpeed,
            
            "voice_recognition_enabled" to voiceRecognitionEnabled,
            "voice_auto_start" to voiceAutoStart,
            "voice_engine" to voiceEngine,
            "voice_language" to voiceLanguage,
            "voice_min_confidence" to voiceMinConfidence,
            "voice_command_feedback" to voiceCommandFeedback,
            "show_partial_results" to showPartialResults,
            
            "config_version" to configVersion
        )
    }
    
    /**
     * Merge with another configuration
     * SR6-HYBRID pattern: mergeWith()
     */
    fun mergeWith(other: ServiceConfiguration): ServiceConfiguration {
        // For boolean flags, prefer enabled state
        // For numeric values, use the newer configuration
        return ServiceConfiguration(
            isEnabled = isEnabled || other.isEnabled,
            verboseLogging = verboseLogging || other.verboseLogging,
            showToasts = showToasts || other.showToasts,
            
            handlersEnabled = handlersEnabled || other.handlersEnabled,
            appLaunchingEnabled = appLaunchingEnabled || other.appLaunchingEnabled,
            dynamicCommandsEnabled = dynamicCommandsEnabled || other.dynamicCommandsEnabled,
            cursorEnabled = cursorEnabled || other.cursorEnabled,
            uiScrapingEnabled = uiScrapingEnabled || other.uiScrapingEnabled,
            fingerprintGesturesEnabled = fingerprintGesturesEnabled || other.fingerprintGesturesEnabled,
            commandCachingEnabled = commandCachingEnabled || other.commandCachingEnabled,
            
            maxCacheSize = maxOf(maxCacheSize, other.maxCacheSize),
            commandTimeout = minOf(commandTimeout, other.commandTimeout),
            initTimeout = minOf(initTimeout, other.initTimeout),
            
            cursorSize = other.cursorSize,  // Use newer
            cursorColor = other.cursorColor,  // Use newer
            cursorSpeed = other.cursorSpeed,  // Use newer
            
            voiceRecognitionEnabled = voiceRecognitionEnabled || other.voiceRecognitionEnabled,
            voiceAutoStart = voiceAutoStart || other.voiceAutoStart,
            voiceEngine = other.voiceEngine,  // Use newer
            voiceLanguage = other.voiceLanguage,  // Use newer
            voiceMinConfidence = maxOf(voiceMinConfidence, other.voiceMinConfidence),  // Use higher threshold
            voiceCommandFeedback = voiceCommandFeedback || other.voiceCommandFeedback,
            showPartialResults = showPartialResults || other.showPartialResults,
            
            configVersion = maxOf(configVersion, other.configVersion)
        )
    }
    
    /**
     * Check if configurations are equivalent
     * SR6-HYBRID pattern: isEquivalentTo()
     */
    fun isEquivalentTo(other: ServiceConfiguration): Boolean {
        // Check functional equivalence, ignoring version
        return copy(configVersion = 0) == other.copy(configVersion = 0)
    }
    
    /**
     * Migrate configuration if needed
     */
    private fun migrateIfNeeded(): ServiceConfiguration {
        if (configVersion >= CURRENT_VERSION) return this
        
        Log.i(TAG, "Migrating configuration from v$configVersion to v$CURRENT_VERSION")
        
        // Apply migrations based on version
        var migrated = this
        
        // Example migration from v0 to v1
        if (configVersion < 1) {
            migrated = migrated.copy(
                // Add new fields with defaults
                fingerprintGesturesEnabled = false,
                commandCachingEnabled = true,
                configVersion = 1
            )
        }
        
        // Future migrations would go here
        // if (configVersion < 2) { ... }
        
        return migrated
    }
    
    /**
     * Validate configuration
     */
    fun validate(): Boolean {
        return when {
            maxCacheSize < 0 -> {
                Log.e(TAG, "Invalid max cache size: $maxCacheSize")
                false
            }
            commandTimeout < 0 -> {
                Log.e(TAG, "Invalid command timeout: $commandTimeout")
                false
            }
            initTimeout < 0 -> {
                Log.e(TAG, "Invalid init timeout: $initTimeout")
                false
            }
            cursorSize < 0 -> {
                Log.e(TAG, "Invalid cursor size: $cursorSize")
                false
            }
            cursorSpeed < 0 -> {
                Log.e(TAG, "Invalid cursor speed: $cursorSpeed")
                false
            }
            voiceMinConfidence < 0.0f || voiceMinConfidence > 1.0f -> {
                Log.e(TAG, "Invalid voice min confidence: $voiceMinConfidence (must be 0.0-1.0)")
                false
            }
            voiceEngine.isBlank() -> {
                Log.e(TAG, "Voice engine cannot be blank")
                false
            }
            voiceLanguage.isBlank() -> {
                Log.e(TAG, "Voice language cannot be blank")
                false
            }
            else -> true
        }
    }
}