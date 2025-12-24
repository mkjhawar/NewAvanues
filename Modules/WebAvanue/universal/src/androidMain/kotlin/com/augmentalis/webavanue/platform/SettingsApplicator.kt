package com.augmentalis.webavanue.platform

import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.webavanue.domain.model.BrowserSettings.*
import com.augmentalis.webavanue.domain.validation.SettingsValidation

/**
 * Applies BrowserSettings to Android WebView.
 *
 * This class bridges the gap between the BrowserSettings data model
 * and Android WebView configuration, fixing the critical integration
 * issue where settings were stored but never applied.
 *
 * Responsibilities:
 * - Translate settings model to WebView configuration
 * - Validate setting values before application
 * - Handle errors gracefully with user feedback
 * - Support incremental updates (no full reload when possible)
 *
 * @see BrowserSettings for the complete settings model
 * @see WebView for Android WebView API
 */
class SettingsApplicator {

    /**
     * Apply all settings to WebView.
     *
     * Call on WebView initialization and when settings change.
     * This is the main entry point for settings application.
     *
     * @param webView Target WebView instance
     * @param settings Complete BrowserSettings to apply
     * @return Result.success if all settings applied, Result.failure with exception if errors occurred
     */
    fun applySettings(webView: WebView, settings: BrowserSettings): Result<Unit> {
        return try {
            Log.i("NaveenViewModel", "applySettings: $settings")
            // Validate and auto-correct settings
            val validationResult = SettingsValidation.validate(settings)

            // Log validation warnings
            validationResult.warnings.forEach { warning ->
                Log.w(TAG, "Settings validation: $warning")
            }

            // Log validation errors (non-blocking, we use corrected values)
            validationResult.errors.forEach { error ->
                Log.e(TAG, "Settings validation error: $error")
            }

            // Use corrected settings for application
            val correctedSettings = validationResult.correctedSettings

            Log.d(TAG, "Applying settings to WebView (validated & corrected)")

            applyPrivacySettings(webView, correctedSettings)
            applyDisplaySettings(webView, correctedSettings)
            applyPerformanceSettings(webView, correctedSettings)
            applyWebXRSettings(webView, correctedSettings)

            Log.d(TAG, "Settings applied successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply settings", e)
            Result.failure(e)
        }
    }

    /**
     * Apply privacy and security settings.
     */
    private fun applyPrivacySettings(webView: WebView, settings: BrowserSettings) {
        webView.settings.apply {
            // JavaScript
            javaScriptEnabled = settings.enableJavaScript
            domStorageEnabled = settings.enableJavaScript
            databaseEnabled = settings.enableJavaScript

            // Pop-ups
            javaScriptCanOpenWindowsAutomatically = !settings.blockPopups

            // Cookies
            CookieManager.getInstance().apply {
                setAcceptCookie(settings.enableCookies)
                setAcceptThirdPartyCookies(
                    webView,
                    settings.enableCookies && !settings.blockTrackers
                )
            }

            // Mixed content (tracker blocking)
            mixedContentMode = if (settings.blockTrackers) {
                WebSettings.MIXED_CONTENT_NEVER_ALLOW
            } else {
                WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }

            // WebRTC (privacy consideration)
            if (!settings.enableWebRTC) {
                // WebRTC disabled for privacy
                mediaPlaybackRequiresUserGesture = true
            }

            // File access (security)
            allowFileAccess = false  // Always disabled for security
            allowContentAccess = false
        }
    }

    /**
     * Apply display and UI settings.
     */
    private fun applyDisplaySettings(webView: WebView, settings: BrowserSettings) {
        webView.settings.apply {
            // Font size
            textZoom = when (settings.fontSize) {
                FontSize.TINY -> 75
                FontSize.SMALL -> 90
                FontSize.MEDIUM -> 100
                FontSize.LARGE -> 125
                FontSize.HUGE -> 150
            }

            // Images
            loadsImagesAutomatically = settings.showImages

            // Zoom controls
            setSupportZoom(settings.forceZoom)
            builtInZoomControls = settings.forceZoom
            displayZoomControls = false  // Hide zoom controls overlay

            // Desktop mode
            if (settings.useDesktopMode) {
                loadWithOverviewMode = true
                useWideViewPort = true

                // Apply desktop mode zoom (constrained to 50-200%)
                val zoom = settings.desktopModeDefaultZoom.coerceIn(50, 200)
                // initialScale is set on WebView, not settings
                webView.setInitialScale(zoom)

                // Set user agent to desktop
                userAgentString = DESKTOP_USER_AGENT
            } else {
                loadWithOverviewMode = false
                useWideViewPort = false

                // NOTE: Initial scale is now set in WebViewContainer based on mode and orientation
                // using mobilePortraitScale and mobileLandscapeScale from settings

                // Reset to mobile user agent
                userAgentString = null  // Use default mobile UA
            }
        }
    }

