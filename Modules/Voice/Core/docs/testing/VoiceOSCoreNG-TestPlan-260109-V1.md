# VoiceOSCoreNG Integration Test Plan

**Date:** 2026-01-09
**Version:** 1.0
**Status:** Ready for Testing
**Target Modules:** VoiceOSCoreNG, NLU, LLM

---

## Overview

This document provides comprehensive test cases for validating VoiceOSCoreNG functionality, including:
- Static and dynamic command processing
- Fuzzy matching and synonym expansion
- NLU/LLM integration
- Speech recognition pipeline
- VUID-based element execution

**Test Environment:**
- Android Studio (Logcat monitoring)
- Real Android device and/or emulator
- Minimum API 26 (Android 8.0)

---

## Prerequisites

### 1. Logcat Filter Setup

Create these Logcat filters in Android Studio:

| Filter Name | Tag Pattern |
|-------------|-------------|
| VoiceOS-Core | `VoiceOSCoreNG\|ActionCoordinator\|HandlerRegistry` |
| VoiceOS-Commands | `CommandMatcher\|CommandRegistry\|UIHandler` |
| VoiceOS-Synonym | `SynonymProvider\|SynonymLoader\|CommandMatcher` |
| NLU-Bridge | `UnifiedNluBridge\|UnifiedNluService\|IntentClassifier` |
| LLM-Provider | `LocalLLMProvider\|ALCEngine\|LLMResponse` |

### 2. Enable Debug Mode

In your app initialization:

```kotlin
val core = VoiceOSCoreNG.Builder()
    .withHandlerFactory(factory)
    .withConfiguration(ServiceConfiguration(debugMode = true))
    .withSynonymProvider(SynonymProviderFactory.create(context))
    .build()
```

### 3. Test Data Setup

Ensure synonym files are in assets:
- `assets/synonyms/en.syn` or `assets/synonyms/en.qsyn`

---

## Test Categories

| # | Category | Priority | Status |
|---|----------|----------|--------|
| 1 | Core Initialization | P0 | ⬜ |
| 2 | Static Commands | P0 | ⬜ |
| 3 | Dynamic Commands | P0 | ⬜ |
| 4 | Fuzzy Matching | P1 | ⬜ |
| 5 | Synonym Expansion | P1 | ⬜ |
| 6 | VUID Execution | P1 | ⬜ |
| 7 | NLU Integration | P2 | ⬜ |
| 8 | LLM Integration | P2 | ⬜ |
| 9 | Performance | P2 | ⬜ |

---

## 1. Core Initialization Tests

### TC-1.1: Basic Initialization

**Objective:** Verify VoiceOSCoreNG initializes successfully.

**Steps:**
1. Launch the app
2. Observe Logcat for initialization messages

**Expected Logcat Output:**
```
I/VoiceOSCoreNG: Initializing...
I/VoiceOSCoreNG: Creating handlers
I/VoiceOSCoreNG: Registering handlers
I/VoiceOSCoreNG: Initializing synonyms
I/VoiceOSCoreNG: Synonym provider initialized for en
I/VoiceOSCoreNG: Initializing speech engine
I/VoiceOSCoreNG: Ready (speechEngine=true, handlers=X)
```

**Pass Criteria:**
- [ ] No exceptions thrown
- [ ] State transitions: Uninitialized → Initializing → Ready
- [ ] Handler count > 0

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

### TC-1.2: Synonym Provider Initialization

**Objective:** Verify SynonymProvider loads correctly.

**Steps:**
1. Launch app with synonym files in assets
2. Check Logcat for synonym loading

**Expected Logcat:**
```
I/SynonymLoader: Loading synonyms for: en
I/SynonymLoader: Loaded X synonyms from builtin
I/VoiceOSCoreNG: Synonym provider initialized for en
```

**Pass Criteria:**
- [ ] Synonyms loaded without error
- [ ] At least 50+ synonyms for English

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

### TC-1.3: State Flow Verification

**Objective:** Verify state flow is observable.

**Test Code:**
```kotlin
// In your test/debug activity
lifecycleScope.launch {
    core.state.collect { state ->
        Log.d("StateTest", "State: $state")
    }
}
```

**Expected States During Lifecycle:**
1. `Uninitialized` (before init)
2. `Initializing(progress, stage)` (during init)
3. `Ready(speechEngineActive, handlerCount)` (after init)
4. `Processing(command, confidence)` (during command)
5. `Ready` (back to ready)

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

## 2. Static Command Tests

### TC-2.1: Navigation Commands

**Objective:** Test built-in navigation commands.

