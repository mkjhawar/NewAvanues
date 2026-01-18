package com.augmentalis.avamagic.plugin

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable

/**
 * Plugin Failure Recovery System
 *
 * Implements tiered escalation approach for plugin failures:
 * 1. Placeholder - Show placeholder UI while attempting recovery
 * 2. Disable - Disable the plugin and continue without it
 * 3. Graceful Crash - Show error UI and offer restart
 *
 * Features:
 * - Automatic retry with exponential backoff
 * - Circuit breaker pattern
 * - Health checks
 * - Recovery strategies per plugin
 * - Audit logging
 *
 * Usage:
 * ```kotlin
 * val recovery = PluginRecoveryManager()
 *
 * // Register plugin with recovery strategy
 * recovery.register(
 *     pluginId = "com.example.weather",
 *     strategy = RecoveryStrategy(
 *         maxRetries = 3,
 *         retryDelayMs = 1000,
 *         escalation = EscalationType.PLACEHOLDER_THEN_DISABLE
 *     )
 * )
 *
 * // Handle failure
 * recovery.handleFailure(
 *     pluginId = "com.example.weather",
 *     error = exception,
 *     context = mapOf("operation" to "fetchData")
 * )
 *
 * // Check plugin health
 * val health = recovery.getPluginHealth("com.example.weather")
 * ```
 *
 * Created by Manoj Jhawar, manoj@ideahq.net
 * Date: 2025-11-19
 * IDEACODE Version: 8.4
 */
class PluginRecoveryManager {

    private val plugins = mutableMapOf<String, PluginState>()
    private val strategies = mutableMapOf<String, RecoveryStrategy>()
    private val circuitBreakers = mutableMapOf<String, CircuitBreaker>()

    private val _events = MutableSharedFlow<RecoveryEvent>(extraBufferCapacity = 100)
    val events: Flow<RecoveryEvent> = _events

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Register a plugin with recovery strategy
     */
    fun register(
        pluginId: String,
        strategy: RecoveryStrategy = RecoveryStrategy()
    ) {
        plugins[pluginId] = PluginState(
            id = pluginId,
            status = PluginStatus.ACTIVE,
            failureCount = 0,
            lastFailure = null
        )
        strategies[pluginId] = strategy
        circuitBreakers[pluginId] = CircuitBreaker(
            threshold = strategy.circuitBreakerThreshold,
            resetTimeMs = strategy.circuitBreakerResetMs
        )
    }

    /**
     * Handle plugin failure
     */
    suspend fun handleFailure(
        pluginId: String,
        error: Throwable,
        context: Map<String, Any> = emptyMap()
    ): RecoveryResult {
        val plugin = plugins[pluginId] ?: return RecoveryResult(
            success = false,
            action = RecoveryAction.NONE,
            error = "Plugin not registered: $pluginId"
        )

        val strategy = strategies[pluginId] ?: RecoveryStrategy()
        val circuitBreaker = circuitBreakers[pluginId]!!

        // Record failure
        plugin.failureCount++
        plugin.lastFailure = System.currentTimeMillis()

        // Check circuit breaker
        if (circuitBreaker.isOpen()) {
            emitEvent(RecoveryEvent.CircuitBreakerOpen(pluginId))
            return escalate(pluginId, plugin, strategy, error, context)
        }

        circuitBreaker.recordFailure()

        // Emit failure event
        emitEvent(RecoveryEvent.PluginFailed(
            pluginId = pluginId,
            error = error.message ?: "Unknown error",
            failureCount = plugin.failureCount,
            context = context
        ))

        // Attempt recovery based on failure count
        return when {
            plugin.failureCount <= strategy.maxRetries -> {
                // Retry
                retry(pluginId, plugin, strategy, error, context)
            }
            plugin.failureCount <= strategy.maxRetries + strategy.placeholderAttempts -> {
                // Show placeholder
                showPlaceholder(pluginId, plugin, strategy)
            }
            else -> {
                // Escalate
                escalate(pluginId, plugin, strategy, error, context)
            }
        }
    }

    /**
     * Attempt to recover plugin
     */
    private suspend fun retry(
        pluginId: String,
        plugin: PluginState,
        strategy: RecoveryStrategy,
        error: Throwable,
        context: Map<String, Any>
    ): RecoveryResult {
        val delay = calculateBackoff(plugin.failureCount, strategy)

        emitEvent(RecoveryEvent.RetryScheduled(
            pluginId = pluginId,
            attempt = plugin.failureCount,
            delayMs = delay
        ))

        delay(delay)

        // Attempt recovery
        return try {
            strategy.recoveryAction?.invoke(pluginId, context)

            // Reset on success
            plugin.failureCount = 0
            plugin.status = PluginStatus.ACTIVE
            circuitBreakers[pluginId]?.reset()

            emitEvent(RecoveryEvent.RecoverySuccess(pluginId))

            RecoveryResult(
                success = true,
                action = RecoveryAction.RETRY,
                message = "Plugin recovered after ${plugin.failureCount} attempts"
            )
        } catch (e: Exception) {
            RecoveryResult(
                success = false,
                action = RecoveryAction.RETRY,
                error = e.message
            )
        }
    }

