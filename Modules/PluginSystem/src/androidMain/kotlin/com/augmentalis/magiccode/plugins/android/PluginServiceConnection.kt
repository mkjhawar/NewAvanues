/**
 * PluginServiceConnection.kt - Service binding manager for plugins
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Manages Android service bindings for plugins that need to connect to
 * Android services. Handles service lifecycle, connection states, and
 * automatic rebinding.
 */
package com.augmentalis.magiccode.plugins.android

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.augmentalis.magiccode.plugins.core.PluginLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents the state of a service connection.
 *
 * @property DISCONNECTED Not connected to any service
 * @property CONNECTING Connection in progress
 * @property CONNECTED Successfully connected
 * @property BINDING_FAILED Binding attempt failed
 * @property DISCONNECTING Disconnection in progress
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    BINDING_FAILED,
    DISCONNECTING
}

/**
 * Information about a bound service connection.
 *
 * @property pluginId Plugin that owns this connection
 * @property serviceClass The service class being bound
 * @property state Current connection state
 * @property binder The service binder (null if not connected)
 * @property connectedAt Timestamp when connection was established (0 if not connected)
 * @property lastError Last error message if binding failed
 */
data class ServiceConnectionInfo(
    val pluginId: String,
    val serviceClass: Class<*>,
    val state: ConnectionState,
    val binder: IBinder? = null,
    val connectedAt: Long = 0,
    val lastError: String? = null
)

/**
 * Service connection manager for plugins that need to bind to Android services.
 *
 * This class manages the lifecycle of service bindings for plugins, including:
 * - Binding and unbinding services
 * - Tracking connection states
 * - Automatic cleanup on plugin unload
 * - Connection state observation via StateFlow
 *
 * ## Usage Example
 * ```kotlin
 * val connectionManager = PluginServiceConnection(context, host)
 *
 * // Bind to a service
 * val success = connectionManager.bindService(
 *     pluginId = "com.example.myplugin",
 *     serviceClass = MyBackgroundService::class.java
 * )
 *
 * // Wait for connection
 * val binder = connectionManager.awaitConnection("com.example.myplugin", timeout = 5000)
 *
 * // Check connection state
 * if (connectionManager.isServiceBound("com.example.myplugin")) {
 *     // Service is connected
 * }
 *
 * // Observe connection state changes
 * connectionManager.getConnectionState("com.example.myplugin")
 *     .collect { info ->
 *         when (info?.state) {
 *             ConnectionState.CONNECTED -> // Handle connection
 *             ConnectionState.DISCONNECTED -> // Handle disconnection
 *             else -> {}
 *         }
 *     }
 *
 * // Unbind when done
 * connectionManager.unbindService("com.example.myplugin")
 * ```
 *
 * @param context Android Context for service binding
 * @param host Reference to the plugin host for callbacks
 * @param scope CoroutineScope for async operations
 *
 * @since 1.0.0
 * @see AndroidPluginHost
 */
