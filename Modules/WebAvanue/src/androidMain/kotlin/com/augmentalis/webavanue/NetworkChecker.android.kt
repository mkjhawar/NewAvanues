package com.augmentalis.webavanue

import android.content.Context

/**
 * Android implementation of NetworkChecker
 *
 * Wraps existing NetworkHelper from android module to provide
 * KMP-compatible network checking.
 *
 * **Requires**: Android context to be set via initialize()
 *
 * ## Usage:
 * ```kotlin
 * // In MainActivity or Application onCreate:
 * NetworkChecker.initialize(applicationContext)
 *
 * // In ViewModel or Composable:
 * val checker = NetworkChecker()
 * if (!checker.isWiFiConnected()) {
 *     showError(checker.getWiFiRequiredMessage())
 * }
 * ```
 */
actual class NetworkChecker {
    private val helper: NetworkHelper by lazy {
        val context = contextProvider
            ?: throw IllegalStateException("NetworkChecker not initialized. Call NetworkChecker.initialize(context) first.")
        NetworkHelper(context)
    }

    actual fun isWiFiConnected(): Boolean {
        return helper.isWiFiConnected()
    }

    actual fun isCellularConnected(): Boolean {
        return helper.isCellularConnected()
    }

    actual fun isConnected(): Boolean {
        return helper.isConnected()
    }

    actual fun getWiFiRequiredMessage(): String? {
        return helper.getDownloadBlockedMessage(wifiOnlySetting = true)
    }

    companion object {
        /**
         * Application context for creating NetworkHelper instances
         *
         * Must be set via initialize() before NetworkChecker can be used.
         */
        private var contextProvider: Context? = null

        /**
         * Initialize NetworkChecker with application context
         *
         * **MUST be called** before creating NetworkChecker instances.
         * Typically called in Application.onCreate() or MainActivity.onCreate().
         *
         * @param context Application or Activity context
         */
        fun initialize(context: Context) {
            contextProvider = context.applicationContext
        }

        /**
         * Check if NetworkChecker has been initialized
         *
         * @return true if initialize() was called, false otherwise
         */
        fun isInitialized(): Boolean {
            return contextProvider != null
        }
    }
}
