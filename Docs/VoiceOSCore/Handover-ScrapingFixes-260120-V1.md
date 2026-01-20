# VoiceOSCore Scraping Fixes - Handover Report
**Date:** 2026-01-20 | **Version:** V1 | **Author:** Claude | **Branch:** `VoiceOSCore-ScrapingUpdate`

---

## Executive Summary

Implementing fixes for voice command scraping and execution issues across 4 test apps (Settings, Calculator, Gmail, Teams). **6 of 8 tasks completed**, with BoundsResolver integration remaining.

---

## Completed Work

### Batch 1: Symbol Handling & Device Capability (COMPLETED)

| File | Location | Status | Description |
|------|----------|--------|-------------|
| `SymbolNormalizer.kt` | `Modules/VoiceOSCore/src/commonMain/.../` | ✅ NEW | Universal + localized symbol → spoken word conversion |
| `DeviceCapabilityManager.kt` (expect) | `Modules/VoiceOSCore/src/commonMain/.../` | ✅ NEW | KMP interface for device speed detection |
| `DeviceCapabilityManager.kt` (actual) | `Modules/VoiceOSCore/src/androidMain/.../` | ✅ NEW | Android implementation with RAM/CPU/SDK scoring |
| `CommandGenerator.kt` | `Modules/VoiceOSCore/src/commonMain/.../` | ✅ MODIFIED | Integrated SymbolNormalizer in `deriveLabel()` |
| `CommandMatcher.kt` | `Modules/VoiceOSCore/src/commonMain/.../` | ✅ MODIFIED | Added `matchWithSymbolAliases()` and symbol normalization in matching |

### Batch 2: Scroll & Content Change Detection (COMPLETED)

| File | Location | Status | Description |
|------|----------|--------|-------------|
| `VoiceOSAccessibilityService.kt` | `android/apps/voiceoscoreng/.../service/` | ✅ MODIFIED | Added `handleContentUpdate()`, `shouldHandleContentChange()`, scroll detection |
| `DynamicCommandGenerator.kt` | `android/apps/voiceoscoreng/.../service/` | ✅ MODIFIED | Added `generateCommandsIncremental()`, AVID assignment tracking, in-memory merging |
| `OverlayStateManager.kt` | `android/apps/voiceoscoreng/.../service/` | ✅ MODIFIED | Added `updateNumberedOverlayItemsIncremental()` for stable numbering |

### Batch 3: Bounds Resolution (PARTIALLY COMPLETED)

| File | Location | Status | Description |
|------|----------|--------|-------------|
| `BoundsResolver.kt` | `android/apps/voiceoscoreng/.../service/` | ✅ NEW | Hybrid 4-layer bounds resolution strategy |
| AndroidGestureHandler integration | TBD | ❌ PENDING | Need to find and integrate BoundsResolver |

---

## Key Implementation Details

### 1. SymbolNormalizer (`SymbolNormalizer.kt`)

```kotlin
// Universal aliases for all locales
val universalAliases = mapOf(
    "&" to listOf("and"),
    "#" to listOf("pound", "hash", "number", "hashtag"),
    "@" to listOf("at", "at the rate of"),
    // ... 25+ symbols
)

// Key methods:
fun normalize(text: String, locale: String = "en"): String
fun matchWithAliases(voiceInput: String, phrase: String, locale: String = "en"): Boolean
fun generatePhraseVariations(phrase: String, locale: String = "en"): List<String>
```

**Usage:** "Display size & text" → "Display size and text" for speech engine

### 2. DeviceCapabilityManager

```kotlin
// Dynamic debounce based on device speed
enum class DeviceSpeed { FAST, MEDIUM, SLOW }

// Debounce values:
// FAST:   100ms content, 50ms scroll
// MEDIUM: 200ms content, 100ms scroll
// SLOW:   300ms content, 150ms scroll

// User override supported via setUserDebounceMs()
```

**Initialization:** Call `DeviceCapabilityManager.init(context)` in service `onServiceConnected()`

### 3. Scroll Detection in Accessibility Service

```kotlin
// NEW: Handle TYPE_WINDOW_CONTENT_CHANGED for scroll
override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    when (event.eventType) {
        TYPE_WINDOW_STATE_CHANGED -> handleScreenChange(...)  // Full scrape
        TYPE_WINDOW_CONTENT_CHANGED -> {
            if (shouldHandleContentChange(event)) {
                handleContentUpdate(event)  // Incremental update
            }
        }
    }
}

// shouldHandleContentChange() checks:
// - continuousScanningEnabled
// - CONTENT_CHANGE_TYPE_SUBTREE flag
// - Source is scrollable or isDynamicContainer()
```

### 4. Incremental Command Generation

```kotlin
// In DynamicCommandGenerator:
private val avidAssignments = mutableMapOf<String, Int>()  // hash → number
private var nextAvidNumber = 1
private var currentAppPackage: String? = null

fun generateCommandsIncremental(
    elements: List<ElementInfo>,
    existingCommands: List<QuantizedCommand>,
    ...
): IncrementalUpdateResult {
    // Reset on app change
    // Preserve AVID numbers for known elements
    // Assign new numbers only for new elements
}
```

