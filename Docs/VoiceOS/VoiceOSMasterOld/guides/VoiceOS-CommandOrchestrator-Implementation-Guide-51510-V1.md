---
title: CommandOrchestrator Implementation Guide
version: v1
status: Living Document
created: 2025-10-15 16:42:48 PDT
last_updated: 2025-10-15 16:42:48 PDT
author: Claude Code (Anthropic)
component: CommandOrchestrator
module: VoiceOSCore
complexity: HIGH
lines_of_code: 745
test_coverage: 78 tests (1,655 LOC)
related_docs:
  - /docs/voiceos-master/architecture/VoiceOSService-SOLID-Refactoring-Summary-251015-0011.md
  - /docs/voiceos-master/implementation/Option4-CommandManager-Implementation-Plan-251015-0152.md
  - /docs/modules/command-manager/architecture/CommandManager-Centralized-Repository-Decision-251014-2347.md
---

# CommandOrchestrator Implementation Guide v1

## Table of Contents

1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Architecture](#architecture)
4. [Implementation Details](#implementation-details)
5. [API Reference](#api-reference)
6. [Usage Examples](#usage-examples)
7. [Testing Guide](#testing-guide)
8. [Performance Specifications](#performance-specifications)
9. [Best Practices](#best-practices)
10. [Related Components](#related-components)

---

## Overview

### Purpose

The **CommandOrchestrator** is the central command execution coordinator in the VoiceOS system, responsible for intelligently routing voice commands through a three-tier execution hierarchy with automatic fallback handling.

**Single Responsibility**: Orchestrate command execution across three distinct command processors with confidence-based routing and fallback logic.

### Key Responsibilities

- **Command Routing**: Direct commands to appropriate execution tier based on confidence and context
- **Fallback Management**: Automatically cascade to lower tiers when higher tiers fail
- **Confidence Validation**: Enforce minimum confidence thresholds (≥0.5)
- **Metrics Tracking**: Monitor execution success rates, latency, and tier performance
- **Event Streaming**: Emit real-time execution events for monitoring and debugging
- **Vocabulary Management**: Coordinate command registration across speech recognition system

### Architecture Role

CommandOrchestrator sits at the heart of the VoiceOS command processing pipeline:

```
Speech Input → SpeechManager → CommandOrchestrator → [Tier 1/2/3] → Action Execution
                                        ↓
                                   StateManager
                                   (context & state)
```

**Position**: Central orchestration layer between speech recognition and command execution
**Dependencies**: StateManager, SpeechManager, CommandManager, VoiceCommandProcessor, ActionCoordinator
**Dependents**: VoiceOSService, CommandExecutionService

---

## Core Concepts

### 1. Three-Tier Execution System

CommandOrchestrator implements a hierarchical command execution model with automatic fallback:

```
┌─────────────────────────────────────────────────────────────┐
│                    COMMAND ORCHESTRATOR                      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ TIER 1: CommandManager                               │   │
│  │ • Database-backed structured commands                │   │
│  │ • High confidence (1.0)                              │   │
│  │ • Full context awareness                             │   │
│  │ • Target: <5ms latency                               │   │
│  └──────────────────────────────────────────────────────┘   │
│                         ↓ (on failure)                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ TIER 2: VoiceCommandProcessor                        │   │
│  │ • Hash-based learned app commands                    │   │
│  │ • Medium confidence (0.5-1.0)                        │   │
│  │ • App-specific actions                               │   │
│  │ • Target: <50ms latency                              │   │
│  └──────────────────────────────────────────────────────┘   │
│                         ↓ (on failure)                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ TIER 3: ActionCoordinator                            │   │
│  │ • Handler-based general actions                      │   │
│  │ • Low confidence acceptable (0.4-0.9)                │   │
│  │ • Best-effort execution                              │   │
│  │ • Target: <100ms latency                             │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Tier Selection Logic**:
- **Tier 1 First**: Always attempted first (unless in fallback mode)
- **Sequential Fallback**: Each tier failure triggers next tier attempt
- **Stop on Success**: No further tiers attempted once command succeeds
- **Failure Cascading**: Exceptions and failures both trigger fallback

### 2. Confidence Threshold System

```kotlin
// Minimum confidence threshold - CRITICAL: matches VoiceOSService.kt line 977
const val MIN_CONFIDENCE_THRESHOLD = 0.5f

// Validation at entry point
if (confidence < MIN_CONFIDENCE_THRESHOLD) {
    return CommandResult.ValidationError("Confidence too low: $confidence")
}
```

**Confidence Scale**:
- **1.0**: Perfect match (Tier 1 structured commands)
- **0.7-0.99**: High confidence (Tier 1/2 hybrid)
- **0.5-0.69**: Medium confidence (Tier 2 preferred)
- **0.4-0.49**: Low confidence (Tier 3 fallback only)
- **<0.4**: Rejected (too uncertain)

### 3. Fallback Mode

Fallback mode is a degraded operation mode that bypasses Tier 1 when CommandManager is unhealthy:

```kotlin
// Enable fallback mode (e.g., when CommandManager is down)
orchestrator.enableFallbackMode()

// Commands now skip Tier 1 and go directly to Tier 2/3
executeCommand("test", 0.9f, context) // → Tier 2 (skipping Tier 1)

// Restore normal routing when CommandManager is healthy
orchestrator.disableFallbackMode()
```

**When to Enable**:
- CommandManager initialization failure
- CommandManager database corruption
- CommandManager performance degradation
- ServiceMonitor detects Tier 1 unhealthy

**Behavior**:
- Skip Tier 1 completely
- Start routing at Tier 2
- Still fallback to Tier 3 if Tier 2 fails
- Metrics track fallback mode activations

### 4. Command Normalization

All commands are normalized before execution:

```kotlin
val normalizedCommand = command.lowercase().trim()
```

**Normalization Rules**:
1. **Lowercase**: "TEST COMMAND" → "test command"
2. **Trim whitespace**: "  test  " → "test"
3. **Preserve internal spaces**: "test command" → "test command"
4. **Applied once**: Same normalized text passed to all tiers

### 5. Event Streaming

CommandOrchestrator emits real-time events for monitoring:

```kotlin
sealed class CommandEvent {
    data class ExecutionStarted(val command: String, val timestamp: Long)
    data class ExecutionCompleted(val command: String, val result: CommandResult, val timestamp: Long)
    data class TierFallback(val fromTier: Int, val toTier: Int, val reason: String)
    data class FallbackModeChanged(val enabled: Boolean, val reason: String)
    data class Error(val message: String, val exception: Exception?)
}

// Subscribe to events
orchestrator.commandEvents.collect { event ->
    when (event) {
        is CommandEvent.ExecutionStarted -> log("Command started: ${event.command}")
        is CommandEvent.TierFallback -> log("Tier ${event.fromTier} → ${event.toTier}")
        // ...
    }
}
```

---

## Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CommandOrchestratorImpl                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ STATE MANAGEMENT                                             │  │
│  │ • isReady: Boolean                                           │  │
│  │ • isFallbackModeEnabled: Boolean                             │  │
│  │ • currentState: CommandOrchestratorState                     │  │
│  │ • commandEvents: Flow<CommandEvent>                          │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ TIER EXECUTORS (Lazy-Initialized)                           │  │
│  │ • commandManager: CommandManager?                            │  │
│  │ • voiceCommandProcessor: VoiceCommandProcessor?              │  │
│  │ • actionCoordinator: ActionCoordinator?                      │  │
│  │ • accessibilityService: AccessibilityService?                │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ METRICS & HISTORY                                            │  │
│  │ • totalCommandsExecuted: AtomicLong                          │  │
│  │ • tier1/2/3SuccessCount: AtomicLong                          │  │
│  │ • failureCount, notFoundCount: AtomicLong                    │  │
│  │ • executionTimes: ConcurrentHashMap<Int, List<Long>>         │  │
│  │ • commandHistory: ConcurrentHashMap<Long, CommandExecution>  │  │
│  │ • registeredCommands: ConcurrentHashMap.KeySet<String>       │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ CORE OPERATIONS                                              │  │
│  │ • executeCommand(command, confidence, context): CommandResult│  │
│  │ • executeTier1/2/3(): CommandResult                          │  │
│  │ • enableFallbackMode() / disableFallbackMode()               │  │
│  │ • registerCommands(Set<String>)                              │  │
│  │ • getMetrics(): CommandMetrics                               │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
         ↓ depends on                ↓ depends on
┌──────────────────┐        ┌──────────────────┐
│  IStateManager   │        │  ISpeechManager  │
│  • App context   │        │  • Vocabulary    │
│  • Device state  │        │  • Recognition   │
└──────────────────┘        └──────────────────┘
```

### Tier Structure

**Tier 1: CommandManager**
- **Technology**: Room database + Structured Command objects
- **Confidence**: Requires 1.0 (exact match)
- **Latency**: <5ms target
- **Use Cases**: Pre-defined commands, global actions, system commands
- **Fallback**: On failure → Tier 2

**Tier 2: VoiceCommandProcessor**
- **Technology**: Hash-based element lookup + learned interactions
- **Confidence**: 0.5-1.0 (medium to high)
- **Latency**: <50ms target
- **Use Cases**: App-specific commands, learned UI interactions
- **Fallback**: On failure → Tier 3

**Tier 3: ActionCoordinator**
- **Technology**: Handler registry + best-effort actions
- **Confidence**: 0.4-0.9 (low to medium)
- **Latency**: <100ms target
- **Use Cases**: General actions, fallback handlers, accessibility actions
- **Fallback**: None (terminal tier)

### Dependencies

**Required at Construction**:
```kotlin
@Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val stateManager: IStateManager,
    private val speechManager: ISpeechManager
)
```

**Set After Initialization** (via `setTierExecutors`):
```kotlin
fun setTierExecutors(
    commandManager: CommandManager?,          // Tier 1
    voiceCommandProcessor: VoiceCommandProcessor?,  // Tier 2
    actionCoordinator: ActionCoordinator?,    // Tier 3
    accessibilityService: AccessibilityService?     // Global actions
)
```

**Why Lazy Initialization?**
- Tier executors require AccessibilityService context
- AccessibilityService not available at DI construction time
- Avoids circular dependency between orchestrator and service

---

## Implementation Details

### 1. Three-Tier Command Execution Flow

#### Complete Execution Flow

```kotlin
suspend fun executeCommand(
    command: String,
    confidence: Float,
    context: CommandContext
): CommandResult {
    // Step 1: Validate state
    if (currentState != CommandOrchestratorState.READY) {
        return CommandResult.ValidationError("Orchestrator not ready")
    }

    // Step 2: Validate confidence (≥0.5)
    if (confidence < MIN_CONFIDENCE_THRESHOLD) {
        return CommandResult.ValidationError("Confidence too low: $confidence")
    }

    // Step 3: Normalize command
    val normalizedCommand = command.lowercase().trim()

    // Step 4: Emit execution started event
    emitEvent(CommandEvent.ExecutionStarted(normalizedCommand, System.currentTimeMillis()))

    // Step 5: Try Tier 1 (unless in fallback mode)
    if (!isFallbackModeEnabled && commandManager != null) {
        val result = executeTier1(normalizedCommand, confidence, context, startTime)
        if (result is CommandResult.Success) {
            tier1SuccessCount.incrementAndGet()
            return result
        }
        // Tier 1 failed - emit fallback event
        emitEvent(CommandEvent.TierFallback(1, 2, "Tier 1 failed"))
    }

    // Step 6: Try Tier 2
    if (voiceCommandProcessor != null) {
        val result = executeTier2(normalizedCommand, startTime)
        if (result is CommandResult.Success) {
            tier2SuccessCount.incrementAndGet()
            return result
        }
        // Tier 2 failed - emit fallback event
        emitEvent(CommandEvent.TierFallback(2, 3, "Tier 2 failed"))
    }

    // Step 7: Try Tier 3 (terminal fallback)
    return executeTier3(normalizedCommand, startTime)
}
```

#### Tier 1 Execution (CommandManager)

```kotlin
private suspend fun executeTier1(
    normalizedCommand: String,
    confidence: Float,
    context: CommandContext,
    startTime: Long
): CommandResult {
    return try {
        // Create structured Command object
        val cmd = Command(
            id = normalizedCommand,
            text = normalizedCommand,
            source = CommandSource.VOICE,
            context = context,
            confidence = confidence,
            timestamp = System.currentTimeMillis()
        )

        // Execute via CommandManager
        val result = commandManager!!.executeCommand(cmd)
        val executionTime = System.currentTimeMillis() - startTime
        recordExecutionTime(1, executionTime)

        // Check success flag
        if (result.success) {
            CommandResult.Success(
                tier = 1,
                executionTimeMs = executionTime,
                details = "CommandManager executed: ${result.response}"
            )
        } else {
            CommandResult.Failure(
                tier = 1,
                reason = result.error?.message ?: "CommandManager failed",
                error = result.error?.let { RuntimeException("${it.code}: ${it.message}") }
            )
        }
    } catch (e: Exception) {
        CommandResult.Failure(tier = 1, reason = "CommandManager exception", error = e)
    }
}
```

**Tier 1 Key Points**:
- ✅ Creates full Command object with all context
- ✅ Uses CommandSource.VOICE
- ✅ Checks result.success flag
- ✅ Records execution time for metrics
- ✅ Converts exceptions to Failure results

#### Tier 2 Execution (VoiceCommandProcessor)

```kotlin
private suspend fun executeTier2(
    normalizedCommand: String,
    startTime: Long
): CommandResult {
    return try {
        // Execute via VoiceCommandProcessor (simple string input)
        val result = voiceCommandProcessor!!.processCommand(normalizedCommand)
        val executionTime = System.currentTimeMillis() - startTime
        recordExecutionTime(2, executionTime)

        // Check success flag
        if (result.success) {
            CommandResult.Success(
                tier = 2,
                executionTimeMs = executionTime,
                details = "VoiceCommandProcessor executed: ${result.message}"
            )
        } else {
            CommandResult.Failure(
                tier = 2,
                reason = result.message ?: "VoiceCommandProcessor failed",
                error = null
            )
        }
    } catch (e: Exception) {
        CommandResult.Failure(tier = 2, reason = "VoiceCommandProcessor exception", error = e)
    }
}
```

**Tier 2 Key Points**:
- ✅ Simple string command input (no Command object)
- ✅ Hash-based lookup in background
- ✅ Returns app-specific execution result
- ✅ Lighter weight than Tier 1

#### Tier 3 Execution (ActionCoordinator)

```kotlin
private suspend fun executeTier3(
    normalizedCommand: String,
    startTime: Long
): CommandResult {
    return try {
        if (actionCoordinator != null) {
            // Execute via ActionCoordinator (best-effort, no return value)
            actionCoordinator!!.executeAction(normalizedCommand)
            val executionTime = System.currentTimeMillis() - startTime
            recordExecutionTime(3, executionTime)

            // CRITICAL: Original code has NO success check
            // Always logs as executed (VoiceOSService.kt line 1137)
            CommandResult.Success(
                tier = 3,
                executionTimeMs = executionTime,
                details = "ActionCoordinator executed (best effort)"
            )
        } else {
            CommandResult.NotFound
        }
    } catch (e: Exception) {
        CommandResult.Failure(tier = 3, reason = "ActionCoordinator exception", error = e)
    }
}
```

**Tier 3 Key Points**:
- ✅ Best-effort execution (no success/failure return)
- ✅ Terminal fallback (no further tiers)
- ✅ Always considered successful unless exception
- ✅ Matches VoiceOSService.kt behavior exactly

### 2. Confidence Threshold Logic

```kotlin
companion object {
    // EXACT match to VoiceOSService.kt line 977
    private const val MIN_CONFIDENCE_THRESHOLD = 0.5f
}

// Validation in executeCommand()
if (confidence < MIN_CONFIDENCE_THRESHOLD) {
    Log.d(TAG, "Command rejected: confidence too low ($confidence < $MIN_CONFIDENCE_THRESHOLD)")
    return CommandResult.ValidationError("Confidence too low: $confidence")
}
```

**Confidence Levels**:
- **Tier 1**: Typically 1.0 (exact database match)
- **Tier 2**: 0.5-1.0 (learned hash match with fuzzy matching)
- **Tier 3**: 0.4-0.9 (general action handlers, more permissive)

**Validation Point**: Entry to `executeCommand()` - rejected before any tier execution

### 3. Fallback Mechanism Between Tiers

**Fallback Triggers**:
1. **Explicit Failure**: `result.success == false`
2. **Exception Thrown**: Caught and converted to Failure
3. **Null Executor**: Tier unavailable (null reference)

**Fallback Events**:
```kotlin
// Emit fallback event when transitioning tiers
emitEvent(CommandEvent.TierFallback(
    fromTier = 1,
    toTier = 2,
    reason = "Tier 1 failed"
))
```

**No Fallback From**:
- Tier 3 (terminal tier)
- CommandResult.ValidationError (pre-execution rejection)
- CommandResult.NotFound (all tiers exhausted)

### 4. Global Action Handling

Global actions bypass the tier system and go directly to AccessibilityService:

```kotlin
fun executeGlobalAction(action: Int): Boolean {
    if (accessibilityService == null) {
        Log.e(TAG, "Cannot execute global action - AccessibilityService not set")
        return false
    }

    return try {
        val result = accessibilityService!!.performGlobalAction(action)
        Log.d(TAG, "Global action $action executed: $result")
        result
    } catch (e: Exception) {
        Log.e(TAG, "Error executing global action $action", e)
        false
    }
}
```

**Common Global Actions**:
- `GLOBAL_ACTION_BACK` (back button)
- `GLOBAL_ACTION_HOME` (home screen)
- `GLOBAL_ACTION_RECENTS` (recent apps)
- `GLOBAL_ACTION_NOTIFICATIONS` (notification shade)
- `GLOBAL_ACTION_QUICK_SETTINGS` (quick settings)

**Usage**:
```kotlin
// Execute back action
orchestrator.executeGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)

// Execute home action
orchestrator.executeGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
```

### 5. Pre/Post Execution Checks

**Pre-Execution Checks** (in `executeCommand`):
```kotlin
// 1. State check
if (currentState != CommandOrchestratorState.READY) {
    return CommandResult.ValidationError("Orchestrator not ready: $currentState")
}

// 2. Confidence validation
if (confidence < MIN_CONFIDENCE_THRESHOLD) {
    return CommandResult.ValidationError("Confidence too low: $confidence")
}

// 3. Command text validation
if (normalizedCommand.isBlank()) {
    return CommandResult.ValidationError("Empty command text")
}
```

**Post-Execution Actions**:
```kotlin
// 1. Record in history
recordCommandExecution(normalizedCommand, confidence, tier, result, startTime)

// 2. Emit completion event
emitEvent(CommandEvent.ExecutionCompleted(normalizedCommand, result, System.currentTimeMillis()))

// 3. Update metrics
when (result) {
    is CommandResult.Success -> tier1/2/3SuccessCount.incrementAndGet()
    is CommandResult.Failure -> failureCount.incrementAndGet()
    is CommandResult.NotFound -> notFoundCount.incrementAndGet()
}

// 4. Return result to caller
return result
```

---

## API Reference

### Initialization & Lifecycle

#### `suspend fun initialize(context: Context)`

Initialize the command orchestrator.

**Parameters**:
- `context: Context` - Android application context

**Throws**:
- `IllegalStateException` - If already initialized

**Behavior**:
- Transitions state: UNINITIALIZED → INITIALIZING → READY
- Clears all metrics and history
- Sets `isReady = true`

**Example**:
```kotlin
val orchestrator: ICommandOrchestrator = /* injected */
orchestrator.initialize(applicationContext)
check(orchestrator.isReady) { "Orchestrator must be ready" }
```

#### `fun pause()`

Pause command processing (preserves state).

**Behavior**:
- Transitions state: READY → PAUSED
- Does nothing if not in READY state
- No commands accepted while paused

**Example**:
```kotlin
orchestrator.pause()
// orchestrator.currentState == CommandOrchestratorState.PAUSED
```

#### `fun resume()`

Resume command processing.

**Behavior**:
- Transitions state: PAUSED → READY
- Does nothing if not in PAUSED state

**Example**:
```kotlin
orchestrator.resume()
// orchestrator.currentState == CommandOrchestratorState.READY
```

#### `fun cleanup()`

Clean up resources and stop processing.

**Behavior**:
- Transitions state: * → SHUTDOWN
- Sets `isReady = false`
- Clears all tier executor references
- Clears history and metrics
- Closes event channel

**Example**:
```kotlin
override fun onDestroy() {
    orchestrator.cleanup()
    super.onDestroy()
}
```

### Command Execution

#### `suspend fun executeCommand(command: String, confidence: Float, context: CommandContext): CommandResult`

Execute a voice command through the three-tier system.

**Parameters**:
- `command: String` - Voice command text (will be normalized)
- `confidence: Float` - Recognition confidence (0.0 to 1.0)
- `context: CommandContext` - Current command context (app, activity, element, etc.)

**Returns**: `CommandResult` (sealed class)
- `CommandResult.Success(tier, executionTimeMs, details)` - Command executed successfully
- `CommandResult.Failure(tier, reason, error)` - Command failed at specific tier
- `CommandResult.NotFound` - Command not found in any tier
- `CommandResult.ValidationError(reason)` - Pre-execution validation failed

**Execution Flow**:
1. Validate state (must be READY)
2. Validate confidence (≥0.5)
3. Normalize command (lowercase + trim)
4. Try Tier 1 (unless fallback mode)
5. Try Tier 2 (if Tier 1 fails/unavailable)
6. Try Tier 3 (if Tier 2 fails/unavailable)
7. Return result with metrics

**Example**:
```kotlin
val context = CommandContext(
    packageName = "com.android.chrome",
    activityName = "MainActivity"
)

val result = orchestrator.executeCommand(
    command = "go back",
    confidence = 0.85f,
    context = context
)

when (result) {
    is CommandResult.Success -> {
        log("Success at tier ${result.tier} in ${result.executionTimeMs}ms")
    }
    is CommandResult.Failure -> {
        log("Failed at tier ${result.tier}: ${result.reason}")
    }
    is CommandResult.NotFound -> {
        log("Command not found in any tier")
    }
    is CommandResult.ValidationError -> {
        log("Validation failed: ${result.reason}")
    }
}
```

#### `suspend fun executeCommand(command: Command): CommandResult`

Execute a structured Command object directly.

**Parameters**:
- `command: Command` - Pre-constructed command object with all context

**Returns**: `CommandResult` (same as above)

**Behavior**:
- Uses command's embedded confidence and context
- Delegates to main `executeCommand()` method

**Example**:
```kotlin
val command = Command(
    id = "back_001",
    text = "go back",
    source = CommandSource.VOICE,
    confidence = 0.9f,
    context = CommandContext(packageName = "com.app"),
    timestamp = System.currentTimeMillis()
)

val result = orchestrator.executeCommand(command)
```

#### `fun executeGlobalAction(action: Int): Boolean`

Execute a global accessibility action.

**Parameters**:
- `action: Int` - Global action constant (AccessibilityService.GLOBAL_ACTION_*)

**Returns**: `Boolean` - true if action executed successfully

**Common Actions**:
- `GLOBAL_ACTION_BACK` (1) - Back button
- `GLOBAL_ACTION_HOME` (2) - Home screen
- `GLOBAL_ACTION_RECENTS` (3) - Recent apps
- `GLOBAL_ACTION_NOTIFICATIONS` (4) - Notification shade

**Example**:
```kotlin
// Go back
if (!orchestrator.executeGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)) {
    log("Failed to execute back action")
}

// Go home
orchestrator.executeGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
```

### Fallback Mode Management

#### `fun enableFallbackMode()`

Enable fallback mode - commands bypass Tier 1 and go directly to Tier 2/3.

**Behavior**:
- Sets `isFallbackModeEnabled = true`
- Increments fallback mode activation counter
- Emits `CommandEvent.FallbackModeChanged(enabled=true)`

**When to Use**:
- CommandManager initialization failed
- CommandManager database corruption detected
- ServiceMonitor reports Tier 1 unhealthy

**Example**:
```kotlin
// ServiceMonitor detected CommandManager failure
if (commandManagerHealth.status == Health.UNHEALTHY) {
    orchestrator.enableFallbackMode()
    log("Fallback mode enabled - using Tier 2/3 only")
}
```

#### `fun disableFallbackMode()`

Disable fallback mode - restore normal three-tier command routing.

**Behavior**:
- Sets `isFallbackModeEnabled = false`
- Emits `CommandEvent.FallbackModeChanged(enabled=false)`

**Example**:
```kotlin
// CommandManager recovered
if (commandManagerHealth.status == Health.HEALTHY) {
    orchestrator.disableFallbackMode()
    log("Fallback mode disabled - restored normal tier routing")
}
```

### Command Registration

#### `suspend fun registerCommands(commandTexts: Set<String>)`

Register commands from database sources.

**Parameters**:
- `commandTexts: Set<String>` - Set of command texts to register

**Behavior**:
- Adds commands to internal registry
- Updates speech recognition vocabulary (via SpeechManager)

**When to Call**:
- After initialization (register existing commands)
- When new commands learned (dynamic registration)
- After database sync (bulk registration)

**Example**:
```kotlin
// Register commands from database
val commands = setOf(
    "go back",
    "go home",
    "open settings",
    "take screenshot"
)
orchestrator.registerCommands(commands)
```

#### `suspend fun updateCommandVocabulary()`

Update command vocabulary for speech recognition.

**Behavior**:
- Combines all registered commands
- Updates SpeechManager vocabulary
- Improves recognition accuracy for known commands

**Example**:
```kotlin
// After registering many commands, update vocabulary
orchestrator.registerCommands(newCommands)
orchestrator.updateCommandVocabulary()
```

### Metrics & Observability

#### `fun getMetrics(): CommandMetrics`

Get current execution metrics.

**Returns**: `CommandMetrics` data class
```kotlin
data class CommandMetrics(
    val totalCommandsExecuted: Long,
    val tier1SuccessCount: Long,
    val tier2SuccessCount: Long,
    val tier3SuccessCount: Long,
    val failureCount: Long,
    val notFoundCount: Long,
    val averageExecutionTimeMs: Long,
    val fallbackModeActivations: Int
)
```

**Example**:
```kotlin
val metrics = orchestrator.getMetrics()
log("Total commands: ${metrics.totalCommandsExecuted}")
log("Tier 1 success rate: ${metrics.tier1SuccessCount * 100.0 / metrics.totalCommandsExecuted}%")
log("Average latency: ${metrics.averageExecutionTimeMs}ms")
```

#### `fun getCommandHistory(limit: Int = 50): List<CommandExecution>`

Get command execution history.

**Parameters**:
- `limit: Int` - Maximum number of recent commands to return (default: 50, max: 100)

**Returns**: `List<CommandExecution>` - Recent command executions
```kotlin
data class CommandExecution(
    val commandText: String,
    val confidence: Float,
    val tier: Int?,
    val result: CommandResult,
    val timestamp: Long,
    val executionTimeMs: Long
)
```

**Example**:
```kotlin
// Get last 10 commands
val history = orchestrator.getCommandHistory(limit = 10)
history.forEach { execution ->
    log("${execution.commandText} - tier ${execution.tier} - ${execution.executionTimeMs}ms")
}
```

### State Properties

#### `val isReady: Boolean`

Indicates if orchestrator is ready to process commands.

**Values**:
- `true` - Ready to accept commands
- `false` - Not initialized or in error state

#### `val isFallbackModeEnabled: Boolean`

Indicates if fallback mode is enabled.

**Values**:
- `true` - Commands skip Tier 1, start at Tier 2
- `false` - Normal three-tier routing

#### `val currentState: CommandOrchestratorState`

Current command execution state.

**States**:
```kotlin
enum class CommandOrchestratorState {
    UNINITIALIZED,  // Before initialize()
    INITIALIZING,   // During initialize()
    READY,          // Normal operation
    PAUSED,         // Paused (no commands accepted)
    ERROR,          // Error state
    SHUTDOWN        // After cleanup()
}
```

#### `val commandEvents: Flow<CommandEvent>`

Flow of command execution events for monitoring.

**Event Types**:
```kotlin
sealed class CommandEvent {
    data class ExecutionStarted(val command: String, val timestamp: Long)
    data class ExecutionCompleted(val command: String, val result: CommandResult, val timestamp: Long)
    data class TierFallback(val fromTier: Int, val toTier: Int, val reason: String)
    data class FallbackModeChanged(val enabled: Boolean, val reason: String)
    data class Error(val message: String, val exception: Exception?)
}
```

**Example**:
```kotlin
lifecycleScope.launch {
    orchestrator.commandEvents.collect { event ->
        when (event) {
            is CommandEvent.ExecutionStarted -> {
                log("Command started: ${event.command}")
            }
            is CommandEvent.TierFallback -> {
                log("Tier ${event.fromTier} → ${event.toTier}: ${event.reason}")
            }
            is CommandEvent.ExecutionCompleted -> {
                log("Command completed: ${event.result}")
            }
            // ...
        }
    }
}
```

---

## Usage Examples

### Example 1: Basic Command Execution

```kotlin
// Initialize orchestrator
val orchestrator: ICommandOrchestrator = /* injected */
orchestrator.initialize(applicationContext)

// Set tier executors (after AccessibilityService available)
orchestrator.setTierExecutors(
    commandManager = commandManager,
    voiceCommandProcessor = voiceCommandProcessor,
    actionCoordinator = actionCoordinator,
    accessibilityService = this
)

// Execute a simple command
val context = CommandContext(
    packageName = currentPackage,
    activityName = currentActivity
)

val result = orchestrator.executeCommand(
    command = "go back",
    confidence = 0.95f,
    context = context
)

when (result) {
    is CommandResult.Success -> {
        Log.i(TAG, "✓ Command executed at tier ${result.tier}")
    }
    is CommandResult.Failure -> {
        Log.e(TAG, "✗ Command failed: ${result.reason}")
    }
    is CommandResult.NotFound -> {
        Log.w(TAG, "Command not found in any tier")
    }
    is CommandResult.ValidationError -> {
        Log.w(TAG, "Validation error: ${result.reason}")
    }
}
```

### Example 2: Handling Tier Fallback

```kotlin
// Monitor tier fallback events
lifecycleScope.launch {
    orchestrator.commandEvents.collect { event ->
        when (event) {
            is CommandEvent.TierFallback -> {
                Log.w(TAG, "Tier ${event.fromTier} failed, falling back to tier ${event.toTier}")
                Log.w(TAG, "Reason: ${event.reason}")

                // Optional: Alert user of degraded performance
                if (event.fromTier == 1 && event.toTier == 2) {
                    showToast("Using fallback command handler")
                }
            }
            is CommandEvent.ExecutionCompleted -> {
                when (event.result) {
                    is CommandResult.Success -> {
                        val tier = (event.result as CommandResult.Success).tier
                        Log.i(TAG, "Command succeeded at tier $tier")
                    }
                    else -> {
                        Log.e(TAG, "All tiers failed for: ${event.command}")
                    }
                }
            }
        }
    }
}

// Execute command that might require fallback
val result = orchestrator.executeCommand("complex command", 0.7f, context)
```

### Example 3: Custom Confidence Thresholds

```kotlin
// Different confidence levels for different command types
fun executeCommandWithCustomValidation(
    command: String,
    rawConfidence: Float,
    commandType: CommandType
): CommandResult {
    // Apply custom threshold based on command type
    val adjustedConfidence = when (commandType) {
        CommandType.DESTRUCTIVE -> max(rawConfidence, 0.9f)  // Require high confidence
        CommandType.NAVIGATION -> max(rawConfidence, 0.6f)   // Medium confidence OK
        CommandType.QUERY -> max(rawConfidence, 0.5f)        // Low confidence acceptable
    }

    // Execute with adjusted confidence
    return orchestrator.executeCommand(
        command = command,
        confidence = adjustedConfidence,
        context = getCurrentContext()
    )
}

// Usage
val result = executeCommandWithCustomValidation(
    command = "delete all",
    rawConfidence = 0.75f,
    commandType = CommandType.DESTRUCTIVE  // Will require ≥0.9 confidence
)
```

### Example 4: Error Handling and Retry

```kotlin
suspend fun executeCommandWithRetry(
    command: String,
    confidence: Float,
    context: CommandContext,
    maxRetries: Int = 2
): CommandResult {
    var lastResult: CommandResult? = null

    repeat(maxRetries + 1) { attempt ->
        val result = orchestrator.executeCommand(command, confidence, context)

        when (result) {
            is CommandResult.Success -> return result

            is CommandResult.Failure -> {
                Log.w(TAG, "Attempt ${attempt + 1} failed: ${result.reason}")
                lastResult = result

                // Optional: backoff delay
                if (attempt < maxRetries) {
                    delay(100L * (attempt + 1))
                }
            }

            is CommandResult.ValidationError -> {
                // Don't retry validation errors
                return result
            }

            is CommandResult.NotFound -> {
                // Don't retry not found
                return result
            }
        }
    }

    return lastResult ?: CommandResult.Failure(
        tier = null,
        reason = "All retries exhausted",
        error = null
    )
}

// Usage
val result = executeCommandWithRetry("unreliable command", 0.8f, context, maxRetries = 3)
```

### Example 5: Command History Analysis

```kotlin
// Analyze recent command performance
fun analyzeCommandPerformance() {
    val history = orchestrator.getCommandHistory(limit = 50)

    // Group by tier
    val tierDistribution = history.groupBy { it.tier }
    tierDistribution.forEach { (tier, executions) ->
        val avgTime = executions.map { it.executionTimeMs }.average()
        Log.i(TAG, "Tier $tier: ${executions.size} commands, avg ${avgTime}ms")
    }

    // Find slow commands
    val slowCommands = history.filter { it.executionTimeMs > 100 }
    slowCommands.forEach { execution ->
        Log.w(TAG, "Slow command: ${execution.commandText} took ${execution.executionTimeMs}ms")
    }

    // Calculate success rates
    val successCount = history.count { it.result is CommandResult.Success }
    val successRate = successCount * 100.0 / history.size
    Log.i(TAG, "Success rate: $successRate% ($successCount/${history.size})")
}
```

### Example 6: Metrics Dashboard

```kotlin
// Display real-time metrics
fun displayMetricsDashboard() {
    val metrics = orchestrator.getMetrics()

    val dashboard = buildString {
        appendLine("=== CommandOrchestrator Metrics ===")
        appendLine("Total Commands: ${metrics.totalCommandsExecuted}")
        appendLine()

        // Tier success distribution
        appendLine("Tier Distribution:")
        appendLine("  Tier 1: ${metrics.tier1SuccessCount} (${percent(metrics.tier1SuccessCount, metrics.totalCommandsExecuted)}%)")
        appendLine("  Tier 2: ${metrics.tier2SuccessCount} (${percent(metrics.tier2SuccessCount, metrics.totalCommandsExecuted)}%)")
        appendLine("  Tier 3: ${metrics.tier3SuccessCount} (${percent(metrics.tier3SuccessCount, metrics.totalCommandsExecuted)}%)")
        appendLine()

        // Failure analysis
        appendLine("Failures:")
        appendLine("  Failed: ${metrics.failureCount}")
        appendLine("  Not Found: ${metrics.notFoundCount}")
        appendLine()

        // Performance
        appendLine("Performance:")
        appendLine("  Avg Execution Time: ${metrics.averageExecutionTimeMs}ms")
        appendLine("  Fallback Activations: ${metrics.fallbackModeActivations}")
    }

    Log.i(TAG, dashboard)
}

private fun percent(count: Long, total: Long): String {
    return if (total > 0) "%.1f".format(count * 100.0 / total) else "0.0"
}
```

### Example 7: Fallback Mode Management

```kotlin
// ServiceMonitor integration
class ServiceMonitor(private val orchestrator: ICommandOrchestrator) {

    fun checkCommandManagerHealth() {
        val health = commandManager.healthCheck()

        when {
            health.isUnhealthy && !orchestrator.isFallbackModeEnabled -> {
                Log.w(TAG, "CommandManager unhealthy - enabling fallback mode")
                orchestrator.enableFallbackMode()
                sendAlert("System running in fallback mode")
            }

            health.isHealthy && orchestrator.isFallbackModeEnabled -> {
                Log.i(TAG, "CommandManager recovered - disabling fallback mode")
                orchestrator.disableFallbackMode()
                sendAlert("System restored to normal operation")
            }
        }
    }

    // Monitor fallback mode changes
    suspend fun monitorFallbackMode() {
        orchestrator.commandEvents.collect { event ->
            if (event is CommandEvent.FallbackModeChanged) {
                if (event.enabled) {
                    Log.w(TAG, "Fallback mode activated: ${event.reason}")
                    updateSystemStatus("DEGRADED")
                } else {
                    Log.i(TAG, "Fallback mode deactivated: ${event.reason}")
                    updateSystemStatus("HEALTHY")
                }
            }
        }
    }
}
```

---

## Testing Guide

### Test Setup

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class CommandOrchestratorImplTest {

    // Mocks
    private lateinit var mockContext: Context
    private lateinit var mockStateManager: IStateManager
    private lateinit var mockSpeechManager: ISpeechManager
    private lateinit var mockCommandManager: CommandManager
    private lateinit var mockVoiceCommandProcessor: VoiceCommandProcessor
    private lateinit var mockActionCoordinator: ActionCoordinator
    private lateinit var mockAccessibilityService: AccessibilityService

    // System under test
    private lateinit var orchestrator: CommandOrchestratorImpl

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        mockContext = mockk(relaxed = true)
        mockStateManager = mockk(relaxed = true)
        mockSpeechManager = mockk(relaxed = true)
        mockCommandManager = mockk(relaxed = true)
        mockVoiceCommandProcessor = mockk(relaxed = true)
        mockActionCoordinator = mockk(relaxed = true)
        mockAccessibilityService = mockk(relaxed = true)

        orchestrator = CommandOrchestratorImpl(
            appContext = mockContext,
            stateManager = mockStateManager,
            speechManager = mockSpeechManager
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }
}
```

### Test Patterns

#### Pattern 1: Testing Tier Success

```kotlin
@Test
fun `executeCommand - Tier 1 success with valid command`() = runTest {
    // Given
    orchestrator.initialize(mockContext)
    orchestrator.setTierExecutors(
        commandManager = mockCommandManager,
        voiceCommandProcessor = mockVoiceCommandProcessor,
        actionCoordinator = mockActionCoordinator,
        accessibilityService = mockAccessibilityService
    )

    val commandResult = mockk<com.augmentalis.commandmanager.models.CommandResult>()
    every { commandResult.success } returns true
    every { commandResult.message } returns "Command executed"
    coEvery { mockCommandManager.executeCommand(any()) } returns commandResult

    val context = CommandContext(packageName = "com.test.app")

    // When
    val result = orchestrator.executeCommand("test command", 0.8f, context)

    // Then
    assertTrue(result is CommandResult.Success)
    assertEquals(1, (result as CommandResult.Success).tier)
    assertEquals(1L, orchestrator.getMetrics().tier1SuccessCount)

    // Verify tier execution
    coVerify(exactly = 1) { mockCommandManager.executeCommand(any()) }
    coVerify(exactly = 0) { mockVoiceCommandProcessor.processCommand(any()) }
    coVerify(exactly = 0) { mockActionCoordinator.executeAction(any()) }
}
```

#### Pattern 2: Testing Tier Fallback

```kotlin
@Test
fun `executeCommand - fallback from Tier 1 to Tier 2`() = runTest {
    // Given
    orchestrator.initialize(mockContext)
    orchestrator.setTierExecutors(mockCommandManager, mockVoiceCommandProcessor, null, null)

    // Tier 1 fails
    val tier1Result = mockk<com.augmentalis.commandmanager.models.CommandResult>()
    every { tier1Result.success } returns false
    coEvery { mockCommandManager.executeCommand(any()) } returns tier1Result

    // Tier 2 succeeds
    val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
    every { tier2Result.success } returns true
    coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

    // When
    val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

    // Then
    assertTrue(result is CommandResult.Success)
    assertEquals(2, (result as CommandResult.Success).tier)

    // Verify both tiers were called
    coVerify(exactly = 1) { mockCommandManager.executeCommand(any()) }
    coVerify(exactly = 1) { mockVoiceCommandProcessor.processCommand(any()) }
}
```

#### Pattern 3: Testing Events

```kotlin
@Test
fun `executeCommand - emits execution started and completed events`() = runTest {
    // Given
    orchestrator.initialize(mockContext)
    orchestrator.setTierExecutors(mockCommandManager, null, null, null)

    val commandResult = mockk<com.augmentalis.commandmanager.models.CommandResult>()
    every { commandResult.success } returns true
    coEvery { mockCommandManager.executeCommand(any()) } returns commandResult

    val events = mutableListOf<CommandEvent>()
    val job = launch {
        orchestrator.commandEvents.take(2).toList(events)
    }

    // When
    orchestrator.executeCommand("test", 0.9f, CommandContext())
    job.join()

    // Then
    assertTrue(events.any { it is CommandEvent.ExecutionStarted })
    assertTrue(events.any { it is CommandEvent.ExecutionCompleted })
}
```

#### Pattern 4: Testing Metrics

```kotlin
@Test
fun `executeCommand - increments correct metrics counters`() = runTest {
    // Given
    orchestrator.initialize(mockContext)
    orchestrator.setTierExecutors(mockCommandManager, null, null, null)

    val commandResult = mockk<com.augmentalis.commandmanager.models.CommandResult>()
    every { commandResult.success } returns true
    coEvery { mockCommandManager.executeCommand(any()) } returns commandResult

    // When
    orchestrator.executeCommand("test1", 0.9f, CommandContext())
    orchestrator.executeCommand("test2", 0.9f, CommandContext())

    // Then
    val metrics = orchestrator.getMetrics()
    assertEquals(2L, metrics.totalCommandsExecuted)
    assertEquals(2L, metrics.tier1SuccessCount)
    assertEquals(0L, metrics.tier2SuccessCount)
    assertEquals(0L, metrics.failureCount)
}
```

#### Pattern 5: Testing Fallback Mode

```kotlin
@Test
fun `executeCommand - fallback mode skips Tier 1`() = runTest {
    // Given
    orchestrator.initialize(mockContext)
    orchestrator.setTierExecutors(mockCommandManager, mockVoiceCommandProcessor, null, null)

    val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
    every { tier2Result.success } returns true
    coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

    // When
    orchestrator.enableFallbackMode()
    val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

    // Then
    assertTrue(result is CommandResult.Success)
    assertEquals(2, (result as CommandResult.Success).tier)

    // Tier 1 should NOT be called
    coVerify(exactly = 0) { mockCommandManager.executeCommand(any()) }
    coVerify(exactly = 1) { mockVoiceCommandProcessor.processCommand(any()) }
}
```

### Mock Setup Best Practices

1. **Relaxed Mocks**: Use `mockk(relaxed = true)` for dependencies with many methods
2. **Explicit Behavior**: Define explicit behavior for methods under test
3. **Slot Capturing**: Use `slot<>()` to capture and verify method arguments
4. **Verification**: Use `coVerify` for suspend functions, `verify` for regular
5. **Cleanup**: Always call `clearAllMocks()` in `@After`

---

## Performance Specifications

### Latency Targets

| Tier | Target Latency | Maximum Latency | Typical Use Case |
|------|---------------|-----------------|------------------|
| **Tier 1** | <5ms | 50ms | Database lookup + execution |
| **Tier 2** | <50ms | 200ms | Hash lookup + UI interaction |
| **Tier 3** | <100ms | 500ms | Handler execution + accessibility |
| **Overall** | <100ms | 1000ms | Full cascade (all tiers) |

### Throughput Targets

- **Sequential Commands**: 10-20 commands/second
- **Concurrent Commands**: 5-10 commands/second (with queueing)
- **Event Emission**: 100+ events/second (buffered channel)
- **History Retention**: 100 commands (configurable)

### Memory Specifications

| Component | Memory Usage | Notes |
|-----------|-------------|-------|
| **Command History** | ~2KB per command | 100 commands = ~200KB |
| **Execution Times** | ~800 bytes | 100 times per tier × 3 tiers |
| **Registered Commands** | ~50 bytes per command | Set-based storage |
| **Event Buffer** | ~10KB | 100 event capacity |
| **Total Overhead** | ~250-300KB | Steady-state operation |

### Scalability Limits

- **Maximum Commands/Session**: No hard limit (history auto-cleans at 100)
- **Maximum Registered Commands**: 10,000+ (hash-based lookup)
- **Maximum Event Subscribers**: 10 concurrent flows
- **Maximum Tier Executors**: 3 (fixed architecture)

---

## Best Practices

### Do's ✅

1. **Always Initialize First**
   ```kotlin
   orchestrator.initialize(context)
   check(orchestrator.isReady) { "Must be ready" }
   ```

2. **Set Tier Executors After Service Available**
   ```kotlin
   override fun onServiceConnected() {
       orchestrator.setTierExecutors(
           commandManager, voiceCommandProcessor,
           actionCoordinator, this
       )
   }
   ```

3. **Monitor Events for Debugging**
   ```kotlin
   orchestrator.commandEvents.collect { event ->
       when (event) {
           is CommandEvent.TierFallback -> logFallback(event)
           is CommandEvent.Error -> reportError(event)
       }
   }
   ```

4. **Use Fallback Mode for Degraded Operation**
   ```kotlin
   if (commandManagerUnhealthy) {
       orchestrator.enableFallbackMode()
   }
   ```

5. **Validate Command Context**
   ```kotlin
   val context = CommandContext(
       packageName = currentPackage,
       activityName = currentActivity,
       focusedElement = currentElement
   )
   orchestrator.executeCommand(command, confidence, context)
   ```

6. **Check Metrics Regularly**
   ```kotlin
   val metrics = orchestrator.getMetrics()
   if (metrics.failureCount > metrics.totalCommandsExecuted * 0.1) {
       alertHighFailureRate()
   }
   ```

7. **Clean Up on Service Destroy**
   ```kotlin
   override fun onDestroy() {
       orchestrator.cleanup()
       super.onDestroy()
   }
   ```

### Don'ts ❌

1. **Don't Execute Before Initialization**
   ```kotlin
   ❌ orchestrator.executeCommand("test", 0.9f, context)
   ✅ orchestrator.initialize(context)
      orchestrator.executeCommand("test", 0.9f, context)
   ```

2. **Don't Bypass Confidence Validation**
   ```kotlin
   ❌ orchestrator.executeCommand("test", 0.3f, context)  // Will be rejected
   ✅ if (confidence >= 0.5f) {
          orchestrator.executeCommand("test", confidence, context)
      }
   ```

3. **Don't Ignore Execution Results**
   ```kotlin
   ❌ orchestrator.executeCommand("test", 0.9f, context)
   ✅ val result = orchestrator.executeCommand("test", 0.9f, context)
      when (result) {
          is CommandResult.Failure -> handleError(result)
          // ...
      }
   ```

4. **Don't Set Tier Executors Multiple Times**
   ```kotlin
   ❌ orchestrator.setTierExecutors(...)
      orchestrator.setTierExecutors(...)  // Overwrites previous
   ✅ Set tier executors once after service connection
   ```

5. **Don't Block on Event Collection**
   ```kotlin
   ❌ runBlocking {
          orchestrator.commandEvents.collect { }  // Blocks forever
      }
   ✅ lifecycleScope.launch {
          orchestrator.commandEvents.collect { }
      }
   ```

6. **Don't Mix Command Sources Without Context**
   ```kotlin
   ❌ orchestrator.executeCommand("test", 0.9f, CommandContext())
   ✅ orchestrator.executeCommand("test", 0.9f, getCurrentContext())
   ```

7. **Don't Forget to Handle NotFound**
   ```kotlin
   ❌ when (result) {
          is CommandResult.Success -> handle()
          is CommandResult.Failure -> handle()
      }
   ✅ when (result) {
          is CommandResult.Success -> handle()
          is CommandResult.Failure -> handle()
          is CommandResult.NotFound -> handleUnknownCommand()
          is CommandResult.ValidationError -> handleValidation()
      }
   ```

### Common Pitfalls

1. **Assuming Tier 3 Can Fail**
   - Tier 3 (ActionCoordinator) is best-effort and always returns Success (unless exception)
   - Don't expect Tier 3 to return Failure for unknown commands

2. **Not Handling Fallback Mode Changes**
   - Listen to `CommandEvent.FallbackModeChanged` events
   - Update UI/logs to indicate degraded operation

3. **Ignoring Execution Time Metrics**
   - Monitor average execution time
   - Alert if latency exceeds targets (>100ms average)

4. **Over-Relying on Tier 1**
   - Have Tier 2/3 ready as fallbacks
   - Don't assume Tier 1 will always succeed

5. **Not Cleaning Up History**
   - History auto-cleans at 100 commands
   - But large commands can accumulate memory
   - Consider periodic manual cleanup for long-running services

---

## Related Components

### Dependencies

#### IStateManager
- **Path**: `/modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/refactoring/interfaces/IStateManager.kt`
- **Purpose**: Provides app context, device state, and session state
- **Used For**: Command context enrichment

#### ISpeechManager
- **Path**: `/modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/refactoring/interfaces/ISpeechManager.kt`
- **Purpose**: Speech recognition and vocabulary management
- **Used For**: Updating command vocabulary, improving recognition

#### CommandManager
- **Path**: `/modules/managers/CommandManager/`
- **Purpose**: Tier 1 executor - structured database commands
- **Documentation**: `/docs/modules/command-manager/`

#### VoiceCommandProcessor
- **Path**: `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceaccessibility/scraping/VoiceCommandProcessor.kt`
- **Purpose**: Tier 2 executor - hash-based learned commands
- **Documentation**: `/docs/modules/voice-accessibility/`

#### ActionCoordinator
- **Path**: `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceaccessibility/accessibility/managers/ActionCoordinator.kt`
- **Purpose**: Tier 3 executor - general action handlers
- **Documentation**: `/docs/modules/voice-accessibility/`

### Dependents

#### VoiceOSService
- **Path**: `/modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/VoiceOSService.kt`
- **Usage**: Main accessibility service that hosts CommandOrchestrator
- **Integration**: Provides AccessibilityService context for tier executors

#### CommandExecutionService
- **Path**: `/modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/services/CommandExecutionService.kt`
- **Usage**: Background service for command execution
- **Integration**: Uses orchestrator for non-UI command processing

#### ServiceMonitor
- **Path**: `/modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/monitoring/ServiceMonitor.kt`
- **Usage**: Health monitoring and fallback mode management
- **Integration**: Monitors orchestrator metrics and controls fallback mode

### Related Documentation

1. **Architecture Decision Record**:
   - `/docs/voiceos-master/architecture/CommandManager-Centralized-Repository-Decision-251014-2347.md`
   - Decision to use centralized CommandManager as Tier 1

2. **Implementation Plan**:
   - `/docs/voiceos-master/implementation/Option4-CommandManager-Implementation-Plan-251015-0152.md`
   - Full implementation roadmap for 3-tier system

3. **SOLID Refactoring Summary**:
   - `/docs/voiceos-master/architecture/VoiceOSService-SOLID-Refactoring-Summary-251015-0011.md`
   - Overall refactoring strategy and principles

4. **Testing Documentation**:
   - `/modules/apps/VoiceOSCore/src/test/java/com/augmentalis/voiceoscore/refactoring/impl/CommandOrchestratorImplTest.kt`
   - 78 comprehensive tests with examples

---

## Appendix: Complete Code Example

### Full Integration Example

```kotlin
class VoiceOSService : AccessibilityService() {

    @Inject lateinit var orchestrator: ICommandOrchestrator
    @Inject lateinit var stateManager: IStateManager
    @Inject lateinit var commandManager: CommandManager
    @Inject lateinit var voiceCommandProcessor: VoiceCommandProcessor
    @Inject lateinit var actionCoordinator: ActionCoordinator

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Initialize orchestrator
        lifecycleScope.launch {
            orchestrator.initialize(applicationContext)

            // Set tier executors (requires AccessibilityService)
            orchestrator.setTierExecutors(
                commandManager = commandManager,
                voiceCommandProcessor = voiceCommandProcessor,
                actionCoordinator = actionCoordinator,
                accessibilityService = this@VoiceOSService
            )

            // Register commands from database
            val commands = commandRepository.getAllCommands()
            orchestrator.registerCommands(commands.map { it.text }.toSet())

            // Monitor events
            monitorOrchestratorEvents()

            Log.i(TAG, "CommandOrchestrator ready")
        }
    }

    private fun monitorOrchestratorEvents() {
        lifecycleScope.launch {
            orchestrator.commandEvents.collect { event ->
                when (event) {
                    is CommandEvent.ExecutionStarted -> {
                        Log.d(TAG, "Executing: ${event.command}")
                    }

                    is CommandEvent.TierFallback -> {
                        Log.w(TAG, "Tier ${event.fromTier} → ${event.toTier}: ${event.reason}")
                    }

                    is CommandEvent.FallbackModeChanged -> {
                        if (event.enabled) {
                            showNotification("System running in fallback mode")
                        } else {
                            showNotification("System restored to normal operation")
                        }
                    }

                    is CommandEvent.ExecutionCompleted -> {
                        when (event.result) {
                            is CommandResult.Success -> {
                                val tier = (event.result as CommandResult.Success).tier
                                val time = (event.result as CommandResult.Success).executionTimeMs
                                Log.i(TAG, "✓ ${event.command} - tier $tier - ${time}ms")
                            }
                            is CommandResult.Failure -> {
                                Log.e(TAG, "✗ ${event.command} failed")
                            }
                            is CommandResult.NotFound -> {
                                Log.w(TAG, "Command not found: ${event.command}")
                            }
                        }
                    }

                    is CommandEvent.Error -> {
                        Log.e(TAG, "Error: ${event.message}", event.exception)
                    }
                }
            }
        }
    }

    // Handle voice command from speech recognition
    fun onVoiceCommandReceived(command: String, confidence: Float) {
        lifecycleScope.launch {
            val context = CommandContext(
                packageName = rootInActiveWindow?.packageName?.toString(),
                activityName = getCurrentActivity(),
                focusedElement = findFocus(FOCUS_INPUT)?.viewIdResourceName,
                deviceState = stateManager.getDeviceState(),
                timestamp = System.currentTimeMillis()
            )

            val result = orchestrator.executeCommand(command, confidence, context)

            when (result) {
                is CommandResult.Success -> {
                    speakFeedback("Command executed")
                }
                is CommandResult.ValidationError -> {
                    speakFeedback("Command confidence too low")
                }
                is CommandResult.NotFound -> {
                    speakFeedback("Command not recognized")
                }
                is CommandResult.Failure -> {
                    speakFeedback("Command failed: ${result.reason}")
                }
            }
        }
    }

    override fun onDestroy() {
        orchestrator.cleanup()
        super.onDestroy()
    }
}
```

---

**Document Version**: v1
**Created**: 2025-10-15 16:42:48 PDT
**Last Updated**: 2025-10-15 16:42:48 PDT
**Status**: Living Document (will be updated as implementation evolves)
**Maintainer**: VoiceOS Core Team
