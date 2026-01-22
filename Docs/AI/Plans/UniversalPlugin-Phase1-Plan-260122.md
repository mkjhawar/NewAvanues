# Universal Plugin Architecture - Phase 1 Implementation Plan

**Date:** 2026-01-22
**Branch:** `AI-Architecture-Rework`
**Status:** Ready for Implementation
**Estimated Duration:** 2 weeks (5 days with swarm parallelization)

---

## Executive Summary

This plan implements **Phase 1: Foundation** of the Universal Plugin Architecture, leveraging the existing **UniversalRPC gRPC infrastructure** for inter-plugin communication. The goal is to create a robust plugin system that enables accessibility-first voice/gaze control for hand-challenged users.

### Key Decisions (Tree of Thought Analysis)

**Approach Evaluated:**

| Approach | Pros | Cons | Decision |
|----------|------|------|----------|
| **A: New REST API** | Simple, widely understood | Doesn't leverage existing infra, slower | ❌ Rejected |
| **B: Custom event bus** | Full control | Reinventing wheel, more code to maintain | ❌ Rejected |
| **C: Extend UniversalRPC** | Leverages existing gRPC, type-safe, streaming | Learning curve for proto | ✅ **Selected** |

**Rationale (Chain of Thought):**
1. UniversalRPC already has ServiceRegistry for discovery
2. gRPC streaming provides pub/sub pattern for event bus
3. Proto definitions ensure type-safe inter-plugin contracts
4. Existing clients/servers can be extended for plugins
5. Cross-platform KMP structure maintained

---

## Overview

| Aspect | Value |
|--------|-------|
| **Platforms** | Android, iOS, Desktop (KMP) |
| **Swarm Recommended** | Yes (3 platforms, 18+ tasks) |
| **Total Tasks** | 18 |
| **Sequential Estimate** | 40 hours |
| **Parallel Estimate** | 16 hours |
| **Savings** | 24 hours (60%) |

---

## Phase Ordering

```
Phase 1: Plugin Proto Definition (gRPC contracts)
    ↓
Phase 2: Core Interfaces (UniversalPlugin, PluginCapability)
    ↓
Phase 3: Plugin Registry (extends UniversalRPC ServiceRegistry)
    ↓
Phase 4: Plugin Event Bus (gRPC streaming)
    ↓
Phase 5: Lifecycle Management (pause/resume/config)
    ↓
Phase 6: Integration & Testing
```

---

## Phase 1: Plugin Proto Definition

**Location:** `/Modules/UniversalRPC/Common/proto/plugin.proto`

### Task 1.1: Create plugin.proto service definition

