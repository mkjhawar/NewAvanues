package com.augmentalis.ava.platform

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Desktop (JVM) implementation of NetworkMonitor.
 *
 * Uses InetAddress.isReachable() to check connectivity by pinging common DNS servers.
 * Polls periodically when monitoring is active.
 */
actual class NetworkMonitor actual constructor() {

    private val _networkState = MutableStateFlow(NetworkState())
    actual val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private var monitoringJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val PING_TIMEOUT_MS = 2000
        private const val POLL_INTERVAL_MS = 5000L
        private val TEST_HOSTS = listOf(
            "8.8.8.8",      // Google DNS
            "1.1.1.1",      // Cloudflare DNS
            "8.8.4.4"       // Google DNS secondary
        )
    }

    actual fun isConnected(): Boolean {
        return checkConnectivity()
    }

    actual fun getNetworkType(): NetworkType {
        if (!isConnected()) {
            return NetworkType.NONE
        }

        // Attempt to detect ethernet vs wifi by checking network interfaces
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isUp && !networkInterface.isLoopback) {
                    val name = networkInterface.name.lowercase()
                    return when {
                        name.contains("eth") || name.contains("en") -> NetworkType.ETHERNET
                        name.contains("wlan") || name.contains("wi") -> NetworkType.WIFI
                        else -> NetworkType.UNKNOWN
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to unknown if detection fails
        }

        return NetworkType.UNKNOWN
    }

    actual fun isMetered(): Boolean {
        // Desktop connections are typically not metered
        // Could be enhanced to detect mobile hotspot connections
        return false
    }

    actual fun startMonitoring() {
        if (monitoringJob != null) {
            return // Already monitoring
        }

        monitoringJob = scope.launch {
            while (isActive) {
                updateNetworkState()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    actual fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private fun checkConnectivity(): Boolean {
        return TEST_HOSTS.any { host ->
            try {
                val address = InetAddress.getByName(host)
                address.isReachable(PING_TIMEOUT_MS)
            } catch (e: Exception) {
                false
            }
        }
    }

    private suspend fun updateNetworkState() {
        withContext(Dispatchers.IO) {
            val connected = checkConnectivity()
            val status = if (connected) NetworkStatus.CONNECTED else NetworkStatus.DISCONNECTED
            val type = if (connected) getNetworkType() else NetworkType.NONE
            val metered = isMetered()

            _networkState.value = NetworkState(
                status = status,
                type = type,
                isMetered = metered
            )
        }
    }
}

/**
 * Factory for creating NetworkMonitor instances on Desktop.
 */
actual object NetworkMonitorFactory {
    actual fun create(): NetworkMonitor {
        return NetworkMonitor()
    }
}
