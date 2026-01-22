package com.augmentalis.magiccode.plugins.marketplace

import kotlinx.serialization.Serializable

/**
 * Plugin listing for marketplace display.
 *
 * Represents a plugin as it appears in marketplace search results and
 * browse views. Contains essential information for users to evaluate
 * plugins before viewing full details or installing.
 *
 * ## Display Information
 * The listing provides:
 * - Identity: plugin ID, name, author
 * - Summary: short description, capabilities, category
 * - Metrics: rating, rating count, download count
 * - Version: current version
 * - Visual: icon URL
 *
 * ## Example
 * ```kotlin
 * val listing = PluginListing(
 *     pluginId = "com.example.voice-assistant",
 *     name = "Voice Assistant Pro",
 *     shortDescription = "Advanced voice commands for accessibility",
 *     author = PublisherInfo("pub123", "Example Corp", verified = true),
 *     version = "2.1.0",
 *     rating = 4.5f,
 *     ratingCount = 1234,
 *     downloadCount = 50000L,
 *     capabilities = listOf("voice.commands", "accessibility"),
 *     iconUrl = "https://marketplace.example.com/icons/voice-assistant.png",
 *     category = "accessibility"
 * )
 * ```
 *
 * @property pluginId Unique plugin identifier in reverse-domain notation
 * @property name Human-readable plugin name
 * @property shortDescription Brief description (typically under 150 chars)
 * @property author Publisher information
 * @property version Current/latest version string
 * @property rating Average user rating (0.0 to 5.0)
 * @property ratingCount Number of user ratings
 * @property downloadCount Total download count
 * @property capabilities List of capability identifiers
 * @property iconUrl URL to plugin icon image (nullable)
 * @property category Primary category for the plugin
 * @since 1.0.0
 * @see PluginDetails
 * @see PublisherInfo
 */
@Serializable
data class PluginListing(
    val pluginId: String,
    val name: String,
    val shortDescription: String,
    val author: PublisherInfo,
    val version: String,
    val rating: Float,
    val ratingCount: Int,
    val downloadCount: Long,
    val capabilities: List<String>,
    val iconUrl: String?,
    val category: String
) {
    /**
     * Get formatted rating string.
     *
     * @return Rating formatted as "X.X (Y reviews)"
     */
    fun getFormattedRating(): String {
        return "%.1f (%d reviews)".format(rating, ratingCount)
    }

    /**
     * Get formatted download count with abbreviation.
     *
     * @return Downloads formatted with K/M suffix for large numbers
     */
    fun getFormattedDownloads(): String {
        return when {
            downloadCount >= 1_000_000 -> "%.1fM".format(downloadCount / 1_000_000.0)
            downloadCount >= 1_000 -> "%.1fK".format(downloadCount / 1_000.0)
            else -> downloadCount.toString()
        }
    }

    /**
     * Check if plugin has high rating (4.0 or above).
     *
     * @return true if rating is 4.0 or higher
     */
    fun isHighlyRated(): Boolean {
        return rating >= 4.0f
    }

    /**
     * Check if plugin is popular (based on downloads).
     *
     * @param threshold Minimum downloads to be considered popular
     * @return true if downloads exceed threshold
     */
    fun isPopular(threshold: Long = 10_000L): Boolean {
        return downloadCount >= threshold
    }

    /**
     * Check if plugin has a specific capability.
     *
     * @param capability Capability identifier to check
     * @return true if plugin declares this capability
     */
    fun hasCapability(capability: String): Boolean {
        return capabilities.contains(capability)
    }
}

/**
 * Publisher/author information.
 *
 * Represents the publisher or developer of a plugin, including
 * verification status for trust indicators.
 *
 * ## Verification Levels
 * - **Verified**: Publisher has passed identity verification and review
 * - **Unverified**: Publisher has not completed verification
 *
 * Verified publishers display a badge in the marketplace UI and
 * can access restricted permissions.
 *
 * @property id Unique publisher identifier
 * @property name Display name of the publisher
 * @property verified Whether publisher is verified
 * @since 1.0.0
 * @see PluginListing
 */
