package com.augmentalis.Avanues.web.universal.presentation.ui.theme.webavanue

import androidx.compose.ui.graphics.Color
import com.augmentalis.Avanues.web.universal.presentation.ui.theme.abstraction.AppColors

/**
 * WebAvanueColors - WebAvanue's Ocean Blue theme color palette
 *
 * This is WebAvanue's custom branding, used when running in standalone mode
 * (outside Avanues ecosystem).
 *
 * When running inside Avanues ecosystem, AvaMagic theme takes over to provide
 * system-wide visual consistency.
 *
 * Brand Identity - Ocean Blue Theme:
 * - Background: #0F172A - Deep slate background
 * - Surface: #1E293B - Elevated surfaces
 * - Surface Elevated: #334155 - Command bar, interactive elements
 * - Primary: #2563EB - Ocean blue accent
 * - Accent: #3B82F6 - Bright blue for links, active states
 *
 * @param isDark Whether dark mode is active (always dark for Ocean Blue theme)
 */
class WebAvanueColors(
    private val isDark: Boolean
) : AppColors {

    // Ocean Blue theme colors (used for both light and dark modes)
    private val bgPrimary = Color(0xFF0F172A)      // Deep slate background
    private val bgSecondary = Color(0xFF1E293B)    // Surface
    private val bgSurface = Color(0xFF334155)      // Surface elevated
    private val primaryBlue = Color(0xFF2563EB)    // Ocean blue primary
    private val accentBlue = Color(0xFF3B82F6)     // Bright blue accent
    private val textPrimary = Color(0xFFF1F5F9)    // On-surface text
    private val textSecondary = Color(0xFFCBD5E1)  // On-surface variant

    // Implement AppColors interface
    override val primary = primaryBlue
    override val onPrimary = Color.White
    override val primaryContainer = bgSurface
    override val onPrimaryContainer = textPrimary

    override val secondary = accentBlue
    override val onSecondary = Color.White
    override val secondaryContainer = bgSecondary
    override val onSecondaryContainer = textPrimary

    override val tertiary = accentBlue
    override val onTertiary = Color.White
    override val tertiaryContainer = bgSecondary
    override val onTertiaryContainer = textPrimary

    override val error = Color(0xFFCF6679)
    override val onError = Color.White
    override val errorContainer = Color(0xFFB00020)
    override val onErrorContainer = Color.White

    override val background = bgPrimary
    override val onBackground = textPrimary

    override val surface = bgSecondary
    override val onSurface = textPrimary
    override val surfaceVariant = bgSecondary
    override val onSurfaceVariant = textSecondary

    override val outline = Color(0xFF2D4A6F)
    override val outlineVariant = Color(0xFF48484A)

    // Browser-specific branding
    override val voiceActive = primaryBlue
    override val voiceInactive = textSecondary
    override val voiceListening = primaryBlue

    override val tabActive = bgSurface
    override val tabInactive = bgSecondary
    override val addressBarBackground = bgSecondary
}
