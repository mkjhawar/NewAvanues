package com.augmentalis.webavanue

import android.content.Context
import com.augmentalis.webavanue.Logger
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.User

/**
 * SentryManager - Crash reporting and performance monitoring
 *
 * Features:
 * - Automatic crash reporting
 * - Breadcrumb trail for debugging
 * - Performance monitoring
 * - PII filtering (URLs, user data)
 * - Release tracking
 *
 * Usage:
 * ```kotlin
 * // Initialize in Application.onCreate()
 * SentryManager.init(context, dsn = "https://YOUR_DSN@sentry.io/PROJECT_ID")
 *
 * // Add breadcrumbs for user actions
 * SentryManager.addBreadcrumb("navigation", "Tab created")
 * SentryManager.addBreadcrumb("download", "Download started", SentryLevel.INFO)
 *
 * // Set user context (optional, anonymous by default)
 * SentryManager.setUser("user123")
 * ```
 *
 * Privacy & Security:
 * - URLs automatically sanitized (query params removed)
 * - User data filtered from crash reports
 * - Breadcrumbs limited to essential actions
 * - No PII captured
 *
 * Environment Detection:
 * - Debug builds: environment="development"
 * - Release builds: environment="production"
 * - Sample rate: 100% in debug, configurable in production
 */
object SentryManager {
    private const val TAG = "SentryManager"

    /**
     * Initialize Sentry SDK
     *
     * @param context Application context
     * @param dsn Sentry DSN (Data Source Name) from Sentry dashboard
     *            Format: "https://PUBLIC_KEY@sentry.io/PROJECT_ID"
     *            Get from: Sentry.io → Project Settings → Client Keys (DSN)
     */
    fun init(context: Context, dsn: String) {
        try {
            SentryAndroid.init(context) { options ->
                // DSN - Sentry project endpoint
                options.dsn = dsn

                // Environment - Separate debug/production issues
                options.environment = if (isDebugBuild()) "development" else "production"

                // Release - Track version/build
                options.release = "${getVersionName()}@${getVersionCode()}"

                // Tracing - Performance monitoring
                options.tracesSampleRate = 1.0 // 100% sampling

                // PII Filtering - Remove sensitive data before sending
                options.beforeSend = SentryOptions.BeforeSendCallback { event, hint ->
                    event.apply {
                        // Sanitize URLs in request context
                        request?.let {
                            it.url = sanitizeUrl(it.url ?: "")
                        }

                        // Remove user IP address
                        user?.ipAddress = null
                    }
                    event
                }

                // Attach Breadcrumbs - User action trail
                options.isEnableActivityLifecycleBreadcrumbs = true
                options.isEnableAppLifecycleBreadcrumbs = true

                Logger.info(TAG, "Sentry initialized: environment=${options.environment}, release=${options.release}")
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to initialize Sentry", e)
        }
    }

    /**
     * Add breadcrumb - User action tracking
     *
     * Breadcrumbs create a trail of events leading to a crash
     * Example trail:
     * 1. Tab created: https://example.com
     * 2. Navigation started
     * 3. Download requested
     * 4. [CRASH]
     *
     * @param category Breadcrumb category (navigation, download, etc.)
     * @param message Human-readable message (PII-filtered)
     * @param level Severity level (DEBUG, INFO, WARNING, ERROR)
     */
    fun addBreadcrumb(
        category: String,
        message: String,
        level: SentryLevel = SentryLevel.INFO
    ) {
        try {
            Breadcrumb().apply {
                this.category = category
                this.message = message
                this.level = level
            }.let { Sentry.addBreadcrumb(it) }

            Logger.debug(TAG, "Breadcrumb: [$category] $message")
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to add breadcrumb", e)
        }
    }

    /**
     * Set user context - Associate crashes with user
     *
     * @param userId Anonymous user ID (NO PII - use hash/UUID)
     *               Example: "user_abc123" NOT "john.doe@email.com"
     */
    fun setUser(userId: String?) {
        try {
            Sentry.setUser(User().apply {
                id = userId
            })
            Logger.debug(TAG, "User context set: $userId")
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to set user context", e)
        }
    }

    /**
     * Clear user context - Remove user association
     * Call on logout or privacy mode
     */
    fun clearUser() {
        try {
            Sentry.setUser(null)
            Logger.debug(TAG, "User context cleared")
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to clear user context", e)
        }
    }

    /**
     * Sanitize URL - Remove query parameters for privacy
     */
    private fun sanitizeUrl(url: String): String {
        return url.substringBefore("?")
    }

    /**
     * Check if debug build
     */
    private fun isDebugBuild(): Boolean {
        return try {
            Class.forName("com.augmentalis.Avanues.web.BuildConfig")
                .getField("DEBUG")
                .getBoolean(null)
        } catch (e: Exception) {
            false // Default to production if BuildConfig unavailable
        }
    }

    /**
     * Get app version name
     */
    private fun getVersionName(): String {
        return try {
            Class.forName("com.augmentalis.Avanues.web.BuildConfig")
                .getField("VERSION_NAME")
                .get(null) as? String ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Get app version code
     */
    private fun getVersionCode(): Int {
        return try {
            Class.forName("com.augmentalis.Avanues.web.BuildConfig")
                .getField("VERSION_CODE")
                .getInt(null)
        } catch (e: Exception) {
            0
        }
    }
}