@Serializable
data class PublisherInfo(
    val id: String,
    val name: String,
    val verified: Boolean = false
) {
    /**
     * Get display name with verification badge indicator.
     *
     * @return Name with checkmark if verified
     */
    fun getDisplayNameWithBadge(): String {
        return if (verified) "$name [Verified]" else name
    }
}

/**
 * Full plugin details for detail view.
 *
 * Contains comprehensive plugin information beyond the listing,
 * including full description, screenshots, changelog, and
 * requirement/permission details.
 *
 * ## Additional Information
 * Beyond [PluginListing], details include:
 * - Full description (can be lengthy markdown)
 * - Screenshot URLs for preview
 * - Changelog for recent updates
 * - System requirements
 * - Required permissions with explanations
 * - Last update timestamp
 *
 * @property listing Basic listing information
 * @property fullDescription Complete plugin description (may include markdown)
 * @property screenshots List of screenshot image URLs
 * @property changelog Recent changes/release notes (nullable)
 * @property requirements List of system requirements
 * @property permissions List of required permissions
 * @property lastUpdated Unix timestamp of last update (milliseconds)
 * @since 1.0.0
 * @see PluginListing
 */
@Serializable
data class PluginDetails(
    val listing: PluginListing,
    val fullDescription: String,
    val screenshots: List<String>,
    val changelog: String?,
    val requirements: List<String>,
    val permissions: List<String>,
    val lastUpdated: Long
) {
    /**
     * Get time since last update in human-readable format.
     *
     * @return String like "2 days ago", "1 month ago", etc.
     */
    fun getTimeSinceUpdate(): String {
        val now = System.currentTimeMillis()
        val diff = now - lastUpdated

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val months = days / 30
        val years = months / 12

        return when {
            years > 0 -> "$years year${if (years > 1) "s" else ""} ago"
            months > 0 -> "$months month${if (months > 1) "s" else ""} ago"
            days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            else -> "Just now"
        }
    }

    /**
     * Check if plugin has any permissions.
     *
     * @return true if plugin requires at least one permission
     */
    fun hasPermissions(): Boolean {
        return permissions.isNotEmpty()
    }

    /**
     * Check if plugin has screenshots.
     *
     * @return true if screenshots are available
     */
    fun hasScreenshots(): Boolean {
        return screenshots.isNotEmpty()
    }

    /**
     * Check if plugin has a changelog.
     *
     * @return true if changelog is available
     */
    fun hasChangelog(): Boolean {
        return !changelog.isNullOrBlank()
    }

    /**
     * Get the first screenshot URL for preview.
     *
     * @return First screenshot URL or null if none
     */
    fun getPreviewScreenshot(): String? {
        return screenshots.firstOrNull()
    }
}

/**
 * Version information for a plugin release.
 *
 * Represents a single version in the plugin's release history,
 * including release date, changelog, and package size.
 *
 * @property version Semantic version string (e.g., "1.2.3")
 * @property releasedAt Unix timestamp of release (milliseconds)
 * @property changelog Release notes for this version (nullable)
 * @property size Package size in bytes
 * @since 1.0.0
 * @see MarketplaceApi.getVersions
 */
