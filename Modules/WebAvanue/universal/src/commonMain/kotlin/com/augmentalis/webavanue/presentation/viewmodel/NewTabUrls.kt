package com.augmentalis.webavanue.ui.viewmodel

/**
 * Constants for new tab page URLs.
 *
 * These special URLs are used when creating new tabs based on the user's
 * newTabPage preference. The WebView will intercept these URLs and display
 * custom composable screens instead of navigating to actual web pages.
 *
 * @see com.augmentalis.webavanue.domain.model.BrowserSettings.NewTabPage
 */
object NewTabUrls {
    /**
     * URL scheme prefix for Avanues internal pages
     */
    private const val SCHEME = "avanues://newtab?mode="

    /**
     * Top sites page - Shows a grid of most frequently visited sites
     */
    const val TOP_SITES = "${SCHEME}top_sites"

    /**
     * Most visited page - Shows sites sorted by visit frequency
     */
    const val MOST_VISITED = "${SCHEME}most_visited"

    /**
     * Speed dial page - Shows user's favorited/pinned sites in a grid
     */
    const val SPEED_DIAL = "${SCHEME}speed_dial"

    /**
     * News feed page - Shows news articles from configured sources
     */
    const val NEWS_FEED = "${SCHEME}news_feed"

    /**
     * Checks if a URL is a special new tab page URL
     */
    fun isNewTabUrl(url: String): Boolean {
        return url.startsWith("avanues://newtab")
    }
}
