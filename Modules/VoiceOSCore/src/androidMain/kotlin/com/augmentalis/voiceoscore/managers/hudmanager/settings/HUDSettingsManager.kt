/**
 * HUDSettingsManager.kt
 * Path: /managers/HUDManager/src/main/java/com/augmentalis/hudmanager/settings/HUDSettingsManager.kt
 * 
 * Created: 2025-01-24
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Manages HUD settings persistence, validation, and updates
 * Handles voice commands for settings control
 */

package com.augmentalis.voiceoscore.managers.hudmanager.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Manages HUD settings with persistence and real-time updates
 */
class HUDSettingsManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: HUDSettingsManager? = null
        private const val PREFS_NAME = "hud_settings"
        private const val KEY_SETTINGS = "settings_json"
        
        fun getInstance(context: Context): HUDSettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HUDSettingsManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // Observable settings state
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<HUDSettings> = _settings.asStateFlow()
    
    // Current settings value
    val currentSettings: HUDSettings
        get() = _settings.value
    
    /**
     * Load settings from persistent storage
     */
    private fun loadSettings(): HUDSettings {
        return try {
            val jsonString = prefs.getString(KEY_SETTINGS, null)
            if (jsonString != null) {
                json.decodeFromString<HUDSettings>(jsonString)
            } else {
                HUDSettings.DEFAULT
            }
        } catch (e: Exception) {
            HUDSettings.DEFAULT
        }
    }
    
    /**
     * Save settings to persistent storage
     */
    fun saveSettings(settings: HUDSettings) {
        val jsonString = json.encodeToString(settings)
        prefs.edit().putString(KEY_SETTINGS, jsonString).apply()
        _settings.value = settings
    }
    
    /**
     * Update specific setting
     */
    fun updateSettings(block: HUDSettings.() -> HUDSettings) {
        val newSettings = currentSettings.block()
        saveSettings(newSettings)
    }
    
    /**
     * Toggle HUD on/off
     */
    fun toggleHUD(enabled: Boolean? = null) {
        updateSettings {
            copy(hudEnabled = enabled ?: !hudEnabled)
        }
    }
    
    /**
     * Set display mode
     */
    fun setDisplayMode(mode: HUDDisplayMode) {
        updateSettings {
            copy(displayMode = mode)
        }
    }
    
    /**
     * Toggle specific display element
     */
    fun toggleDisplayElement(element: DisplayElement) {
        updateSettings {
            copy(displayElements = displayElements.toggle(element))
        }
    }
    
    /**
     * Apply preset configuration
     */
    fun applyPreset(preset: HUDPreset) {
        val presetSettings = when (preset) {
            HUDPreset.MINIMAL -> HUDSettings.MINIMAL
            HUDPreset.DRIVING -> HUDSettings.DRIVING
            HUDPreset.PRIVACY -> HUDSettings.PRIVACY
            HUDPreset.DEFAULT -> HUDSettings.DEFAULT
            HUDPreset.CUSTOM -> currentSettings // Keep current
        }
        saveSettings(presetSettings)
    }
    
    /**
     * Adjust visual settings
     */
    fun adjustTransparency(value: Float) {
        updateSettings {
            copy(visual = visual.copy(transparency = value.coerceIn(0f, 1f)))
        }
    }
    
    fun adjustBrightness(value: Float) {
        updateSettings {
            copy(visual = visual.copy(brightness = value.coerceIn(0.5f, 2f)))
        }
    }
    
    fun setColorTheme(theme: ColorTheme) {
        updateSettings {
            copy(visual = visual.copy(colorTheme = theme))
        }
    }
    
    /**
     * Privacy controls
     */
    fun enablePrivacyMode(enabled: Boolean = true) {
        updateSettings {
            copy(privacy = privacy.copy(
                hideInPublic = enabled,
                blurSensitiveContent = enabled,
                disableInMeetings = enabled
            ))
        }
    }
    
    /**
     * Performance optimization
     */
    fun setPerformanceMode(mode: PerformanceMode) {
        val performanceSettings = when (mode) {
            PerformanceMode.BATTERY_SAVER -> PerformanceSettings(
                targetFps = 30,
                batteryOptimization = true,
                adaptiveQuality = true,
                particleEffects = false,
                shadowQuality = ShadowQuality.OFF
            )
            PerformanceMode.BALANCED -> PerformanceSettings(
                targetFps = 60,
                batteryOptimization = true,
                adaptiveQuality = true
            )
            PerformanceMode.PERFORMANCE -> PerformanceSettings(
                targetFps = 120,
                batteryOptimization = false,
                adaptiveQuality = false,
                shadowQuality = ShadowQuality.ULTRA,
                textureQuality = TextureQuality.ULTRA
            )
        }
        updateSettings {
            copy(performance = performanceSettings)
        }
    }
    
    /**
     * Handle voice commands for settings
     */
    fun handleVoiceCommand(command: String): Boolean {
        val normalizedCommand = command.lowercase().trim()
        
        return when {
            // Master controls
            "turn off hud" in normalizedCommand || "disable hud" in normalizedCommand -> {
                toggleHUD(false)
                true
            }
            "turn on hud" in normalizedCommand || "enable hud" in normalizedCommand -> {
                toggleHUD(true)
                true
            }
            
            // Display modes
            "minimal mode" in normalizedCommand || "minimal display" in normalizedCommand -> {
                setDisplayMode(HUDDisplayMode.MINIMAL)
                true
            }
            "full display" in normalizedCommand || "show everything" in normalizedCommand -> {
                setDisplayMode(HUDDisplayMode.FULL)
                true
            }
            "contextual mode" in normalizedCommand -> {
                setDisplayMode(HUDDisplayMode.CONTEXTUAL)
                true
            }
            
            // Privacy
            "privacy mode" in normalizedCommand || "enable privacy" in normalizedCommand -> {
                enablePrivacyMode(true)
                true
            }
            "disable privacy" in normalizedCommand -> {
                enablePrivacyMode(false)
                true
            }
            
            // Visual adjustments
            "increase transparency" in normalizedCommand -> {
                adjustTransparency(currentSettings.visual.transparency + 0.1f)
                true
            }
            "decrease transparency" in normalizedCommand -> {
                adjustTransparency(currentSettings.visual.transparency - 0.1f)
                true
            }
            "brighter" in normalizedCommand || "increase brightness" in normalizedCommand -> {
                adjustBrightness(currentSettings.visual.brightness + 0.1f)
                true
            }
            "darker" in normalizedCommand || "decrease brightness" in normalizedCommand -> {
                adjustBrightness(currentSettings.visual.brightness - 0.1f)
                true
            }
            
            // Theme
            "dark mode" in normalizedCommand || "dark theme" in normalizedCommand -> {
                setColorTheme(ColorTheme.DARK)
                true
            }
            "light mode" in normalizedCommand || "light theme" in normalizedCommand -> {
                setColorTheme(ColorTheme.LIGHT)
                true
            }
            
            // Display elements
            "hide notifications" in normalizedCommand -> {
                toggleDisplayElement(DisplayElement.NOTIFICATIONS)
                true
            }
            "show notifications" in normalizedCommand -> {
                toggleDisplayElement(DisplayElement.NOTIFICATIONS)
                true
            }
            "hide time" in normalizedCommand -> {
                toggleDisplayElement(DisplayElement.TIME)
                true
            }
            "show battery" in normalizedCommand -> {
                toggleDisplayElement(DisplayElement.BATTERY)
                true
            }
            
            // Performance
            "battery saver" in normalizedCommand || "save battery" in normalizedCommand -> {
                setPerformanceMode(PerformanceMode.BATTERY_SAVER)
                true
            }
            "performance mode" in normalizedCommand || "maximum performance" in normalizedCommand -> {
                setPerformanceMode(PerformanceMode.PERFORMANCE)
                true
            }
            
            // Presets
            "driving mode" in normalizedCommand -> {
                applyPreset(HUDPreset.DRIVING)
                true
            }
            "reset settings" in normalizedCommand || "default settings" in normalizedCommand -> {
                applyPreset(HUDPreset.DEFAULT)
                true
            }
            
            else -> false
        }
    }
    
    /**
     * Reset to default settings
     */
    fun resetToDefaults() {
        saveSettings(HUDSettings.DEFAULT)
    }
    
    /**
     * Export settings as JSON string
     */
    fun exportSettings(): String {
        return json.encodeToString(currentSettings)
    }
    
    /**
     * Import settings from JSON string
     */
    fun importSettings(jsonString: String): Boolean {
        return try {
            val imported = json.decodeFromString<HUDSettings>(jsonString)
            saveSettings(imported)
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Display elements that can be toggled
 */
enum class DisplayElement {
    BATTERY,
    TIME,
    DATE,
    NOTIFICATIONS,
    MESSAGES,
    VOICE_COMMANDS,
    GAZE_TARGET,
    NAVIGATION,
    COMPASS,
    SYSTEM_INFO
}

/**
 * Preset configurations
 */
enum class HUDPreset {
    DEFAULT,
    MINIMAL,
    DRIVING,
    PRIVACY,
    CUSTOM
}

/**
 * Performance modes
 */
enum class PerformanceMode {
    BATTERY_SAVER,
    BALANCED,
    PERFORMANCE
}

/**
 * Extension function to toggle display elements
 */
private fun DisplayElements.toggle(element: DisplayElement): DisplayElements {
    return when (element) {
        DisplayElement.BATTERY -> copy(batteryStatus = !batteryStatus)
        DisplayElement.TIME -> copy(time = !time)
        DisplayElement.DATE -> copy(date = !date)
        DisplayElement.NOTIFICATIONS -> copy(notifications = !notifications)
        DisplayElement.MESSAGES -> copy(messages = !messages)
        DisplayElement.VOICE_COMMANDS -> copy(voiceCommands = !voiceCommands)
        DisplayElement.GAZE_TARGET -> copy(gazeTarget = !gazeTarget)
        DisplayElement.NAVIGATION -> copy(navigationHints = !navigationHints)
        DisplayElement.COMPASS -> copy(compass = !compass)
        DisplayElement.SYSTEM_INFO -> copy(systemDiagnostics = !systemDiagnostics)
    }
}