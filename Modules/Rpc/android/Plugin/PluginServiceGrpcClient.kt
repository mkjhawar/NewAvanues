/*
 * PluginServiceGrpcClient.kt - gRPC Client for Universal Plugin Service
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Client for communicating with PluginService from plugin.proto.
 * Supports both Unix Domain Socket (local) and TCP (remote) connections
 * with automatic reconnection and exponential backoff retry logic.
 */

package com.augmentalis.rpc.android.plugin

import com.augmentalis.magiccode.plugins.universal.*
import com.augmentalis.rpc.plugin.*
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.netty.NettyChannelBuilder
import io.netty.channel.epoll.EpollDomainSocketChannel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.unix.DomainSocketAddress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Connection configuration for Plugin Service gRPC client.
 */
data class PluginClientConfig(
    val mode: ConnectionMode,
    val host: String = "localhost",
    val port: Int = UniversalPluginRegistry.DEFAULT_PORT_PLUGIN_REGISTRY,
    val socketPath: String = PluginServiceGrpcServer.DEFAULT_UDS_PATH,
    val connectionTimeoutMs: Long = 5000,
    val requestTimeoutMs: Long = 30000,
    val autoReconnect: Boolean = true,
    val maxRetries: Int = 3,
    val initialBackoffMs: Long = 100,
    val maxBackoffMs: Long = 10000,
    val backoffMultiplier: Double = 2.0,
    val useTls: Boolean = false,
    val keepAliveIntervalSec: Long = 30,
    val keepAliveTimeoutSec: Long = 10
) {
    enum class ConnectionMode {
        UDS,
        TCP
    }
}

/**
 * Connection state for plugin service client.
 */
sealed class PluginConnectionState {
    object Disconnected : PluginConnectionState()
    object Connecting : PluginConnectionState()
    object Connected : PluginConnectionState()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : PluginConnectionState()
    data class Error(val message: String, val cause: Throwable? = null) : PluginConnectionState()
}

/**
 * gRPC Client for Plugin Service.
 *
 * Provides methods to communicate with the PluginService for plugin registration,
 * discovery, lifecycle management, and event bus operations.
 *
 * ## Usage
 * ```kotlin
 * val client = PluginServiceGrpcClient.forLocalConnection()
 * client.connect()
 *
 * // Register a plugin
 * val result = client.registerPlugin(
 *     pluginId = "com.example.myplugin",
 *     pluginName = "My Plugin",
 *     version = "1.0.0",
 *     capabilities = listOf(PluginCapability.LLM_TEXT_GENERATION),
 *     endpointAddress = "localhost:50061"
 * )
 *
 * // Discover plugins
 * val plugins = client.discoverPlugins(listOf("llm.text-generation"))
 *
 * // Subscribe to events
 * client.subscribeEvents(listOf("plugin.registered")).collect { event ->
 *     println("Event: ${event.eventType}")
 * }
 *
 * client.close()
 * ```
 */
