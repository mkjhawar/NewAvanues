/**
 * DivergenceAlerts.kt - Real-time alert system for divergences
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI Testing Framework
 * Created: 2025-10-15 02:48:36 PDT
 */
package com.augmentalis.voiceoscore.testing

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Alert action to take when divergence detected
 */
enum class AlertAction {
    LOG_ONLY,           // Just log the alert
    NOTIFY,             // Send notification
    ROLLBACK,           // Trigger rollback to legacy
    CIRCUIT_BREAK,      // Open circuit breaker
    TERMINATE           // Terminate comparison (testing failed)
}

/**
 * Alert rule configuration
 */
data class AlertRule(
    val name: String,
    val condition: (ComparisonResult) -> Boolean,
    val action: AlertAction,
    val cooldownMs: Long = 0,  // Minimum time between alerts
    val maxAlertsPerHour: Int = Int.MAX_VALUE,
    val enabled: Boolean = true
)

/**
 * Alert triggered by a divergence
 */
data class DivergenceAlert(
    val timestamp: Long = System.currentTimeMillis(),
    val rule: AlertRule,
    val result: ComparisonResult,
    val action: AlertAction,
    val message: String,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Alert listener interface
 */
interface AlertListener {
    suspend fun onAlert(alert: DivergenceAlert)
}

/**
 * Rollback trigger interface
 */
interface RollbackTrigger {
    suspend fun triggerRollback(reason: String, result: ComparisonResult)
}

/**
 * Circuit breaker for comparison framework
 */
class ComparisonCircuitBreaker(
    private val failureThreshold: Int = 5,
    private val timeWindowMs: Long = 60_000,  // 1 minute
    private val resetTimeoutMs: Long = 300_000  // 5 minutes
) {
    private val failures = mutableListOf<Long>()
    private val lock = Any()

    @Volatile
    private var state: CircuitState = CircuitState.CLOSED

    @Volatile
    private var lastOpenTime: Long = 0

    enum class CircuitState {
        CLOSED,   // Normal operation
        OPEN,     // Circuit open - no comparisons
        HALF_OPEN // Testing if circuit can close
    }

    /**
     * Check if circuit is open
     */
    fun isOpen(): Boolean {
        synchronized(lock) {
            // Try to reset if timeout expired
            if (state == CircuitState.OPEN &&
                System.currentTimeMillis() - lastOpenTime > resetTimeoutMs
            ) {
                state = CircuitState.HALF_OPEN
                Log.i(TAG, "Circuit breaker entering HALF_OPEN state")
            }

            return state == CircuitState.OPEN
        }
    }

    /**
     * Record a failure
     */
    fun recordFailure() {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            failures.add(now)

            // Remove old failures outside time window
            failures.removeAll { it < now - timeWindowMs }

            // Check if threshold exceeded
            if (failures.size >= failureThreshold) {
                if (state == CircuitState.CLOSED) {
                    state = CircuitState.OPEN
                    lastOpenTime = now
                    Log.e(TAG, "Circuit breaker OPENED after $failureThreshold failures in ${timeWindowMs}ms")
                } else if (state == CircuitState.HALF_OPEN) {
                    state = CircuitState.OPEN
                    lastOpenTime = now
                    Log.e(TAG, "Circuit breaker re-OPENED (half-open test failed)")
                }
            }
        }
    }

    /**
     * Record a success
     */
    fun recordSuccess() {
        synchronized(lock) {
            if (state == CircuitState.HALF_OPEN) {
                state = CircuitState.CLOSED
                failures.clear()
                Log.i(TAG, "Circuit breaker CLOSED (recovery successful)")
            }
        }
    }

    /**
     * Manually reset the circuit breaker
     */
    fun reset() {
        synchronized(lock) {
            state = CircuitState.CLOSED
            failures.clear()
            lastOpenTime = 0
            Log.i(TAG, "Circuit breaker manually reset")
        }
    }

    /**
     * Get current state
     */
    fun getState(): CircuitState {
        synchronized(lock) {
            return state
        }
    }

    companion object {
        private const val TAG = "CircuitBreaker"
    }
}

