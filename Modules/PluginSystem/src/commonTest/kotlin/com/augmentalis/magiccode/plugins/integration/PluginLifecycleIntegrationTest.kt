/**
 * PluginLifecycleIntegrationTest.kt - Full lifecycle integration tests
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Comprehensive integration tests for plugin lifecycle management including:
 * - Full lifecycle transitions (register -> initialize -> activate -> pause -> resume -> shutdown)
 * - Multiple plugin management
 * - State transition validation
 * - Plugin recovery after failure
 * - Graceful shutdown with active operations
 * - Health check integration
 * - Configuration change handling
 */
package com.augmentalis.magiccode.plugins.integration

import com.augmentalis.magiccode.plugins.universal.*
import com.augmentalis.universalrpc.ServiceRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for plugin lifecycle management.
 *
 * Tests the complete lifecycle flow from registration to shutdown,
 * including error handling and recovery scenarios.
 */
class PluginLifecycleIntegrationTest {

    // =========================================================================
    // Complete Lifecycle Tests
    // =========================================================================

    @Test
    fun testCompletePluginLifecycle() = runBlocking {
        // Arrange
        val serviceRegistry = ServiceRegistry()
        val registry = UniversalPluginRegistry(serviceRegistry)
        val eventBus = GrpcPluginEventBus()
        val lifecycleManager = PluginLifecycleManager(registry, eventBus)

        val plugin = TestPlugin(pluginId = "lifecycle.test.complete")
        val context = TestUtils.createMockPluginContext()
        val config = TestUtils.createMockPluginConfig()
        val endpoint = TestUtils.createMockServiceEndpoint()

        // Act & Assert: Register
        val registerResult = registry.register(plugin, endpoint)
        assertTrue(registerResult.isSuccess, "Plugin should register successfully")
        assertTrue(registry.isRegistered("lifecycle.test.complete"), "Plugin should be registered")

        // Act & Assert: Manage
        val manageResult = lifecycleManager.manage(plugin, context)
        assertTrue(manageResult.isSuccess, "Plugin should be managed successfully")
        assertTrue(lifecycleManager.isManaged("lifecycle.test.complete"), "Plugin should be managed")

        // Act & Assert: Initialize
        val initResult = lifecycleManager.initialize("lifecycle.test.complete", config)
        assertTrue(initResult.isSuccess(), "Plugin should initialize successfully")
        assertEquals(1, plugin.initializeCount, "Initialize should be called once")
        assertEquals(PluginState.ACTIVE, plugin.state, "Plugin should be ACTIVE after init")

        // Act & Assert: Pause
        val pauseResult = lifecycleManager.pause("lifecycle.test.complete")
        assertTrue(pauseResult.isSuccess, "Plugin should pause successfully")
        assertEquals(1, plugin.pauseCount, "Pause should be called once")
        assertEquals(PluginState.PAUSED, plugin.state, "Plugin should be PAUSED")

        // Act & Assert: Resume
        val resumeResult = lifecycleManager.resume("lifecycle.test.complete")
        assertTrue(resumeResult.isSuccess, "Plugin should resume successfully")
        assertEquals(1, plugin.resumeCount, "Resume should be called once")
        assertEquals(PluginState.ACTIVE, plugin.state, "Plugin should be ACTIVE after resume")

        // Act & Assert: Shutdown
        val shutdownResult = lifecycleManager.shutdown("lifecycle.test.complete")
        assertTrue(shutdownResult.isSuccess, "Plugin should shutdown successfully")
        assertEquals(1, plugin.shutdownCount, "Shutdown should be called once")
        assertEquals(PluginState.STOPPED, plugin.state, "Plugin should be STOPPED")
    }