class PluginServiceGrpcClient(
    private val config: PluginClientConfig,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : Closeable {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val channelMutex = Mutex()
    private var channel: ManagedChannel? = null
    private var reconnectJob: Job? = null
    private var udsEventLoopGroup: EpollEventLoopGroup? = null

    private val _connectionState = MutableStateFlow<PluginConnectionState>(PluginConnectionState.Disconnected)
    val connectionState: StateFlow<PluginConnectionState> = _connectionState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    /**
     * Connect to the Plugin Service.
     */
    suspend fun connect(): Boolean = withContext(dispatcher) {
        channelMutex.withLock {
            if (channel != null && !channel!!.isShutdown) {
                return@withContext true
            }

            _connectionState.value = PluginConnectionState.Connecting

            try {
                channel = createChannel()
                _connectionState.value = PluginConnectionState.Connected
                _isConnected.value = true
                true
            } catch (e: Exception) {
                _connectionState.value = PluginConnectionState.Error("Connection failed: ${e.message}", e)
                _isConnected.value = false

                if (config.autoReconnect) {
                    startReconnection()
                }
                false
            }
        }
    }

    /**
     * Disconnect from the Plugin Service.
     */
    suspend fun disconnect() = withContext(dispatcher) {
        channelMutex.withLock {
            reconnectJob?.cancel()
            reconnectJob = null

            channel?.let { ch ->
                if (!ch.isShutdown) {
                    ch.shutdown()
                    try {
                        ch.awaitTermination(5, TimeUnit.SECONDS)
                    } catch (e: InterruptedException) {
                        ch.shutdownNow()
                    }
                }
            }
            channel = null
            _connectionState.value = PluginConnectionState.Disconnected
            _isConnected.value = false

            udsEventLoopGroup?.shutdownGracefully()
            udsEventLoopGroup = null
        }
    }

    /**
     * Get the managed channel for gRPC calls.
     */
    suspend fun getChannel(): ManagedChannel = withContext(dispatcher) {
        channelMutex.withLock {
            channel?.let {
                if (!it.isShutdown) return@withContext it
            }
        }

        if (!connect()) {
            throw IllegalStateException("Failed to connect to Plugin Service")
        }

        channel ?: throw IllegalStateException("Channel is null after connect")
    }

    // ========== Plugin Service API Methods ==========

    /**
     * Register a plugin with the Plugin Service.
     *
     * @param pluginId Unique plugin identifier (reverse-domain notation)
     * @param pluginName Human-readable plugin name
     * @param version Plugin version
     * @param capabilities List of capabilities this plugin provides
     * @param endpointAddress Service endpoint address (host:port or UDS path)
     * @param endpointProtocol Protocol type ("grpc", "uds", "tcp")
     * @return Registration result with assigned ID
     */
    suspend fun registerPlugin(
        pluginId: String,
        pluginName: String,
        version: String,
        capabilities: List<PluginCapability>,
        endpointAddress: String,
        endpointProtocol: String = "grpc"
    ): RegisterPluginResult = withRetry { ch ->
        val request = RegisterPluginRequest(
            request_id = generateRequestId(),
            plugin_id = pluginId,
            plugin_name = pluginName,
            version = version,
            capabilities = capabilities.map { it.toProtoCapability() },
            endpoint_address = endpointAddress,
            endpoint_protocol = endpointProtocol
        )

        // Call gRPC stub - this would use generated code
        val response = callRegisterPlugin(ch, request)

        RegisterPluginResult(
            success = response.success,
            message = response.message,
            assignedId = response.assigned_id
        )
    }

    /**
     * Unregister a plugin from the Plugin Service.
     *
     * @param pluginId The plugin ID to unregister
     * @return Result indicating success or failure
     */
    suspend fun unregisterPlugin(pluginId: String): UnregisterPluginResult = withRetry { ch ->
        val request = UnregisterPluginRequest(
            request_id = generateRequestId(),
            plugin_id = pluginId
        )

        val response = callUnregisterPlugin(ch, request)

        UnregisterPluginResult(
            success = response.success,
            message = response.message
        )
    }

    /**
     * Discover plugins by capability.
     *
     * @param capabilityFilter List of capability IDs to filter by (empty = all)
     * @param includeDisabled Whether to include disabled plugins
     * @return List of matching plugins
     */
    suspend fun discoverPlugins(
        capabilityFilter: List<String> = emptyList(),
        includeDisabled: Boolean = false
    ): List<PluginInfoResult> = withRetry { ch ->
        val request = DiscoverPluginsRequest(
            request_id = generateRequestId(),
            capability_filter = capabilityFilter,
            include_disabled = includeDisabled
        )

        val response = callDiscoverPlugins(ch, request)

        response.plugins.map { info ->
            PluginInfoResult(
                pluginId = info.plugin_id,
                pluginName = info.plugin_name,
                version = info.version,
                state = info.state.toPluginState(),
                capabilities = info.capabilities.map { it.toPluginCapability() },
                endpointAddress = info.endpoint_address,
                registeredAt = info.registered_at,
                lastHealthCheck = info.last_health_check
            )
        }
    }

    /**
     * Get information about a specific plugin.
     *
     * @param pluginId The plugin ID to query
     * @return Plugin information or null if not found
     */
    suspend fun getPluginInfo(pluginId: String): PluginInfoResult? = withRetry { ch ->
        val request = GetPluginInfoRequest(
            request_id = generateRequestId(),
            plugin_id = pluginId
        )

        try {
            val info = callGetPluginInfo(ch, request)

            PluginInfoResult(
                pluginId = info.plugin_id,
                pluginName = info.plugin_name,
                version = info.version,
                state = info.state.toPluginState(),
                capabilities = info.capabilities.map { it.toPluginCapability() },
                endpointAddress = info.endpoint_address,
                registeredAt = info.registered_at,
                lastHealthCheck = info.last_health_check
            )
        } catch (e: StatusException) {
            if (e.status.code == Status.Code.NOT_FOUND) {
                null
            } else {
                throw e
            }
        }
    }

    /**
     * Send a lifecycle command to a plugin.
     *
     * @param pluginId The target plugin ID
     * @param action The lifecycle action to perform
     * @param config Optional configuration for CONFIG_CHANGED action
     * @return Result with new state
     */
    suspend fun sendLifecycleCommand(
        pluginId: String,
        action: LifecycleAction,
        config: Map<String, String> = emptyMap()
    ): LifecycleResult = withRetry { ch ->
        val request = LifecycleCommand(
            request_id = generateRequestId(),
            plugin_id = pluginId,
            action = action,
            config = config
        )

        val response = callSendLifecycleCommand(ch, request)

        LifecycleResult(
            success = response.success,
            message = response.message,
            newState = response.new_state.toPluginState()
        )
    }

    /**
     * Subscribe to plugin events.
     *
     * @param eventTypes Event types to subscribe to (empty = all)
     * @param sourcePlugins Source plugins to filter by (empty = all)
     * @param subscriberPluginId ID of the subscribing plugin
     * @return Flow of plugin events
     */
    suspend fun subscribeEvents(
        eventTypes: List<String> = emptyList(),
        sourcePlugins: List<String> = emptyList(),
        subscriberPluginId: String = "client"
    ): Flow<PluginEventResult> = withStreaming { ch ->
        val request = SubscribeEventsRequest(
            request_id = generateRequestId(),
            subscriber_plugin_id = subscriberPluginId,
            event_types = eventTypes,
            source_plugins = sourcePlugins
        )

        callSubscribeEvents(ch, request)
    }

    /**
     * Publish an event to the event bus.
     *
     * @param sourcePluginId The source plugin ID
     * @param eventType The type of event
     * @param payload Event payload
     * @param payloadJson Optional JSON payload for complex data
     * @return Number of subscribers notified
     */
    suspend fun publishEvent(
        sourcePluginId: String,
        eventType: String,
        payload: Map<String, String> = emptyMap(),
        payloadJson: String? = null
    ): PublishEventResult = withRetry { ch ->
        val event = PluginEventProto(
            event_id = generateRequestId(),
            source_plugin_id = sourcePluginId,
            event_type = eventType,
            timestamp = System.currentTimeMillis(),
            payload = payload,
            payload_json = payloadJson ?: ""
        )

        val response = callPublishEvent(ch, event)

        PublishEventResult(
            success = response.success,
            subscribersNotified = response.subscribers_notified
        )
    }

    /**
     * Check health of a plugin.
     *
     * @param pluginId The plugin ID to check
     * @return Health check result
     */
    suspend fun healthCheck(pluginId: String): HealthCheckResult = withRetry { ch ->
        val request = HealthCheckRequest(
            request_id = generateRequestId(),
            plugin_id = pluginId
        )

        val response = callHealthCheck(ch, request)

        HealthCheckResult(
            healthy = response.healthy,
            statusMessage = response.status_message,
            diagnostics = response.diagnostics
        )
    }

    // ========== Helper Methods ==========

    private suspend fun <T> withRetry(block: suspend (ManagedChannel) -> T): T = withContext(dispatcher) {
        var lastException: Exception? = null
        var currentDelay = config.initialBackoffMs

        repeat(config.maxRetries) { attempt ->
            try {
                val ch = getChannel()
                return@withContext block(ch)
            } catch (e: StatusException) {
                lastException = e

                when (e.status.code) {
                    Status.Code.INVALID_ARGUMENT,
                    Status.Code.NOT_FOUND,
                    Status.Code.ALREADY_EXISTS,
                    Status.Code.PERMISSION_DENIED,
                    Status.Code.UNAUTHENTICATED,
                    Status.Code.UNIMPLEMENTED -> throw e
                    else -> {
                        if (attempt < config.maxRetries - 1) {
                            delay(currentDelay)
                            currentDelay = (currentDelay * config.backoffMultiplier)
                                .toLong()
                                .coerceAtMost(config.maxBackoffMs)
                        }
                    }
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < config.maxRetries - 1) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * config.backoffMultiplier)
                        .toLong()
                        .coerceAtMost(config.maxBackoffMs)
                }
            }
        }

        throw lastException ?: IllegalStateException("Retry failed with unknown error")
    }

    private suspend fun <T> withStreaming(block: suspend (ManagedChannel) -> Flow<T>): Flow<T> =
        withContext(dispatcher) {
            val ch = getChannel()
            block(ch)
        }

    private fun createChannel(): ManagedChannel {
        return when (config.mode) {
            PluginClientConfig.ConnectionMode.UDS -> createUdsChannel()
            PluginClientConfig.ConnectionMode.TCP -> createTcpChannel()
        }
    }

    private fun createUdsChannel(): ManagedChannel {
        // Shut down any previous group before creating a new one
        udsEventLoopGroup?.shutdownGracefully()
        val eventLoopGroup = EpollEventLoopGroup()
        udsEventLoopGroup = eventLoopGroup

        return NettyChannelBuilder
            .forAddress(DomainSocketAddress(config.socketPath))
            .channelType(EpollDomainSocketChannel::class.java)
            .eventLoopGroup(eventLoopGroup)
            .usePlaintext()
            .keepAliveTime(config.keepAliveIntervalSec, TimeUnit.SECONDS)
            .keepAliveTimeout(config.keepAliveTimeoutSec, TimeUnit.SECONDS)
            .keepAliveWithoutCalls(true)
            .build()
    }

    private fun createTcpChannel(): ManagedChannel {
        val builder = ManagedChannelBuilder
            .forAddress(config.host, config.port)
            .keepAliveTime(config.keepAliveIntervalSec, TimeUnit.SECONDS)
            .keepAliveTimeout(config.keepAliveTimeoutSec, TimeUnit.SECONDS)
            .keepAliveWithoutCalls(true)

        if (!config.useTls) {
            builder.usePlaintext()
        }

        return builder.build()
    }

    private fun startReconnection() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            var currentDelay = config.initialBackoffMs
            val maxAttempts = 10

            repeat(maxAttempts) { attempt ->
                _connectionState.value = PluginConnectionState.Reconnecting(attempt + 1, maxAttempts)

                delay(currentDelay)

                try {
                    channelMutex.withLock {
                        channel = createChannel()
                        _connectionState.value = PluginConnectionState.Connected
                        _isConnected.value = true
                    }
                    return@launch
                } catch (e: Exception) {
                    currentDelay = (currentDelay * config.backoffMultiplier)
                        .toLong()
                        .coerceAtMost(config.maxBackoffMs)
                }
            }

            _connectionState.value = PluginConnectionState.Error("Max reconnection attempts reached")
            _isConnected.value = false
        }
    }

    private fun generateRequestId(): String = UUID.randomUUID().toString()

    override fun close() {
        scope.launch { disconnect() }
    }

    // ========== Stub gRPC Calls (will use generated code) ==========

    private suspend fun callRegisterPlugin(ch: ManagedChannel, request: RegisterPluginRequest): RegisterPluginResponse {
        // TODO: Use generated gRPC stub
        // val stub = PluginServiceGrpc.newBlockingStub(ch)
        // return stub.registerPlugin(request)
        return RegisterPluginResponse(
            request_id = request.request_id,
            success = true,
            message = "Stub response",
            assigned_id = request.plugin_id
        )
    }

    private suspend fun callUnregisterPlugin(ch: ManagedChannel, request: UnregisterPluginRequest): UnregisterPluginResponse {
        return UnregisterPluginResponse(
            request_id = request.request_id,
            success = true,
            message = "Stub response"
        )
    }

    private suspend fun callDiscoverPlugins(ch: ManagedChannel, request: DiscoverPluginsRequest): DiscoverPluginsResponse {
        return DiscoverPluginsResponse(
            request_id = request.request_id,
            plugins = emptyList()
        )
    }

    private suspend fun callGetPluginInfo(ch: ManagedChannel, request: GetPluginInfoRequest): PluginInfo {
        return PluginInfo(
            plugin_id = request.plugin_id,
            plugin_name = "Unknown",
            version = "0.0.0",
            state = PluginStateProto.PLUGIN_STATE_UNKNOWN
        )
    }

    private suspend fun callSendLifecycleCommand(ch: ManagedChannel, request: LifecycleCommand): LifecycleResponse {
        return LifecycleResponse(
            request_id = request.request_id,
            success = true,
            message = "Stub response",
            new_state = PluginStateProto.PLUGIN_STATE_ACTIVE
        )
    }

    private suspend fun callSubscribeEvents(ch: ManagedChannel, request: SubscribeEventsRequest): Flow<PluginEventResult> {
        return flow {
            // Stub: emit no events
        }
    }

    private suspend fun callPublishEvent(ch: ManagedChannel, event: PluginEventProto): PublishEventResponse {
        return PublishEventResponse(
            request_id = event.event_id,
            success = true,
            subscribers_notified = 0
        )
    }

    private suspend fun callHealthCheck(ch: ManagedChannel, request: HealthCheckRequest): HealthCheckResponse {
        return HealthCheckResponse(
            request_id = request.request_id,
            healthy = true,
            status_message = "Stub response"
        )
    }

    companion object {
        /**
         * Create a client for local UDS connection.
         */
        fun forLocalConnection(
            socketPath: String = PluginServiceGrpcServer.DEFAULT_UDS_PATH
        ): PluginServiceGrpcClient {
            return PluginServiceGrpcClient(
                PluginClientConfig(
                    mode = PluginClientConfig.ConnectionMode.UDS,
                    socketPath = socketPath
                )
            )
        }

        /**
         * Create a client for remote TCP connection.
         */
        fun forRemoteConnection(
            host: String,
            port: Int = UniversalPluginRegistry.DEFAULT_PORT_PLUGIN_REGISTRY,
            useTls: Boolean = false
        ): PluginServiceGrpcClient {
            return PluginServiceGrpcClient(
                PluginClientConfig(
                    mode = PluginClientConfig.ConnectionMode.TCP,
                    host = host,
                    port = port,
                    useTls = useTls
                )
            )
        }
    }
}

