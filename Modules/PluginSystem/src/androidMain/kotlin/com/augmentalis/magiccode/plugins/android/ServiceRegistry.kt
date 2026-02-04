/**
 * ServiceRegistry.kt - Platform service registry for Android plugins
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides dependency injection for Android platform services that plugins can access.
 * This registry manages platform-specific services like AccessibilityService, repositories,
 * and other Android-specific dependencies.
 */
package com.augmentalis.magiccode.plugins.android

import android.accessibilityservice.AccessibilityService
import com.augmentalis.magiccode.plugins.core.PluginLog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Registry for platform services that plugins can access.
 *
 * Provides dependency injection for Android services, allowing plugins to access
 * platform-specific functionality without tight coupling. Services are registered
 * with string identifiers and can be retrieved by type.
 *
 * ## Thread Safety
 * All operations are thread-safe using Mutex for synchronization.
 *
 * ## Service Lifecycle
 * Services should be registered when available and unregistered when no longer valid.
 * For example, AccessibilityService should be registered in onServiceConnected() and
 * unregistered in onDestroy().
 *
 * ## Usage Example
 * ```kotlin
 * val registry = ServiceRegistry()
 *
 * // Register services
 * registry.register(ServiceRegistry.ACCESSIBILITY_SERVICE, accessibilityService)
 * registry.register(ServiceRegistry.ELEMENT_REPOSITORY, elementRepository)
 *
 * // Retrieve services
 * val service: AccessibilityService? = registry.get(ServiceRegistry.ACCESSIBILITY_SERVICE)
 * val repo = registry.getOrThrow<ElementRepository>(ServiceRegistry.ELEMENT_REPOSITORY)
 *
 * // Check availability
 * if (registry.hasService(ServiceRegistry.ACCESSIBILITY_SERVICE)) {
 *     // Safe to use accessibility features
 * }
 * ```
 *
 * @since 1.0.0
 * @see AndroidPluginHost
 * @see AndroidPluginContext
 */
class ServiceRegistry {

    /**
     * Internal storage for registered services.
     * Key: Service ID string, Value: Service instance
     */
    private val services = mutableMapOf<String, Any>()

    /**
     * Mutex for thread-safe access to the services map.
     */
    private val mutex = Mutex()

    /**
     * Listeners for service registration/unregistration events.
     */
    private val listeners = mutableListOf<ServiceRegistryListener>()

    /**
     * Register a service with the registry.
     *
     * If a service with the same ID already exists, it will be replaced
     * and a warning will be logged.
     *
     * @param T The service type
     * @param serviceId Unique identifier for the service (use companion object constants)
     * @param service The service instance to register
     */
    suspend fun <T : Any> register(serviceId: String, service: T) {
        mutex.withLock {
            val existing = services[serviceId]
            if (existing != null) {
                PluginLog.w(TAG, "Replacing existing service: $serviceId")
            }
            services[serviceId] = service
            PluginLog.d(TAG, "Registered service: $serviceId (${service::class.simpleName})")
        }
        notifyServiceRegistered(serviceId)
    }

    /**
     * Register a service with the registry (non-suspending version).
     *
     * This is a convenience method for contexts where coroutines are not available.
     * Uses synchronization instead of Mutex.
     *
     * @param T The service type
     * @param serviceId Unique identifier for the service
     * @param service The service instance to register
     */
    @Synchronized
    fun <T : Any> registerSync(serviceId: String, service: T) {
        val existing = services[serviceId]
        if (existing != null) {
            PluginLog.w(TAG, "Replacing existing service: $serviceId")
        }
        services[serviceId] = service
        PluginLog.d(TAG, "Registered service: $serviceId (${service::class.simpleName})")
        // Note: Listeners not notified in sync version to avoid deadlocks
    }

    /**
     * Retrieve a service from the registry.
     *
     * Returns null if the service is not registered or if the type doesn't match.
     *
     * @param T The expected service type
     * @param serviceId The service identifier
     * @return The service instance or null if not found
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> get(serviceId: String): T? {
        return mutex.withLock {
            try {
                services[serviceId] as? T
            } catch (e: ClassCastException) {
                PluginLog.e(TAG, "Type mismatch for service: $serviceId", e)
                null
            }
        }
    }

    /**
     * Retrieve a service from the registry (non-suspending version).
     *
     * @param T The expected service type
     * @param serviceId The service identifier
     * @return The service instance or null if not found
     */
    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun <T : Any> getSync(serviceId: String): T? {
        return try {
            services[serviceId] as? T
        } catch (e: ClassCastException) {
            PluginLog.e(TAG, "Type mismatch for service: $serviceId", e)
            null
        }
    }

