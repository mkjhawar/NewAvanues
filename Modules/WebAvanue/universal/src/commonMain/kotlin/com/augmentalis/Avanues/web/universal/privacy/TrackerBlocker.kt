package com.augmentalis.Avanues.web.universal.privacy

/**
 * Tracker Blocker for WebAvanue
 *
 * Blocks tracking scripts, analytics, and fingerprinting attempts.
 * Protects user privacy by preventing third-party tracking.
 *
 * PHASE 2: Privacy & Security
 */
class TrackerBlocker {

    private val trackerPatterns = listOf(
        // Social media trackers
        Regex(".*facebook\\.com/tr.*"),
        Regex(".*facebook\\.net.*"),
        Regex(".*connect\\.facebook.*"),
        Regex(".*pixel\\.facebook.*"),
        Regex(".*twitter\\.com/i/.*"),
        Regex(".*analytics\\.twitter\\.com.*"),
        Regex(".*linkedin\\.com/px.*"),
        Regex(".*instagram\\.com/.*logging.*"),

        // Analytics trackers
        Regex(".*google-analytics\\.com.*"),
        Regex(".*googletagmanager\\.com.*"),
        Regex(".*googletagservices\\.com.*"),
        Regex(".*scorecardresearch\\.com.*"),
        Regex(".*quantserve\\.com.*"),
        Regex(".*chartbeat\\.com.*"),
        Regex(".*hotjar\\.com.*"),
        Regex(".*mouseflow\\.com.*"),
        Regex(".*crazyegg\\.com.*"),
        Regex(".*luckyorange\\.com.*"),
        Regex(".*fullstory\\.com.*"),

        // Generic tracking patterns
        Regex(".*track.*"),
        Regex(".*pixel.*"),
        Regex(".*beacon.*"),
        Regex(".*telemetry.*"),
        Regex(".*analytics.*"),
        Regex(".*tracking.*"),

        // Fingerprinting
        Regex(".*fingerprint.*"),
        Regex(".*devicefingerprint.*"),
        Regex(".*clientfingerprint.*"),

        // Ad tracking
        Regex(".*tracking\\..*"),
        Regex(".*collector.*"),
        Regex(".*metrics.*")
    )

    fun shouldBlock(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return trackerPatterns.any { it.matches(lowerUrl) }
    }

    // Statistics
    private var blockedCount = 0

    fun getBlockedCount(): Int = blockedCount

    fun incrementBlocked() {
        blockedCount++
    }

    fun resetStats() {
        blockedCount = 0
    }
}
