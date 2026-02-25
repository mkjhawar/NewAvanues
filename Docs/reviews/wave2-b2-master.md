# VoiceOSCore — Wave 2 / Batch 2 Master Review Entry
# Scope: Scraping Pipeline, Overlay System, Element Fingerprinting, Screen Context
# Date: 260222 | Reviewer: code-reviewer agent | Score: 74/100 | Health: YELLOW

Full report: `docs/reviews/VoiceOSCore-Scraping-Review-QualityAnalysis-260222-V1.md`

---

## Files Reviewed

| File | Lines | Notes |
|------|-------|-------|
| `androidMain/.../AndroidScreenExtractor.kt` | 255 | DFS traversal, depth limit=30, child recycling |
| `androidMain/.../AccessibilityNodeAdapter.kt` | 141 | ANI→ElementInfo conversion, AVID generation |
| `androidMain/.../VoiceOSAccessibilityService.kt` | 540 | Thin wrapper, debounce, scroll tracking |
| `commonMain/.../element/ElementFingerprint.kt` | 110 | Delegates to AVID module Fingerprint/TypeCode |
| `commonMain/.../overlay/OverlayItemGenerator.kt` | 109 | List-app + all-clickable paths |
| `commonMain/.../overlay/OverlayStateManager.kt` | 178 | MutableStateFlow singleton, badge/mode/feedback |
| `commonMain/.../overlay/OverlayNumberingExecutor.kt` | 193 | Per-container numbering, screen context |
| `commonMain/.../overlay/NumberOverlayRenderer.kt` | 447 | Platform-agnostic badge rendering params |
| `commonMain/.../overlay/OverlayManager.kt` | 296 | Facade over Registry+Visibility+Disposal |
| `commonMain/.../overlay/OverlayRegistry.kt` | 125 | Registration and lookup |
| `commonMain/.../overlay/OverlayVisibilityManager.kt` | 104 | Show/hide delegation |
| `commonMain/.../screen/ScreenFingerprinter.kt` | 327 | SHA-256 content + structural fingerprinting |
| `commonMain/.../command/CommandGenerator.kt` | 617 | Label derivation, ordinal/numeric/label commands |
| `commonMain/.../element/ElementInfo.kt` | 265 | Core data class, stability score, deprecated helpers |
| `commonMain/.../element/ElementLabels.kt` | 251 | Label pipeline, findTopLevelListItems |
| `commonMain/.../PersistenceDecisionEngine.kt` | 636 | 4-layer hybrid persistence decisions |

---

## Critical Findings (P0)

### 1. Root node memory leak — every screen change
**File**: `VoiceOSAccessibilityService.kt:336`
**Impact**: Accumulating ANI leak on API < 34. On busy screens (rapid navigation, auto-refreshing
content) this can exhaust the accessibility node pool, causing the service to receive null roots.

```kotlin
// CURRENT (line 336):
val root = rootInActiveWindow ?: return@launch
val elements = screenExtractor.extract(root)
// root is NEVER recycled

// FIX:
val root = rootInActiveWindow ?: return@launch
try {
    val elements = screenExtractor.extract(root)
    // ... rest of processing
} finally {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        root.recycle()
    }
}
```
Same fix needed in `refreshScreen()` at line 483.

### 2. Debug println in hot command-generation path
**File**: `CommandGenerator.kt:407,411`
**Impact**: Synchronous I/O on Dispatchers.Default per list item per screen change. 50-item list =
100 synchronous `println` calls per screen update.

```kotlin
// REMOVE both occurrences:
println("[generateListLabelCommands] label = $label")   // line 407
println("[generateListLabelCommands] label = $label")   // line 411 (duplicate)
```

---

## High Findings (P1)

### 3. AccessibilityEvent not recycled in refreshScreen()
**File**: `VoiceOSAccessibilityService.kt:484-488`
`AccessibilityEvent.obtain()` creates an event from the pool. It is never recycled.

### 4. OverlayNumberingExecutor race condition
**File**: `OverlayNumberingExecutor.kt:23-24`
`assignments` and `nextNumbers` are plain `mutableMapOf()` accessed from:
- `handleScreenChange` coroutine (Dispatchers.Default) — calls `handleScreenContext` → `clearAllAssignmentsInternal`
- `onScrollSettled` callback path — calls `assignNumbers`
- NumbersOverlayExecutor suspend methods — called from various coroutine contexts

Fix: add `private val mutex = Mutex()` and wrap all map mutations with `mutex.withLock { }`.

### 5. Weak elementHash used as DB foreign key
**File**: `CommandGenerator.kt:455`, `CommandGeneratorHelpers.kt:109`, `ElementInfo.kt:96`, `BoundsResolver.kt:360`
Four independent 32-bit `hashCode()` truncations used as stable element identifiers, computed from
different inputs. Should unify on the existing SHA-256 infrastructure or on the AVID fingerprint.

### 6. processVoiceCommand always emits EXECUTE action type in callback
**File**: `VoiceOSAccessibilityService.kt:509-514`
`onCommandExecuted` always receives `CommandActionType.EXECUTE` regardless of matched command type.
Breaks any telemetry or feedback logic that branches on action type.