    @Test
    fun testMultiplePluginsLifecycle() = runBlocking {
        // Arrange
        val serviceRegistry = ServiceRegistry()
        val registry = UniversalPluginRegistry(serviceRegistry)
        val eventBus = GrpcPluginEventBus()
        val lifecycleManager = PluginLifecycleManager(registry, eventBus)

        val plugins = listOf(
            TestPlugin(pluginId = "multi.plugin.1"),
            TestPlugin(pluginId = "multi.plugin.2"),
            TestPlugin(pluginId = "multi.plugin.3")
        )

        val context = TestUtils.createMockPluginContext()
        val config = TestUtils.createMockPluginConfig()

        // Act: Register and manage all plugins
        plugins.forEach { plugin ->
            val endpoint = TestUtils.createMockServiceEndpoint(serviceName = plugin.pluginId)
            registry.register(plugin, endpoint)
            lifecycleManager.manage(plugin, context)
            lifecycleManager.initialize(plugin.pluginId, config)
        }

        // Assert: All plugins registered and active
        assertEquals(3, registry.pluginCount, "All 3 plugins should be registered")
        plugins.forEach { plugin ->
            assertTrue(registry.isRegistered(plugin.pluginId), "Plugin ${plugin.pluginId} should be registered")
            assertEquals(PluginState.ACTIVE, plugin.state, "Plugin ${plugin.pluginId} should be ACTIVE")
        }

        // Act: Pause all plugins concurrently
        coroutineScope {
            plugins.map { plugin ->
                async { lifecycleManager.pause(plugin.pluginId) }
            }.awaitAll()
        }

        // Assert: All plugins paused
        plugins.forEach { plugin ->
            assertEquals(PluginState.PAUSED, plugin.state, "Plugin ${plugin.pluginId} should be PAUSED")
        }

        // Act: Shutdown all
        val failures = lifecycleManager.shutdownAll()

        // Assert: No failures
        assertTrue(failures.isEmpty(), "All plugins should shutdown successfully")
        plugins.forEach { plugin ->
            assertEquals(PluginState.STOPPED, plugin.state, "Plugin ${plugin.pluginId} should be STOPPED")
        }
    }

    // =========================================================================
    // State Transition Tests
    // =========================================================================

    @Test
    fun testStateTransitions() = runBlocking {
        // Arrange
        val plugin = TestPlugin(pluginId = "state.transitions")
        val context = TestUtils.createMockPluginContext()
        val config = TestUtils.createMockPluginConfig()

        // Assert: Initial state
        assertEquals(PluginState.UNINITIALIZED, plugin.state, "Initial state should be UNINITIALIZED")

        // Act & Assert: Initialize transition
        val initResult = plugin.initialize(config, context)
        assertTrue(initResult.isSuccess(), "Initialization should succeed")
        assertEquals(PluginState.ACTIVE, plugin.state, "State should transition to ACTIVE")

        // Act & Assert: Pause transition
        val pauseResult = plugin.pause()
        assertTrue(pauseResult.isSuccess, "Pause should succeed from ACTIVE")
        assertEquals(PluginState.PAUSED, plugin.state, "State should transition to PAUSED")

        // Act & Assert: Resume transition
        val resumeResult = plugin.resume()
        assertTrue(resumeResult.isSuccess, "Resume should succeed from PAUSED")
        assertEquals(PluginState.ACTIVE, plugin.state, "State should transition back to ACTIVE")

        // Act & Assert: Shutdown transition
        val shutdownResult = plugin.shutdown()
        assertTrue(shutdownResult.isSuccess, "Shutdown should succeed from ACTIVE")
        assertEquals(PluginState.STOPPED, plugin.state, "State should transition to STOPPED")
    }

    @Test
    fun testInvalidStateTransitions() = runBlocking {
        // Arrange
        val plugin = TestPlugin(pluginId = "invalid.transitions")

        // Assert: Cannot pause from UNINITIALIZED
        val pauseResult = plugin.pause()
        assertTrue(pauseResult.isFailure, "Cannot pause from UNINITIALIZED state")

        // Assert: Cannot resume from UNINITIALIZED
        val resumeResult = plugin.resume()
        assertTrue(resumeResult.isFailure, "Cannot resume from UNINITIALIZED state")

        // Initialize the plugin
        val context = TestUtils.createMockPluginContext()
        val config = TestUtils.createMockPluginConfig()
        plugin.initialize(config, context)

        // Assert: Cannot resume from ACTIVE (not paused)
        val resumeFromActive = plugin.resume()
        assertTrue(resumeFromActive.isFailure, "Cannot resume from ACTIVE state")
    }