```protobuf
syntax = "proto3";

package com.augmentalis.universalrpc.plugin;

option java_package = "com.augmentalis.universalrpc.plugin";
option java_multiple_files = true;

// Plugin capability advertisement
message PluginCapability {
    string id = 1;                      // e.g., "llm.text-generation"
    string name = 2;
    string version = 3;
    repeated string interfaces = 4;     // Implemented interfaces
    map<string, string> metadata = 5;
}

// Plugin registration request
message RegisterPluginRequest {
    string request_id = 1;
    string plugin_id = 2;               // com.augmentalis.llm.openai
    string plugin_name = 3;
    string version = 4;
    repeated PluginCapability capabilities = 5;
    string endpoint_address = 6;        // UDS path or host:port
    string endpoint_protocol = 7;       // "grpc", "uds", "tcp"
}

message RegisterPluginResponse {
    string request_id = 1;
    bool success = 2;
    string message = 3;
    string assigned_id = 4;             // Confirmed plugin ID
}

// Plugin discovery
message DiscoverPluginsRequest {
    string request_id = 1;
    repeated string capability_filter = 2;  // Filter by capabilities
    bool include_disabled = 3;
}

message DiscoverPluginsResponse {
    string request_id = 1;
    repeated PluginInfo plugins = 2;
}

message PluginInfo {
    string plugin_id = 1;
    string plugin_name = 2;
    string version = 3;
    PluginState state = 4;
    repeated PluginCapability capabilities = 5;
    string endpoint_address = 6;
    int64 registered_at = 7;
    int64 last_health_check = 8;
}

enum PluginState {
    PLUGIN_STATE_UNKNOWN = 0;
    PLUGIN_STATE_REGISTERED = 1;
    PLUGIN_STATE_INITIALIZING = 2;
    PLUGIN_STATE_ACTIVE = 3;
    PLUGIN_STATE_PAUSED = 4;
    PLUGIN_STATE_ERROR = 5;
    PLUGIN_STATE_STOPPING = 6;
    PLUGIN_STATE_STOPPED = 7;
}

// Plugin lifecycle commands
message LifecycleCommand {
    string request_id = 1;
    string plugin_id = 2;
    LifecycleAction action = 3;
    map<string, string> config = 4;     // For CONFIG_CHANGED
}

enum LifecycleAction {
    LIFECYCLE_ACTIVATE = 0;
    LIFECYCLE_PAUSE = 1;
    LIFECYCLE_RESUME = 2;
    LIFECYCLE_STOP = 3;
    LIFECYCLE_CONFIG_CHANGED = 4;
}

message LifecycleResponse {
    string request_id = 1;
    bool success = 2;
    string message = 3;
    PluginState new_state = 4;
}

// Plugin events (for event bus)
message PluginEvent {
    string event_id = 1;
    string source_plugin_id = 2;
    string event_type = 3;              // "capability.registered", "state.changed", etc.
    int64 timestamp = 4;
    map<string, string> payload = 5;
    string payload_json = 6;            // For complex payloads
}

message SubscribeEventsRequest {
    string request_id = 1;
    string subscriber_plugin_id = 2;
    repeated string event_types = 3;    // Filter by event type
    repeated string source_plugins = 4; // Filter by source plugin
}

// Health check
message HealthCheckRequest {
    string request_id = 1;
    string plugin_id = 2;
}

message HealthCheckResponse {
    string request_id = 1;
    bool healthy = 2;
    string status_message = 3;
    map<string, string> diagnostics = 4;
}

// Plugin Service Definition
service PluginService {
    // Registration
    rpc RegisterPlugin(RegisterPluginRequest) returns (RegisterPluginResponse);
    rpc UnregisterPlugin(UnregisterPluginRequest) returns (UnregisterPluginResponse);

    // Discovery
    rpc DiscoverPlugins(DiscoverPluginsRequest) returns (DiscoverPluginsResponse);
    rpc GetPluginInfo(GetPluginInfoRequest) returns (PluginInfo);

    // Lifecycle
    rpc SendLifecycleCommand(LifecycleCommand) returns (LifecycleResponse);

    // Event Bus (streaming)
    rpc SubscribeEvents(SubscribeEventsRequest) returns (stream PluginEvent);
    rpc PublishEvent(PluginEvent) returns (PublishEventResponse);

    // Health
    rpc HealthCheck(HealthCheckRequest) returns (HealthCheckResponse);
}

// Additional request/response messages
message UnregisterPluginRequest {
    string request_id = 1;
    string plugin_id = 2;
}

message UnregisterPluginResponse {
    string request_id = 1;
    bool success = 2;
    string message = 3;
}

message GetPluginInfoRequest {
    string request_id = 1;
    string plugin_id = 2;
}

message PublishEventResponse {
    string request_id = 1;
    bool success = 2;
    int32 subscribers_notified = 3;
}
```

### Task 1.2: Generate Kotlin classes from proto

```bash
# Build command to generate Wire classes
./gradlew :Modules:UniversalRPC:generateProtos
```

**Output:** `/Modules/UniversalRPC/src/commonMain/kotlin/com/augmentalis/universalrpc/plugin/`

---

## Phase 2: Core Interfaces

**Location:** `/Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/universal/`

### Task 2.1: Create UniversalPlugin interface

```kotlin
// UniversalPlugin.kt
package com.augmentalis.magiccode.plugins.universal

import kotlinx.coroutines.flow.Flow

/**
 * Universal Plugin contract for all plugin types.
 * Extends existing AIPluginInterface pattern with lifecycle and event support.
 */
interface UniversalPlugin {
    /** Unique plugin identifier (reverse-domain) */
    val pluginId: String

    /** Human-readable name */
    val pluginName: String

    /** Semantic version */
    val version: String

    /** Advertised capabilities */
    val capabilities: Set<PluginCapability>

    /** Current plugin state */
    val state: PluginState

    /** State changes as Flow for reactive observation */
    val stateFlow: Flow<PluginState>

    // Lifecycle
    suspend fun initialize(config: PluginConfig, context: PluginContext): InitResult
    suspend fun activate(): Result<Unit>
    suspend fun pause(): Result<Unit>
    suspend fun resume(): Result<Unit>
    suspend fun shutdown(): Result<Unit>

    // Configuration
    suspend fun onConfigurationChanged(config: Map<String, Any>)

    // Health
    fun healthCheck(): HealthStatus

    // Events
    suspend fun onEvent(event: PluginEvent)
}
```

