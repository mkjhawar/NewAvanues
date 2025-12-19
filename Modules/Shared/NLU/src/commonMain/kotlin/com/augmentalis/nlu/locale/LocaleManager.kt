package com.augmentalis.nlu.locale

/**
 * Manages user locale preferences with system fallback
 *
 * Priority: User override > System default > en-US
 * Supports 52 languages via fallback chain
 *
 * Architecture:
 * - Expect/actual pattern for platform-specific locale detection
 * - User preferences persisted across sessions
 * - Fallback chain ensures robustness (e.g., fr-FR → fr → en-US)
 *
 * Usage:
 * ```kotlin
 * val localeManager = LocaleManager(context)
 * val currentLocale = localeManager.getCurrentLocale()  // "fr-FR"
 * val fallbacks = localeManager.getFallbackChain("fr-FR")  // ["fr-FR", "fr", "en-US"]
 * ```
 */
expect class LocaleManager {
    /**
     * Get current locale (e.g., "fr-FR", "en-US")
     *
     * Priority:
     * 1. User override (if set via setLocale)
     * 2. System default locale
     * 3. en-US (ultimate fallback)
     *
     * @return Current locale in BCP-47 format (language-COUNTRY)
     */
    fun getCurrentLocale(): String

    /**
     * Set user locale preference (persisted across sessions)
     *
     * This overrides system locale detection.
     * Pass null to clear override and revert to system default.
     *
     * @param locale Locale in BCP-47 format (e.g., "es-MX", "zh-CN") or null to clear
     */
    fun setLocale(locale: String?)

    /**
     * Get fallback chain for robustness
     *
     * Creates a priority list of locales to try when loading embeddings.
     * Each entry is progressively less specific until reaching en-US.
     *
     * Examples:
     * - "fr-FR" → ["fr-FR", "fr", "en-US"]
     * - "en-GB" → ["en-GB", "en", "en-US"]
     * - "en" → ["en", "en-US"]
     * - "zh-CN" → ["zh-CN", "zh", "en-US"]
     *
     * @param locale Starting locale
     * @return Ordered list of fallback locales (most specific to least specific)
     */
    fun getFallbackChain(locale: String): List<String>

    /**
     * Check if locale is supported by AVA
     *
     * AVA supports 52+ languages. This checks against the supported list.
     *
     * @param locale Locale to check (e.g., "fr-FR")
     * @return true if locale is in supported set
     */
    fun isLocaleSupported(locale: String): Boolean

    /**
     * Get list of all supported locales
     *
     * @return Immutable set of supported locale codes
     */
    fun getSupportedLocales(): Set<String>
}
