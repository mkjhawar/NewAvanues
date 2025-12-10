# Kotlin Flow Types Guide

**Last Updated:** 2025-11-13
**Author:** VoiceOS Development Team

---

## Overview

This guide helps you choose the correct Kotlin Flow type for your use case in VoiceOS. Choosing the wrong Flow type can lead to subtle bugs like missed events or unnecessary emissions.

**Related:** [ADR-001: StateFlow vs SharedFlow for Voice Commands](../architecture/decisions/ADR-001-stateflow-vs-sharedflow-for-voice-commands.md)

---

## Quick Decision Tree

```
Is this data representing...

├─ Current condition/status? (e.g., "is listening", "current temp")
│  └─ Use StateFlow
│
└─ Discrete occurrence/event? (e.g., "button clicked", "command received")
   └─ Use SharedFlow
```

---

## StateFlow

### When to Use

Use **StateFlow** when you need to represent **current state** or **latest value**:

✅ **Engine status** (isListening, isInitialized, isConnected)
✅ **Configuration** (current language, current theme)
✅ **UI state** (loading, error, success)
✅ **Sensor readings** (current temperature, GPS location)
✅ **User profile** (current user, logged in status)

### Characteristics

- **Conflates emissions**: Only emits when value changes
- **Always has a value**: Requires initial value
- **Deduplicates**: Skips emission if `newValue == previousValue`
- **Replay = 1**: New collectors immediately get latest value
- **Hot**: Stays active even with no collectors

### Example

```kotlin
// Engine state
private val _engineState = MutableStateFlow(EngineState.STOPPED)
val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

// Update state
_engineState.value = EngineState.RUNNING

// Collect (only gets changes)
engineState.collect { state ->
    when (state) {
        EngineState.RUNNING -> startProcessing()
        EngineState.STOPPED -> stopProcessing()
    }
}
```

### Anti-Pattern ❌

**Don't use StateFlow for events:**

```kotlin
// ❌ WRONG: Voice commands are events, not state
private val _voiceCommands = MutableStateFlow<String>("")

// Problem: If same command spoken twice, second emission might not trigger
_voiceCommands.value = "open settings"  // Emits
_voiceCommands.value = "open settings"  // Might NOT emit (same value)
```

---

## SharedFlow

### When to Use

Use **SharedFlow** when you need to emit **discrete events**:

✅ **Voice commands** (each command is distinct)
✅ **User actions** (clicks, swipes, taps)
✅ **Notifications** (each notification is an event)
✅ **Error events** (each error occurrence)
✅ **Analytics events** (track user actions)
✅ **WebSocket messages** (incoming messages)

### Characteristics

- **No conflation**: Every emission triggers collection
- **No initial value**: Cold until first emission
- **Allows duplicates**: Emits even if value is same
- **Configurable replay**: Can replay N previous events
- **Configurable buffer**: Can buffer events if collector is slow
- **Hot**: Can stay active without collectors (if configured)

### Example

```kotlin
// Command events
private val _commandEvents = MutableSharedFlow<CommandEvent>(
    replay = 0,                        // Don't replay to new collectors
    extraBufferCapacity = 10,          // Buffer 10 events if collector is slow
    onBufferOverflow = BufferOverflow.DROP_OLDEST  // Drop oldest if buffer full
)
val commandEvents: SharedFlow<CommandEvent> = _commandEvents.asSharedFlow()

// Emit event
_commandEvents.emit(CommandEvent("open settings"))
_commandEvents.emit(CommandEvent("open settings"))  // Both will trigger collection

// Collect (gets every event)
commandEvents.collect { event ->
    processCommand(event.command)  // Called for EVERY emission
}
```

### Buffer Configuration

```kotlin
// No buffer - collector must keep up (or events dropped)
MutableSharedFlow<Event>(
    extraBufferCapacity = 0,
    onBufferOverflow = BufferOverflow.SUSPEND  // Suspends emitter if collector slow
)

// Small buffer - handles burst events
MutableSharedFlow<Event>(
    extraBufferCapacity = 10,
    onBufferOverflow = BufferOverflow.DROP_OLDEST  // Drop old events if full
)

// Large buffer - never drop events (memory cost)
MutableSharedFlow<Event>(
    extraBufferCapacity = 100,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)
```

---

## Common Patterns in VoiceOS

### Pattern 1: Dual Flow Architecture (Recommended)

**Use case:** Speech recognition system

```kotlin
class SpeechEngineManager {
    // StateFlow: Engine state (current condition)
    private val _engineState = MutableStateFlow(EngineState())
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    // SharedFlow: Command events (discrete occurrences)
    private val _commandEvents = MutableSharedFlow<CommandEvent>(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val commandEvents: SharedFlow<CommandEvent> = _commandEvents.asSharedFlow()

    private fun onRecognitionResult(text: String, confidence: Float) {
        // Update state
        _engineState.value = _engineState.value.copy(
            lastRecognition = text,
            isProcessing = false
        )

        // Emit event
        _commandEvents.emit(CommandEvent(text, confidence))
    }
}

class VoiceOSService {
    fun initializeRecognition() {
        serviceScope.launch {
            // Collect state (for monitoring)
            launch {
                speechEngine.engineState.collect { state ->
                    updateUI(state)
                }
            }

            // Collect events (for processing)
            launch {
                speechEngine.commandEvents.collect { event ->
                    processCommand(event.command)
                }
            }
        }
    }
}
```

