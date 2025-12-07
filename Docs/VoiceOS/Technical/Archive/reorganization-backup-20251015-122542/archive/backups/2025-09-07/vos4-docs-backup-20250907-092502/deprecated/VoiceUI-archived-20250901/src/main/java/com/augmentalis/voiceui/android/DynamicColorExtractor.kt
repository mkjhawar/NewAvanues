/**
 * DynamicColorExtractor.kt - Real implementation of Android 12+ dynamic color extraction
 * 
 * Extracts colors from the system wallpaper and creates a complete Material You
 * color scheme that adapts to the user's personalization.
 */

package com.augmentalis.voiceui.android

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.isSystemInDarkTheme
import com.google.android.material.color.DynamicColors as MaterialDynamicColors
import com.google.android.material.color.MaterialColors
import android.content.res.Configuration
import androidx.compose.material3.ColorScheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * REAL DYNAMIC COLOR EXTRACTOR
 * 
 * Extracts actual dynamic colors from Android 12+ systems
 * with fallback support for older versions.
 */
class DynamicColorExtractor(private val context: Context) {
    
    companion object {
        private const val TAG = "DynamicColorExtractor"
        
        // Fallback colors for pre-Android 12
        private val FALLBACK_PRIMARY = Color(0xFF6750A4)
        private val FALLBACK_SECONDARY = Color(0xFF625B71)
        private val FALLBACK_TERTIARY = Color(0xFF7D5260)
        private val FALLBACK_BACKGROUND_LIGHT = Color(0xFFFFFBFE)
        private val FALLBACK_BACKGROUND_DARK = Color(0xFF1C1B1F)
    }
    
    private val _colorScheme = MutableStateFlow<ExtractedColorScheme?>(null)
    val colorScheme: StateFlow<ExtractedColorScheme?> = _colorScheme
    
    init {
        extractColors()
    }
    
