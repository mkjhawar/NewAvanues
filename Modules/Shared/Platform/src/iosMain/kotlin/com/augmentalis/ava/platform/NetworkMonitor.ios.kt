package com.augmentalis.ava.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Network.*
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
        updateNetworkState()
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
                path?.let { updateNetworkState(it) }
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

    private fun updateNetworkState(path: nw_path_t? = null) {
        val currentPath = path ?: pathMonitor?.let { nw_path_monitor_copy_current_path(it) }

        val state = if (currentPath != null) {
            val status = when (nw_path_get_status(currentPath)) {
                nw_path_status_satisfied, nw_path_status_satisfiable -> NetworkStatus.CONNECTED
                nw_path_status_unsatisfied -> NetworkStatus.DISCONNECTED
                else -> NetworkStatus.UNKNOWN
            }

            val type = when {
                nw_path_uses_interface_type(currentPath, nw_interface_type_wifi) -> NetworkType.WIFI
                nw_path_uses_interface_type(currentPath, nw_interface_type_cellular) -> NetworkType.CELLULAR
                nw_path_uses_interface_type(currentPath, nw_interface_type_wired) -> NetworkType.ETHERNET
                else -> NetworkType.UNKNOWN
            }

            val isMetered = nw_path_is_expensive(currentPath)

            NetworkState(
                status = status,
                type = type,
                isMetered = isMetered
            )
        } else {
            NetworkState(
                status = NetworkStatus.UNKNOWN,
                type = NetworkType.NONE,
                isMetered = false
            )
        }

        _networkState.value = state
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
