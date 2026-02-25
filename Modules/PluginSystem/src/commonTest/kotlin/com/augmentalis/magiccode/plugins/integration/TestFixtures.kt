/**
 * TestFixtures.kt - Shared test infrastructure for integration tests
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides TestPlugin (a full UniversalPlugin implementation with lifecycle tracking)
 * and TestUtils (mock factory methods) used across integration test files:
 * - DiscoveryIntegrationTest
 * - PluginLifecycleIntegrationTest
 * - EventBusIntegrationTest
 */
package com.augmentalis.magiccode.plugins.integration

import com.augmentalis.magiccode.plugins.universal.*
import com.augmentalis.rpc.ServiceEndpoint
import com.augmentalis.rpc.ServiceRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Open test implementation of [UniversalPlugin] with lifecycle tracking.
 *
 * Tracks how many times each lifecycle method is called and records configuration
 * changes. Supports configurable failure modes for testing error/recovery paths.
 *
 * @param pluginId Unique plugin ID (reverse-domain)
 * @param pluginName Human-readable name (defaults to "Test Plugin: <pluginId>")
 * @param version Semantic version (defaults to "1.0.0")
 * @param capabilities Set of capabilities (defaults to a single LLM_TEXT_GENERATION capability)
 * @param failOnPause If true, [pause] returns a failure and transitions to ERROR
 * @param failOnShutdown If true, [shutdown] returns a failure but still transitions to STOPPED
 * @param initDelay Delay in milliseconds added to [initialize] for slow-plugin testing
 */
