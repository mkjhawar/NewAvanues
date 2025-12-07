/**
 * ThemeEngine.kt - Theme management system for VoiceUI
 */

package com.augmentalis.voiceui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Theme engine for VoiceUI with support for multiple theme styles
 */
class ThemeEngine(private val context: Context) {
    
    companion object {
        const val THEME_ARVISION = "arvision"
        const val THEME_MATERIAL = "material"
        const val THEME_VISIONOS = "visionos"
        const val THEME_CUSTOM = "custom"
    }
    
    data class Theme(
        val name: String,
        val description: String,
        val primaryColor: String,
        val secondaryColor: String = "",
        val backgroundColor: String = "",
        val textColor: String = ""
    ) {
        fun toJson(): String = ""
    }
    
    // Theme state
    private val _currentTheme = MutableStateFlow(THEME_ARVISION)
    val currentTheme: StateFlow<String> = _currentTheme
    val themeFlow: StateFlow<Theme?> get() = MutableStateFlow(getTheme(_currentTheme.value))
    
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode
    
    private val themes = mutableMapOf<String, Theme>()
    
    /**
     * Set the active theme
     */
    fun setTheme(themeName: String, animate: Boolean = true) {
        _currentTheme.value = themeName
    }
    
    fun getCurrentTheme(): String = _currentTheme.value
    
    fun getTheme(name: String): Theme? = themes[name] ?: Theme(
        name = name,
        description = "$name theme",
        primaryColor = "#007AFF"
    )
    
    fun getAvailableThemes(): List<Theme> = listOf(
        Theme(THEME_ARVISION, "AR Vision optimized", "#00D4FF"),
        Theme(THEME_MATERIAL, "Material Design", "#6200EE"),
        Theme(THEME_VISIONOS, "VisionOS style", "#007AFF")
    )
    
    fun registerCustomTheme(name: String, config: String) {
        themes[name] = Theme(name, "Custom theme", "#007AFF")
    }
    
    /**
     * Toggle dark mode
     */
    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }
    
    /**
     * Main theme composable
     */
    @Composable
    fun VoiceUITheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit
    ) {
        val theme by currentTheme.collectAsState()
        val colorScheme = when (theme) {
            THEME_ARVISION -> getARVisionColorScheme(darkTheme)
            THEME_MATERIAL -> getMaterialColorScheme(darkTheme)
            THEME_VISIONOS -> getVisionOSColorScheme(darkTheme)
            THEME_CUSTOM -> getCustomColorScheme(darkTheme)
            else -> getARVisionColorScheme(darkTheme)
        }
        
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getTypography(),
            content = content
        )
    }
    
    /**
     * ARVision color scheme - spatial computing optimized
     */
    private fun getARVisionColorScheme(darkTheme: Boolean): ColorScheme {
        return if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFF00D4FF),
                onPrimary = Color(0xFF003544),
                primaryContainer = Color(0xFF004E63),
                onPrimaryContainer = Color(0xFF8EEFFF),
                secondary = Color(0xFF82F7FF),
                onSecondary = Color(0xFF003739),
                secondaryContainer = Color(0xFF004F52),
                onSecondaryContainer = Color(0xFFA6F7FF),
                tertiary = Color(0xFFB8C7FF),
                onTertiary = Color(0xFF1F2F67),
                tertiaryContainer = Color(0xFF37477F),
                onTertiaryContainer = Color(0xFFDBE0FF),
                background = Color(0xFF0A0F14),
                onBackground = Color(0xFFE0E3E8),
                surface = Color(0xFF0A0F14),
                onSurface = Color(0xFFE0E3E8),
                surfaceVariant = Color(0xFF3F4947),
                onSurfaceVariant = Color(0xFFBFC9C7),
                outline = Color(0xFF899391)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF006782),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFF8EEFFF),
                onPrimaryContainer = Color(0xFF001F28),
                secondary = Color(0xFF006A6E),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFA6F7FF),
                onSecondaryContainer = Color(0xFF002022),
                tertiary = Color(0xFF4F5F97),
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFDBE0FF),
                onTertiaryContainer = Color(0xFF0A1B51),
                background = Color(0xFFFAFDFF),
                onBackground = Color(0xFF191C1E),
                surface = Color(0xFFFAFDFF),
                onSurface = Color(0xFF191C1E),
                surfaceVariant = Color(0xFFDBE4E2),
                onSurfaceVariant = Color(0xFF3F4947),
                outline = Color(0xFF6F7977)
            )
        }
    }
    
    /**
     * Material Design color scheme
     */
    private fun getMaterialColorScheme(darkTheme: Boolean): ColorScheme {
        return if (darkTheme) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }
    }
    
    /**
     * VisionOS-inspired color scheme
     */
    private fun getVisionOSColorScheme(darkTheme: Boolean): ColorScheme {
        return if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFF007AFF),
                secondary = Color(0xFF5AC8FA),
                tertiary = Color(0xFF5856D6),
                background = Color(0xFF000000),
                surface = Color(0xFF1C1C1E),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onTertiary = Color.White,
                onBackground = Color(0xFFE5E5E7),
                onSurface = Color(0xFFE5E5E7)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF007AFF),
                secondary = Color(0xFF34C759),
                tertiary = Color(0xFF5856D6),
                background = Color(0xFFF2F2F7),
                surface = Color.White,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onTertiary = Color.White,
                onBackground = Color(0xFF1C1C1E),
                onSurface = Color(0xFF1C1C1E)
            )
        }
    }
    
    /**
     * Custom color scheme (placeholder)
     */
    private fun getCustomColorScheme(darkTheme: Boolean): ColorScheme {
        // TODO: Load from user preferences
        return getARVisionColorScheme(darkTheme)
    }
    
    /**
     * Typography configuration
     */
    private fun getTypography(): Typography {
        return Typography(
            // Use default Material3 typography
            // Can be customized per theme if needed
        )
    }
    
    fun shutdown() {
        // Clean up if needed
    }
    
    fun setTheme(themeName: String) {
        setTheme(themeName, true)
    }
}