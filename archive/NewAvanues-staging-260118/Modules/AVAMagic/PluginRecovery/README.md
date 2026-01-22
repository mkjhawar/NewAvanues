# Plugin Failure Recovery Module

**Version:** 1.0.0
**Date:** 2025-11-19
**Author:** Manoj Jhawar (manoj@ideahq.net)

---

## Overview

Implements tiered escalation approach for plugin failures with automatic recovery, circuit breakers, and health monitoring.

### Escalation Tiers

1. **Retry** - Automatic retry with exponential backoff
2. **Placeholder** - Show placeholder UI while recovering
3. **Disable** - Disable plugin and continue
4. **Graceful Crash** - Show error UI, offer restart

---

## Quick Start

```kotlin
val recovery = PluginRecoveryManager()

// Register plugin
recovery.register(
    pluginId = "com.example.weather",
    strategy = RecoveryStrategy(
        maxRetries = 3,
        retryDelayMs = 1000,
        escalation = EscalationType.PLACEHOLDER_THEN_DISABLE
    )
)

// Handle failures
try {
    weatherPlugin.fetchData()
} catch (e: Exception) {
    val result = recovery.handleFailure(
        pluginId = "com.example.weather",
        error = e,
        context = mapOf("operation" to "fetchData")
    )

    when (result.action) {
        RecoveryAction.RETRY -> // Automatic retry in progress
        RecoveryAction.PLACEHOLDER -> showPlaceholder()
        RecoveryAction.DISABLE -> showDisabledMessage()
        RecoveryAction.CRASH -> showErrorScreen(result.error)
    }
}

// Monitor events
recovery.events.collect { event ->
    when (event) {
        is RecoveryEvent.PluginFailed -> log("Plugin failed")
        is RecoveryEvent.RecoverySuccess -> log("Recovered")
        is RecoveryEvent.PluginDisabled -> notifyUser()
    }
}
```

---

## Recovery Strategy

```kotlin
RecoveryStrategy(
    // Retry configuration
    maxRetries = 3,              // Retry attempts before escalating
    retryDelayMs = 1000,         // Initial delay
    maxRetryDelayMs = 30000,     // Max delay (exponential backoff)

    // Placeholder
    placeholderAttempts = 2,     // Attempts while showing placeholder
    placeholderMessage = "Loading...",

    // Escalation
    escalation = EscalationType.PLACEHOLDER_THEN_DISABLE,

    // Circuit breaker
    circuitBreakerThreshold = 5,  // Failures before opening
    circuitBreakerResetMs = 60000, // Reset after 60s

    // Manual recovery
    allowManualRecovery = true,

    // Custom recovery action
    recoveryAction = { pluginId, context ->
        // Attempt to recover
        plugins[pluginId]?.reinitialize()
    }
)
```

---

## Escalation Types

| Type | Behavior |
|------|----------|
| `DISABLE` | Disable immediately after retries |
| `GRACEFUL_CRASH` | Show error, offer restart |
| `PLACEHOLDER_THEN_DISABLE` | Placeholder → Disable |
| `PLACEHOLDER_THEN_CRASH` | Placeholder → Crash |

---

## Circuit Breaker

Prevents cascading failures:

```
CLOSED → (failures >= threshold) → OPEN
OPEN → (reset time passed) → HALF_OPEN
HALF_OPEN → (success) → CLOSED
HALF_OPEN → (failure) → OPEN
```

---

## Health Monitoring

### Single Plugin

```kotlin
val health = recovery.getPluginHealth("com.example.weather")
if (!health.healthy) {
    showWarning("Weather plugin is unhealthy")
}
```

### All Plugins

```kotlin
val result = recovery.healthCheck()
println("Healthy: ${result.healthyPlugins}/${result.totalPlugins}")
```

---

## Events

```kotlin
recovery.events.collect { event ->
    when (event) {
        is RecoveryEvent.PluginFailed -> {
            // Log failure
        }
        is RecoveryEvent.RetryScheduled -> {
            // Show retry indicator
        }
        is RecoveryEvent.RecoverySuccess -> {
            // Clear error state
        }
        is RecoveryEvent.PlaceholderShown -> {
            // Update UI
        }
        is RecoveryEvent.PluginDisabled -> {
            // Notify user
        }
        is RecoveryEvent.GracefulCrash -> {
            // Show error dialog
        }
        is RecoveryEvent.PluginReset -> {
            // Plugin recovered
        }
        is RecoveryEvent.CircuitBreakerOpen -> {
            // Fast-fail mode
        }
    }
}
```

---

## Manual Recovery

```kotlin
// Reset plugin after manual intervention
val success = recovery.resetPlugin("com.example.weather")
if (success) {
    // Plugin is active again
    weatherPlugin.fetchData()
}
```

---

## Plugin States

- `ACTIVE` - Normal operation
- `PLACEHOLDER` - Showing placeholder UI
- `DISABLED` - Plugin disabled
- `CRASHED` - Plugin crashed, requires restart

---

## Files

```
modules/AVAMagic/PluginRecovery/
├── build.gradle.kts
├── README.md
└── src/
    └── commonMain/
        └── kotlin/
            └── com/augmentalis/avamagic/plugin/
                └── PluginRecovery.kt
```

---

## Dependencies

- `kotlinx-serialization-json:1.6.0`
- `kotlinx-coroutines-core:1.7.3`

---

## License

Proprietary - Augmentalis ES

---

**IDEACODE Version:** 8.4
**Created by:** Manoj Jhawar (manoj@ideahq.net)
