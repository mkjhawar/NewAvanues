# VoiceOS Plan: Command Generation Architecture Fix

**Plan ID:** VoiceOS-Plan-CommandGenArchitecture-60105-V1
**Related Issue:** VoiceOS-Issue-CommandGenArchitecture-60105-V1
**Created:** 2026-01-05
**Priority:** High
**Estimated Effort:** Medium (2-3 phases)

---

## Objective

Move voice command generation from Android test app to KMP `commonMain` with minimal overhead. Commands should be available for voice execution without unnecessary storage or processing.

---

## Design Principles

1. **Minimal Overhead:** No redundant storage, lazy generation
2. **Single Pass:** Generate commands during element extraction, not separately
3. **In-Memory First:** Keep active commands in memory, persist only when needed
4. **KMP Native:** All logic in `commonMain`, platform-specific only for persistence

---

## Architecture

### Current (Broken)
```
AccessibilityEvent → VoiceOSAccessibilityService.scanScreen()
                           ↓
                    generateCommands() → GeneratedCommand (local)
                           ↓
                    Display in UI → DISCARDED
```

### Target (Efficacious)
```
AccessibilityEvent → ElementExtractor (KMP)
                           ↓
                    QuantizedElement + QuantizedCommand (generated together)
                           ↓
                    CommandRegistry (in-memory, KMP)
                           ↓
              ┌────────────┴────────────┐
              ↓                         ↓
        Voice Matcher            UI Display (Android)
              ↓
        Action Executor
```

---

## Implementation Phases

### Phase 1: KMP CommandGenerator (Core Logic)

**Goal:** Move command generation logic to `commonMain`

**Files to Create:**
```
Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/
├── command/
│   ├── CommandGenerator.kt      # Core generation logic
│   └── CommandRegistry.kt       # In-memory command storage
```

**CommandGenerator.kt:**
```kotlin
package com.augmentalis.voiceoscoreng.command

import com.augmentalis.voiceoscoreng.avu.QuantizedCommand
import com.augmentalis.voiceoscoreng.avu.QuantizedElement
import com.augmentalis.voiceoscoreng.avu.CommandActionType
import com.augmentalis.voiceoscoreng.common.VUIDGenerator

object CommandGenerator {

    /**
     * Generate command from element during extraction (single pass).
     * Returns null if element is not actionable or has no label.
     */
    fun fromElement(
        element: QuantizedElement,
        label: String?,
        packageName: String
    ): QuantizedCommand? {
        // Skip non-actionable or unlabeled elements
        if (!element.isActionable() || label.isNullOrBlank()) return null
        if (label == element.className.substringAfterLast(".")) return null

        val actionType = deriveActionType(element)
        val vuid = element.vuid.ifEmpty {
            VUIDGenerator.generate(packageName, element.typeCode, element.hash)
        }

        return QuantizedCommand(
            uuid = "", // Generated on persist if needed
            phrase = "${actionType.verb} $label",
            actionType = actionType,
            targetVuid = vuid,
            confidence = calculateConfidence(element, label)
        )
    }

    private fun deriveActionType(element: QuantizedElement): CommandActionType {
        return when {
            element.isClickable && element.className.contains("Button") -> CommandActionType.TAP
            element.isClickable && element.className.contains("EditText") -> CommandActionType.FOCUS
            element.isClickable -> CommandActionType.TAP
            element.isScrollable -> CommandActionType.SCROLL
            element.isLongClickable -> CommandActionType.LONG_PRESS
            else -> CommandActionType.TAP
        }
    }

    private fun calculateConfidence(element: QuantizedElement, label: String): Float {
        var confidence = 0.5f
        if (element.resourceId.isNotEmpty()) confidence += 0.2f
        if (element.contentDescription.isNotEmpty()) confidence += 0.15f
        if (label.length in 2..20) confidence += 0.1f
        if (element.isClickable) confidence += 0.05f
        return confidence.coerceIn(0f, 1f)
    }
}
```

**CommandRegistry.kt:**
```kotlin
package com.augmentalis.voiceoscoreng.command

import com.augmentalis.voiceoscoreng.avu.QuantizedCommand

/**
 * In-memory command registry for active screen.
 * Replaced on each scan - no accumulation.
 */
class CommandRegistry {
    private val commands = mutableMapOf<String, QuantizedCommand>() // keyed by VUID

    fun update(newCommands: List<QuantizedCommand>) {
        commands.clear()
        newCommands.forEach { cmd ->
            cmd.targetVuid?.let { vuid ->
                commands[vuid] = cmd
            }
        }
    }

    fun findByPhrase(phrase: String): QuantizedCommand? {
        val normalized = phrase.lowercase().trim()
        return commands.values.firstOrNull { cmd ->
            cmd.phrase.lowercase() == normalized ||
            normalized.endsWith(cmd.phrase.substringAfter(" ").lowercase())
        }
    }

    fun findByVuid(vuid: String): QuantizedCommand? = commands[vuid]

    fun all(): List<QuantizedCommand> = commands.values.toList()

    fun clear() = commands.clear()

    val size: Int get() = commands.size
}
```