    /**
     * Apply performance and optimization settings.
     */
    private fun applyPerformanceSettings(webView: WebView, settings: BrowserSettings) {
        webView.settings.apply {
            // Hardware acceleration priority
            // Note: Actual HW acceleration requires manifest attribute
            setRenderPriority(
                if (settings.hardwareAcceleration)
                    WebSettings.RenderPriority.HIGH
                else
                    WebSettings.RenderPriority.NORMAL
            )

            // Cache mode (data saver)
            cacheMode = if (settings.dataSaver) {
                WebSettings.LOAD_CACHE_ELSE_NETWORK
            } else {
                WebSettings.LOAD_DEFAULT
            }

            // App cache (deprecated in API 33+ but some sites still use)
            // Note: This setting is ignored in Android API 33+
            // No replacement - modern sites use Service Workers instead

            // Text reflow for readability
            layoutAlgorithm = if (settings.textReflow) {
                WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
            } else {
                WebSettings.LayoutAlgorithm.NORMAL
            }
        }
    }

    /**
     * Apply WebXR settings.
     *
     * Currently placeholder - WebXR API integration pending.
     */
    private fun applyWebXRSettings(webView: WebView, settings: BrowserSettings) {
        // WebXR settings will be applied when WebXR API is integrated
        // Placeholder for future implementation

        if (settings.enableWebXR) {
            Log.d(TAG, "WebXR enabled: AR=${settings.enableAR}, VR=${settings.enableVR}")
            // TODO: Enable WebXR polyfill or native support
        }
    }

    /**
     * Apply incremental setting change without full reload.
     *
     * Attempts to apply a single setting change efficiently.
     * Returns true if successful without reload, false if reload required.
     *
     * @param webView Target WebView instance
     * @param settingKey Setting identifier (e.g., "fontSize", "enableJavaScript")
     * @param value New value for the setting
     * @return true if applied without reload, false if reload required for this setting
     */
    fun applyIncrementalUpdate(
        webView: WebView,
        settingKey: String,
        value: Any
    ): Boolean {
        return try {
            when (settingKey) {
                "fontSize" -> {
                    val zoom = when (value as FontSize) {
                        FontSize.TINY -> 75
                        FontSize.SMALL -> 90
                        FontSize.MEDIUM -> 100
                        FontSize.LARGE -> 125
                        FontSize.HUGE -> 150
                    }
                    webView.settings.textZoom = zoom
                    true // No reload needed
                }

                "showImages" -> {
                    webView.settings.loadsImagesAutomatically = value as Boolean
                    false // Reload required to load/hide images
                }

                "enableJavaScript" -> {
                    webView.settings.javaScriptEnabled = value as Boolean
                    false // Reload required for safety
                }

                "enableCookies" -> {
                    CookieManager.getInstance().setAcceptCookie(value as Boolean)
                    false // Reload recommended
                }

                "blockPopups" -> {
                    webView.settings.javaScriptCanOpenWindowsAutomatically = !(value as Boolean)
                    true // No reload needed
                }

                "forceZoom" -> {
                    webView.settings.setSupportZoom(value as Boolean)
                    webView.settings.builtInZoomControls = value as Boolean
                    true // No reload needed
                }

                else -> {
                    Log.w(TAG, "Unknown setting for incremental update: $settingKey")
                    false // Unknown setting, reload to be safe
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed incremental update for $settingKey", e)
            false // Error occurred, reload required
        }
    }

    companion object {
        private const val TAG = "SettingsApplicator"

        /**
         * Desktop user agent string.
         * Mimics Chrome on Windows for maximum compatibility.
         */
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
    }
}
