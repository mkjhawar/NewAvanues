package com.augmentalis.rpc.ipc

/**
 * Service Connector
 *
 * Platform-agnostic interface for connecting to AIDL services.
 * This is the unified expect class that merges functionality from
 * IPCConnector and UniversalIPC modules.
 *
 * ## Usage
 * ```kotlin
 * val connector = ServiceConnector()
 * connector.setContext(context)  // Android only
 *
 * val result = connector.connect(endpoint)
 * when (result) {
 *     is ConnectionResult.Success -> {
 *         val connection = result.connection
 *         connector.invoke(connection.id, MethodInvocation("methodName", params))
 *     }
 *     is ConnectionResult.Error -> {
 *         println("Failed: ${result.error}")
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 * @author Avanues Platform Team
 */
expect class ServiceConnector() {

    /**
     * Connect to a service endpoint
     *
     * Uses the AvuIPCParser for data exchange when establishing connections
     * and transmitting method invocation data.
     *
     * @param endpoint Service endpoint containing package name, interface, and methods
     * @return ConnectionResult indicating success or failure
     */
    suspend fun connect(endpoint: ServiceEndpoint): ConnectionResult

    /**
     * Disconnect from a service
     *
     * @param connectionId Connection ID to disconnect
     */
    suspend fun disconnect(connectionId: String)

    /**
     * Invoke a method on connected service
     *
     * Serializes parameters using AvuIPCParser format for transmission
     * and deserializes the response.
     *
     * @param connectionId Connection ID
     * @param invocation Method to invoke with parameters
     * @return MethodResult with return value or error
     */
    suspend fun invoke(connectionId: String, invocation: MethodInvocation): MethodResult

    /**
     * Check if connection is active
     *
     * @param connectionId Connection ID
     * @return true if connected
     */
    fun isConnected(connectionId: String): Boolean

    /**
     * Get connection by ID
     *
     * @param connectionId Connection ID
     * @return Connection or null if not found
     */
    fun getConnection(connectionId: String): Connection?

    /**
     * Get all active connections
     *
     * @return List of active connections
     */
    fun getAllConnections(): List<Connection>
}

/**
 * Service endpoint definition for AIDL services.
 *
 * Contains all information needed to bind to and communicate with a service.
 *
 * @property id Unique identifier for the service (typically package.service format)
 * @property aidlInterface Fully qualified AIDL interface name
 * @property methods List of available method names on the service
 * @property permissions Required permissions to bind to this service
 */
data class ServiceEndpoint(
    val id: String,
    val aidlInterface: String,
    val methods: List<String> = emptyList(),
    val permissions: List<String> = emptyList()
)
