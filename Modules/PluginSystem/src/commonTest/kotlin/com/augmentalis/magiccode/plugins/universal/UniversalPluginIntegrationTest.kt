/*
 * UniversalPluginIntegrationTest.kt - Integration tests for Universal Plugin Architecture
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Tests for Phase 1 Foundation components:
 * - UniversalPluginRegistry
 * - PluginEventBus
 * - PluginLifecycleManager
 * - Plugin state management
 */

package com.augmentalis.magiccode.plugins.universal

import com.augmentalis.universalrpc.ServiceEndpoint
import com.augmentalis.universalrpc.ServiceRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for Universal Plugin Architecture Phase 1.
 */
class UniversalPluginIntegrationTest {

    // ========== Registry Tests ==========

    @Test
    fun `registry registers plugin successfully`() = runBlocking {
        val serviceRegistry = ServiceRegistry()
        val registry = UniversalPluginRegistry(serviceRegistry)
        val plugin = createTestPlugin("test.plugin.1")
        val endpoint = createTestEndpoint()

        val result = registry.register(plugin, endpoint)

        assertTrue(result.isSuccess)
        assertEquals("test.plugin.1", result.getOrNull()?.pluginId)
        assertTrue(registry.isRegistered("test.plugin.1"))
    }

    @Test
    fun `registry prevents duplicate registration`() = runBlocking {
        val serviceRegistry = ServiceRegistry()
        val registry = UniversalPluginRegistry(serviceRegistry)
        val plugin = createTestPlugin("test.plugin.dup")
        val endpoint = createTestEndpoint()

        registry.register(plugin, endpoint)
        val result = registry.register(plugin, endpoint)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("already registered") == true)
    }

    @Test
    fun `registry discovers plugins by capability`() = runBlocking {
        val serviceRegistry = ServiceRegistry()
        val registry = UniversalPluginRegistry(serviceRegistry)

        // Register plugins with different capabilities
        val llmPlugin = createTestPlugin("llm.plugin", setOf(PluginCapability.LLM_TEXT_GENERATION))
        val speechPlugin = createTestPlugin("speech.plugin", setOf(PluginCapability.SPEECH_RECOGNITION))
        val multiPlugin = createTestPlugin("multi.plugin", setOf(
            PluginCapability.LLM_TEXT_GENERATION,
            PluginCapability.NLU_INTENT
        ))

        registry.register(llmPlugin, createTestEndpoint("llm"))
        registry.register(speechPlugin, createTestEndpoint("speech"))
        registry.register(multiPlugin, createTestEndpoint("multi"))

        // Discover by LLM capability
        val llmPlugins = registry.discoverByCapability(PluginCapability.LLM_TEXT_GENERATION.id)
        assertEquals(2, llmPlugins.size)
        assertTrue(llmPlugins.any { it.pluginId == "llm.plugin" })
        assertTrue(llmPlugins.any { it.pluginId == "multi.plugin" })

        // Discover by speech capability
        val speechPlugins = registry.discoverByCapability(PluginCapability.SPEECH_RECOGNITION.id)
        assertEquals(1, speechPlugins.size)
        assertEquals("speech.plugin", speechPlugins[0].pluginId)
    }

    @Test
    fun `registry unregisters plugin correctly`() = runBlocking {
        val serviceRegistry = ServiceRegistry()
        val registry = UniversalPluginRegistry(serviceRegistry)
        val plugin = createTestPlugin("test.unregister")
        val endpoint = createTestEndpoint()

        registry.register(plugin, endpoint)
        assertTrue(registry.isRegistered("test.unregister"))

        val success = registry.unregister("test.unregister")
        assertTrue(success)
        assertFalse(registry.isRegistered("test.unregister"))
    }

    @Test
    fun `registry updates plugin state`() = runBlocking {
        val serviceRegistry = ServiceRegistry()
        val registry = UniversalPluginRegistry(serviceRegistry)
        val plugin = createTestPlugin("test.state")
        val endpoint = createTestEndpoint()

        registry.register(plugin, endpoint)

        val updated = registry.updateState("test.state", PluginState.PAUSED)
        assertTrue(updated)

        val registration = registry.getPlugin("test.state")
        assertNotNull(registration)
        assertEquals(PluginState.PAUSED, registration.state)
    }

    // ========== Event Bus Tests ==========

    @Test
    fun `event bus publishes and receives events`() = runBlocking {
        val eventBus = GrpcPluginEventBus()

        // Subscribe before publishing
        val receivedEvents = mutableListOf<PluginEvent>()
        val subscription = eventBus.subscribe(EventFilter.ALL)

        // Publish event
        val event = createPluginEvent(
            sourcePluginId = "test.source",
            eventType = "test.event"
        )
        eventBus.publish(event)

        // Collect event with timeout
        withTimeout(1000) {
            val received = subscription.first()
            receivedEvents.add(received)
        }

        assertEquals(1, receivedEvents.size)
        assertEquals("test.source", receivedEvents[0].sourcePluginId)
        assertEquals("test.event", receivedEvents[0].eventType)
    }

    @Test
    fun `event bus filters by event type`() = runBlocking {
        val eventBus = GrpcPluginEventBus()

        // Subscribe to specific event type
        val filter = EventFilter(eventTypes = setOf("wanted.event"))
        val subscription = eventBus.subscribeToTypes("wanted.event")

        // Publish multiple events
        eventBus.publish(createPluginEvent("source", "unwanted.event"))
        eventBus.publish(createPluginEvent("source", "wanted.event"))
        eventBus.publish(createPluginEvent("source", "another.unwanted"))

        // Should only receive the wanted event
        withTimeout(1000) {
            val received = subscription.first()
            assertEquals("wanted.event", received.eventType)
        }
    }

    @Test
    fun `event bus filters by source plugin`() = runBlocking {
        val eventBus = GrpcPluginEventBus()

        // Subscribe to specific source
        val subscription = eventBus.subscribeToPlugins("wanted.source")

        // Publish events from different sources
        eventBus.publish(createPluginEvent("unwanted.source", "event1"))
        eventBus.publish(createPluginEvent("wanted.source", "event2"))

        withTimeout(1000) {
            val received = subscription.first()
            assertEquals("wanted.source", received.sourcePluginId)
        }
    }

    // ========== Lifecycle Manager Tests ==========

    @Test
    fun `lifecycle manager initializes plugin`() = runBlocking {
        val serviceRegistry = ServiceRegistry()
        val registry = UniversalPluginRegistry(serviceRegistry)
        val eventBus = GrpcPluginEventBus()
        val lifecycleManager = PluginLifecycleManager(registry, eventBus)

        val plugin = createTestPlugin("test.lifecycle")
        val context = createTestContext()

        val result = lifecycleManager.manage(plugin, context)
        assertTrue(result.isSuccess)

        // Verify plugin was registered
        assertTrue(registry.isRegistered("test.lifecycle"))
    }

    @Test
    fun `lifecycle manager pauses and resumes plugin`() = runBlocking {
        val serviceRegistry = ServiceRegistry()
        val registry = UniversalPluginRegistry(serviceRegistry)
        val eventBus = GrpcPluginEventBus()
        val lifecycleManager = PluginLifecycleManager(registry, eventBus)

        val plugin = createTestPlugin("test.pauseresume")
        val context = createTestContext()

        lifecycleManager.manage(plugin, context)

        // Pause
        val pauseResult = lifecycleManager.pause("test.pauseresume")
        assertTrue(pauseResult.isSuccess)
        assertEquals(PluginState.PAUSED, registry.getPlugin("test.pauseresume")?.state)

        // Resume
        val resumeResult = lifecycleManager.resume("test.pauseresume")
        assertTrue(resumeResult.isSuccess)
        assertEquals(PluginState.ACTIVE, registry.getPlugin("test.pauseresume")?.state)
    }

    @Test
    fun `lifecycle manager shuts down plugin`() = runBlocking {
        val serviceRegistry = ServiceRegistry()
        val registry = UniversalPluginRegistry(serviceRegistry)
        val eventBus = GrpcPluginEventBus()
        val lifecycleManager = PluginLifecycleManager(registry, eventBus)

        val plugin = createTestPlugin("test.shutdown")
        val context = createTestContext()

        lifecycleManager.manage(plugin, context)

        val result = lifecycleManager.shutdown("test.shutdown")
        assertTrue(result.isSuccess)
        assertEquals(PluginState.STOPPED, registry.getPlugin("test.shutdown")?.state)
    }

    // ========== Capability Tests ==========

    @Test
    fun `capability equality works correctly`() {
        val cap1 = PluginCapability(
            id = "test.cap",
            name = "Test Capability",
            version = "1.0.0"
        )
        val cap2 = PluginCapability(
            id = "test.cap",
            name = "Test Capability",
            version = "1.0.0"
        )

        assertEquals(cap1, cap2)
        assertEquals(cap1.hashCode(), cap2.hashCode())
    }

    @Test
    fun `well-known capabilities have correct IDs`() {
        assertEquals("llm.text-generation", PluginCapability.LLM_TEXT_GENERATION.id)
        assertEquals("speech.recognition", PluginCapability.SPEECH_RECOGNITION.id)
        assertEquals("nlu.intent", PluginCapability.NLU_INTENT.id)
        assertEquals("accessibility.handler", PluginCapability.ACCESSIBILITY_HANDLER.id)
    }

    // ========== State Tests ==========

    @Test
    fun `plugin state transitions are valid`() {
        assertTrue(PluginState.ACTIVE.canPause())
        assertTrue(PluginState.PAUSED.canResume())
        assertTrue(PluginState.ACTIVE.canShutdown())
        assertFalse(PluginState.STOPPED.canPause())
        assertFalse(PluginState.FAILED.isOperational())
    }

    @Test
    fun `plugin state helper methods work correctly`() {
        assertTrue(PluginState.ACTIVE.isOperational())
        assertTrue(PluginState.PAUSED.isOperational())
        assertFalse(PluginState.UNINITIALIZED.isOperational())

        assertTrue(PluginState.INITIALIZING.isTransitional())
        assertTrue(PluginState.STOPPING.isTransitional())
        assertFalse(PluginState.ACTIVE.isTransitional())

        assertTrue(PluginState.ERROR.isError())
        assertTrue(PluginState.FAILED.isError())
        assertFalse(PluginState.ACTIVE.isError())
    }

    // ========== Helper Functions ==========

    private fun createTestPlugin(
        id: String,
        capabilities: Set<PluginCapability> = setOf(PluginCapability.LLM_TEXT_GENERATION)
    ): UniversalPlugin {
        return TestPlugin(
            pluginId = id,
            pluginName = "Test Plugin: $id",
            version = "1.0.0",
            capabilities = capabilities
        )
    }

    private fun createTestEndpoint(name: String = "test"): ServiceEndpoint {
        return ServiceEndpoint(
            serviceName = name,
            host = "localhost",
            port = 50060,
            protocol = "grpc"
        )
    }

    private fun createTestContext(): PluginContext {
        return PluginContext(
            appDataDir = "/tmp/test/data",
            cacheDir = "/tmp/test/cache",
            serviceRegistry = ServiceRegistry(),
            eventBus = GrpcPluginEventBus(),
            platformInfo = PlatformInfo(
                platform = "test",
                osVersion = "1.0",
                sdkVersion = 1
            )
        )
    }
}