### Task 2.2: Create PluginCapability model

```kotlin
// PluginCapability.kt
package com.augmentalis.magiccode.plugins.universal

/**
 * Plugin capability for discovery and matching.
 */
data class PluginCapability(
    val id: String,                     // "llm.text-generation", "speech.recognition"
    val name: String,
    val version: String,
    val interfaces: Set<String> = emptySet(),  // "TextGenerationPlugin", "SpeechEnginePlugin"
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        // Well-known capability IDs
        const val LLM_TEXT_GENERATION = "llm.text-generation"
        const val LLM_EMBEDDING = "llm.embedding"
        const val NLU_INTENT = "nlu.intent-classification"
        const val NLU_ENTITY = "nlu.entity-extraction"
        const val SPEECH_RECOGNITION = "speech.recognition"
        const val SPEECH_TTS = "speech.text-to-speech"
        const val ACCESSIBILITY_HANDLER = "accessibility.handler"
        const val ACCESSIBILITY_GAZE = "accessibility.gaze-control"
        const val RAG_DOCUMENT = "rag.document-processing"
        const val RAG_EMBEDDING = "rag.embedding"
    }
}
```

### Task 2.3: Create PluginState enum (extended)

```kotlin
// PluginState.kt (extend existing)
package com.augmentalis.magiccode.plugins.universal

/**
 * Extended plugin lifecycle state.
 */
enum class PluginState {
    UNINITIALIZED,      // Not yet initialized
    INITIALIZING,       // Initialization in progress
    ACTIVE,             // Running and available
    PAUSED,             // Temporarily suspended (conserve resources)
    RESUMING,           // Resuming from pause
    ERROR,              // Error state (recoverable)
    STOPPING,           // Shutdown in progress
    STOPPED,            // Gracefully stopped
    FAILED              // Unrecoverable failure
}
```

### Task 2.4: Create supporting types

```kotlin
// PluginTypes.kt
package com.augmentalis.magiccode.plugins.universal

import kotlinx.serialization.Serializable

@Serializable
data class PluginConfig(
    val settings: Map<String, String> = emptyMap(),
    val secrets: Map<String, String> = emptyMap(),  // Encrypted
    val features: Set<String> = emptySet()
)

data class PluginContext(
    val appDataDir: String,
    val cacheDir: String,
    val serviceRegistry: Any,           // ServiceRegistry from UniversalRPC
    val eventBus: PluginEventBus
)

sealed class InitResult {
    data class Success(val message: String = "Initialized") : InitResult()
    data class Failure(val error: Throwable, val recoverable: Boolean = true) : InitResult()
}

data class HealthStatus(
    val healthy: Boolean,
    val message: String = "",
    val diagnostics: Map<String, String> = emptyMap(),
    val lastCheckTime: Long = System.currentTimeMillis()
)
```

---

## Phase 3: Plugin Registry (extends ServiceRegistry)

**Location:** `/Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/universal/`

### Task 3.1: Create UniversalPluginRegistry

