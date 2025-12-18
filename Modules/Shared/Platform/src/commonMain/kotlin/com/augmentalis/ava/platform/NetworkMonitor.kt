package com.augmentalis.ava.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Network connectivity state.
 */
enum class NetworkStatus {
    CONNECTED,
    DISCONNECTED,
    UNKNOWN
}

/**
 * Network connection type.
 */
enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    UNKNOWN,
    NONE
}

/**
 * Network state information.
 */
data class NetworkState(
    val status: NetworkStatus = NetworkStatus.UNKNOWN,
    val type: NetworkType = NetworkType.UNKNOWN,
    val isMetered: Boolean = false
)

/**
 * Cross-platform network connectivity monitor.
 *
 * Platform implementations:
 * - Android: ConnectivityManager with NetworkCallback
 * - iOS: NWPathMonitor
 * - Desktop: InetAddress reachability check
 */
expect class NetworkMonitor() {

    /**
     * Current network state as a StateFlow for reactive observation.
     */
    val networkState: StateFlow<NetworkState>

    /**
     * Check if device is currently connected to a network.
     */
    fun isConnected(): Boolean

    /**
     * Get the current network type.
     */
    fun getNetworkType(): NetworkType

    /**
     * Check if the current connection is metered (e.g., cellular data).
     */
    fun isMetered(): Boolean

    /**
     * Start monitoring network changes.
     */
    fun startMonitoring()

    /**
     * Stop monitoring network changes.
     */
    fun stopMonitoring()
}

/**
 * Factory for creating NetworkMonitor instances.
 */
expect object NetworkMonitorFactory {
    fun create(): NetworkMonitor
}