**Why this works:**
- State monitoring doesn't interfere with event processing
- Can handle rapid consecutive identical commands
- Clear separation of concerns

---

### Pattern 2: StateFlow for Configuration

**Use case:** App settings, user preferences

```kotlin
class SettingsManager {
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun updateLanguage(lang: String) {
        _settings.value = _settings.value.copy(language = lang)
    }
}

// Usage
settingsManager.settings.collect { settings ->
    configureUIForLanguage(settings.language)
}
```

---

### Pattern 3: SharedFlow for Analytics

**Use case:** User action tracking

```kotlin
class AnalyticsManager {
    private val _events = MutableSharedFlow<AnalyticsEvent>(
        extraBufferCapacity = 50,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    suspend fun trackEvent(event: AnalyticsEvent) {
        _events.emit(event)
    }
}

// Usage
analyticsManager.events.collect { event ->
    sendToBackend(event)
}
```

---

## Common Mistakes

### Mistake 1: Using StateFlow for Commands

```kotlin
// ❌ WRONG
val commands = MutableStateFlow<String>("")

// Problem: Same command twice won't emit twice
commands.value = "back"  // Emits
commands.value = "back"  // Might not emit (same value)
```

**Fix:**
```kotlin
// ✅ CORRECT
val commands = MutableSharedFlow<String>()

commands.emit("back")  // Emits
commands.emit("back")  // Also emits (separate event)
```

---

### Mistake 2: Using SharedFlow for State

```kotlin
// ❌ WRONG
val isConnected = MutableSharedFlow<Boolean>()

// Problem: New collectors don't know current state
viewModelScope.launch {
    isConnected.emit(true)
}

// Later, new collector starts - doesn't know if connected!
isConnected.collect { connected ->
    // Won't trigger until NEXT emission
}
```

**Fix:**
```kotlin
// ✅ CORRECT
val isConnected = MutableStateFlow(false)

// New collectors immediately get current value
isConnected.collect { connected ->
    // Triggers immediately with current state
}
```

---

### Mistake 3: Not Handling Buffer Overflow

```kotlin
// ❌ RISKY
val events = MutableSharedFlow<Event>()  // No buffer!

// If collector is slow, events will be dropped silently
```

**Fix:**
```kotlin
// ✅ BETTER
val events = MutableSharedFlow<Event>(
    extraBufferCapacity = 10,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)

// Or handle backpressure
val events = MutableSharedFlow<Event>(
    onBufferOverflow = BufferOverflow.SUSPEND  // Suspends emitter
)
```

---

## Code Review Checklist

When reviewing Flow-based code, check:

- [ ] **StateFlow used for state?** (current condition, latest value)
- [ ] **SharedFlow used for events?** (discrete occurrences)
- [ ] **SharedFlow has buffer configured?** (extraBufferCapacity, onBufferOverflow)
- [ ] **Collectors properly scoped?** (lifecycleScope, viewModelScope)
- [ ] **No memory leaks?** (collectors cancelled when not needed)
- [ ] **Thread safety?** (emissions on correct dispatcher)

---

## Performance Considerations

### StateFlow
- ✅ Low memory (only stores latest value)
- ✅ Efficient (conflates rapid updates)
- ⚠️ Hot (stays active even without collectors)

### SharedFlow
- ⚠️ Higher memory (stores buffer)
- ⚠️ Can accumulate events if no collectors
- ✅ Flexible (configurable replay, buffer)

**Recommendation:** Use StateFlow by default for state, SharedFlow only when you need event semantics.

---

## Testing

### Testing StateFlow

```kotlin
@Test
fun `test state updates`() = runTest {
    val stateFlow = MutableStateFlow(0)

    stateFlow.test {
        assertEquals(0, awaitItem())  // Initial value

        stateFlow.value = 1
        assertEquals(1, awaitItem())

        stateFlow.value = 1  // Same value
        // No emission (StateFlow deduplicates)

        stateFlow.value = 2
        assertEquals(2, awaitItem())
    }
}
```

### Testing SharedFlow

```kotlin
@Test
fun `test event emissions`() = runTest {
    val sharedFlow = MutableSharedFlow<String>()

    sharedFlow.test {
        sharedFlow.emit("event1")
        assertEquals("event1", awaitItem())

        sharedFlow.emit("event1")  // Same value
        assertEquals("event1", awaitItem())  // Still emits!

        sharedFlow.emit("event2")
        assertEquals("event2", awaitItem())
    }
}
```

---

## Related Reading

- [Kotlin StateFlow Documentation](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/)
- [Kotlin SharedFlow Documentation](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-shared-flow/)
- [ADR-001: StateFlow vs SharedFlow](../architecture/decisions/ADR-001-stateflow-vs-sharedflow-for-voice-commands.md)
- [VoiceOS Voice Command Fix](../fixes/VoiceOSCore-voice-command-flow-2025-11-13.md)

---

## Questions?

If you're unsure which Flow type to use, ask yourself:

1. **Will the same value occur multiple times and need to be processed each time?**
   - Yes → SharedFlow
   - No → StateFlow

2. **Do new collectors need to know the current value immediately?**
   - Yes → StateFlow
   - No → SharedFlow

3. **Is this representing a state or an event?**
   - State → StateFlow
   - Event → SharedFlow

When in doubt, **prefer StateFlow** (simpler, more efficient) unless you specifically need event semantics.