| Voice Input | Expected Action | Expected Log |
|-------------|-----------------|--------------|
| "scroll down" | Scroll viewport down | `NavigationHandler: Scrolled down` |
| "scroll up" | Scroll viewport up | `NavigationHandler: Scrolled up` |
| "go back" | Navigate back | `SystemHandler: Went back` |
| "go home" | Go to home screen | `SystemHandler: Went home` |
| "show recents" | Show recent apps | `SystemHandler: Showing recent apps` |

**Test Method:**
```kotlin
val result = core.processCommand("scroll down")
Log.d("Test", "Result: ${result.isSuccess}, message: ${result.message}")
```

**Pass Criteria:**
- [ ] All commands return `HandlerResult.success`
- [ ] Corresponding executor method called
- [ ] State returns to Ready after each command

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

### TC-2.2: Static Command Quantization for NLU

**Objective:** Verify static commands are available in QuantizedCommand format.

**Test Code:**
```kotlin
val staticCommands = core.getStaticQuantizedCommands()
Log.d("StaticCmd", "Count: ${staticCommands.size}")
staticCommands.take(5).forEach { cmd ->
    Log.d("StaticCmd", "- ${cmd.phrase}: ${cmd.actionType}, vuid=${cmd.targetVuid}")
}
```

**Expected Output:**
```
D/StaticCmd: Count: 50+
D/StaticCmd: - go back: BACK, vuid=null
D/StaticCmd: - go home: HOME, vuid=null
D/StaticCmd: - scroll down: SCROLL_DOWN, vuid=null
```

**Pass Criteria:**
- [ ] Static commands have `targetVuid = null`
- [ ] UUID format: `static__{category}__{phrase}`
- [ ] Confidence = 1.0 for all static commands

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

### TC-2.3: NLU Schema Generation

**Objective:** Verify NLU schema is properly formatted for LLM.

**Test Code:**
```kotlin
val schema = core.getNluSchema()
Log.d("NLU", "Schema:\n$schema")
```

**Expected Output:**
```
# Static Voice Commands

## NAVIGATION
- go back: BACK - Navigate to previous screen
  Aliases: navigate back, back, previous screen
- go home: HOME - Go to home screen
  Aliases: home, navigate home, open home
...

## Dynamic Commands (Current Screen)
(No screen-specific commands available)
```

**Pass Criteria:**
- [ ] Schema includes all categories
- [ ] Aliases listed correctly
- [ ] Dynamic section shows placeholder when empty

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

## 3. Dynamic Command Tests

### TC-3.1: Dynamic Command Registration

**Objective:** Test registering screen-specific commands.

**Test Code:**
```kotlin
val commands = listOf(
    QuantizedCommand(
        uuid = "test_cmd_1",
        phrase = "click Submit",
        actionType = CommandActionType.CLICK,
        targetVuid = "vuid_submit_btn_123",
        confidence = 1.0f,
        metadata = mapOf("packageName" to "com.test.app")
    ),
    QuantizedCommand(
        uuid = "test_cmd_2",
        phrase = "click Cancel",
        actionType = CommandActionType.CLICK,
        targetVuid = "vuid_cancel_btn_456",
        confidence = 1.0f
    )
)

core.updateDynamicCommands(commands, updateSpeechEngine = false)

Log.d("DynCmd", "Dynamic count: ${core.dynamicCommandCount}")
```

**Expected:**
- Dynamic count = 2
- Commands retrievable by phrase

**Pass Criteria:**
- [ ] `dynamicCommandCount` returns correct value
- [ ] Commands searchable via `canHandle()`

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

### TC-3.2: Dynamic Command Execution

**Objective:** Test execution of dynamic commands with VUID.

**Test Code:**
```kotlin
// First register command
core.updateDynamicCommands(listOf(
    QuantizedCommand(
        uuid = "submit_cmd",
        phrase = "click Submit",
        actionType = CommandActionType.CLICK,
        targetVuid = "vuid_submit_123",
        confidence = 1.0f
    )
))

// Execute
val result = core.processCommand("click Submit")
```

**Expected Logcat:**
```
I/ActionCoordinator: Processing: click submit
I/CommandMatcher: Exact match found: click Submit
I/UIHandler: VUID path: clicking vuid_submit_123
I/UIExecutor: clickByVuid(vuid_submit_123)
```

**Pass Criteria:**
- [ ] Command matched via CommandRegistry
- [ ] VUID used for execution (NOT text search)
- [ ] `clickByVuid` called, NOT `getScreenElements`

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

### TC-3.3: Dynamic Command Clear