    @Test
    fun testStateFlowUpdates() = runBlocking {
        // Arrange
        val plugin = TestPlugin(pluginId = "stateflow.test")
        val context = TestUtils.createMockPluginContext()
        val config = TestUtils.createMockPluginConfig()
        val collectedStates = mutableListOf<PluginState>()

        // Start collecting states in background
        val collectJob = launch {
            plugin.stateFlow.collect { state ->
                collectedStates.add(state)
            }
        }

        // Allow initial state to be collected
        delay(50)

        // Act: Perform lifecycle operations
        plugin.initialize(config, context)
        delay(50)
        plugin.pause()
        delay(50)
        plugin.resume()
        delay(50)
        plugin.shutdown()
        delay(50)

        collectJob.cancel()

        // Assert: State transitions were observed
        assertTrue(collectedStates.contains(PluginState.UNINITIALIZED), "Should observe UNINITIALIZED")
        assertTrue(collectedStates.contains(PluginState.ACTIVE), "Should observe ACTIVE")
        assertTrue(collectedStates.contains(PluginState.PAUSED), "Should observe PAUSED")
        assertTrue(collectedStates.contains(PluginState.STOPPED), "Should observe STOPPED")
    }

    // =========================================================================
    // Recovery Tests
    // =========================================================================

    @Test
    fun testPluginRecoveryAfterInitFailure() = runBlocking {
        // Arrange: Plugin that fails on first init
        var failInit = true
        val plugin = object : TestPlugin(pluginId = "recovery.init") {
            override suspend fun onInitialize(): InitResult {
                return if (failInit) {
                    failInit = false  // Will succeed on retry
                    InitResult.failure("Simulated failure", recoverable = true)
                } else {
                    InitResult.success("Recovered successfully")
                }
            }
        }

        val context = TestUtils.createMockPluginContext()
        val config = TestUtils.createMockPluginConfig()

        // Act: First initialization should fail
        val firstAttempt = plugin.initialize(config, context)

        // Assert: First attempt fails
        assertTrue(firstAttempt.isFailure(), "First initialization should fail")
        assertEquals(PluginState.FAILED, plugin.state, "Plugin should be in FAILED state")

        // Reset plugin state for retry
        (plugin as TestPlugin).reset()

        // Act: Retry initialization
        val retryAttempt = plugin.initialize(config, context)

        // Assert: Retry succeeds
        assertTrue(retryAttempt.isSuccess(), "Retry should succeed")
        assertEquals(PluginState.ACTIVE, plugin.state, "Plugin should be ACTIVE after recovery")
    }

    @Test
    fun testPluginRecoveryAfterPauseFailure() = runBlocking {
        // Arrange
        val plugin = TestPlugin(pluginId = "recovery.pause", failOnPause = true)
        val context = TestUtils.createMockPluginContext()
        val config = TestUtils.createMockPluginConfig()

        plugin.initialize(config, context)
        assertEquals(PluginState.ACTIVE, plugin.state)

        // Act: Pause should fail
        val pauseResult = plugin.pause()

        // Assert: Pause failed and state is ERROR
        assertTrue(pauseResult.isFailure, "Pause should fail")
        assertEquals(PluginState.ERROR, plugin.state, "Plugin should be in ERROR state")

        // Recovery: Shutdown should still work from ERROR state
        val shutdownResult = plugin.shutdown()
        assertTrue(shutdownResult.isSuccess, "Shutdown should succeed from ERROR state")
        assertEquals(PluginState.STOPPED, plugin.state, "Plugin should be STOPPED")
    }

    // =========================================================================
    // Graceful Shutdown Tests
    // =========================================================================