**Tasks:**
- [ ] Create `command/` package in commonMain
- [ ] Implement `CommandGenerator.kt`
- [ ] Implement `CommandRegistry.kt`
- [ ] Add unit tests

---

### Phase 2: Integration with Element Extraction

**Goal:** Wire command generation into element extraction pipeline

**Modify:** `ElementExtractor` or create integrated extraction

**Approach:** Generate commands during element processing, not as separate pass

```kotlin
// In element extraction flow
fun processNode(node: AccessibilityNodeInfo, ...): Pair<QuantizedElement, QuantizedCommand?> {
    val element = extractElement(node)
    val label = deriveLabel(node)
    val command = CommandGenerator.fromElement(element, label, packageName)
    return element to command
}
```

**Tasks:**
- [ ] Modify `JitProcessor.kt` to generate commands during extraction
- [ ] Update `VoiceOSAccessibilityService.kt` to use KMP `CommandGenerator`
- [ ] Replace local `GeneratedCommand` with `QuantizedCommand`
- [ ] Wire `CommandRegistry` to service

---

### Phase 3: Voice Execution Pipeline

**Goal:** Enable voice command matching and execution

**Files:**
```
Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/
├── command/
│   └── CommandMatcher.kt        # Fuzzy matching for voice input
```

**CommandMatcher.kt:**
```kotlin
package com.augmentalis.voiceoscoreng.command

import com.augmentalis.voiceoscoreng.avu.QuantizedCommand

object CommandMatcher {

    fun match(
        voiceInput: String,
        registry: CommandRegistry,
        threshold: Float = 0.7f
    ): MatchResult {
        val normalized = voiceInput.lowercase().trim()

        // Exact match
        registry.findByPhrase(normalized)?.let {
            return MatchResult.Exact(it)
        }

        // Fuzzy match
        val candidates = registry.all()
            .map { cmd -> cmd to similarity(normalized, cmd.phrase.lowercase()) }
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }

        return when {
            candidates.isEmpty() -> MatchResult.NoMatch
            candidates.size == 1 -> MatchResult.Fuzzy(candidates[0].first, candidates[0].second)
            else -> MatchResult.Ambiguous(candidates.map { it.first })
        }
    }

    private fun similarity(a: String, b: String): Float {
        // Simple word overlap - can enhance later
        val wordsA = a.split(" ").toSet()
        val wordsB = b.split(" ").toSet()
        val intersection = wordsA.intersect(wordsB).size
        val union = wordsA.union(wordsB).size
        return if (union == 0) 0f else intersection.toFloat() / union
    }

    sealed class MatchResult {
        data class Exact(val command: QuantizedCommand) : MatchResult()
        data class Fuzzy(val command: QuantizedCommand, val confidence: Float) : MatchResult()
        data class Ambiguous(val candidates: List<QuantizedCommand>) : MatchResult()
        object NoMatch : MatchResult()
    }
}
```

**Tasks:**
- [ ] Implement `CommandMatcher.kt`
- [ ] Wire to voice recognition callback
- [ ] Add action execution based on matched command
- [ ] Add unit tests for matching

---

## Migration Steps

1. **Create KMP command package** (non-breaking)
2. **Add CommandGenerator and Registry** (non-breaking)
3. **Update test app to use KMP types** (breaking change to test app only)
4. **Remove local GeneratedCommand** (cleanup)
5. **Add CommandMatcher** (new feature)
6. **Wire voice execution** (new feature)

---

## Files Affected

| File | Change |
|------|--------|
| `command/CommandGenerator.kt` | CREATE |
| `command/CommandRegistry.kt` | CREATE |
| `command/CommandMatcher.kt` | CREATE (Phase 3) |
| `VoiceOSAccessibilityService.kt` | MODIFY - use KMP types |
| `JitProcessor.kt` | MODIFY - integrate command gen |

---

## Success Criteria

1. [ ] Commands generated using KMP `CommandGenerator`
2. [ ] Commands stored in `CommandRegistry` (in-memory)
3. [ ] Test app displays commands from shared implementation
4. [ ] Voice input can match commands via `CommandMatcher`
5. [ ] No regression in scan performance
6. [ ] Memory usage stable (commands cleared per scan)

---

## Out of Scope (Deferred)

- AVU file export
- Persistent command database
- Synonym learning/expansion
- Cross-app command sharing

---

## Risks

| Risk | Mitigation |
|------|------------|
| Performance regression | Single-pass generation, lazy evaluation |
| Memory growth | Clear registry on each scan |
| Breaking test app | Phase 2 is isolated change |

---

## Notes

- User emphasized minimal overhead - this design avoids database writes
- AVU export remains future work - focus on execution pipeline
- `CommandRegistry` is intentionally simple - can enhance later if needed
