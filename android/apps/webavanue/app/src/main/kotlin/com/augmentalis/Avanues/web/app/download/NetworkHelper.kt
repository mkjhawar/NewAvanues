package com.augmentalis.webavanue.app.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * Network utility helper for download management
 *
 * Provides network state checking for WiFi-only download enforcement.
 * Handles API level differences in Android's connectivity APIs.
 *
 * ## Usage
 * ```kotlin
 * val networkHelper = NetworkHelper(context)
 *
 * if (downloadOverWiFiOnly && !networkHelper.isWiFiConnected()) {
 *     showError("WiFi required for downloads")
 *     return
 * }
 *
 * // Proceed with download
 * ```
 *
 * @param context Android context
 */
class NetworkHelper(private val context: Context) {

    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    /**
     * Check if device is connected to WiFi network
     *
     * Uses appropriate API based on Android version:
     * - API 23+: NetworkCapabilities for accurate detection
     * - API 21-22: Legacy NetworkInfo API
     *
     * **Returns `true` when**:
     * - Connected to WiFi network
     * - WiFi network has internet capability
     *
     * **Returns `false` when**:
     * - Connected to cellular/mobile data
     * - Connected to Ethernet (treated as non-WiFi)
     * - No network connection
     * - WiFi connected but no internet
     *
     * ## API Level Handling
     * ```
     * API 23+ (Marshmallow):
     *   - Uses NetworkCapabilities.TRANSPORT_WIFI
     *   - Checks NET_CAPABILITY_INTERNET
     *   - More reliable and future-proof
     *
     * API 21-22 (Lollipop):
     *   - Uses NetworkInfo.getType()
     *   - Checks TYPE_WIFI
     *   - Legacy method (deprecated but still works)
     * ```
     *
     * @return true if connected to WiFi, false otherwise
     */
    fun isWiFiConnected(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // API 23+: Use NetworkCapabilities
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

            } else {
                // API 21-22: Use legacy NetworkInfo
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo ?: return false

                @Suppress("DEPRECATION")
                networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI
            }
        } catch (e: Exception) {
            // On error, assume not connected to WiFi (safer for WiFi-only enforcement)
            false
        }
    }

    /**
     * Check if device is connected to cellular/mobile data
     *
     * Useful for showing warnings about data usage when downloading
     * large files over cellular connection.
     *
     * @return true if connected to cellular, false otherwise
     */
    fun isCellularConnected(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo ?: return false

                @Suppress("DEPRECATION")
                networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_MOBILE
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if any network connection is available
     *
     * Returns true for WiFi, cellular, Ethernet, VPN, or any other
     * network with internet capability.
     *
     * @return true if connected to any network, false if offline
     */
    fun isConnected(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo ?: return false

                @Suppress("DEPRECATION")
                networkInfo.isConnected
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get user-friendly network type description
     *
     * Returns human-readable string describing current network connection:
     * - "WiFi"
     * - "Cellular"
     * - "Ethernet"
     * - "VPN"
     * - "No connection"
     *
     * Useful for displaying in error messages or logs.
     *
     * @return Network type as string
     */
    fun getNetworkTypeDescription(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return "No connection"
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                    ?: return "No connection"

                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                    else -> "Unknown"
                }

            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                    ?: return "No connection"

                @Suppress("DEPRECATION")
                when (networkInfo.type) {
                    ConnectivityManager.TYPE_WIFI -> "WiFi"
                    ConnectivityManager.TYPE_MOBILE -> "Cellular"
                    ConnectivityManager.TYPE_ETHERNET -> "Ethernet"
                    ConnectivityManager.TYPE_VPN -> "VPN"
                    else -> "Unknown"
                }
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Check if device is metered network (cellular or metered WiFi)
     *
     * Metered networks are typically:
     * - Cellular/mobile data
     * - WiFi hotspots shared from phones
     * - WiFi networks marked as "metered" in Android settings
     *
     * Use this to warn users about potential data charges when
     * downloading large files.
     *
     * @return true if network is metered, false otherwise
     */
    fun isMeteredNetwork(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                connectivityManager.isActiveNetworkMetered
            } else {
                // On older APIs, assume cellular is metered, WiFi is not
                isCellularConnected()
            }
        } catch (e: Exception) {
            // Assume metered on error (safer for user)
            true
        }
    }

    /**
     * Get download permission error message based on network state
     *
     * Returns appropriate user-facing error message when WiFi-only
     * setting blocks a download.
     *
     * ## Message Examples
     * - "WiFi required for downloads. Currently connected to Cellular."
     * - "WiFi required for downloads. No network connection."
     * - null (if WiFi is connected)
     *
     * @param wifiOnlySetting Whether WiFi-only downloads is enabled
     * @return Error message or null if download allowed
     */
    fun getDownloadBlockedMessage(wifiOnlySetting: Boolean): String? {
        if (!wifiOnlySetting) {
            // WiFi-only not enforced
            return null
        }

        return when {
            isWiFiConnected() -> null // WiFi connected - download allowed
            isCellularConnected() -> "WiFi required for downloads. Currently connected to Cellular data."
            !isConnected() -> "WiFi required for downloads. No network connection available."
            else -> "WiFi required for downloads. Please connect to WiFi and try again."
        }
    }

    companion object {
        /**
         * Quick check if WiFi is connected (without creating NetworkHelper instance)
         *
         * Convenience method for one-off checks. For multiple checks,
         * create a NetworkHelper instance instead to reuse ConnectivityManager.
         *
         * @param context Android context
         * @return true if connected to WiFi, false otherwise
         */
        fun isWiFiConnected(context: Context): Boolean {
            return NetworkHelper(context).isWiFiConnected()
        }
    }
}