    /**
     * Retrieve a service or throw an exception if not found.
     *
     * Use this method when the service is required for operation.
     *
     * @param T The expected service type
     * @param serviceId The service identifier
     * @return The service instance
     * @throws IllegalStateException if the service is not registered
     * @throws ClassCastException if the service type doesn't match
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> getOrThrow(serviceId: String): T {
        return mutex.withLock {
            val service = services[serviceId]
                ?: throw IllegalStateException("Service not registered: $serviceId")
            service as T
        }
    }

    /**
     * Retrieve a service or throw an exception (non-suspending version).
     *
     * @param T The expected service type
     * @param serviceId The service identifier
     * @return The service instance
     * @throws IllegalStateException if the service is not registered
     */
    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun <T : Any> getOrThrowSync(serviceId: String): T {
        val service = services[serviceId]
            ?: throw IllegalStateException("Service not registered: $serviceId")
        return service as T
    }

    /**
     * Unregister a service from the registry.
     *
     * @param serviceId The service identifier to unregister
     * @return true if the service was unregistered, false if it wasn't registered
     */
    suspend fun unregister(serviceId: String): Boolean {
        val removed = mutex.withLock {
            val existed = services.containsKey(serviceId)
            if (existed) {
                services.remove(serviceId)
                PluginLog.d(TAG, "Unregistered service: $serviceId")
            }
            existed
        }
        if (removed) {
            notifyServiceUnregistered(serviceId)
        }
        return removed
    }

    /**
     * Unregister a service (non-suspending version).
     *
     * @param serviceId The service identifier to unregister
     * @return true if the service was unregistered
     */
    @Synchronized
    fun unregisterSync(serviceId: String): Boolean {
        val existed = services.containsKey(serviceId)
        if (existed) {
            services.remove(serviceId)
            PluginLog.d(TAG, "Unregistered service: $serviceId")
        }
        return existed
    }

    /**
     * Check if a service is registered.
     *
     * @param serviceId The service identifier
     * @return true if the service is registered
     */
    suspend fun hasService(serviceId: String): Boolean {
        return mutex.withLock {
            services.containsKey(serviceId)
        }
    }

    /**
     * Check if a service is registered (non-suspending version).
     *
     * @param serviceId The service identifier
     * @return true if the service is registered
     */
    @Synchronized
    fun hasServiceSync(serviceId: String): Boolean {
        return services.containsKey(serviceId)
    }

    /**
     * Get all registered service IDs.
     *
     * @return Set of all registered service identifiers
     */
    suspend fun getRegisteredServiceIds(): Set<String> {
        return mutex.withLock {
            services.keys.toSet()
        }
    }

    /**
     * Get the count of registered services.
     *
     * @return Number of registered services
     */
    suspend fun getServiceCount(): Int {
        return mutex.withLock {
            services.size
        }
    }

    /**
     * Clear all registered services.
     *
     * Use with caution - this will remove all services from the registry.
     * Primarily useful for testing or complete shutdown scenarios.
     */
    suspend fun clear() {
        val removedIds = mutex.withLock {
            val ids = services.keys.toSet()
            services.clear()
            PluginLog.i(TAG, "Cleared all services (${ids.size} removed)")
            ids
        }
        removedIds.forEach { notifyServiceUnregistered(it) }
    }

