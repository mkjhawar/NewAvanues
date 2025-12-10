# JIT-LearnApp Merge - Developer Guide

**Date**: 2025-12-04
**Status**: PLANNING
**Branch**: kmp/main-jitlearnapp-merge
**Related ADRs**: ADR-001, ADR-002, ADR-003

---

## Overview

This guide documents the architectural refactor to unify JIT Mode and Full Exploration Mode into a single, cohesive LearnApp system with a shared core and mode-specific wrappers.

---

## Problem Statement

### Current Architecture (Duplicated)

```
LearnApp System (Current)
├── JIT Mode (JustInTimeLearner.kt)
│   ├── Element capture logic
│   ├── UUID generation ← DUPLICATED
│   ├── Voice command generation ← DUPLICATED
│   └── Database storage
└── Full Exploration (ExplorationEngine.kt)
    ├── Element capture logic
    ├── UUID generation ← DUPLICATED
    ├── Voice command generation ← MISSING!
    └── Database storage
```

**Issues:**
1. ❌ Code duplication (~500 lines duplicated)
2. ❌ Full Exploration doesn't generate voice commands
3. ❌ Changes must be made twice
4. ❌ Risk of modes diverging
5. ❌ Harder to maintain and test

---

## Solution: Unified Architecture

### New Architecture (Shared Core)

```
LearnApp System (Refactored)
├── LearnAppCore.kt ← NEW: Shared logic
│   ├── generateUUID()
│   ├── generateVoiceCommand()
│   ├── generateSynonyms()
│   ├── calculateElementHash()
│   └── processElement() ← Main entry point
│
├── JustInTimeLearner.kt (Thin Wrapper)
│   ├── Uses: LearnAppCore
│   ├── Mode: IMMEDIATE (1 element at a time)
│   ├── Storage: Immediate insert
│   └── Feedback: Toast notification
│
└── ExplorationEngine.kt (Thin Wrapper)
    ├── Uses: LearnAppCore
    ├── Mode: BATCH (100+ elements at once)
    ├── Storage: Batch insert
    └── Feedback: Progress bar
```

**Benefits:**
1. ✅ Zero code duplication
2. ✅ Both modes generate voice commands
3. ✅ Changes apply to both automatically
4. ✅ Modes stay in sync
5. ✅ Easier to test and maintain
6. ✅ Better performance (mode-specific optimizations)

---

## Architecture Decisions

See Architecture Decision Records (ADRs) for detailed rationale:

- **[ADR-001](./adr/ADR-001-extract-learnapp-core.md)**: Extract LearnAppCore from duplicated code
- **[ADR-002](./adr/ADR-002-processing-mode-enum.md)**: Use ProcessingMode enum for mode-specific behavior
- **[ADR-003](./adr/ADR-003-batch-vs-immediate-storage.md)**: Batch storage for Exploration, immediate for JIT

---

## Component Design

### LearnAppCore.kt (New)

**Purpose**: Shared business logic for element learning

**Responsibilities:**
- Generate UUIDs for elements
- Generate voice commands from elements
- Generate command synonyms
- Calculate element hashes for deduplication
- Process elements with mode-specific optimizations

**Public API:**
```kotlin
class LearnAppCore(
    private val database: VoiceOSDatabaseManager,
    private val uuidGenerator: ThirdPartyUuidGenerator
) {
    /**
     * Process element and generate UUID + voice command
     *
     * @param element Element to process
     * @param packageName App package name
     * @param mode Processing mode (IMMEDIATE or BATCH)
     * @return Processing result with UUID and command
     */
    fun processElement(
        element: ElementInfo,
        packageName: String,
        mode: ProcessingMode
    ): ElementProcessingResult

    /**
     * Flush batch queue (for BATCH mode)
     */
    suspend fun flushBatch()
}
```

**Processing Modes:**
```kotlin
enum class ProcessingMode {
    /** JIT Mode: Process 1 element immediately */
    IMMEDIATE,

    /** Exploration Mode: Batch process 100+ elements */
    BATCH
}
```

**Processing Result:**
```kotlin
data class ElementProcessingResult(
    val uuid: String,
    val command: GeneratedCommandDTO?,
    val success: Boolean,
    val error: String? = null
)
```

---

### JustInTimeLearner.kt (Refactored)

**Changes:**
- Remove duplicate code
- Use LearnAppCore for all processing
- Keep JIT-specific logic (toast, immediate storage)

**Before:**
```kotlin
class JustInTimeLearner {
    fun onUserClick(element: ElementInfo) {
        // 500+ lines of UUID + command generation
        val uuid = generateUUID(element)
        val command = generateVoiceCommand(element)
        database.insert(command)
        showToast("Learned: ${command.commandText}")
    }
}
```

