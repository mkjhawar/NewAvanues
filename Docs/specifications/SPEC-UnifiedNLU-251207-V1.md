# Unified NLU System Specification

**Spec ID:** SPEC-UnifiedNLU-251207-V1
**Date:** 2025-12-07
**Status:** Draft
**Version:** 1.0
**Platform:** Android (KMP-ready)

---

## Executive Summary

Consolidate VoiceOS CommandManager and AVA IntentClassifier into a unified NLU system using AVU (Avanues Universal Format) as the single file format. This addresses critical issues from the VoiceOS Review Report while enabling semantic understanding across both apps.

---

## Problem Statement

### Current State

| Issue | Impact | Severity |
|-------|--------|----------|
| **Duplicate CommandContext** | Type mismatch between `sealed class` (CommandManager) and `data class` (command-models) | HIGH |
| **ActionFactory placeholders** | `DynamicUIAction`, `DynamicAppAction`, `DynamicOverlayAction` log-only stubs | CRITICAL |
| **Separate NLU systems** | VoiceOS (pattern) and AVA (semantic) don't share intents | MEDIUM |
| **No runtime testing** | Placeholder actions will fail silently | HIGH |

### Desired State

- Single `CommandContext` definition in shared module
- All ActionFactory handlers fully implemented
- Unified NLU with AVU format for all intents
- Hybrid classification (pattern + semantic)

---

## Functional Requirements

### FR-1: AVU NLU Format (Shared)

| ID | Requirement |
|----|-------------|
| FR-1.1 | Define NLU-specific IPC codes (INT, PAT, SYN, EMB, ACT) in AVU spec |
| FR-1.2 | Create `.aai` files for unified intent definitions |
| FR-1.3 | Include pre-computed embeddings as base64-encoded EMB entries |
| FR-1.4 | Support locale fallback chain (user locale → en-US) |

### FR-2: Shared NLU Module

| ID | Requirement |
|----|-------------|
| FR-2.1 | Create `Modules/Shared/NLU` KMP module |
| FR-2.2 | Implement `AvuIntentParser` for `.aai` files |
| FR-2.3 | Define `UnifiedIntent` data class with patterns, synonyms, embeddings |
| FR-2.4 | Implement `IntentOntologyRepository` with SQLDelight |
| FR-2.5 | Export/import `.aai` files from database |

### FR-3: Hybrid Intent Classifier

| ID | Requirement |
|----|-------------|
| FR-3.1 | Exact pattern match (fastest path) |
| FR-3.2 | Fuzzy pattern match with Levenshtein distance ≤ 3 |
| FR-3.3 | Semantic similarity via pre-computed embeddings |
| FR-3.4 | Combined ranking with configurable weights |
| FR-3.5 | Classification < 50ms target |

### FR-4: CommandContext Unification

| ID | Requirement |
|----|-------------|
| FR-4.1 | Deprecate `com.augmentalis.commandmanager.context.CommandContext` (sealed class) |
| FR-4.2 | Keep `com.augmentalis.voiceos.command.CommandContext` (data class) as primary |
| FR-4.3 | Add missing fields from sealed class to data class |
| FR-4.4 | Update all imports to use unified definition |

### FR-5: ActionFactory Implementation

| ID | Requirement |
|----|-------------|
| FR-5.1 | Implement `DynamicUIAction` with OverlayManager integration |
| FR-5.2 | Implement `DynamicAppAction` with PackageManager (already done - verify) |
| FR-5.3 | Implement `DynamicOverlayAction` with VoiceOS overlay system |
| FR-5.4 | Implement `DynamicBrowserAction` with WebAvanue integration |
| FR-5.5 | Implement `DynamicPositionAction` for cursor/alignment |
| FR-5.6 | Remove all placeholder log statements |

---

## Non-Functional Requirements

### NFR-1: Performance

| Metric | Target |
|--------|--------|
| Pattern match | < 5ms |
| Fuzzy match | < 20ms |
| Semantic match | < 50ms |
| Hybrid classification | < 50ms total |
| Memory (embeddings) | < 50MB for 500 intents |

### NFR-2: Compatibility

| Requirement | Details |
|-------------|---------|
| Android API | 28+ (Pie) |
| KMP targets | Android, iOS (future) |
| AVU schema | avu-1.0 |
| ONNX Runtime | 1.16.3 |

### NFR-3: Quality