    /**
     * Show placeholder UI
     */
    private suspend fun showPlaceholder(
        pluginId: String,
        plugin: PluginState,
        strategy: RecoveryStrategy
    ): RecoveryResult {
        plugin.status = PluginStatus.PLACEHOLDER

        emitEvent(RecoveryEvent.PlaceholderShown(
            pluginId = pluginId,
            message = strategy.placeholderMessage
        ))

        return RecoveryResult(
            success = true,
            action = RecoveryAction.PLACEHOLDER,
            message = strategy.placeholderMessage
        )
    }

    /**
     * Escalate failure
     */
    private suspend fun escalate(
        pluginId: String,
        plugin: PluginState,
        strategy: RecoveryStrategy,
        error: Throwable,
        context: Map<String, Any>
    ): RecoveryResult {
        return when (strategy.escalation) {
            EscalationType.DISABLE -> {
                plugin.status = PluginStatus.DISABLED

                emitEvent(RecoveryEvent.PluginDisabled(
                    pluginId = pluginId,
                    reason = "Too many failures: ${plugin.failureCount}"
                ))

                RecoveryResult(
                    success = true,
                    action = RecoveryAction.DISABLE,
                    message = "Plugin disabled due to repeated failures"
                )
            }

            EscalationType.GRACEFUL_CRASH -> {
                plugin.status = PluginStatus.CRASHED

                emitEvent(RecoveryEvent.GracefulCrash(
                    pluginId = pluginId,
                    error = error.message ?: "Unknown error",
                    recoverable = strategy.allowManualRecovery
                ))

                RecoveryResult(
                    success = false,
                    action = RecoveryAction.CRASH,
                    error = "Plugin crashed: ${error.message}",
                    requiresRestart = true
                )
            }

            EscalationType.PLACEHOLDER_THEN_DISABLE -> {
                if (plugin.status != PluginStatus.PLACEHOLDER) {
                    showPlaceholder(pluginId, plugin, strategy)
                } else {
                    plugin.status = PluginStatus.DISABLED
                    emitEvent(RecoveryEvent.PluginDisabled(pluginId, "Escalated from placeholder"))
                    RecoveryResult(
                        success = true,
                        action = RecoveryAction.DISABLE,
                        message = "Plugin disabled after placeholder timeout"
                    )
                }
            }

            EscalationType.PLACEHOLDER_THEN_CRASH -> {
                if (plugin.status != PluginStatus.PLACEHOLDER) {
                    showPlaceholder(pluginId, plugin, strategy)
                } else {
                    plugin.status = PluginStatus.CRASHED
                    emitEvent(RecoveryEvent.GracefulCrash(pluginId, error.message ?: "", true))
                    RecoveryResult(
                        success = false,
                        action = RecoveryAction.CRASH,
                        error = "Plugin crashed after placeholder"
                    )
                }
            }
        }
    }

    /**
     * Reset plugin state (manual recovery)
     */
    suspend fun resetPlugin(pluginId: String): Boolean {
        val plugin = plugins[pluginId] ?: return false

        plugin.failureCount = 0
        plugin.status = PluginStatus.ACTIVE
        plugin.lastFailure = null
        circuitBreakers[pluginId]?.reset()

        emitEvent(RecoveryEvent.PluginReset(pluginId))

        return true
    }

    /**
     * Get plugin health status
     */
    fun getPluginHealth(pluginId: String): PluginHealth? {
        val plugin = plugins[pluginId] ?: return null
        val circuitBreaker = circuitBreakers[pluginId] ?: return null

        return PluginHealth(
            pluginId = pluginId,
            status = plugin.status,
            failureCount = plugin.failureCount,
            lastFailure = plugin.lastFailure,
            circuitBreakerState = circuitBreaker.state,
            healthy = plugin.status == PluginStatus.ACTIVE && !circuitBreaker.isOpen()
        )
    }

    /**
     * Get all plugin health statuses
     */
    fun getAllHealth(): List<PluginHealth> {
        return plugins.keys.mapNotNull { getPluginHealth(it) }
    }

    /**
     * Perform health check on all plugins
     */
    suspend fun healthCheck(): HealthCheckResult {
        val results = mutableListOf<PluginHealth>()
        var healthy = 0
        var unhealthy = 0

        plugins.forEach { (id, _) ->
            val health = getPluginHealth(id)
            if (health != null) {
                results.add(health)
                if (health.healthy) healthy++ else unhealthy++
            }
        }

        return HealthCheckResult(
            timestamp = System.currentTimeMillis(),
            totalPlugins = plugins.size,
            healthyPlugins = healthy,
            unhealthyPlugins = unhealthy,
            plugins = results
        )
    }

