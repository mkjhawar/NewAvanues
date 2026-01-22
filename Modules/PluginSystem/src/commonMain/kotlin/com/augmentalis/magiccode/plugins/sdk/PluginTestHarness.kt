/**
 * PluginTestHarness.kt - Testing utilities for plugins
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides a comprehensive test harness for unit testing UniversalPlugin
 * implementations with mocked context, events, and assertions.
 */
package com.augmentalis.magiccode.plugins.sdk

import com.augmentalis.magiccode.plugins.universal.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Test harness for UniversalPlugin implementations.
 *
 * Provides utilities for creating, initializing, and testing plugins in
 * isolation. Includes event recording, state assertions, and lifecycle
 * management helpers.
 *
 * ## Usage Example
 * ```kotlin
 * class MyPluginTest {
 *     private val harness = PluginTestHarness.forPlugin { MyLLMPlugin() }
 *
 *     @BeforeTest
 *     fun setup() {
 *         harness.createPlugin()
 *     }
 *
 *     @AfterTest
 *     fun teardown() {
 *         harness.cleanup()
 *     }
 *
 *     @Test
 *     fun `plugin initializes successfully`() = runTest {
 *         val result = harness.initializeWith(
 *             config = testConfig(mapOf("apiKey" to "test-key")),
 *             context = testContext()
 *         )
 *
 *         assertTrue(result.isSuccess())
 *         harness.assertState(PluginState.ACTIVE)
 *         harness.assertHealthy()
 *     }
 *
 *     @Test
 *     fun `plugin handles events`() = runTest {
 *         harness.initializeWithDefaults()
 *
 *         val event = PluginEvent(
 *             eventId = "test-1",
 *             sourcePluginId = "test.source",
 *             eventType = PluginEvent.TYPE_VOICE_COMMAND,
 *             payload = mapOf("command" to "hello")
 *         )
 *
 *         harness.sendEvent(event)
 *
 *         val received = harness.getReceivedEvents()
 *         assertEquals(1, received.size)
 *         assertEquals("hello", received[0].payload["command"])
 *     }
 * }
 * ```
 *
 * @param T The plugin type being tested
 * @param pluginFactory Factory function to create new plugin instances
 * @since 1.0.0
 * @see UniversalPlugin
 */
