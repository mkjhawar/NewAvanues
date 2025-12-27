package com.augmentalis.Avanues.web.universal.presentation.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation of network status monitoring using ConnectivityManager
 */
@Composable
actual fun rememberNetworkStatusMonitor(): NetworkStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(NetworkStatus.CONNECTED) }

    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Initial status check
        status = getCurrentNetworkStatus(connectivityManager)

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                status = NetworkStatus.CONNECTED
            }

            override fun onLost(network: Network) {
                status = NetworkStatus.DISCONNECTED
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                status = when {
                    !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> {
                        NetworkStatus.DISCONNECTED
                    }
                    !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> {
                        NetworkStatus.RECONNECTING
                    }
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> {
                        NetworkStatus.CONNECTED
                    }
                    else -> {
                        // Metered connection (cellular data) - could be considered SLOW
                        NetworkStatus.CONNECTED
                    }
                }
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        onDispose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    return status
}

/**
 * Get current network status synchronously
 */
private fun getCurrentNetworkStatus(connectivityManager: ConnectivityManager): NetworkStatus {
    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(network)

    return when {
        capabilities == null -> NetworkStatus.DISCONNECTED
        !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> NetworkStatus.DISCONNECTED
        !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> NetworkStatus.RECONNECTING
        else -> NetworkStatus.CONNECTED
    }
}
