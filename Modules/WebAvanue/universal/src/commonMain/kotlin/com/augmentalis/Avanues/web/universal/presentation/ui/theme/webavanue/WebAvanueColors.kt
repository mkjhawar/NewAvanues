package com.augmentalis.webavanue.ui.screen.theme.webavanue

import androidx.compose.ui.graphics.Color
import com.augmentalis.webavanue.ui.screen.theme.abstraction.AppColors

/**
 * WebAvanueColors - WebAvanue's Ocean Blue Glassmorphism theme
 *
 * This is WebAvanue's custom branding, used when running in standalone mode
 * (outside Avanues ecosystem).
 *
 * When running inside Avanues ecosystem, AvaMagic theme takes over to provide
 * system-wide visual consistency.
 *
 * Brand Identity - Ocean Blue Glassmorphism Theme:
 * - Dark: Deep slate background (#0F172A), light text
 * - Light: Light gray background (#F8FAFC), dark text
 * - Primary: #2563EB - Ocean blue accent (both modes)
 * - Accent: #3B82F6 - Bright blue for links, active states
 *
 * Glassmorphism on Android 12+ (API 31+):
 * - True backdrop blur with RenderEffect
 * - Translucent surfaces create depth
 * - Elevated shadows for 3D effect
 *
 * Fallback on Android 11 and below:
 * - Solid colors with Ocean Blue palette
 * - Elevation shadows maintained
 *
 * @param isDark Whether dark mode is active
 */
class WebAvanueColors(
    private val isDark: Boolean
) : AppColors {

    // ========== DARK THEME COLORS ==========
    // Ocean Blue Glassmorphism theme colors
    private val darkBgPrimary = Color(0xFF0F172A)      // Deep slate background
    private val darkGlassSurface = Color(0x141E293B)   // Slate 8% opacity - subtle glass
    private val darkGlassSurfaceMedium = Color(0x1F334155) // Slate 12% opacity - medium glass
    private val darkGlassSurfaceHigh = Color(0x33334155)   // Slate 20% opacity - elevated glass
    private val darkGlassBorder = Color(0x262563EB)    // Ocean blue 15% opacity - glass border
    private val darkSolidSurface = Color(0xFF1E293B)   // Solid slate surface
    private val darkSolidSurfaceElevated = Color(0xFF334155) // Solid elevated surface
    private val darkTextPrimary = Color(0xFFF1F5F9)    // On-surface text
    private val darkTextSecondary = Color(0xFFCBD5E1)  // On-surface variant

    // ========== LIGHT THEME COLORS ==========
    // Clean light theme with Ocean Blue accents
    private val lightBgPrimary = Color(0xFFF8FAFC)     // Light gray background
    private val lightGlassSurface = Color(0x14E2E8F0)  // Light glass
    private val lightGlassSurfaceMedium = Color(0x1FCBD5E1) // Medium light glass
    private val lightGlassSurfaceHigh = Color(0x33CBD5E1)   // Heavy light glass
    private val lightGlassBorder = Color(0x262563EB)   // Ocean blue 15% opacity - glass border
    private val lightSolidSurface = Color(0xFFFFFFFF)  // White surface
    private val lightSolidSurfaceElevated = Color(0xFFF1F5F9) // Light elevated surface
    private val lightTextPrimary = Color(0xFF0F172A)   // Dark text for light background
    private val lightTextSecondary = Color(0xFF475569) // Secondary dark text

    // Accent colors (same for both themes)
    private val primaryBlue = Color(0xFF2563EB)    // Ocean blue primary
    private val accentBlue = Color(0xFF3B82F6)     // Bright blue accent

    // ========== Theme-aware color selections ==========
    private val bgPrimary get() = if (isDark) darkBgPrimary else lightBgPrimary
    private val solidSurface get() = if (isDark) darkSolidSurface else lightSolidSurface
    private val solidSurfaceElevated get() = if (isDark) darkSolidSurfaceElevated else lightSolidSurfaceElevated
    private val textPrimary get() = if (isDark) darkTextPrimary else lightTextPrimary
    private val textSecondary get() = if (isDark) darkTextSecondary else lightTextSecondary
    private val glassSurface get() = if (isDark) darkGlassSurface else lightGlassSurface
    private val glassSurfaceMedium get() = if (isDark) darkGlassSurfaceMedium else lightGlassSurfaceMedium
    private val glassSurfaceHigh get() = if (isDark) darkGlassSurfaceHigh else lightGlassSurfaceHigh
    private val glassBorder get() = if (isDark) darkGlassBorder else lightGlassBorder

    // Implement AppColors interface
    // Note: Use solidSurface for Material3 components that don't support transparency
    // Components can override with glassSurface + blur modifier for glassmorphism
    override val primary = primaryBlue
    override val onPrimary = Color.White
    override val primaryContainer get() = solidSurfaceElevated
    override val onPrimaryContainer get() = textPrimary

    override val secondary = accentBlue
    override val onSecondary = Color.White
    override val secondaryContainer get() = solidSurface
    override val onSecondaryContainer get() = textPrimary

    override val tertiary = accentBlue
    override val onTertiary = Color.White
    override val tertiaryContainer get() = solidSurface
    override val onTertiaryContainer get() = textPrimary

    override val error = Color(0xFFEF4444)
    override val onError = Color.White
    override val errorContainer = Color(0x33EF4444)  // Red 20% glass
    override val onErrorContainer = Color(0xFFEF4444)

    override val background get() = bgPrimary
    override val onBackground get() = textPrimary

    override val surface get() = solidSurface
    override val onSurface get() = textPrimary
    override val surfaceVariant get() = solidSurface
    override val onSurfaceVariant get() = textSecondary

    override val outline get() = glassBorder
    override val outlineVariant = Color(0x1A2563EB)  // Ocean blue 10% opacity

    // Browser-specific branding
    override val voiceActive = primaryBlue
    override val voiceInactive get() = textSecondary
    override val voiceListening = primaryBlue

    override val tabActive get() = solidSurfaceElevated
    override val tabInactive get() = solidSurface
    override val addressBarBackground get() = solidSurface

    // Custom properties for glassmorphism (not in AppColors interface)
    // Components can access these via LocalAppColors
    val glassLight get() = glassSurface
    val glassMedium get() = glassSurfaceMedium
    val glassHeavy get() = glassSurfaceHigh
    val glassOutline get() = glassBorder
}