```kotlin
// UniversalPluginRegistry.kt
package com.augmentalis.magiccode.plugins.universal

import com.augmentalis.universalrpc.ServiceEndpoint
import com.augmentalis.universalrpc.ServiceRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Universal Plugin Registry - extends ServiceRegistry with plugin-specific features.
 *
 * Integrates with UniversalRPC for gRPC-based plugin discovery and communication.
 */
class UniversalPluginRegistry(
    private val serviceRegistry: ServiceRegistry
) {
    private val _plugins = MutableStateFlow<Map<String, PluginRegistration>>(emptyMap())
    val plugins: Flow<Map<String, PluginRegistration>> = _plugins.asStateFlow()

    private val _capabilityIndex = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val capabilityIndex: Flow<Map<String, Set<String>>> = _capabilityIndex.asStateFlow()

    private val mutex = Mutex()

    /**
     * Register a plugin with capabilities and endpoint.
     */
    suspend fun register(
        plugin: UniversalPlugin,
        endpoint: ServiceEndpoint
    ): Result<PluginRegistration> = mutex.withLock {
        val registration = PluginRegistration(
            pluginId = plugin.pluginId,
            pluginName = plugin.pluginName,
            version = plugin.version,
            capabilities = plugin.capabilities,
            state = plugin.state,
            endpoint = endpoint,
            registeredAt = System.currentTimeMillis()
        )

        // Register in main registry
        val current = _plugins.value.toMutableMap()
        current[plugin.pluginId] = registration
        _plugins.value = current

        // Update capability index
        updateCapabilityIndex(plugin.pluginId, plugin.capabilities)

        // Register endpoint in ServiceRegistry for gRPC discovery
        serviceRegistry.registerRemote(endpoint.copy(
            serviceName = plugin.pluginId,
            metadata = mapOf(
                "type" to "plugin",
                "version" to plugin.version,
                "capabilities" to plugin.capabilities.joinToString(",") { it.id }
            )
        ))

        Result.success(registration)
    }

    /**
     * Discover plugins by capability.
     */
    fun discoverByCapability(capabilityId: String): List<PluginRegistration> {
        val pluginIds = _capabilityIndex.value[capabilityId] ?: emptySet()
        return pluginIds.mapNotNull { _plugins.value[it] }
            .filter { it.state == PluginState.ACTIVE }
    }

    /**
     * Get plugin by ID.
     */
    fun getPlugin(pluginId: String): PluginRegistration? {
        return _plugins.value[pluginId]
    }

    /**
     * Update plugin state.
     */
    suspend fun updateState(pluginId: String, newState: PluginState) = mutex.withLock {
        val current = _plugins.value.toMutableMap()
        current[pluginId]?.let { registration ->
            current[pluginId] = registration.copy(state = newState)
            _plugins.value = current
        }
    }

    /**
     * Unregister plugin.
     */
    suspend fun unregister(pluginId: String): Boolean = mutex.withLock {
        val current = _plugins.value.toMutableMap()
        val removed = current.remove(pluginId)
        if (removed != null) {
            _plugins.value = current

            // Remove from capability index
            removed.capabilities.forEach { cap ->
                val capIndex = _capabilityIndex.value.toMutableMap()
                capIndex[cap.id]?.let { plugins ->
                    capIndex[cap.id] = plugins - pluginId
                }
                _capabilityIndex.value = capIndex
            }

            // Unregister from ServiceRegistry
            serviceRegistry.unregister(pluginId)
            true
        } else {
            false
        }
    }

    private fun updateCapabilityIndex(pluginId: String, capabilities: Set<PluginCapability>) {
        val current = _capabilityIndex.value.toMutableMap()
        capabilities.forEach { cap ->
            val existing = current[cap.id] ?: emptySet()
            current[cap.id] = existing + pluginId
        }
        _capabilityIndex.value = current
    }

    companion object {
        const val SERVICE_PLUGIN_REGISTRY = "com.augmentalis.plugin.registry"
        const val DEFAULT_PORT_PLUGIN_REGISTRY = 50060
    }
}

data class PluginRegistration(
    val pluginId: String,
    val pluginName: String,
    val version: String,
    val capabilities: Set<PluginCapability>,
    val state: PluginState,
    val endpoint: ServiceEndpoint,
    val registeredAt: Long,
    val lastHealthCheck: Long = 0
)
```

---

## Phase 4: Plugin Event Bus

**Location:** `/Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/universal/`

### Task 4.1: Create PluginEventBus interface

