# VoiceOSCore Scraping Fixes - Handover Report V2
**Date:** 2026-01-20 | **Version:** V2 | **Author:** Claude | **Branch:** `VoiceOSCore-ScrapingUpdate`

---

## Executive Summary

Continuing work on voice command scraping fixes. Completed file restructuring (moving logic from app to module), now need to fix 4 runtime issues with command execution.

---

## Completed Work (This Session)

### 1. SymbolNormalizer Fix
- Fixed symbol mapping: `&` = "and"/"ampersand", `+` = "plus" (not "and")
- All 31 tests passing
- Committed and pushed

### 2. File Restructuring - Logic Moved to Module

Files successfully created in `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/`:

| File | Status | Description |
|------|--------|-------------|
| `ExplorationModels.kt` | ✅ Created | Data classes: ExplorationResult, VUIDInfo, HierarchyNode, DuplicateInfo, DeduplicationStats, GeneratedCommand |
| `AVUFormatter.kt` | ✅ Created | AVU format generation |
| `ElementExtractorUtils.kt` | ✅ Created | Utility functions: isDynamicContainer, findTopLevelListItems, deriveElementLabels |
| `OverlayModels.kt` | ✅ Created | Enums: NumbersOverlayMode, InstructionBarMode, BadgeTheme, AppNumbersPreference + NumberOverlayItem data class |
| `ScreenCacheManager.kt` | ✅ Created | Screen hash and caching logic |
| `BoundsResolver.kt` | ✅ Created | 4-layer bounds resolution (via agent) |
| `AndroidCommandPersistence.kt` | ✅ Created | Database bridge (via agent) |
| `BootReceiver.kt` | ✅ Created | Boot receiver (via agent) |
| `DynamicCommandGenerator.kt` | ✅ Created | Command generation with OverlayStateCallback interface (via agent) |

### 3. Original App Files (NOT YET DELETED)

The original files in `android/apps/voiceoscoreng/` still exist and need to be:
1. Updated to import from `com.augmentalis.voiceoscore` instead of local
2. Or deleted if fully replaced

---

## Pending Work

### Task 1: Update App Imports
Files in app need import updates to use module classes:
- `VoiceOSAccessibilityService.kt`
- `OverlayService.kt`
- `OverlayStateManager.kt` (now should use enums from module)
- Other files referencing moved classes

### Task 2: Delete Duplicate Files
After imports are updated, delete the original files from app that were moved to module.

### Task 3: Fix 4 Runtime Issues (CRITICAL)

#### Issue 1: Content Not Scraped on Scroll/UI Change
**Symptom:** When user scrolls or UI changes, new content is not being scraped.
**Likely Cause:** `TYPE_WINDOW_CONTENT_CHANGED` not being handled, or `continuousScanningEnabled` is false.
**Files to Check:**
- `android/apps/voiceoscoreng/.../VoiceOSAccessibilityService.kt`
- Look for `onAccessibilityEvent()`, `shouldHandleContentChange()`, `handleContentUpdate()`

#### Issue 2: Known Screen Hash Not Registering Commands with Voice Engine
**Symptom:** When a cached screen is found, commands aren't sent to speech engine.
**Likely Cause:** Cache HIT path doesn't call `updateSpeechEngine()` callback.
**Files to Check:**
- `android/apps/voiceoscoreng/.../VoiceOSAccessibilityService.kt`
- Look for screen hash lookup and compare cache HIT vs cache MISS paths

#### Issue 3: Calculator Numeric Click Commands Not Working
**Symptom:** Saying "1", "2", etc. in Calculator app doesn't click the buttons.
**Likely Cause:** Bounds resolution failing, or gesture dispatch not executing.
**Files to Check:**
- `Modules/VoiceOSCore/src/androidMain/.../AndroidGestureDispatcher.kt`
- `Modules/VoiceOSCore/src/commonMain/.../ActionCoordinator.kt`
- Look for TAP/CLICK action handlers

#### Issue 4: Index Voice Commands Not Clicking
**Symptom:** "first", "second", "1", "2" for list items don't trigger clicks.
**Likely Cause:** Index commands missing bounds metadata, or handler not executing tap.
**Files to Check:**
- `Modules/VoiceOSCore/src/commonMain/.../CommandGenerator.kt` - `generateListIndexCommands()`
- `Modules/VoiceOSCore/src/commonMain/.../ActionCoordinator.kt` - index command handling

---

## Key Architecture Notes