    /**
     * Extract dynamic colors from system
     */
    fun extractColors() {
        _colorScheme.value = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                extractAndroid12Colors()
            }
            else -> {
                createFallbackColors()
            }
        }
    }
    
    /**
     * Extract Material You colors on Android 12+
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun extractAndroid12Colors(): ExtractedColorScheme {
        val isDarkMode = isDarkModeEnabled()
        
        return if (MaterialDynamicColors.isDynamicColorAvailable()) {
            // Get actual dynamic colors from system
            val colorScheme = if (isDarkMode) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
            
            ExtractedColorScheme(
                primary = colorScheme.primary,
                onPrimary = colorScheme.onPrimary,
                primaryContainer = colorScheme.primaryContainer,
                onPrimaryContainer = colorScheme.onPrimaryContainer,
                
                secondary = colorScheme.secondary,
                onSecondary = colorScheme.onSecondary,
                secondaryContainer = colorScheme.secondaryContainer,
                onSecondaryContainer = colorScheme.onSecondaryContainer,
                
                tertiary = colorScheme.tertiary,
                onTertiary = colorScheme.onTertiary,
                tertiaryContainer = colorScheme.tertiaryContainer,
                onTertiaryContainer = colorScheme.onTertiaryContainer,
                
                background = colorScheme.background,
                onBackground = colorScheme.onBackground,
                
                surface = colorScheme.surface,
                onSurface = colorScheme.onSurface,
                surfaceVariant = colorScheme.surfaceVariant,
                onSurfaceVariant = colorScheme.onSurfaceVariant,
                
                error = colorScheme.error,
                onError = colorScheme.onError,
                errorContainer = colorScheme.errorContainer,
                onErrorContainer = colorScheme.onErrorContainer,
                
                outline = colorScheme.outline,
                outlineVariant = colorScheme.outlineVariant,
                
                inverseSurface = colorScheme.inverseSurface,
                inverseOnSurface = colorScheme.inverseOnSurface,
                inversePrimary = colorScheme.inversePrimary,
                
                isDynamic = true,
                isDarkMode = isDarkMode
            )
        } else {
            // Fallback for devices without dynamic color support
            createFallbackColors()
        }
    }
    
    /**
     * Create fallback Material 3 colors for older Android versions
     */
    private fun createFallbackColors(): ExtractedColorScheme {
        val isDarkMode = isDarkModeEnabled()
        
        return if (isDarkMode) {
            ExtractedColorScheme(
                primary = Color(0xFFD0BCFF),
                onPrimary = Color(0xFF381E72),
                primaryContainer = Color(0xFF4F378B),
                onPrimaryContainer = Color(0xFFEADDFF),
                
                secondary = Color(0xFFCCC2DC),
                onSecondary = Color(0xFF332D41),
                secondaryContainer = Color(0xFF4A4458),
                onSecondaryContainer = Color(0xFFE8DEF8),
                
                tertiary = Color(0xFFEFB8C8),
                onTertiary = Color(0xFF492532),
                tertiaryContainer = Color(0xFF633B48),
                onTertiaryContainer = Color(0xFFFFD8E4),
                
                background = Color(0xFF1C1B1F),
                onBackground = Color(0xFFE6E1E5),
                
                surface = Color(0xFF1C1B1F),
                onSurface = Color(0xFFE6E1E5),
                surfaceVariant = Color(0xFF49454F),
                onSurfaceVariant = Color(0xFFCAC4D0),
                
                error = Color(0xFFF2B8B5),
                onError = Color(0xFF601410),
                errorContainer = Color(0xFF8C1D18),
                onErrorContainer = Color(0xFFF9DEDC),
                
                outline = Color(0xFF938F99),
                outlineVariant = Color(0xFF49454F),
                
                inverseSurface = Color(0xFFE6E1E5),
                inverseOnSurface = Color(0xFF313033),
                inversePrimary = Color(0xFF6750A4),
                
                isDynamic = false,
                isDarkMode = true
            )
        } else {
            ExtractedColorScheme(
                primary = FALLBACK_PRIMARY,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFEADDFF),
                onPrimaryContainer = Color(0xFF21005D),
                
                secondary = FALLBACK_SECONDARY,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFE8DEF8),
                onSecondaryContainer = Color(0xFF1D192B),
                
                tertiary = FALLBACK_TERTIARY,
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFFFD8E4),
                onTertiaryContainer = Color(0xFF31111D),
                
                background = FALLBACK_BACKGROUND_LIGHT,
                onBackground = Color(0xFF1C1B1F),
                
                surface = FALLBACK_BACKGROUND_LIGHT,
                onSurface = Color(0xFF1C1B1F),
                surfaceVariant = Color(0xFFE7E0EC),
                onSurfaceVariant = Color(0xFF49454F),
                
                error = Color(0xFFBA1A1A),
                onError = Color.White,
                errorContainer = Color(0xFFFFDAD6),
                onErrorContainer = Color(0xFF410002),
                
                outline = Color(0xFF79747E),
                outlineVariant = Color(0xFFCAC4D0),
                
                inverseSurface = Color(0xFF313033),
                inverseOnSurface = Color(0xFFF4EFF4),
                inversePrimary = Color(0xFFD0BCFF),
                
                isDynamic = false,
                isDarkMode = false
            )
        }
    }
    
    /**
     * Check if dark mode is enabled
     */
    private fun isDarkModeEnabled(): Boolean {
        return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            else -> false
        }
    }
    
    /**
     * Get harmonized color (blend custom color with dynamic scheme)
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun harmonizeColor(color: Color): Color {
        return if (MaterialDynamicColors.isDynamicColorAvailable()) {
            val harmonized = MaterialColors.harmonize(
                color.toArgb(),
                context.getColor(android.R.color.system_accent1_500)
            )
            Color(harmonized)
        } else {
            color
        }
    }
    
    /**
     * Create color scheme from seed color
     */
    fun createFromSeed(seedColor: Color, isDark: Boolean = false): ExtractedColorScheme {
        // This would use Material Color Utilities to generate a full scheme
        // For now, return a basic scheme based on the seed
        return ExtractedColorScheme(
            primary = seedColor,
            onPrimary = if (isDark) Color.Black else Color.White,
            primaryContainer = seedColor.copy(alpha = 0.3f),
            onPrimaryContainer = if (isDark) Color.White else Color.Black,
            
            secondary = adjustHue(seedColor, 60f),
            onSecondary = if (isDark) Color.Black else Color.White,
            secondaryContainer = adjustHue(seedColor, 60f).copy(alpha = 0.3f),
            onSecondaryContainer = if (isDark) Color.White else Color.Black,
            
            tertiary = adjustHue(seedColor, 120f),
            onTertiary = if (isDark) Color.Black else Color.White,
            tertiaryContainer = adjustHue(seedColor, 120f).copy(alpha = 0.3f),
            onTertiaryContainer = if (isDark) Color.White else Color.Black,
            
            background = if (isDark) Color(0xFF1C1B1F) else Color(0xFFFFFBFE),
            onBackground = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F),
            
            surface = if (isDark) Color(0xFF1C1B1F) else Color(0xFFFFFBFE),
            onSurface = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F),
            surfaceVariant = if (isDark) Color(0xFF49454F) else Color(0xFFE7E0EC),
            onSurfaceVariant = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
            
            error = if (isDark) Color(0xFFF2B8B5) else Color(0xFFBA1A1A),
            onError = if (isDark) Color(0xFF601410) else Color.White,
            errorContainer = if (isDark) Color(0xFF8C1D18) else Color(0xFFFFDAD6),
            onErrorContainer = if (isDark) Color(0xFFF9DEDC) else Color(0xFF410002),
            
            outline = if (isDark) Color(0xFF938F99) else Color(0xFF79747E),
            outlineVariant = if (isDark) Color(0xFF49454F) else Color(0xFFCAC4D0),
            
            inverseSurface = if (isDark) Color(0xFFE6E1E5) else Color(0xFF313033),
            inverseOnSurface = if (isDark) Color(0xFF313033) else Color(0xFFF4EFF4),
            inversePrimary = if (isDark) seedColor.copy(alpha = 0.8f) else seedColor.copy(alpha = 0.6f),
            
            isDynamic = false,
            isDarkMode = isDark
        )
    }
    
    /**
     * Adjust hue of a color
     */
    private fun adjustHue(color: Color, degrees: Float): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[0] = (hsv[0] + degrees) % 360f
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}

