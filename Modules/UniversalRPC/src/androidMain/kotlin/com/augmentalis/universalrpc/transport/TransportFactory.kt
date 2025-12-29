/**
 * TransportFactory.kt - Factory for creating transport instances
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * Provides factory methods for creating appropriate transport instances
 * based on configuration and platform capabilities.
 */
package com.augmentalis.universalrpc.transport

import android.content.Context
import android.os.Build
import com.augmentalis.universalrpc.ServiceRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Transport factory for creating client and server transports
 *
 * Automatically selects the best transport type based on:
 * - Target service location (local vs remote)
 * - Platform capabilities
 * - Performance requirements
 */
object TransportFactory {

    /**
     * Default socket paths for known services
     */
    object SocketPaths {
        const val VOICEOS = "universalrpc.voiceos"
        const val VOICE_CURSOR = "universalrpc.voicecursor"
        const val VOICE_RECOGNITION = "universalrpc.voicerecognition"
        const val VUID_CREATOR = "universalrpc.vuidcreator"
        const val EXPLORATION = "universalrpc.exploration"
        const val AVA = "universalrpc.ava"
        const val COCKPIT = "universalrpc.cockpit"
        const val NLU = "universalrpc.nlu"
        const val WEBAVANUE = "universalrpc.webavanue"

        fun forService(serviceName: String): String = when (serviceName) {
            ServiceRegistry.SERVICE_VOICEOS -> VOICEOS
            ServiceRegistry.SERVICE_VOICE_CURSOR -> VOICE_CURSOR
            ServiceRegistry.SERVICE_VOICE_RECOGNITION -> VOICE_RECOGNITION
            ServiceRegistry.SERVICE_VUID_CREATOR -> VUID_CREATOR
            ServiceRegistry.SERVICE_EXPLORATION -> EXPLORATION
            ServiceRegistry.SERVICE_AVA -> AVA
            ServiceRegistry.SERVICE_COCKPIT -> COCKPIT
            ServiceRegistry.SERVICE_NLU -> NLU
            ServiceRegistry.SERVICE_WEBAVANUE -> WEBAVANUE
            else -> "universalrpc.${serviceName.substringAfterLast('.')}"
        }
    }

    /**
     * Create a client transport for connecting to a service
     *
     * @param address The transport address
     * @param config Transport configuration
     * @return A configured Transport instance
     */
    fun createClient(
        address: TransportAddress,
        config: TransportConfig = TransportConfig()
    ): Transport {
        return when (address) {
            is TransportAddress.UnixSocket -> UnixDomainSocketTransport(address, config)
            is TransportAddress.TcpSocket -> TcpSocketTransport(address, config)
            is TransportAddress.InMemory -> throw TransportException(
                "In-memory transport not supported on Android",
                isRecoverable = false
            )
        }
    }

    /**
     * Create a server transport for accepting connections
     *
     * @param address The transport address
     * @param config Transport configuration
     * @return A configured ServerTransport instance
     */
    fun createServer(
        address: TransportAddress,
        config: TransportConfig = TransportConfig()
    ): ServerTransport {
        return when (address) {
            is TransportAddress.UnixSocket -> UnixDomainSocketServerTransport(address, config)
            is TransportAddress.TcpSocket -> TcpSocketServerTransport(address, config)
            is TransportAddress.InMemory -> throw TransportException(
                "In-memory transport not supported on Android",
                isRecoverable = false
            )
        }
    }

    /**
     * Create the optimal transport for a given service
     *
     * Automatically selects Unix Domain Socket for local services
     * and TCP for remote services.
     *
     * @param serviceName The service name to connect to
     * @param isLocal Whether the service is on the same device
     * @param remoteHost Remote host address (if not local)
     * @param remotePort Remote port number (if not local)
     * @param config Transport configuration
     * @return A configured Transport instance
     */
    fun createForService(
        serviceName: String,
        isLocal: Boolean = true,
        remoteHost: String = "localhost",
        remotePort: Int = 0,
        config: TransportConfig = TransportConfig()
    ): Transport {
        return if (isLocal) {
            // Use Unix Domain Socket for local communication
            val socketPath = SocketPaths.forService(serviceName)
            createClient(
                TransportAddress.UnixSocket(socketPath, abstract = true),
                config
            )
        } else {
            // Use TCP for remote communication
            val port = if (remotePort > 0) remotePort else getDefaultPort(serviceName)
            createClient(
                TransportAddress.TcpSocket(remoteHost, port),
                config
            )
        }
    }