// ========== Result Data Classes ==========

data class RegisterPluginResult(
    val success: Boolean,
    val message: String,
    val assignedId: String
)

data class UnregisterPluginResult(
    val success: Boolean,
    val message: String
)

data class PluginInfoResult(
    val pluginId: String,
    val pluginName: String,
    val version: String,
    val state: PluginState,
    val capabilities: List<PluginCapability>,
    val endpointAddress: String,
    val registeredAt: Long,
    val lastHealthCheck: Long
)

data class LifecycleResult(
    val success: Boolean,
    val message: String,
    val newState: PluginState
)

data class PluginEventResult(
    val eventId: String,
    val sourcePluginId: String,
    val eventType: String,
    val timestamp: Long,
    val payload: Map<String, String>,
    val payloadJson: String?
)

data class PublishEventResult(
    val success: Boolean,
    val subscribersNotified: Int
)

data class HealthCheckResult(
    val healthy: Boolean,
    val statusMessage: String,
    val diagnostics: Map<String, String>
)

// ========== Extension Functions ==========

private fun PluginCapability.toProtoCapability(): PluginCapabilityProto = PluginCapabilityProto(
    id = this.id,
    name = this.name,
    version = this.version,
    interfaces = this.interfaces.toList(),
    metadata = this.metadata
)

private fun PluginCapabilityProto.toPluginCapability(): PluginCapability = PluginCapability(
    id = this.id,
    name = this.name,
    version = this.version,
    interfaces = this.interfaces.toSet(),
    metadata = this.metadata
)

private fun PluginStateProto.toPluginState(): PluginState = when (this) {
    PluginStateProto.PLUGIN_STATE_UNKNOWN -> PluginState.UNINITIALIZED
    PluginStateProto.PLUGIN_STATE_REGISTERED -> PluginState.UNINITIALIZED
    PluginStateProto.PLUGIN_STATE_INITIALIZING -> PluginState.INITIALIZING
    PluginStateProto.PLUGIN_STATE_ACTIVE -> PluginState.ACTIVE
    PluginStateProto.PLUGIN_STATE_PAUSED -> PluginState.PAUSED
    PluginStateProto.PLUGIN_STATE_ERROR -> PluginState.ERROR
    PluginStateProto.PLUGIN_STATE_STOPPING -> PluginState.STOPPING
    PluginStateProto.PLUGIN_STATE_STOPPED -> PluginState.STOPPED
}