**Objective:** Verify commands are cleared on context change.

**Test Code:**
```kotlin
// Add commands
core.updateDynamicCommands(listOf(...))
Log.d("Test", "Before clear: ${core.dynamicCommandCount}")

// Clear (e.g., when leaving app)
core.clearDynamicCommands()
Log.d("Test", "After clear: ${core.dynamicCommandCount}")

// Try to execute
val result = core.processCommand("click Submit")
Log.d("Test", "Result: ${result.isSuccess}, handled: ${result.status}")
```

**Expected:**
- Count goes from N → 0
- Previously registered command no longer matches

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

## 4. Fuzzy Matching Tests

### TC-4.1: Minor Typo Handling

**Objective:** Test fuzzy matching handles minor typos.

**Test Cases:**

| Input (with typo) | Expected Match | Min Confidence |
|-------------------|----------------|----------------|
| "clik Submit" | click Submit | 0.70 |
| "scrol down" | scroll down | 0.70 |
| "go bak" | go back | 0.70 |
| "submitt" | Submit | 0.70 |

**Test Code:**
```kotlin
// Register "click Submit" command first
core.updateDynamicCommands(listOf(
    QuantizedCommand(phrase = "click Submit", ...)
))

// Test with typo
val result = core.processCommand("clik Submit")
```

**Expected Logcat:**
```
I/CommandMatcher: Fuzzy match: 'clik Submit' → 'click Submit' (score=0.85)
```

**Pass Criteria:**
- [ ] Fuzzy match found for minor typos
- [ ] Confidence score logged
- [ ] Command executed successfully

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

### TC-4.2: Ambiguous Command Handling

**Objective:** Test behavior when multiple commands match.

**Test Code:**
```kotlin
// Register similar commands
core.updateDynamicCommands(listOf(
    QuantizedCommand(phrase = "click Submit Form", ...),
    QuantizedCommand(phrase = "click Submit Button", ...),
    QuantizedCommand(phrase = "click Submit Order", ...)
))

// Ambiguous query
val result = core.processCommand("click Submit")
```

**Expected:**
```
I/CommandMatcher: Ambiguous: 3 candidates for 'click Submit'
W/ActionCoordinator: Awaiting selection: 3 matches found
```

**Pass Criteria:**
- [ ] Returns `HandlerResult.awaitingSelection`
- [ ] Match count provided
- [ ] Accessibility announcement set

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

## 5. Synonym Expansion Tests

### TC-5.1: Basic Synonym Expansion

**Objective:** Verify synonyms expand voice input.

**Standard Synonyms (from en.syn):**
| User Says | Expands To | Action |
|-----------|------------|--------|
| "tap Submit" | "click Submit" | CLICK |
| "press Submit" | "click Submit" | CLICK |
| "hit Submit" | "click Submit" | CLICK |
| "page down" | "scroll down" | SCROLL_DOWN |

**Test Code:**
```kotlin
// Register with canonical phrase
core.updateDynamicCommands(listOf(
    QuantizedCommand(phrase = "click Submit", ...)
))

// Say synonym
val result = core.processCommand("tap Submit")
```

**Expected Logcat:**
```
I/CommandMatcher: Expanding 'tap submit' with synonyms
I/SynonymProvider: tap → click
I/CommandMatcher: Expanded to: 'click submit'
I/CommandMatcher: Exact match found: click Submit
```

**Pass Criteria:**
- [ ] Synonym expanded before matching
- [ ] Original phrase logged
- [ ] Expanded phrase matches command

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

### TC-5.2: Multi-Word Synonym Expansion

**Objective:** Test multi-word phrase synonyms.

| User Says | Expands To |
|-----------|------------|
| "long press X" | "long click X" |
| "hold down X" | "long click X" |
| "right click X" | "long click X" |

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

### TC-5.3: Language-Specific Synonyms

**Objective:** Test non-English synonym files.

**Test Code:**
```kotlin
// Configure for Spanish
val config = ServiceConfiguration(
    voiceLanguage = "es-ES",
    synonymsEnabled = true,
    synonymLanguage = "es"
)
```

**Required:** `assets/synonyms/es.syn` or `es.qsyn`

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

## 6. VUID Execution Tests

### TC-6.1: VUID Priority Over Text Search

**Objective:** Verify VUID path is used when available.

**Setup:**
1. Register command with VUID
2. Have UI element with same text visible

**Test Code:**
```kotlin
core.updateDynamicCommands(listOf(
    QuantizedCommand(
        phrase = "click Submit",
        targetVuid = "vuid_abc123",
        actionType = CommandActionType.CLICK,
        ...
    )
))

val result = core.processCommand("click Submit")
```