    @Test
    fun testGracefulShutdown() = runBlocking {
        // Arrange
        val serviceRegistry = ServiceRegistry()
        val registry = UniversalPluginRegistry(serviceRegistry)
        val eventBus = GrpcPluginEventBus()
        val lifecycleManager = PluginLifecycleManager(registry, eventBus)

        val plugins = (1..5).map { TestPlugin(pluginId = "graceful.$it") }
        val context = TestUtils.createMockPluginContext()
        val config = TestUtils.createMockPluginConfig()

        plugins.forEach { plugin ->
            val endpoint = TestUtils.createMockServiceEndpoint(serviceName = plugin.pluginId)
            registry.register(plugin, endpoint)
            lifecycleManager.manage(plugin, context)
            lifecycleManager.initialize(plugin.pluginId, config)
        }

        assertEquals(5, lifecycleManager.managedCount, "Should have 5 managed plugins")

        // Act: Shutdown all plugins
        val failures = lifecycleManager.shutdownAll()

        // Assert
        assertTrue(failures.isEmpty(), "All plugins should shutdown gracefully")
        assertEquals(0, lifecycleManager.managedCount, "No plugins should be managed after shutdown")
        plugins.forEach { plugin ->
            assertEquals(PluginState.STOPPED, plugin.state, "Plugin ${plugin.pluginId} should be STOPPED")
            assertEquals(1, plugin.shutdownCount, "Shutdown should be called exactly once")
        }
    }

    @Test
    fun testGracefulShutdownWithFailure() = runBlocking {
        // Arrange
        val serviceRegistry = ServiceRegistry()
        val registry = UniversalPluginRegistry(serviceRegistry)
        val eventBus = GrpcPluginEventBus()
        val lifecycleManager = PluginLifecycleManager(registry, eventBus)

        // Create plugins with one that fails on shutdown
        val normalPlugin = TestPlugin(pluginId = "shutdown.normal")
        val failingPlugin = TestPlugin(pluginId = "shutdown.failing", failOnShutdown = true)

        val context = TestUtils.createMockPluginContext()
        val config = TestUtils.createMockPluginConfig()

        for (plugin in listOf(normalPlugin, failingPlugin)) {
            val endpoint = TestUtils.createMockServiceEndpoint(serviceName = plugin.pluginId)
            registry.register(plugin, endpoint)
            lifecycleManager.manage(plugin, context)
            lifecycleManager.initialize(plugin.pluginId, config)
        }

        // Act: Shutdown all
        val failures = lifecycleManager.shutdownAll()

        // Assert: One failure reported but both still shut down
        assertEquals(1, failures.size, "One plugin should fail to shutdown")
        assertEquals("shutdown.failing", failures[0], "Failing plugin should be in failures list")

        // Both plugins should still be in STOPPED state (shutdown is always marked complete)
        assertEquals(PluginState.STOPPED, normalPlugin.state, "Normal plugin should be STOPPED")
        assertEquals(PluginState.STOPPED, failingPlugin.state, "Failing plugin should still be STOPPED")
    }

    @Test
    fun testShutdownWithSlowPlugin() = runBlocking {
        // Arrange
        val plugin = TestPlugin(pluginId = "slow.shutdown", initDelay = 100)
        val context = TestUtils.createMockPluginContext()
        val config = TestUtils.createMockPluginConfig()

        plugin.initialize(config, context)

        // Act: Shutdown with timeout
        val result = withTimeout(5000) {
            plugin.shutdown()
        }

        // Assert
        assertTrue(result.isSuccess, "Shutdown should complete within timeout")
        assertEquals(PluginState.STOPPED, plugin.state, "Plugin should be STOPPED")
    }

    // =========================================================================
    // Health Check Integration Tests
    // =========================================================================

    @Test
    fun testHealthCheckDuringLifecycle() = runBlocking {
        // Arrange
        val serviceRegistry = ServiceRegistry()
        val registry = UniversalPluginRegistry(serviceRegistry)
        val eventBus = GrpcPluginEventBus()
        val lifecycleManager = PluginLifecycleManager(registry, eventBus)

        val plugin = TestPlugin(pluginId = "health.lifecycle")
        val context = TestUtils.createMockPluginContext()
        val config = TestUtils.createMockPluginConfig()
        val endpoint = TestUtils.createMockServiceEndpoint()

        registry.register(plugin, endpoint)
        lifecycleManager.manage(plugin, context)
        lifecycleManager.initialize(plugin.pluginId, config)

        // Act & Assert: Health check when ACTIVE
        var health = plugin.healthCheck()
        assertTrue(health.healthy, "Plugin should be healthy when ACTIVE")
        assertEquals("OK", health.message.take(2), "Health message should indicate healthy")

        // Pause and check health
        lifecycleManager.pause(plugin.pluginId)
        health = plugin.healthCheck()
        assertTrue(health.healthy, "Plugin should still be healthy when PAUSED")

        // Simulate unhealthy state
        plugin.simulatedHealthy = false
        health = plugin.healthCheck()
        assertFalse(health.healthy, "Plugin should be unhealthy when simulated")

        // Shutdown and check health
        lifecycleManager.shutdown(plugin.pluginId)
        health = plugin.healthCheck()
        assertFalse(health.healthy, "Plugin should be unhealthy when STOPPED")
    }