**After:**
```kotlin
class JustInTimeLearner(
    private val core: LearnAppCore  // ← Injected
) {
    fun onUserClick(element: ElementInfo) {
        // Use core (10 lines)
        val result = core.processElement(
            element = element,
            packageName = currentPackage,
            mode = ProcessingMode.IMMEDIATE
        )

        // JIT-specific: Show toast
        if (result.success) {
            showToast("Learned: ${result.command?.commandText}")
        }
    }
}
```

---

### ExplorationEngine.kt (Refactored)

**Changes:**
- Remove missing command generation
- Use LearnAppCore for all processing
- Keep Exploration-specific logic (batch, progress)

**Before:**
```kotlin
class ExplorationEngine {
    private fun registerElements(elements: List<ElementInfo>) {
        elements.forEach { element ->
            val uuid = generateUUID(element)
            // ❌ No voice command generation!
            database.insert(uuid)
        }
    }
}
```

**After:**
```kotlin
class ExplorationEngine(
    private val core: LearnAppCore  // ← Injected
) {
    private suspend fun registerElements(elements: List<ElementInfo>) {
        // Process all elements
        val results = elements.map { element ->
            core.processElement(
                element = element,
                packageName = currentPackage,
                mode = ProcessingMode.BATCH
            )
        }

        // Flush batch to database
        core.flushBatch()

        // Exploration-specific: Update progress
        updateProgress(elements.size)
    }
}
```

---

## Migration Strategy

### Phase 1: Extract Core (2 hours)
1. Create `LearnAppCore.kt`
2. Extract UUID generation logic from JIT
3. Extract voice command generation logic from JIT
4. Extract synonym generation logic
5. Add ProcessingMode enum
6. Add batch queue for BATCH mode
7. Unit tests for LearnAppCore

### Phase 2: Refactor JIT (1 hour)
1. Inject LearnAppCore into JustInTimeLearner
2. Replace duplicated code with core.processElement()
3. Keep JIT-specific toast notification
4. Update unit tests
5. Verify JIT mode still works

### Phase 3: Refactor Exploration (1 hour)
1. Inject LearnAppCore into ExplorationEngine
2. Replace registerElements() with core.processElement()
3. Add core.flushBatch() call
4. Keep Exploration-specific progress updates
5. Update unit tests
6. Verify Exploration generates commands

### Phase 4: Integration Testing (2 hours)
1. Test JIT mode end-to-end
2. Test Exploration mode end-to-end
3. Verify voice commands work in both modes
4. Performance testing
5. Memory leak testing

**Total Time**: 6 hours

---

## Testing Strategy

### Unit Tests

**LearnAppCore:**
- `testGenerateUUID()` - UUID generation
- `testGenerateVoiceCommand()` - Command generation
- `testGenerateSynonyms()` - Synonym generation
- `testProcessElementImmediate()` - IMMEDIATE mode
- `testProcessElementBatch()` - BATCH mode
- `testFlushBatch()` - Batch queue flushing

**JustInTimeLearner:**
- `testJitCaptureWithCore()` - Uses LearnAppCore
- `testJitToastNotification()` - JIT-specific feedback
- `testJitImmediateStorage()` - Immediate insert

**ExplorationEngine:**
- `testExplorationWithCore()` - Uses LearnAppCore
- `testExplorationBatchStorage()` - Batch insert
- `testExplorationProgress()` - Progress updates
- `testVoiceCommandGeneration()` - Commands created

### Integration Tests

**End-to-End JIT:**
1. User clicks element
2. JIT captures → LearnAppCore processes
3. Voice command inserted to DB
4. Toast shown
5. VoiceCommandManager notified
6. User can say voice command

**End-to-End Exploration:**
1. Start full exploration
2. Discover 50 elements
3. ExplorationEngine → LearnAppCore processes batch
4. 50 voice commands inserted to DB
5. Progress updated
6. VoiceCommandManager notified
7. User can say all 50 voice commands

---

## Performance Considerations

### Batch Processing (Exploration)

**Before (Missing):**
- No voice commands generated

**After (Optimized):**
- Commands generated in batch
- Single database transaction
- ~100ms for 50 elements

