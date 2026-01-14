/**
 * ServiceRegistry.kt - Service discovery and registration
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * Manages service registration and discovery for cross-device communication.
 */
package com.augmentalis.universalrpc

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service endpoint information
 */
data class ServiceEndpoint(
    val serviceName: String,
    val host: String,
    val port: Int,
    val protocol: String = "grpc",    // "grpc", "grpc-web", "uds"
    val metadata: Map<String, String> = emptyMap()
) {
    val address: String
        get() = when (protocol) {
            "uds" -> host  // Unix domain socket path
            else -> "$host:$port"
        }
}

/**
 * Service registry for discovering and registering services
 */
class ServiceRegistry {
    private val _services = MutableStateFlow<Map<String, ServiceEndpoint>>(emptyMap())
    val services: Flow<Map<String, ServiceEndpoint>> = _services.asStateFlow()

    private val _localServices = mutableMapOf<String, IRpcService>()

    /**
     * Register a local service
     */
    fun registerLocal(service: IRpcService, endpoint: ServiceEndpoint) {
        _localServices[service.serviceName] = service
        val current = _services.value.toMutableMap()
        current[service.serviceName] = endpoint
        _services.value = current
    }

    /**
     * Register a remote service endpoint
     */
    fun registerRemote(endpoint: ServiceEndpoint) {
        val current = _services.value.toMutableMap()
        current[endpoint.serviceName] = endpoint
        _services.value = current
    }

    /**
     * Unregister a service
     */
    fun unregister(serviceName: String) {
        _localServices.remove(serviceName)
        val current = _services.value.toMutableMap()
        current.remove(serviceName)
        _services.value = current
    }

    /**
     * Get endpoint for a service
     */
    fun getEndpoint(serviceName: String): ServiceEndpoint? {
        return _services.value[serviceName]
    }

    /**
     * Get local service instance
     */
    fun getLocalService(serviceName: String): IRpcService? {
        return _localServices[serviceName]
    }

    /**
     * Check if service is available
     */
    fun isAvailable(serviceName: String): Boolean {
        return _services.value.containsKey(serviceName)
    }

    /**
     * Get all registered services
     */
    fun getAllServices(): List<ServiceEndpoint> {
        return _services.value.values.toList()
    }

    /**
     * Clear all registrations
     */
    fun clear() {
        _localServices.clear()
        _services.value = emptyMap()
    }

    companion object {
        // Well-known service names
        const val SERVICE_VOICEOS = "com.augmentalis.voiceos"
        const val SERVICE_VOICE_CURSOR = "com.augmentalis.voicecursor"
        const val SERVICE_VOICE_RECOGNITION = "com.augmentalis.voicerecognition"
        const val SERVICE_AVID_CREATOR = "com.augmentalis.avidcreator"
        @Deprecated("Use SERVICE_AVID_CREATOR", ReplaceWith("SERVICE_AVID_CREATOR"))
        const val SERVICE_VUID_CREATOR = SERVICE_AVID_CREATOR
        const val SERVICE_EXPLORATION = "com.augmentalis.exploration"
        const val SERVICE_AVA = "com.augmentalis.ava"
        const val SERVICE_COCKPIT = "com.augmentalis.cockpit"
        const val SERVICE_NLU = "com.augmentalis.nlu"
        const val SERVICE_WEBAVANUE = "com.augmentalis.webavanue"

        // Default ports
        const val DEFAULT_PORT_VOICEOS = 50051
        const val DEFAULT_PORT_AVA = 50052
        const val DEFAULT_PORT_COCKPIT = 50053
        const val DEFAULT_PORT_NLU = 50054
        const val DEFAULT_PORT_WEBAVANUE = 50055
    }
}