### KMP Structure
```
Modules/VoiceOSCore/
├── src/commonMain/kotlin/com/augmentalis/voiceoscore/
│   ├── CommandGenerator.kt      # Command generation logic
│   ├── CommandMatcher.kt        # Voice matching with symbol aliases
│   ├── ActionCoordinator.kt     # Command execution coordination
│   ├── SymbolNormalizer.kt      # Symbol → spoken word conversion
│   └── ...
├── src/androidMain/kotlin/com/augmentalis/voiceoscore/
│   ├── VoiceOSAccessibilityService.kt  # Base service (abstract)
│   ├── AndroidGestureDispatcher.kt     # Gesture execution
│   ├── AndroidScreenExtractor.kt       # UI tree extraction
│   ├── DynamicCommandGenerator.kt      # NEW: Moved from app
│   ├── BoundsResolver.kt               # NEW: Moved from app
│   └── ...
```

### App Structure (Should Be Thin)
```
android/apps/voiceoscoreng/
├── service/
│   ├── VoiceOSAccessibilityService.kt  # Extends module base, adds app wiring
│   ├── OverlayService.kt               # UI overlay (Compose)
│   └── OverlayStateManager.kt          # StateFlow management (uses module enums)
├── ui/                                  # UI screens
└── MainActivity.kt                      # App entry
```

### DynamicCommandGenerator Interface Change
The moved `DynamicCommandGenerator.kt` now uses an interface for overlay updates:

```kotlin
interface OverlayStateCallback {
    fun updateNumberedOverlayItems(items: List<NumberOverlayItem>)
    fun updateNumberedOverlayItemsIncremental(items: List<NumberOverlayItem>)
}

// App needs to implement this to bridge to OverlayStateManager
class DynamicCommandGenerator(
    ...,
    private val overlayStateCallback: OverlayStateCallback? = null
)
```

---

## Commands to Resume

```bash
# Switch to working branch
git checkout VoiceOSCore-ScrapingUpdate

# Check current status
git status

# Key files to read first
cat Docs/VoiceOSCore/Handover-ScrapingFixes-260120-V2.md
```

---

## Recommended Next Steps

1. **Start with Issue Investigation**
   - Read `VoiceOSAccessibilityService.kt` in the app
   - Trace the accessibility event flow
   - Find where each of the 4 issues occurs

2. **Fix Order (by dependency)**
   - Issue 1 (scroll scraping) - foundational
   - Issue 2 (cache registration) - depends on scraping
   - Issue 3 & 4 (click execution) - can be fixed together

3. **Testing**
   - Test with Settings app (symbol normalization)
   - Test with Calculator app (numeric clicks)
   - Test with Gmail app (index commands, scroll)

---

## Related Documents

| Document | Path |
|----------|------|
| Original Analysis | `Docs/VoiceOSCore/Analysis-ScrapingIssues-260120-V1.md` |
| Implementation Plan | `Docs/VoiceOSCore/Plan-ScrapingFixes-260120-V1.md` |
| Previous Handover | `Docs/VoiceOSCore/Handover-ScrapingFixes-260120-V1.md` |
| This Handover | `Docs/VoiceOSCore/Handover-ScrapingFixes-260120-V2.md` |

---

## Git Status Summary

**Modified files (unstaged):**
- `Modules/VoiceOSCore/src/commonMain/.../CommandGenerator.kt`
- `Modules/VoiceOSCore/src/commonMain/.../CommandMatcher.kt`
- `android/apps/voiceoscoreng/.../service/VoiceOSAccessibilityService.kt`
- `android/apps/voiceoscoreng/.../service/DynamicCommandGenerator.kt`
- `android/apps/voiceoscoreng/.../service/OverlayStateManager.kt`
- Various GlassmorphismUtils files

**New files (untracked in module):**
- `Modules/VoiceOSCore/src/androidMain/.../ExplorationModels.kt`
- `Modules/VoiceOSCore/src/androidMain/.../AVUFormatter.kt`
- `Modules/VoiceOSCore/src/androidMain/.../ElementExtractorUtils.kt`
- `Modules/VoiceOSCore/src/androidMain/.../OverlayModels.kt`
- `Modules/VoiceOSCore/src/androidMain/.../ScreenCacheManager.kt`
- `Modules/VoiceOSCore/src/androidMain/.../BoundsResolver.kt`
- `Modules/VoiceOSCore/src/androidMain/.../AndroidCommandPersistence.kt`
- `Modules/VoiceOSCore/src/androidMain/.../BootReceiver.kt`
- `Modules/VoiceOSCore/src/androidMain/.../DynamicCommandGenerator.kt`

---

**Status:** File restructuring ~90% complete, 4 runtime bugs need fixing
**Priority:** Fix runtime issues before cleanup/commit
