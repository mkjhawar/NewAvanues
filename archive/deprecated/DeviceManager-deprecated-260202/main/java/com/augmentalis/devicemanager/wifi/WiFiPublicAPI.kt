// Copyright (c) 2025 Augmentalis, Inc.
// Author: Manoj Jhawar
// Purpose: Public API for WiFi operations

package com.augmentalis.devicemanager.wifi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector
import com.augmentalis.devicemanager.network.WiFiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Public API for WiFi operations
 *
 * Provides a simplified facade for WiFi functionality:
 * - Checking if WiFi is enabled
 * - Getting current connected network information
 * - Getting WiFi signal strength
 * - Scanning for available networks
 * - Monitoring WiFi state changes
 *
 * This class wraps the complex internal WiFiManager to provide a clean,
 * easy-to-use public interface for consuming applications.
 *
 * Requirements:
 * - ACCESS_WIFI_STATE permission (basic WiFi info)
 * - ACCESS_FINE_LOCATION permission (network scanning, Android 6+)
 * - CHANGE_WIFI_STATE permission (enable/disable WiFi, deprecated on Android 10+)
 *
 * Usage:
 * ```kotlin
 * val wifiAPI = WiFiPublicAPI(context, capabilities)
 *
 * if (wifiAPI.isEnabled()) {
 *     val network = wifiAPI.getConnectedNetwork()
 *     val signal = wifiAPI.getSignalStrength()
 *     wifiAPI.scanNetworks()
 * }
 * ```
 *
 * @param context Android application context
 * @param capabilities Device capabilities from DeviceDetector
 */
