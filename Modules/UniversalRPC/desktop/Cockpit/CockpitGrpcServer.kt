/**
 * CockpitGrpcServer.kt - Desktop gRPC server for Cockpit management
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * Main gRPC server implementation using grpc-netty-shaded for JVM.
 * Provides cross-device management, window coordination, and sync services.
 */
package com.augmentalis.universalrpc.desktop.cockpit

import com.augmentalis.universalrpc.service.IRpcService
import com.augmentalis.universalrpc.service.ServiceRegistry
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.health.v1.HealthCheckResponse
import io.grpc.protobuf.services.HealthStatusManager
import io.grpc.protobuf.services.ProtoReflectionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Configuration for the Cockpit gRPC server.
 */
data class CockpitServerConfig(
    val port: Int = ServiceRegistry.DEFAULT_PORT_COCKPIT,
    val enableReflection: Boolean = true,
    val enableHealthCheck: Boolean = true,
    val maxInboundMessageSize: Int = 4 * 1024 * 1024, // 4MB
    val maxInboundMetadataSize: Int = 8 * 1024, // 8KB
    val deviceHeartbeatIntervalMs: Long = 10_000L,
    val deviceTimeoutMs: Long = 30_000L,
    val shutdownTimeoutSeconds: Long = 30
)

/**
 * Server status for monitoring
 */
enum class ServerStatus {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING,
    FAILED
}

/**
 * Server status listener interface
 */
interface CockpitServerListener {
    fun onStatusChanged(status: ServerStatus)
    fun onDeviceConnected(deviceId: String)
    fun onDeviceDisconnected(deviceId: String)
    fun onError(error: Throwable)
}

/**
 * Desktop gRPC server for Cockpit cross-device management.
 *
 * Features:
 * - Device registration and tracking
 * - Cross-device communication
 * - Window management commands
 * - Layout preset management
 * - Sync functionality
 * - Health checks and reflection for debugging
 */
