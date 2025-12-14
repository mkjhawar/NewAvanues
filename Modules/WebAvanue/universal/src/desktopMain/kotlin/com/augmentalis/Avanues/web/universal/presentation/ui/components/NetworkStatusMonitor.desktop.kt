package com.augmentalis.webavanue.ui.screen.components

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Desktop implementation of network status monitoring using socket checks
 */
@Composable
actual fun rememberNetworkStatusMonitor(): NetworkStatus {
    var status by remember { mutableStateOf(NetworkStatus.CONNECTED) }

    LaunchedEffect(Unit) {
        while (isActive) {
            status = checkNetworkConnection()
            delay(5000) // Check every 5 seconds
        }
    }

    return status
}

/**
 * Check network connection by attempting to connect to common DNS servers
 */
private suspend fun checkNetworkConnection(): NetworkStatus {
    return try {
        // Try to connect to Google's DNS server
        val socket = Socket()
        val socketAddress = InetSocketAddress("8.8.8.8", 53)
        socket.connect(socketAddress, 3000) // 3 second timeout
        socket.close()
        NetworkStatus.CONNECTED
    } catch (e: Exception) {
        try {
            // Fallback: Try Cloudflare's DNS
            val socket = Socket()
            val socketAddress = InetSocketAddress("1.1.1.1", 53)
            socket.connect(socketAddress, 3000)
            socket.close()
            NetworkStatus.CONNECTED
        } catch (e2: Exception) {
            NetworkStatus.DISCONNECTED
        }
    }
}