open class TestPlugin(
    override val pluginId: String,
    override val pluginName: String = "Test Plugin: $pluginId",
    override val version: String = "1.0.0",
    override val capabilities: Set<PluginCapability> = setOf(
        PluginCapability(
            id = PluginCapability.LLM_TEXT_GENERATION,
            name = "Text Generation",
            version = "1.0.0"
        )
    ),
    private val failOnPause: Boolean = false,
    private val failOnShutdown: Boolean = false,
    private val initDelay: Long = 0L
) : UniversalPlugin {

    private val _stateFlow = MutableStateFlow(PluginState.UNINITIALIZED)
    override val state: PluginState get() = _stateFlow.value
    override val stateFlow: StateFlow<PluginState> = _stateFlow

    /** Number of times [initialize] completed successfully. */
    var initializeCount: Int = 0
        private set

    /** Number of times [pause] completed successfully. */
    var pauseCount: Int = 0
        private set

    /** Number of times [resume] completed successfully. */
    var resumeCount: Int = 0
        private set

    /** Number of times [shutdown] completed successfully. */
    var shutdownCount: Int = 0
        private set

    /** Number of times [onConfigurationChanged] was called. */
    var configChangeCount: Int = 0
        private set

    /** Ordered list of configuration maps received via [onConfigurationChanged]. */
    val configChanges: MutableList<Map<String, Any>> = mutableListOf()

    /** When false, [healthCheck] reports unhealthy regardless of state. */
    var simulatedHealthy: Boolean = true

    /**
     * Hook for subclasses that want to override initialization logic.
     * Called inside [initialize] after the delay (if any) and before
     * the state transitions to ACTIVE.
     *
     * @return [InitResult] -- if a failure is returned the plugin transitions to FAILED
     */
    protected open suspend fun onInitialize(): InitResult {
        return InitResult.success("Initialized successfully")
    }

    /**
     * Reset the plugin to its initial UNINITIALIZED state and clear all counters.
     * Useful for retry/recovery tests.
     */
    fun reset() {
        _stateFlow.value = PluginState.UNINITIALIZED
        initializeCount = 0
        pauseCount = 0
        resumeCount = 0
        shutdownCount = 0
        configChangeCount = 0
        configChanges.clear()
        simulatedHealthy = true
    }

    // =========================================================================
    // UniversalPlugin lifecycle implementation
    // =========================================================================

    override suspend fun initialize(config: PluginConfig, context: PluginContext): InitResult {
        _stateFlow.value = PluginState.INITIALIZING

        if (initDelay > 0) {
            delay(initDelay)
        }

        val result = onInitialize()

        if (result.isSuccess()) {
            _stateFlow.value = PluginState.ACTIVE
            initializeCount++
        } else {
            _stateFlow.value = PluginState.FAILED
        }

        return result
    }

    override suspend fun activate(): Result<Unit> {
        _stateFlow.value = PluginState.ACTIVE
        return Result.success(Unit)
    }

    override suspend fun pause(): Result<Unit> {
        if (state != PluginState.ACTIVE) {
            return Result.failure(IllegalStateException("Cannot pause from state $state"))
        }
        if (failOnPause) {
            _stateFlow.value = PluginState.ERROR
            return Result.failure(RuntimeException("Simulated pause failure"))
        }
        _stateFlow.value = PluginState.PAUSED
        pauseCount++
        return Result.success(Unit)
    }

    override suspend fun resume(): Result<Unit> {
        if (state != PluginState.PAUSED) {
            return Result.failure(IllegalStateException("Cannot resume from state $state"))
        }
        _stateFlow.value = PluginState.ACTIVE
        resumeCount++
        return Result.success(Unit)
    }

    override suspend fun shutdown(): Result<Unit> {
        if (state != PluginState.ACTIVE && state != PluginState.PAUSED && state != PluginState.ERROR) {
            return Result.failure(IllegalStateException("Cannot shutdown from state $state"))
        }
        if (failOnShutdown) {
            _stateFlow.value = PluginState.STOPPED
            shutdownCount++
            return Result.failure(RuntimeException("Simulated shutdown failure"))
        }
        _stateFlow.value = PluginState.STOPPED
        shutdownCount++
        return Result.success(Unit)
    }

    override suspend fun onConfigurationChanged(config: Map<String, Any>) {
        configChangeCount++
        configChanges.add(config)
    }

    override fun healthCheck(): HealthStatus {
        val isHealthy = simulatedHealthy && state != PluginState.STOPPED && state != PluginState.FAILED
        return HealthStatus(
            healthy = isHealthy,
            message = if (isHealthy) "Plugin healthy" else "Plugin unhealthy (state=$state)",
            diagnostics = mapOf(
                "initializeCount" to initializeCount.toString(),
                "pauseCount" to pauseCount.toString(),
                "resumeCount" to resumeCount.toString(),
                "shutdownCount" to shutdownCount.toString(),
                "configChangeCount" to configChangeCount.toString()
            ),
            lastCheckTime = System.currentTimeMillis(),
            checkDurationMs = 1
        )
    }

    override suspend fun onEvent(event: PluginEvent) {
        // No-op for basic test plugin
    }
}

/**
 * Factory methods for creating mock objects used in integration tests.
 */
object TestUtils {

    /**
     * Create a mock [PluginContext] with sensible test defaults.
     */
    fun createMockPluginContext(): PluginContext {
        return PluginContext(
            appDataDir = "/tmp/test/data",
            cacheDir = "/tmp/test/cache",
            serviceRegistry = ServiceRegistry(),
            eventBus = GrpcPluginEventBus(),
            platformInfo = PlatformInfo(
                platform = "test",
                osVersion = "1.0"
            )
        )
    }

    /**
     * Create a mock [PluginConfig] with sensible test defaults.
     */
    fun createMockPluginConfig(): PluginConfig {
        return PluginConfig(
            settings = mapOf(
                "testMode" to "true"
            )
        )
    }

    /**
     * Create a mock [ServiceEndpoint] with sensible test defaults.
     *
     * @param serviceName The gRPC service name (defaults to "test")
     */
    fun createMockServiceEndpoint(serviceName: String = "test"): ServiceEndpoint {
        return ServiceEndpoint(
            serviceName = serviceName,
            host = "localhost",
            port = 50060,
            protocol = "grpc"
        )
    }
}
