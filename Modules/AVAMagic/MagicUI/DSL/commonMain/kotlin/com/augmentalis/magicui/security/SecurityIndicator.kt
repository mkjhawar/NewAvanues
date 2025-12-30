package com.augmentalis.magicui.security

import com.augmentalis.magicui.core.DeveloperVerificationLevel
import com.augmentalis.magicui.core.PluginSource

/**
 * Security indicator display model for plugin verification status.
 *
 * Provides user-facing security information about a plugin's verification
 * level, trust status, and source. Used to render security badges, warnings,
 * and informational UI elements that help users make informed decisions about
 * plugin trust.
 *
 * ## Purpose
 * Fulfills FR-031: Security Indicators Display requirement by providing
 * a standardized model for displaying plugin verification status across
 * all platforms (Android, JVM, iOS).
 *
 * ## Usage Example
 * ```kotlin
 * val indicator = SecurityIndicator.from(manifest)
 *
 * // Display badge
 * when (indicator.badgeType) {
 *     BadgeType.VERIFIED -> showVerifiedBadge(indicator.badgeText, indicator.badgeColor)
 *     BadgeType.REGISTERED -> showRegisteredBadge(indicator.badgeText, indicator.badgeColor)
 *     BadgeType.UNVERIFIED -> showUnverifiedBadge(indicator.badgeText, indicator.badgeColor)
 * }
 *
 * // Display details
 * showSecurityDetails(
 *     title = indicator.displayTitle,
 *     description = indicator.displayDescription,
 *     recommendations = indicator.recommendations
 * )
 * ```
 *
 * ## Platform Integration
 * - **Android**: MaterialComponents badges/chips, color resources
 * - **JVM**: Swing labels/icons, color constants
 * - **iOS**: UILabel with attributed text, system colors
 *
 * @property verificationLevel Developer verification level from manifest
 * @property pluginSource Source of the plugin (pre-bundled, store, third-party)
 *
 * @since 1.0.0
 * @see DeveloperVerificationLevel
 * @see PluginSource
 */
data class SecurityIndicator(
    val verificationLevel: DeveloperVerificationLevel,
    val pluginSource: PluginSource
) {
    /**
     * Badge type for visual representation.
     */
    val badgeType: BadgeType
        get() = when (verificationLevel) {
            DeveloperVerificationLevel.VERIFIED -> BadgeType.VERIFIED
            DeveloperVerificationLevel.REGISTERED -> BadgeType.REGISTERED
            DeveloperVerificationLevel.UNVERIFIED -> BadgeType.UNVERIFIED
        }

    /**
     * Badge text to display (short label).
     * Examples: "VERIFIED", "REGISTERED", "UNVERIFIED"
     */
    val badgeText: String
        get() = when (verificationLevel) {
            DeveloperVerificationLevel.VERIFIED -> "VERIFIED"
            DeveloperVerificationLevel.REGISTERED -> "REGISTERED"
            DeveloperVerificationLevel.UNVERIFIED -> "UNVERIFIED"
        }

    /**
     * Badge icon/symbol (emoji or unicode symbol for cross-platform consistency).
     */
    val badgeIcon: String
        get() = when (verificationLevel) {
            DeveloperVerificationLevel.VERIFIED -> "✓" // Check mark
            DeveloperVerificationLevel.REGISTERED -> "🔒" // Lock
            DeveloperVerificationLevel.UNVERIFIED -> "⚠" // Warning
        }

    /**
     * Badge color (hex color code for platform rendering).
     *
     * Platform implementations should map these to appropriate color resources:
     * - Android: Color.parseColor(badgeColor)
     * - JVM: Color.decode(badgeColor)
     * - iOS: UIColor(hexString: badgeColor)
     */
    val badgeColor: String
        get() = when (verificationLevel) {
            DeveloperVerificationLevel.VERIFIED -> "#4CAF50" // Green (Material Design Green 500)
            DeveloperVerificationLevel.REGISTERED -> "#2196F3" // Blue (Material Design Blue 500)
            DeveloperVerificationLevel.UNVERIFIED -> "#FF9800" // Orange (Material Design Orange 500)
        }

    /**
     * Display title for security details dialog.
     */
    val displayTitle: String
        get() = when (verificationLevel) {
            DeveloperVerificationLevel.VERIFIED -> "Verified Developer"
            DeveloperVerificationLevel.REGISTERED -> "Registered Developer"
            DeveloperVerificationLevel.UNVERIFIED -> "Unverified Developer"
        }

    /**
     * Detailed description of verification level.
     */
    val displayDescription: String
        get() = when (verificationLevel) {
            DeveloperVerificationLevel.VERIFIED ->
                "This plugin is from a verified developer who has passed manual code review. " +
                "Verified plugins undergo security audits and are considered highly trustworthy."

            DeveloperVerificationLevel.REGISTERED ->
                "This plugin is from a registered developer with code signing. " +
                "Registered plugins are signed with a verified certificate and undergo selective review."

            DeveloperVerificationLevel.UNVERIFIED ->
                "This plugin is from an unverified developer. " +
                "Unverified plugins have not undergone code review. Exercise caution when granting permissions."
        }

    /**
     * Trust level as percentage (for visual trust meters or progress indicators).
     * - VERIFIED: 100% (full trust)
     * - REGISTERED: 75% (high trust)
     * - UNVERIFIED: 40% (limited trust)
     */
    val trustPercentage: Int
        get() = when (verificationLevel) {
            DeveloperVerificationLevel.VERIFIED -> 100
            DeveloperVerificationLevel.REGISTERED -> 75
            DeveloperVerificationLevel.UNVERIFIED -> 40
        }

    /**
     * User-facing recommendations based on verification level.
     */
    val recommendations: List<String>
        get() = when (verificationLevel) {
            DeveloperVerificationLevel.VERIFIED -> listOf(
                "✓ Safe to grant requested permissions",
                "✓ Regular security audits performed",
                "✓ Developer identity verified"
            )

            DeveloperVerificationLevel.REGISTERED -> listOf(
                "✓ Developer identity verified via code signing",
                "⚠ Grant permissions carefully",
                "⚠ Selective review for high-risk categories"
            )

            DeveloperVerificationLevel.UNVERIFIED -> listOf(
                "⚠ No code review performed",
                "⚠ Grant minimal permissions only",
                "⚠ Uninstall if behavior seems suspicious",
                "⚠ Plugin runs in sandboxed environment"
            )
        }

    /**
     * Whether to show security warning dialog on first install.
     */
    val shouldShowWarning: Boolean
        get() = verificationLevel == DeveloperVerificationLevel.UNVERIFIED

    /**
     * Warning message to display (if applicable).
     */
    val warningMessage: String?
        get() = if (shouldShowWarning) {
            "This plugin is from an unverified developer and has not been reviewed. " +
            "Only install plugins from sources you trust. Grant permissions carefully."
        } else {
            null
        }

    /**
     * Source-specific information text.
     */
    val sourceInfo: String
        get() = when (pluginSource) {
            PluginSource.PRE_BUNDLED -> "Pre-bundled with application"
            PluginSource.APPAVENUE_STORE -> "Downloaded from AppAvenue Store"
            PluginSource.THIRD_PARTY -> "Installed from third-party source"
        }

    /**
     * Whether this plugin requires elevated security review.
     * UNVERIFIED plugins from THIRD_PARTY sources require extra caution.
     */
    val requiresElevatedReview: Boolean
        get() = verificationLevel == DeveloperVerificationLevel.UNVERIFIED &&
                pluginSource == PluginSource.THIRD_PARTY

    companion object {
        /**
         * Create SecurityIndicator from plugin manifest values.
         *
         * @param verificationLevel Verification level string from manifest
         * @param source Source string from manifest
         * @return SecurityIndicator instance
         * @throws IllegalArgumentException if values are invalid
         */
        fun from(verificationLevel: String, source: String): SecurityIndicator {
            val level = try {
                DeveloperVerificationLevel.valueOf(verificationLevel.uppercase())
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Invalid verification level: $verificationLevel. " +
                    "Must be one of: VERIFIED, REGISTERED, UNVERIFIED"
                )
            }

            val pluginSource = try {
                PluginSource.valueOf(source.uppercase())
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Invalid plugin source: $source. " +
                    "Must be one of: PRE_BUNDLED, APPAVENUE_STORE, THIRD_PARTY"
                )
            }

            return SecurityIndicator(level, pluginSource)
        }
    }
}

