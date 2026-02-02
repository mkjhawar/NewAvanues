/**
 * PlatformClient.kt - Desktop (JVM) implementation of UniversalClient
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * JVM-specific implementation using gRPC and Unix Domain Sockets.
 */
package com.augmentalis.rpc.client

import com.augmentalis.rpc.ConnectionState
import com.augmentalis.rpc.ServiceConnectionListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * JVM/Desktop implementation of UniversalClient.
 * Supports gRPC and Unix Domain Sockets.
 */
actual class PlatformClient actual constructor(
    override val config: ClientConfig
) : UniversalClient {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val listeners = mutableListOf<ServiceConnectionListener>()

    override suspend fun connect(): Boolean {
        _connectionState.value = ConnectionState.CONNECTING
        return try {
            when (config.protocol) {
                ClientConfig.Protocol.GRPC -> connectGrpc()
                ClientConfig.Protocol.UDS -> connectUDS()
                else -> connectGrpc()
            }
            _connectionState.value = ConnectionState.CONNECTED
            notifyListeners(ConnectionState.CONNECTED)
            true
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.FAILED
            notifyListeners(ConnectionState.FAILED)
            false
        }
    }

    private fun connectGrpc() {
        // TODO: Implement gRPC connection using grpc-netty
    }

    private fun connectUDS() {
        // TODO: Implement Unix Domain Socket connection
    }

    override suspend fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
        notifyListeners(ConnectionState.DISCONNECTED)
    }

    override suspend fun send(message: String): String? {
        if (!isConnected) return null
        // TODO: Implement message sending
        return null
    }

    override suspend fun request(request: ByteArray): ByteArray {
        if (!isConnected) throw IllegalStateException("Client not connected")
        // TODO: Implement request/response
        return ByteArray(0)
    }

    override fun receiveStream(): Flow<String> {
        // TODO: Implement streaming
        return emptyFlow()
    }

    override fun addConnectionListener(listener: ServiceConnectionListener) {
        listeners.add(listener)
    }

    override fun removeConnectionListener(listener: ServiceConnectionListener) {
        listeners.remove(listener)
    }

    override suspend fun close() {
        disconnect()
        listeners.clear()
    }

    private fun notifyListeners(state: ConnectionState) {
        listeners.forEach { listener ->
            when (state) {
                ConnectionState.CONNECTED -> listener.onConnected()
                ConnectionState.DISCONNECTED -> listener.onDisconnected()
                ConnectionState.FAILED -> listener.onConnectionFailed(Exception("Connection failed"))
                else -> {}
            }
        }
    }
}