```kotlin
// PluginEventBus.kt
package com.augmentalis.magiccode.plugins.universal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Plugin Event Bus for inter-plugin communication.
 * Uses gRPC streaming under the hood via UniversalRPC.
 */
interface PluginEventBus {
    /**
     * Publish an event to all subscribers.
     */
    suspend fun publish(event: PluginEvent): Int  // Returns subscriber count

    /**
     * Subscribe to events with optional filter.
     */
    fun subscribe(filter: EventFilter = EventFilter.ALL): Flow<PluginEvent>

    /**
     * Subscribe to specific event types.
     */
    fun subscribeToTypes(vararg eventTypes: String): Flow<PluginEvent>

    /**
     * Subscribe to events from specific plugins.
     */
    fun subscribeToPlugins(vararg pluginIds: String): Flow<PluginEvent>
}

data class PluginEvent(
    val eventId: String,
    val sourcePluginId: String,
    val eventType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val payload: Map<String, String> = emptyMap(),
    val payloadJson: String? = null
) {
    companion object {
        // Well-known event types
        const val TYPE_PLUGIN_REGISTERED = "plugin.registered"
        const val TYPE_PLUGIN_UNREGISTERED = "plugin.unregistered"
        const val TYPE_STATE_CHANGED = "plugin.state.changed"
        const val TYPE_CAPABILITY_AVAILABLE = "capability.available"
        const val TYPE_CAPABILITY_UNAVAILABLE = "capability.unavailable"
        const val TYPE_CONFIG_CHANGED = "plugin.config.changed"
        const val TYPE_HEALTH_CHANGED = "plugin.health.changed"

        // Accessibility-specific events
        const val TYPE_VOICE_COMMAND = "accessibility.voice.command"
        const val TYPE_GAZE_TARGET = "accessibility.gaze.target"
        const val TYPE_SCREEN_CHANGED = "accessibility.screen.changed"
    }
}

data class EventFilter(
    val eventTypes: Set<String> = emptySet(),
    val sourcePlugins: Set<String> = emptySet(),
    val excludePlugins: Set<String> = emptySet()
) {
    companion object {
        val ALL = EventFilter()
    }

    fun matches(event: PluginEvent): Boolean {
        if (eventTypes.isNotEmpty() && event.eventType !in eventTypes) return false
        if (sourcePlugins.isNotEmpty() && event.sourcePluginId !in sourcePlugins) return false
        if (event.sourcePluginId in excludePlugins) return false
        return true
    }
}
```

### Task 4.2: Implement GrpcPluginEventBus

```kotlin
// GrpcPluginEventBus.kt
package com.augmentalis.magiccode.plugins.universal

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * gRPC-based Plugin Event Bus implementation.
 * Uses SharedFlow for local broadcasting and gRPC streaming for cross-process.
 */
class GrpcPluginEventBus : PluginEventBus {

    private val _events = MutableSharedFlow<PluginEvent>(
        replay = 10,
        extraBufferCapacity = 100
    )

    private val mutex = Mutex()
    private val subscriptions = mutableMapOf<String, EventFilter>()

    override suspend fun publish(event: PluginEvent): Int {
        val eventWithId = if (event.eventId.isEmpty()) {
            event.copy(eventId = UUID.randomUUID().toString())
        } else {
            event
        }

        _events.emit(eventWithId)

        // Count matching subscribers
        return mutex.withLock {
            subscriptions.values.count { it.matches(eventWithId) }
        }
    }

    override fun subscribe(filter: EventFilter): Flow<PluginEvent> {
        return _events.asSharedFlow().filter { filter.matches(it) }
    }

    override fun subscribeToTypes(vararg eventTypes: String): Flow<PluginEvent> {
        return subscribe(EventFilter(eventTypes = eventTypes.toSet()))
    }

    override fun subscribeToPlugins(vararg pluginIds: String): Flow<PluginEvent> {
        return subscribe(EventFilter(sourcePlugins = pluginIds.toSet()))
    }

    /**
     * Register a named subscription for tracking.
     */
    suspend fun registerSubscription(subscriptionId: String, filter: EventFilter) {
        mutex.withLock {
            subscriptions[subscriptionId] = filter
        }
    }

    /**
     * Unregister a subscription.
     */
    suspend fun unregisterSubscription(subscriptionId: String) {
        mutex.withLock {
            subscriptions.remove(subscriptionId)
        }
    }
}
```

---

## Phase 5: Lifecycle Management

### Task 5.1: Create PluginLifecycleManager