| Metric | Target |
|--------|--------|
| Test coverage | 90%+ critical paths |
| Classification accuracy | 95%+ for trained intents |
| False positive rate | < 5% |

---

## Technical Design

### AVU NLU IPC Codes

```
# New codes for NLU (add to AVU-UNIVERSAL-FORMAT-SPEC.md)

| Code | Meaning | Format | Purpose |
|------|---------|--------|---------|
| INT  | Intent Definition | id:canonical:category:priority:action | Intent metadata |
| PAT  | Pattern | intent_id:pattern_text | Pattern for matching |
| SYN  | Synonym | intent_id:synonym_text | Alternative phrases |
| EMB  | Embedding | intent_id:model:dimension:base64_vector | Pre-computed BERT |
| ACT  | Action | intent_id:action_type:params | Execution action |
```

### Module Structure

```
Modules/
├── Shared/
│   └── NLU/                              # NEW
│       ├── build.gradle.kts
│       └── src/
│           ├── commonMain/kotlin/
│           │   └── com/augmentalis/shared/nlu/
│           │       ├── model/
│           │       │   ├── UnifiedIntent.kt
│           │       │   └── IntentMatch.kt
│           │       ├── parser/
│           │       │   └── AvuIntentParser.kt
│           │       ├── classifier/
│           │       │   └── HybridIntentClassifier.kt
│           │       ├── repository/
│           │       │   └── IntentOntologyRepository.kt
│           │       └── matcher/
│           │           ├── PatternMatcher.kt
│           │           ├── FuzzyMatcher.kt
│           │           └── SemanticMatcher.kt
│           └── commonMain/sqldelight/
│               └── com/augmentalis/shared/nlu/
│                   └── UnifiedIntent.sq
```

### UnifiedIntent Data Class

```kotlin
// UnifiedIntent.kt
data class UnifiedIntent(
    val id: String,                    // e.g., "nav_back"
    val canonicalPhrase: String,       // e.g., "go back"
    val patterns: List<String>,        // From PAT: entries
    val synonyms: List<String>,        // From SYN: entries
    val embedding: FloatArray?,        // From EMB: base64 decoded (384-dim)
    val category: String,              // nav, media, system, etc.
    val actionId: String,              // Action to execute
    val priority: Int,                 // Higher = more important
    val locale: String,                // e.g., "en-US"
    val source: String                 // core, voiceos, user
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnifiedIntent) return false
        return id == other.id && locale == other.locale
    }

    override fun hashCode(): Int = id.hashCode() * 31 + locale.hashCode()
}
```

### Hybrid Classifier

```kotlin
// HybridIntentClassifier.kt
class HybridIntentClassifier(
    private val repository: IntentOntologyRepository,
    private val patternMatcher: PatternMatcher,
    private val fuzzyMatcher: FuzzyMatcher,
    private val semanticMatcher: SemanticMatcher?
) {
    data class ClassificationResult(
        val intent: UnifiedIntent?,
        val confidence: Float,
        val method: MatchMethod,
        val allMatches: List<IntentMatch>
    )

    enum class MatchMethod { EXACT, FUZZY, SEMANTIC, HYBRID, NOT_FOUND }

    suspend fun classify(utterance: String, locale: String): ClassificationResult {
        val intents = repository.getAllIntents(locale)

        // 1. Exact match (fastest)
        patternMatcher.findExact(utterance, intents)?.let {
            return ClassificationResult(it, 1.0f, MatchMethod.EXACT, listOf(IntentMatch(it, 1.0f)))
        }

        // 2. Fuzzy match
        val fuzzyMatches = fuzzyMatcher.findMatches(utterance, intents, threshold = 0.7f)

        // 3. Semantic match (if available)
        val semanticMatches = semanticMatcher?.findMatches(utterance, intents, threshold = 0.6f)
            ?: emptyList()

        // 4. Combined ranking
        val combined = combineMatches(fuzzyMatches, semanticMatches)

        return if (combined.isNotEmpty()) {
            val best = combined.first()
            val method = when {
                fuzzyMatches.any { it.intent.id == best.intent.id } &&
                semanticMatches.any { it.intent.id == best.intent.id } -> MatchMethod.HYBRID
                fuzzyMatches.any { it.intent.id == best.intent.id } -> MatchMethod.FUZZY
                else -> MatchMethod.SEMANTIC
            }
            ClassificationResult(best.intent, best.score, method, combined)
        } else {
            ClassificationResult(null, 0f, MatchMethod.NOT_FOUND, emptyList())
        }
    }

    private fun combineMatches(
        fuzzy: List<IntentMatch>,
        semantic: List<IntentMatch>
    ): List<IntentMatch> {
        val scoreMap = mutableMapOf<String, Float>()

        fuzzy.forEach { scoreMap[it.intent.id] = maxOf(scoreMap[it.intent.id] ?: 0f, it.score) }
        semantic.forEach { scoreMap[it.intent.id] = maxOf(scoreMap[it.intent.id] ?: 0f, it.score) }

        val allIntents = (fuzzy.map { it.intent } + semantic.map { it.intent }).distinctBy { it.id }

        return allIntents
            .map { IntentMatch(it, scoreMap[it.id] ?: 0f) }
            .sortedByDescending { it.score }
    }
}
```

