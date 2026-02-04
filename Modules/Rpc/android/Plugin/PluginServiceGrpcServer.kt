/*
 * PluginServiceGrpcServer.kt - gRPC Server for Universal Plugin Service
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Implements PluginService from plugin.proto using grpc-kotlin.
 * Supports both UDS (Unix Domain Socket) for local IPC and TCP for remote connections.
 * Integrates with UniversalPluginRegistry and PluginEventBus for plugin management.
 */

package com.augmentalis.rpc.android.plugin

import com.augmentalis.magiccode.plugins.universal.*
import com.augmentalis.rpc.ServiceEndpoint
import com.augmentalis.rpc.plugin.*
import io.grpc.Server
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

/**
 * Plugin Service gRPC Server implementation.
 *
 * Provides gRPC endpoints for plugin registration, discovery, lifecycle management,
 * and event bus operations. Delegates actual operations to UniversalPluginRegistry
 * and GrpcPluginEventBus.
 *
 * @param registry The UniversalPluginRegistry for plugin management
 * @param eventBus The GrpcPluginEventBus for event broadcasting
 * @param serverConfig Server configuration for transport options
 */
class PluginServiceGrpcServer(
    private val registry: UniversalPluginRegistry,
    private val eventBus: GrpcPluginEventBus,
    private val serverConfig: ServerConfig = ServerConfig()
) {
    private var server: Server? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Server configuration for transport options.
     */
    data class ServerConfig(
        val port: Int = UniversalPluginRegistry.DEFAULT_PORT_PLUGIN_REGISTRY,
        val udsPath: String? = null,
        val useTls: Boolean = false,
        val maxConcurrentStreams: Int = 100,
        val keepAliveTimeSeconds: Long = 60,
        val maxInboundMessageSize: Int = 4 * 1024 * 1024  // 4MB
    )

    /**
     * Starts the gRPC server with configured transport.
     */
    fun start() {
        val serviceImpl = PluginServiceImpl(registry, eventBus, scope)

        server = if (serverConfig.udsPath != null) {
            createUdsServer(serviceImpl)
        } else {
            createTcpServer(serviceImpl)
        }

        server?.start()
    }

    /**
     * Creates a UDS-based server for local IPC.
     */
    private fun createUdsServer(serviceImpl: PluginServiceGrpc.PluginServiceImplBase): Server {
        val socketFile = File(serverConfig.udsPath!!)
        if (socketFile.exists()) {
            socketFile.delete()
        }

        return NettyServerBuilder
            .forAddress(io.netty.channel.unix.DomainSocketAddress(socketFile))
            .addService(serviceImpl)
            .maxConcurrentCallsPerConnection(serverConfig.maxConcurrentStreams)
            .maxInboundMessageSize(serverConfig.maxInboundMessageSize)
            .build()
    }

    /**
     * Creates a TCP-based server for remote connections.
     */
    private fun createTcpServer(serviceImpl: PluginServiceGrpc.PluginServiceImplBase): Server {
        return NettyServerBuilder
            .forAddress(InetSocketAddress(serverConfig.port))
            .addService(serviceImpl)
            .maxConcurrentCallsPerConnection(serverConfig.maxConcurrentStreams)
            .maxInboundMessageSize(serverConfig.maxInboundMessageSize)
            .keepAliveTime(serverConfig.keepAliveTimeSeconds, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Stops the gRPC server gracefully.
     */
    fun stop() {
        scope.cancel()
        server?.shutdown()?.awaitTermination(5, TimeUnit.SECONDS)
    }

    /**
     * Blocks until the server is terminated.
     */
    fun blockUntilShutdown() {
        server?.awaitTermination()
    }

    /**
     * Returns true if the server is currently running.
     */
    fun isRunning(): Boolean = server?.isShutdown == false

    /**
     * Returns the port the server is listening on.
     */
    fun getPort(): Int = server?.port ?: -1

    companion object {
        /** Default UDS path for plugin service */
        const val DEFAULT_UDS_PATH = "/data/local/tmp/plugin_service.sock"
    }
}

/**
 * gRPC service implementation for PluginService.
 */
internal class PluginServiceImpl(
    private val registry: UniversalPluginRegistry,
    private val eventBus: GrpcPluginEventBus,
    private val scope: CoroutineScope
) : PluginServiceGrpc.PluginServiceImplBase() {

    override fun registerPlugin(
        request: RegisterPluginRequest,
        responseObserver: StreamObserver<RegisterPluginResponse>
    ) {
        scope.launch {
            try {
                // Create endpoint from request
                val endpoint = ServiceEndpoint(
                    serviceName = request.plugin_id,
                    host = parseHost(request.endpoint_address),
                    port = parsePort(request.endpoint_address),
                    protocol = request.endpoint_protocol,
                    metadata = mapOf(
                        "version" to request.version,
                        "capabilities" to request.capabilities.joinToString(",") { it.id }
                    )
                )

                // Create a PluginRegistration
                val registration = PluginRegistration(
                    pluginId = request.plugin_id,
                    pluginName = request.plugin_name,
                    version = request.version,
                    capabilities = request.capabilities.map { cap ->
                        PluginCapability(
                            id = cap.id,
                            name = cap.name,
                            version = cap.version,
                            interfaces = cap.interfaces.toSet(),
                            metadata = cap.metadata
                        )
                    }.toSet(),
                    state = PluginState.ACTIVE,
                    endpoint = endpoint,
                    registeredAt = System.currentTimeMillis()
                )

                val result = registry.register(registration)

                val response = if (result.isSuccess) {
                    // Publish registration event
                    eventBus.publish(createPluginEvent(
                        sourcePluginId = request.plugin_id,
                        eventType = PluginEvent.TYPE_REGISTERED,
                        payload = mapOf(
                            "version" to request.version,
                            "capabilities" to request.capabilities.size.toString()
                        )
                    ))

                    RegisterPluginResponse(
                        request_id = request.request_id,
                        success = true,
                        message = "Plugin registered successfully",
                        assigned_id = request.plugin_id
                    )
                } else {
                    RegisterPluginResponse(
                        request_id = request.request_id,
                        success = false,
                        message = result.exceptionOrNull()?.message ?: "Registration failed"
                    )
                }

                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun unregisterPlugin(
        request: UnregisterPluginRequest,
        responseObserver: StreamObserver<UnregisterPluginResponse>
    ) {
        scope.launch {
            try {
                val success = registry.unregister(request.plugin_id)

                if (success) {
                    eventBus.publish(createPluginEvent(
                        sourcePluginId = request.plugin_id,
                        eventType = PluginEvent.TYPE_UNREGISTERED
                    ))
                }

                val response = UnregisterPluginResponse(
                    request_id = request.request_id,
                    success = success,
                    message = if (success) "Plugin unregistered" else "Plugin not found"
                )

                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun discoverPlugins(
        request: DiscoverPluginsRequest,
        responseObserver: StreamObserver<DiscoverPluginsResponse>
    ) {
        scope.launch {
            try {
                val plugins = if (request.capability_filter.isEmpty()) {
                    if (request.include_disabled) {
                        registry.plugins.value.values.toList()
                    } else {
                        registry.getActivePlugins()
                    }
                } else {
                    registry.discoverByCapabilities(request.capability_filter.toSet())
                }

                val pluginInfos = plugins.map { reg ->
                    PluginInfo(
                        plugin_id = reg.pluginId,
                        plugin_name = reg.pluginName,
                        version = reg.version,
                        state = reg.state.toProtoState(),
                        capabilities = reg.capabilities.map { it.toProtoCapability() },
                        endpoint_address = "${reg.endpoint.host}:${reg.endpoint.port}",
                        registered_at = reg.registeredAt,
                        last_health_check = reg.lastHealthCheck
                    )
                }

                val response = DiscoverPluginsResponse(
                    request_id = request.request_id,
                    plugins = pluginInfos
                )

                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun getPluginInfo(
        request: GetPluginInfoRequest,
        responseObserver: StreamObserver<PluginInfo>
    ) {
        scope.launch {
            try {
                val registration = registry.getPlugin(request.plugin_id)

                if (registration != null) {
                    val pluginInfo = PluginInfo(
                        plugin_id = registration.pluginId,
                        plugin_name = registration.pluginName,
                        version = registration.version,
                        state = registration.state.toProtoState(),
                        capabilities = registration.capabilities.map { it.toProtoCapability() },
                        endpoint_address = "${registration.endpoint.host}:${registration.endpoint.port}",
                        registered_at = registration.registeredAt,
                        last_health_check = registration.lastHealthCheck
                    )
                    responseObserver.onNext(pluginInfo)
                    responseObserver.onCompleted()
                } else {
                    responseObserver.onError(
                        io.grpc.Status.NOT_FOUND
                            .withDescription("Plugin not found: ${request.plugin_id}")
                            .asException()
                    )
                }
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun sendLifecycleCommand(
        request: LifecycleCommand,
        responseObserver: StreamObserver<LifecycleResponse>
    ) {
        scope.launch {
            try {
                val registration = registry.getPlugin(request.plugin_id)

                if (registration == null) {
                    responseObserver.onNext(LifecycleResponse(
                        request_id = request.request_id,
                        success = false,
                        message = "Plugin not found: ${request.plugin_id}"
                    ))
                    responseObserver.onCompleted()
                    return@launch
                }

                val newState = when (request.action) {
                    LifecycleAction.LIFECYCLE_ACTIVATE -> PluginState.ACTIVE
                    LifecycleAction.LIFECYCLE_PAUSE -> PluginState.PAUSED
                    LifecycleAction.LIFECYCLE_RESUME -> PluginState.ACTIVE
                    LifecycleAction.LIFECYCLE_STOP -> PluginState.STOPPED
                    LifecycleAction.LIFECYCLE_CONFIG_CHANGED -> registration.state
                    else -> registration.state
                }

                val success = registry.updateState(request.plugin_id, newState)

                if (success) {
                    eventBus.publish(createStateChangeEvent(
                        pluginId = request.plugin_id,
                        newState = newState.name,
                        oldState = registration.state.name
                    ))
                }

                val response = LifecycleResponse(
                    request_id = request.request_id,
                    success = success,
                    message = if (success) "State updated to $newState" else "Failed to update state",
                    new_state = newState.toProtoState()
                )

                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun subscribeEvents(
        request: SubscribeEventsRequest,
        responseObserver: StreamObserver<PluginEventProto>
    ) {
        scope.launch {
            try {
                val filter = EventFilter(
                    eventTypes = request.event_types.toSet(),
                    sourcePlugins = request.source_plugins.toSet()
                )

                // Register subscription for tracking
                eventBus.registerSubscription(
                    subscriptionId = "${request.subscriber_plugin_id}_${request.request_id}",
                    filter = filter
                )

                eventBus.subscribe(filter).collect { event ->
                    val protoEvent = PluginEventProto(
                        event_id = event.eventId,
                        source_plugin_id = event.sourcePluginId,
                        event_type = event.eventType,
                        timestamp = event.timestamp,
                        payload = event.payload,
                        payload_json = event.payloadJson ?: ""
                    )
                    responseObserver.onNext(protoEvent)
                }

                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun publishEvent(
        request: PluginEventProto,
        responseObserver: StreamObserver<PublishEventResponse>
    ) {
        scope.launch {
            try {
                val event = PluginEvent(
                    eventId = request.event_id,
                    sourcePluginId = request.source_plugin_id,
                    eventType = request.event_type,
                    timestamp = request.timestamp,
                    payload = request.payload,
                    payloadJson = request.payload_json.takeIf { it.isNotEmpty() }
                )

                val subscribersNotified = eventBus.publish(event)

                val response = PublishEventResponse(
                    request_id = request.event_id,
                    success = true,
                    subscribers_notified = subscribersNotified
                )

                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun healthCheck(
        request: HealthCheckRequest,
        responseObserver: StreamObserver<HealthCheckResponse>
    ) {
        scope.launch {
            try {
                val registration = registry.getPlugin(request.plugin_id)

                val response = if (registration != null) {
                    // Update health check timestamp
                    registry.updateHealthCheck(request.plugin_id)

                    HealthCheckResponse(
                        request_id = request.request_id,
                        healthy = registration.state == PluginState.ACTIVE,
                        status_message = "Plugin state: ${registration.state}",
                        diagnostics = mapOf(
                            "state" to registration.state.name,
                            "version" to registration.version,
                            "uptime" to "${System.currentTimeMillis() - registration.registeredAt}ms"
                        )
                    )
                } else {
                    HealthCheckResponse(
                        request_id = request.request_id,
                        healthy = false,
                        status_message = "Plugin not found"
                    )
                }

                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    // Helper functions
    private fun parseHost(address: String): String {
        return if (address.contains(":")) {
            address.substringBefore(":")
        } else {
            address
        }
    }

    private fun parsePort(address: String): Int {
        return if (address.contains(":")) {
            address.substringAfter(":").toIntOrNull() ?: 50060
        } else {
            50060
        }
    }
}

/**
 * Extension to convert PluginState to proto PluginStateProto.
 */
private fun PluginState.toProtoState(): PluginStateProto = when (this) {
    PluginState.UNINITIALIZED -> PluginStateProto.PLUGIN_STATE_UNKNOWN
    PluginState.INITIALIZING -> PluginStateProto.PLUGIN_STATE_INITIALIZING
    PluginState.ACTIVE -> PluginStateProto.PLUGIN_STATE_ACTIVE
    PluginState.PAUSED -> PluginStateProto.PLUGIN_STATE_PAUSED
    PluginState.RESUMING -> PluginStateProto.PLUGIN_STATE_ACTIVE
    PluginState.ERROR -> PluginStateProto.PLUGIN_STATE_ERROR
    PluginState.STOPPING -> PluginStateProto.PLUGIN_STATE_STOPPING
    PluginState.STOPPED -> PluginStateProto.PLUGIN_STATE_STOPPED
    PluginState.FAILED -> PluginStateProto.PLUGIN_STATE_ERROR
}

/**
 * Extension to convert PluginCapability to proto PluginCapabilityProto.
 */
private fun PluginCapability.toProtoCapability(): PluginCapabilityProto = PluginCapabilityProto(
    id = this.id,
    name = this.name,
    version = this.version,
    interfaces = this.interfaces.toList(),
    metadata = this.metadata
)

/**
 * Delegate interface for PluginService operations.
 * Implement this to customize plugin service behavior.
 */
interface PluginServiceDelegate {
    suspend fun onPluginRegistered(pluginId: String, registration: PluginRegistration)
    suspend fun onPluginUnregistered(pluginId: String)
    suspend fun onLifecycleCommand(pluginId: String, action: LifecycleAction): Boolean
    suspend fun validatePlugin(request: RegisterPluginRequest): Boolean
}

/**
 * Stub implementation of PluginServiceGrpc.PluginServiceImplBase for compilation.
 * This will be replaced by generated code from plugin.proto.
 */
abstract class PluginServiceGrpc {
    abstract class PluginServiceImplBase {
        open fun registerPlugin(
            request: RegisterPluginRequest,
            responseObserver: StreamObserver<RegisterPluginResponse>
        ) {}

        open fun unregisterPlugin(
            request: UnregisterPluginRequest,
            responseObserver: StreamObserver<UnregisterPluginResponse>
        ) {}

        open fun discoverPlugins(
            request: DiscoverPluginsRequest,
            responseObserver: StreamObserver<DiscoverPluginsResponse>
        ) {}

        open fun getPluginInfo(
            request: GetPluginInfoRequest,
            responseObserver: StreamObserver<PluginInfo>
        ) {}

        open fun sendLifecycleCommand(
            request: LifecycleCommand,
            responseObserver: StreamObserver<LifecycleResponse>
        ) {}

        open fun subscribeEvents(
            request: SubscribeEventsRequest,
            responseObserver: StreamObserver<PluginEventProto>
        ) {}

        open fun publishEvent(
            request: PluginEventProto,
            responseObserver: StreamObserver<PublishEventResponse>
        ) {}

        open fun healthCheck(
            request: HealthCheckRequest,
            responseObserver: StreamObserver<HealthCheckResponse>
        ) {}
    }
}
