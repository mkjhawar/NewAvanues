package com.augmentalis.webavanue.ui.screen.components

import androidx.compose.runtime.*
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.Network.*
import platform.darwin.dispatch_queue_create
import platform.darwin.DISPATCH_QUEUE_SERIAL

/**
 * iOS implementation of network status monitoring using NWPathMonitor
 */
@Composable
actual fun rememberNetworkStatusMonitor(): NetworkStatus {
    var status by remember { mutableStateOf(NetworkStatus.CONNECTED) }

    DisposableEffect(Unit) {
        val monitor = nw_path_monitor_create()
        val queue = dispatch_queue_create("NetworkMonitor", DISPATCH_QUEUE_SERIAL)

        nw_path_monitor_set_update_handler(monitor) { path ->
            status = when (nw_path_get_status(path)) {
                nw_path_status_satisfied, nw_path_status_satisfiable -> {
                    // Check if it's expensive (cellular/metered)
                    if (nw_path_is_expensive(path)) {
                        // Could mark as SLOW for metered connections
                        NetworkStatus.CONNECTED
                    } else {
                        NetworkStatus.CONNECTED
                    }
                }
                nw_path_status_unsatisfied -> NetworkStatus.DISCONNECTED
                else -> NetworkStatus.RECONNECTING
            }
        }

        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_start(monitor)

        onDispose {
            nw_path_monitor_cancel(monitor)
        }
    }

    return status
}
