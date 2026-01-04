package com.augmentalis.magicui.components.ipc

import com.augmentalis.magicui.components.argscanner.ServiceEndpoint

/**
 * Service Connector
 *
 * Platform-agnostic interface for connecting to AIDL services.
 *
 * ## Usage
 * ```kotlin
 * val connector = ServiceConnector(registry)
 * connector.setPackageManager(packageManager)  // Android only
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
 */
expect class ServiceConnector {

    /**
     * Connect to a service endpoint
     *
     * @param endpoint Service endpoint from ARG registry
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
     * @param connectionId Connection ID
     * @param invocation Method to invoke
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
     * @return Connection or null
     */
    fun getConnection(connectionId: String): Connection?

    /**
     * Get all active connections
     *
     * @return List of active connections
     */
    fun getAllConnections(): List<Connection>
}