/**
 * Real-time alert system for divergences
 *
 * Features:
 * - Rule-based alerting
 * - Alert throttling and cooldown
 * - Circuit breaker integration
 * - Rollback triggering
 * - Low latency (<100ms)
 */
class DivergenceAlertSystem(
    private val rollbackTrigger: RollbackTrigger? = null
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val alertChannel = Channel<DivergenceAlert>(Channel.UNLIMITED)

    private val _alertFlow = MutableSharedFlow<DivergenceAlert>(
        replay = 0,
        extraBufferCapacity = 100
    )
    val alertFlow: SharedFlow<DivergenceAlert> = _alertFlow.asSharedFlow()

    private val rules = ConcurrentHashMap<String, AlertRule>()
    private val listeners = mutableListOf<AlertListener>()
    private val lastAlertTimes = ConcurrentHashMap<String, AtomicLong>()
    private val alertCounts = ConcurrentHashMap<String, MutableList<Long>>()

    val circuitBreaker = ComparisonCircuitBreaker()

    init {
        // Start alert processor
        scope.launch {
            for (alert in alertChannel) {
                processAlert(alert)
            }
        }

        // Register default rules
        registerDefaultRules()
    }

    /**
     * Register default alert rules
     */
    private fun registerDefaultRules() {
        // Rule: Any CRITICAL divergence triggers rollback
        addRule(
            AlertRule(
                name = "critical_divergence",
                condition = { it.maxSeverity == DivergenceSeverity.CRITICAL },
                action = AlertAction.ROLLBACK,
                cooldownMs = 0  // No cooldown for critical
            )
        )

        // Rule: Circuit break on multiple HIGH severity in short time
        addRule(
            AlertRule(
                name = "high_severity_burst",
                condition = { result ->
                    result.divergences.count { it.severity == DivergenceSeverity.HIGH } >= 3
                },
                action = AlertAction.CIRCUIT_BREAK,
                cooldownMs = 10_000,  // 10 second cooldown
                maxAlertsPerHour = 6
            )
        )

        // Rule: Notify on any divergence (with throttling)
        addRule(
            AlertRule(
                name = "any_divergence",
                condition = { it.hasDivergence },
                action = AlertAction.NOTIFY,
                cooldownMs = 5_000,  // 5 second cooldown
                maxAlertsPerHour = 100
            )
        )
    }

    /**
     * Add an alert rule
     */
    fun addRule(rule: AlertRule) {
        rules[rule.name] = rule
        Log.d(TAG, "Alert rule added: ${rule.name}")
    }

    /**
     * Remove an alert rule
     */
    fun removeRule(ruleName: String) {
        rules.remove(ruleName)
        Log.d(TAG, "Alert rule removed: $ruleName")
    }

    /**
     * Add an alert listener
     */
    fun addListener(listener: AlertListener) {
        listeners.add(listener)
    }

    /**
     * Evaluate comparison result against alert rules
     *
     * Returns within 100ms (non-blocking)
     */
    suspend fun evaluate(result: ComparisonResult) {
        val evaluationStart = System.currentTimeMillis()

        // Skip if circuit is open
        if (circuitBreaker.isOpen()) {
            Log.w(TAG, "Circuit breaker is OPEN - skipping alert evaluation")
            return
        }

        // Evaluate all enabled rules
        rules.values.filter { it.enabled }.forEach { rule ->
            try {
                if (rule.condition(result)) {
                    val alert = createAlert(rule, result)

                    // Check throttling
                    if (shouldSendAlert(rule, alert)) {
                        // Send alert (non-blocking)
                        alertChannel.trySend(alert)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error evaluating rule ${rule.name}", e)
            }
        }

        val evaluationTime = System.currentTimeMillis() - evaluationStart
        if (evaluationTime > 100) {
            Log.w(TAG, "Alert evaluation took ${evaluationTime}ms (target: <100ms)")
        }
    }

    /**
     * Check if alert should be sent (throttling)
     */
    private fun shouldSendAlert(rule: AlertRule, alert: DivergenceAlert): Boolean {
        val now = System.currentTimeMillis()

        // Check cooldown
        val lastAlert = lastAlertTimes.getOrPut(rule.name) { AtomicLong(0) }
        if (now - lastAlert.get() < rule.cooldownMs) {
            Log.v(TAG, "Alert suppressed (cooldown): ${rule.name}")
            return false
        }

        // Check hourly limit
        val alertTimes = alertCounts.getOrPut(rule.name) { mutableListOf() }
        synchronized(alertTimes) {
            // Remove alerts older than 1 hour
            alertTimes.removeAll { it < now - 3600_000 }

            if (alertTimes.size >= rule.maxAlertsPerHour) {
                Log.v(TAG, "Alert suppressed (hourly limit): ${rule.name}")
                return false
            }

            alertTimes.add(now)
        }

        lastAlertTimes[rule.name]?.set(now)
        return true
    }

    /**
     * Process an alert
     */
    private suspend fun processAlert(alert: DivergenceAlert) {
        try {
            // Log alert
            logAlert(alert)

            // Emit to flow
            _alertFlow.emit(alert)

            // Execute action
            when (alert.action) {
                AlertAction.LOG_ONLY -> {
                    // Already logged
                }

                AlertAction.NOTIFY -> {
                    // Notify all listeners
                    listeners.forEach { listener ->
                        try {
                            listener.onAlert(alert)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in alert listener", e)
                        }
                    }
                }

                AlertAction.ROLLBACK -> {
                    Log.e(TAG, "TRIGGERING ROLLBACK: ${alert.message}")
                    rollbackTrigger?.triggerRollback(alert.message, alert.result)
                    circuitBreaker.recordFailure()
                }

                AlertAction.CIRCUIT_BREAK -> {
                    Log.e(TAG, "OPENING CIRCUIT BREAKER: ${alert.message}")
                    circuitBreaker.recordFailure()
                }

                AlertAction.TERMINATE -> {
                    Log.e(TAG, "TERMINATING COMPARISON: ${alert.message}")
                    circuitBreaker.recordFailure()
                    // TODO: Implement termination logic
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing alert", e)
        }
    }

    /**
     * Create an alert from a rule and result
     */
    private fun createAlert(rule: AlertRule, result: ComparisonResult): DivergenceAlert {
        val message = buildString {
            append("Alert: ${rule.name} - ")
            append("Method: ${result.methodName}, ")
            append("Divergences: ${result.divergences.size}, ")
            append("Max Severity: ${result.maxSeverity}")
        }

        return DivergenceAlert(
            timestamp = System.currentTimeMillis(),
            rule = rule,
            result = result,
            action = rule.action,
            message = message,
            metadata = mapOf(
                "executionId" to result.executionId,
                "method" to result.methodName,
                "severity" to result.maxSeverity.toString()
            )
        )
    }

    /**
     * Log an alert
     */
    private fun logAlert(alert: DivergenceAlert) {
        val logLevel = when (alert.action) {
            AlertAction.LOG_ONLY -> Log.DEBUG
            AlertAction.NOTIFY -> Log.INFO
            AlertAction.ROLLBACK, AlertAction.CIRCUIT_BREAK, AlertAction.TERMINATE -> Log.ERROR
        }

        Log.println(logLevel, TAG, alert.message)
    }

    /**
     * Get alert statistics
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "totalAlerts" to alertCounts.values.sumOf { it.size },
            "ruleStats" to rules.mapValues { (name, rule) ->
                mapOf(
                    "enabled" to rule.enabled,
                    "alertCount" to (alertCounts[name]?.size ?: 0),
                    "lastAlert" to (lastAlertTimes[name]?.get() ?: 0)
                )
            },
            "circuitBreakerState" to circuitBreaker.getState().toString()
        )
    }

    /**
     * Reset alert system
     */
    fun reset() {
        lastAlertTimes.clear()
        alertCounts.clear()
        circuitBreaker.reset()
        Log.i(TAG, "Alert system reset")
    }

    companion object {
        private const val TAG = "DivergenceAlerts"
    }
}
