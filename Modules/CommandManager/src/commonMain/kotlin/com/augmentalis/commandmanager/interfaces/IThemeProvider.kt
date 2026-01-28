/**
 * IThemeProvider.kt - Theme provider interface for VoiceOSCoreNG
 *
 * Defines the contract for theme management across the application.
 * Implementations must be thread-safe and support reactive updates.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 */
package com.augmentalis.commandmanager

import com.augmentalis.commandmanager.OverlayTheme
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for theme management throughout the VoiceOS application.
 *
 * Provides reactive theme observation via StateFlow and thread-safe
 * theme switching capabilities. All implementations must ensure
 * thread safety for concurrent access.
 *
 * Usage:
 * ```kotlin
 * // Observe theme changes
 * themeProvider.currentTheme.collect { theme ->
 *     updateUI(theme)
 * }
 *
 * // Switch theme variant
 * themeProvider.setVariant(ThemeVariant.HIGH_CONTRAST)
 * ```
 */
interface IThemeProvider {

    /**
     * Current theme configuration as observable StateFlow.
     *
     * Emits the current [OverlayTheme] and updates whenever the theme
     * changes via [setVariant], [setCustomTheme], or [resetToDefault].
     */
    val currentTheme: StateFlow<OverlayTheme>

    /**
     * Current theme variant as observable StateFlow.
     *
     * Emits the current [ThemeVariant] representing which preset
     * or custom theme is active. For custom themes, this will be
     * [ThemeVariant.DEFAULT] unless a custom variant is tracked.
     */
    val currentVariant: StateFlow<ThemeVariant>

    /**
     * Set the theme variant.
     *
     * Updates both [currentVariant] and [currentTheme] to the
     * corresponding preset theme for the given variant.
     *
     * Thread-safe: Can be called from any thread.
     *
     * @param variant The theme variant to apply
     */
    fun setVariant(variant: ThemeVariant)

    /**
     * Set a custom theme configuration.
     *
     * Updates [currentTheme] to the provided custom theme.
     * The [currentVariant] will remain unchanged but the actual
     * theme displayed will be the custom one.
     *
     * Thread-safe: Can be called from any thread.
     *
     * @param theme The custom theme configuration to apply
     */
    fun setCustomTheme(theme: OverlayTheme)

    /**
     * Reset to the default theme.
     *
     * Restores [currentVariant] to [ThemeVariant.DEFAULT] and
     * [currentTheme] to [OverlayTheme.DEFAULT].
     *
     * Thread-safe: Can be called from any thread.
     */
    fun resetToDefault()

    /**
     * Check if the current theme has animations enabled.
     *
     * Convenience method for checking if animations should be applied.
     *
     * @return true if animations are enabled in the current theme
     */
    fun isAnimationEnabled(): Boolean = currentTheme.value.animationEnabled

    /**
     * Get the theme for a specific variant without changing current theme.
     *
     * Useful for preview or comparison purposes.
     *
     * @param variant The variant to get the theme for
     * @return The OverlayTheme for the specified variant
     */
    fun getThemeForVariant(variant: ThemeVariant): OverlayTheme
}