---

## Medium Findings (P2)

### 7. Dynamic-screen fingerprinting bypass
`isDynamicContentScreen()` exists but is never called in the screen-change pipeline. The service
always uses content-hash, causing unnecessary re-processing (or misses) for live-updating screens.

### 8. findTopLevelListItems heuristic too narrow
`ElementLabels.kt:178-182`: Hardcoded Gmail patterns. Non-Gmail list apps (Telegram, Signal, etc.)
produce empty results → no overlay badges → silent failure.
Fix: fall back to `generateForAllClickable()` when `generateForListApp()` returns empty.

### 9. trimIfNeeded() is dead code
`OverlayNumberingExecutor.kt:133-143`: Never called. Assignments maps grow unbounded in long sessions.

### 10. Duplicate label derivation
`CommandGenerator.deriveLabel()` and `ElementLabels.deriveElementLabels()` both implement
`text > contentDescription > resourceId` priority. The latter is declared "SINGLE SOURCE OF TRUTH"
but the former ignores it.

### 11. Rule 7 violation — 131 files
`Author: VOS4 Development Team` present in 131 files across VoiceOSCore. Additionally,
`Code-Reviewed-By: CCA` in 4 overlay files is an indirect AI attribution (CCA = Claude Code Agent).
Both must be removed globally.

### 12. extractShortLabel dead branch
`CommandGenerator.kt:288`: The `else if (text.startsWith(", , ,"))` branch is unreachable for
real Gmail contentDescriptions. Dead code should be removed.

---

## What is Working Well

- **Architecture**: Clean separation of platform code (androidMain) from KMP shared logic (commonMain).
  `ElementInfo`, `ElementFingerprint`, `OverlayItemGenerator`, `OverlayStateManager`, and
  `CommandGenerator` are all pure Kotlin with no Android imports — iOS reuse is structurally valid.

- **Node recycling**: `AndroidScreenExtractor` correctly recycles child nodes in a `finally` block
  within `traverseChildren`. The API-level guard (`< UPSIDE_DOWN_CAKE`) is correct.

- **Debouncing design**: The dual-path debounce (immediate for WINDOW_STATE_CHANGED, debounced for
  WINDOW_CONTENT_CHANGED) with separate scroll-settle callback is architecturally correct. The NAV-500
  fixes are well documented inline.

- **Per-container numbering**: `OverlayNumberingExecutor` correctly scopes numbering to individual
  scroll containers, enabling multi-panel screens to have independent badge sequences. The structural-
  change-ratio threshold (0.4) for distinguishing scroll vs navigation is sensible.

- **PersistenceDecisionEngine**: The 4-layer (App → Container → Screen → Content) decision engine
  is well-designed. `batchDecide()` correctly shares App and Screen classifications across all
  elements to avoid repeated computation. The confidence levels and rule numbering are consistent.

- **Label pipeline**: `ElementLabels.cleanCommaLabel()` correctly handles Gmail-style comma-separated
  contentDescriptions. The single-source-of-truth principle is documented even if not fully enforced.

- **AVID fingerprint**: SHA-256 with `packageName` inclusion is the right choice for cross-app
  uniqueness and VOS export portability. The `TypeCode:hash8` format is human-readable and parseable.

---

## Action Backlog (Prioritized)

| Priority | Action | File |
|----------|--------|------|
| P0 | Add `try/finally root.recycle()` in `handleScreenChange` | VoiceOSAccessibilityService.kt:336 |
| P0 | Add `try/finally root.recycle()` in `refreshScreen` | VoiceOSAccessibilityService.kt:483 |
| P0 | Remove both `println` in `generateListLabelCommands` | CommandGenerator.kt:407,411 |
| P1 | Add `Mutex` to `OverlayNumberingExecutor` | OverlayNumberingExecutor.kt:23 |
| P1 | Recycle event in `refreshScreen` synthetic event | VoiceOSAccessibilityService.kt:484 |
| P1 | Unify elementHash to use SHA-256 or existing AVID | CommandGenerator.kt:447-456 |
| P1 | Pass actual matched command to `onCommandExecuted` | VoiceOSAccessibilityService.kt:509 |
| P2 | Call `isDynamicContentScreen` to select fingerprint variant | VoiceOSAccessibilityService.kt:348 |
| P2 | Fallback to `generateForAllClickable` when list detection returns empty | OverlayItemGenerator.kt |
| P2 | Call `trimIfNeeded()` after `assignNumbers()` | OverlayNumberingExecutor.kt:95 |
| P2 | Replace all `println` with `LoggingUtils` in non-test files | VoiceOSCore.kt, StaticCommandPersistenceImpl.kt |
| P2 | Remove `Code-Reviewed-By: CCA` from 4 overlay files | OverlayManager.kt + 3 others |
| P2 | Batch-remove `Author: VOS4 Development Team` from 131 files | Module-wide |
| P2 | Remove dead `extractShortLabel` dead branch | CommandGenerator.kt:288 |
| P2 | Unify `deriveLabel` into `ElementLabels.deriveElementLabels` | CommandGenerator.kt:466 |