class CockpitGrpcServer(
    private val config: CockpitServerConfig = CockpitServerConfig()
) : IRpcService {

    override val serviceName: String = ServiceRegistry.SERVICE_COCKPIT
    override val version: String = "1.0.0"

    private val logger = Logger.getLogger(CockpitGrpcServer::class.java.name)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Core components
    val deviceRegistry = DeviceRegistry()
    private val serviceImpl = CockpitServiceImpl(deviceRegistry)

    // gRPC server
    private var server: Server? = null
    private var healthManager: HealthStatusManager? = null

    // Status tracking
    private var _status: ServerStatus = ServerStatus.STOPPED
    val status: ServerStatus get() = _status

    private val listeners = mutableListOf<CockpitServerListener>()
    private var heartbeatJob: Job? = null

    /**
     * Add a server status listener.
     */
    fun addListener(listener: CockpitServerListener) {
        listeners.add(listener)
    }

    /**
     * Remove a server status listener.
     */
    fun removeListener(listener: CockpitServerListener) {
        listeners.remove(listener)
    }

    /**
     * Start the gRPC server.
     *
     * @param interceptors Optional list of server interceptors
     * @return True if server started successfully
     */
    fun start(interceptors: List<ServerInterceptor> = emptyList()): Boolean {
        if (_status == ServerStatus.RUNNING) {
            logger.warning("Server already running on port ${config.port}")
            return true
        }

        return try {
            updateStatus(ServerStatus.STARTING)
            logger.info("Starting Cockpit gRPC server on port ${config.port}...")

            // Create health manager
            healthManager = if (config.enableHealthCheck) {
                HealthStatusManager().apply {
                    setStatus("", HealthCheckResponse.ServingStatus.NOT_SERVING)
                    setStatus(serviceName, HealthCheckResponse.ServingStatus.NOT_SERVING)
                }
            } else null

            // Build server
            val serverBuilder = ServerBuilder.forPort(config.port)
                .maxInboundMessageSize(config.maxInboundMessageSize)
                .maxInboundMetadataSize(config.maxInboundMetadataSize)

            // Add main service with interceptors
            if (interceptors.isNotEmpty()) {
                serverBuilder.addService(
                    ServerInterceptors.intercept(serviceImpl, interceptors)
                )
            } else {
                serverBuilder.addService(serviceImpl)
            }

            // Add health check service
            healthManager?.let { serverBuilder.addService(it.healthService) }

            // Add reflection service for debugging
            if (config.enableReflection) {
                serverBuilder.addService(ProtoReflectionService.newInstance())
            }

            server = serverBuilder.build().start()

            // Update health status to serving
            healthManager?.apply {
                setStatus("", HealthCheckResponse.ServingStatus.SERVING)
                setStatus(serviceName, HealthCheckResponse.ServingStatus.SERVING)
            }

            // Start device heartbeat monitoring
            startHeartbeatMonitoring()

            // Setup device event listeners
            setupDeviceEventListeners()

            updateStatus(ServerStatus.RUNNING)
            logger.info("Cockpit gRPC server started successfully on port ${config.port}")

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(Thread {
                logger.info("Shutting down Cockpit gRPC server due to JVM shutdown...")
                runBlocking { shutdown() }
            })

            true
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to start Cockpit gRPC server", e)
            updateStatus(ServerStatus.FAILED)
            notifyError(e)
            false
        }
    }

    /**
     * Block until server shuts down.
     */
    fun blockUntilShutdown() {
        server?.awaitTermination()
    }

    /**
     * Check if server is ready to accept requests.
     */
    override suspend fun isReady(): Boolean = _status == ServerStatus.RUNNING

    /**
     * Shutdown the server gracefully.
     */
    override suspend fun shutdown() {
        if (_status == ServerStatus.STOPPED || _status == ServerStatus.STOPPING) {
            return
        }

        updateStatus(ServerStatus.STOPPING)
        logger.info("Shutting down Cockpit gRPC server...")

        try {
            // Cancel heartbeat monitoring
            heartbeatJob?.cancel()
            heartbeatJob = null

            // Update health status
            healthManager?.apply {
                setStatus("", HealthCheckResponse.ServingStatus.NOT_SERVING)
                setStatus(serviceName, HealthCheckResponse.ServingStatus.NOT_SERVING)
            }

            // Shutdown service implementation
            serviceImpl.shutdown()

            // Clear device registry
            deviceRegistry.clear()

            // Shutdown gRPC server
            server?.let { srv ->
                srv.shutdown()
                if (!srv.awaitTermination(config.shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
                    logger.warning("Server did not terminate in time, forcing shutdown...")
                    srv.shutdownNow()
                    srv.awaitTermination(5, TimeUnit.SECONDS)
                }
            }

            server = null
            healthManager = null

            // Cancel coroutine scope
            scope.cancel()

            updateStatus(ServerStatus.STOPPED)
            logger.info("Cockpit gRPC server shutdown complete")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error during server shutdown", e)
            updateStatus(ServerStatus.FAILED)
            notifyError(e)
        }
    }

    /**
     * Get server statistics.
     */
    fun getStats(): ServerStats {
        val deviceStats = deviceRegistry.getDeviceStats()
        return ServerStats(
            status = _status,
            port = config.port,
            totalDevices = deviceStats.total,
            onlineDevices = deviceStats.online,
            devicesByPlatform = deviceStats.byPlatform,
            uptimeMs = if (_status == ServerStatus.RUNNING) {
                System.currentTimeMillis() - (startTime ?: System.currentTimeMillis())
            } else 0
        )
    }

    private var startTime: Long? = null

    // ========== Private Helpers ==========

    private fun startHeartbeatMonitoring() {
        heartbeatJob = scope.launch {
            startTime = System.currentTimeMillis()
            while (isActive) {
                delay(config.deviceHeartbeatIntervalMs)
                try {
                    val pruned = deviceRegistry.pruneStaleDevices(config.deviceTimeoutMs)
                    if (pruned > 0) {
                        logger.info("Pruned $pruned stale devices")
                    }
                } catch (e: Exception) {
                    logger.log(Level.WARNING, "Error during heartbeat monitoring", e)
                }
            }
        }
    }

    private fun setupDeviceEventListeners() {
        scope.launch {
            deviceRegistry.deviceEvents.collect { event ->
                when (event.eventType) {
                    DeviceEventType.CONNECTED -> {
                        logger.info("Device connected: ${event.deviceId}")
                        listeners.forEach { it.onDeviceConnected(event.deviceId) }
                    }
                    DeviceEventType.DISCONNECTED -> {
                        logger.info("Device disconnected: ${event.deviceId}")
                        listeners.forEach { it.onDeviceDisconnected(event.deviceId) }
                    }
                    else -> {
                        logger.fine("Device event: ${event.eventType} for ${event.deviceId}")
                    }
                }
            }
        }
    }

    private fun updateStatus(newStatus: ServerStatus) {
        val oldStatus = _status
        _status = newStatus
        if (oldStatus != newStatus) {
            listeners.forEach { it.onStatusChanged(newStatus) }
        }
    }

    private fun notifyError(error: Throwable) {
        listeners.forEach { it.onError(error) }
    }

    companion object {
        /**
         * Create and start a server with default configuration.
         */
        fun createAndStart(
            port: Int = ServiceRegistry.DEFAULT_PORT_COCKPIT,
            block: CockpitServerConfig.() -> Unit = {}
        ): CockpitGrpcServer {
            val config = CockpitServerConfig(port = port).apply(block)
            return CockpitGrpcServer(config).apply { start() }
        }

        /**
         * Main entry point for standalone server execution.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            val port = args.getOrNull(0)?.toIntOrNull() ?: ServiceRegistry.DEFAULT_PORT_COCKPIT

            val server = CockpitGrpcServer(CockpitServerConfig(port = port))

            server.addListener(object : CockpitServerListener {
                override fun onStatusChanged(status: ServerStatus) {
                    println("Server status: $status")
                }

                override fun onDeviceConnected(deviceId: String) {
                    println("Device connected: $deviceId")
                }

                override fun onDeviceDisconnected(deviceId: String) {
                    println("Device disconnected: $deviceId")
                }

                override fun onError(error: Throwable) {
                    System.err.println("Server error: ${error.message}")
                    error.printStackTrace()
                }
            })

            if (server.start()) {
                println("Cockpit gRPC Server running on port $port")
                println("Press Ctrl+C to stop")
                server.blockUntilShutdown()
            } else {
                System.err.println("Failed to start server")
                System.exit(1)
            }
        }
    }
}

/**
 * Server statistics summary.
 */
data class ServerStats(
    val status: ServerStatus,
    val port: Int,
    val totalDevices: Int,
    val onlineDevices: Int,
    val devicesByPlatform: Map<String, Int>,
    val uptimeMs: Long
) {
    val uptimeFormatted: String
        get() {
            val seconds = uptimeMs / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            return when {
                days > 0 -> "${days}d ${hours % 24}h ${minutes % 60}m"
                hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
                minutes > 0 -> "${minutes}m ${seconds % 60}s"
                else -> "${seconds}s"
            }
        }
}