    /**
     * Create a server for hosting a service
     *
     * @param serviceName The service name to host
     * @param enableTcp Whether to also enable TCP connections
     * @param tcpPort TCP port to listen on (if enabled)
     * @param config Transport configuration
     * @return A pair of (UnixSocket server, optional TCP server)
     */
    fun createServiceHost(
        serviceName: String,
        enableTcp: Boolean = false,
        tcpPort: Int = 0,
        config: TransportConfig = TransportConfig()
    ): ServiceHost {
        val socketPath = SocketPaths.forService(serviceName)
        val udsServer = createServer(
            TransportAddress.UnixSocket(socketPath, abstract = true),
            config
        )

        val tcpServer = if (enableTcp) {
            val port = if (tcpPort > 0) tcpPort else getDefaultPort(serviceName)
            createServer(
                TransportAddress.TcpSocket("0.0.0.0", port),
                config
            )
        } else {
            null
        }

        return ServiceHost(udsServer, tcpServer, serviceName)
    }

    /**
     * Get the default port for a service
     */
    private fun getDefaultPort(serviceName: String): Int = when (serviceName) {
        ServiceRegistry.SERVICE_VOICEOS -> ServiceRegistry.DEFAULT_PORT_VOICEOS
        ServiceRegistry.SERVICE_AVA -> ServiceRegistry.DEFAULT_PORT_AVA
        ServiceRegistry.SERVICE_COCKPIT -> ServiceRegistry.DEFAULT_PORT_COCKPIT
        ServiceRegistry.SERVICE_NLU -> ServiceRegistry.DEFAULT_PORT_NLU
        ServiceRegistry.SERVICE_WEBAVANUE -> ServiceRegistry.DEFAULT_PORT_WEBAVANUE
        else -> 50060 // Default fallback port
    }

    /**
     * Check if Unix Domain Sockets are available on this device
     *
     * UDS is available on all Android versions, but abstract namespace
     * requires SELinux policy considerations on Android 9+.
     */
    fun isUnixSocketAvailable(): Boolean = true

    /**
     * Get recommended transport type for current device
     */
    fun getRecommendedTransportType(): TransportType {
        return if (isUnixSocketAvailable()) {
            TransportType.UNIX_DOMAIN_SOCKET
        } else {
            TransportType.TCP
        }
    }
}

/**
 * Represents a hosted service with optional multi-transport support
 */
class ServiceHost(
    val udsServer: ServerTransport,
    val tcpServer: ServerTransport?,
    val serviceName: String
) {
    private val acceptLoops = mutableListOf<Job>()
    private val scope = CoroutineScope(
        Dispatchers.IO + SupervisorJob()
    )

    /**
     * Handler for incoming connections
     */
    var connectionHandler: (suspend (Transport) -> Unit)? = null

    /**
     * Start the service host
     */
    suspend fun start() {
        udsServer.start()

        tcpServer?.start()

        // Start accept loops
        acceptLoops.add(scope.launch {
            acceptLoop(udsServer)
        })

        tcpServer?.let { tcp ->
            acceptLoops.add(scope.launch {
                acceptLoop(tcp)
            })
        }
    }

    private suspend fun acceptLoop(server: ServerTransport) {
        while (server.isListening) {
            try {
                val client = server.accept()
                scope.launch {
                    try {
                        connectionHandler?.invoke(client)
                    } catch (e: Exception) {
                        // Log error but continue accepting
                    } finally {
                        client.disconnect(graceful = true)
                    }
                }
            } catch (e: TransportException) {
                if (!server.isListening) break
                // Log error and continue
            }
        }
    }

    /**
     * Stop the service host
     */
    suspend fun stop(graceful: Boolean = true) {
        acceptLoops.forEach { it.cancel() }
        acceptLoops.clear()

        udsServer.stop(graceful)
        tcpServer?.stop(graceful)
    }

    /**
     * Close the service host and release all resources
     */
    suspend fun close() {
        stop(graceful = false)
        udsServer.close()
        tcpServer?.close()
        scope.cancel()
    }

    /**
     * Get all active connections across all transports
     */
    fun getAllConnections(): List<Transport> {
        val connections = mutableListOf<Transport>()
        connections.addAll(udsServer.getConnections())
        tcpServer?.let { connections.addAll(it.getConnections()) }
        return connections
    }
}

