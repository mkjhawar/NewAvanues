package com.augmentalis.ava.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Network.nw_interface_type_cellular
import platform.Network.nw_interface_type_wifi
import platform.Network.nw_interface_type_wired
import platform.Network.nw_path_get_status
import platform.Network.nw_path_is_expensive
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_monitor_t
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_status_satisfiable
import platform.Network.nw_path_status_unsatisfied
import platform.Network.nw_path_t
import platform.Network.nw_path_uses_interface_type
import platform.darwin.dispatch_get_main_queue

/**
 * iOS implementation of NetworkMonitor using NWPathMonitor.
 */
@OptIn(ExperimentalForeignApi::class)
actual class NetworkMonitor actual constructor() {
    private var pathMonitor: nw_path_monitor_t? = null
    private var isMonitoring = false

    private val _networkState = MutableStateFlow(NetworkState())
    actual val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    init {
        pathMonitor = nw_path_monitor_create()
        // Start monitoring immediately to get initial state
        startMonitoring()
    }

    actual fun isConnected(): Boolean {
        return _networkState.value.status == NetworkStatus.CONNECTED
    }

    actual fun getNetworkType(): NetworkType {
        return _networkState.value.type
    }

    actual fun isMetered(): Boolean {
        return _networkState.value.isMetered
    }

    actual fun startMonitoring() {
        if (isMonitoring) return

        pathMonitor?.let { monitor ->
            nw_path_monitor_set_update_handler(monitor) { path ->
                if (path != null) {
                    updateNetworkState(path)
                }
            }
            nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
            nw_path_monitor_start(monitor)
            isMonitoring = true
        }
    }

    actual fun stopMonitoring() {
        if (!isMonitoring) return

        pathMonitor?.let { monitor ->
            nw_path_monitor_cancel(monitor)
            isMonitoring = false
        }
    }

    private fun updateNetworkState(path: nw_path_t) {
        val pathStatus = nw_path_get_status(path)

        val status = when (pathStatus) {
            nw_path_status_satisfied, nw_path_status_satisfiable -> NetworkStatus.CONNECTED
            nw_path_status_unsatisfied -> NetworkStatus.DISCONNECTED
            else -> NetworkStatus.UNKNOWN
        }

        val type = when {
            nw_path_uses_interface_type(path, nw_interface_type_wifi) -> NetworkType.WIFI
            nw_path_uses_interface_type(path, nw_interface_type_cellular) -> NetworkType.CELLULAR
            nw_path_uses_interface_type(path, nw_interface_type_wired) -> NetworkType.ETHERNET
            else -> NetworkType.UNKNOWN
        }

        val isMetered = nw_path_is_expensive(path)

        _networkState.value = NetworkState(
            status = status,
            type = type,
            isMetered = isMetered
        )
    }
}

/**
 * iOS factory for NetworkMonitor.
 */
actual object NetworkMonitorFactory {
    actual fun create(): NetworkMonitor {
        return NetworkMonitor()
    }
}