### 5. BoundsResolver (4-Layer Strategy)

```kotlin
class BoundsResolver(private val service: AccessibilityService) {
    fun resolve(command: QuantizedCommand): Bounds? {
        // Layer 1: Metadata bounds (0-1ms, 60% success)
        tryMetadataBounds(command)?.let { return it }

        // Layer 2: Delta compensation (1-2ms, 25% success)
        tryDeltaCompensation(command)?.let { return it }

        // Layer 3: Resource ID search (5-10ms, 10% success)
        tryAnchorSearch(command)?.let { return it }

        // Layer 4: Full tree search (50-100ms, 5% fallback)
        return tryFullTreeSearch(command)
    }
}
```

---

## Remaining Work

### Task 8: Integrate BoundsResolver with Click Execution

**What needs to be done:**
1. Find where click commands are executed using bounds from metadata
2. Replace direct bounds parsing with `BoundsResolver.resolve(command)`
3. Handle null result (element not found)

**Probable locations to check:**
- `VoiceOSCoreAndroidFactory.kt` - creates handlers
- Handler that processes TAP/CLICK action types
- Look for code that does: `command.metadata["bounds"]` → click

**Integration pattern:**
```kotlin
// BEFORE:
val boundsStr = command.metadata["bounds"]
val bounds = parseBounds(boundsStr) ?: return HandlerResult.notHandled()
dispatcher.click(bounds)

// AFTER:
val bounds = boundsResolver.resolve(command)
if (bounds == null) {
    Log.w(TAG, "Could not resolve bounds for '${command.phrase}'")
    return HandlerResult.notHandled()
}
dispatcher.click(bounds)
```

---

## Testing Checklist

After completing BoundsResolver integration:

### Settings App
- [ ] "Sound and vibration" works when saying "sound and vibration"
- [ ] "Display size & text" matches "display size and text"
- [ ] Scroll down shows new items with working voice commands

### Calculator App
- [ ] Numeric buttons 1-9 respond to voice commands
- [ ] Test rapid number entry
- [ ] Verify bounds resolution finds buttons correctly

### Gmail App
- [ ] Email list items respond to index commands ("first", "second")
- [ ] Scroll reveals new emails with working commands
- [ ] Overlay numbers update correctly (existing items keep numbers)
- [ ] In-app navigation (inbox → email → back) triggers re-scrape

### Teams App
- [ ] Landing screen commands work
- [ ] Navigation within Teams triggers re-scrape

### Performance
- [ ] Bounds resolution layer hit rates logged
- [ ] Debounce works correctly on different device speeds
- [ ] Memory usage stays bounded on app change

---

## User Decisions (Captured)

| Decision | User Choice |
|----------|-------------|
| Special Characters | All symbols with aliases, Universal + Localized |
| Memory Limit | Unlimited until app change |
| Debounce | Dynamic (device-based), default 200ms, user-configurable |
| Bounds Strategy | Hybrid 4-layer approach |

---

## Files Changed Summary

### NEW Files (5)
```
Modules/VoiceOSCore/src/commonMain/.../SymbolNormalizer.kt
Modules/VoiceOSCore/src/commonMain/.../DeviceCapabilityManager.kt (expect)
Modules/VoiceOSCore/src/androidMain/.../DeviceCapabilityManager.kt (actual)
android/apps/voiceoscoreng/.../service/BoundsResolver.kt
```

### MODIFIED Files (5)
```
Modules/VoiceOSCore/src/commonMain/.../CommandGenerator.kt
Modules/VoiceOSCore/src/commonMain/.../CommandMatcher.kt
android/apps/voiceoscoreng/.../service/VoiceOSAccessibilityService.kt
android/apps/voiceoscoreng/.../service/DynamicCommandGenerator.kt
android/apps/voiceoscoreng/.../service/OverlayStateManager.kt
```

---

## Related Documents

| Document | Path |
|----------|------|
| Analysis | `Docs/VoiceOSCore/Analysis-ScrapingIssues-260120-V1.md` |
| Plan | `Docs/VoiceOSCore/Plan-ScrapingFixes-260120-V1.md` |
| This Handover | `Docs/VoiceOSCore/Handover-ScrapingFixes-260120-V1.md` |

---

## Quick Resume Commands

```bash
# Switch to the working branch
git checkout VoiceOSCore-ScrapingUpdate

# Check status
git status

# Key files to read first
cat Docs/VoiceOSCore/Plan-ScrapingFixes-260120-V1.md
```

---

## Context for Next Session

1. **BoundsResolver is created but not integrated** - Need to find where TAP/CLICK commands execute and replace bounds parsing with `BoundsResolver.resolve()`

2. **All KMP-first patterns followed** - Common code in `commonMain`, Android-specific in `androidMain`

3. **No breaking changes** - All additions are additive, existing functionality preserved

4. **Build not tested** - Changes may have compilation issues to resolve

---

**Status:** 75% Complete (6/8 tasks)
**Next Action:** Integrate BoundsResolver with click execution handler
