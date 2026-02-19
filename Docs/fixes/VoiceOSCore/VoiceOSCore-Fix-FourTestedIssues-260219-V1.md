# VoiceOSCore-Fix-FourTestedIssues-260219-V1

## Context
Four issues reported from on-device VoiceOS testing on Pixel 9 emulator.
All four fixed incrementally with separate commits for easy bisect.

## Branch: Cockpit-Development

---

## P0: FlowRow Crash in Cursor Color Settings (CRASH)

**Commit:** `282584df`

**Symptom:** App crashes with `NoSuchMethodError: FlowRow(...FlowRowOverflow...)` when clicking Cursor Color option in VoiceCursor Appearance settings.

**Root Cause:** Compose BOM version mismatch.
- AvanueUI (KMP module) compiled with JetBrains Compose 1.7.3 → foundation-layout 1.7.x (has `FlowRowOverflow` parameter)
- App uses `compose-bom:2024.06.00` → resolves foundation-layout to ~1.6.8 (no `FlowRowOverflow`)
- Runtime `FlowRow` signature doesn't match compile-time signature → `NoSuchMethodError`

**Fix:**
1. Updated `compose-bom` from `2024.06.00` to `2024.12.01` in `gradle/libs.versions.toml`
2. Replaced `FlowRow` in `SettingsColorRow` with `Column` + `Row` chunked grid (belt-and-suspenders)

**Files:** `gradle/libs.versions.toml`, `Modules/AvanueUI/.../SettingsComponents.kt`

**Note:** `FlowRow` is used in 14 locations across 10 files. The BOM bump protects all of them.

---

## P1: Overlay Selection Commands Not Working

**Commit:** `3d6e1541`

**Symptom:** Commands like "first", "1", "tap first", "select first", element labels not working.

**Root Cause:** `VoiceOSAccessibilityService.generateCommands()` only called `generateListIndexCommands()` (ordinals: "first", "second"), but never called:
- `generateNumericCommands()` → "1", "2", "3" matching overlay badge numbers
- `generateListLabelCommands()` → element labels like "Gmail", "Settings"

The correct implementation existed in `CommandOrchestrator` (calls all 3) but was not used in the active service code path.

**Fix:** Added `generateNumericCommands()` and `generateListLabelCommands()` calls alongside existing `generateListIndexCommands()` for list elements.

**Files:** `Modules/VoiceOSCore/.../VoiceOSAccessibilityService.kt`

**Known limitation:** For non-list apps with `NumbersOverlayMode.ON`, overlay badges show on all clickable elements but numeric commands only generate for elements with `listIndex >= 0`. Full overlay-to-command alignment for non-list apps requires deeper architectural work.

---

## P2: Overlay Not Clearing on App Switch

**Commit:** `6c01c390`

**Symptom:** Overlay badges from previous app persist for 100-500ms over the new app after switching. Sometimes don't clear at all.

**Root Cause:** On package change, `handleScreenChangeDebounced()` set `lastScreenHash = ""` but did NOT clear overlay items. The clear only happened asynchronously inside `DynamicCommandGenerator.processScreen()` → `handleScreenContext(isAppChange=true)` → `clearOverlayItems()`, creating a visible gap.

The code comment explained why `clearDynamicCommands()` was avoided (race condition with empty CommandRegistry), but `clearOverlayItems()` is separate — only affects visible badges, not the command registry.

**Fix:**
1. Added `onAppSwitched(newPackageName)` callback in `VoiceOSAccessibilityService` base class
2. Called it synchronously in the package-change path before `handleScreenChange()`
3. Overridden in `VoiceAvanueAccessibilityService` to clear overlay items + reset numbering immediately

**Files:** `Modules/VoiceOSCore/.../VoiceOSAccessibilityService.kt`, `apps/avanues/.../VoiceAvanueAccessibilityService.kt`

---

## P3: Static Command Execution Delay (2-5 seconds)

**Commit:** `959b52aa`

**Symptom:** Static commands have 2-5 second delay after recognition.

**Root Cause:** All command processing ran on `Dispatchers.Main`. The voice pipeline performed:
- O(n) `StaticCommandRegistry.findByPhrase` (275+ commands, no index)
- O(n log n) fuzzy matching in `CommandMatcher.match`
- Double `Mutex.withLock` acquisition (`canHandle` + `findHandler` called separately)
- `handlers.values.flatten()` list allocation 2x per command

All this computation blocked the main/UI thread.

**Fix (3 optimizations):**
1. **Dispatcher switch:** `processVoiceCommand` now launches on `Dispatchers.Default` instead of `Dispatchers.Main`; UI callbacks use `withContext(Main)`
2. **Collapse duplicate:** Eliminated separate `canHandle` + `findHandler` calls in ActionCoordinator Step 2; single `findHandler` call now (one mutex lock, one handler scan)
3. **HashMap index:** `StaticCommandRegistry.findByPhrase` now O(1) via `_phraseIndex: Map<String, StaticCommand>` built on `initialize()`; `findByPhraseInDomains` uses fast-path index before falling back to filter

**Files:** `Modules/VoiceOSCore/.../VoiceOSAccessibilityService.kt`, `Modules/VoiceOSCore/.../ActionCoordinator.kt`, `Modules/VoiceOSCore/.../StaticCommandRegistry.kt`

---

## P4: AvanueUI Dependency Missing in DeviceManager

**Commit:** `85fed03e`

