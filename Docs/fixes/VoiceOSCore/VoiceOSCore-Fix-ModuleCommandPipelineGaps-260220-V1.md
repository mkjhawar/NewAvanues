# VoiceOSCore-Fix-ModuleCommandPipelineGaps-260220-V1

## Problem

All ~180 new module voice commands (Cockpit, Camera, Note, Annotation, Image, Video, Cast, AI) fail to execute on-device. The voice engine recognizes the phrase but commands either route with wrong actionType or fail to parse during VOS loading.

Testing report: `Static Commands_ Testing Report (3).md` shows:
- Pre-existing handlers (nav, scroll, media, volume, settings): **Working**
- New module commands (cockpit, camera, note, image, video, annotation, cast, AI): **Not working**

## Root Cause Analysis (5 bugs in command pipeline)

### Bug 1 (CRITICAL): processVoiceCommand Step 2 uses wrong actionType

**Location**: `ActionCoordinator.kt:701-706`

Step 2 created `QuantizedCommand(actionType = CommandActionType.EXECUTE)` for ALL static handler matches. Module executors have `when(actionType)` blocks routing by specific types (ADD_FRAME, FORMAT_BOLD, VIDEO_PLAY, etc.). `EXECUTE` matches no case → falls to else → silent failure.

Pre-existing handlers (MediaHandler, ScreenHandler, etc.) worked because they route by phrase text, not actionType. Module handlers route by actionType because they use the ModuleCommandCallbacks executor pattern.

### Bug 2 (CRITICAL): CommandCategory enum missing module entries

**Location**: `CommandModels.kt:457-502`

The enum had 15 entries: NAVIGATION, TEXT, MEDIA, SYSTEM, APP, ACCESSIBILITY, VOICE, GESTURE, CUSTOM, INPUT, APP_CONTROL, VOICE_CONTROL, APP_LAUNCH, BROWSER, WEB_GESTURE.

Missing: CAMERA, COCKPIT, NOTE, ANNOTATION, IMAGE, VIDEO, CAST, AI.

When VosParser maps "cam" → "CAMERA", `CommandCategory.valueOf("CAMERA")` throws → falls to CUSTOM category.

### Bug 3 (CRITICAL): VosParser ACTION_MAP missing 60+ entries

**Location**: `VosParser.kt ACTION_MAP`

Had entries for cam_*/cockpit_*/note_*/browser_*/gesture_* but was completely missing:
- ann_* (15 annotation action IDs)
- img_* (18 image action IDs)
- vid_* (12 video action IDs)
- cast_* (5 cast action IDs)
- ai_* (5 AI action IDs)

VosParser CATEGORY_MAP was also missing: ann, img, vid, cast, ai prefixes.

### Bug 4 (MODERATE): DB version not bumped

**Location**: `CommandLoader.kt:76` and `StaticCommandPersistenceImpl.kt:41`

Both versions were stale (3.1 and 2.1), meaning new VOS commands would not be reloaded on app start even after fixing the parser.

### Bug 5 (MODERATE): domainToCategory mapping incomplete

**Location**: `ActionCoordinator.kt:116-124`

Missing: annotation, image, video, cast domain mappings → fell to APP category.

## Solution

### Fix 1: StaticCommandRegistry lookup in Step 2

Added `StaticCommandRegistry.findByPhrase(normalizedText)` before the generic handler match. When found, uses `staticMatch.toQuantizedCommand()` which resolves the correct actionType from VOS data. Falls through to existing EXECUTE-based handler match as last resort.

Flow: "bold" → `findByPhrase("bold")` → matches `note_bold` → `toQuantizedCommand()` → `QuantizedCommand(actionType=FORMAT_BOLD)` → NoteCommandHandler → executor routes FORMAT_BOLD correctly.

### Fix 2: Added 8 entries to CommandCategory enum

Added: CAMERA, COCKPIT, NOTE, ANNOTATION, IMAGE, VIDEO, CAST, AI.

### Fix 3: Added 60+ VosParser map entries

CATEGORY_MAP: +5 entries (ann→ANNOTATION, img→IMAGE, vid→VIDEO, cast→CAST, ai→AI)
ACTION_MAP: +60 entries covering all annotation, image, video, cast, and AI action IDs.

### Fix 4: Bumped DB versions

- CommandLoader: "3.1" → "3.2"
- StaticCommandPersistenceImpl: "2.1" → "2.2"

### Fix 5: Added domain mappings

Added annotation, image, video, cast to `domainToCategory()`.

## Files Modified

| File | Change |
|------|--------|
| `actions/ActionCoordinator.kt` | +StaticCommandRegistry import, Step 2 registry lookup, domainToCategory +4 entries |
| `CommandModels.kt` | CommandCategory enum +8 entries |
| `loader/VosParser.kt` | CATEGORY_MAP +5, ACTION_MAP +60 entries |
| `commandmanager/loader/CommandLoader.kt` | version "3.1" → "3.2" |
| `loader/StaticCommandPersistenceImpl.kt` | version "2.1" → "2.2" |

## Impact

- ~180 module commands now route with correct actionType through the dispatch pipeline
- Pre-existing handlers (nav, media, system, etc.) unaffected — they still match via Step 0/Step 1 or the fallback EXECUTE path
- DB reload forced on next app start via version bump
- VoiceControl/Text/Input "Actual Action Not Working" issues are separate (executor wiring gaps, not pipeline gaps)

## Verification

After installing, all module commands should:
1. Be recognized by speech grammar (already working)
2. Resolve to correct CommandActionType via StaticCommandRegistry
3. Route to correct handler via HandlerRegistry
4. Execute via ModuleCommandCallbacks executor (when module is active)
