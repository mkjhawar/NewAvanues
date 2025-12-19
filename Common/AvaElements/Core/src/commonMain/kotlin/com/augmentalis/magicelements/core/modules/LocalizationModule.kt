package com.augmentalis.magicelements.core.modules

import com.augmentalis.magicelements.core.base.BaseModule
import com.augmentalis.magicelements.core.base.ModuleTier

/**
 * Delegate interface for localization operations.
 * Platform implementations provide the actual localization functionality.
 */
interface LocalizationModuleDelegate {
    fun translate(key: String, params: Map<String, Any?>?): String
    fun getLocale(): String
    fun setLocale(locale: String)
    fun getAvailableLocales(): List<String>
    fun isRTL(): Boolean
    fun formatNumber(value: Number, locale: String?): String
    fun formatCurrency(value: Number, currency: String, locale: String?): String
    fun formatDate(timestamp: Long, style: String): String
    fun formatTime(timestamp: Long, style: String): String
    fun formatRelative(timestamp: Long): String
    fun pluralize(count: Int, singular: String, plural: String?): String
}

/**
 * LocalizationModule provides internationalization and localization functionality.
 *
 * Features:
 * - String translation with parameter interpolation
 * - Locale management (get, set, list available)
 * - Text direction detection (LTR/RTL)
 * - Number formatting
 * - Currency formatting
 * - Date and time formatting (multiple styles)
 * - Relative time formatting
 * - Pluralization rules
 *
 * All methods are DATA tier - no runtime checks required.
 *
 * Usage:
 * ```kotlin
 * // Translation
 * val greeting = localization.t("welcome.message", mapOf("name" to "John"))
 *
 * // Locale management
 * val current = localization.locale()
 * localization.setLocale("es-ES")
 * val available = localization.availableLocales()
 *
 * // Text direction
 * val isRightToLeft = localization.isRTL()
 * val direction = localization.direction()
 *
 * // Formatting
 * val formattedNumber = localization.format.number(1234.56)
 * val formattedPrice = localization.format.currency(99.99, "USD")
 * val formattedDate = localization.format.date(timestamp, "medium")
 * val formattedTime = localization.format.time(timestamp, "short")
 * val relativeTime = localization.format.relative(timestamp) // "2 hours ago"
 *
 * // Pluralization
 * val message = localization.plural(count, "item", "items")
 * ```
 */
class LocalizationModule(
    private val delegate: LocalizationModuleDelegate
) : BaseModule(
    name = "localization",
    version = "1.0.0",
    minimumTier = ModuleTier.DATA
) {

    /**
     * Formatting sub-module for numbers, dates, and currency.
     */
    val format = FormatModule()

    /**
     * Translates a localization key with optional parameters.
     *
     * @param key The localization key to translate
     * @param params Optional parameters for string interpolation
     * @return The translated string, or the key if translation not found
     */
    fun t(key: String, params: Map<String, Any?>? = null): String {
        return delegate.translate(key, params)
    }

    /**
     * Gets the current locale.
     *
     * @return The current locale code (e.g., "en-US", "es-ES")
     */
    fun locale(): String {
        return delegate.getLocale()
    }

    /**
     * Sets the current locale.
     *
     * @param locale The locale code to set (e.g., "en-US", "es-ES")
     */
    fun setLocale(locale: String) {
        delegate.setLocale(locale)
    }

    /**
     * Gets the list of available locales.
     *
     * @return List of locale codes that are available
     */
    fun availableLocales(): List<String> {
        return delegate.getAvailableLocales()
    }

    /**
     * Checks if the current locale uses right-to-left text direction.
     *
     * @return true if RTL, false if LTR
     */
    fun isRTL(): Boolean {
        return delegate.isRTL()
    }

    /**
     * Gets the text direction for the current locale.
     *
     * @return "rtl" for right-to-left, "ltr" for left-to-right
     */
    fun direction(): String {
        return if (delegate.isRTL()) "rtl" else "ltr"
    }

    /**
     * Pluralizes a string based on count.
     *
     * @param count The count to determine singular/plural
     * @param singular The singular form
     * @param plural The plural form (defaults to singular + "s")
     * @return The appropriate form based on count
     */
    fun plural(count: Int, singular: String, plural: String? = null): String {
        return delegate.pluralize(count, singular, plural)
    }

    /**
     * Formatting sub-module providing number, currency, and date/time formatting.
     */
    inner class FormatModule {

        /**
         * Formats a number according to locale conventions.
         *
         * @param value The number to format
         * @param locale Optional locale override (uses current if not specified)
         * @return The formatted number string
         */
        fun number(value: Number, locale: String? = null): String {
            return delegate.formatNumber(value, locale)
        }

        /**
         * Formats a number as currency.
         *
         * @param value The amount to format
         * @param currency The currency code (e.g., "USD", "EUR")
         * @param locale Optional locale override (uses current if not specified)
         * @return The formatted currency string
         */
        fun currency(value: Number, currency: String, locale: String? = null): String {
            return delegate.formatCurrency(value, currency, locale)
        }

        /**
         * Formats a date according to locale conventions.
         *
         * @param timestamp The timestamp in milliseconds
         * @param style The format style: "short", "medium", or "long"
         * @return The formatted date string
         */
        fun date(timestamp: Long, style: String = "medium"): String {
            return delegate.formatDate(timestamp, style)
        }

        /**
         * Formats a time according to locale conventions.
         *
         * @param timestamp The timestamp in milliseconds
         * @param style The format style: "short", "medium", or "long"
         * @return The formatted time string
         */
        fun time(timestamp: Long, style: String = "medium"): String {
            return delegate.formatTime(timestamp, style)
        }

        /**
         * Formats a timestamp as relative time.
         *
         * @param timestamp The timestamp in milliseconds
         * @return The relative time string (e.g., "2 hours ago", "in 3 days")
         */
        fun relative(timestamp: Long): String {
            return delegate.formatRelative(timestamp)
        }
    }
}
