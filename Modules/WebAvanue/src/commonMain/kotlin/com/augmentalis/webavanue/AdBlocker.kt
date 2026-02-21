package com.augmentalis.webavanue

/**
 * Ad Blocker for WebAvanue
 *
 * Uses pattern matching to block common ad servers and ad-related URLs.
 * Lightweight implementation focused on major ad networks.
 *
 * PHASE 2: Privacy & Security
 */
class AdBlocker {

    private val adPatterns = listOf(
        // Common ad servers
        Regex(".*doubleclick\\.net.*"),
        Regex(".*googlesyndication\\.com.*"),
        Regex(".*googleadservices\\.com.*"),
        Regex(".*adservice\\.google\\..*"),
        Regex(".*amazon-adsystem\\.com.*"),
        Regex(".*advertising\\.com.*"),
        Regex(".*adbrite\\.com.*"),
        Regex(".*adbureau\\.net.*"),
        Regex(".*admob\\.com.*"),
        Regex(".*adsense\\..*"),
        Regex(".*adserver.*"),
        Regex(".*ad\\..*"),
        Regex(".*ads\\..*"),

        // Common ad paths
        Regex(".*/ads/.*"),
        Regex(".*/ad/.*"),
        Regex(".*/banner.*"),
        Regex(".*/advert.*"),
        Regex(".*/sponsor.*"),
        Regex(".*_ads\\..*"),
        Regex(".*-ads\\..*"),

        // Ad networks
        Regex(".*adnxs\\.com.*"),  // AppNexus
        Regex(".*rubiconproject\\.com.*"),
        Regex(".*openx\\.net.*"),
        Regex(".*pubmatic\\.com.*"),
        Regex(".*criteo\\..*"),
        Regex(".*outbrain\\.com.*"),
        Regex(".*taboola\\.com.*"),
    )

    /**
     * Checks if URL should be blocked
     * @param url The resource URL to check
     * @return true if should be blocked, false otherwise
     */
    fun shouldBlock(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return adPatterns.any { it.matches(lowerUrl) }
    }

    // Statistics — @Volatile for cross-thread visibility (network + UI threads)
    @kotlin.concurrent.Volatile
    private var blockedCount = 0

    fun getBlockedCount(): Int = blockedCount

    @Synchronized
    fun incrementBlocked() {
        blockedCount++
    }

    @Synchronized
    fun resetStats() {
        blockedCount = 0
    }
}
