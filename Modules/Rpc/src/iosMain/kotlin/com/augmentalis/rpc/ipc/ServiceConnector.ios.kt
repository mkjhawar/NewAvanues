package com.augmentalis.rpc.ipc

/**
 * iOS Service Connector
 *
 * Placeholder implementation for iOS IPC.
 * iOS uses XPC or URL schemes instead of AIDL services.
 *
 * @since 1.0.0
 * @author Avanues Platform Team
 */
actual class ServiceConnector {

    actual suspend fun connect(endpoint: ServiceEndpoint): ConnectionResult {
        return ConnectionResult.Error(
            IPCError.ServiceUnavailable("iOS does not support AIDL services. Use XPC or URL schemes.")
        )
    }

    actual suspend fun disconnect(connectionId: String) {
        // No-op on iOS
    }

    actual suspend fun invoke(connectionId: String, invocation: MethodInvocation): MethodResult {
        return MethodResult.Error(
            IPCError.ServiceUnavailable("iOS does not support AIDL services. Use XPC or URL schemes.")
        )
    }

    actual fun isConnected(connectionId: String): Boolean = false

    actual fun getConnection(connectionId: String): Connection? = null

    actual fun getAllConnections(): List<Connection> = emptyList()
}