/**
 * Test implementation of UniversalPlugin for testing.
 */
private class TestPlugin(
    override val pluginId: String,
    override val pluginName: String,
    override val version: String,
    override val capabilities: Set<PluginCapability>
) : UniversalPlugin {

    private val _stateFlow = MutableStateFlow(PluginState.ACTIVE)
    override val state: PluginState get() = _stateFlow.value
    override val stateFlow: StateFlow<PluginState> = _stateFlow

    override suspend fun initialize(config: PluginConfig, context: PluginContext): InitResult {
        _stateFlow.value = PluginState.ACTIVE
        return InitResult.Success("Initialized")
    }

    override suspend fun activate(): Result<Unit> {
        _stateFlow.value = PluginState.ACTIVE
        return Result.success(Unit)
    }

    override suspend fun pause(): Result<Unit> {
        _stateFlow.value = PluginState.PAUSED
        return Result.success(Unit)
    }

    override suspend fun resume(): Result<Unit> {
        _stateFlow.value = PluginState.ACTIVE
        return Result.success(Unit)
    }

    override suspend fun shutdown(): Result<Unit> {
        _stateFlow.value = PluginState.STOPPED
        return Result.success(Unit)
    }

    override suspend fun onConfigurationChanged(config: Map<String, Any>) {}

    override fun healthCheck(): HealthStatus {
        return HealthStatus(
            healthy = true,
            message = "OK",
            diagnostics = emptyMap(),
            lastCheckTime = System.currentTimeMillis(),
            checkDurationMs = 1
        )
    }

    override suspend fun onEvent(event: PluginEvent) {}
}