/**
 * Complete extracted color scheme
 */
data class ExtractedColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    
    val background: Color,
    val onBackground: Color,
    
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    
    val outline: Color,
    val outlineVariant: Color,
    
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val inversePrimary: Color,
    
    val isDynamic: Boolean,
    val isDarkMode: Boolean
) {
    /**
     * Convert to Compose ColorScheme
     */
    fun toComposeColorScheme(): ColorScheme {
        return ColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = inversePrimary,
            
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            
            background = background,
            onBackground = onBackground,
            
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = primary,
            
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            
            outline = outline,
            outlineVariant = outlineVariant,
            
            scrim = Color.Black
        )
    }
    
    /**
     * Convert to custom theme
     */
    fun toCustomTheme(name: String = "Dynamic Theme"): com.augmentalis.voiceui.theming.CustomTheme {
        return com.augmentalis.voiceui.theming.CustomThemeBuilder()
            .name(name)
            .colors {
                primary(primary)
                secondary(secondary)
                background(background)
                surface(surface)
                error(error)
                onPrimary(onPrimary)
                onSecondary(onSecondary)
                onBackground(onBackground)
                onSurface(onSurface)
                onError(onError)
                
                custom("primaryContainer", primaryContainer)
                custom("onPrimaryContainer", onPrimaryContainer)
                custom("secondaryContainer", secondaryContainer)
                custom("onSecondaryContainer", onSecondaryContainer)
                custom("tertiary", tertiary)
                custom("onTertiary", onTertiary)
                custom("tertiaryContainer", tertiaryContainer)
                custom("onTertiaryContainer", onTertiaryContainer)
                custom("surfaceVariant", surfaceVariant)
                custom("onSurfaceVariant", onSurfaceVariant)
                custom("outline", outline)
                custom("outlineVariant", outlineVariant)
            }
            .build()
    }
}