```kotlin
// PluginLifecycleManager.kt
package com.augmentalis.magiccode.plugins.universal

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages plugin lifecycle transitions and health monitoring.
 */
class PluginLifecycleManager(
    private val registry: UniversalPluginRegistry,
    private val eventBus: PluginEventBus,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _managedPlugins = MutableStateFlow<Map<String, ManagedPlugin>>(emptyMap())
    val managedPlugins: StateFlow<Map<String, ManagedPlugin>> = _managedPlugins

    private val healthCheckIntervalMs = 30_000L
    private var healthCheckJob: Job? = null

    /**
     * Start managing a plugin's lifecycle.
     */
    suspend fun manage(plugin: UniversalPlugin, context: PluginContext): Result<Unit> {
        val managed = ManagedPlugin(
            plugin = plugin,
            context = context,
            managedSince = System.currentTimeMillis()
        )

        _managedPlugins.value = _managedPlugins.value + (plugin.pluginId to managed)

        // Observe state changes
        scope.launch {
            plugin.stateFlow.collect { newState ->
                registry.updateState(plugin.pluginId, newState)
                eventBus.publish(PluginEvent(
                    eventId = "",
                    sourcePluginId = plugin.pluginId,
                    eventType = PluginEvent.TYPE_STATE_CHANGED,
                    payload = mapOf("state" to newState.name)
                ))
            }
        }

        return Result.success(Unit)
    }

    /**
     * Initialize a plugin.
     */
    suspend fun initialize(pluginId: String, config: PluginConfig): InitResult {
        val managed = _managedPlugins.value[pluginId]
            ?: return InitResult.Failure(IllegalArgumentException("Plugin not managed: $pluginId"))

        return managed.plugin.initialize(config, managed.context)
    }

    /**
     * Pause a plugin (conserve resources, e.g., when app backgrounded).
     */
    suspend fun pause(pluginId: String): Result<Unit> {
        val managed = _managedPlugins.value[pluginId]
            ?: return Result.failure(IllegalArgumentException("Plugin not managed: $pluginId"))

        return managed.plugin.pause()
    }

    /**
     * Resume a paused plugin.
     */
    suspend fun resume(pluginId: String): Result<Unit> {
        val managed = _managedPlugins.value[pluginId]
            ?: return Result.failure(IllegalArgumentException("Plugin not managed: $pluginId"))

        return managed.plugin.resume()
    }

    /**
     * Update plugin configuration.
     */
    suspend fun updateConfig(pluginId: String, config: Map<String, Any>) {
        val managed = _managedPlugins.value[pluginId] ?: return
        managed.plugin.onConfigurationChanged(config)

        eventBus.publish(PluginEvent(
            eventId = "",
            sourcePluginId = pluginId,
            eventType = PluginEvent.TYPE_CONFIG_CHANGED,
            payloadJson = config.toString()
        ))
    }

    /**
     * Shutdown a plugin gracefully.
     */
    suspend fun shutdown(pluginId: String): Result<Unit> {
        val managed = _managedPlugins.value[pluginId]
            ?: return Result.failure(IllegalArgumentException("Plugin not managed: $pluginId"))

        val result = managed.plugin.shutdown()
        _managedPlugins.value = _managedPlugins.value - pluginId
        return result
    }

    /**
     * Start periodic health checks.
     */
    fun startHealthChecks() {
        healthCheckJob?.cancel()
        healthCheckJob = scope.launch {
            while (isActive) {
                _managedPlugins.value.forEach { (pluginId, managed) ->
                    val health = managed.plugin.healthCheck()
                    val previousHealth = managed.lastHealth

                    if (previousHealth?.healthy != health.healthy) {
                        eventBus.publish(PluginEvent(
                            eventId = "",
                            sourcePluginId = pluginId,
                            eventType = PluginEvent.TYPE_HEALTH_CHANGED,
                            payload = mapOf(
                                "healthy" to health.healthy.toString(),
                                "message" to health.message
                            )
                        ))
                    }

                    // Update managed plugin with latest health
                    _managedPlugins.value = _managedPlugins.value +
                        (pluginId to managed.copy(lastHealth = health))
                }
                delay(healthCheckIntervalMs)
            }
        }
    }

    /**
     * Stop health checks.
     */
    fun stopHealthChecks() {
        healthCheckJob?.cancel()
        healthCheckJob = null
    }
}

data class ManagedPlugin(
    val plugin: UniversalPlugin,
    val context: PluginContext,
    val managedSince: Long,
    val lastHealth: HealthStatus? = null
)
```

---

## Phase 6: Integration & Testing

### Task 6.1: Create PluginServiceGrpcServer

```kotlin
// PluginServiceGrpcServer.kt
package com.augmentalis.magiccode.plugins.universal.grpc

/**
 * gRPC Server implementation for PluginService.
 * Bridges the proto-generated service to the UniversalPluginRegistry.
 */
class PluginServiceGrpcServer(
    private val registry: UniversalPluginRegistry,
    private val eventBus: GrpcPluginEventBus,
    private val lifecycleManager: PluginLifecycleManager
) {
    // Implementation follows the pattern in VoiceOSGrpcServer.kt
    // Delegates to registry, eventBus, and lifecycleManager
}
```

