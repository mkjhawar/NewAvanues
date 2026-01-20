# VoiceOSCore Scraping Issues Analysis
**Date:** 2026-01-20 | **Version:** V1 | **Author:** Claude

## Executive Summary

Analysis of voice command scraping and execution issues across 4 test apps: Settings, Calculator, Gmail, and Teams. The issues fall into **3 core problem categories**:

1. **Text Parsing Issues** - Special characters splitting labels
2. **Screen Change Detection** - Scroll/navigation not triggering re-scrape
3. **Click Execution Failures** - Bounds becoming stale or coordinates not reaching dispatcher

---

## Issue Breakdown

### 1. Settings App - Issue A: Special Characters Splitting Labels

**Symptom:** "Display size & text", "Color & Motion", "Sound & vibration" each split into separate commands

**Evidence from logs:**
```
11:15:23.542 VoiceOSA11yService  D  Speech result: Sound (confidence: 1.0)
11:15:23.584 VoiceOSFactory      D  Executing TAP/CLICK for 'tap sound', metadata: {..., originalLabel=Sound, ...}
```
The `originalLabel=Sound` shows only "Sound" was registered, not "Sound & vibration".

**Root Cause Analysis:**

The label derivation in `CommandGenerator.deriveLabel()` (line 262-273) takes element.text directly:
```kotlin
private fun deriveLabel(element: ElementInfo): String {
    return when {
        element.text.isNotBlank() -> element.text  // Takes full text
        ...
    }
}
```

However, examining `ElementExtractor.extractElements()` (line 83-98):
```kotlin
val element = ElementInfo(
    ...
    text = node.text?.toString() ?: "",
    ...
)
```

The text extraction is correct. The issue is likely in **one of these places**:

1. **Vivoka Speech Engine grammar** - May not support "&" in phrases
2. **Speech recognition output** - Voice input "Sound and vibration" not matching "Sound & vibration"
3. **Command phrase storage** - The `updateSpeechEngine()` call passes phrases to Vivoka

**User Insight:** Treat "&" as equivalent to "and" across all languages.

---

### 2. Settings App - Issue B: Dynamic Command Click Fails After Scrolling

**Symptom:** After scrolling, newly visible items don't respond to voice commands

**Root Cause Analysis:**

The screen change handling in `VoiceOSAccessibilityService.handleScreenChange()` (line 467-534):
```kotlin
private fun handleScreenChange(packageName: String?) {
    // Only triggers on TYPE_WINDOW_STATE_CHANGED
    ...
    val isKnown = screenCacheManager.hasScreen(screenHash)
    if (isKnown && appVersion == storedVersion) {
        val cachedCommands = screenCacheManager.getCommandsForScreen(screenHash)
        // LOADS FROM CACHE - doesn't re-scrape!
        ...
    }
}
```

**Critical Gap:**
- `TYPE_WINDOW_STATE_CHANGED` fires on app/activity changes
- `TYPE_WINDOW_CONTENT_CHANGED` fires on scroll but is largely ignored (line 432-436):
```kotlin
AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
    if (event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE != 0) {
        //Log.v(TAG, "Window content changed (subtree): ${event.packageName}")
    }
}
```

**Why screen hash doesn't change:**
Screen hash is calculated from element hashes (`className|resourceId|text`). After scrolling:
- Same RecyclerView structure
- Same resource IDs
- Different content but not detected

**User Insight:** Need a way to detect "same screen, different content" and selectively re-scrape.

---

### 3. Calculator App - Numeric Commands Not Executing

**Symptom:** Numbers 1-9 recognized but click doesn't happen

**Evidence from logs:**
```
11:19:14.502 VoiceOSA11yService  D  Screen known - loaded 32 cached commands for com.google.android.calculator
```
Commands are loaded from cache.

**Root Cause Analysis:**

Looking at `AndroidGestureHandler.execute()` (line 139-206):
```kotlin
CommandActionType.TAP, CommandActionType.CLICK -> {
    ...
    val bounds = parseBoundsFromMetadata(command.metadata)
    if (bounds != null) {
        val success = dispatcher.click(bounds)
        ...
    } else {
        Log.w(TAG, "No bounds in metadata for '${command.phrase}', returning notHandled")
        HandlerResult.notHandled()
    }
}
```

The execution depends on valid `bounds` in metadata. When loading from cache, bounds are preserved but:

1. **Screen orientation may differ** - Cached bounds become invalid
2. **Keyboard layout changes** - Calculator buttons move
3. **Commands stored without bounds** - Some commands may not have bounds in cache

**Need to verify:** Check if numeric button commands have bounds in their metadata when cached.

---

### 4. Gmail App - Issues A-D

#### Issue A: Indexed Commands Recognized But Click Doesn't Execute

**Same root cause as Calculator** - bounds may be stale or missing.

#### Issue B: Screen Content Not Scraped on Scroll

