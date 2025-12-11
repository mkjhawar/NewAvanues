package com.augmentalis.webavanue.domain.model

/**
 * Constants for new tab page URLs.
 *
 * Used by TabViewModel to determine what content to load when creating a new tab
 * based on the user's newTabPage setting.
 */
object NewTabUrl {
    /**
     * Blank page - loads nothing
     */
    const val BLANK = "about:blank"

    /**
     * Special internal URL for top sites grid
     * The WebView will intercept this and show a composable with most visited sites
     */
    const val TOP_SITES = "avanues://newtab?mode=top_sites"

    /**
     * Special internal URL for most visited sites list
     */
    const val MOST_VISITED = "avanues://newtab?mode=most_visited"

    /**
     * Special internal URL for speed dial favorites grid
     */
    const val SPEED_DIAL = "avanues://newtab?mode=speed_dial"

    /**
     * Special internal URL for news feed
     */
    const val NEWS_FEED = "avanues://newtab?mode=news_feed"

    /**
     * URL scheme prefix for all internal new tab pages
     */
    const val INTERNAL_SCHEME = "avanues://"

    /**
     * Check if a URL is an internal new tab page
     */
    fun isInternalNewTabUrl(url: String): Boolean {
        return url.startsWith(INTERNAL_SCHEME)
    }
}