**Expected Logcat:**
```
I/UIHandler: VUID path: vuid_abc123
I/UIExecutor: clickByVuid(vuid_abc123) - SUCCESS
```

**NOT Expected:**
```
I/UIHandler: Text search path: Submit
I/UIExecutor: getScreenElements() called
```

**Pass Criteria:**
- [ ] `clickByVuid` called
- [ ] `getScreenElements` NOT called
- [ ] Execution faster (no tree traversal)

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

### TC-6.2: Fallback to Text Search

**Objective:** Verify text search when no VUID.

**Test Code:**
```kotlin
// Command without VUID
core.updateDynamicCommands(listOf(
    QuantizedCommand(
        phrase = "click Submit",
        targetVuid = null, // No VUID
        actionType = CommandActionType.CLICK,
        ...
    )
))

val result = core.processCommand("click Submit")
```

**Expected:**
```
I/UIHandler: No VUID, using text search
I/UIExecutor: getScreenElements()
I/ElementDisambiguator: Found 1 match for 'Submit'
```

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

## 7. NLU Integration Tests

### TC-7.1: NLU Bridge Initialization

**Objective:** Verify UnifiedNluBridge initializes.

**Test Code:**
```kotlin
val bridge = UnifiedNluBridge.getInstance(context)
val result = bridge.initialize()

Log.d("NLU", "Init: ${result.success}, intents: ${result.sharedIntentCount}")
```

**Expected:**
```
I/UnifiedNluBridge: Initializing UnifiedNluBridge...
I/UnifiedNluBridge: ✅ Bridge initialized: X shared intents
```

**Pass Criteria:**
- [ ] `result.success = true`
- [ ] Shared intent count > 0
- [ ] No exceptions

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

### TC-7.2: Fast Pattern Classification

**Objective:** Test fast-path pattern matching.

**Test Code:**
```kotlin
val match = bridge.classifyFast("play music")
Log.d("NLU", "Fast match: ${match?.intent?.id}, score: ${match?.score}")
```

**Expected:**
- Match found in < 5ms
- High confidence for known patterns

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

### TC-7.3: Hybrid Classification

**Objective:** Test pattern + BERT hybrid classification.

**Test Code:**
```kotlin
val result = bridge.classifyHybrid(
    utterance = "I want to listen to some tunes",
    intentClassifier = bertClassifier,
    candidateIntents = listOf("play_music", "search", "navigate")
)

Log.d("NLU", "Method: ${result.method}, intent: ${result.intent}, confidence: ${result.confidence}")
```

**Expected Methods:**
- `PATTERN_ONLY` - Fast path for known phrases
- `BERT_ONLY` - When no pattern match
- `HYBRID` - Combined when both available

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

## 8. LLM Integration Tests

### TC-8.1: Local LLM Provider

**Objective:** Test local on-device LLM.

**Prerequisites:**
- LLM model downloaded
- Sufficient device memory (4GB+ RAM)

**Test Code:**
```kotlin
val provider = LocalLLMProvider(context)
val result = provider.generate(
    prompt = "What is the weather like today?",
    maxTokens = 50
)

Log.d("LLM", "Response: ${result.text}")
Log.d("LLM", "Latency: ${result.latencyMs}ms")
```

**Pass Criteria:**
- [ ] Response generated
- [ ] Latency < 5000ms for first token
- [ ] No OOM errors

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

### TC-8.2: LLM with VoiceOS Commands Context

**Objective:** Test LLM understanding of available commands.

**Test Code:**
```kotlin
val schema = core.getNluSchema()
val prompt = """
You are a voice assistant. Available commands:
$schema

User said: "I want to go to the main screen"
Which command should execute? Respond with just the command phrase.
"""

val response = llmProvider.generate(prompt, maxTokens = 20)
Log.d("LLM", "LLM suggested: ${response.text}")
```

**Expected:**
- LLM responds with "go home" or similar navigation command

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

### TC-8.3: Cloud LLM Fallback

**Objective:** Test fallback to cloud when local fails.

**Test Code:**
```kotlin
// Force local failure by using unavailable model
val config = LLMConfig(preferLocal = true, fallbackToCloud = true)
val provider = HybridLLMProvider(config)

val result = provider.generate("Hello")
Log.d("LLM", "Provider used: ${result.providerUsed}")
```

**Expected:**
- Falls back to cloud (Anthropic/OpenAI)
- `providerUsed` indicates cloud provider

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

## 9. Performance Tests

### TC-9.1: Command Processing Latency