/**
 * Builder for creating transports with fluent API
 */
class TransportBuilder {
    private var address: TransportAddress? = null
    private var config: TransportConfig = TransportConfig()
    private var isServer: Boolean = false

    fun unixSocket(path: String, abstract: Boolean = true): TransportBuilder {
        address = TransportAddress.UnixSocket(path, abstract)
        return this
    }

    fun tcpSocket(host: String, port: Int): TransportBuilder {
        address = TransportAddress.TcpSocket(host, port)
        return this
    }

    fun forService(serviceName: String, isLocal: Boolean = true): TransportBuilder {
        address = if (isLocal) {
            TransportAddress.UnixSocket(TransportFactory.SocketPaths.forService(serviceName), abstract = true)
        } else {
            val port = when (serviceName) {
                ServiceRegistry.SERVICE_VOICEOS -> ServiceRegistry.DEFAULT_PORT_VOICEOS
                ServiceRegistry.SERVICE_AVA -> ServiceRegistry.DEFAULT_PORT_AVA
                else -> 50060
            }
            TransportAddress.TcpSocket("localhost", port)
        }
        return this
    }

    fun withConfig(config: TransportConfig): TransportBuilder {
        this.config = config
        return this
    }

    fun connectionTimeout(ms: Long): TransportBuilder {
        config = config.copy(connectionTimeoutMs = ms)
        return this
    }

    fun readTimeout(ms: Long): TransportBuilder {
        config = config.copy(readTimeoutMs = ms)
        return this
    }

    fun writeTimeout(ms: Long): TransportBuilder {
        config = config.copy(writeTimeoutMs = ms)
        return this
    }

    fun maxRetries(count: Int): TransportBuilder {
        config = config.copy(maxRetryAttempts = count)
        return this
    }

    fun autoReconnect(enabled: Boolean): TransportBuilder {
        config = config.copy(autoReconnect = enabled)
        return this
    }

    fun keepAliveInterval(ms: Long): TransportBuilder {
        config = config.copy(keepAliveIntervalMs = ms)
        return this
    }

    fun bufferSize(size: Int): TransportBuilder {
        config = config.copy(bufferSize = size)
        return this
    }

    fun asServer(): TransportBuilder {
        isServer = true
        return this
    }

    fun buildClient(): Transport {
        val addr = address ?: throw IllegalStateException("Address not set")
        return TransportFactory.createClient(addr, config)
    }

    fun buildServer(): ServerTransport {
        val addr = address ?: throw IllegalStateException("Address not set")
        return TransportFactory.createServer(addr, config)
    }
}

/**
 * DSL function for creating transports
 */
fun transport(block: TransportBuilder.() -> Unit): Transport {
    return TransportBuilder().apply(block).buildClient()
}

/**
 * DSL function for creating server transports
 */
fun serverTransport(block: TransportBuilder.() -> Unit): ServerTransport {
    return TransportBuilder().apply(block).asServer().buildServer()
}

/**
 * Extension functions for convenient transport creation
 */

/**
 * Create a Unix Domain Socket transport for a service
 */
fun Transport.Companion.forService(
    serviceName: String,
    config: TransportConfig = TransportConfig()
): Transport = TransportFactory.createForService(serviceName, isLocal = true, config = config)

/**
 * Create a TCP transport for a remote service
 */
fun Transport.Companion.forRemoteService(
    serviceName: String,
    host: String,
    port: Int = 0,
    config: TransportConfig = TransportConfig()
): Transport = TransportFactory.createForService(
    serviceName,
    isLocal = false,
    remoteHost = host,
    remotePort = port,
    config = config
)
