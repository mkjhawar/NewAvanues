package com.augmentalis.httpavanue.platform

import okio.BufferedSink
import okio.BufferedSource

/**
 * Platform-abstracted socket interface using expect/actual pattern.
 * Backed by java.net.Socket on Android/JVM, BSD sockets on iOS.
 */
expect class Socket {
    companion object {
        suspend fun connect(
            host: String,
            port: Int,
            config: SocketConfig = SocketConfig(),
        ): Socket
    }

    fun source(): BufferedSource
    fun sink(): BufferedSink
    fun close()
    fun isConnected(): Boolean
    fun remoteAddress(): String
    fun setReadTimeout(timeoutMs: Long)
}

/**
 * Platform-abstracted socket server interface
 */
expect class SocketServer(config: SocketConfig = SocketConfig()) {
    fun bind(port: Int, backlog: Int = 50)
    suspend fun accept(): Socket
    fun close()
    fun isBound(): Boolean
    fun localPort(): Int
}

/**
 * Socket configuration
 */
data class SocketConfig(
    val readTimeout: Long = 30_000,
    val writeTimeout: Long = 30_000,
    val keepAlive: Boolean = true,
    val tcpNoDelay: Boolean = true,
    val receiveBufferSize: Int = 8192,
    val sendBufferSize: Int = 8192,
    val tls: TlsConfig = TlsConfig.disabled(),
)
