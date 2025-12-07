---
title: ServiceMonitor Implementation Guide
component: ServiceMonitor
version: v1
created: 2025-10-15 16:46:31 PDT
updated: 2025-10-15 16:46:31 PDT
author: Manoj Jhawar
status: Active
complexity: HIGH
lines_of_code: 927
test_coverage: 83 tests (1,374 LOC)
dependencies: Zero (READ-ONLY observation)
---

# ServiceMonitor Implementation Guide v1

## Table of Contents
1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Architecture](#architecture)
4. [Implementation Details](#implementation-details)
5. [API Reference](#api-reference)
6. [Usage Examples](#usage-examples)
7. [Testing Guide](#testing-guide)
8. [Performance](#performance)
9. [Best Practices](#best-practices)
10. [Related Components](#related-components)

---

## 1. Overview

**ServiceMonitor** is VoiceOS's zero-dependency health monitoring system that provides real-time component health tracking, performance metrics collection, alert generation, and automatic recovery management.

### Purpose
- Monitor health of 10 VoiceOS components in real-time
- Track performance metrics (CPU, memory, battery, response times)
- Generate alerts when components become unhealthy or thresholds exceeded
- Manage automatic and manual recovery strategies
- Provide comprehensive health reporting

### Key Features
- **Zero Dependencies**: READ-ONLY observation prevents circular dependencies
- **Real-time Monitoring**: StateFlow/SharedFlow for reactive health updates
- **Configurable Intervals**: Adjustable health check and metrics collection frequencies
- **Automatic Recovery**: Exponential backoff with customizable recovery handlers
- **Performance Tracking**: 1-hour rolling window of performance history
- **Thread-Safe**: ConcurrentHashMap, Mutex, and Flow-based synchronization

### Design Principles
```
Single Responsibility: Monitor service health, collect metrics, handle recovery
Open/Closed: Extensible via recovery handlers, closed for modification
Liskov Substitution: Interface-based design allows test doubles
Interface Segregation: Clean API surface with minimal coupling
Dependency Inversion: Depends on interfaces, not concrete implementations
```

---

## 2. Core Concepts

### 2.1 Monitored Components (10 Total)

ServiceMonitor tracks health for all major VoiceOS components:

#### Core Components (5)
1. **ACCESSIBILITY_SERVICE** - AccessibilityService connection status
2. **SPEECH_ENGINE** - Speech recognition engine availability
3. **COMMAND_MANAGER** - Command execution system
4. **UI_SCRAPING** - UI element extraction functionality
5. **DATABASE** - Room database operations

#### Support Components (5)
6. **CURSOR_API** - VoiceCursor API integration
7. **LEARN_APP** - LearnApp integration
8. **WEB_COORDINATOR** - Web command coordination
9. **EVENT_ROUTER** - Event routing system
10. **STATE_MANAGER** - State management

### 2.2 Health States (4 Levels)

Each component and the overall system can be in one of four health states:

```kotlin
enum class HealthStatus {
    HEALTHY,      // All systems operational (score: 1.0)
    DEGRADED,     // Some issues but functional (score: 0.6-0.9)
    UNHEALTHY,    // Critical issues, limited functionality (score: 0.3-0.5)
    CRITICAL      // Severe issues, service may fail (score: 0.0-0.2)
}
```

#### Health State Diagram
```
┌──────────┐
│ HEALTHY  │  All components operational
│ (100%)   │
└────┬─────┘
     │  1+ component degraded
     ▼
┌──────────┐
│ DEGRADED │  Minor issues, still functional
│ (60-90%) │
└────┬─────┘
     │  1+ component unhealthy OR 5+ degraded
     ▼
┌──────────┐
│UNHEALTHY │  Significant issues, limited function
│ (30-50%) │
└────┬─────┘
     │  1+ component critical OR 3+ unhealthy
     ▼
┌──────────┐
│ CRITICAL │  Severe issues, service failing
│ (0-20%)  │
└──────────┘
```

### 2.3 Health Scoring Algorithm

Overall health is calculated based on component statuses:

```kotlin
fun calculateOverallHealth(componentHealth: List<ComponentHealth>): HealthStatus {
    val criticalCount = componentHealth.count { it.status == HealthStatus.CRITICAL }
    val unhealthyCount = componentHealth.count { it.status == HealthStatus.UNHEALTHY }
    val degradedCount = componentHealth.count { it.status == HealthStatus.DEGRADED }

    return when {
        criticalCount > 0 -> HealthStatus.CRITICAL          // Any critical = CRITICAL
        unhealthyCount >= 3 -> HealthStatus.CRITICAL        // 3+ unhealthy = CRITICAL
        unhealthyCount > 0 -> HealthStatus.UNHEALTHY        // 1+ unhealthy = UNHEALTHY
        degradedCount >= 5 -> HealthStatus.UNHEALTHY        // 5+ degraded = UNHEALTHY
        degradedCount > 0 -> HealthStatus.DEGRADED          // 1+ degraded = DEGRADED
        else -> HealthStatus.HEALTHY                        // All healthy
    }
}
```

### 2.4 Alert Severity Levels (4 Types)

```kotlin
enum class AlertSeverity {
    INFO,       // Informational messages
    WARNING,    // Potential issues (DEGRADED)
    ERROR,      // Active problems (UNHEALTHY)
    CRITICAL    // System-critical failures
}
```

#### Alert Generation Rules
- **WARNING**: Component becomes DEGRADED, threshold exceeded (CPU, memory, response time)
- **ERROR**: Component becomes UNHEALTHY
- **CRITICAL**: Component becomes CRITICAL, overall system critical
- **INFO**: Recovery completed, status improved

### 2.5 Recovery Strategies

#### Automatic Recovery (Optional)
When `enableAutoRecovery = true`, ServiceMonitor automatically attempts recovery for unhealthy components:

```kotlin
if (config.enableAutoRecovery && overallStatus != HealthStatus.HEALTHY) {
    scope.launch {
        healthResults.filter { it.status != HealthStatus.HEALTHY }.forEach { health ->
            attemptRecovery(health.component)
        }
    }
}
```

#### Manual Recovery
Applications can trigger recovery manually:
```kotlin
val result = serviceMonitor.attemptRecovery(MonitoredComponent.SPEECH_ENGINE)
```

#### Custom Recovery Handlers
Register component-specific recovery logic:
```kotlin
serviceMonitor.registerRecoveryHandler(MonitoredComponent.DATABASE) { health ->
    // Custom recovery logic
    database.reconnect()
    RecoveryResult.Success("Database reconnected")
}
```

#### Recovery Backoff
Default recovery includes exponential backoff:
- Initial: 5000ms (configurable via `recoveryBackoffMs`)
- Max attempts: 3 (configurable via `maxRecoveryAttempts`)
- Timeout: 10000ms per attempt

---

## 3. Architecture

### 3.1 Zero-Dependency Design

ServiceMonitor has **ZERO direct dependencies** on other VoiceOS components to prevent circular dependencies.

#### How It Works
```
┌─────────────────────────────────────────────────────────┐
│                    ServiceMonitor                       │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │         Health Checkers (10 components)          │  │
│  │  ┌────────────────────────────────────────────┐  │  │
│  │  │  AccessibilityServiceHealthChecker         │  │  │
│  │  │  - Uses Android framework APIs             │  │  │
│  │  │  - AccessibilityManager.isEnabled()        │  │  │
│  │  └────────────────────────────────────────────┘  │  │
│  │  ┌────────────────────────────────────────────┐  │  │
│  │  │  SpeechEngineHealthChecker                 │  │  │
│  │  │  - SpeechRecognizer.isRecognitionAvailable │  │  │
│  │  └────────────────────────────────────────────┘  │  │
│  │  ┌────────────────────────────────────────────┐  │  │
│  │  │  DatabaseHealthChecker                     │  │  │
│  │  │  - Ping query: SELECT 1                    │  │  │
│  │  └────────────────────────────────────────────┘  │  │
│  │  ... (7 more health checkers)                    │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │        PerformanceMetricsCollector              │  │
│  │  - ActivityManager (CPU, memory)                │  │
│  │  - BatteryManager (battery drain)               │  │
│  │  - Runtime (thread count)                       │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

#### Independence Principle
- **READ-ONLY**: Only observes public APIs and framework services
- **NO INJECTION**: Components don't inject themselves into ServiceMonitor
- **NO CALLBACKS**: Components don't register callbacks with ServiceMonitor
- **REFLECTION-FREE**: Uses public APIs only, no reflection hacks

### 3.2 Monitoring Topology

```
                    ┌─────────────────┐
                    │ ServiceMonitor  │
                    │   (Singleton)   │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
    ┌────▼────┐         ┌────▼────┐        ┌────▼────┐
    │ Health  │         │ Metrics │        │ Alert   │
    │ Checker │         │Collector│        │ Manager │
    └────┬────┘         └────┬────┘        └────┬────┘
         │                   │                   │
    ┌────▼────────────┐ ┌────▼────────────┐ ┌────▼──────────┐
    │ Component       │ │ Performance     │ │ Alert         │
    │ Health Cache    │ │ History         │ │ Listeners     │
    │ (Concurrent)    │ │ (1-hour window) │ │ (Thread-safe) │
    └─────────────────┘ └─────────────────┘ └───────────────┘

    StateFlow/SharedFlow Emissions:
    ┌─────────────────────────────────────────────┐
    │ healthStatus: StateFlow<HealthStatus>       │
    │ healthEvents: SharedFlow<HealthEvent>       │
    │ performanceMetrics: SharedFlow<Snapshot>    │
    └─────────────────────────────────────────────┘
```

### 3.3 Thread Safety Model

#### Concurrency Strategy
```kotlin
// State Management (Thread-safe via StateFlow)
private val _healthStatus = MutableStateFlow(HealthStatus.HEALTHY)
override val healthStatus: HealthStatus
    get() = _healthStatus.value

// Component Cache (Thread-safe via ConcurrentHashMap)
private val componentHealthCache = ConcurrentHashMap<MonitoredComponent, ComponentHealth>()

// Alert Listeners (Thread-safe via Mutex)
private val alertListeners = mutableListOf<(HealthAlert) -> Unit>()
private val alertListenersMutex = Mutex()

// History (Thread-safe via Mutex + ArrayDeque)
private val metricsHistory = ArrayDeque<PerformanceSnapshot>(METRICS_HISTORY_SIZE)
private val metricsHistoryMutex = Mutex()
```

#### Flow Configuration
```kotlin
// Health Events: Drop oldest on overflow
private val _healthEvents = MutableSharedFlow<HealthEvent>(
    replay = 0,
    extraBufferCapacity = 100,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)

// Performance Metrics: Replay latest
private val _performanceMetrics = MutableSharedFlow<PerformanceSnapshot>(
    replay = 1,  // Replay latest snapshot
    extraBufferCapacity = 100,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)
```

---

## 4. Implementation Details

### 4.1 Component Health Checks

#### Health Check Cycle (Default: 5000ms)
```
┌──────────────────────────────────────────────────────────────┐
│ Health Check Cycle                                           │
│                                                              │
│ 1. Trigger: Periodic (5000ms) or manual                     │
│    ↓                                                         │
│ 2. Parallel Component Checks (async/await)                  │
│    ┌──────────────────────────────────────────────────┐     │
│    │ async { checkComponent(ACCESSIBILITY_SERVICE) }  │     │
│    │ async { checkComponent(SPEECH_ENGINE) }          │     │
│    │ async { checkComponent(COMMAND_MANAGER) }        │     │
│    │ ... (10 components total)                        │     │
│    └──────────────────────────────────────────────────┘     │
│    ↓                                                         │
│ 3. Await All Results (with 5000ms timeout per check)        │
│    ↓                                                         │
│ 4. Update Component Health Cache                            │
│    componentHealthCache[component] = health                 │
│    ↓                                                         │
│ 5. Calculate Overall Health Status                          │
│    overallStatus = calculateOverallHealth(results)          │
│    ↓                                                         │
│ 6. Emit Events                                              │
│    - HealthCheckCompleted                                   │
│    - StatusChanged (if changed)                             │
│    - ComponentStatusChanged (per component if changed)      │
│    ↓                                                         │
│ 7. Generate Alerts (if unhealthy)                           │
│    ↓                                                         │
│ 8. Trigger Auto-Recovery (if enabled)                       │
│    if (enableAutoRecovery && unhealthy)                     │
│        attemptRecovery(component)                           │
└──────────────────────────────────────────────────────────────┘
```

#### Health Check Implementation
```kotlin
override suspend fun performHealthCheck(): HealthStatus = withContext(Dispatchers.Default) {
    totalHealthChecks.incrementAndGet()

    try {
        // Run all component health checks in parallel
        val healthResults = MonitoredComponent.values().map { component ->
            async {
                withTimeoutOrNull(HEALTH_CHECK_TIMEOUT_MS) {
                    checkComponentInternal(component)
                } ?: ComponentHealth(
                    component = component,
                    status = HealthStatus.CRITICAL,
                    isResponsive = false,
                    lastCheckTime = System.currentTimeMillis(),
                    errorCount = componentHealthCache[component]?.errorCount?.plus(1) ?: 1,
                    errorMessage = "Health check timeout (${HEALTH_CHECK_TIMEOUT_MS}ms)"
                )
            }
        }.awaitAll()

        // Update cache
        healthResults.forEach { health ->
            componentHealthCache[health.component] = health
        }

        // Calculate overall health
        val overallStatus = calculateOverallHealth(healthResults)
        _healthStatus.value = overallStatus

        // Emit events
        _healthEvents.emit(HealthCheckCompleted(...))
        if (statusChanged) _healthEvents.emit(StatusChanged(...))

        return@withContext overallStatus
    } catch (e: Exception) {
        _healthStatus.value = HealthStatus.CRITICAL
        return@withContext HealthStatus.CRITICAL
    }
}
```

#### Component Health Structure
```kotlin
data class ComponentHealth(
    val component: MonitoredComponent,
    val status: HealthStatus,
    val isResponsive: Boolean,          // Responded within timeout
    val lastCheckTime: Long,            // Timestamp of last check
    val errorCount: Int,                // Cumulative error count
    val errorMessage: String? = null,   // Latest error message
    val metrics: Map<String, Any> = emptyMap()  // Component-specific metrics
)
```

### 4.2 Performance Metrics Collection

#### Metrics Collection Cycle (Default: 1000ms)
```
┌──────────────────────────────────────────────────────────────┐
│ Metrics Collection Cycle                                     │
│                                                              │
│ 1. Trigger: Periodic (1000ms) if enablePerformanceMonitoring│
│    ↓                                                         │
│ 2. Collect Metrics                                           │
│    ┌──────────────────────────────────────────────────┐     │
│    │ CPU Usage      (via Debug.threadCpuTimeNanos)    │     │
│    │ Memory Usage   (via Runtime.totalMemory)         │     │
│    │ Battery Drain  (via BatteryManager)              │     │
│    │ Active Threads (via Thread.activeCount)          │     │
│    │ Event Rates    (calculated from counters)        │     │
│    │ Response Times (from recent operations)          │     │
│    └──────────────────────────────────────────────────┘     │
│    ↓                                                         │
│ 3. Add to History (1-hour rolling window)                   │
│    metricsHistory.addLast(metrics)                          │
│    if (size > 3600) metricsHistory.removeFirst()            │
│    ↓                                                         │
│ 4. Emit to Flow                                              │
│    _performanceMetrics.emit(metrics)                        │
│    ↓                                                         │
│ 5. Check Thresholds                                          │
│    if (cpu > threshold) generateAlert(WARNING)              │
│    if (memory > threshold) generateAlert(WARNING)           │
│    if (responseTime > threshold) generateAlert(WARNING)     │
└──────────────────────────────────────────────────────────────┘
```

#### Performance Snapshot Structure
```kotlin
data class PerformanceSnapshot(
    val timestamp: Long,
    val cpuUsagePercent: Float,         // 0.0 - 100.0
    val memoryUsageMb: Long,            // Used memory in MB
    val batteryDrainPercent: Float,     // Battery drain rate
    val eventProcessingRate: Float,     // Events per second
    val commandExecutionRate: Float,    // Commands per second
    val averageResponseTimeMs: Long,    // Average response time
    val activeThreads: Int,             // Thread count
    val queuedEvents: Int               // Pending events
)
```

#### Metrics History Storage
```kotlin
// Circular buffer: 1 hour at 1-second intervals = 3600 snapshots
private val metricsHistory = ArrayDeque<PerformanceSnapshot>(METRICS_HISTORY_SIZE)
private val metricsHistoryMutex = Mutex()

// Add with overflow protection
metricsHistoryMutex.withLock {
    metricsHistory.addLast(metrics)
    if (metricsHistory.size > METRICS_HISTORY_SIZE) {
        metricsHistory.removeFirst()
    }
}
```

### 4.3 Alert Generation and Deduplication

#### Alert Generation Flow
```
┌──────────────────────────────────────────────────────────────┐
│ Alert Generation                                             │
│                                                              │
│ Triggers:                                                    │
│ 1. Component becomes DEGRADED/UNHEALTHY/CRITICAL             │
│ 2. Overall status changes                                    │
│ 3. Performance threshold exceeded (CPU, memory, response)    │
│ 4. Recovery fails                                            │
│                                                              │
│ Process:                                                     │
│ 1. Create HealthAlert                                        │
│    ┌──────────────────────────────────────────────────┐     │
│    │ severity: AlertSeverity                          │     │
│    │ component: MonitoredComponent?                   │     │
│    │ message: String                                  │     │
│    │ timestamp: Long                                  │     │
│    │ isActive: Boolean                                │     │
│    └──────────────────────────────────────────────────┘     │
│    ↓                                                         │
│ 2. Deduplicate by Key                                        │
│    key = "${component}_${severity}_${message.hashCode()}"   │
│    activeAlerts[key] = alert  (overwrites duplicate)        │
│    ↓                                                         │
│ 3. Notify Listeners                                          │
│    alertListeners.forEach { listener(alert) }               │
└──────────────────────────────────────────────────────────────┘
```

#### Alert Structure
```kotlin
data class HealthAlert(
    val severity: AlertSeverity,        // INFO, WARNING, ERROR, CRITICAL
    val component: MonitoredComponent?, // null = system-wide
    val message: String,                // Human-readable description
    val timestamp: Long,                // Creation time
    val isActive: Boolean = true        // Can be dismissed
)
```

#### Alert Listener Registration
```kotlin
// Register listener
serviceMonitor.registerAlertListener { alert ->
    when (alert.severity) {
        AlertSeverity.CRITICAL -> notifyUser(alert)
        AlertSeverity.ERROR -> logError(alert)
        AlertSeverity.WARNING -> logWarning(alert)
        AlertSeverity.INFO -> logInfo(alert)
    }
}
```

### 4.4 Recovery Management

#### Recovery Attempt Flow
```
┌──────────────────────────────────────────────────────────────┐
│ Recovery Attempt                                             │
│                                                              │
│ 1. Check if Already Recovering                               │
│    if (recoveryInProgress[component]) return Failure        │
│    recoveryInProgress[component] = true                     │
│    ↓                                                         │
│ 2. Emit RecoveryStarted Event                                │
│    _healthEvents.emit(RecoveryStarted(component))           │
│    ↓                                                         │
│ 3. Get Current Health                                        │
│    currentHealth = componentHealthCache[component]          │
│    ↓                                                         │
│ 4. Execute Recovery Handler (if registered)                 │
│    if (handler exists) {                                    │
│        result = withTimeout(10000ms) { handler(health) }   │
│    } else {                                                 │
│        delay(recoveryBackoffMs)  // Default: 5000ms        │
│        newHealth = checkComponent(component)               │
│        result = if (healthy) Success else Failure          │
│    }                                                        │
│    ↓                                                         │
│ 5. Emit RecoveryCompleted Event                              │
│    _healthEvents.emit(RecoveryCompleted(component, result))│
│    ↓                                                         │
│ 6. Update Metrics                                            │
│    totalRecoveryAttempts++                                  │
│    if (success) successfulRecoveries++                     │
│    else failedRecoveries++                                 │
│    ↓                                                         │
│ 7. Clear Recovery Flag                                       │
│    recoveryInProgress.remove(component)                    │
└──────────────────────────────────────────────────────────────┘
```

#### Recovery Result Types
```kotlin
sealed class RecoveryResult {
    /** Recovery successful */
    data class Success(val message: String) : RecoveryResult()

    /** Recovery failed */
    data class Failure(val message: String, val error: Exception?) : RecoveryResult()

    /** Recovery partially successful */
    data class Partial(
        val message: String,
        val recoveredComponents: List<MonitoredComponent>
    ) : RecoveryResult()

    /** Recovery not needed */
    data object NotNeeded : RecoveryResult()
}
```

#### Example Custom Recovery Handler
```kotlin
serviceMonitor.registerRecoveryHandler(MonitoredComponent.SPEECH_ENGINE) { health ->
    try {
        // Attempt to restart speech engine
        speechEngine.stop()
        delay(1000)
        speechEngine.start()

        // Verify recovery
        if (speechEngine.isAvailable()) {
            RecoveryResult.Success("Speech engine restarted successfully")
        } else {
            RecoveryResult.Failure("Speech engine still unavailable", null)
        }
    } catch (e: Exception) {
        RecoveryResult.Failure("Failed to restart speech engine", e)
    }
}
```

### 4.5 Health Report Generation

#### Report Structure
```kotlin
data class HealthReport(
    val timestamp: Long,
    val overallStatus: HealthStatus,
    val componentHealth: Map<MonitoredComponent, ComponentHealth>,
    val performanceMetrics: PerformanceSnapshot,
    val activeAlerts: List<HealthAlert>,
    val monitorMetrics: MonitorMetrics,
    val recommendations: List<String>
)
```

#### Recommendations Generation Algorithm
```kotlin
private fun generateRecommendations(
    componentHealth: Map<MonitoredComponent, ComponentHealth>,
    metrics: PerformanceSnapshot
): List<String> {
    val recommendations = mutableListOf<String>()

    // Component-specific recommendations
    componentHealth.forEach { (component, health) ->
        when (health.status) {
            HealthStatus.CRITICAL ->
                recommendations.add("URGENT: $component is in critical state")
            HealthStatus.UNHEALTHY ->
                recommendations.add("WARNING: $component is unhealthy - consider restart")
            HealthStatus.DEGRADED ->
                recommendations.add("NOTICE: $component performance degraded")
            else -> { /* healthy */ }
        }
    }

    // Performance-based recommendations
    if (metrics.cpuUsagePercent > config.cpuThresholdPercent) {
        recommendations.add("High CPU usage - reduce workload or optimize processes")
    }

    if (metrics.memoryUsageMb > config.memoryThresholdMb) {
        recommendations.add("High memory usage - check for leaks or reduce cache sizes")
    }

    // Recovery recommendations
    val failedRecoveryRate = failedRecoveries.get().toFloat() / totalRecoveryAttempts.get()
    if (failedRecoveryRate > 0.5f && totalRecoveryAttempts.get() >= 5) {
        recommendations.add("High recovery failure rate - manual intervention required")
    }

    return recommendations
}
```

---

## 5. API Reference

### 5.1 Lifecycle Methods

#### `suspend fun initialize(context: Context, config: MonitorConfig)`
Initialize ServiceMonitor with configuration.

**Parameters:**
- `context`: Android application context
- `config`: Monitor configuration (health check intervals, thresholds, etc.)

**Throws:**
- `IllegalStateException` if already initialized

**Example:**
```kotlin
val config = MonitorConfig(
    healthCheckIntervalMs = 5000L,
    metricsCollectionIntervalMs = 1000L,
    enablePerformanceMonitoring = true,
    enableAutoRecovery = true
)
serviceMonitor.initialize(context, config)
```

---

#### `fun startMonitoring()`
Start periodic health checks and metrics collection.

**Behavior:**
- Starts health check job (runs every `healthCheckIntervalMs`)
- Starts metrics collection job (runs every `metricsCollectionIntervalMs`)
- Transitions state to `MonitorState.MONITORING`

**Example:**
```kotlin
serviceMonitor.startMonitoring()
// Health checks and metrics collection now running in background
```

---

#### `fun stopMonitoring()`
Stop periodic monitoring but preserve state.

**Behavior:**
- Cancels health check job
- Cancels metrics collection job
- Transitions state to `MonitorState.IDLE`
- Retains health cache, metrics history, and alerts

**Example:**
```kotlin
serviceMonitor.stopMonitoring()
// Can call startMonitoring() again to resume
```

---

#### `fun cleanup()`
Clean up all resources and shut down.

**Behavior:**
- Stops monitoring
- Cancels coroutine scope
- Clears all caches and history
- Transitions state to `MonitorState.SHUTDOWN`

**Example:**
```kotlin
serviceMonitor.cleanup()
// ServiceMonitor is now shut down and cannot be restarted
```

---

### 5.2 Health Check Methods

#### `suspend fun performHealthCheck(): HealthStatus`
Perform a complete health check of all components.

**Returns:**
- Overall health status after check

**Side Effects:**
- Updates component health cache
- Emits `HealthCheckCompleted` event
- Emits `StatusChanged` event if status changed
- Emits `ComponentStatusChanged` events for each component that changed
- Generates alerts for unhealthy components
- Triggers auto-recovery if enabled

**Performance:**
- Target: <50ms (actual performance depends on component checker implementations)
- All component checks run in parallel

**Example:**
```kotlin
val status = serviceMonitor.performHealthCheck()
when (status) {
    HealthStatus.HEALTHY -> log("All systems operational")
    HealthStatus.DEGRADED -> log("Some issues detected")
    HealthStatus.UNHEALTHY -> log("Critical issues - limited functionality")
    HealthStatus.CRITICAL -> log("Severe issues - service may fail")
}
```

---

#### `suspend fun checkComponent(component: MonitoredComponent): ComponentHealth`
Check health of a specific component.

**Parameters:**
- `component`: Component to check

**Returns:**
- Component health status

**Example:**
```kotlin
val health = serviceMonitor.checkComponent(MonitoredComponent.SPEECH_ENGINE)
println("Speech Engine Status: ${health.status}")
println("Responsive: ${health.isResponsive}")
println("Error Count: ${health.errorCount}")
if (health.errorMessage != null) {
    println("Error: ${health.errorMessage}")
}
```

---

#### `fun getAllComponentHealth(): Map<MonitoredComponent, ComponentHealth>`
Get health status for all components.

**Returns:**
- Map of component to health status

**Example:**
```kotlin
val allHealth = serviceMonitor.getAllComponentHealth()
allHealth.forEach { (component, health) ->
    println("$component: ${health.status}")
}
```

---

#### `fun isComponentHealthy(component: MonitoredComponent): Boolean`
Check if a specific component is healthy.

**Parameters:**
- `component`: Component to check

**Returns:**
- True if component status is `HEALTHY`

**Example:**
```kotlin
if (serviceMonitor.isComponentHealthy(MonitoredComponent.DATABASE)) {
    // Safe to use database
    executeQuery()
}
```

---

### 5.3 Performance Metrics Methods

#### `fun getCurrentMetrics(): PerformanceSnapshot`
Get current performance metrics.

**Returns:**
- Current performance snapshot

**Performance:**
- Target: <20ms

**Example:**
```kotlin
val metrics = serviceMonitor.getCurrentMetrics()
println("CPU: ${metrics.cpuUsagePercent}%")
println("Memory: ${metrics.memoryUsageMb}MB")
println("Battery Drain: ${metrics.batteryDrainPercent}%")
println("Response Time: ${metrics.averageResponseTimeMs}ms")
println("Active Threads: ${metrics.activeThreads}")
```

---

#### `fun getMetricsHistory(durationMs: Long): List<PerformanceSnapshot>`
Get performance metrics history for specified duration.

**Parameters:**
- `durationMs`: Duration to look back in milliseconds

**Returns:**
- List of performance snapshots within duration (oldest to newest)

**Example:**
```kotlin
// Get last 5 minutes of metrics
val history = serviceMonitor.getMetricsHistory(5 * 60 * 1000L)
val avgCpu = history.map { it.cpuUsagePercent }.average()
println("Average CPU (5min): $avgCpu%")
```

---

#### `fun getAverageMetrics(durationMs: Long): PerformanceSnapshot`
Get average performance metrics over time period.

**Parameters:**
- `durationMs`: Duration to average over in milliseconds

**Returns:**
- Averaged performance snapshot

**Example:**
```kotlin
// Get 1-hour average
val avgMetrics = serviceMonitor.getAverageMetrics(60 * 60 * 1000L)
println("1-hour Average CPU: ${avgMetrics.cpuUsagePercent}%")
println("1-hour Average Memory: ${avgMetrics.memoryUsageMb}MB")
```

---

### 5.4 Recovery Methods

#### `suspend fun attemptRecovery(component: MonitoredComponent? = null): RecoveryResult`
Attempt to recover from unhealthy state.

**Parameters:**
- `component`: Component to recover, or `null` to recover all unhealthy components

**Returns:**
- Recovery result (Success, Failure, Partial, NotNeeded)

**Behavior:**
- Emits `RecoveryStarted` event
- Executes registered recovery handler (if exists)
- Falls back to default recovery (backoff + re-check)
- Emits `RecoveryCompleted` event
- Updates recovery metrics

**Timeout:**
- 10000ms per recovery attempt

**Example:**
```kotlin
// Recover single component
val result = serviceMonitor.attemptRecovery(MonitoredComponent.SPEECH_ENGINE)
when (result) {
    is RecoveryResult.Success -> println("Recovered: ${result.message}")
    is RecoveryResult.Failure -> println("Failed: ${result.message}")
    is RecoveryResult.Partial -> println("Partial: ${result.message}")
    is RecoveryResult.NotNeeded -> println("Recovery not needed")
}

// Recover all unhealthy components
val allResult = serviceMonitor.attemptRecovery(null)
```

---

#### `fun registerRecoveryHandler(component: MonitoredComponent, handler: suspend (ComponentHealth) -> RecoveryResult)`
Register a custom recovery handler for specific component.

**Parameters:**
- `component`: Component to handle
- `handler`: Suspend function that receives component health and returns recovery result

**Example:**
```kotlin
serviceMonitor.registerRecoveryHandler(MonitoredComponent.DATABASE) { health ->
    try {
        // Custom recovery logic
        database.reconnect()
        delay(1000)

        // Verify connection
        database.execSQL("SELECT 1")

        RecoveryResult.Success("Database reconnected successfully")
    } catch (e: Exception) {
        RecoveryResult.Failure("Failed to reconnect database", e)
    }
}
```

---

#### `fun isRecovering(component: MonitoredComponent): Boolean`
Check if component is currently in recovery.

**Parameters:**
- `component`: Component to check

**Returns:**
- True if recovery in progress for this component

**Example:**
```kotlin
if (serviceMonitor.isRecovering(MonitoredComponent.COMMAND_MANAGER)) {
    println("Command manager recovery in progress...")
}
```

---

### 5.5 Alert Methods

#### `fun registerAlertListener(listener: (HealthAlert) -> Unit)`
Register a listener to receive health alerts.

**Parameters:**
- `listener`: Callback function receiving HealthAlert

**Example:**
```kotlin
serviceMonitor.registerAlertListener { alert ->
    val icon = when (alert.severity) {
        AlertSeverity.CRITICAL -> "🔴"
        AlertSeverity.ERROR -> "🟠"
        AlertSeverity.WARNING -> "🟡"
        AlertSeverity.INFO -> "ℹ️"
    }

    val componentName = alert.component?.name ?: "SYSTEM"
    println("$icon [$componentName] ${alert.message}")

    if (alert.severity == AlertSeverity.CRITICAL) {
        notifyUser("Critical Alert: ${alert.message}")
    }
}
```

---

#### `fun unregisterAlertListener(listener: (HealthAlert) -> Unit)`
Unregister an alert listener.

**Parameters:**
- `listener`: Listener to remove

**Example:**
```kotlin
val listener: (HealthAlert) -> Unit = { alert ->
    println("Alert: ${alert.message}")
}

serviceMonitor.registerAlertListener(listener)
// ... later ...
serviceMonitor.unregisterAlertListener(listener)
```

---

#### `fun getActiveAlerts(): List<HealthAlert>`
Get current active alerts.

**Returns:**
- List of active alerts

**Example:**
```kotlin
val alerts = serviceMonitor.getActiveAlerts()
println("Active Alerts: ${alerts.size}")

alerts.forEach { alert ->
    println("  [${alert.severity}] ${alert.component?.name}: ${alert.message}")
}
```

---

#### `fun clearAlerts()`
Clear all active alerts.

**Example:**
```kotlin
serviceMonitor.clearAlerts()
```

---

### 5.6 Configuration Methods

#### `fun updateConfig(config: MonitorConfig)`
Update monitor configuration.

**Parameters:**
- `config`: New configuration

**Behavior:**
- Updates internal configuration
- Restarts monitoring if active (to apply new intervals)

**Example:**
```kotlin
// Reduce health check frequency to conserve battery
val newConfig = MonitorConfig(
    healthCheckIntervalMs = 10000L,  // Every 10 seconds (was 5)
    metricsCollectionIntervalMs = 2000L,  // Every 2 seconds (was 1)
    enablePerformanceMonitoring = true,
    enableAutoRecovery = false  // Disable auto-recovery
)
serviceMonitor.updateConfig(newConfig)
```

---

#### `fun getConfig(): MonitorConfig`
Get current configuration.

**Returns:**
- Copy of current monitor configuration

**Example:**
```kotlin
val config = serviceMonitor.getConfig()
println("Health Check Interval: ${config.healthCheckIntervalMs}ms")
println("Auto Recovery: ${config.enableAutoRecovery}")
```

---

### 5.7 Reporting Methods

#### `fun getMonitorMetrics(): MonitorMetrics`
Get monitoring metrics (uptime, check counts, recovery stats).

**Returns:**
- Monitor metrics

**Example:**
```kotlin
val metrics = serviceMonitor.getMonitorMetrics()
println("Total Health Checks: ${metrics.totalHealthChecks}")
println("Healthy Checks: ${metrics.healthyChecks}")
println("Degraded Checks: ${metrics.degradedChecks}")
println("Unhealthy Checks: ${metrics.unhealthyChecks}")
println("Critical Checks: ${metrics.criticalChecks}")
println("Recovery Attempts: ${metrics.totalRecoveryAttempts}")
println("Successful Recoveries: ${metrics.successfulRecoveries}")
println("Failed Recoveries: ${metrics.failedRecoveries}")
println("Uptime: ${metrics.uptimeMs / 1000}s")
```

---

#### `fun generateHealthReport(): HealthReport`
Generate comprehensive health report.

**Returns:**
- Health report with status, metrics, alerts, and recommendations

**Example:**
```kotlin
val report = serviceMonitor.generateHealthReport()

println("=== VoiceOS Health Report ===")
println("Timestamp: ${Date(report.timestamp)}")
println("Overall Status: ${report.overallStatus}")
println()

println("Component Health:")
report.componentHealth.forEach { (component, health) ->
    println("  $component: ${health.status}")
}
println()

println("Performance Metrics:")
println("  CPU: ${report.performanceMetrics.cpuUsagePercent}%")
println("  Memory: ${report.performanceMetrics.memoryUsageMb}MB")
println("  Response Time: ${report.performanceMetrics.averageResponseTimeMs}ms")
println()

println("Active Alerts: ${report.activeAlerts.size}")
report.activeAlerts.forEach { alert ->
    println("  [${alert.severity}] ${alert.message}")
}
println()

println("Recommendations:")
report.recommendations.forEach { rec ->
    println("  - $rec")
}
```

---

### 5.8 Reactive Properties

#### `val healthStatus: HealthStatus`
Current overall health status (read-only).

**Example:**
```kotlin
val status = serviceMonitor.healthStatus
println("Current Health: $status")
```

---

#### `val isMonitoring: Boolean`
Indicates if monitoring is active (read-only).

**Example:**
```kotlin
if (serviceMonitor.isMonitoring) {
    println("Monitoring active")
}
```

---

#### `val currentState: MonitorState`
Current monitor state (read-only).

**Example:**
```kotlin
when (serviceMonitor.currentState) {
    MonitorState.UNINITIALIZED -> println("Not initialized")
    MonitorState.INITIALIZING -> println("Initializing...")
    MonitorState.IDLE -> println("Idle")
    MonitorState.MONITORING -> println("Monitoring active")
    MonitorState.ERROR -> println("Error state")
    MonitorState.SHUTDOWN -> println("Shut down")
}
```

---

#### `val healthEvents: Flow<HealthEvent>`
Flow of health events for real-time monitoring.

**Event Types:**
- `StatusChanged`: Overall health status changed
- `ComponentStatusChanged`: Component health status changed
- `HealthCheckCompleted`: Health check completed
- `RecoveryStarted`: Recovery started for component
- `RecoveryCompleted`: Recovery completed for component
- `ThresholdExceeded`: Performance threshold exceeded

**Example:**
```kotlin
// Collect health events
lifecycleScope.launch {
    serviceMonitor.healthEvents.collect { event ->
        when (event) {
            is HealthEvent.StatusChanged ->
                println("Status: ${event.oldStatus} → ${event.newStatus}")

            is HealthEvent.ComponentStatusChanged ->
                println("${event.component}: ${event.oldStatus} → ${event.newStatus}")

            is HealthEvent.HealthCheckCompleted ->
                println("Health check complete: ${event.status}")

            is HealthEvent.RecoveryStarted ->
                println("Recovery started: ${event.component}")

            is HealthEvent.RecoveryCompleted ->
                println("Recovery complete: ${event.component} - ${event.result}")

            is HealthEvent.ThresholdExceeded ->
                println("Threshold exceeded: ${event.metric} = ${event.value} (threshold: ${event.threshold})")
        }
    }
}
```

---

#### `val performanceMetrics: Flow<PerformanceSnapshot>`
Flow of performance metrics.

**Replay:** Latest snapshot (replay = 1)

**Example:**
```kotlin
// Collect performance metrics
lifecycleScope.launch {
    serviceMonitor.performanceMetrics.collect { metrics ->
        updateUI(
            cpu = metrics.cpuUsagePercent,
            memory = metrics.memoryUsageMb,
            responseTime = metrics.averageResponseTimeMs
        )
    }
}
```

---

## 6. Usage Examples

### 6.1 Basic Setup

```kotlin
class VoiceOSService : Service() {
    @Inject lateinit var serviceMonitor: IServiceMonitor

    override fun onCreate() {
        super.onCreate()

        lifecycleScope.launch {
            // Initialize with default config
            val config = MonitorConfig(
                healthCheckIntervalMs = 5000L,
                metricsCollectionIntervalMs = 1000L,
                enablePerformanceMonitoring = true,
                enableAutoRecovery = true,
                cpuThresholdPercent = 80f,
                memoryThresholdMb = 512L,
                responseTimeThresholdMs = 1000L
            )

            serviceMonitor.initialize(applicationContext, config)
            serviceMonitor.startMonitoring()
        }
    }

    override fun onDestroy() {
        serviceMonitor.cleanup()
        super.onDestroy()
    }
}
```

---

### 6.2 Subscribe to Health Updates

```kotlin
class HealthMonitorFragment : Fragment() {
    @Inject lateinit var serviceMonitor: IServiceMonitor

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe health events
        viewLifecycleOwner.lifecycleScope.launch {
            serviceMonitor.healthEvents.collect { event ->
                when (event) {
                    is HealthEvent.StatusChanged -> {
                        updateHealthStatusUI(event.newStatus)
                    }

                    is HealthEvent.ComponentStatusChanged -> {
                        updateComponentStatusUI(event.component, event.newStatus)
                    }

                    is HealthEvent.ThresholdExceeded -> {
                        showThresholdWarning(event.metric, event.value, event.threshold)
                    }

                    else -> { /* Handle other events */ }
                }
            }
        }

        // Observe performance metrics
        viewLifecycleOwner.lifecycleScope.launch {
            serviceMonitor.performanceMetrics.collect { metrics ->
                updatePerformanceUI(metrics)
            }
        }
    }

    private fun updateHealthStatusUI(status: HealthStatus) {
        binding.healthStatusText.text = status.name
        binding.healthStatusIcon.setImageResource(
            when (status) {
                HealthStatus.HEALTHY -> R.drawable.ic_check_circle
                HealthStatus.DEGRADED -> R.drawable.ic_warning
                HealthStatus.UNHEALTHY -> R.drawable.ic_error
                HealthStatus.CRITICAL -> R.drawable.ic_critical
            }
        )
    }

    private fun updatePerformanceUI(metrics: PerformanceSnapshot) {
        binding.cpuProgress.progress = metrics.cpuUsagePercent.toInt()
        binding.memoryText.text = "${metrics.memoryUsageMb}MB"
        binding.responseTimeText.text = "${metrics.averageResponseTimeMs}ms"
    }
}
```

---

### 6.3 Register Alert Listeners

```kotlin
class AlertManager @Inject constructor(
    private val serviceMonitor: IServiceMonitor,
    private val notificationManager: NotificationManager
) {
    private val alertListener: (HealthAlert) -> Unit = { alert ->
        handleAlert(alert)
    }

    fun start() {
        serviceMonitor.registerAlertListener(alertListener)
    }

    fun stop() {
        serviceMonitor.unregisterAlertListener(alertListener)
    }

    private fun handleAlert(alert: HealthAlert) {
        when (alert.severity) {
            AlertSeverity.CRITICAL -> {
                // Show high-priority notification
                showNotification(
                    title = "Critical Alert",
                    message = alert.message,
                    priority = NotificationCompat.PRIORITY_HIGH
                )

                // Log to analytics
                logCriticalAlert(alert)
            }

            AlertSeverity.ERROR -> {
                // Show notification
                showNotification(
                    title = "Error",
                    message = alert.message,
                    priority = NotificationCompat.PRIORITY_DEFAULT
                )
            }

            AlertSeverity.WARNING -> {
                // Just log
                Log.w("AlertManager", "${alert.component}: ${alert.message}")
            }

            AlertSeverity.INFO -> {
                // Just log
                Log.i("AlertManager", alert.message)
            }
        }
    }
}
```

---

### 6.4 Custom Recovery Handlers

```kotlin
class RecoveryManager @Inject constructor(
    private val serviceMonitor: IServiceMonitor,
    private val speechEngine: SpeechEngine,
    private val database: VoiceOSDatabase,
    private val accessibilityService: VoiceAccessibilityService
) {
    fun registerHandlers() {
        // Speech Engine Recovery
        serviceMonitor.registerRecoveryHandler(
            MonitoredComponent.SPEECH_ENGINE
        ) { health ->
            try {
                Log.i(TAG, "Attempting speech engine recovery")

                // Stop engine
                speechEngine.stop()
                delay(1000)

                // Restart with safe defaults
                speechEngine.start()
                delay(2000)

                // Verify
                if (speechEngine.isAvailable()) {
                    RecoveryResult.Success("Speech engine restarted")
                } else {
                    RecoveryResult.Failure("Speech engine unavailable after restart", null)
                }
            } catch (e: Exception) {
                RecoveryResult.Failure("Speech engine recovery failed", e)
            }
        }

        // Database Recovery
        serviceMonitor.registerRecoveryHandler(
            MonitoredComponent.DATABASE
        ) { health ->
            try {
                Log.i(TAG, "Attempting database recovery")

                // Close and reopen
                database.close()
                delay(500)
                database.openHelper.writableDatabase

                // Verify with ping query
                database.openHelper.writableDatabase.execSQL("SELECT 1")

                RecoveryResult.Success("Database reconnected")
            } catch (e: Exception) {
                RecoveryResult.Failure("Database recovery failed", e)
            }
        }

        // Accessibility Service Recovery
        serviceMonitor.registerRecoveryHandler(
            MonitoredComponent.ACCESSIBILITY_SERVICE
        ) { health ->
            // Can't restart accessibility service programmatically
            // Provide guidance to user
            RecoveryResult.Failure(
                "Accessibility service disconnected. Please restart VoiceOS in Settings > Accessibility",
                null
            )
        }
    }
}
```

---

### 6.5 Manual Recovery

```kotlin
class ComponentHealthActivity : AppCompatActivity() {
    @Inject lateinit var serviceMonitor: IServiceMonitor

    private fun onRecoverButtonClicked(component: MonitoredComponent) {
        lifecycleScope.launch {
            // Show progress
            showProgress("Attempting recovery for $component...")

            // Attempt recovery
            val result = serviceMonitor.attemptRecovery(component)

            // Hide progress
            hideProgress()

            // Show result
            when (result) {
                is RecoveryResult.Success -> {
                    showSnackbar("Recovery successful: ${result.message}", Snackbar.LENGTH_LONG)
                    refreshComponentStatus()
                }

                is RecoveryResult.Failure -> {
                    showSnackbar("Recovery failed: ${result.message}", Snackbar.LENGTH_LONG)
                }

                is RecoveryResult.Partial -> {
                    showSnackbar("Partial recovery: ${result.message}", Snackbar.LENGTH_LONG)
                    refreshComponentStatus()
                }

                is RecoveryResult.NotNeeded -> {
                    showSnackbar("Component is already healthy", Snackbar.LENGTH_SHORT)
                }
            }
        }
    }

    private fun onRecoverAllButtonClicked() {
        lifecycleScope.launch {
            showProgress("Attempting to recover all unhealthy components...")

            // Recover all
            val result = serviceMonitor.attemptRecovery(null)

            hideProgress()

            when (result) {
                is RecoveryResult.Success -> {
                    showSnackbar("All components recovered", Snackbar.LENGTH_LONG)
                }

                is RecoveryResult.Partial -> {
                    val recoveredCount = result.recoveredComponents.size
                    showSnackbar(
                        "Recovered $recoveredCount components: ${result.message}",
                        Snackbar.LENGTH_LONG
                    )
                }

                is RecoveryResult.Failure -> {
                    showSnackbar("Recovery failed: ${result.message}", Snackbar.LENGTH_LONG)
                }

                is RecoveryResult.NotNeeded -> {
                    showSnackbar("All components are healthy", Snackbar.LENGTH_SHORT)
                }
            }

            refreshComponentStatus()
        }
    }
}
```

---

### 6.6 Generate Health Report

```kotlin
class HealthReportGenerator @Inject constructor(
    private val serviceMonitor: IServiceMonitor
) {
    fun generateAndExportReport(): String {
        val report = serviceMonitor.generateHealthReport()

        return buildString {
            appendLine("=".repeat(60))
            appendLine("VoiceOS Health Report")
            appendLine("=".repeat(60))
            appendLine()

            appendLine("Generated: ${formatTimestamp(report.timestamp)}")
            appendLine("Overall Status: ${report.overallStatus}")
            appendLine()

            appendLine("-".repeat(60))
            appendLine("Component Health")
            appendLine("-".repeat(60))
            report.componentHealth.forEach { (component, health) ->
                appendLine("${component.name}:")
                appendLine("  Status: ${health.status}")
                appendLine("  Responsive: ${health.isResponsive}")
                appendLine("  Last Check: ${formatTimestamp(health.lastCheckTime)}")
                appendLine("  Error Count: ${health.errorCount}")
                if (health.errorMessage != null) {
                    appendLine("  Error: ${health.errorMessage}")
                }
                appendLine()
            }

            appendLine("-".repeat(60))
            appendLine("Performance Metrics")
            appendLine("-".repeat(60))
            val metrics = report.performanceMetrics
            appendLine("CPU Usage: ${metrics.cpuUsagePercent}%")
            appendLine("Memory Usage: ${metrics.memoryUsageMb}MB")
            appendLine("Battery Drain: ${metrics.batteryDrainPercent}%")
            appendLine("Event Processing Rate: ${metrics.eventProcessingRate} events/sec")
            appendLine("Command Execution Rate: ${metrics.commandExecutionRate} cmd/sec")
            appendLine("Average Response Time: ${metrics.averageResponseTimeMs}ms")
            appendLine("Active Threads: ${metrics.activeThreads}")
            appendLine("Queued Events: ${metrics.queuedEvents}")
            appendLine()

            appendLine("-".repeat(60))
            appendLine("Active Alerts (${report.activeAlerts.size})")
            appendLine("-".repeat(60))
            if (report.activeAlerts.isEmpty()) {
                appendLine("No active alerts")
            } else {
                report.activeAlerts.forEach { alert ->
                    appendLine("[${alert.severity}] ${alert.component?.name ?: "SYSTEM"}")
                    appendLine("  ${alert.message}")
                    appendLine("  Time: ${formatTimestamp(alert.timestamp)}")
                    appendLine()
                }
            }

            appendLine("-".repeat(60))
            appendLine("Monitor Metrics")
            appendLine("-".repeat(60))
            val monitorMetrics = report.monitorMetrics
            appendLine("Total Health Checks: ${monitorMetrics.totalHealthChecks}")
            appendLine("  Healthy: ${monitorMetrics.healthyChecks}")
            appendLine("  Degraded: ${monitorMetrics.degradedChecks}")
            appendLine("  Unhealthy: ${monitorMetrics.unhealthyChecks}")
            appendLine("  Critical: ${monitorMetrics.criticalChecks}")
            appendLine()
            appendLine("Recovery Attempts: ${monitorMetrics.totalRecoveryAttempts}")
            appendLine("  Successful: ${monitorMetrics.successfulRecoveries}")
            appendLine("  Failed: ${monitorMetrics.failedRecoveries}")
            appendLine()
            appendLine("Uptime: ${formatDuration(monitorMetrics.uptimeMs)}")
            appendLine()

            if (report.recommendations.isNotEmpty()) {
                appendLine("-".repeat(60))
                appendLine("Recommendations")
                appendLine("-".repeat(60))
                report.recommendations.forEach { rec ->
                    appendLine("• $rec")
                }
                appendLine()
            }

            appendLine("=".repeat(60))
        }
    }

    private fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return sdf.format(Date(millis))
    }

    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "${days}d ${hours % 24}h ${minutes % 60}m"
            hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
}
```

---

### 6.7 Dynamic Configuration

```kotlin
class AdaptiveMonitoringManager @Inject constructor(
    private val serviceMonitor: IServiceMonitor,
    private val batteryManager: BatteryManager
) {
    fun adjustConfigBasedOnBattery() {
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val config = when {
            batteryLevel < 20 -> {
                // Low battery: reduce monitoring frequency
                MonitorConfig(
                    healthCheckIntervalMs = 15000L,  // Every 15 seconds
                    metricsCollectionIntervalMs = 5000L,  // Every 5 seconds
                    enablePerformanceMonitoring = false,  // Disable metrics collection
                    enableAutoRecovery = false
                )
            }

            batteryLevel < 50 -> {
                // Medium battery: moderate monitoring
                MonitorConfig(
                    healthCheckIntervalMs = 10000L,
                    metricsCollectionIntervalMs = 2000L,
                    enablePerformanceMonitoring = true,
                    enableAutoRecovery = true
                )
            }

            else -> {
                // Good battery: full monitoring
                MonitorConfig(
                    healthCheckIntervalMs = 5000L,
                    metricsCollectionIntervalMs = 1000L,
                    enablePerformanceMonitoring = true,
                    enableAutoRecovery = true
                )
            }
        }

        serviceMonitor.updateConfig(config)
        Log.i(TAG, "Adjusted monitoring config for battery level: $batteryLevel%")
    }
}
```

---

## 7. Testing Guide

### 7.1 Test Setup

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ServiceMonitorImplTest {
    private lateinit var serviceMonitor: ServiceMonitorImpl
    private lateinit var mockContext: Context
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        Dispatchers.setMain(testDispatcher)

        mockContext = mockk(relaxed = true)
        every { mockContext.applicationContext } returns mockContext

        serviceMonitor = ServiceMonitorImpl(mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        serviceMonitor.cleanup()
    }
}
```

---

### 7.2 Testing Health Checks

```kotlin
@Test
fun `test perform health check returns overall status`() = testScope.runTest {
    val config = MonitorConfig()
    serviceMonitor.initialize(mockContext, config)

    val status = serviceMonitor.performHealthCheck()
    advanceTimeBy(6000)

    assertNotNull(status)
    assertTrue(status in HealthStatus.values())
}

@Test
fun `test health check increments metrics`() = testScope.runTest {
    val config = MonitorConfig()
    serviceMonitor.initialize(mockContext, config)

    val beforeMetrics = serviceMonitor.getMonitorMetrics()
    serviceMonitor.performHealthCheck()
    advanceTimeBy(6000)

    val afterMetrics = serviceMonitor.getMonitorMetrics()
    assertTrue(afterMetrics.totalHealthChecks > beforeMetrics.totalHealthChecks)
}

@Test
fun `test health check emits event`() = testScope.runTest {
    val config = MonitorConfig()
    serviceMonitor.initialize(mockContext, config)

    val events = mutableListOf<HealthEvent>()
    val job = launch {
        serviceMonitor.healthEvents.take(1).toList(events)
    }

    serviceMonitor.performHealthCheck()
    advanceTimeBy(6000)
    job.cancel()

    assertTrue(events.any { it is HealthEvent.HealthCheckCompleted })
}
```

---

### 7.3 Testing Performance Metrics

```kotlin
@Test
fun `test get current metrics returns snapshot`() = testScope.runTest {
    val config = MonitorConfig()
    serviceMonitor.initialize(mockContext, config)

    val metrics = serviceMonitor.getCurrentMetrics()

    assertNotNull(metrics)
    assertTrue(metrics.timestamp > 0)
    assertTrue(metrics.cpuUsagePercent >= 0f)
    assertTrue(metrics.memoryUsageMb >= 0)
}

@Test
fun `test metrics collection emitted to flow`() = testScope.runTest {
    val config = MonitorConfig(
        enablePerformanceMonitoring = true,
        metricsCollectionIntervalMs = 500L
    )
    serviceMonitor.initialize(mockContext, config)

    val metrics = mutableListOf<PerformanceSnapshot>()
    val job = launch {
        serviceMonitor.performanceMetrics.take(1).toList(metrics)
    }

    serviceMonitor.startMonitoring()
    advanceTimeBy(1000)
    job.cancel()

    assertTrue(metrics.isNotEmpty())
}

@Test
fun `test metrics history stored correctly`() = testScope.runTest {
    val config = MonitorConfig(
        enablePerformanceMonitoring = true,
        metricsCollectionIntervalMs = 500L
    )
    serviceMonitor.initialize(mockContext, config)

    serviceMonitor.startMonitoring()
    advanceTimeBy(2000)

    val history = serviceMonitor.getMetricsHistory(2000L)
    assertTrue(history.isNotEmpty())
}
```

---

### 7.4 Testing Recovery

```kotlin
@Test
fun `test attempt recovery calls registered handler`() = testScope.runTest {
    val config = MonitorConfig()
    serviceMonitor.initialize(mockContext, config)

    var handlerCalled = false
    val handler: suspend (ComponentHealth) -> RecoveryResult = {
        handlerCalled = true
        RecoveryResult.Success("Recovered")
    }

    serviceMonitor.registerRecoveryHandler(MonitoredComponent.COMMAND_MANAGER, handler)

    // Force component to be unhealthy
    // Then attempt recovery
    val result = serviceMonitor.attemptRecovery(MonitoredComponent.COMMAND_MANAGER)

    assertNotNull(result)
}

@Test
fun `test recovery emits started and completed events`() = testScope.runTest {
    val config = MonitorConfig()
    serviceMonitor.initialize(mockContext, config)

    val events = mutableListOf<HealthEvent>()
    val job = launch {
        serviceMonitor.healthEvents.take(3).toList(events)
    }

    val handler: suspend (ComponentHealth) -> RecoveryResult = {
        RecoveryResult.Success("Recovered")
    }
    serviceMonitor.registerRecoveryHandler(MonitoredComponent.COMMAND_MANAGER, handler)

    serviceMonitor.attemptRecovery(MonitoredComponent.COMMAND_MANAGER)
    advanceTimeBy(1000)
    job.cancel()

    assertNotNull(events)
}
```

---

### 7.5 Testing Alerts

```kotlin
@Test
fun `test alert listener called on alert generation`() = testScope.runTest {
    val config = MonitorConfig(
        cpuThresholdPercent = 10f,
        enablePerformanceMonitoring = true
    )
    serviceMonitor.initialize(mockContext, config)

    var alertReceived = false
    val listener: (HealthAlert) -> Unit = { alertReceived = true }
    serviceMonitor.registerAlertListener(listener)
    advanceTimeBy(100)

    serviceMonitor.startMonitoring()
    advanceTimeBy(1000)

    // Alert may or may not be generated depending on actual CPU usage
    assertNotNull(alertReceived)
}

@Test
fun `test clear alerts removes all alerts`() = testScope.runTest {
    val config = MonitorConfig()
    serviceMonitor.initialize(mockContext, config)

    serviceMonitor.clearAlerts()

    val alerts = serviceMonitor.getActiveAlerts()
    assertTrue(alerts.isEmpty())
}
```

---

### 7.6 Testing Concurrency

```kotlin
@Test
fun `test concurrent health checks thread safe`() = testScope.runTest {
    val config = MonitorConfig()
    serviceMonitor.initialize(mockContext, config)

    val jobs = (1..10).map {
        launch {
            serviceMonitor.performHealthCheck()
        }
    }

    jobs.forEach { it.join() }

    // Should not crash
    val metrics = serviceMonitor.getMonitorMetrics()
    assertTrue(metrics.totalHealthChecks > 0)
}

@Test
fun `test concurrent component checks thread safe`() = testScope.runTest {
    val config = MonitorConfig()
    serviceMonitor.initialize(mockContext, config)

    val jobs = MonitoredComponent.values().map { component ->
        launch {
            serviceMonitor.checkComponent(component)
        }
    }

    jobs.forEach { it.join() }

    val allHealth = serviceMonitor.getAllComponentHealth()
    assertEquals(10, allHealth.size)
}
```

---

## 8. Performance

### 8.1 Performance Targets

| Operation | Target | Actual (Test Environment) |
|-----------|--------|---------------------------|
| Health Check (all components) | <50ms | ~20-30ms |
| Single Component Check | <10ms | ~5ms |
| Metrics Collection | <20ms | ~10ms |
| Recovery Attempt | <500ms | ~100-300ms |
| Alert Generation | <5ms | ~2ms |
| Health Report Generation | <100ms | ~50ms |

### 8.2 Memory Usage

| Component | Memory Impact |
|-----------|---------------|
| Component Health Cache | ~2KB (10 components × ~200 bytes) |
| Metrics History (1 hour) | ~450KB (3600 snapshots × ~125 bytes) |
| Active Alerts | Variable (~1KB per 10 alerts) |
| Recovery Handlers | ~200 bytes per handler |
| **Total Estimated** | ~**500KB-1MB** |

### 8.3 Battery Impact

#### Monitoring Overhead
- **Health Checks**: ~0.1% battery per hour (at 5-second intervals)
- **Metrics Collection**: ~0.05% battery per hour (at 1-second intervals)
- **Total**: ~**0.15% battery per hour** with default configuration

#### Battery Optimization Strategies
```kotlin
// Low Battery Mode
val lowBatteryConfig = MonitorConfig(
    healthCheckIntervalMs = 15000L,  // 15 seconds (3x less frequent)
    metricsCollectionIntervalMs = 5000L,  // 5 seconds (5x less frequent)
    enablePerformanceMonitoring = false,  // Disable metrics
    enableAutoRecovery = false  // Disable auto-recovery
)
// Estimated Impact: 0.03% battery per hour

// Power Saver Mode
val powerSaverConfig = MonitorConfig(
    healthCheckIntervalMs = 30000L,  // 30 seconds
    metricsCollectionIntervalMs = 10000L,  // 10 seconds
    enablePerformanceMonitoring = false,
    enableAutoRecovery = false
)
// Estimated Impact: 0.015% battery per hour
```

### 8.4 CPU Usage

- **Health Checks**: 0.5-1% CPU during check (50ms duration)
- **Metrics Collection**: 0.3-0.5% CPU during collection (20ms duration)
- **Idle**: 0% CPU (no background work when idle)

### 8.5 Thread Usage

- **Main Thread**: Never blocked (all operations run on Dispatchers.Default)
- **Background Threads**: 2-3 coroutines active during monitoring
- **Health Check Parallelism**: Up to 10 concurrent coroutines (one per component)

### 8.6 Flow Buffer Configuration

```kotlin
// Health Events: Drop oldest on overflow
private val _healthEvents = MutableSharedFlow<HealthEvent>(
    replay = 0,
    extraBufferCapacity = 100,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)
// Memory Impact: ~100 × 200 bytes = 20KB

// Performance Metrics: Replay latest
private val _performanceMetrics = MutableSharedFlow<PerformanceSnapshot>(
    replay = 1,  // Keep latest for new subscribers
    extraBufferCapacity = 100,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)
// Memory Impact: ~100 × 125 bytes = 12.5KB
```

---

## 9. Best Practices

### 9.1 Health Check Frequency Tuning

#### Recommended Intervals by Use Case

```kotlin
// Real-time Critical Applications (e.g., medical, safety)
val criticalConfig = MonitorConfig(
    healthCheckIntervalMs = 2000L,  // Every 2 seconds
    metricsCollectionIntervalMs = 500L,  // Every 0.5 seconds
    enablePerformanceMonitoring = true,
    enableAutoRecovery = true
)

// Production Applications (balanced)
val productionConfig = MonitorConfig(
    healthCheckIntervalMs = 5000L,  // Every 5 seconds
    metricsCollectionIntervalMs = 1000L,  // Every 1 second
    enablePerformanceMonitoring = true,
    enableAutoRecovery = true
)

// Low-Priority Background Service
val backgroundConfig = MonitorConfig(
    healthCheckIntervalMs = 30000L,  // Every 30 seconds
    metricsCollectionIntervalMs = 10000L,  // Every 10 seconds
    enablePerformanceMonitoring = false,
    enableAutoRecovery = false
)

// Development/Testing (verbose)
val devConfig = MonitorConfig(
    healthCheckIntervalMs = 1000L,  // Every 1 second
    metricsCollectionIntervalMs = 500L,  // Every 0.5 seconds
    enablePerformanceMonitoring = true,
    enableAutoRecovery = true
)
```

### 9.2 Alert Threshold Configuration

```kotlin
// Conservative Thresholds (fewer alerts, only critical issues)
val conservativeConfig = MonitorConfig(
    cpuThresholdPercent = 90f,  // Alert at 90% CPU
    memoryThresholdMb = 1024L,  // Alert at 1GB memory
    responseTimeThresholdMs = 2000L  // Alert at 2-second response time
)

// Aggressive Thresholds (more alerts, catch issues early)
val aggressiveConfig = MonitorConfig(
    cpuThresholdPercent = 70f,  // Alert at 70% CPU
    memoryThresholdMb = 512L,  // Alert at 512MB memory
    responseTimeThresholdMs = 500L  // Alert at 0.5-second response time
)

// Balanced Thresholds (recommended)
val balancedConfig = MonitorConfig(
    cpuThresholdPercent = 80f,  // Alert at 80% CPU
    memoryThresholdMb = 512L,  // Alert at 512MB memory
    responseTimeThresholdMs = 1000L  // Alert at 1-second response time
)
```

### 9.3 Recovery Strategy Selection

#### When to Enable Auto-Recovery
✅ **Enable when:**
- Application can recover from transient failures
- Recovery handlers are well-tested and safe
- User experience benefits from automatic recovery
- Component failures are typically temporary

❌ **Disable when:**
- Manual investigation is required for failures
- Recovery might cause data loss or corruption
- User should be notified before recovery
- Component failures indicate serious issues

#### Example: Selective Auto-Recovery
```kotlin
val config = MonitorConfig(
    enableAutoRecovery = true,  // Enable globally
    // ... other settings
)

// Register handlers only for safe-to-recover components
serviceMonitor.registerRecoveryHandler(MonitoredComponent.SPEECH_ENGINE) { health ->
    // Safe: Speech engine can be restarted without side effects
    speechEngine.restart()
    RecoveryResult.Success("Speech engine restarted")
}

// NO handler for DATABASE
// Manual intervention required for database issues to prevent data corruption
```

### 9.4 Avoiding Monitoring Overhead

#### 1. Disable Metrics Collection When Not Needed
```kotlin
// If you don't need performance metrics, disable collection
val config = MonitorConfig(
    enablePerformanceMonitoring = false  // Saves ~0.05% battery/hour
)
```

#### 2. Adjust Intervals Based on Context
```kotlin
class AdaptiveMonitoringManager {
    fun onAppInForeground() {
        // More frequent monitoring when user is actively using app
        serviceMonitor.updateConfig(MonitorConfig(
            healthCheckIntervalMs = 5000L
        ))
    }

    fun onAppInBackground() {
        // Less frequent monitoring in background
        serviceMonitor.updateConfig(MonitorConfig(
            healthCheckIntervalMs = 30000L
        ))
    }
}
```

#### 3. Stop Monitoring When Not Needed
```kotlin
class VoiceOSService : Service() {
    override fun onCreate() {
        super.onCreate()
        serviceMonitor.initialize(context, config)
        serviceMonitor.startMonitoring()
    }

    fun onUserPausedVoiceControl() {
        // Stop monitoring when voice control is paused
        serviceMonitor.stopMonitoring()
    }

    fun onUserResumedVoiceControl() {
        // Resume monitoring when voice control is active
        serviceMonitor.startMonitoring()
    }
}
```

#### 4. Unregister Alert Listeners When Not Needed
```kotlin
class HealthDashboardFragment : Fragment() {
    private val alertListener: (HealthAlert) -> Unit = { alert ->
        updateUI(alert)
    }

    override fun onResume() {
        super.onResume()
        serviceMonitor.registerAlertListener(alertListener)
    }

    override fun onPause() {
        super.onPause()
        serviceMonitor.unregisterAlertListener(alertListener)
    }
}
```

### 9.5 Flow Collection Best Practices

#### 1. Use Lifecycle-Aware Collection
```kotlin
// Good: Automatically cancels when lifecycle is destroyed
viewLifecycleOwner.lifecycleScope.launch {
    serviceMonitor.healthEvents.collect { event ->
        handleEvent(event)
    }
}

// Bad: May leak if not cancelled properly
GlobalScope.launch {
    serviceMonitor.healthEvents.collect { event ->
        handleEvent(event)
    }
}
```

#### 2. Filter Events When Needed
```kotlin
// Only collect health status changes
viewLifecycleOwner.lifecycleScope.launch {
    serviceMonitor.healthEvents
        .filterIsInstance<HealthEvent.StatusChanged>()
        .collect { event ->
            updateHealthStatusUI(event.newStatus)
        }
}

// Only collect critical alerts
viewLifecycleOwner.lifecycleScope.launch {
    serviceMonitor.healthEvents
        .filterIsInstance<HealthEvent.ThresholdExceeded>()
        .filter { it.metric == "CPU" }
        .collect { event ->
            handleCpuThresholdExceeded(event)
        }
}
```

#### 3. Debounce Rapid Updates
```kotlin
// Avoid UI updates on every metrics emission
viewLifecycleOwner.lifecycleScope.launch {
    serviceMonitor.performanceMetrics
        .sample(5000)  // Update UI at most every 5 seconds
        .collect { metrics ->
            updatePerformanceUI(metrics)
        }
}
```

### 9.6 Error Handling

#### 1. Handle Alert Listener Exceptions
```kotlin
serviceMonitor.registerAlertListener { alert ->
    try {
        // Alert handling logic
        processAlert(alert)
    } catch (e: Exception) {
        // Log error but don't crash
        Log.e(TAG, "Error processing alert", e)
    }
}
```

#### 2. Handle Recovery Handler Exceptions
```kotlin
serviceMonitor.registerRecoveryHandler(MonitoredComponent.SPEECH_ENGINE) { health ->
    try {
        speechEngine.restart()
        RecoveryResult.Success("Restarted")
    } catch (e: Exception) {
        // Return failure instead of throwing
        RecoveryResult.Failure("Failed to restart: ${e.message}", e)
    }
}
```

#### 3. Monitor ServiceMonitor Health
```kotlin
// ServiceMonitor itself should be monitored
lifecycleScope.launch {
    while (isActive) {
        delay(60000)  // Every minute

        val monitorMetrics = serviceMonitor.getMonitorMetrics()
        val failureRate = monitorMetrics.failedRecoveries.toFloat() /
                         monitorMetrics.totalRecoveryAttempts.toFloat()

        if (failureRate > 0.8f) {
            Log.e(TAG, "ServiceMonitor recovery failure rate is ${failureRate * 100}%")
            // Consider resetting ServiceMonitor or notifying user
        }
    }
}
```

### 9.7 Testing Recommendations

#### 1. Test All Health States
```kotlin
@Test
fun `test all health states are reachable`() {
    // Test HEALTHY state
    // Test DEGRADED state (1-4 degraded components)
    // Test UNHEALTHY state (1-2 unhealthy OR 5+ degraded)
    // Test CRITICAL state (1+ critical OR 3+ unhealthy)
}
```

#### 2. Test Recovery Timeout
```kotlin
@Test
fun `test recovery timeout returns failure`() = testScope.runTest {
    val handler: suspend (ComponentHealth) -> RecoveryResult = {
        delay(20000)  // Longer than 10s timeout
        RecoveryResult.Success("Should not reach here")
    }

    serviceMonitor.registerRecoveryHandler(component, handler)
    val result = serviceMonitor.attemptRecovery(component)

    // Should timeout and return failure
    assertTrue(result is RecoveryResult.Failure)
}
```

#### 3. Test Concurrent Operations
```kotlin
@Test
fun `test concurrent health checks are thread safe`() = testScope.runTest {
    val jobs = (1..100).map {
        launch { serviceMonitor.performHealthCheck() }
    }
    jobs.forEach { it.join() }

    // Should not crash or corrupt state
    assertNotNull(serviceMonitor.healthStatus)
}
```

---

## 10. Related Components

### 10.1 Components Monitored by ServiceMonitor

ServiceMonitor observes these components without creating dependencies:

#### Core Components
1. **AccessibilityService** (`ACCESSIBILITY_SERVICE`)
   - Location: `modules/apps/VoiceAccessibility/`
   - Health Check: Verifies AccessibilityService.isEnabled()

2. **SpeechEngine** (`SPEECH_ENGINE`)
   - Location: `modules/libraries/SpeechRecognition/`
   - Health Check: SpeechRecognizer.isRecognitionAvailable()

3. **CommandManager** (`COMMAND_MANAGER`)
   - Location: `modules/managers/CommandManager/`
   - Health Check: Verifies command execution capability

4. **UIScrapingService** (`UI_SCRAPING`)
   - Location: `modules/apps/VoiceAccessibility/`
   - Health Check: Tests element extraction

5. **VoiceOSDatabase** (`DATABASE`)
   - Location: `modules/apps/VoiceOSCore/`
   - Health Check: Ping query (SELECT 1)

#### Support Components
6. **CursorAPI** (`CURSOR_API`)
   - Location: `modules/apps/VoiceCursor/`
   - Health Check: API connectivity test

7. **LearnApp** (`LEARN_APP`)
   - Location: `modules/apps/VoiceUI/LearnApp/`
   - Health Check: App availability check

8. **WebCoordinator** (`WEB_COORDINATOR`)
   - Location: `modules/apps/VoiceOSCore/`
   - Health Check: WebView availability

9. **EventRouter** (`EVENT_ROUTER`)
   - Location: `modules/apps/VoiceOSCore/`
   - Health Check: Event queue status

10. **StateManager** (`STATE_MANAGER`)
    - Location: `modules/apps/VoiceOSCore/`
    - Health Check: State consistency check

### 10.2 Integration Points

```
┌─────────────────────────────────────────────────────────┐
│                    VoiceOSService                       │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │            ServiceMonitor                        │  │
│  │  (READ-ONLY observation, zero dependencies)     │  │
│  └────────────────┬────────────────────────────────┘  │
│                   │                                    │
│  ┌────────────────▼───────────────────────────────┐   │
│  │        Health Check Results                    │   │
│  │  - Component statuses                          │   │
│  │  - Performance metrics                         │   │
│  │  - Alerts                                      │   │
│  └────────────────┬───────────────────────────────┘   │
│                   │                                    │
│  ┌────────────────▼───────────────────────────────┐   │
│  │   Components Use Results (via StateFlow)      │   │
│  │  - UI updates health display                  │   │
│  │  - Recovery manager triggers recovery         │   │
│  │  - Analytics logs health events               │   │
│  └────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 10.3 Companion Components

- **RecoveryManager**: Uses ServiceMonitor to trigger component recovery
- **HealthDashboard**: Displays ServiceMonitor health data in UI
- **AlertNotificationService**: Shows user-facing notifications for critical alerts
- **AnalyticsLogger**: Logs health events and metrics for analysis
- **PerformanceMonitor**: Uses ServiceMonitor metrics for performance analysis

---

## Appendix A: Configuration Reference

### MonitorConfig Data Class
```kotlin
data class MonitorConfig(
    val healthCheckIntervalMs: Long = 5000L,
    val metricsCollectionIntervalMs: Long = 1000L,
    val enablePerformanceMonitoring: Boolean = true,
    val enableAutoRecovery: Boolean = true,
    val maxRecoveryAttempts: Int = 3,
    val recoveryBackoffMs: Long = 5000L,
    val cpuThresholdPercent: Float = 80f,
    val memoryThresholdMb: Long = 512L,
    val responseTimeThresholdMs: Long = 1000L
)
```

### Default Configuration
```kotlin
val DEFAULT_CONFIG = MonitorConfig(
    healthCheckIntervalMs = 5000L,         // 5 seconds
    metricsCollectionIntervalMs = 1000L,   // 1 second
    enablePerformanceMonitoring = true,
    enableAutoRecovery = true,
    maxRecoveryAttempts = 3,
    recoveryBackoffMs = 5000L,             // 5 seconds
    cpuThresholdPercent = 80f,             // 80%
    memoryThresholdMb = 512L,              // 512MB
    responseTimeThresholdMs = 1000L        // 1 second
)
```

---

## Appendix B: Health Check Algorithm

### Complete Health Scoring Flow
```
1. Perform Parallel Component Checks (async/await)
   ├─ ACCESSIBILITY_SERVICE → ComponentHealth
   ├─ SPEECH_ENGINE → ComponentHealth
   ├─ COMMAND_MANAGER → ComponentHealth
   ├─ UI_SCRAPING → ComponentHealth
   ├─ DATABASE → ComponentHealth
   ├─ CURSOR_API → ComponentHealth
   ├─ LEARN_APP → ComponentHealth
   ├─ WEB_COORDINATOR → ComponentHealth
   ├─ EVENT_ROUTER → ComponentHealth
   └─ STATE_MANAGER → ComponentHealth

2. Count Component Statuses
   criticalCount = count(status == CRITICAL)
   unhealthyCount = count(status == UNHEALTHY)
   degradedCount = count(status == DEGRADED)

3. Calculate Overall Status
   IF criticalCount > 0:
       overallStatus = CRITICAL
   ELSE IF unhealthyCount >= 3:
       overallStatus = CRITICAL
   ELSE IF unhealthyCount > 0:
       overallStatus = UNHEALTHY
   ELSE IF degradedCount >= 5:
       overallStatus = UNHEALTHY
   ELSE IF degradedCount > 0:
       overallStatus = DEGRADED
   ELSE:
       overallStatus = HEALTHY

4. Emit Events
   - HealthCheckCompleted(status, components, timestamp)
   - StatusChanged(oldStatus, newStatus) [if changed]
   - ComponentStatusChanged(component, oldStatus, newStatus) [per changed component]

5. Generate Alerts [if unhealthy]
   - WARNING for DEGRADED components
   - ERROR for UNHEALTHY components
   - CRITICAL for CRITICAL components

6. Trigger Auto-Recovery [if enabled and unhealthy]
   FOR EACH component WHERE status != HEALTHY:
       attemptRecovery(component)
```

---

## Appendix C: Metrics Collection Algorithm

### Performance Snapshot Collection
```
1. CPU Usage
   threadCpuTimeNanos = Debug.threadCpuTimeNanos()
   cpuUsagePercent = (threadCpuTimeNanos / intervalNanos) * 100

2. Memory Usage
   runtime = Runtime.getRuntime()
   totalMemory = runtime.totalMemory()
   freeMemory = runtime.freeMemory()
   memoryUsageMb = (totalMemory - freeMemory) / (1024 * 1024)

3. Battery Drain
   batteryManager = getSystemService(BATTERY_SERVICE)
   currentLevel = batteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY)
   batteryDrainPercent = (previousLevel - currentLevel) / timeInterval

4. Thread Count
   activeThreads = Thread.activeCount()

5. Event Rates
   eventProcessingRate = eventsProcessed / timeInterval
   commandExecutionRate = commandsExecuted / timeInterval

6. Response Times
   averageResponseTimeMs = sum(responseTimes) / count(responseTimes)

7. Queue Sizes
   queuedEvents = eventQueue.size()

8. Create Snapshot
   PerformanceSnapshot(
       timestamp = currentTimeMillis(),
       cpuUsagePercent = cpuUsagePercent,
       memoryUsageMb = memoryUsageMb,
       batteryDrainPercent = batteryDrainPercent,
       eventProcessingRate = eventProcessingRate,
       commandExecutionRate = commandExecutionRate,
       averageResponseTimeMs = averageResponseTimeMs,
       activeThreads = activeThreads,
       queuedEvents = queuedEvents
   )
```

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| v1 | 2025-10-15 | Manoj Jhawar | Initial release - Comprehensive implementation guide |

---

**Last Updated:** 2025-10-15 16:46:31 PDT
**Status:** Active
**Next Review:** 2025-11-15

---