    @Test
    fun testHealthCheckDiagnostics() = runBlocking {
        // Arrange
        val plugin = TestPlugin(pluginId = "health.diagnostics")
        val context = TestUtils.createMockPluginContext()
        val config = TestUtils.createMockPluginConfig()

        plugin.initialize(config, context)
        plugin.pause()
        plugin.resume()

        // Act
        val health = plugin.healthCheck()

        // Assert: Diagnostics contain expected data
        assertNotNull(health.diagnostics["initializeCount"], "Should have initializeCount diagnostic")
        assertEquals("1", health.diagnostics["initializeCount"], "initializeCount should be 1")
        assertNotNull(health.diagnostics["pauseCount"], "Should have pauseCount diagnostic")
        assertEquals("1", health.diagnostics["pauseCount"], "pauseCount should be 1")
        assertNotNull(health.diagnostics["resumeCount"], "Should have resumeCount diagnostic")
        assertEquals("1", health.diagnostics["resumeCount"], "resumeCount should be 1")
    }

    @Test
    fun testHealthCheckTimestamps() = runBlocking {
        // Arrange
        val plugin = TestPlugin(pluginId = "health.timestamp")
        val context = TestUtils.createMockPluginContext()
        val config = TestUtils.createMockPluginConfig()

        plugin.initialize(config, context)

        // Act
        val before = System.currentTimeMillis()
        val health = plugin.healthCheck()
        val after = System.currentTimeMillis()

        // Assert: Timestamp is reasonable
        assertTrue(
            health.lastCheckTime in before..after,
            "Health check timestamp should be between before ($before) and after ($after), got ${health.lastCheckTime}"
        )
    }

    // =========================================================================
    // Configuration Update Tests
    // =========================================================================

    @Test
    fun testConfigurationUpdates() = runBlocking {
        // Arrange
        val serviceRegistry = ServiceRegistry()
        val registry = UniversalPluginRegistry(serviceRegistry)
        val eventBus = GrpcPluginEventBus()
        val lifecycleManager = PluginLifecycleManager(registry, eventBus)

        val plugin = TestPlugin(pluginId = "config.updates")
        val context = TestUtils.createMockPluginContext()
        val config = TestUtils.createMockPluginConfig()
        val endpoint = TestUtils.createMockServiceEndpoint()

        registry.register(plugin, endpoint)
        lifecycleManager.manage(plugin, context)
        lifecycleManager.initialize(plugin.pluginId, config)

        // Act: Update configuration
        val newConfig = mapOf<String, Any>(
            "theme" to "dark",
            "timeout" to 10000
        )
        lifecycleManager.updateConfig(plugin.pluginId, newConfig)

        // Assert
        assertEquals(1, plugin.configChangeCount, "Config change should be called once")
        assertEquals(1, plugin.configChanges.size, "Should have one config change recorded")
        assertEquals("dark", plugin.configChanges[0]["theme"], "Theme should be updated")
        assertEquals(10000, plugin.configChanges[0]["timeout"], "Timeout should be updated")
    }

    @Test
    fun testMultipleConfigurationUpdates() = runBlocking {
        // Arrange
        val plugin = TestPlugin(pluginId = "config.multiple")
        val context = TestUtils.createMockPluginContext()
        val config = TestUtils.createMockPluginConfig()

        plugin.initialize(config, context)

        // Act: Multiple config updates
        plugin.onConfigurationChanged(mapOf("setting1" to "value1"))
        plugin.onConfigurationChanged(mapOf("setting2" to "value2"))
        plugin.onConfigurationChanged(mapOf("setting3" to "value3"))

        // Assert
        assertEquals(3, plugin.configChangeCount, "Should have 3 config changes")
        assertEquals(3, plugin.configChanges.size, "Should record 3 config changes")
    }