### CommandContext Unification

```kotlin
// KEEP: com.augmentalis.voiceos.command.CommandContext (enhanced)
data class CommandContext(
    // Existing fields
    val packageName: String? = null,
    val activityName: String? = null,
    val viewId: String? = null,
    val screenContent: String? = null,
    val userLocation: String? = null,
    val deviceState: Map<String, Any> = emptyMap(),
    val focusedElement: String? = null,
    val customData: Map<String, Any> = emptyMap(),

    // NEW: Fields from sealed class
    val screenElements: List<String> = emptyList(),
    val hasEditableFields: Boolean = false,
    val hasScrollableContent: Boolean = false,
    val hasClickableElements: Boolean = false,
    val timeOfDay: String? = null,
    val dayOfWeek: Int? = null,
    val locationType: String? = null,
    val activityType: String? = null
)

// DEPRECATE: com.augmentalis.commandmanager.context.CommandContext
// Add @Deprecated annotation and migration guide
```

### ActionFactory Fixes

```kotlin
// DynamicUIAction - IMPLEMENT
class DynamicUIAction(
    private val action: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        return when {
            action.contains("hide") && action.contains("keyboard") -> {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(null, 0)
                createSuccessResult(command, "Keyboard hidden")
            }
            action.contains("hide") && action.contains("overlay") -> {
                // Send broadcast to OverlayManager
                val intent = Intent("com.augmentalis.voiceos.HIDE_OVERLAY")
                context.sendBroadcast(intent)
                createSuccessResult(command, "Overlay hidden")
            }
            action.contains("show") && action.contains("overlay") -> {
                val intent = Intent("com.augmentalis.voiceos.SHOW_OVERLAY")
                context.sendBroadcast(intent)
                createSuccessResult(command, "Overlay shown")
            }
            action.contains("close") -> {
                accessibilityService?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                createSuccessResult(command, "Closed")
            }
            else -> createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Unknown UI action: $action")
        }
    }
}

// DynamicOverlayAction - IMPLEMENT
class DynamicOverlayAction(
    private val action: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        val intent = Intent().apply {
            setPackage(context.packageName)
            when {
                action.contains("hide_help") -> {
                    setAction("com.augmentalis.voiceos.TOGGLE_HELP")
                    putExtra("show", false)
                }
                action.contains("show_help") -> {
                    setAction("com.augmentalis.voiceos.TOGGLE_HELP")
                    putExtra("show", true)
                }
                action.contains("hide_command") -> {
                    setAction("com.augmentalis.voiceos.TOGGLE_COMMANDS")
                    putExtra("show", false)
                }
                action.contains("show_command") -> {
                    setAction("com.augmentalis.voiceos.TOGGLE_COMMANDS")
                    putExtra("show", true)
                }
                else -> {
                    setAction("com.augmentalis.voiceos.OVERLAY_ACTION")
                    putExtra("action", action)
                }
            }
        }
        context.sendBroadcast(intent)
        return createSuccessResult(command, successMessage)
    }
}

// DynamicPositionAction - IMPLEMENT
class DynamicPositionAction(
    private val action: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        val metrics = context.resources.displayMetrics
        val centerX = metrics.widthPixels / 2
        val centerY = metrics.heightPixels / 2

        return when {
            action.contains("center_cursor") -> {
                // Move cursor overlay to center
                val intent = Intent("com.augmentalis.voiceos.CURSOR_POSITION")
                intent.putExtra("x", centerX)
                intent.putExtra("y", centerY)
                context.sendBroadcast(intent)
                createSuccessResult(command, "Cursor centered")
            }
            action.contains("center") -> {
                // Center view in scrollable content
                val rootNode = accessibilityService?.rootInActiveWindow
                val focusedNode = rootNode?.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
                focusedNode?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_TO_POSITION)
                focusedNode?.recycle()
                rootNode?.recycle()
                createSuccessResult(command, "Centered")
            }
            else -> createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Unknown position action")
        }
    }
}
```

