/**
 * UniversalClient.kt - Cross-platform RPC client abstraction
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * Provides expect/actual pattern for platform-specific client implementations.
 * Supports connection management, service discovery, and common RPC operations.
 */
package com.augmentalis.rpc.client

import com.augmentalis.rpc.ConnectionState
import com.augmentalis.rpc.IRpcService
import com.augmentalis.rpc.ServiceConnectionListener
import com.augmentalis.rpc.ServiceEndpoint
import com.augmentalis.rpc.ServiceRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Cross-platform client interface for UniversalRPC.
 * Platform-specific implementations handle the actual transport layer.
 */
interface UniversalClient {
    /** Current connection state */
    val connectionState: StateFlow<ConnectionState>

    /** Client configuration */
    val config: ClientConfig

    /** Whether the client is currently connected */
    val isConnected: Boolean
        get() = connectionState.value == ConnectionState.CONNECTED

    /**
     * Connect to the remote service.
     * @return true if connection was successful
     */
    suspend fun connect(): Boolean

    /**
     * Disconnect from the remote service.
     */
    suspend fun disconnect()

    /**
     * Send a raw message to the connected service.
     * @param message The message to send (IPC protocol format)
     * @return Response message or null if no response
     */
    suspend fun send(message: String): String?

    /**
     * Send a request and wait for response.
     * @param request Request data
     * @return Response data
     */
    suspend fun request(request: ByteArray): ByteArray

    /**
     * Stream messages from the server.
     * @return Flow of incoming messages
     */
    fun receiveStream(): Flow<String>

    /**
     * Add a connection state listener.
     */
    fun addConnectionListener(listener: ServiceConnectionListener)

    /**
     * Remove a connection state listener.
     */
    fun removeConnectionListener(listener: ServiceConnectionListener)

    /**
     * Close the client and release all resources.
     */
    suspend fun close()
}

/**
 * Expect declaration for platform-specific client implementation.
 * Actual implementations provided in:
 * - androidMain: AndroidUniversalClient (gRPC, Binder, UDS)
 * - iosMain: IOSUniversalClient (gRPC, XPC, UDS)
 * - jvmMain: JvmUniversalClient (gRPC, UDS)
 * - jsMain: JsUniversalClient (WebSocket, HTTP/2)
 */
expect class PlatformClient(config: ClientConfig) : UniversalClient

/**
 * Client that connects to services via ServiceRegistry.
 * Provides automatic service discovery and connection management.
 */
class RegistryAwareClient(
    private val registry: ServiceRegistry,
    private val serviceName: String,
    private val configProvider: (ServiceEndpoint) -> ClientConfig = { endpoint ->
        ClientConfig(
            host = endpoint.host,
            port = endpoint.port,
            protocol = when (endpoint.protocol) {
                "grpc" -> ClientConfig.Protocol.GRPC
                "uds" -> ClientConfig.Protocol.UDS
                "websocket" -> ClientConfig.Protocol.WEBSOCKET
                else -> ClientConfig.Protocol.GRPC
            }
        )
    }
) : UniversalClient {

    private var platformClient: PlatformClient? = null
    private var _config: ClientConfig = ClientConfig.DEFAULT

    override val config: ClientConfig
        get() = _config

    override val connectionState: StateFlow<ConnectionState>
        get() = platformClient?.connectionState
            ?: throw IllegalStateException("Client not initialized. Call connect() first.")

    override suspend fun connect(): Boolean {
        val endpoint = registry.getEndpoint(serviceName)
            ?: throw ServiceNotFoundException(serviceName)

        _config = configProvider(endpoint)

        // Check for local service first
        val localService = registry.getLocalService(serviceName)
        if (localService != null && localService.isReady()) {
            // For local services, we can use in-process communication
            // Platform client still needed for API consistency
        }

        platformClient = PlatformClient(_config)
        return platformClient?.connect() ?: false
    }

    override suspend fun disconnect() {
        platformClient?.disconnect()
    }

    override suspend fun send(message: String): String? {
        return platformClient?.send(message)
    }

    override suspend fun request(request: ByteArray): ByteArray {
        return platformClient?.request(request)
            ?: throw IllegalStateException("Client not connected")
    }

    override fun receiveStream(): Flow<String> {
        return platformClient?.receiveStream()
            ?: throw IllegalStateException("Client not connected")
    }

    override fun addConnectionListener(listener: ServiceConnectionListener) {
        platformClient?.addConnectionListener(listener)
    }

    override fun removeConnectionListener(listener: ServiceConnectionListener) {
        platformClient?.removeConnectionListener(listener)
    }

    override suspend fun close() {
        platformClient?.close()
        platformClient = null
    }

    /**
     * Check if the target service is available in the registry.
     */
    fun isServiceAvailable(): Boolean = registry.isAvailable(serviceName)

    /**
     * Get the endpoint for the target service.
     */
    fun getServiceEndpoint(): ServiceEndpoint? = registry.getEndpoint(serviceName)
}

/**
 * Exception thrown when a service is not found in the registry.
 */
class ServiceNotFoundException(
    serviceName: String
) : Exception("Service not found: $serviceName")

/**
 * Exception thrown when connection fails.
 */
class ConnectionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when a request times out.
 */
class RequestTimeoutException(
    message: String = "Request timed out"
) : Exception(message)

/**
 * Client connection state with additional metadata.
 */
data class ClientConnectionInfo(
    val state: ConnectionState,
    val connectedAt: Long? = null,
    val lastActivityAt: Long? = null,
    val reconnectAttempts: Int = 0,
    val latencyMs: Long? = null,
    val endpoint: ServiceEndpoint? = null
)

/**
 * Base class for typed service clients.
 * Extend this to create service-specific clients with typed methods.
 */
abstract class TypedServiceClient<T : IRpcService>(
    protected val client: UniversalClient
) {
    abstract val serviceName: String

    val connectionState: StateFlow<ConnectionState>
        get() = client.connectionState

    val isConnected: Boolean
        get() = client.isConnected

    suspend fun connect(): Boolean = client.connect()
    suspend fun disconnect() = client.disconnect()
    suspend fun close() = client.close()
}