/**
 * Badge type enumeration for security indicators.
 *
 * Defines the visual style and semantic meaning of security badges
 * displayed in plugin lists, details pages, and permission dialogs.
 */
enum class BadgeType {
    /**
     * Verified developer badge.
     * Color: Green
     * Icon: Checkmark (✓)
     * Trust: Highest
     */
    VERIFIED,

    /**
     * Registered developer badge.
     * Color: Blue
     * Icon: Lock (🔒)
     * Trust: High
     */
    REGISTERED,

    /**
     * Unverified developer badge.
     * Color: Orange/Yellow
     * Icon: Warning (⚠)
     * Trust: Limited
     */
    UNVERIFIED
}

/**
 * Security indicator display configuration.
 *
 * Provides platform-specific rendering hints for security indicators.
 * Platforms can override these defaults to match their UI guidelines.
 */
data class SecurityIndicatorConfig(
    /**
     * Whether to show security badge in plugin list.
     * Default: true
     */
    val showBadgeInList: Boolean = true,

    /**
     * Whether to show detailed security info in plugin details page.
     * Default: true
     */
    val showDetailsPage: Boolean = true,

    /**
     * Whether to show security warning dialog for unverified plugins.
     * Default: true
     */
    val showWarningDialog: Boolean = true,

    /**
     * Whether to show trust percentage indicator.
     * Default: true
     */
    val showTrustPercentage: Boolean = true,

    /**
     * Whether to show security recommendations list.
     * Default: true
     */
    val showRecommendations: Boolean = true,

    /**
     * Badge size in density-independent pixels (dp/pt).
     * Default: 20dp (small badge for list items)
     */
    val badgeSize: Int = 20,

    /**
     * Icon size in density-independent pixels (dp/pt).
     * Default: 16dp
     */
    val iconSize: Int = 16
) {
    companion object {
        /**
         * Default configuration (all features enabled).
         */
        val DEFAULT = SecurityIndicatorConfig()

        /**
         * Minimal configuration (badges only, no detailed info).
         */
        val MINIMAL = SecurityIndicatorConfig(
            showDetailsPage = false,
            showWarningDialog = false,
            showTrustPercentage = false,
            showRecommendations = false
        )

        /**
         * Privacy-focused configuration (no visual indicators, warnings only).
         */
        val PRIVACY_FOCUSED = SecurityIndicatorConfig(
            showBadgeInList = false,
            showDetailsPage = false,
            showWarningDialog = true,
            showTrustPercentage = false,
            showRecommendations = true
        )
    }
}
