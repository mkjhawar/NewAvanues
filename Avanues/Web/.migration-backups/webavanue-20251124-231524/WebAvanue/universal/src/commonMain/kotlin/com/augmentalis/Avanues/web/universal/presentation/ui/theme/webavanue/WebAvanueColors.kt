package com.augmentalis.Avanues.web.universal.presentation.ui.theme.webavanue

import androidx.compose.ui.graphics.Color
import com.augmentalis.Avanues.web.universal.presentation.ui.theme.abstraction.AppColors

/**
 * WebAvanueColors - WebAvanue's Dark 3D theme color palette
 *
 * This is WebAvanue's custom branding, used when running in standalone mode
 * (outside Avanues ecosystem).
 *
 * When running inside Avanues ecosystem, AvaMagic theme takes over to provide
 * system-wide visual consistency.
 *
 * Brand Identity - Dark 3D Theme:
 * - Background Primary: #1A1A2E - Deep dark base
 * - Background Secondary: #16213E - Elevated surfaces
 * - Background Surface: #0F3460 - Command bar, interactive elements
 * - Accent Voice: #A78BFA - Voice/listening state
 * - Accent Blue: #60A5FA - Links, active states
 *
 * @param isDark Whether dark mode is active (always dark for 3D theme)
 */
class WebAvanueColors(
    private val isDark: Boolean
) : AppColors {

    // Dark 3D theme colors (used for both light and dark modes)
    private val bgPrimary = Color(0xFF1A1A2E)
    private val bgSecondary = Color(0xFF16213E)
    private val bgSurface = Color(0xFF0F3460)
    private val accentVoice = Color(0xFFA78BFA)
    private val accentBlue = Color(0xFF60A5FA)
    private val textPrimary = Color(0xFFE8E8E8)
    private val textSecondary = Color(0xFFA0A0A0)

    // Implement AppColors interface
    override val primary = accentVoice
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
    override val voiceActive = accentVoice
    override val voiceInactive = textSecondary
    override val voiceListening = accentVoice

    override val tabActive = bgSurface
    override val tabInactive = bgSecondary
    override val addressBarBackground = bgSecondary
}