**Objective:** Measure command processing time.

**Test Code:**
```kotlin
val iterations = 100
val times = mutableListOf<Long>()

repeat(iterations) {
    val start = System.currentTimeMillis()
    core.processCommand("scroll down")
    times.add(System.currentTimeMillis() - start)
}

Log.d("Perf", "Avg: ${times.average()}ms")
Log.d("Perf", "P50: ${times.sorted()[50]}ms")
Log.d("Perf", "P99: ${times.sorted()[99]}ms")
```

**Target Metrics:**
| Metric | Target | Actual |
|--------|--------|--------|
| P50 latency | < 50ms | ⬜ |
| P99 latency | < 200ms | ⬜ |
| Avg latency | < 75ms | ⬜ |

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

### TC-9.2: Dynamic Command Lookup Performance

**Objective:** Test lookup speed with many commands.

**Test Code:**
```kotlin
// Register 500 commands
val commands = (1..500).map { i ->
    QuantizedCommand(
        phrase = "click Element $i",
        targetVuid = "vuid_$i",
        ...
    )
}
core.updateDynamicCommands(commands)

// Measure lookup
val start = System.currentTimeMillis()
repeat(100) {
    core.processCommand("click Element 250")
}
val avgMs = (System.currentTimeMillis() - start) / 100.0

Log.d("Perf", "Avg lookup with 500 commands: ${avgMs}ms")
```

**Target:** < 10ms average lookup

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

### TC-9.3: Memory Usage

**Objective:** Monitor memory during operation.

**Steps:**
1. Open Android Studio Memory Profiler
2. Initialize VoiceOSCoreNG
3. Register 1000 dynamic commands
4. Process 100 commands
5. Clear commands
6. Run GC
7. Check for memory leaks

**Pass Criteria:**
- [ ] Heap returns to baseline after clear
- [ ] No retained objects from cleared commands
- [ ] Peak usage < 50MB for VoiceOSCoreNG

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Blocked

---

## Appendix A: Logcat Commands

```bash
# Filter VoiceOS logs only
adb logcat -s VoiceOSCoreNG:* ActionCoordinator:* CommandMatcher:*

# Save logs to file
adb logcat -d > voiceos_test_$(date +%Y%m%d_%H%M%S).log

# Clear logs before test
adb logcat -c

# Show with timestamps
adb logcat -v time | grep -E "VoiceOS|Command|Handler"
```

---

## Appendix B: Debug Commands

Add to your debug menu or test activity:

```kotlin
// Dump all commands
fun dumpCommands() {
    Log.d("Debug", "=== Static Commands ===")
    core.getStaticQuantizedCommands().forEach {
        Log.d("Debug", "${it.phrase} -> ${it.actionType}")
    }

    Log.d("Debug", "=== Dynamic Commands ===")
    Log.d("Debug", "Count: ${core.dynamicCommandCount}")

    Log.d("Debug", "=== NLU Schema ===")
    Log.d("Debug", core.getNluSchema())

    Log.d("Debug", "=== AVU Format ===")
    Log.d("Debug", core.getCommandsAsAvu())
}

// Dump metrics
fun dumpMetrics() {
    val metrics = core.getMetricsSummary()
    Log.d("Debug", "Commands processed: ${metrics.totalCommands}")
    Log.d("Debug", "Success rate: ${metrics.successRate}%")
    Log.d("Debug", "Avg latency: ${metrics.avgLatencyMs}ms")
}

// Dump debug info
suspend fun dumpDebug() {
    Log.d("Debug", core.getDebugInfo())
}
```

---

## Appendix C: Test Result Summary Template

| Category | Tests | Passed | Failed | Blocked |
|----------|-------|--------|--------|---------|
| Core Initialization | 3 | ⬜ | ⬜ | ⬜ |
| Static Commands | 3 | ⬜ | ⬜ | ⬜ |
| Dynamic Commands | 3 | ⬜ | ⬜ | ⬜ |
| Fuzzy Matching | 2 | ⬜ | ⬜ | ⬜ |
| Synonym Expansion | 3 | ⬜ | ⬜ | ⬜ |
| VUID Execution | 2 | ⬜ | ⬜ | ⬜ |
| NLU Integration | 3 | ⬜ | ⬜ | ⬜ |
| LLM Integration | 3 | ⬜ | ⬜ | ⬜ |
| Performance | 3 | ⬜ | ⬜ | ⬜ |
| **TOTAL** | **25** | **⬜** | **⬜** | **⬜** |

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-09 | Claude Code | Initial test plan |

---

*End of Document*