---

## Database Schema

```sql
-- UnifiedIntent.sq

CREATE TABLE unified_intent (
    id TEXT NOT NULL,
    canonical_phrase TEXT NOT NULL,
    patterns TEXT NOT NULL,           -- JSON array
    synonyms TEXT NOT NULL,           -- JSON array
    category TEXT NOT NULL,
    action_id TEXT NOT NULL,
    priority INTEGER NOT NULL DEFAULT 1,
    locale TEXT NOT NULL DEFAULT 'en-US',
    source TEXT NOT NULL,             -- 'core', 'voiceos', 'ava', 'user'

    -- Embedding (nullable - computed async)
    embedding_vector BLOB,
    embedding_dimension INTEGER,
    embedding_model_version TEXT,

    -- Timestamps
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,

    PRIMARY KEY (id, locale)
);

CREATE INDEX idx_unified_intent_locale ON unified_intent(locale);
CREATE INDEX idx_unified_intent_category ON unified_intent(category);
CREATE INDEX idx_unified_intent_source ON unified_intent(source);

-- Queries
selectAllByLocale:
SELECT * FROM unified_intent WHERE locale = ? ORDER BY priority DESC;

selectByIdAndLocale:
SELECT * FROM unified_intent WHERE id = ? AND locale = ?;

selectByCategory:
SELECT * FROM unified_intent WHERE category = ? AND locale = ? ORDER BY priority DESC;

insertOrReplace:
INSERT OR REPLACE INTO unified_intent(
    id, canonical_phrase, patterns, synonyms, category, action_id,
    priority, locale, source, embedding_vector, embedding_dimension,
    embedding_model_version, created_at, updated_at
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

updateEmbedding:
UPDATE unified_intent SET
    embedding_vector = ?,
    embedding_dimension = ?,
    embedding_model_version = ?,
    updated_at = ?
WHERE id = ? AND locale = ?;

deleteBySource:
DELETE FROM unified_intent WHERE source = ?;
```

---

## AVU NLU File Example

```
# Avanues Universal Format v1.0
# Type: AVA
# Extension: .aai
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: shared
metadata:
  file: core-navigation.aai
  category: nlu_intents
  count: 24
---
# Navigation intents
INT:nav_back:go back:navigation:10:GLOBAL_ACTION_BACK
PAT:nav_back:go back
PAT:nav_back:navigate back
PAT:nav_back:previous screen
PAT:nav_back:back
SYN:nav_back:return
SYN:nav_back:previous
EMB:nav_back:mobilebert-384:384:QWFBYUFhQW...

INT:nav_home:go home:navigation:10:GLOBAL_ACTION_HOME
PAT:nav_home:go home
PAT:nav_home:home screen
PAT:nav_home:go to home
SYN:nav_home:home
SYN:nav_home:main screen
EMB:nav_home:mobilebert-384:384:QmJCYkJiQm...

INT:nav_recents:show recent apps:navigation:8:GLOBAL_ACTION_RECENTS
PAT:nav_recents:show recent apps
PAT:nav_recents:recent apps
PAT:nav_recents:open recents
SYN:nav_recents:recents
SYN:nav_recents:app switcher
EMB:nav_recents:mobilebert-384:384:Q2NDY0Nj...

# Volume intents
INT:vol_up:volume up:media:8:VolumeAction.VOLUME_UP
PAT:vol_up:volume up
PAT:vol_up:increase volume
PAT:vol_up:louder
SYN:vol_up:turn up
SYN:vol_up:raise volume
EMB:vol_up:mobilebert-384:384:RGREZEdE...

INT:vol_down:volume down:media:8:VolumeAction.VOLUME_DOWN
PAT:vol_down:volume down
PAT:vol_down:decrease volume
PAT:vol_down:quieter
SYN:vol_down:turn down
SYN:vol_down:lower volume
EMB:vol_down:mobilebert-384:384:RWVFZUVl...
---
synonyms:
  back: [return, previous, go back]
  home: [main, start, launcher]
  volume: [sound, audio, loudness]
```

