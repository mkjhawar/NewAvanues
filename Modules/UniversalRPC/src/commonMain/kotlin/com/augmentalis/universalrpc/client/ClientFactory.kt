/**
 * ClientFactory.kt - Factory for creating platform-specific RPC clients
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * Provides factory methods to create appropriate client instances
 * based on configuration and platform capabilities.
 */
package com.augmentalis.universalrpc.client

import com.augmentalis.universalrpc.ServiceEndpoint
import com.augmentalis.universalrpc.ServiceRegistry

/**
 * Factory for creating UniversalRPC clients.
 * Handles platform detection and optimal transport selection.
 */
object ClientFactory {

    /**
     * Create a client with the given configuration.
     * Uses the platform-specific implementation.
     */
    fun create(config: ClientConfig): UniversalClient {
        return PlatformClient(config)
    }

    /**
     * Create a client for a specific service endpoint.
     */
    fun create(endpoint: ServiceEndpoint): UniversalClient {
        val config = ClientConfig(
            host = endpoint.host,
            port = endpoint.port,
            protocol = protocolFromString(endpoint.protocol),
            metadata = endpoint.metadata
        )
        return create(config)
    }

    /**
     * Create a client that discovers services via registry.
     */
    fun createWithRegistry(
        registry: ServiceRegistry,
        serviceName: String
    ): RegistryAwareClient {
        return RegistryAwareClient(registry, serviceName)
    }

    /**
     * Create a client for VoiceOS service.
     */
    fun createForVoiceOS(
        host: String = "localhost",
        registry: ServiceRegistry? = null
    ): UniversalClient {
        return if (registry != null) {
            RegistryAwareClient(registry, ServiceRegistry.SERVICE_VOICEOS)
        } else {
            create(ClientConfig.forVoiceOS(host))
        }
    }

    /**
     * Create a client for AVA service.
     */
    fun createForAVA(
        host: String = "localhost",
        registry: ServiceRegistry? = null
    ): UniversalClient {
        return if (registry != null) {
            RegistryAwareClient(registry, ServiceRegistry.SERVICE_AVA)
        } else {
            create(ClientConfig.forAVA(host))
        }
    }

    /**
     * Create a client for Cockpit service.
     */
    fun createForCockpit(
        host: String = "localhost",
        registry: ServiceRegistry? = null
    ): UniversalClient {
        return if (registry != null) {
            RegistryAwareClient(registry, ServiceRegistry.SERVICE_COCKPIT)
        } else {
            create(ClientConfig.forCockpit(host))
        }
    }

    /**
     * Create a client for NLU service.
     */
    fun createForNLU(
        host: String = "localhost",
        registry: ServiceRegistry? = null
    ): UniversalClient {
        return if (registry != null) {
            RegistryAwareClient(registry, ServiceRegistry.SERVICE_NLU)
        } else {
            create(ClientConfig.forNLU(host))
        }
    }

    /**
     * Create a client for WebAvanue service.
     */
    fun createForWebAvanue(
        host: String = "localhost",
        registry: ServiceRegistry? = null
    ): UniversalClient {
        return if (registry != null) {
            RegistryAwareClient(registry, ServiceRegistry.SERVICE_WEBAVANUE)
        } else {
            create(ClientConfig.forWebAvanue(host))
        }
    }

    /**
     * Create a client for Unix Domain Socket connection.
     */
    fun createForUDS(socketPath: String): UniversalClient {
        return create(ClientConfig.forUDS(socketPath))
    }

    /**
     * Create a pool of clients for load balancing.
     */
    fun createPool(
        config: ClientConfig,
        poolSize: Int = 4
    ): ClientPool {
        return ClientPool(config, poolSize)
    }

    /**
     * Convert protocol string to Protocol enum.
     */
    private fun protocolFromString(protocol: String): ClientConfig.Protocol {
        return when (protocol.lowercase()) {
            "grpc" -> ClientConfig.Protocol.GRPC
            "uds" -> ClientConfig.Protocol.UDS
            "websocket", "ws", "wss" -> ClientConfig.Protocol.WEBSOCKET
            "http2", "http2_json" -> ClientConfig.Protocol.HTTP2_JSON
            else -> ClientConfig.Protocol.GRPC
        }
    }
}

/**
 * Pool of clients for connection reuse and load balancing.
 */
class ClientPool(
    private val config: ClientConfig,
    private val poolSize: Int = 4
) {
    private val clients = mutableListOf<UniversalClient>()
    private var currentIndex = 0

    /**
     * Initialize the pool by creating clients.
     */
    suspend fun initialize() {
        repeat(poolSize) {
            val client = ClientFactory.create(config)
            client.connect()
            clients.add(client)
        }
    }

    /**
     * Get the next available client (round-robin).
     * Note: Not thread-safe, use proper synchronization in production.
     */
    fun acquire(): UniversalClient {
        if (clients.isEmpty()) {
            throw IllegalStateException("Pool not initialized. Call initialize() first.")
        }
        val client = clients[currentIndex]
        currentIndex = (currentIndex + 1) % clients.size
        return client
    }

    /**
     * Execute an operation with a pooled client.
     */
    suspend fun <T> withClient(block: suspend (UniversalClient) -> T): T {
        return block(acquire())
    }

    /**
     * Close all clients in the pool.
     */
    suspend fun close() {
        clients.forEach { it.close() }
        clients.clear()
        currentIndex = 0
    }

    /**
     * Get the current pool size.
     */
    val size: Int
        get() = clients.size

    /**
     * Check if the pool is initialized.
     */
    val isInitialized: Boolean
        get() = clients.isNotEmpty()
}

/**
 * Expect declaration for platform-specific factory utilities.
 * Provides platform-optimal default configurations.
 */
expect object PlatformClientFactory {
    /**
     * Get the default protocol for this platform.
     */
    fun getDefaultProtocol(): ClientConfig.Protocol

    /**
     * Check if UDS (Unix Domain Socket) is supported on this platform.
     */
    fun isUDSSupported(): Boolean

    /**
     * Get platform-specific default configuration.
     */
    fun getDefaultConfig(): ClientConfig

    /**
     * Get the platform name for logging/debugging.
     */
    fun getPlatformName(): String
}
