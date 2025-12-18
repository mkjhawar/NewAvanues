package com.augmentalis.ava.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android implementation of NetworkMonitor using ConnectivityManager.
 */
actual class NetworkMonitor actual constructor() {
    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val _networkState = MutableStateFlow(NetworkState())
    actual val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    internal fun init(context: Context) {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateNetworkState()
            }

            override fun onLost(network: Network) {
                _networkState.value = NetworkState(
                    status = NetworkStatus.DISCONNECTED,
                    type = NetworkType.NONE,
                    isMetered = false
                )
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                updateNetworkState()
            }
        }

        networkCallback?.let {
            connectivityManager.registerNetworkCallback(request, it)
        }
    }

    actual fun stopMonitoring() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
        }
        networkCallback = null
    }

    private fun updateNetworkState() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        val state = if (capabilities != null) {
            val type = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                else -> NetworkType.UNKNOWN
            }

            val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

            NetworkState(
                status = NetworkStatus.CONNECTED,
                type = type,
                isMetered = isMetered
            )
        } else {
            NetworkState(
                status = NetworkStatus.DISCONNECTED,
                type = NetworkType.NONE,
                isMetered = false
            )
        }

        _networkState.value = state
    }

    companion object {
        private var appContext: Context? = null

        fun initialize(context: Context) {
            appContext = context.applicationContext
        }

        internal fun getContext(): Context {
            return appContext ?: throw IllegalStateException(
                "NetworkMonitor not initialized. Call NetworkMonitor.initialize(context) in Application.onCreate()"
            )
        }
    }
}

/**
 * Android factory for NetworkMonitor.
 */
actual object NetworkMonitorFactory {
    actual fun create(): NetworkMonitor {
        return NetworkMonitor().apply {
            init(NetworkMonitor.getContext())
        }
    }
}