---

## Migration Path

### Phase 1: AVU NLU Codes (0.5 day)

1. Add INT/PAT/SYN/EMB/ACT codes to `AVU-UNIVERSAL-FORMAT-SPEC.md`
2. Create validation rules
3. Update existing parsers to recognize new codes

### Phase 2: Shared NLU Module (1 day)

1. Create `Modules/Shared/NLU` with build.gradle.kts
2. Implement `AvuIntentParser`
3. Define `UnifiedIntent` data class
4. Create SQLDelight schema
5. Implement `IntentOntologyRepository`

### Phase 3: CommandContext Unification (0.5 day)

1. Add new fields to `com.augmentalis.voiceos.command.CommandContext`
2. Deprecate `com.augmentalis.commandmanager.context.CommandContext`
3. Update all imports (find/replace)
4. Update VoiceOSService to use unified definition

### Phase 4: ActionFactory Fixes (1 day)

1. Implement `DynamicUIAction` with overlay/keyboard logic
2. Implement `DynamicOverlayAction` with broadcast intents
3. Implement `DynamicPositionAction` for cursor control
4. Verify `DynamicAppAction` works (appears complete)
5. Remove placeholder log statements
6. Add unit tests for each action type

### Phase 5: VoiceOS → AVU Migration (1 day)

1. Create `VosToAvuConverter`
2. Convert existing `.vos` commands to `.aai` format
3. Update `CommandLoader` to read from `.aai` files
4. Keep `CommandResolver` for pattern matching

### Phase 6: AVA → AVU Migration (1 day)

1. Create `AvaToAvuConverter`
2. Include embeddings as base64 EMB entries
3. Update `IntentSourceCoordinator` to load from `.aai`
4. Keep AVA's embedding computation for new intents

### Phase 7: Hybrid Classifier (1.5 days)

1. Create `HybridIntentClassifier` in shared module
2. Implement pattern/fuzzy/semantic matchers
3. Integrate with VoiceOS CommandManager
4. Integrate with AVA ClassifyIntentUseCase

### Phase 8: Testing (1 day)

1. Unit tests for all matchers
2. Integration tests for classifier
3. ActionFactory tests (all action types)
4. Benchmark: target < 50ms classification

---

## Success Criteria

- [ ] Single `CommandContext` definition used everywhere
- [ ] All ActionFactory handlers return functional actions (no placeholders)
- [ ] AVU `.aai` files parsed by both VoiceOS and AVA
- [ ] Hybrid classifier achieves 95%+ accuracy on test set
- [ ] Classification < 50ms on target devices
- [ ] Test coverage 90%+ for critical paths
- [ ] VoiceOS commands "Open Settings", "Launch Spotify" work

---

## Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| SQLDelight | 2.0.1 | Shared database |
| ONNX Runtime | 1.16.3 | Semantic embeddings |
| kotlinx-serialization | 1.6.2 | JSON parsing |

---

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Breaking existing commands | Comprehensive migration tests |
| Performance regression | Benchmark before/after |
| Embedding size | Base64 adds ~33% overhead |
| Circular dependencies | Shared module has no app deps |

---

## References

- [NLU-UNIFICATION-PROPOSAL.md](../Migration/NLU-UNIFICATION-PROPOSAL.md)
- [AVU-UNIVERSAL-FORMAT-SPEC.md](../../Docs/VoiceOS/Technical/specifications/AVU-UNIVERSAL-FORMAT-SPEC.md)
- [APP-CONSOLIDATION-ANALYSIS.md](../Migration/APP-CONSOLIDATION-ANALYSIS.md)
- VoiceOS Review Report (2025-12-07)

---

## Appendix: VoiceOS Review Issues Addressed

| Issue | Resolution |
|-------|------------|
| Incomplete ActionFactory placeholders | FR-5: Full implementation for all action types |
| Duplicate CommandContext | FR-4: Unify to single data class |
| Missing runtime testing | Phase 8: Comprehensive test suite |
| Legacy CommandDatabase references | Clean up during Phase 3 |

---

Updated: 2025-12-07 | IDEACODE v10.3.1
