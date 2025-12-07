package com.augmentalis.Avanues.web.universal.presentation.ui.theme

/**
 * ThemeConfig - Theme selection and configuration
 *
 * Determines which theme to use based on:
 * 1. User preference (if set)
 * 2. Environment detection (Avanues ecosystem vs standalone)
 * 3. Default fallback (Material Design 3)
 */

/**
 * ThemeType - Available theme implementations
 */
enum class ThemeType {
    /**
     * App Branding - Developer's unique branding (WebAvanue colors)
     * Used when app is running outside Avanues ecosystem (standalone mode)
     * Each app can define its own unique color palette
     */
    APP_BRANDING,

    /**
     * AvaMagic - Avanues system-wide theme (replaces app branding)
     * Used when app is running inside Avanues ecosystem
     *
     * Features:
     * - Queries Avanues system for theme colors
     * - Provides visual consistency across all Avanues apps
     * - Voice-first design language
     * - System decides colors (not app developer)
     */
    AVAMAGIC,

    /**
     * Auto-detect - Automatically choose theme based on environment
     * Default behavior
     *
     * Logic:
     * - If Avanues ecosystem detected → AVAMAGIC
     * - If standalone → APP_BRANDING
     */
    AUTO
}

/**
 * ThemePreferences - User preference storage
 *
 * Platform-specific implementation (expect/actual pattern)
 * - Android: SharedPreferences or DataStore
 * - iOS: UserDefaults
 * - Desktop: Properties file
 * - Web: LocalStorage
 */
expect object ThemePreferences {
    /**
     * Get user's preferred theme
     * @return ThemeType or null if not set (use auto-detect)
     */
    fun getTheme(): ThemeType?

    /**
     * Set user's preferred theme
     * @param theme ThemeType to use (or AUTO for auto-detection)
     */
    fun setTheme(theme: ThemeType)

    /**
     * Clear user preference (revert to auto-detection)
     */
    fun clearTheme()
}

/**
 * ThemeDetector - Auto-detection logic
 *
 * Platform-specific implementation (expect/actual pattern)
 * Detects if app is running in Avanues ecosystem by checking for:
 * - Avanues Launcher package (Android)
 * - VoiceOS framework (iOS)
 * - Avanues API availability (Web)
 */
expect object ThemeDetector {
    /**
     * Detect if app is running in Avanues ecosystem
     * @return true if Avanues ecosystem detected
     */
    fun isAvanuesEcosystem(): Boolean

    /**
     * Get detected theme based on environment
     * @return ThemeType.AVAMAGIC if in Avanues, ThemeType.APP_BRANDING otherwise
     */
    fun detectTheme(): ThemeType
}

/**
 * Resolve actual theme to use
 *
 * Logic:
 * 1. If user preference set → use that
 * 2. Else if AUTO or null → detect environment
 * 3. Else → fallback to APP_BRANDING
 *
 * @param preference User's preferred theme (or null)
 * @return Resolved ThemeType (never AUTO)
 */
fun resolveTheme(preference: ThemeType? = null): ThemeType {
    return when (preference) {
        ThemeType.APP_BRANDING -> ThemeType.APP_BRANDING
        ThemeType.AVAMAGIC -> ThemeType.AVAMAGIC
        ThemeType.AUTO, null -> ThemeDetector.detectTheme()
    }
}