    // =========================================================================
    // Edge Cases and Error Handling
    // =========================================================================

    @Test
    fun testUnmanagedPluginOperations() = runBlocking {
        // Arrange
        val serviceRegistry = ServiceRegistry()
        val registry = UniversalPluginRegistry(serviceRegistry)
        val eventBus = GrpcPluginEventBus()
        val lifecycleManager = PluginLifecycleManager(registry, eventBus)

        // Act: Try operations on unmanaged plugin
        val initResult = lifecycleManager.initialize("nonexistent.plugin", TestUtils.createMockPluginConfig())
        val pauseResult = lifecycleManager.pause("nonexistent.plugin")
        val resumeResult = lifecycleManager.resume("nonexistent.plugin")
        val shutdownResult = lifecycleManager.shutdown("nonexistent.plugin")

        // Assert: All operations should fail gracefully
        assertTrue(initResult.isFailure(), "Init should fail for unmanaged plugin")
        assertTrue(pauseResult.isFailure, "Pause should fail for unmanaged plugin")
        assertTrue(resumeResult.isFailure, "Resume should fail for unmanaged plugin")
        assertTrue(shutdownResult.isFailure, "Shutdown should fail for unmanaged plugin")
    }

    @Test
    fun testDuplicatePluginRegistration() = runBlocking {
        // Arrange
        val serviceRegistry = ServiceRegistry()
        val registry = UniversalPluginRegistry(serviceRegistry)

        val plugin1 = TestPlugin(pluginId = "duplicate.test")
        val plugin2 = TestPlugin(pluginId = "duplicate.test")
        val endpoint = TestUtils.createMockServiceEndpoint()

        // Act
        val firstResult = registry.register(plugin1, endpoint)
        val secondResult = registry.register(plugin2, endpoint)

        // Assert
        assertTrue(firstResult.isSuccess, "First registration should succeed")
        assertTrue(secondResult.isFailure, "Second registration should fail")
        assertTrue(
            secondResult.exceptionOrNull()?.message?.contains("already registered") == true,
            "Error message should indicate duplicate registration"
        )
    }

    @Test
    fun testPluginLifecycleWithEvents() = runBlocking {
        // Arrange
        val serviceRegistry = ServiceRegistry()
        val registry = UniversalPluginRegistry(serviceRegistry)
        val eventBus = GrpcPluginEventBus()
        val lifecycleManager = PluginLifecycleManager(registry, eventBus)

        val plugin = TestPlugin(pluginId = "events.lifecycle")
        val context = TestUtils.createMockPluginContext()
        val config = TestUtils.createMockPluginConfig()
        val endpoint = TestUtils.createMockServiceEndpoint()

        // Subscribe to state change events
        val stateEvents = mutableListOf<PluginEvent>()
        val subscription = eventBus.subscribeToTypes(PluginEvent.TYPE_STATE_CHANGED)
        val collectJob = launch {
            subscription.collect { event ->
                if (event.sourcePluginId == plugin.pluginId) {
                    stateEvents.add(event)
                }
            }
        }

        // Act: Perform lifecycle operations
        registry.register(plugin, endpoint)
        lifecycleManager.manage(plugin, context)
        delay(50) // Allow events to propagate
        lifecycleManager.initialize(plugin.pluginId, config)
        delay(50)
        lifecycleManager.pause(plugin.pluginId)
        delay(50)
        lifecycleManager.resume(plugin.pluginId)
        delay(50)
        lifecycleManager.shutdown(plugin.pluginId)
        delay(50)

        collectJob.cancel()

        // Assert: State change events were published
        assertTrue(stateEvents.isNotEmpty(), "Should have received state change events")
        assertTrue(
            stateEvents.any { it.payload["state"] == "ACTIVE" },
            "Should have ACTIVE state event"
        )
    }
}