**Symptom:** DeviceManager module fails to compile; AvanueUI theme types (`AvanueTheme`, `AvanueColorPalette`) are unresolved.

**Root Cause:** `Modules/DeviceManager/build.gradle.kts` did not declare a dependency on `:Modules:AvanueUI`. Other modules that use AvanueUI components (theming, color tokens) already carry this dependency, but DeviceManager was missed when AvanueUI was introduced as a KMP module.

Additionally, DeviceManager's `compose-bom` version was still pinned to `2024.06.00`, out of alignment with the rest of the project after the P0 BOM bump to `2024.12.01`.

**Fix:**
1. Added `implementation(project(":Modules:AvanueUI"))` to `Modules/DeviceManager/build.gradle.kts`
2. Updated `compose-bom` in DeviceManager to `2024.12.01` to match the project-wide version

**Files:** `Modules/DeviceManager/build.gradle.kts`

---

## P5: Handler List Allocation + CommandRegistry Performance

**Commit:** `8b2094b4`

**Symptom:** Even after the P3 dispatcher fix, handler lookup and command registry access had unnecessary allocation overhead at every voice command invocation.

**Root Cause:** Two distinct inefficiencies:
1. `HandlerRegistry` called `handlers.values.flatten()` on every `findHandler()` and `canHandle()` invocation, allocating a new `List<IHandler>` each time. With 13 registered handlers, this allocation happened at minimum twice per command (the collapsed `findHandler` from P3 was a single call, but internal registry iteration still rebuilt the flat list on every call).
2. `StaticCommandRegistry.all()` and related query methods were `suspend`-free, forcing callers to use `runBlocking { }` from coroutine-safe call sites. This blocked the coroutine thread pool and prevented callers from taking advantage of structured concurrency.

**Fix:**
1. **Handler list cache:** `HandlerRegistry` now computes the flattened handler list once in an `init` block and stores it as `_cachedHandlers: List<IHandler>`. `findHandler()` and `canHandle()` iterate `_cachedHandlers` directly — zero allocation per call. The cache is rebuilt only when handlers are registered (startup only).
2. **Suspend variants:** Added `suspend` overloads alongside all existing `StaticCommandRegistry` query methods (`all()`, `findByPhrase()`, `findByPhraseInDomains()`, `byCategory()`). Callers in coroutine scope use the suspend variants (run on the caller's dispatcher); legacy `runBlocking` callers continue to work via the non-suspend overloads during the transition.

**Files:** `Modules/VoiceOSCore/.../HandlerRegistry.kt`, `Modules/VoiceOSCore/.../StaticCommandRegistry.kt`

---

## P6: Overlay Badge Numbers Misaligned with Voice Commands + AUTO Mode

**Commit:** `2cfc0391`

**Symptom:**
- Numbers shown on overlay badges (e.g., badge "3") did not match the numeric voice commands ("three", "3", "third") that VoiceOS generated. Badges could show "5" for an element that VoiceOS assigned ordinal index 2.
- In `NumbersOverlayMode.AUTO`, overlay badges only appeared when the target app was active, leaving users with no visual reference when using VoiceOS on other apps.

**Root Cause:**
- Badge number assignment used a separate index counter in `OverlayItemGenerator` that was not synchronized with the `listIndex` used by `generateNumericCommands()` in `VoiceOSAccessibilityService`. The two counters could diverge when elements were filtered or skipped differently.
- `AUTO` mode evaluated `isTargetApp()` to decide whether to render overlay numbers, but non-target apps still generate index-based commands when `listIndex >= 0`. No badges meant users couldn't confirm which element corresponded to which number.

**Fix:**
1. **Badge-as-source-of-truth:** `OverlayItemGenerator.assignBadgeNumbers()` now generates the authoritative sequential badge numbers. These are stored on each `OverlayItem` and written to `CommandRegistry` under the `"overlay_numbers"` source key. `generateNumericCommands()` reads badge numbers from `CommandRegistry["overlay_numbers"]` rather than maintaining its own counter. Three command forms are registered per badge: digit (`"3"`), word (`"three"`), ordinal (`"third"`).
2. **AUTO mode badge rendering:** `AUTO` mode now renders overlay badges for all apps (not just the target app), matching the behaviour of `ON` mode. The distinction between `AUTO` and `ON` is retained for other behaviour (e.g., when VoiceOS activates vs. stays passive), but badge visibility is now consistent between the two modes.

**Files:** `Modules/VoiceOSCore/.../OverlayItemGenerator.kt`, `Modules/VoiceOSCore/.../VoiceOSAccessibilityService.kt`, `Modules/VoiceOSCore/.../CommandRegistry.kt`

---

## Verification Checklist
- [ ] Cursor Color dialog opens without crash
- [ ] "1", "2", "tap first" commands work in list apps (Gmail, etc.)
- [ ] Overlay badges clear instantly on app switch
- [ ] Static commands execute without noticeable delay
- [ ] No regression in voice command routing for existing commands
- [ ] DeviceManager module compiles without AvanueUI or BOM errors
- [ ] `HandlerRegistry.findHandler()` does not allocate a new list per call (confirm via allocation profiler or code review)
- [ ] `StaticCommandRegistry` suspend variants callable from coroutine scope without `runBlocking`
- [ ] Badge number on overlay item matches the numeric voice command that activates it ("3" badge → "3", "three", "third" all work)
- [ ] AUTO mode shows overlay badges on non-target apps
