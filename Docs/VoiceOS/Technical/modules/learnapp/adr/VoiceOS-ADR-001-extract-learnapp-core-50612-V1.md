# ADR-001: Extract LearnAppCore from Duplicated Code

**Status**: Proposed
**Date**: 2025-12-04
**Deciders**: Manoj Jhawar, Development Team
**Technical Story**: JIT-LearnApp Code Merge

---

## Context and Problem Statement

LearnApp currently has two learning modes (JIT and Full Exploration) with ~500 lines of duplicated code for UUID generation and voice command generation. Full Exploration mode is missing voice command generation entirely, making discovered elements unusable with voice control.

**Current Issues:**
- Code duplication (~500 lines)
- Full Exploration doesn't generate voice commands
- Bug fixes must be applied twice
- Risk of modes diverging over time
- Harder to maintain and test

---

## Decision Drivers

- **Code Quality**: Eliminate duplication (DRY principle)
- **Maintainability**: Fix bugs once, not twice
- **Consistency**: Ensure both modes work identically
- **Performance**: Don't sacrifice mode-specific optimizations
- **Testability**: Easier to test shared logic

---

## Considered Options

### Option 1: Extract LearnAppCore (Recommended)

**Description**: Create `LearnAppCore.kt` with shared business logic. JIT and Exploration modes become thin wrappers.

**Architecture**:
```
LearnAppCore (Shared Logic)
├── generateUUID()
├── generateVoiceCommand()
├── generateSynonyms()
└── processElement()

JustInTimeLearner (Wrapper)
└── Uses LearnAppCore + JIT-specific logic

ExplorationEngine (Wrapper)
└── Uses LearnAppCore + Exploration-specific logic
```

**Pros**:
- ✅ Zero code duplication
- ✅ Single source of truth
- ✅ Easier to test (test core once)
- ✅ Mode-specific optimizations preserved (wrappers)
- ✅ Easy to add new learning modes
- ✅ Clear separation of concerns

**Cons**:
- ⚠️ Requires refactoring both modes
- ⚠️ 6 hours implementation time
- ⚠️ Risk of regressions during refactor

---

### Option 2: Copy JIT Logic to Exploration (Quick Fix)

**Description**: Copy voice command generation from JIT mode to Exploration mode.

**Pros**:
- ✅ Quick fix (4 hours)
- ✅ Less code changes
- ✅ Lower regression risk

**Cons**:
- ❌ Code duplication persists
- ❌ Still need to fix bugs twice
- ❌ Technical debt increases
- ❌ Modes can still diverge

---

### Option 3: Merge JIT into Exploration

**Description**: Delete JIT mode, make Exploration handle both active and passive learning.

**Pros**:
- ✅ One codebase
- ✅ No duplication

**Cons**:
- ❌ Loss of JIT-specific optimizations
- ❌ Worse performance for passive learning
- ❌ More complex single class
- ❌ Harder to test

---

## Decision Outcome

**Chosen Option**: **Option 1 - Extract LearnAppCore**

**Rationale:**
1. Eliminates code duplication permanently
2. Same time investment as quick fix (6 vs 4 hours)
3. Much better long-term architecture
4. Preserves mode-specific optimizations
5. Easier to extend with new learning modes
6. Testability improvements

---

## Implementation Plan

### Phase 1: Extract Core (2 hours)
```kotlin
// New file: LearnAppCore.kt
class LearnAppCore(
    private val database: VoiceOSDatabaseManager,
    private val uuidGenerator: ThirdPartyUuidGenerator
) {
    fun generateUUID(element: ElementInfo, packageName: String): String
    fun generateVoiceCommand(element: ElementInfo, uuid: String): GeneratedCommandDTO?
    fun generateSynonyms(actionType: String, label: String): String
    fun processElement(element: ElementInfo, mode: ProcessingMode): ElementProcessingResult
    suspend fun flushBatch()
}
```

### Phase 2: Refactor JIT (1 hour)
```kotlin
class JustInTimeLearner(
    private val core: LearnAppCore  // Inject core
) {
    fun onUserClick(element: ElementInfo) {
        val result = core.processElement(element, ProcessingMode.IMMEDIATE)
        if (result.success) {
            showToast("Learned: ${result.command?.commandText}")
        }
    }
}
```

### Phase 3: Refactor Exploration (1 hour)
```kotlin
class ExplorationEngine(
    private val core: LearnAppCore  // Inject core
) {
    private suspend fun registerElements(elements: List<ElementInfo>) {
        elements.forEach { core.processElement(it, ProcessingMode.BATCH) }
        core.flushBatch()
        updateProgress(elements.size)
    }
}
```

### Phase 4: Testing (2 hours)
- Unit tests for LearnAppCore
- Integration tests for both modes
- Performance testing
- Memory leak testing

---

## Consequences

### Positive

- ✅ **Code Quality**: DRY principle applied
- ✅ **Maintainability**: Fix bugs once
- ✅ **Consistency**: Modes always in sync
- ✅ **Testability**: Test core logic once
- ✅ **Extensibility**: Easy to add new modes

### Negative

- ⚠️ **Complexity**: One more class to understand
- ⚠️ **Migration Risk**: Potential regressions during refactor
- ⚠️ **Testing Overhead**: Need comprehensive tests

### Neutral

- 🔵 **Performance**: Equal (batch optimizations preserved)
- 🔵 **Memory**: Equal (~150KB peak for Exploration)

---

## Validation

### Success Criteria

**Must Have:**
- ✅ JIT mode works (no regression)
- ✅ Exploration generates voice commands (new feature)
- ✅ Zero code duplication
- ✅ All tests pass
- ✅ Performance equal or better

**Nice to Have:**
- ⭐ 20% performance improvement
- ⭐ Easier to add new modes
- ⭐ Better error messages

### Testing Strategy

1. **Unit Tests**: Test LearnAppCore independently
2. **Integration Tests**: Test JIT and Exploration end-to-end
3. **Performance Tests**: Compare before/after
4. **Memory Tests**: Check for leaks

### Rollback Plan

If issues arise:
1. Revert commit: `git revert <commit-hash>`
2. Return to kmp/main
3. Apply quick fix (Option 2) instead

---

## Related Decisions

- [ADR-002: ProcessingMode Enum](./ADR-002-processing-mode-enum.md)
- [ADR-003: Batch vs Immediate Storage](./ADR-003-batch-vs-immediate-storage.md)

---

## More Information

- **Implementation Plan**: `../../plans/jit-learnapp-merge-implementation-plan-251204.md`
- **Developer Guide**: `../jit-learnapp-merge-developer-guide-251204.md`
- **Codebase Analysis**: `../../specifications/learnapp-codebase-analysis-251204.md`

---

**Status**: Proposed → Accepted → Implemented
**Last Updated**: 2025-12-04
**Version**: 1.0