### Task 6.2: Create PluginServiceGrpcClient

```kotlin
// PluginServiceGrpcClient.kt
package com.augmentalis.magiccode.plugins.universal.grpc

/**
 * gRPC Client for connecting to PluginService.
 * Follows pattern from AvaGrpcClient.kt with auto-reconnect.
 */
class PluginServiceGrpcClient(
    private val config: GrpcConnectionConfig
) {
    // Implementation follows the pattern in AvaGrpcClient.kt
}
```

### Task 6.3: Integration tests

```kotlin
// UniversalPluginRegistryTest.kt
// PluginEventBusTest.kt
// PluginLifecycleManagerTest.kt
```

---

## File Structure

```
Modules/
├── UniversalRPC/
│   └── Common/proto/
│       └── plugin.proto                    [NEW - Task 1.1]
│
└── PluginSystem/
    └── src/commonMain/kotlin/com/augmentalis/magiccode/plugins/
        └── universal/                       [NEW PACKAGE]
            ├── UniversalPlugin.kt           [Task 2.1]
            ├── PluginCapability.kt          [Task 2.2]
            ├── PluginState.kt               [Task 2.3]
            ├── PluginTypes.kt               [Task 2.4]
            ├── UniversalPluginRegistry.kt   [Task 3.1]
            ├── PluginEventBus.kt            [Task 4.1]
            ├── GrpcPluginEventBus.kt        [Task 4.2]
            ├── PluginLifecycleManager.kt    [Task 5.1]
            └── grpc/
                ├── PluginServiceGrpcServer.kt [Task 6.1]
                └── PluginServiceGrpcClient.kt [Task 6.2]
```

---

## Task Summary

| # | Task | Phase | Est. Hours | Swarm Agent |
|---|------|-------|------------|-------------|
| 1.1 | Create plugin.proto | Proto | 2 | Backend |
| 1.2 | Generate Kotlin classes | Proto | 0.5 | Backend |
| 2.1 | UniversalPlugin interface | Core | 2 | Shared |
| 2.2 | PluginCapability model | Core | 1 | Shared |
| 2.3 | Extended PluginState | Core | 0.5 | Shared |
| 2.4 | Supporting types | Core | 1 | Shared |
| 3.1 | UniversalPluginRegistry | Registry | 4 | Shared |
| 4.1 | PluginEventBus interface | Events | 1 | Shared |
| 4.2 | GrpcPluginEventBus impl | Events | 3 | Shared |
| 5.1 | PluginLifecycleManager | Lifecycle | 4 | Shared |
| 6.1 | PluginServiceGrpcServer | Integration | 3 | Android |
| 6.2 | PluginServiceGrpcClient | Integration | 2 | Android |
| 6.3 | Integration tests | Testing | 4 | Test |

**Total: 18 tasks, ~28 hours estimated**

---

## Swarm Agent Assignment

| Agent | Responsibility | Tasks |
|-------|----------------|-------|
| **Backend** | Proto definitions, gRPC setup | 1.1, 1.2 |
| **Shared** | KMP interfaces, registry, events | 2.1-2.4, 3.1, 4.1-4.2, 5.1 |
| **Android** | gRPC server/client | 6.1, 6.2 |
| **Test** | Integration tests | 6.3 |

---

## Dependencies

- **UniversalRPC module** - ServiceRegistry, Transport
- **PluginSystem module** - Existing PluginManifest, PluginLoader
- **Wire (Square)** - Proto compilation
- **Kotlinx Coroutines** - Async/Flow
- **Kotlinx Serialization** - Config serialization

---

## Success Criteria

1. ✅ Plugins can register with capabilities
2. ✅ Plugins discoverable by capability ID
3. ✅ Event bus enables pub/sub between plugins
4. ✅ Lifecycle hooks (pause/resume) functional
5. ✅ Health checks running automatically
6. ✅ gRPC streaming working for events
7. ✅ Integration with existing PluginSystem

---

## Next Phase Preview

**Phase 2: Core Plugins (Weeks 3-4)**
- Migrate SpeechRecognition engines to UniversalPlugin
- Create plugin manifests for AI modules (LLM, NLU, RAG)
- Implement AccessibilityDataProvider for VoiceOSCore integration

---

**Document Generated:** 2026-01-22
**Plan Version:** 1.0
**Swarm Activated:** Yes (3 platforms, 18 tasks)