**Same root cause as Settings Issue B** - TYPE_WINDOW_CONTENT_CHANGED ignored.

**Evidence from logs:**
```
11:49:27.165 VoiceOSA11yService  D  Screen known - loaded 1 cached commands for com.google.android.gm
```
Only 1 command loaded from cache for Gmail - likely just a menu button, not email items.

#### Issue C: In-App Navigation Doesn't Trigger Re-Scraping

**Root Cause:**
When navigating within Gmail (e.g., Inbox -> email detail):
- Activity doesn't change (same Gmail activity)
- TYPE_WINDOW_STATE_CHANGED may not fire
- Only TYPE_WINDOW_CONTENT_CHANGED fires

The current detection in `onAccessibilityEvent()` (line 424-437) only processes `TYPE_WINDOW_STATE_CHANGED`.

#### Issue D: Voice Overlay Not Updating on Screen Change

**Root Cause:**
`OverlayStateManager.updateNumberedOverlayItems()` is called from `DynamicCommandGenerator.generateCommands()` (line 110):
```kotlin
// Populate numbered overlay items for visual display
val overlayItems = generateOverlayItems(listItems, elements, packageName)

// Update overlay state
OverlayStateManager.updateNumberedOverlayItems(overlayItems)
```

If screen change is skipped due to caching, overlay items never update.

**User Insight:** Keep changing content in memory, not disk. Re-hash on scroll but preserve previously assigned numbers.

---

### 5. Teams App - Same as Gmail

The Teams logs show the same pattern:
```
12:09:16.721 VoiceOSA11yService  D  Window state changed: com.microsoft.teams
12:09:17.100 VoiceOSA11yService  D  Screen changed to com.microsoft.teams - new screen, scanning
```

Initial scan works, but subsequent in-app navigation doesn't trigger re-scan.

---

## Core Architecture Issues Identified

| # | Issue | Impact | Root Location |
|---|-------|--------|---------------|
| 1 | Special character handling in phrases | Labels split incorrectly | `SpeechEngine` / grammar update |
| 2 | TYPE_WINDOW_CONTENT_CHANGED ignored | Scroll doesn't re-scrape | `VoiceOSAccessibilityService:432` |
| 3 | Screen hash doesn't detect content changes | Cache serves stale commands | `ScreenCacheManager.generateScreenHash()` |
| 4 | Bounds become stale after caching | Clicks fail on cached commands | `DynamicCommandGenerator` persistence |
| 5 | Overlay only updates on full scrape | Overlay shows stale numbers | `OverlayStateManager` update timing |

---

## Data Flow Analysis

### Current Flow (Problematic)
```
AccessibilityEvent
    │
    ├── TYPE_WINDOW_STATE_CHANGED
    │       │
    │       ├── Check screen hash
    │       │       │
    │       │       ├── Known? → Load from cache (STALE DATA)
    │       │       │
    │       │       └── New? → Full scrape → Cache
    │       │
    │       └── Update speech engine
    │
    └── TYPE_WINDOW_CONTENT_CHANGED
            │
            └── Mostly ignored (debounced/logged only)
```

### Required Flow (Fixed)
```
AccessibilityEvent
    │
    ├── TYPE_WINDOW_STATE_CHANGED
    │       │
    │       └── Full scrape → Update commands → Update overlay
    │
    └── TYPE_WINDOW_CONTENT_CHANGED
            │
            ├── Check if significant change (scroll detected)
            │       │
            │       ├── Yes → Incremental scrape
            │       │       │
            │       │       ├── Merge with in-memory commands
            │       │       │
            │       │       └── Update overlay (preserve numbers for existing)
            │       │
            │       └── No → Ignore
            │
            └── Track for in-app navigation detection
```

---

## Key Files Requiring Changes

| File | Change Type | Purpose |
|------|-------------|---------|
| `VoiceOSAccessibilityService.kt` | Major | Handle TYPE_WINDOW_CONTENT_CHANGED for scroll |
| `CommandGenerator.kt` | Minor | Normalize "&" to "and" in phrases |
| `ScreenCacheManager.kt` | Major | Implement content-aware hashing |
| `DynamicCommandGenerator.kt` | Moderate | In-memory command merging |
| `OverlayStateManager.kt` | Moderate | Incremental overlay updates |
| `CommandRegistry.kt` | Minor | Support merge operations |

---

## References

- `VoiceOSAccessibilityService.kt:424-437` - Event handling
- `VoiceOSAccessibilityService.kt:467-534` - Screen change handling
- `CommandGenerator.kt:262-273` - Label derivation
- `ElementExtractor.kt:61-148` - Element extraction
- `AndroidGestureHandler:139-206` - Click execution
- `DynamicCommandGenerator.kt:72-155` - Command generation

---

## Next Steps

1. Review this analysis with user
2. Get approval on proposed architectural changes
3. Create detailed fix plan
4. Implement fixes with test coverage
