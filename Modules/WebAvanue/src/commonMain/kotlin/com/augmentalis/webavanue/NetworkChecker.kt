package com.augmentalis.webavanue

/**
 * Platform-specific network checker for download management
 *
 * Provides WiFi detection and network type checking for enforcing
 * downloadOverWiFiOnly setting.
 *
 * ## Platform Implementations:
 * - **Android**: Uses ConnectivityManager APIs (API 23+ vs 21-22 aware)
 * - **iOS**: Uses Network.framework
 * - **Desktop**: Uses platform network APIs
 *
 * ## Usage:
 * ```kotlin
 * val networkChecker = NetworkChecker()
 *
 * if (settings.downloadOverWiFiOnly && !networkChecker.isWiFiConnected()) {
 *     showError(networkChecker.getBlockedMessage())
 *     return
 * }
 *
 * // Proceed with download
 * ```
 *
 * @see com.augmentalis.Avanues.web.app.download.NetworkHelper (Android implementation)
 */
expect class NetworkChecker() {
    /**
     * Check if device is connected to WiFi network
     *
     * Returns true if:
     * - Connected to WiFi network
     * - WiFi has internet capability
     *
     * Returns false if:
     * - Connected to cellular/mobile data
     * - Connected to Ethernet (treated as non-WiFi)
     * - No network connection
     * - WiFi connected but no internet
     *
     * @return true if connected to WiFi, false otherwise
     */
    fun isWiFiConnected(): Boolean

    /**
     * Check if device is connected to cellular/mobile data
     *
     * @return true if connected to cellular, false otherwise
     */
    fun isCellularConnected(): Boolean

    /**
     * Check if any network connection is available
     *
     * @return true if connected to any network, false if offline
     */
    fun isConnected(): Boolean

    /**
     * Get user-friendly error message when WiFi-only blocks download
     *
     * Returns appropriate message based on current network state:
     * - "WiFi required. Currently on Cellular."
     * - "WiFi required. No network connection."
     * - null (if WiFi is connected)
     *
     * @return Error message or null if download allowed
     */
    fun getWiFiRequiredMessage(): String?
}
