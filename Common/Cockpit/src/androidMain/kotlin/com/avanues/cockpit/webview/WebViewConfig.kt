package com.avanues.cockpit.webview

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * WebView configuration for Cockpit windows
 *
 * Ported from Task_Cockpit BrowserUtils.kt with Cockpit-specific enhancements.
 * Configures WebView instances with optimal settings for spatial workspace usage.
 *
 * **Key Features:**
 * - JavaScript enabled for voice command injection
 * - DOM storage for state persistence
 * - Mixed content allowed (HTTP + HTTPS)
 * - Zoom controls for accessibility
 * - Cookie management for session persistence
 *
 * **Voice-First Integration:**
 * - JavaScript enabled to inject voice command handlers
 * - WebView becomes voice-controllable via JS injection
 * - State persistence supports "Resume where I left off" commands
 */
object WebViewConfig {

    /**
     * Standard configuration for Cockpit WebView windows
     *
     * Applied to all WEB_APP type windows for consistent behavior.
     * Enables JavaScript, DOM storage, mixed content, and zoom controls.
     *
     * @param webView The WebView instance to configure
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun applyStandardConfig(webView: WebView) {
        webView.settings.apply {
            // JavaScript - REQUIRED for voice command injection
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true

            // DOM Storage - Required for complex web apps
            domStorageEnabled = true
            databaseEnabled = true

            // Caching - Enable for offline support
            cacheMode = WebSettings.LOAD_DEFAULT
            setAppCacheEnabled(true)

            // Mixed Content - Allow HTTP + HTTPS (common in web apps)
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            // Zoom - Enable for accessibility
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false // Hide on-screen controls (voice-controlled)

            // Load Images - Enable (but could be disabled for performance)
            loadsImagesAutomatically = true
            blockNetworkImage = false

            // Viewport - Support responsive design
            useWideViewPort = true
            loadWithOverviewMode = true

            // Media - Enable autoplay (for YouTube, etc.)
            mediaPlaybackRequiresUserGesture = false

            // Safe Browsing - Enable for security
            safeBrowsingEnabled = true

            // Text - Enable text selection (for "Read this" voice commands)
            textZoom = 100 // Default 100%

            // User Agent - Use default (can be customized per window)
            userAgentString = WebSettings.getDefaultUserAgent(webView.context)
        }

        // Enable cookies - Required for login persistence
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
    }

    /**
     * YouTube-optimized configuration
     *
     * Special settings for YouTube video playback in Cockpit windows.
     * Enables autoplay, fullscreen, and picture-in-picture.
     *
     * Voice commands: "Play video", "Pause", "Fullscreen", "Picture in picture"
     *
     * @param webView The WebView instance to configure
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun applyYouTubeConfig(webView: WebView) {
        applyStandardConfig(webView) // Start with standard config

        webView.settings.apply {
            // Media - Autoplay for seamless video experience
            mediaPlaybackRequiresUserGesture = false

            // Fullscreen - Enable for theater mode
            javaScriptCanOpenWindowsAutomatically = true

            // User Agent - Desktop mode for full features
            userAgentString = DESKTOP_USER_AGENT
        }
    }

    /**
     * Minimal configuration for widgets
     *
     * Lightweight settings for simple widgets (clock, weather, shortcuts).
     * Disables unnecessary features for performance.
     *
     * @param webView The WebView instance to configure
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun applyWidgetConfig(webView: WebView) {
        webView.settings.apply {
            // JavaScript - Still enabled for voice commands
            javaScriptEnabled = true

            // Storage - Minimal
            domStorageEnabled = true
            databaseEnabled = false
            cacheMode = WebSettings.LOAD_NO_CACHE

            // Mixed Content - Block for security
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

            // Zoom - Disable (fixed size widgets)
            setSupportZoom(false)

            // Images - Enable (widgets often icon-based)
            loadsImagesAutomatically = true

            // Viewport - Fixed
            useWideViewPort = false
            loadWithOverviewMode = false
        }
    }

    /**
     * Checks if a URL is a YouTube video page
     *
     * Ported from BrowserUtils.isYouTube() and checkIsYoutubeVideoView().
     * Used to apply YouTube-specific configuration.
     *
     * @param url The URL to check
     * @return True if URL is a YouTube video page
     */
    fun isYouTubeVideo(url: String): Boolean {
        // Basic check
        val hasYouTube = url.contains("youtube.com", ignoreCase = true) ||
                url.contains("youtu.be", ignoreCase = true)

        if (!hasYouTube) return false

        // Pattern check (from old code)
        val pattern = Regex(
            "^((?:https?:)?//)?((?:www|m)\\.)?(youtube(-nocookie)?\\.com|youtu.be)(/(?:[\\w\\-]+\\?v=|embed/|v/)?)([\\w\\-]+)(\\S+)?\$"
        )
        if (!pattern.matches(url)) return false

        // Exclude API/tracking URLs (from old code)
        val invalidPrefixes = listOf(
            "https://www.youtube.com/youtubei/v1/att/",
            "https://m.youtube.com/static/",
            "https://m.youtube.com/s/",
            "https://m.youtube.com/youtubei/v1/",
            "https://m.youtube.com/generate",
            "https://m.youtube.com/youtubei/v1/log_event",
            "https://m.youtube.com/api/stats/",
            "https://www.youtube.com/pcs/activeview",
            "https://www.youtube.com/s/",
            "https://www.youtube.com/youtubei/v1/log_event",
            "https://www.youtube.com/api/stats/",
            "https://www.youtube.com/pagead"
        )

        return invalidPrefixes.none { url.startsWith(it) }
    }

