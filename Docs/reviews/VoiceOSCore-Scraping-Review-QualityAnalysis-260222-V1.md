# VoiceOSCore — Scraping & Overlay Quality Report — 260222

## Summary
SCORE: 74/100 | HEALTH: YELLOW
SCOPE: Scraping Pipeline, Overlay System, Element Fingerprinting, Screen Context Management

The scraping and overlay subsystem is architecturally sound and has been through several meaningful
bug-fix iterations (NAV-500 Fix #1/#2, unified AVID). The core pipeline is clean, well-documented,
and correctly separates platform code from KMP shared logic. However, several concrete defects
remain: a root-node memory leak on every screen change, two production `println` debug statements
in hot command-generation code, an 8-bit `String.hashCode()` truncation used as a DB foreign-key
(32-bit collision window), a thread-safety gap in `OverlayNumberingExecutor` (mutable maps accessed
from multiple coroutine contexts), and 131 files retaining the `Author: VOS4 Development Team`
Rule-7 attribution violation.

---

## Scraping Pipeline Data Flow

```
Android AccessibilityService event
  └─ onAccessibilityEvent()                     [VoiceOSAccessibilityService.kt]
       ├─ TYPE_WINDOW_STATE_CHANGED → immediate (cancel pending debounce, fire onInAppNavigation)
       ├─ TYPE_WINDOW_CONTENT_CHANGED → debounced (DeviceCapabilityManager.getContentDebounceMs())
       ├─ TYPE_VIEW_SCROLLED → handleScrollEvent() → BoundsResolver.updateScrollOffset()
       │                          └─ schedules onScrollSettled() (debounced 300ms)
       └─ TYPE_VIEW_CLICKED → debounced content refresh

  handleScreenChange() [Dispatchers.Default coroutine]
    └─ rootInActiveWindow → AndroidScreenExtractor.extract(root)
         └─ traverseNode() [depth-limited DFS, MAX_DEPTH=30]
              └─ AccessibilityNodeAdapter.toElementInfo()
                   ├─ ElementFingerprint.generate()   → "BTN:a3f2e1c9" AVID
                   └─ ElementInfo (with bounds, flags, container metadata)
         (child nodes recycled in finally block)

  ScreenFingerprinter.calculateFingerprint(elements)
    ├─ SHA-256 of sorted { className:resourceId:normalizedText:normalizedDesc }
    └─ If hash == lastScreenHash → skip (deduplication gate)

  CommandGenerator.fromElement() [per element]
    ├─ deriveLabel() with (Voice:...) hint extraction + RealWear ML script fallback
    ├─ deriveActionType()
    └─ QuantizedCommand { phrase=label, targetAvid=fingerprint, metadata }

  CommandGenerator.generateListIndexCommands()   → ordinal commands (first/second/...)
  CommandGenerator.generateNumericCommands()      → digit+word forms (1/"one")
  CommandGenerator.generateListLabelCommands()    → sender/title label commands

  ActionCoordinator.updateDynamicCommandsBySource("accessibility", commands)

  Overlay path (parallel, app-level):
    OverlayItemGenerator.generateForListApp() OR generateForAllClickable()
      └─ ElementLabels.deriveElementLabels()  → Map<Int, String> (single label source-of-truth)
           ├─ Priority: text > contentDescription (cleanCommaLabel) > resourceId (cleanResourceId)
           └─ child-walking fallback (depth+1 scan)
      └─ NumberOverlayItem { number, label, bounds, avid }

  OverlayNumberingExecutor.assignNumbers()
    └─ sorted by top→left, assigns 1-N
    └─ per-container LinkedHashMap for multi-scroll-area screens

  OverlayStateManager.updateNumberedOverlayItems()
    └─ MutableStateFlow → Compose UI collects → renders badges
```

---

## P0 Critical Issues

- **[VoiceOSAccessibilityService.kt:336]** `rootInActiveWindow` acquired inside
  `Dispatchers.Default` coroutine but NEVER recycled.
  `AccessibilityNodeInfo` returned by `rootInActiveWindow` must be recycled after use on API
  < 34 (`UPSIDE_DOWN_CAKE`). `AndroidScreenExtractor` only recycles **child** nodes (correctly,
  in `traverseNode`/`recycleNode`), but the **root** node obtained at line 336 is never passed to
  `recycleNode`. On every screen-change event (potentially dozens per minute in a busy app), one
  root `AccessibilityNodeInfo` leaks. Long sessions accumulate unbounded leaks in the accessibility
  pool. The `refreshScreen()` path at line 483 has the same problem.

- **[CommandGenerator.kt:407,411]** Two bare `println` debug statements in the hot
  `generateListLabelCommands()` path. These execute on every list-label command generation cycle
  (called from `handleScreenChange` on every screen update). On a screen with 50 list items this
  fires 50+ times per screen change. `println` is synchronous and unbuffered on Android; it adds
  measurable latency to the Dispatchers.Default coroutine that handles screen extraction. Both
  lines print `label` twice (the second print is inside the `if (!element.canPerformAction())`
  guard and is always reached on the same element — they appear to be left-over from a debugging
  session, not intentional dual-logging).

---

## P1 High Issues

- **[CommandGenerator.kt:455 / CommandGeneratorHelpers.kt:109 / ElementInfo.kt:96 /
  BoundsResolver.kt:360]** `String.hashCode().toUInt().toString(16).padStart(8, '0')` used as
  element hash for database FK references (`elementHash` metadata key). Kotlin's `String.hashCode`
  is a 32-bit signed integer. The 8-hex-character (`toUInt()`) representation has an effective
  space of 2^32 = ~4 billion values. For a moderate-size app with thousands of scraped elements
  across sessions this is adequate in isolation, but because four separate files independently
  compute this hash from DIFFERENT inputs (resourceId vs contentDesc vs text vs className:bounds),
  two elements with different identifiers can produce the same `elementHash`. This hash is stored
  as a metadata value and used for element lookup. Collisions cause the wrong element to be
  targeted by a voice command. The fix is to use the already-available SHA-256 infrastructure from
  `FingerprintUtils.calculateSHA256()` (which is used for screen fingerprinting) or to use the
  AVID fingerprint already attached to the `QuantizedCommand` (which includes packageName for
  cross-app uniqueness).

- **[OverlayNumberingExecutor.kt:23-24]** `assignments` (`mutableMapOf`) and `nextNumbers`
  (`mutableMapOf`) are plain unsynchronized `LinkedHashMap`/`HashMap`. The `assignNumbers()` and
  `handleScreenContext()` methods are non-`suspend` and called from the Dispatchers.Default
  screen-change coroutine; the `NumbersOverlayExecutor` `suspend` methods (`getOrAssignNumber`,
  `clearNumberAssignments`, `onScreenTransition`) are also called from coroutine contexts. All
  access the same maps. If a scroll event triggers `onScrollSettled` → overlay refresh (which calls
  `assignNumbers`) while a screen-change coroutine is calling `clearAllAssignmentsInternal`, there
  is a ConcurrentModificationException risk on `assignments.clear()` vs iterating. The class
  comment says it is "KMP commonMain" but it references the mutable maps from multiple coroutine
  contexts. `Mutex` (from `kotlinx.coroutines.sync`) should guard all map mutations.

- **[ScreenFingerprinter.kt:5]** `Author: VOS4 Development Team` — Rule 7 violation. This file
  is in `commonMain` and is used as the screen-deduplication gate. This particular violation is
  noted here because the file is part of the critical path; the broader violation count is
  documented under Code Smells.

- **[VoiceOSAccessibilityService.kt:484-488]** `refreshScreen()` calls
  `AccessibilityEvent.obtain()` and immediately passes the synthetic event to `handleScreenChange()`
  which launches a Dispatchers.Default coroutine. The `AccessibilityEvent` is never recycled.
  On API < 34 this leaks the event from the system pool. The event should be recycled after the
  coroutine is launched (or use `packageName` as a string and avoid creating a synthetic event
  entirely, since `handleScreenChange` only reads `event.packageName`).

---

## P2 Medium Issues

- **[ScreenFingerprinter.kt:177-195]** The primary `calculateFingerprint()` uses content-based
  hashing (including normalized text) while `VoiceOSAccessibilityService` uses it to gate screen
  refreshes via `lastScreenHash`. For highly dynamic screens (chat apps, live dashboards), content
  normalization handles timestamps/counts but NOT arbitrary dynamic text (e.g., live typing
  indicators, stock prices, live sports scores). This means screen changes can be missed when
  dynamic content changes in ways the regex normalizers do not anticipate. The `isDynamicContentScreen()`
  method exists but is never called from the service's `handleScreenChange()` path — the service
  always uses content-hash regardless. The fix is to call `isDynamicContentScreen` and switch to
  `calculateStructuralFingerprint` for dynamic screens, as the interface was clearly designed for.

- **[ElementLabels.kt:178-182]** `findTopLevelListItems()` hard-codes detection heuristics for
  Gmail-style rows (`startsWith("unread,")`, `startsWith("starred,")`, etc.) and a height range
  of 60–300dp. This will silently produce zero list items for apps whose list rows do not match
  these patterns (e.g., Telegram, which uses different contentDescription formats). When
  `topLevelItems` is empty, `generateForListApp()` returns `emptyList()` and the overlay shows
  nothing. The affected user sees no badges even though the screen has list content.
  Suggestion: fall back to `generateForAllClickable()` when `generateForListApp()` returns empty.

- **[AccessibilityNodeAdapter.kt:106]** `isDynamicContainer()` uses `className.contains(it,
  ignoreCase = true)`. A class name like `"LazyColumnScrollableContainer"` (Compose internal)
  matches `"LazyColumn"` correctly. However `"ScrollView"` will also match
  `"HorizontalScrollView"` (since `"HorizontalScrollView"` contains `"ScrollView"`). This is
  likely harmless since both are intended to be treated as dynamic containers, but the order in
  `DYNAMIC_CONTAINERS` matters if stricter handling were added — `HorizontalScrollView` should be
  listed before `ScrollView` in a contains-check to avoid ambiguous partial matches.

- **[CommandGenerator.kt:288]** `extractShortLabel()` has a second email pattern branch
  (`"else if (text.startsWith(", , ,"))`) that is checked after `text.startsWith("Unread,")`. The
  pattern `", , ,"` is unusual; real Gmail contentDescriptions with a starred-but-read email start
  with `"Read, , ,"` or similar. The branch will likely never trigger. More importantly, the
  comment says `"Email pattern: ', , , SenderName, , Subject...'"` but the loop variable is `i`
  starting at `0`, which is the same loop as the `Unread` branch. This is dead or unreachable
  code that could be removed to simplify the function.

- **[OverlayManager.kt:8]** `Author: VOS4 Development Team` combined with `Code-Reviewed-By: CCA`
  — both are invalid attributions per Rule 7. `CCA` likely refers to `Claude Code Agent`. The
  `Code-Reviewed-By` attribution pattern appears in `OverlayRegistry.kt`, `OverlayVisibilityManager.kt`,
  `OverlayDisposal.kt`, and `NumberOverlayRenderer.kt` as well.

- **[VoiceOSAccessibilityService.kt:509-514]** `processVoiceCommand()` always constructs a
  synthetic `QuantizedCommand(phrase=utterance, actionType=CommandActionType.EXECUTE, ...)` for the
  `onCommandExecuted` callback, regardless of what the actual matched command's actionType was.
  This was already flagged in Part 1 session memory. The app-level service cannot distinguish a
  CLICK command from a TYPE command in `onCommandExecuted`, breaking telemetry and any conditional
  feedback logic.

---

## Fingerprint Analysis

### Hash algorithm
`ElementFingerprint` delegates to `Fingerprint.forElement()` (in the AVID module), which uses
SHA-256 truncated to 8 hex characters. The input is:
`packageName + className + resourceId + text + contentDescription`
with `ifBlank { null }` for optional fields.

`TypeCode.fromTypeName(className)` derives the 3-letter type prefix (`BTN`, `INP`, `EDI`, etc.)
producing the canonical `BTN:a3f2e1c9` format.

### Collision risk
The AVID fingerprint (used for voice-command targeting) is based on SHA-256 truncated to 8 hex
characters = 32 bits of entropy. For a typical screen with 20–100 elements, the birthday-collision
probability is negligible (~0.001% at 100 elements). This is acceptable.

The secondary `elementHash` computed inline in `CommandGenerator.deriveElementHash()` and in
`CommandGeneratorHelpers` uses Java `String.hashCode()` (32-bit) directly — this is weaker. With
only ~10,000 commands across a session, the collision rate is still low in practice, but using two
different hash algorithms for what are conceptually the same element identity is a DRY violation and
a latent bug (especially since `elementHash` is stored in metadata and used for DB FK references).

### Uniqueness coverage
- AVID fingerprint: includes `packageName` — cross-app unique. Strong.
- `stableId()` in `ElementInfo`: `"$className|$resourceId|${bounds.left},${bounds.top}".hashCode()`
  — no `packageName`, so two apps with identically-named elements at the same coordinates would
  collide. Used only in `DoNotClickListModel` and `DynamicContentDetector` (not in the main
  command-targeting path), so not critical, but still a gap worth documenting.
- Screen fingerprint: SHA-256 of sorted element data — strong, 64 hex characters. No collision
  concern in practice.

---

## Overlay System

### Rendering approach
The overlay system uses a layered, platform-agnostic architecture:
1. `OverlayStateManager` (KMP `object`) holds `MutableStateFlow<List<NumberOverlayItem>>` as
   single source of truth.
2. `OverlayNumberingExecutor` assigns 1-N numbers by visual position (top→left sort) and manages
   per-container stable numbering.
3. `NumberOverlayRenderer` computes `RenderingParams` (badge center, radius, color, shape) from
   `NumberOverlayStyle` — fully platform-agnostic.
4. Platform layer (Android Compose, iOS SwiftUI) collects the StateFlow and renders badges using
   `RenderingParams`.

This is a clean separation. The renderer correctly handles: colorblind accessibility shapes
(circle/square/diamond state encoding), high-contrast mode, large-text mode, touch-bound expansion
(+8px TOUCH_PADDING), and drop shadow toggle.

### Performance characteristics
- `generateForListApp()` calls `elements.filter` + `findTopLevelListItems` (O(n)) + `elements.indexOf`
  per item (O(n)). The inner `indexOf` scan is O(n*m) where m = number of list items. For a typical
  30-item email list this is 30 × (30..300 total elements) = ~3,000–9,000 comparisons per overlay
  generation cycle. Acceptable for typical screens, but could degrade on complex DOM trees.
  Fix: pre-build an `index → element` map before the inner loop.

- `generateForAllClickable()` applies a hard cap of 50 elements and returns `emptyList()` when
  exceeded. This is a safe guard but could silently fail: the user sees no badges on a complex app
  screen and gets no feedback explaining why. Logging the rejection is present (`LoggingUtils.d`)
  but the end user receives no visual indication.

- `OverlayNumberingExecutor.trimIfNeeded()` trims per-container maps at 500 entries, but this
  method is never called from any code path visible in the reviewed files. The trim function is dead
  code. Without trimming, the `assignments` maps grow unbounded for long-running sessions with many
  containers.

---

## Code Smells

- **Rule 7 violation (Author: VOS4 Development Team)**: Found in 131 files across the VoiceOSCore
  module. This is a systemic batch violation. Files particularly visible in the scraping/overlay
  path: `ScreenFingerprinter.kt`, `NumberOverlayRenderer.kt`, `OverlayManager.kt`,
  `OverlayRegistry.kt`, `OverlayVisibilityManager.kt`, `OverlayDisposal.kt`, `OverlayConfig.kt`.

- **Code-Reviewed-By: CCA attribution**: Appears in at least 4 overlay files
  (`OverlayManager.kt`, `OverlayRegistry.kt`, `OverlayVisibilityManager.kt`,
  `NumberOverlayRenderer.kt`). "CCA" = Claude Code Agent. This is an indirect AI attribution
  and must be removed per Rule 7.

- **Duplicate label derivation logic**: `CommandGenerator.deriveLabel()` and
  `ElementLabels.deriveElementLabels()` both perform `text > contentDescription > resourceId`
  priority logic independently. The docstring on `ElementLabels` explicitly says it is the
  "SINGLE SOURCE OF TRUTH for overlay label text" but `CommandGenerator` has its own copy. These
  should share the same pipeline to prevent divergence.

- **Debug `println` proliferation**: Beyond the P0 case in `CommandGenerator`, there are 7
  additional `println` calls in `VoiceOSCore.kt` (not guarded by a debug flag), 3 in
  `StaticCommandPersistenceImpl.kt`, and 2 in `VoiceCommandPrompt.kt` that fire unconditionally on
  every no-match. All should use `LoggingUtils.d/w` which can be disabled at runtime.

- **`OverlayNumberingExecutor.trimIfNeeded()` is dead code**: The function exists (lines 133–143)
  but has zero call sites in the codebase. It was presumably intended to be called after
  `assignNumbers()` or periodically. Without it, `assignments` maps grow without bound.

- **`@Deprecated` `isDynamicContent` / `shouldPersist` in `ElementInfo`**: These deprecated
  properties are still referenced by the `fromElementWithPersistence(element, packageName)`
  overload which is itself marked `@Deprecated`. The deprecation chain is correct but no
  migration timeline is set. Legacy callers that haven't migrated will silently use the weaker
  2-layer heuristic instead of the 4-layer `PersistenceDecisionEngine`.

---

## Severity Summary

| Severity | Count | Key Items |
|----------|-------|-----------|
| P0 Critical | 2 | Root node leak on every screen change; debug println in hot path |
| P1 High | 4 | Root event leak in refreshScreen; OverlayNumberingExecutor race; hash collision risk; onCommandExecuted always EXECUTE |
| P2 Medium | 6 | No structural fingerprint for dynamic screens; findTopLevelListItems heuristic gaps; trimIfNeeded dead code; duplicate label derivation; Rule 7 (131 files); Code-Reviewed-By: CCA attribution |
