# Voice Command Freeze Fix - Technical Summary

**Date:** 2026-02-02
**Branch:** `claude/fix-voice-command-freeze-Rs0jN`
**Commits:** `b8ce81b9`, `76f389bc`

---

## Issue Description

Voice commands stop executing after 10-20 minutes of use when interacting with apps that generate continuous accessibility events (Device Info, YouTube, Media Player, My Files on RealWear NAV-500).

### Observed Behavior
- `VivokaAndroidEngine.kt` continues to receive speech results and emit them correctly via SharedFlow
- `VoiceOSAccessibilityService.kt` collector stops receiving the emitted results
- Voice commands are recognized by the Vivoka engine but no actions are executed

---

## Root Cause Analysis

### Primary Issue: Coroutine Dispatcher Starvation

| Component | Dispatcher | Problem |
|-----------|------------|---------|
| `VivokaAndroidEngine` emits | `Dispatchers.IO` | Working correctly |
| `VoiceOSAccessibilityService.serviceScope` | `Dispatchers.Default` | Shared with event processing |
| Speech result collection | `Dispatchers.Default` | **Starved for CPU time** |
| Accessibility event handlers | `Dispatchers.Default` | **Saturates thread pool** |

When apps generate rapid accessibility events, `Dispatchers.Default` becomes saturated with:
1. Screen hash generation
2. Element extraction (`extractElements()`)
3. Command generation (`generateCommands()`)
4. Speech engine grammar compilation (`updateCommands()`)

The speech result collector coroutine never gets scheduled to run because other coroutines are consuming all dispatcher threads.

### Secondary Issue: Continuous Speech Engine Updates

Speech engine grammar compilation (Vivoka `setDynamicCommands()`) is an expensive blocking operation. When called on every debounced accessibility event (every 100-300ms), it:
- Blocks the recognizer during compilation
- Creates a backlog of pending updates
- Prevents speech results from being processed

### Tertiary Issue: Non-Actionable Widget Commands

`CommandGenerator` was generating voice commands for all widgets including:
- Text labels (non-clickable)
- Static views without actions
- Decorative elements

This flooded the speech engine grammar with useless commands, making compilation slower.

---

## Fix Implementation

### Commit 1: `b8ce81b9` - Dedicated Speech Dispatcher

**Files Modified:**
- `VoiceOSAccessibilityService.kt`
- `VivokaAndroidEngine.kt`

#### 1.1 Dedicated Speech Collection Dispatcher

Created a single-threaded executor specifically for speech result collection, isolated from accessibility event processing:

```kotlin
private val speechDispatcher = Executors.newSingleThreadExecutor { r ->
    Thread(r, "VoiceOS-SpeechCollector").apply { isDaemon = true }
}.asCoroutineDispatcher()
```

#### 1.2 Buffered Flow Collection

Added buffering to prevent backpressure from blocking the emitter:

```kotlin
voiceOSCore?.speechResults
    ?.buffer(64)  // 64 items buffer
    ?.collect { speechResult ->
        // Process in child coroutine to not block collector
        launch {
            processCommand(speechResult)
        }
    }
```

#### 1.3 Retry Logic with Exponential Backoff

Added resilient collection with automatic restart on failure:

```kotlin
while (isActive && consecutiveFailures < maxConsecutiveFailures) {
    try {
        // Collection logic
        consecutiveFailures = 0  // Reset on success
    } catch (e: Exception) {
        consecutiveFailures++
        val delayMs = baseRestartDelayMs * consecutiveFailures
        delay(delayMs)  // Exponential backoff
    }
}
```

#### 1.4 Increased SharedFlow Buffer Capacity

In `VivokaAndroidEngine.kt`:

```kotlin
private val _results = MutableSharedFlow<SpeechResult>(
    replay = 1,
    extraBufferCapacity = 64  // Prevents emit() blocking
)

private val _errors = MutableSharedFlow<SpeechError>(
    replay = 1,
    extraBufferCapacity = 16
)
```

---

### Commit 2: `76f389bc` - Comprehensive Throttling

**Files Modified:**
- `DeviceCapabilityManager.kt`
- `VoiceOSAccessibilityService.kt`
- `CommandGenerator.kt`

#### 2.1 Increased Debounce Values

| Device Speed | Operation | Before | After | Change |
|--------------|-----------|--------|-------|--------|
| **SLOW** | CONTENT_CHANGE | 300ms | 800ms | +167% |
| **SLOW** | SCROLL | 150ms | 500ms | +233% |
| **SLOW** | SPEECH_ENGINE_UPDATE | 400ms | 2000ms | +400% |
| **MEDIUM** | CONTENT_CHANGE | 200ms | 500ms | +150% |
| **MEDIUM** | SCROLL | 100ms | 300ms | +200% |
| **MEDIUM** | SPEECH_ENGINE_UPDATE | 250ms | 1000ms | +300% |
| **FAST** | CONTENT_CHANGE | 100ms | 250ms | +150% |
| **FAST** | SCROLL | 50ms | 150ms | +200% |
| **FAST** | SPEECH_ENGINE_UPDATE | 150ms | 500ms | +233% |

Made `CONTENT_CHANGE` and `SCROLL` operations skippable (`canSkip = true`) to allow dropping events during high load.

#### 2.2 Speech Engine Update Throttling

New throttled update method with atomic guards:

```kotlin
private val lastSpeechEngineUpdateTime = AtomicLong(0L)
private val isSpeechEngineUpdating = AtomicBoolean(false)
private val isProcessingAccessibilityEvent = AtomicBoolean(false)

private fun throttledSpeechEngineUpdate(phrases: List<String>, forceUpdate: Boolean = false) {
    val config = DeviceCapabilityManager.getTimingConfig(TimingOperation.SPEECH_ENGINE_UPDATE)
    val now = System.currentTimeMillis()
    val lastUpdate = lastSpeechEngineUpdateTime.get()

    // Skip if within minimum interval
    if (!forceUpdate && config.canSkip && (now - lastUpdate < config.minIntervalMs)) {
        return  // Throttled
    }

    // Skip if already updating (atomic compare-and-set)
    if (!isSpeechEngineUpdating.compareAndSet(false, true)) {
        return  // Already updating
    }

    serviceScope.launch {
        try {
            voiceOSCore?.updateCommands(phrases)
            lastSpeechEngineUpdateTime.set(System.currentTimeMillis())
        } finally {
            isSpeechEngineUpdating.set(false)
        }
    }
}
```

#### 2.3 Processing Guard for Event Handlers

Prevents queue buildup when events arrive faster than processing:

```kotlin
private fun handleContentUpdate(event: AccessibilityEvent) {
    // Debounce check
    if (now - lastContentUpdateTime < debounceMs) return

    // Skip if already processing
    if (!isProcessingAccessibilityEvent.compareAndSet(false, true)) {
        return  // Already processing previous event
    }

    serviceScope.launch {
        try {
            // Processing logic
        } finally {
            isProcessingAccessibilityEvent.set(false)
        }
    }
}
```

#### 2.4 Filter Non-Actionable Elements from Command Generation

Updated `CommandGenerator.kt` to require `isClickable || isLongClickable`:

```kotlin
// generateListIndexCommands()
val bestElementPerIndex = listItems
    .filter { it.listIndex >= 0 && (it.isClickable || it.isLongClickable) }
    .groupBy { it.listIndex }
    // ...

// generateNumericCommands()
val bestElementPerIndex = listItems
    .filter { it.listIndex >= 0 && (it.isClickable || it.isLongClickable) }
    // ...

// generateListLabelCommands()
return listItems
    .filter { it.listIndex >= 0 && (it.isClickable || it.isLongClickable) }
    .mapNotNull { element -> /* ... */ }
```

---

## Architecture After Fix

```
┌─────────────────────────────────────────────────────────────────┐
│                    VoiceOSAccessibilityService                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────────┐    ┌────────────────────────────────┐ │
│  │  speechDispatcher    │    │      serviceScope              │ │
│  │  (Single Thread)     │    │   (Dispatchers.Default)        │ │
│  │                      │    │                                │ │
│  │  ┌────────────────┐  │    │  ┌──────────────────────────┐  │ │
│  │  │ Speech Result  │  │    │  │  Accessibility Event     │  │ │
│  │  │ Collection     │  │    │  │  Processing              │  │ │
│  │  │                │  │    │  │                          │  │ │
│  │  │ • buffer(64)   │  │    │  │  • handleContentUpdate() │  │ │
│  │  │ • child launch │  │    │  │  • handleScrollEvent()   │  │ │
│  │  │ • retry logic  │  │    │  │  • handleWindowsChange() │  │ │
│  │  └────────────────┘  │    │  └──────────────────────────┘  │ │
│  └──────────────────────┘    │                                │ │
│          │                   │  ┌──────────────────────────┐  │ │
│          │                   │  │  Throttling Guards       │  │ │
│          ▼                   │  │                          │  │ │
│  ┌──────────────────────┐    │  │  • lastSpeechEngineTime  │  │ │
│  │  VoiceOSCore         │    │  │  • isSpeechEngineUpdating│  │ │
│  │  .speechResults      │◄───┤  │  • isProcessingEvent     │  │ │
│  │  (SharedFlow)        │    │  └──────────────────────────┘  │ │
│  └──────────────────────┘    └────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    VivokaAndroidEngine                           │
├─────────────────────────────────────────────────────────────────┤
│  _results = MutableSharedFlow(                                   │
│      replay = 1,                                                 │
│      extraBufferCapacity = 64  ◄── Prevents emit() blocking     │
│  )                                                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## Testing Recommendations

1. **Device Info App** - Keep open for 30+ minutes with continuous UI updates
2. **YouTube** - Play video and interact with UI for extended period
3. **My Files** - Browse folders with many files, scroll through lists
4. **Media Player** - Play media with progress bar updates

### Expected Behavior After Fix
- Voice commands should continue working beyond 20 minutes
- Log should show "Speech engine update throttled" messages during rapid events
- Log should show "Content update skipped - already processing" during event bursts
- Speech result collection should not show failures

### Logs to Monitor
```
adb logcat | grep -E "(VoiceOSA11yService|VivokaAndroidEngine|Speech)"
```

Look for:
- `Speech result:` - Confirms results are being received
- `Speech engine update throttled` - Confirms throttling is working
- `Content update skipped` - Confirms processing guard is working
- `Speech collection failed` - Should NOT appear frequently

---

## Files Changed Summary

| File | Lines Changed | Purpose |
|------|---------------|---------|
| `VoiceOSAccessibilityService.kt` | +155, -16 | Dedicated dispatcher, throttling, guards |
| `VivokaAndroidEngine.kt` | +26 | SharedFlow buffer capacity |
| `DeviceCapabilityManager.kt` | +32, -17 | Increased debounce values |
| `CommandGenerator.kt` | +18, -7 | Filter non-actionable elements |

---

## Related Issues
- NAV-600: Voice command freeze after extended use
- RealWear NAV-500 specific issue with continuous accessibility events

## Authors
- Analysis and implementation by Claude Code
- Testing and validation by Manoj Jhawar
