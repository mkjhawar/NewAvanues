/**
 * IRpcService.kt - Common RPC service interface
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * Base interface for all UniversalRPC services.
 */
package com.augmentalis.rpc

/**
 * Base interface for RPC services
 */
interface IRpcService {
    /**
     * Service name for registration/discovery
     */
    val serviceName: String

    /**
     * Service version
     */
    val version: String

    /**
     * Check if service is ready
     */
    suspend fun isReady(): Boolean

    /**
     * Shutdown the service
     */
    suspend fun shutdown()
}

/**
 * Service connection state
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED
}

/**
 * Service connection listener
 */
interface ServiceConnectionListener {
    fun onConnectionStateChanged(state: ConnectionState) {
        when (state) {
            ConnectionState.CONNECTED -> onConnected()
            ConnectionState.DISCONNECTED -> onDisconnected()
            ConnectionState.FAILED -> onConnectionFailed(null)
            else -> {}
        }
    }
    fun onConnected() {}
    fun onDisconnected() {}
    fun onConnectionFailed(error: Throwable?) {}
    fun onError(error: Throwable)
}
