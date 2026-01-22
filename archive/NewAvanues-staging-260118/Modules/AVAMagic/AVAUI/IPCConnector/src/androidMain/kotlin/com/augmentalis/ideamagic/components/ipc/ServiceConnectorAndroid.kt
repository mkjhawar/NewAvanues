package com.augmentalis.avamagic.components.ipc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import com.augmentalis.avamagic.components.argscanner.ServiceEndpoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/**
 * Android AIDL Service Connector
 *
 * Connects to remote services via Android's AIDL (Android Interface Definition Language).
 *
 * @since 1.0.0
 */
actual class ServiceConnector(
    private var context: Context? = null
) {
    private val mutex = Mutex()
    private val connections = mutableMapOf<String, ConnectionData>()
    private val serviceConnections = mutableMapOf<String, ServiceConnection>()

    /**
     * Set Android context (required before connecting)
     */
    fun setContext(ctx: Context) {
        this.context = ctx
    }

    /**
     * Connect to a service endpoint
     */
    actual suspend fun connect(endpoint: ServiceEndpoint): ConnectionResult {
        val ctx = context ?: return ConnectionResult.Error(
            IPCError.ServiceUnavailable("Context not set. Call setContext() first.")
        )

        val connectionId = generateConnectionId(endpoint)

        return suspendCancellableCoroutine { continuation ->
            val serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    val connection = Connection(
                        id = connectionId,
                        packageName = endpoint.id.substringBefore("."),
                        serviceId = endpoint.id,
                        state = ConnectionState.CONNECTED,
                        protocol = IPCProtocol.AIDL,
                        handle = binder
                    )

                    val connectionData = ConnectionData(connection, this)
                    connections[connectionId] = connectionData
                    serviceConnections[connectionId] = this

                    continuation.resume(ConnectionResult.Success(connection))
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    // Service crashed or was killed
                    connections[connectionId]?.let { data ->
                        val disconnected = data.connection.copy(state = ConnectionState.DISCONNECTED)
                        connections[connectionId] = data.copy(connection = disconnected)
                    }
                }

                override fun onBindingDied(name: ComponentName) {
                    // Process died - more severe than onServiceDisconnected
                    connections.remove(connectionId)
                    serviceConnections.remove(connectionId)
                }
            }

            // Create intent for service
            val intent = Intent().apply {
                action = endpoint.aidlInterface
                setPackage(endpoint.id.substringBefore("."))
            }

            // Bind to service
            try {
                val bound = ctx.bindService(
                    intent,
                    serviceConnection,
                    Context.BIND_AUTO_CREATE
                )

                if (!bound) {
                    continuation.resume(
                        ConnectionResult.Error(
                            IPCError.ServiceNotFound(endpoint.id)
                        )
                    )
                }
            } catch (e: SecurityException) {
                continuation.resume(
                    ConnectionResult.Error(
                        IPCError.PermissionDenied(e.message ?: "Permission denied")
                    )
                )
            } catch (e: Exception) {
                continuation.resume(
                    ConnectionResult.Error(
                        IPCError.ServiceUnavailable(e.message ?: "Binding failed")
                    )
                )
            }

            continuation.invokeOnCancellation {
                try {
                    ctx.unbindService(serviceConnection)
                } catch (e: Exception) {
                    // Ignore unbind errors during cancellation
                }
            }
        }
    }

    /**
     * Disconnect from a service
     */
    actual suspend fun disconnect(connectionId: String) {
        val ctx = context ?: return

        mutex.withLock {
            val serviceConnection = serviceConnections.remove(connectionId)
            if (serviceConnection != null) {
                try {
                    ctx.unbindService(serviceConnection)
                } catch (e: Exception) {
                    // Ignore unbind errors
                }
            }

            connections.remove(connectionId)
        }
    }

    /**
     * Invoke a method on connected service
     */
    actual suspend fun invoke(connectionId: String, invocation: MethodInvocation): MethodResult {
        val connectionData = connections[connectionId]
            ?: return MethodResult.Error(IPCError.ServiceUnavailable("Connection not found"))

        val binder = connectionData.connection.handle as? IBinder
            ?: return MethodResult.Error(IPCError.InvalidResponse("Invalid binder"))

        return try {
            // Note: Actual method invocation would use generated AIDL stubs
            // This is a placeholder showing the pattern

            if (!binder.isBinderAlive) {
                return MethodResult.Error(IPCError.ServiceUnavailable("Service process died"))
            }

            // Simulate method call
            // In real implementation, you would:
            // val service = IYourService.Stub.asInterface(binder)
            // val result = service.yourMethod(params)

            MethodResult.Success("Method ${invocation.methodName} invoked")

        } catch (e: RemoteException) {
            MethodResult.Error(IPCError.NetworkFailure(e))
        } catch (e: SecurityException) {
            MethodResult.Error(IPCError.PermissionDenied(e.message ?: "Permission denied"))
        } catch (e: Exception) {
            MethodResult.Error(IPCError.InvalidResponse(e.message ?: "Unknown error"))
        }
    }

    /**
     * Check if connection is active
     */
    actual fun isConnected(connectionId: String): Boolean {
        return connections[connectionId]?.connection?.state == ConnectionState.CONNECTED
    }

    /**
     * Get connection by ID
     */
    actual fun getConnection(connectionId: String): Connection? {
        return connections[connectionId]?.connection
    }

    /**
     * Get all active connections
     */
    actual fun getAllConnections(): List<Connection> {
        return connections.values.map { it.connection }
    }

    private fun generateConnectionId(endpoint: ServiceEndpoint): String {
        return "${endpoint.id}-${System.currentTimeMillis()}"
    }

    /**
     * Internal connection data
     */
    private data class ConnectionData(
        val connection: Connection,
        val serviceConnection: ServiceConnection
    )
}