class PluginServiceConnection(
    private val context: Context,
    private val host: AndroidPluginHost? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    /**
     * Map of plugin ID to service connection information.
     */
    private val _connections = MutableStateFlow<Map<String, ServiceConnectionInfo>>(emptyMap())

    /**
     * Observable map of all connections.
     */
    val connections: StateFlow<Map<String, ServiceConnectionInfo>> = _connections.asStateFlow()

    /**
     * Active ServiceConnection instances for proper unbinding.
     */
    private val serviceConnections = ConcurrentHashMap<String, PluginServiceConnectionImpl>()

    /**
     * Channels for awaiting connection completion.
     */
    private val connectionChannels = ConcurrentHashMap<String, Channel<Boolean>>()

    /**
     * Mutex for thread-safe state updates.
     */
    private val mutex = Mutex()

    /**
     * Listeners for connection events.
     */
    private val listeners = mutableListOf<ConnectionListener>()

    /**
     * Bind a service for a plugin.
     *
     * Initiates a service binding and tracks the connection state.
     * The binding is asynchronous - use awaitConnection() to wait for completion.
     *
     * @param pluginId The plugin requesting the service
     * @param serviceClass The service class to bind
     * @param bindingFlags Flags passed to Context.bindService (default: BIND_AUTO_CREATE)
     * @return true if binding was initiated, false if binding failed to start
     */
    suspend fun bindService(
        pluginId: String,
        serviceClass: Class<*>,
        bindingFlags: Int = Context.BIND_AUTO_CREATE
    ): Boolean {
        // Check if already bound
        val existing = _connections.value[pluginId]
        if (existing != null && existing.state == ConnectionState.CONNECTED) {
            PluginLog.d(TAG, "Service already bound for plugin: $pluginId")
            return true
        }

        return mutex.withLock {
            try {
                PluginLog.d(TAG, "Binding service ${serviceClass.simpleName} for plugin: $pluginId")

                // Update state to connecting
                updateConnectionState(
                    pluginId,
                    ServiceConnectionInfo(
                        pluginId = pluginId,
                        serviceClass = serviceClass,
                        state = ConnectionState.CONNECTING
                    )
                )

                // Create the service connection
                val connection = PluginServiceConnectionImpl(pluginId, serviceClass)
                serviceConnections[pluginId] = connection

                // Create channel for awaiting connection
                connectionChannels[pluginId] = Channel(1)

                // Create intent and bind
                val intent = Intent(context, serviceClass)
                val bound = context.bindService(intent, connection, bindingFlags)

                if (!bound) {
                    PluginLog.e(TAG, "Failed to initiate service binding for plugin: $pluginId")
                    updateConnectionState(
                        pluginId,
                        ServiceConnectionInfo(
                            pluginId = pluginId,
                            serviceClass = serviceClass,
                            state = ConnectionState.BINDING_FAILED,
                            lastError = "bindService returned false"
                        )
                    )
                    serviceConnections.remove(pluginId)
                    connectionChannels.remove(pluginId)?.close()
                }

                bound
            } catch (e: Exception) {
                PluginLog.e(TAG, "Exception during service binding for plugin: $pluginId", e)
                updateConnectionState(
                    pluginId,
                    ServiceConnectionInfo(
                        pluginId = pluginId,
                        serviceClass = serviceClass,
                        state = ConnectionState.BINDING_FAILED,
                        lastError = e.message
                    )
                )
                false
            }
        }
    }

    /**
     * Bind to a service using an Intent.
     *
     * Allows specifying a custom intent for binding to services
     * not defined by a simple class.
     *
     * @param pluginId The plugin requesting the service
     * @param intent The intent specifying the service to bind
     * @param bindingFlags Flags passed to Context.bindService
     * @return true if binding was initiated
     */
    suspend fun bindServiceWithIntent(
        pluginId: String,
        intent: Intent,
        bindingFlags: Int = Context.BIND_AUTO_CREATE
    ): Boolean {
        return mutex.withLock {
            try {
                val componentClass = intent.component?.className?.let {
                    Class.forName(it)
                } ?: Any::class.java

                PluginLog.d(TAG, "Binding service with intent for plugin: $pluginId")

                updateConnectionState(
                    pluginId,
                    ServiceConnectionInfo(
                        pluginId = pluginId,
                        serviceClass = componentClass,
                        state = ConnectionState.CONNECTING
                    )
                )

                val connection = PluginServiceConnectionImpl(pluginId, componentClass)
                serviceConnections[pluginId] = connection
                connectionChannels[pluginId] = Channel(1)

                val bound = context.bindService(intent, connection, bindingFlags)

                if (!bound) {
                    updateConnectionState(
                        pluginId,
                        ServiceConnectionInfo(
                            pluginId = pluginId,
                            serviceClass = componentClass,
                            state = ConnectionState.BINDING_FAILED,
                            lastError = "bindService returned false"
                        )
                    )
                    serviceConnections.remove(pluginId)
                    connectionChannels.remove(pluginId)?.close()
                }

                bound
            } catch (e: Exception) {
                PluginLog.e(TAG, "Exception during intent binding for plugin: $pluginId", e)
                false
            }
        }
    }

    /**
     * Wait for a service connection to complete.
     *
     * Suspends until the service is connected or timeout expires.
     *
     * @param pluginId The plugin waiting for connection
     * @param timeoutMs Maximum time to wait in milliseconds
     * @return The service binder if connected, null if timeout or failed
     */
    suspend fun awaitConnection(pluginId: String, timeoutMs: Long = 10000): IBinder? {
        val channel = connectionChannels[pluginId] ?: return null

        return withTimeoutOrNull(timeoutMs) {
            // Wait for connection signal
            channel.receive()
            // Return the binder if connected
            _connections.value[pluginId]?.binder
        }
    }

    /**
     * Unbind a service for a plugin.
     *
     * Disconnects from the service and cleans up connection state.
     *
     * @param pluginId The plugin whose service should be unbound
     */
    suspend fun unbindService(pluginId: String) {
        mutex.withLock {
            val connection = serviceConnections.remove(pluginId)
            if (connection != null) {
                PluginLog.d(TAG, "Unbinding service for plugin: $pluginId")

                val info = _connections.value[pluginId]
                if (info != null) {
                    updateConnectionState(
                        pluginId,
                        info.copy(state = ConnectionState.DISCONNECTING)
                    )
                }

                try {
                    context.unbindService(connection)
                } catch (e: IllegalArgumentException) {
                    PluginLog.w(TAG, "Service was not bound when unbinding: $pluginId", e)
                }

                updateConnectionState(
                    pluginId,
                    ServiceConnectionInfo(
                        pluginId = pluginId,
                        serviceClass = info?.serviceClass ?: Any::class.java,
                        state = ConnectionState.DISCONNECTED
                    )
                )

                connectionChannels.remove(pluginId)?.close()
            }
        }
    }

    /**
     * Check if a service is bound for a plugin.
     *
     * @param pluginId The plugin identifier
     * @return true if the service is connected
     */
    fun isServiceBound(pluginId: String): Boolean {
        return _connections.value[pluginId]?.state == ConnectionState.CONNECTED
    }

    /**
     * Get the connection state flow for a specific plugin.
     *
     * @param pluginId The plugin identifier
     * @return StateFlow of connection info, or null if no connection exists
     */
    fun getConnectionState(pluginId: String): StateFlow<ServiceConnectionInfo?> {
        return MutableStateFlow(_connections.value[pluginId]).asStateFlow()
    }

    /**
     * Get the service binder for a plugin.
     *
     * @param pluginId The plugin identifier
     * @return The IBinder if connected, null otherwise
     */
    fun getBinder(pluginId: String): IBinder? {
        return _connections.value[pluginId]?.binder
    }

    /**
     * Get all current connections.
     *
     * @return List of all connection information
     */
    fun getAllConnections(): List<ServiceConnectionInfo> {
        return _connections.value.values.toList()
    }

    /**
     * Unbind all services.
     *
     * Disconnects from all bound services. Typically called during
     * plugin host shutdown.
     */
    suspend fun unbindAll() {
        PluginLog.i(TAG, "Unbinding all services (${serviceConnections.size} connections)")

        val pluginIds = serviceConnections.keys.toList()
        pluginIds.forEach { pluginId ->
            unbindService(pluginId)
        }
    }

    /**
     * Add a connection listener.
     *
     * @param listener The listener to add
     */
    fun addConnectionListener(listener: ConnectionListener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    /**
     * Remove a connection listener.
     *
     * @param listener The listener to remove
     */
    fun removeConnectionListener(listener: ConnectionListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    /**
     * Update connection state and notify listeners.
     */
    private fun updateConnectionState(pluginId: String, info: ServiceConnectionInfo) {
        val current = _connections.value.toMutableMap()
        current[pluginId] = info
        _connections.value = current

        // Notify listeners
        val currentListeners = synchronized(listeners) { listeners.toList() }
        when (info.state) {
            ConnectionState.CONNECTED -> {
                currentListeners.forEach { it.onServiceConnected(pluginId, info.binder!!) }
            }
            ConnectionState.DISCONNECTED -> {
                currentListeners.forEach { it.onServiceDisconnected(pluginId) }
            }
            ConnectionState.BINDING_FAILED -> {
                currentListeners.forEach {
                    it.onBindingFailed(pluginId, info.lastError ?: "Unknown error")
                }
            }
            else -> {}
        }
    }

    /**
     * Internal ServiceConnection implementation.
     */
    private inner class PluginServiceConnectionImpl(
        private val pluginId: String,
        private val serviceClass: Class<*>
    ) : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            PluginLog.i(TAG, "Service connected for plugin: $pluginId (${name?.className})")

            scope.launch {
                updateConnectionState(
                    pluginId,
                    ServiceConnectionInfo(
                        pluginId = pluginId,
                        serviceClass = serviceClass,
                        state = ConnectionState.CONNECTED,
                        binder = service,
                        connectedAt = System.currentTimeMillis()
                    )
                )

                // Signal waiting coroutines
                connectionChannels[pluginId]?.send(true)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            PluginLog.w(TAG, "Service disconnected for plugin: $pluginId (${name?.className})")

            scope.launch {
                updateConnectionState(
                    pluginId,
                    ServiceConnectionInfo(
                        pluginId = pluginId,
                        serviceClass = serviceClass,
                        state = ConnectionState.DISCONNECTED
                    )
                )

                // Signal waiting coroutines with failure
                connectionChannels[pluginId]?.close()
            }
        }

        override fun onBindingDied(name: ComponentName?) {
            PluginLog.e(TAG, "Service binding died for plugin: $pluginId (${name?.className})")

            scope.launch {
                updateConnectionState(
                    pluginId,
                    ServiceConnectionInfo(
                        pluginId = pluginId,
                        serviceClass = serviceClass,
                        state = ConnectionState.DISCONNECTED,
                        lastError = "Binding died"
                    )
                )

                connectionChannels[pluginId]?.close()
            }
        }

        override fun onNullBinding(name: ComponentName?) {
            PluginLog.w(TAG, "Null binding for plugin: $pluginId (${name?.className})")

            scope.launch {
                updateConnectionState(
                    pluginId,
                    ServiceConnectionInfo(
                        pluginId = pluginId,
                        serviceClass = serviceClass,
                        state = ConnectionState.BINDING_FAILED,
                        lastError = "Service returned null binding"
                    )
                )

                connectionChannels[pluginId]?.send(false)
            }
        }
    }

    companion object {
        private const val TAG = "PluginServiceConnection"
    }
}

/**
 * Listener interface for service connection events.
 */
interface ConnectionListener {
    /**
     * Called when a service is successfully connected.
     *
     * @param pluginId The plugin that owns the connection
     * @param binder The service binder
     */
    fun onServiceConnected(pluginId: String, binder: IBinder)

    /**
     * Called when a service is disconnected.
     *
     * @param pluginId The plugin that owned the connection
     */
    fun onServiceDisconnected(pluginId: String)

    /**
     * Called when binding fails.
     *
     * @param pluginId The plugin that requested the binding
     * @param error Error message describing the failure
     */
    fun onBindingFailed(pluginId: String, error: String)
}

/**
 * Simple implementation of ConnectionListener using lambdas.
 */
class SimpleConnectionListener(
    private val onConnected: (String, IBinder) -> Unit = { _, _ -> },
    private val onDisconnected: (String) -> Unit = { },
    private val onFailed: (String, String) -> Unit = { _, _ -> }
) : ConnectionListener {
    override fun onServiceConnected(pluginId: String, binder: IBinder) = onConnected(pluginId, binder)
    override fun onServiceDisconnected(pluginId: String) = onDisconnected(pluginId)
    override fun onBindingFailed(pluginId: String, error: String) = onFailed(pluginId, error)
}