class PluginTestHarness<T : UniversalPlugin>(
    private val pluginFactory: () -> T
) {

    private var _plugin: T? = null
    private val _receivedEvents = mutableListOf<PluginEvent>()
    private val _publishedEvents = mutableListOf<PluginEvent>()
    private val _testEventBus = TestPluginEventBus()
    private var _testContext: PluginContext? = null
    private var _testConfig: PluginConfig = PluginConfig.EMPTY

    /**
     * The current plugin instance.
     *
     * @throws IllegalStateException if [createPlugin] hasn't been called
     */
    val plugin: T
        get() = _plugin ?: throw IllegalStateException("Plugin not created. Call createPlugin() first.")

    /**
     * Whether a plugin instance exists.
     */
    val hasPlugin: Boolean
        get() = _plugin != null

    // =========================================================================
    // Plugin Lifecycle
    // =========================================================================

    /**
     * Create a new plugin instance.
     *
     * Replaces any existing plugin. Does not initialize the plugin.
     *
     * @return The created plugin instance
     */
    fun createPlugin(): T {
        _plugin = pluginFactory()
        _receivedEvents.clear()
        _publishedEvents.clear()
        return _plugin!!
    }

    /**
     * Initialize the plugin with default test configuration.
     *
     * Uses [testContext] and [testConfig] for initialization.
     *
     * @return InitResult from plugin initialization
     */
    suspend fun initializeWithDefaults(): InitResult {
        val plugin = _plugin ?: createPlugin()
        val context = testContext()
        val config = testConfig()
        _testContext = context
        _testConfig = config
        return plugin.initialize(config, context)
    }

    /**
     * Initialize the plugin with custom configuration.
     *
     * @param config Plugin configuration
     * @param context Plugin context
     * @return InitResult from plugin initialization
     */
    suspend fun initializeWith(config: PluginConfig, context: PluginContext): InitResult {
        val plugin = _plugin ?: createPlugin()
        _testConfig = config
        _testContext = context
        return plugin.initialize(config, context)
    }

    /**
     * Initialize the plugin with settings map.
     *
     * Creates a config from the settings and uses test context.
     *
     * @param settings Configuration settings
     * @return InitResult from plugin initialization
     */
    suspend fun initializeWithSettings(settings: Map<String, String>): InitResult {
        return initializeWith(
            config = testConfig(settings),
            context = testContext()
        )
    }

    /**
     * Activate the plugin.
     *
     * @return Result from plugin activation
     */
    suspend fun activate(): Result<Unit> {
        return plugin.activate()
    }

    /**
     * Pause the plugin.
     *
     * @return Result from plugin pause
     */
    suspend fun pause(): Result<Unit> {
        return plugin.pause()
    }

    /**
     * Resume the plugin.
     *
     * @return Result from plugin resume
     */
    suspend fun resume(): Result<Unit> {
        return plugin.resume()
    }

    /**
     * Shutdown the plugin.
     *
     * @return Result from plugin shutdown
     */
    suspend fun shutdown(): Result<Unit> {
        return plugin.shutdown()
    }

    // =========================================================================
    // Event Handling
    // =========================================================================

    /**
     * Send an event to the plugin.
     *
     * Calls the plugin's [UniversalPlugin.onEvent] method and records
     * the event for later verification.
     *
     * @param event The event to send
     */
    suspend fun sendEvent(event: PluginEvent) {
        _receivedEvents.add(event)
        plugin.onEvent(event)
    }

    /**
     * Send multiple events to the plugin.
     *
     * @param events Events to send in order
     */
    suspend fun sendEvents(vararg events: PluginEvent) {
        events.forEach { sendEvent(it) }
    }

    /**
     * Get all events that were sent to the plugin.
     *
     * @return List of received events in order
     */
    fun getReceivedEvents(): List<PluginEvent> = _receivedEvents.toList()

    /**
     * Get received events filtered by type.
     *
     * @param eventType Event type to filter by
     * @return List of matching events
     */
    fun getReceivedEventsOfType(eventType: String): List<PluginEvent> {
        return _receivedEvents.filter { it.eventType == eventType }
    }

    /**
     * Clear all recorded events.
     */
    fun clearEvents() {
        _receivedEvents.clear()
        _publishedEvents.clear()
    }

    /**
     * Get the test event bus.
     *
     * @return Test event bus for advanced event testing
     */
    fun getEventBus(): TestPluginEventBus = _testEventBus

    // =========================================================================
    // Assertions
    // =========================================================================

    /**
     * Assert that the plugin is in the expected state.
     *
     * @param expected Expected plugin state
     * @throws AssertionError if state doesn't match
     */
    fun assertState(expected: PluginState) {
        val actual = plugin.state
        if (actual != expected) {
            throw AssertionError("Expected state $expected but was $actual")
        }
    }

    /**
     * Assert that the plugin is healthy.
     *
     * @throws AssertionError if health check fails
     */
    fun assertHealthy() {
        val health = plugin.healthCheck()
        if (!health.healthy) {
            throw AssertionError("Plugin is not healthy: ${health.message}")
        }
    }

    /**
     * Assert that the plugin is not healthy.
     *
     * @throws AssertionError if health check passes
     */
    fun assertUnhealthy() {
        val health = plugin.healthCheck()
        if (health.healthy) {
            throw AssertionError("Expected plugin to be unhealthy but it is healthy")
        }
    }

    /**
     * Assert that the plugin has a specific capability.
     *
     * @param capabilityId Capability ID to check
     * @throws AssertionError if capability is not present
     */
    fun assertHasCapability(capabilityId: String) {
        if (!plugin.hasCapability(capabilityId)) {
            throw AssertionError(
                "Expected plugin to have capability '$capabilityId' " +
                    "but it only has: ${plugin.getCapabilityIds()}"
            )
        }
    }

    /**
     * Assert that a specific event was received.
     *
     * @param eventType Event type to check for
     * @throws AssertionError if no matching event was received
     */
    fun assertEventReceived(eventType: String) {
        val matching = getReceivedEventsOfType(eventType)
        if (matching.isEmpty()) {
            throw AssertionError(
                "Expected event of type '$eventType' but received: " +
                    "${_receivedEvents.map { it.eventType }}"
            )
        }
    }

    /**
     * Assert that a specific number of events were received.
     *
     * @param count Expected number of events
     * @throws AssertionError if count doesn't match
     */
    fun assertEventCount(count: Int) {
        val actual = _receivedEvents.size
        if (actual != count) {
            throw AssertionError("Expected $count events but received $actual")
        }
    }

    // =========================================================================
    // Cleanup
    // =========================================================================

    /**
     * Clean up the harness and shutdown the plugin.
     *
     * Call this in @AfterTest to ensure proper cleanup.
     */
    suspend fun cleanup() {
        _plugin?.let { plugin ->
            if (plugin.state.canShutdown()) {
                plugin.shutdown()
            }
        }
        _plugin = null
        _receivedEvents.clear()
        _publishedEvents.clear()
        _testContext = null
        _testConfig = PluginConfig.EMPTY
    }

    /**
     * Clean up synchronously (for use in non-suspend contexts).
     *
     * Note: Does not call plugin shutdown. Use [cleanup] when possible.
     */
    fun cleanupSync() {
        _plugin = null
        _receivedEvents.clear()
        _publishedEvents.clear()
        _testContext = null
        _testConfig = PluginConfig.EMPTY
    }

    // =========================================================================
    // Companion Object
    // =========================================================================

    companion object {
        /**
         * Create a test harness for the specified plugin type.
         *
         * @param factory Factory function to create plugin instances
         * @return New test harness
         */
        fun <T : UniversalPlugin> forPlugin(factory: () -> T) = PluginTestHarness(factory)

        /**
         * Create a test context with default values.
         *
         * @param appDataDir Application data directory (default: "/tmp/test")
         * @param cacheDir Cache directory (default: "/tmp/cache")
         * @param platform Platform identifier (default: "test")
         * @return Test PluginContext
         */
        fun testContext(
            appDataDir: String = "/tmp/test",
            cacheDir: String = "/tmp/cache",
            platform: String = "test"
        ): PluginContext {
            return PluginContextBuilder.create()
                .appDataDir(appDataDir)
                .cacheDir(cacheDir)
                .apply {
                    when (platform) {
                        "android" -> android()
                        "ios" -> ios()
                        "desktop" -> desktop()
                        else -> extra("platform", platform)
                    }
                }
                .build()
        }

        /**
         * Create a test configuration with settings.
         *
         * @param settings Configuration settings map (default: empty)
         * @param secrets Secret values map (default: empty)
         * @param features Enabled feature flags (default: empty)
         * @return Test PluginConfig
         */
        fun testConfig(
            settings: Map<String, String> = emptyMap(),
            secrets: Map<String, String> = emptyMap(),
            features: Set<String> = emptySet()
        ): PluginConfig {
            return PluginConfig(
                settings = settings,
                secrets = secrets,
                features = features
            )
        }

        /**
         * Create a test event.
         *
         * @param eventType Event type
         * @param sourcePluginId Source plugin ID (default: "test.source")
         * @param payload Event payload (default: empty)
         * @return Test PluginEvent
         */
        fun testEvent(
            eventType: String,
            sourcePluginId: String = "test.source",
            payload: Map<String, String> = emptyMap()
        ): PluginEvent {
            return PluginEvent(
                eventId = "test-${System.currentTimeMillis()}",
                sourcePluginId = sourcePluginId,
                eventType = eventType,
                payload = payload
            )
        }

        /**
         * Create a voice command event for testing.
         *
         * @param command The voice command text
         * @return Voice command PluginEvent
         */
        fun voiceCommandEvent(command: String): PluginEvent {
            return testEvent(
                eventType = PluginEvent.TYPE_VOICE_COMMAND,
                payload = mapOf("command" to command)
            )
        }

        /**
         * Create a configuration change event for testing.
         *
         * @param changes Changed configuration values
         * @return Config changed PluginEvent
         */
        fun configChangedEvent(changes: Map<String, String>): PluginEvent {
            return testEvent(
                eventType = PluginEvent.TYPE_CONFIG_CHANGED,
                payload = changes
            )
        }
    }

    // =========================================================================
    // Test Event Bus
    // =========================================================================

    /**
     * Test implementation of PluginEventBus for testing event flows.
     */
    class TestPluginEventBus : PluginEventBus {
        private val _events = MutableSharedFlow<PluginEvent>(replay = 10)
        private val _publishedEvents = mutableListOf<PluginEvent>()

        /**
         * Get all events that have been published through this bus.
         */
        val publishedEvents: List<PluginEvent>
            get() = _publishedEvents.toList()

        override suspend fun publish(event: PluginEvent): Int {
            _publishedEvents.add(event)
            _events.emit(event)
            return _events.subscriptionCount.value
        }

        override fun subscribe(filter: EventFilter): Flow<PluginEvent> {
            return _events.asSharedFlow()
        }

        override fun subscribeToTypes(vararg eventTypes: String): Flow<PluginEvent> {
            return subscribe(EventFilter.forTypes(*eventTypes))
        }

        override fun subscribeToPlugins(vararg pluginIds: String): Flow<PluginEvent> {
            return subscribe(EventFilter.forPlugins(*pluginIds))
        }

        /**
         * Clear all published events.
         */
        fun clear() {
            _publishedEvents.clear()
        }

        /**
         * Get published events of a specific type.
         */
        fun getPublishedEventsOfType(eventType: String): List<PluginEvent> {
            return _publishedEvents.filter { it.eventType == eventType }
        }
    }
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Run a test with automatic cleanup.
 *
 * @param block Test block to execute
 */
suspend inline fun <T : UniversalPlugin> PluginTestHarness<T>.runTest(
    crossinline block: suspend PluginTestHarness<T>.() -> Unit
) {
    try {
        block()
    } finally {
        cleanup()
    }
}

/**
 * Initialize and run assertions in one call.
 *
 * @param config Plugin configuration
 * @param context Plugin context
 * @param assertions Assertions to run after initialization
 * @return InitResult from initialization
 */
suspend inline fun <T : UniversalPlugin> PluginTestHarness<T>.initializeAndAssert(
    config: PluginConfig = PluginTestHarness.testConfig(),
    context: PluginContext = PluginTestHarness.testContext(),
    crossinline assertions: PluginTestHarness<T>.() -> Unit
): InitResult {
    val result = initializeWith(config, context)
    assertions()
    return result
}