    /**
     * Extracts display-friendly domain name from URL
     *
     * Ported from BrowserUtils.getDisplayDomainName().
     * Used for window titles and voice announcements.
     *
     * Example:
     * - Input: "https://www.github.com/anthropics/claude-code"
     * - Output: "github.com"
     * - Voice: "GitHub website"
     *
     * @param url The URL to extract domain from
     * @return Display-friendly domain name, or original URL if extraction fails
     */
    fun getDisplayDomain(url: String?): String {
        if (url.isNullOrEmpty()) return ""

        return try {
            val uri = java.net.URI(url)
            val host = uri.host ?: return url

            // Remove "www." prefix
            if (host.startsWith("www.")) {
                host.substring(4)
            } else {
                host
            }
        } catch (e: Exception) {
            url
        }
    }

    /**
     * Clears WebView cache
     *
     * Voice command: "Clear browser cache"
     *
     * @param webView The WebView instance to clear
     */
    fun clearCache(webView: WebView) {
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()

        // Clear cookies
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    /**
     * Clears cookies for a specific domain
     *
     * Voice command: "Clear cookies for [domain]"
     *
     * @param domain The domain to clear cookies for
     */
    fun clearCookiesForDomain(domain: String) {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(domain)

        cookies?.split(";")?.forEach { cookie ->
            val cookieParts = cookie.split("=")
            if (cookieParts.size >= 2) {
                val cookieName = cookieParts[0].trim()
                val expiredCookie = "$cookieName=; Max-Age=0; path=/"
                cookieManager.setCookie(domain, expiredCookie)
            }
        }

        cookieManager.flush()
    }

    // Constants

    /** Desktop User Agent (for sites that block mobile browsers) */
    private const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    /** Known login URLs (for voice commands like "Sign in to Google") */
    object LoginUrls {
        const val GOOGLE = "https://myaccount.google.com/?utm_source=sign_in_no_continue"
        const val GOOGLE_LOGOUT = "https://accounts.google.com/Logout"
        const val MICROSOFT = "https://account.microsoft.com/?refd=account.microsoft.com&refp=signedout-index"
        const val MICROSOFT_LOGOUT = "https://www.microsoft365.com/estslogout"
    }
}
