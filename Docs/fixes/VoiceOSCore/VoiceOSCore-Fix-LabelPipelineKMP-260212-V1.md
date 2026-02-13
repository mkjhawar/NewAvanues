# VoiceOSCore Fix: Label Pipeline KMP Consolidation

**Date**: 2026-02-12
**Branch**: VoiceOSCore-KotlinUpdate (0a35b41d), IosVoiceOS-Development (2dc01263)
**Module**: VoiceOSCore + apps/avanues

## Problem

Overlay label cleaning (`cleanCommaLabel`) was not working on device for two reasons:

1. **Case-sensitive STATUS_WORDS matching**: `cleanCommaLabel` used `it in statusWords` where the set contained lowercase values ("unread") but Android accessibility returns capitalized values ("Unread"). The comparison never matched, so status prefixes were never stripped.

2. **Multiple label code paths**: 4 separate label derivation paths existed, only 1 went through `cleanCommaLabel`:
   - Path A: `deriveElementLabels()` → `cleanCommaLabel` (correct)
   - Path B: `generateForListApp()` fallback → raw `element.contentDescription.take(20)` (no cleaning)
   - Path C: `generateForAllClickable()` fallback → raw `element.contentDescription.take(20)` (no cleaning)
   - Path D: `deriveIconLabel()` (Layer 1) → raw `element.contentDescription.take(15)` (no cleaning)

## Root Cause

Label derivation code was scattered across multiple Android-only files with inconsistent cleaning. No single source of truth existed for label text.

## Solution

Consolidated all label logic into a single KMP commonMain file: `ElementLabels.kt`

### New KMP File

`Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/element/ElementLabels.kt`

Contains:
- `HierarchyNode` data class (moved from Android ElementExtractor)
- `cleanCommaLabel()` — now with **case-insensitive** matching via `it.lowercase() in STATUS_WORDS`
- `cleanResourceId()` — humanizes resource ID strings
- `isIconOnlyElement()` — classifies icon-only interactive elements
- `findTopLevelListItems()` — now with case-insensitive prefix matching
- `deriveElementLabels()` — **SINGLE SOURCE OF TRUTH** for all overlay label text

### Architecture

```
Platform extraction (Android AccessibilityNodeInfo / iOS AXUIElement)
  → List<ElementInfo> + List<HierarchyNode>
  → ElementLabels.deriveElementLabels()    ← KMP single source of truth
  → Map<Int, String>                       ← cleaned labels for all elements
  → Overlay generators consume labels map (no fallback paths)
```

### Label Priority (per element)

1. `text` — already human-readable visible text
2. `contentDescription` → `cleanCommaLabel()` strips status prefixes
3. `resourceId` → `cleanResourceId()` humanizes the ID
4. Walk direct children for text/contentDescription
5. Fallback to simplified class name

### Android Changes

| File | Change |
|------|--------|
| `ElementExtractor.kt` | Removed: HierarchyNode, findTopLevelListItems, deriveElementLabels, cleanCommaLabel, cleanResourceId, isIconOnlyElement. Added: import KMP HierarchyNode |
| `OverlayItemGenerator.kt` | Removed: deriveIconLabel(), all fallback label paths. Now consumes pre-derived labels map only |
| `DynamicCommandGenerator.kt` | Changed: `ElementExtractor.deriveElementLabels` → `ElementLabels.deriveElementLabels` |

### Key Fixes

- Case-insensitive `STATUS_WORDS` set with expanded vocabulary (16 status words)
- Case-insensitive prefix checks in `findTopLevelListItems` (startsWith "unread,", etc.)
- Eliminated all raw `contentDescription.take(N)` fallback paths
- All overlay generators (Layer 1 icon labels + Layer 2 numbered badges) use same pipeline

## Impact

- Gmail inbox: "Unread, , , John Doe, Subject, 2:30 PM" → now correctly shows "John Doe"
- Icon labels: now properly cleaned through same pipeline
- iOS: can now share the entire label pipeline via KMP commonMain
- ~170 lines of duplicate Android code removed

## Verification

```bash
./gradlew :Modules:VoiceOSCore:compileDebugKotlinAndroid :apps:avanues:compileDebugKotlin
```

Both branches build successfully.
