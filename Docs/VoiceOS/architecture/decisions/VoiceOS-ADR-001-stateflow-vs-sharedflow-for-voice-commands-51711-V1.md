# ADR-001: StateFlow vs SharedFlow for Voice Commands

**Date:** 2025-11-13
**Status:** Accepted
**Deciders:** Development Team
**Context:** Voice command flow bug (only first command processed after service enable)

---

## Context

Voice commands were recognized by the Vivoka speech engine but only the **first command** reached VoiceOSService for processing after enabling the accessibility service. Subsequent commands were recognized (logged in `SpeechEngineManager`) but did not trigger the command processing flow in `VoiceOSService`.

### Technical Background

The original implementation used **StateFlow** for both:
1. **Engine state** (isListening, isInitialized, engineStatus)
2. **Voice commands** (recognized command text)

StateFlow has these characteristics:
- **Conflates emissions**: Only emits when value changes
- **Deduplicates**: Skips emission if new value equals previous value
- **State-oriented**: Designed for representing current state, not discrete events

### The Problem

When a voice command was processed:
1. SpeechEngineManager emitted new command via `_speechState.value = state.copy(fullTranscript = newCommand)`
2. VoiceOSService collected with `speechState.collectLatest { ... }`
3. After processing first command, state remained with `fullTranscript` set
4. Next command updated state, but **StateFlow behavior** didn't guarantee new emission to collectors
5. `collectLatest` may cancel previous collection, causing race conditions

**Root Cause:** Mixing **events** (voice commands - discrete occurrences) with **state** (engine status - current condition) in a single StateFlow.

---

## Decision

**Separate state from events using two distinct flows:**

### Option 1: StateFlow for Engine State
```kotlin
// Engine lifecycle state (current condition)
private val _speechState = MutableStateFlow(SpeechState())
val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()
```

### Option 2: SharedFlow for Command Events
```kotlin
// Command events (discrete occurrences)
private val _commandEvents = MutableSharedFlow<CommandEvent>(
    extraBufferCapacity = 10,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)
val commandEvents: SharedFlow<CommandEvent> = _commandEvents.asSharedFlow()
```

### Architecture Principle

**Use the right Flow type for the right semantics:**

| Use Case | Flow Type | Why |
|----------|-----------|-----|
| **Engine status** (isListening, errors) | StateFlow | Current condition that changes over time |
| **Voice commands** | SharedFlow | Discrete events that must all be delivered |
| **Configuration** | StateFlow | Current settings |
| **User actions** (clicks, taps) | SharedFlow | Each action is distinct |
| **Sensor data** | StateFlow | Current value (temperature, GPS) |
| **Notifications** | SharedFlow | Each notification is an event |

---

## Consequences

### Positive

✅ **Reliability**: Every command triggers collection (no missed commands)
✅ **Architectural correctness**: Commands are events, not state
✅ **No race conditions**: SharedFlow doesn't cancel previous collectors
✅ **No timing dependencies**: No need for delays or manual state clearing
✅ **Extensible**: Can add more event types (errors, partial results)
✅ **Clear separation**: State monitoring vs event processing are independent

### Negative

⚠️ **Slightly more code**: Two flows instead of one (~30 lines added)
⚠️ **Two collectors**: VoiceOSService must maintain two separate collection loops
⚠️ **Learning curve**: Developers must understand StateFlow vs SharedFlow semantics

### Neutral

➡️ **Migration**: Existing StateFlow monitoring unaffected (backward compatible)
➡️ **Performance**: Negligible (SharedFlow with buffer is efficient)

---

## Implementation

### SpeechEngineManager.kt

```kotlin
// Added SharedFlow for command events
private val _commandEvents = MutableSharedFlow<CommandEvent>(
    extraBufferCapacity = 10,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)
val commandEvents: SharedFlow<CommandEvent> = _commandEvents.asSharedFlow()

// Modified handleSpeechResult() to emit events
private fun handleSpeechResult(result: RecognitionResult) {
    // Update state (for monitoring)
    _speechState.value = _speechState.value.copy(...)

    // Emit command event (for processing)
    engineScope.launch {
        _commandEvents.emit(CommandEvent(
            command = result.text,
            confidence = result.confidence
        ))
    }
}

// New data class
data class CommandEvent(
    val command: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)
```

### VoiceOSService.kt

```kotlin
private fun initializeVoiceRecognition() {
    speechEngineManager.initializeEngine(SpeechEngine.VIVOKA)

    serviceScope.launch {
        // Collector 1: Monitor engine state
        launch {
            speechEngineManager.speechState.collectLatest { state ->
                // Handle initialization, start listening
            }
        }

        // Collector 2: Process command events
        launch {
            speechEngineManager.commandEvents.collect { event ->
                if (event.confidence > 0.5f && event.command.isNotBlank()) {
                    handleVoiceCommand(event.confidence, event.command)
                }
            }
        }
    }
}
```

---

## Alternatives Considered

### Alternative 1: Manual State Clearing
**Rejected** - Band-aid solution, doesn't address architectural issue

### Alternative 2: Auto-Clear with Delay
**Rejected** - Timing-based, fragile, race conditions

### Alternative 3: Full StateFlow → SharedFlow Migration
**Rejected** - Overkill, breaks existing functionality, high risk

---

## Related Decisions

- None (first ADR)

---

## References

- [Kotlin StateFlow Documentation](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/)
- [Kotlin SharedFlow Documentation](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-shared-flow/)
- [Fix Analysis Document](../fixes/VoiceOSCore-voice-command-flow-2025-11-13.md)

---

## Notes

This decision establishes a **design principle** for future development:

**When choosing between StateFlow and SharedFlow, ask:**
- Is this a **state** (current condition)? → StateFlow
- Is this an **event** (discrete occurrence)? → SharedFlow

This principle applies to all Flow-based communication in VoiceOS.