    private fun calculateBackoff(attempt: Int, strategy: RecoveryStrategy): Long {
        val delay = strategy.retryDelayMs * (1 shl (attempt - 1).coerceAtMost(5))
        return delay.coerceAtMost(strategy.maxRetryDelayMs)
    }

    private suspend fun emitEvent(event: RecoveryEvent) {
        _events.emit(event)
    }

    /**
     * Shutdown manager
     */
    fun shutdown() {
        scope.cancel()
    }
}

/**
 * Plugin state
 */
data class PluginState(
    val id: String,
    var status: PluginStatus,
    var failureCount: Int,
    var lastFailure: Long?
)

/**
 * Plugin status
 */
enum class PluginStatus {
    ACTIVE,
    PLACEHOLDER,
    DISABLED,
    CRASHED
}

/**
 * Recovery strategy
 */
data class RecoveryStrategy(
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000,
    val maxRetryDelayMs: Long = 30000,
    val placeholderAttempts: Int = 2,
    val placeholderMessage: String = "Plugin temporarily unavailable",
    val escalation: EscalationType = EscalationType.PLACEHOLDER_THEN_DISABLE,
    val circuitBreakerThreshold: Int = 5,
    val circuitBreakerResetMs: Long = 60000,
    val allowManualRecovery: Boolean = true,
    val recoveryAction: (suspend (String, Map<String, Any>) -> Unit)? = null
)

/**
 * Escalation types
 */
enum class EscalationType {
    DISABLE,                    // Disable plugin immediately
    GRACEFUL_CRASH,            // Show error and offer restart
    PLACEHOLDER_THEN_DISABLE,  // Show placeholder, then disable
    PLACEHOLDER_THEN_CRASH     // Show placeholder, then crash
}

/**
 * Recovery result
 */
@Serializable
data class RecoveryResult(
    val success: Boolean,
    val action: RecoveryAction,
    val message: String? = null,
    val error: String? = null,
    val requiresRestart: Boolean = false
)

/**
 * Recovery actions
 */
enum class RecoveryAction {
    NONE,
    RETRY,
    PLACEHOLDER,
    DISABLE,
    CRASH
}

/**
 * Recovery events
 */
sealed class RecoveryEvent {
    data class PluginFailed(
        val pluginId: String,
        val error: String,
        val failureCount: Int,
        val context: Map<String, Any>
    ) : RecoveryEvent()

    data class RetryScheduled(
        val pluginId: String,
        val attempt: Int,
        val delayMs: Long
    ) : RecoveryEvent()

    data class RecoverySuccess(
        val pluginId: String
    ) : RecoveryEvent()

    data class PlaceholderShown(
        val pluginId: String,
        val message: String
    ) : RecoveryEvent()

    data class PluginDisabled(
        val pluginId: String,
        val reason: String
    ) : RecoveryEvent()

    data class GracefulCrash(
        val pluginId: String,
        val error: String,
        val recoverable: Boolean
    ) : RecoveryEvent()

    data class PluginReset(
        val pluginId: String
    ) : RecoveryEvent()

    data class CircuitBreakerOpen(
        val pluginId: String
    ) : RecoveryEvent()
}

/**
 * Plugin health
 */
@Serializable
data class PluginHealth(
    val pluginId: String,
    val status: PluginStatus,
    val failureCount: Int,
    val lastFailure: Long?,
    val circuitBreakerState: CircuitBreakerState,
    val healthy: Boolean
)

/**
 * Health check result
 */
@Serializable
data class HealthCheckResult(
    val timestamp: Long,
    val totalPlugins: Int,
    val healthyPlugins: Int,
    val unhealthyPlugins: Int,
    val plugins: List<PluginHealth>
)

/**
 * Circuit breaker
 */
class CircuitBreaker(
    private val threshold: Int,
    private val resetTimeMs: Long
) {
    var state: CircuitBreakerState = CircuitBreakerState.CLOSED
        private set

    private var failureCount = 0
    private var lastFailureTime = 0L

    fun recordFailure() {
        failureCount++
        lastFailureTime = System.currentTimeMillis()

        if (failureCount >= threshold) {
            state = CircuitBreakerState.OPEN
        }
    }

    fun isOpen(): Boolean {
        if (state == CircuitBreakerState.OPEN) {
            // Check if reset time has passed
            if (System.currentTimeMillis() - lastFailureTime > resetTimeMs) {
                state = CircuitBreakerState.HALF_OPEN
                return false
            }
            return true
        }
        return false
    }

    fun reset() {
        state = CircuitBreakerState.CLOSED
        failureCount = 0
        lastFailureTime = 0
    }
}

/**
 * Circuit breaker states
 */
enum class CircuitBreakerState {
    CLOSED,     // Normal operation
    OPEN,       // Blocking requests
    HALF_OPEN   // Testing recovery
}