@Serializable
data class VersionInfo(
    val version: String,
    val releasedAt: Long,
    val changelog: String?,
    val size: Long
) {
    /**
     * Get formatted size string.
     *
     * @return Size formatted with KB/MB suffix
     */
    fun getFormattedSize(): String {
        return when {
            size >= 1024 * 1024 -> "%.1f MB".format(size / (1024.0 * 1024.0))
            size >= 1024 -> "%.1f KB".format(size / 1024.0)
            else -> "$size bytes"
        }
    }

    /**
     * Get formatted release date.
     *
     * Returns the timestamp as a relative time description since
     * exact date formatting requires platform-specific APIs.
     *
     * @return Relative time string (e.g., "3 months ago")
     */
    fun getFormattedDate(): String {
        // KMP-compatible: return relative time instead of formatted date
        // Platform-specific implementations can override for exact dates
        val now = System.currentTimeMillis()
        val diff = now - releasedAt

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val months = days / 30
        val years = months / 12

        return when {
            years > 0 -> "$years year${if (years > 1) "s" else ""} ago"
            months > 0 -> "$months month${if (months > 1) "s" else ""} ago"
            days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            else -> "Just now"
        }
    }

    /**
     * Get the release timestamp in milliseconds.
     *
     * Use this for platform-specific date formatting.
     *
     * @return Unix timestamp in milliseconds
     */
    fun getReleaseTimestamp(): Long = releasedAt

    /**
     * Parse version into components.
     *
     * @return Triple of (major, minor, patch) or null if invalid
     */
    fun parseVersion(): Triple<Int, Int, Int>? {
        val parts = version.split("-")[0].split(".") // Remove prerelease suffix first
        if (parts.size != 3) return null
        return try {
            Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Check if this is a major version update from another version.
     *
     * @param other Other version to compare
     * @return true if this is a major version bump
     */
    fun isMajorUpdateFrom(other: VersionInfo): Boolean {
        val thisVersion = parseVersion() ?: return false
        val otherVersion = other.parseVersion() ?: return false
        return thisVersion.first > otherVersion.first
    }
}

/**
 * Update information for an installed plugin.
 *
 * Represents an available update for a plugin that is currently
 * installed, including version comparison and changelog.
 *
 * @property pluginId Unique plugin identifier
 * @property currentVersion Currently installed version
 * @property newVersion Available new version
 * @property changelog Changes in the new version (nullable)
 * @property size Package size of the update in bytes
 * @since 1.0.0
 * @see MarketplaceApi.checkUpdates
 */
@Serializable
data class UpdateInfo(
    val pluginId: String,
    val currentVersion: String,
    val newVersion: String,
    val changelog: String?,
    val size: Long
) {
    /**
     * Get formatted version change string.
     *
     * @return String like "1.0.0 -> 1.1.0"
     */
    fun getVersionChangeString(): String {
        return "$currentVersion -> $newVersion"
    }

    /**
     * Get formatted size string.
     *
     * @return Size formatted with KB/MB suffix
     */
    fun getFormattedSize(): String {
        return when {
            size >= 1024 * 1024 -> "%.1f MB".format(size / (1024.0 * 1024.0))
            size >= 1024 -> "%.1f KB".format(size / 1024.0)
            else -> "$size bytes"
        }
    }

    /**
     * Check if this is a major version update.
     *
     * Compares major version numbers to determine if this
     * represents a significant update.
     *
     * @return true if major version number increased
     */
    fun isMajorUpdate(): Boolean {
        val currentMajor = currentVersion.split(".").firstOrNull()?.toIntOrNull() ?: return false
        val newMajor = newVersion.split(".").firstOrNull()?.toIntOrNull() ?: return false
        return newMajor > currentMajor
    }

    /**
     * Check if this is a minor version update.
     *
     * @return true if minor version increased (same major)
     */
    fun isMinorUpdate(): Boolean {
        val currentParts = currentVersion.split(".")
        val newParts = newVersion.split(".")

        if (currentParts.size < 2 || newParts.size < 2) return false

        val currentMajor = currentParts[0].toIntOrNull() ?: return false
        val newMajor = newParts[0].toIntOrNull() ?: return false
        val currentMinor = currentParts[1].toIntOrNull() ?: return false
        val newMinor = newParts[1].toIntOrNull() ?: return false

        return newMajor == currentMajor && newMinor > currentMinor
    }

    /**
     * Check if this is a patch version update.
     *
     * @return true if only patch version increased
     */
    fun isPatchUpdate(): Boolean {
        return !isMajorUpdate() && !isMinorUpdate()
    }

    /**
     * Get update type as string.
     *
     * @return "Major", "Minor", or "Patch"
     */
    fun getUpdateType(): String {
        return when {
            isMajorUpdate() -> "Major"
            isMinorUpdate() -> "Minor"
            else -> "Patch"
        }
    }
}