class WiFiPublicAPI(
    private val context: Context,
    private val capabilities: DeviceDetector.DeviceCapabilities
) {

    companion object {
        private const val TAG = "WiFiPublicAPI"
    }

    // Internal WiFi manager
    private val wifiManager: WiFiManager by lazy {
        WiFiManager(context, capabilities)
    }

    // Coroutine scope for API operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ========== PUBLIC DATA CLASSES ==========

    /**
     * Connected network information
     */
    data class NetworkInfo(
        val ssid: String,
        val bssid: String,
        val signalStrength: Int,      // dBm (-100 to 0, higher is better)
        val signalLevel: SignalLevel,  // Qualitative signal strength
        val linkSpeed: Int,            // Mbps
        val frequency: Int,            // MHz (2400-2500 for 2.4GHz, 5000-6000 for 5GHz)
        val standard: WiFiStandard,
        val isSecure: Boolean,
        val ipAddress: String?
    )

    /**
     * Available network from scan
     */
    data class AvailableNetwork(
        val ssid: String,
        val bssid: String,
        val signalStrength: Int,
        val signalLevel: SignalLevel,
        val frequency: Int,
        val standard: WiFiStandard,
        val securityType: SecurityType,
        val isHidden: Boolean
    )

    /**
     * WiFi signal strength levels
     */
    enum class SignalLevel {
        EXCELLENT,  // > -50 dBm
        GOOD,       // -50 to -60 dBm
        FAIR,       // -60 to -70 dBm
        WEAK,       // -70 to -80 dBm
        POOR        // < -80 dBm
    }

    /**
     * WiFi standards
     */
    enum class WiFiStandard {
        LEGACY,     // 802.11a/b/g
        WIFI_4,     // 802.11n
        WIFI_5,     // 802.11ac
        WIFI_6,     // 802.11ax
        WIFI_6E,    // 802.11ax with 6GHz
        WIFI_7,     // 802.11be
        UNKNOWN
    }

    /**
     * Network security types
     */
    enum class SecurityType {
        OPEN,
        WEP,
        WPA,
        WPA2_PSK,
        WPA2_ENTERPRISE,
        WPA3_PSK,
        WPA3_ENTERPRISE,
        UNKNOWN
    }

    /**
     * WiFi status information
     */
    data class WiFiStatus(
        val isEnabled: Boolean,
        val isConnected: Boolean,
        val supports5GHz: Boolean,
        val supports6GHz: Boolean,
        val supportsWiFi6: Boolean,
        val supportsWiFi6E: Boolean
    )

    // ========== PUBLIC API METHODS ==========

    /**
     * Checks if WiFi is enabled on the device
     *
     * @return true if WiFi is enabled, false otherwise
     */
    fun isEnabled(): Boolean {
        return try {
            wifiManager.isWifiEnabled()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking WiFi enabled state", e)
            false
        }
    }

    /**
     * Checks if device is currently connected to a WiFi network
     *
     * @return true if connected to WiFi, false otherwise
     */
    fun isConnected(): Boolean {
        return try {
            wifiManager.wifiState.value.isConnected
        } catch (e: Exception) {
            Log.e(TAG, "Error checking WiFi connection state", e)
            false
        }
    }

    /**
     * Gets information about the currently connected WiFi network
     *
     * Requires ACCESS_WIFI_STATE permission.
     * On Android 10+, also requires ACCESS_FINE_LOCATION permission.
     *
     * @return Network information, or null if not connected or permission denied
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION
        ]
    )
    fun getConnectedNetwork(): NetworkInfo? {
        return try {
            val currentNetwork = wifiManager.wifiState.value.currentNetwork ?: return null

            NetworkInfo(
                ssid = currentNetwork.ssid.removeSurrounding("\""),
                bssid = currentNetwork.bssid,
                signalStrength = currentNetwork.level,
                signalLevel = getSignalLevel(currentNetwork.level),
                linkSpeed = currentNetwork.maxLinkSpeed,
                frequency = currentNetwork.frequency,
                standard = mapWiFiStandard(currentNetwork.standard),
                isSecure = currentNetwork.securityType != com.augmentalis.devicemanager.network.WiFiManager.SecurityType.OPEN,
                ipAddress = getIPAddress()
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied for getting connected network", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connected network", e)
            null
        }
    }

    /**
     * Gets the current WiFi signal strength in dBm
     *
     * Requires ACCESS_WIFI_STATE permission.
     * On Android 10+, also requires ACCESS_FINE_LOCATION permission.
     *
     * @return Signal strength in dBm (-100 to 0, higher is better), or null if not connected
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION
        ]
    )
    fun getSignalStrength(): Int? {
        return try {
            val currentNetwork = wifiManager.wifiState.value.currentNetwork
            currentNetwork?.level
        } catch (e: Exception) {
            Log.e(TAG, "Error getting signal strength", e)
            null
        }
    }

    /**
     * Gets the qualitative signal level (EXCELLENT, GOOD, FAIR, WEAK, POOR)
     *
     * Requires ACCESS_WIFI_STATE permission.
     * On Android 10+, also requires ACCESS_FINE_LOCATION permission.
     *
     * @return Signal level, or null if not connected
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION
        ]
    )
    fun getSignalLevel(): SignalLevel? {
        return getSignalStrength()?.let { getSignalLevel(it) }
    }

    /**
     * Starts scanning for available WiFi networks
     *
     * Results are available via scanResultsFlow.
     *
     * Requires:
     * - ACCESS_WIFI_STATE permission
     * - ACCESS_FINE_LOCATION permission
     *
     * Note: On Android 9+, scans are throttled to a few per minute.
     */
    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION
        ]
    )
    fun scanNetworks() {
        try {
            wifiManager.startScan()
            Log.i(TAG, "WiFi scan started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for WiFi scanning", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting WiFi scan", e)
        }
    }

    /**
     * Gets list of available networks from last scan
     *
     * Call scanNetworks() first to trigger a scan.
     *
     * Requires:
     * - ACCESS_WIFI_STATE permission
     * - ACCESS_FINE_LOCATION permission
     *
     * @return List of available networks, empty if no results or permission denied
     */
    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION
        ]
    )
    fun getAvailableNetworks(): List<AvailableNetwork> {
        return try {
            wifiManager.scanResults.value.map { network ->
                AvailableNetwork(
                    ssid = network.ssid,
                    bssid = network.bssid,
                    signalStrength = network.level,
                    signalLevel = getSignalLevel(network.level),
                    frequency = network.frequency,
                    standard = mapWiFiStandard(network.standard),
                    securityType = mapSecurityType(network.securityType),
                    isHidden = network.isHidden
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied for getting available networks", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available networks", e)
            emptyList()
        }
    }

    /**
     * Gets current WiFi status
     *
     * @return WiFi status information
     */
    fun getStatus(): WiFiStatus {
        return try {
            val state = wifiManager.wifiState.value
            WiFiStatus(
                isEnabled = state.isEnabled,
                isConnected = state.isConnected,
                supports5GHz = capabilities.wifi?.is5GHzSupported ?: false,
                supports6GHz = capabilities.wifi?.is6GHzSupported ?: false,
                supportsWiFi6 = capabilities.wifi?.isWiFi6Supported ?: false,
                supportsWiFi6E = capabilities.wifi?.isWiFi6ESupported ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting WiFi status", e)
            WiFiStatus(
                isEnabled = false,
                isConnected = false,
                supports5GHz = false,
                supports6GHz = false,
                supportsWiFi6 = false,
                supportsWiFi6E = false
            )
        }
    }

    /**
     * Checks if 5 GHz WiFi is supported
     *
     * @return true if 5 GHz band is supported
     */
    fun supports5GHz(): Boolean {
        return capabilities.wifi?.is5GHzSupported ?: false
    }

    /**
     * Checks if 6 GHz WiFi (WiFi 6E) is supported
     *
     * @return true if 6 GHz band is supported
     */
    fun supports6GHz(): Boolean {
        return capabilities.wifi?.is6GHzSupported ?: false
    }

    /**
     * Checks if WiFi 6 (802.11ax) is supported
     *
     * @return true if WiFi 6 is supported
     */
    fun supportsWiFi6(): Boolean {
        return capabilities.wifi?.isWiFi6Supported ?: false
    }

    /**
     * Enables WiFi on the device
     *
     * Note: On Android 10+ (API 29), this requires user interaction.
     * Apps cannot programmatically enable WiFi.
     *
     * Requires CHANGE_WIFI_STATE permission.
     *
     * @return true if WiFi was enabled (or on Android 10+), false otherwise
     */
    @RequiresPermission(Manifest.permission.CHANGE_WIFI_STATE)
    fun enableWiFi(): Boolean {
        return try {
            wifiManager.setWifiEnabled(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling WiFi", e)
            false
        }
    }

    /**
     * Disables WiFi on the device
     *
     * Note: On Android 10+ (API 29), this requires user interaction.
     * Apps cannot programmatically disable WiFi.
     *
     * Requires CHANGE_WIFI_STATE permission.
     *
     * @return true if WiFi was disabled (or on Android 10+), false otherwise
     */
    @RequiresPermission(Manifest.permission.CHANGE_WIFI_STATE)
    fun disableWiFi(): Boolean {
        return try {
            wifiManager.setWifiEnabled(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling WiFi", e)
            false
        }
    }

    // ========== REACTIVE STREAMS ==========

    /**
     * Flow of WiFi state changes
     *
     * Emits updates when WiFi state changes (enabled/disabled, connected/disconnected)
     *
     * Usage:
     * ```kotlin
     * wifiAPI.wifiStateFlow.collect { status ->
     *     println("WiFi enabled: ${status.isEnabled}")
     * }
     * ```
     */
    val wifiStateFlow: Flow<WiFiStatus> = wifiManager.wifiState.map { state ->
        WiFiStatus(
            isEnabled = state.isEnabled,
            isConnected = state.isConnected,
            supports5GHz = capabilities.wifi?.is5GHzSupported ?: false,
            supports6GHz = capabilities.wifi?.is6GHzSupported ?: false,
            supportsWiFi6 = capabilities.wifi?.isWiFi6Supported ?: false,
            supportsWiFi6E = capabilities.wifi?.isWiFi6ESupported ?: false
        )
    }

    /**
     * Flow of available WiFi networks from scans
     *
     * Emits updated network list after each scan completes.
     *
     * Usage:
     * ```kotlin
     * wifiAPI.scanResultsFlow.collect { networks ->
     *     println("Found ${networks.size} networks")
     * }
     * ```
     */
    val scanResultsFlow: Flow<List<AvailableNetwork>> = wifiManager.scanResults.map { results ->
        results.map { network ->
            AvailableNetwork(
                ssid = network.ssid,
                bssid = network.bssid,
                signalStrength = network.level,
                signalLevel = getSignalLevel(network.level),
                frequency = network.frequency,
                standard = mapWiFiStandard(network.standard),
                securityType = mapSecurityType(network.securityType),
                isHidden = network.isHidden
            )
        }
    }

    // ========== HELPER METHODS ==========

    /**
     * Converts signal strength (dBm) to qualitative level
     */
    private fun getSignalLevel(rssi: Int): SignalLevel {
        return when {
            rssi >= -50 -> SignalLevel.EXCELLENT
            rssi >= -60 -> SignalLevel.GOOD
            rssi >= -70 -> SignalLevel.FAIR
            rssi >= -80 -> SignalLevel.WEAK
            else -> SignalLevel.POOR
        }
    }

    /**
     * Maps internal WiFi standard to public API standard
     */
    private fun mapWiFiStandard(standard: com.augmentalis.devicemanager.network.WiFiManager.WiFiStandard): WiFiStandard {
        return when (standard) {
            com.augmentalis.devicemanager.network.WiFiManager.WiFiStandard.LEGACY -> WiFiStandard.LEGACY
            com.augmentalis.devicemanager.network.WiFiManager.WiFiStandard.WIFI_4 -> WiFiStandard.WIFI_4
            com.augmentalis.devicemanager.network.WiFiManager.WiFiStandard.WIFI_5 -> WiFiStandard.WIFI_5
            com.augmentalis.devicemanager.network.WiFiManager.WiFiStandard.WIFI_6 -> WiFiStandard.WIFI_6
            com.augmentalis.devicemanager.network.WiFiManager.WiFiStandard.WIFI_6E -> WiFiStandard.WIFI_6E
            com.augmentalis.devicemanager.network.WiFiManager.WiFiStandard.WIFI_7 -> WiFiStandard.WIFI_7
            com.augmentalis.devicemanager.network.WiFiManager.WiFiStandard.UNKNOWN -> WiFiStandard.UNKNOWN
        }
    }

    /**
     * Maps internal security type to public API security type
     */
    private fun mapSecurityType(securityType: com.augmentalis.devicemanager.network.WiFiManager.SecurityType): SecurityType {
        return when (securityType) {
            com.augmentalis.devicemanager.network.WiFiManager.SecurityType.OPEN -> SecurityType.OPEN
            com.augmentalis.devicemanager.network.WiFiManager.SecurityType.WEP -> SecurityType.WEP
            com.augmentalis.devicemanager.network.WiFiManager.SecurityType.WPA -> SecurityType.WPA
            com.augmentalis.devicemanager.network.WiFiManager.SecurityType.WPA2_PSK -> SecurityType.WPA2_PSK
            com.augmentalis.devicemanager.network.WiFiManager.SecurityType.WPA2_ENTERPRISE -> SecurityType.WPA2_ENTERPRISE
            com.augmentalis.devicemanager.network.WiFiManager.SecurityType.WPA3_PSK -> SecurityType.WPA3_PSK
            com.augmentalis.devicemanager.network.WiFiManager.SecurityType.WPA3_ENTERPRISE -> SecurityType.WPA3_ENTERPRISE
            else -> SecurityType.UNKNOWN
        }
    }

    /**
     * Gets device IP address
     */
    private fun getIPAddress(): String? {
        return try {
            // Simple implementation, could be expanded
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP address", e)
            null
        }
    }

    // ========== CLEANUP ==========

    /**
     * Cleans up resources
     *
     * Call this when the API instance is no longer needed.
     */
    fun dispose() {
        try {
            wifiManager.cleanup()
            scope.cancel()
            Log.d(TAG, "WiFi Public API disposed")
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing WiFi Public API", e)
        }
    }

    // ========== UTILITY METHODS ==========

    /**
     * Returns a human-readable summary of WiFi status
     */
    fun getStatusSummary(): String = buildString {
        val status = getStatus()
        appendLine("WiFi Status:")
        appendLine("  Enabled: ${status.isEnabled}")
        appendLine("  Connected: ${status.isConnected}")
        appendLine("  5 GHz Support: ${status.supports5GHz}")
        appendLine("  6 GHz Support: ${status.supports6GHz}")
        appendLine("  WiFi 6 Support: ${status.supportsWiFi6}")
        appendLine("  WiFi 6E Support: ${status.supportsWiFi6E}")

        if (status.isConnected) {
            try {
                getConnectedNetwork()?.let { network ->
                    appendLine("\nConnected Network:")
                    appendLine("  SSID: ${network.ssid}")
                    appendLine("  Signal: ${network.signalLevel} (${network.signalStrength} dBm)")
                    appendLine("  Speed: ${network.linkSpeed} Mbps")
                    appendLine("  Standard: ${network.standard}")
                    appendLine("  Frequency: ${network.frequency} MHz")
                    appendLine("  Secure: ${network.isSecure}")
                }
            } catch (e: SecurityException) {
                appendLine("\n(Permission required for network details)")
            }
        }

        try {
            val available = getAvailableNetworks()
            if (available.isNotEmpty()) {
                appendLine("\nAvailable Networks: ${available.size}")
                available.take(5).forEach { network ->
                    appendLine("  - ${network.ssid}")
                    appendLine("    Signal: ${network.signalLevel} (${network.signalStrength} dBm)")
                }
                if (available.size > 5) {
                    appendLine("  ... and ${available.size - 5} more")
                }
            }
        } catch (e: SecurityException) {
            appendLine("\n(Permission required for network scanning)")
        }
    }
}