    /**
     * Add a listener for service registration events.
     *
     * @param listener The listener to add
     */
    fun addListener(listener: ServiceRegistryListener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    /**
     * Remove a service registration listener.
     *
     * @param listener The listener to remove
     */
    fun removeListener(listener: ServiceRegistryListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    /**
     * Notify listeners of service registration.
     */
    private fun notifyServiceRegistered(serviceId: String) {
        val currentListeners = synchronized(listeners) { listeners.toList() }
        currentListeners.forEach { it.onServiceRegistered(serviceId) }
    }

    /**
     * Notify listeners of service unregistration.
     */
    private fun notifyServiceUnregistered(serviceId: String) {
        val currentListeners = synchronized(listeners) { listeners.toList() }
        currentListeners.forEach { it.onServiceUnregistered(serviceId) }
    }

    /**
     * Get a snapshot of all registered services.
     *
     * Returns a copy of the internal service map for debugging purposes.
     *
     * @return Map of service IDs to their class names
     */
    suspend fun getServiceSnapshot(): Map<String, String> {
        return mutex.withLock {
            services.mapValues { (_, service) ->
                service::class.qualifiedName ?: service::class.simpleName ?: "Unknown"
            }
        }
    }

    companion object {
        private const val TAG = "ServiceRegistry"

        // =========================================================================
        // Well-Known Service IDs
        // =========================================================================

        /**
         * Android AccessibilityService instance.
         * Type: android.accessibilityservice.AccessibilityService
         */
        const val ACCESSIBILITY_SERVICE = "accessibility_service"

        /**
         * Element repository for UI element data.
         * Type: IElementRepository (or platform-specific implementation)
         */
        const val ELEMENT_REPOSITORY = "element_repository"

        /**
         * Command repository for voice commands.
         * Type: ICommandRepository (or platform-specific implementation)
         */
        const val COMMAND_REPOSITORY = "command_repository"

        /**
         * Preference repository for user settings.
         * Type: IPreferenceRepository (or SharedPreferences wrapper)
         */
        const val PREFERENCE_REPOSITORY = "preference_repository"

        /**
         * Scraped app repository for learned app data.
         * Type: IScrapedAppRepository
         */
        const val SCRAPED_APP_REPOSITORY = "scraped_app_repository"

        /**
         * VoiceOS Core instance for voice command processing.
         * Type: VoiceOSCore
         */
        const val VOICEOS_CORE = "voiceos_core"

        /**
         * Speech recognition engine.
         * Type: SpeechRecognitionPlugin or similar
         */
        const val SPEECH_RECOGNITION = "speech_recognition"

        /**
         * Text-to-speech engine.
         * Type: TTSPlugin or similar
         */
        const val TEXT_TO_SPEECH = "text_to_speech"

        /**
         * NLU (Natural Language Understanding) service.
         * Type: NLUPlugin or NLUServiceClient
         */
        const val NLU_SERVICE = "nlu_service"

        /**
         * Overlay manager for UI overlays.
         * Type: OverlayStateManager or similar
         */
        const val OVERLAY_MANAGER = "overlay_manager"

        /**
         * Theme provider for UI theming.
         * Type: ThemeManager or ThemeProviderPlugin
         */
        const val THEME_PROVIDER = "theme_provider"

        /**
         * Database instance.
         * Type: Platform-specific database (Room, SQLDelight, etc.)
         */
        const val DATABASE = "database"

        /**
         * UniversalRPC ServiceRegistry for gRPC services.
         * Type: com.augmentalis.rpc.ServiceRegistry
         */
        const val RPC_SERVICE_REGISTRY = "rpc_service_registry"

        /**
         * Plugin event bus for inter-plugin communication.
         * Type: PluginEventBus
         */
        const val PLUGIN_EVENT_BUS = "plugin_event_bus"

        /**
         * Universal plugin registry.
         * Type: UniversalPluginRegistry
         */
        const val PLUGIN_REGISTRY = "plugin_registry"

        /**
         * Create a pre-configured ServiceRegistry with common default registrations.
         *
         * @param accessibilityService Optional AccessibilityService to register
         * @return Configured ServiceRegistry instance
         */
        fun createWithDefaults(accessibilityService: AccessibilityService? = null): ServiceRegistry {
            return ServiceRegistry().also { registry ->
                accessibilityService?.let {
                    registry.registerSync(ACCESSIBILITY_SERVICE, it)
                }
            }
        }
    }
}

/**
 * Listener interface for service registration events.
 *
 * Implement this interface to be notified when services are registered
 * or unregistered from the ServiceRegistry.
 */
interface ServiceRegistryListener {
    /**
     * Called when a service is registered.
     *
     * @param serviceId The ID of the registered service
     */
    fun onServiceRegistered(serviceId: String)

    /**
     * Called when a service is unregistered.
     *
     * @param serviceId The ID of the unregistered service
     */
    fun onServiceUnregistered(serviceId: String)
}

/**
 * Extension function to register multiple services at once.
 *
 * @param services Map of service IDs to service instances
 */
suspend fun ServiceRegistry.registerAll(services: Map<String, Any>) {
    services.forEach { (id, service) ->
        register(id, service)
    }
}

/**
 * Extension function to check if all required services are available.
 *
 * @param serviceIds Service IDs to check
 * @return true if all services are registered
 */
suspend fun ServiceRegistry.hasAllServices(vararg serviceIds: String): Boolean {
    return serviceIds.all { hasService(it) }
}

/**
 * Extension function to get the AccessibilityService if available.
 *
 * Convenience method for the common use case of accessing the accessibility service.
 *
 * @return AccessibilityService or null if not registered
 */
suspend fun ServiceRegistry.getAccessibilityService(): AccessibilityService? {
    return get(ServiceRegistry.ACCESSIBILITY_SERVICE)
}