**Implementation:**
```kotlin
// LearnAppCore.kt
private val batchQueue = mutableListOf<GeneratedCommandDTO>()

fun processElement(element: ElementInfo, mode: ProcessingMode) {
    val command = generateVoiceCommand(element)

    when (mode) {
        ProcessingMode.IMMEDIATE -> {
            database.insert(command)  // Insert now
        }
        ProcessingMode.BATCH -> {
            batchQueue.add(command)  // Queue for batch
        }
    }
}

suspend fun flushBatch() {
    if (batchQueue.isEmpty()) return
    database.batchInsert(batchQueue)  // Single transaction
    batchQueue.clear()
}
```

### Memory Management

**JIT Mode:**
- Processes 1 element → ~1KB memory
- No batching needed
- Immediate cleanup

**Exploration Mode:**
- Processes 100 elements → ~100KB memory
- Batch queue ~50KB
- Flush after each screen
- Total: ~150KB peak memory

---

## Error Handling

### LearnAppCore Error Handling

```kotlin
fun processElement(element: ElementInfo, mode: ProcessingMode): ElementProcessingResult {
    return try {
        // 1. Generate UUID
        val uuid = generateUUID(element)

        // 2. Generate command
        val command = generateVoiceCommand(element, uuid)

        // 3. Store (mode-specific)
        when (mode) {
            ProcessingMode.IMMEDIATE -> {
                database.insert(command)
            }
            ProcessingMode.BATCH -> {
                batchQueue.add(command)
            }
        }

        ElementProcessingResult(uuid, command, success = true)

    } catch (e: Exception) {
        Log.e(TAG, "Failed to process element", e)
        ElementProcessingResult(
            uuid = "",
            command = null,
            success = false,
            error = e.message
        )
    }
}
```

### Mode-Specific Error Handling

**JIT Mode:**
- Error → Show toast "Failed to learn element"
- Continue (non-critical)
- Log error for debugging

**Exploration Mode:**
- Error → Log warning
- Continue with next element
- Report error count in summary
- Don't fail entire exploration

---

## Rollback Plan

If refactor fails or causes issues:

### Quick Rollback
1. Revert commit: `git revert <commit-hash>`
2. Redeploy previous version
3. Investigate issues

### Staged Rollback
1. Keep LearnAppCore
2. Revert JIT changes only
3. Test JIT with old code
4. Fix issues
5. Re-apply JIT changes

### Full Rollback
1. Delete `kmp/main-jitlearnapp-merge` branch
2. Return to `kmp/main`
3. Full Exploration still won't generate commands (existing issue)
4. Plan alternative approach

---

## Success Criteria

### Must Have
- ✅ JIT mode still works (no regression)
- ✅ Exploration mode generates voice commands (new feature)
- ✅ Zero code duplication
- ✅ All tests pass
- ✅ Performance equal or better
- ✅ No memory leaks

### Nice to Have
- ⭐ 20% performance improvement (batch optimizations)
- ⭐ Easier to add new learning modes
- ⭐ Better error messages
- ⭐ Cleaner architecture

---

## Future Extensions

### New Learning Modes (Easy to Add)

**Smart JIT (Predictive):**
```kotlin
class SmartJitLearner(private val core: LearnAppCore) {
    fun onUserClick(element: ElementInfo) {
        // Learn clicked element
        core.processElement(element, mode = ProcessingMode.IMMEDIATE)

        // NEW: Learn similar elements automatically
        val similarElements = findSimilarElements(element)
        similarElements.forEach {
            core.processElement(it, mode = ProcessingMode.BATCH)
        }
        core.flushBatch()
    }
}
```

**Hybrid Mode (JIT + Exploration):**
```kotlin
class HybridLearner(private val core: LearnAppCore) {
    fun learn(screen: ScreenState) {
        // Explore visible elements (fast)
        val visible = screen.visibleElements
        visible.forEach {
            core.processElement(it, mode = ProcessingMode.IMMEDIATE)
        }

        // Explore offscreen in background (slow)
        launch {
            val offscreen = screen.offscreenElements
            offscreen.forEach {
                core.processElement(it, mode = ProcessingMode.BATCH)
            }
            core.flushBatch()
        }
    }
}
```

---

## Related Documentation

- [ADR-001: Extract LearnAppCore](./adr/ADR-001-extract-learnapp-core.md)
- [ADR-002: ProcessingMode Enum](./adr/ADR-002-processing-mode-enum.md)
- [ADR-003: Batch vs Immediate Storage](./adr/ADR-003-batch-vs-immediate-storage.md)
- [Implementation Plan](../../plans/jit-learnapp-merge-implementation-plan-251204.md)
- [Tier 1 Implementation](./learnapp-tier1-implementation-summary-251204.md)

---

**Version**: 1.0
**Author**: Claude + Manoj Jhawar
**Review Status**: Pending
**Next Steps**: Create ADRs, then run /iplan
