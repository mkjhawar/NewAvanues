# WebAvanue — Testing Issue Analysis
**Date:** 2026-02-10 | **Branch:** `060226-1-consolidation-framework`

---

## Executive Summary

Six issues reported from device testing. Root cause analysis reveals **3 critical bugs** and **3 secondary issues**. The critical bugs are interconnected — a missing `configChanges` manifest attribute causes Activity recreation on rotation, which triggers a Voyager navigation key collision crash, which in turn causes ANRs during recovery.

| # | Issue | Severity | Root Cause | Fix Complexity |
|---|-------|----------|------------|----------------|
| 1 | Desktop mode resets on rotation | CRITICAL | Missing `configChanges` in manifest — Activity recreated on rotation | 1 line |
| 2 | Mode icon/rendering mismatch after rotation | CRITICAL | Same root cause as #1 — tab's `isDesktopMode` survives DB but WebView recreated with default UA | Flows from Fix #1 |
| 3 | Voice commands not registering from web | HIGH | `BrowserVoiceOSCallback` persists commands to DB but never sends them to VoiceOSCore's speech engine | Medium |
| 4 | Command toggle causes ANR/freeze/crash | CRITICAL | `HistoryScreenNav:transition` Voyager key collision crash + composition cascade | Medium |
| 5 | Search icon not working | LOW | "Search" icon is actually the "Go" button — submits URL input, not a separate search action | UX clarification |
| 6 | Overall sluggish performance | MEDIUM | Cumulative: WebView recreation, voice command overhead, 67KB BrowserScreen.kt recomposition cost | Flows from other fixes |

---

## Issue 1: Desktop Mode Resets on Rotation

### Symptom
Select Advanced/Desktop mode → rotate to landscape → mode switches back to Mobile.

### Root Cause
**`AndroidManifest.xml` does not declare `android:configChanges` on `MainActivity`.**

```xml
<!-- CURRENT: No configChanges -->
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTask"
    ...>
```

Without `configChanges="orientation|screenSize|screenLayout|smallestScreenSize|keyboard|keyboardHidden"`, Android **destroys and recreates** the Activity on rotation. This means:

1. The `WebView` is destroyed (loses its user agent setting)
2. Compose state is saved/restored via `rememberSaveable`
3. Voyager navigation stack is serialized/deserialized
4. `TabViewModel` reloads from database — `Tab.isDesktopMode` is preserved in SQLDelight
5. **BUT** the `WebView` is brand new and starts with the **default mobile user agent**
6. The WebView's `setDesktopMode()` is only called on explicit toggle, not on restoration

### Why It Worked Before
Earlier builds likely had `configChanges` in the manifest, or WebAvanue was a separate Activity with its own manifest entry that included it.

### Fix
Add `configChanges` to MainActivity in `apps/avanues/src/main/AndroidManifest.xml`:
```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTask"
    android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|keyboard|keyboardHidden"
    ...>
```

This prevents Activity recreation on rotation. Compose handles layout changes automatically via `BoxWithConstraints` and orientation-aware composables (already implemented in `AddressBar.kt` and `BottomCommandBar.kt`).

**Secondary fix (defense in depth):** After WebView creation, apply the current tab's `isDesktopMode` setting:
```kotlin
// In WebViewContainer, after WebView initialization:
val tab = tabViewModel.activeTab.value
if (tab?.tab?.isDesktopMode == true) {
    webViewController.setDesktopMode(true)
}
```

---

## Issue 2: Mode Mismatch After Rotation (Icon vs Rendering)

### Symptom
In landscape, select Desktop mode → rotate to portrait. Top bar icon shows Mobile, but WebView renders Desktop.

### Root Cause
Same Activity recreation problem as Issue #1. On restoration:
- `Tab.isDesktopMode` is `true` in database → `val isDesktopMode = activeTab?.tab?.isDesktopMode ?: false` reads correctly
- But the `AddressBar` toggle icon depends on `isDesktopMode` prop, which comes from `activeTab` state
- **Race condition**: `TabViewModel` loads tabs asynchronously from SQLDelight. During the brief loading window, `activeTab` is `null`, so `isDesktopMode` defaults to `false`
- The `WebView` meanwhile may have cached desktop-mode content from before rotation

### Fix
Flows from Fix #1 (adding `configChanges`). Additionally, ensure the `WebViewContainer` applies the tab's `isDesktopMode` on every composition, not just on toggle:

In `BrowserScreen.kt` line 710, the `isDesktopMode` is already passed to `WebViewContainer`. Verify that `WebViewContainer` applies it as a `LaunchedEffect`:
```kotlin
LaunchedEffect(isDesktopMode) {
    webViewController.setDesktopMode(isDesktopMode)
}
```

---

## Issue 3: Voice Commands Not Registering from Web

### Symptom
DOM scraping works (`VoiceOS: DOM scraped - X elements, Y voice commands generated`), but voice commands from the web browser are not registered with the speech engine.

### Root Cause
**`BrowserVoiceOSCallback` generates and persists commands but never sends them to VoiceOSCore's speech engine.**

The data flow breaks here:
```
✅ WebView loads page → DOMScraperBridge injects JS → scrapes DOM
✅ BrowserVoiceOSCallback.onDOMScraped() receives elements
✅ VoiceCommandGenerator creates WebVoiceCommand objects
✅ Commands persisted to ScrapedWebCommand table (for whitelisted domains)
✅ Log shows "DOM scraped - X elements, Y voice commands generated"
❌ Commands NEVER sent to VoiceOSCore.updateCommands()
❌ Vivoka speech grammar never includes web commands
❌ Voice engine cannot recognize web-specific phrases
```

The `BrowserVoiceOSCallback` stores commands internally in `_currentScrapeResult` StateFlow, but nothing bridges these to VoiceOSCore's command pipeline. The accessibility-based scraping (VoiceOSCore's `AndroidScreenExtractor`) generates commands from the *accessibility tree*, which for WebView only sees the WebView container — not individual DOM elements.

### How It Should Work
After `onDOMScraped()` generates web voice commands, they need to be sent to VoiceOSCore as additional dynamic commands:
```kotlin
// In BrowserVoiceOSCallback.onDOMScraped():
val webPhrases = commands.map { it.phrase }
voiceOSCore?.addWebCommands(webPhrases)  // needs new API or updateCommands() integration
```

### Fix Approach
This requires a bridge between `BrowserVoiceOSCallback` (WebAvanue module) and VoiceOSCore. Options:
1. **Callback interface**: `BrowserVoiceOSCallback` emits commands via a callback that the host service collects and feeds to VoiceOSCore
2. **SharedFlow**: Expose web commands as a Flow that `VoiceAvanueAccessibilityService` collects alongside screen-scraped commands
3. **Direct VoiceOSCore reference**: Pass VoiceOSCore reference to BrowserVoiceOSCallback (tight coupling, not recommended)

**Recommended**: Option 2 — `BrowserVoiceOSCallback` already exposes `currentScrapeResult: StateFlow`. The accessibility service should collect from this and merge web commands into the speech grammar.

---

## Issue 4: Command Toggle Causes ANR/Freeze/Crash

### ANR Log Analysis

**Three ANRs** occurred within 5 minutes, all on `WebAvanueAlias`:

```
15:03:46 ANR #1: PID 9912, Waited 5002ms for MotionEvent
         App consuming 49% CPU, 1.25M minor page faults
15:05:50 ANR #2: PID 10856, Waited 5000ms for MotionEvent
15:08:52 ANR #3: PID 11275, Waited 5004ms for MotionEvent
         WebView sandboxed process consuming 15% CPU
```

**FATAL CRASH** (occurs after ANR recovery):
```
java.lang.IllegalArgumentException: Key com.augmentalis.webavanue.HistoryScreenNav:transition was used multiple times
  at SaveableStateHolderImpl$SaveableStateProvider
```

### Root Cause: Voyager Screen Key Collision

`HistoryScreenNav` is a **plain class** (not `data class`):
```kotlin
class HistoryScreenNav : Screen {  // ← NO key override, NO data class
```

Voyager generates the `Screen.key` from the class name by default. When multiple instances exist (which happens during Activity recreation or navigation restore), the `SaveableStateHolder` encounters the same key `"com.augmentalis.webavanue.HistoryScreenNav:transition"` for different instances → crash.

**Same problem exists for:**
- `BrowserScreenNav` (plain class)
- `DownloadsScreenNav` (plain class)
- `SettingsScreenNav` (plain class)
- `XRSettingsScreenNav` (plain class)
- `AboutScreenNav` (plain class)
- `ARPreviewScreenNav` (plain class)

Only `BookmarksScreenNav` is safe (it's a `data class` with `folderId` parameter, giving it unique identity).

### Why Command Toggle Triggers This
1. User taps command toggle → `isCommandBarVisible = !isCommandBarVisible`
2. `BottomCommandBar` `AnimatedVisibility` fires → heavy compose recomposition (31KB composable)
3. WebView + CommandBar + AnimatedVisibility overloads the main thread
4. Input events queue up → Android detects ANR (5000ms timeout)
5. After ANR, system may force-stop the Activity → restart
6. On restart, Voyager tries to restore navigation stack → `HistoryScreenNav` key collision → crash

### Fix
1. **Convert all Screen classes to `data object`** (Kotlin 1.9+) or override `key`:
```kotlin
data object HistoryScreenNav : Screen {
    @Composable
    override fun Content() { ... }
}
```
Or for classes that can't be `data object`:
```kotlin
class HistoryScreenNav : Screen {
    override val key = "HistoryScreen_${hashCode()}"
    ...
}
```

2. **Reduce command bar recomposition cost**: The `BottomCommandBar.kt` (31KB) is heavy. Consider:
   - Moving `AnimatedVisibility` outside the `BoxWithConstraints`
   - Using `remember` for stable lambdas
   - Debouncing the toggle to prevent rapid fire

---

## Issue 5: Search Icon Not Working

### Symptom
Top bar search icon does not perform any action.

### Root Cause
The "search" icon (🔍) at `AddressBar.kt:334` is the **"Go" button** that submits the URL/search query typed in the address bar:

```kotlin
// Line 324-339
IconButton(
    onClick = {
        dismissKeyboard()
        onGo()  // ← Submits the current URL input
    },
) {
    Icon(
        imageVector = Icons.Default.Search,
        contentDescription = "Go (Voice: go)",
        ...
    )
}
```

It triggers `onGo()` which calls `tabViewModel.navigateToUrl(urlInput)`. If the URL input field is empty, nothing happens because of the guard at `BrowserScreen.kt:524`:
```kotlin
onGo = {
    if (urlInput.isNotBlank()) {
        tabViewModel.navigateToUrl(urlInput)
    }
}
```

### Fix
Two options:
1. **If the user expects a separate search feature**: Add a dedicated search function that opens a search overlay or focuses the address bar with a search prefix
2. **If the user expects "Go" to always do something**: When URL is blank, navigate to the default search engine homepage, or focus the address bar for input

---

## Issue 6: Overall Sluggish Performance

### Contributing Factors

1. **Activity recreation on rotation**: WebView destruction and recreation is expensive (~500ms)
2. **67KB BrowserScreen.kt**: Single composable file with massive state. Every state change triggers wide recomposition
3. **BottomCommandBar AnimatedVisibility**: 31KB composable sliding in/out with animations
4. **Voice engine overhead**: Vivoka model compilation takes 50-200ms per grammar update; `VivokaModel: Cannot compile model - not ready` errors indicate model init failures
5. **No `configChanges`**: Every orientation change = full Activity lifecycle (destroy, create, resume, restore state, recreate WebView, reload page)
6. **1.25M minor page faults** (from ANR log): Indicates heavy memory allocation/deallocation, likely from WebView + Compose rendering pipeline

### Fix
Primarily flows from Fix #1 (configChanges) and Fix #4 (key collision). Additional:
- Consider extracting BrowserScreen into smaller composables with stable keys
- Profile the command bar toggle path specifically
- Ensure Vivoka model initialization completes before first speech command

---

## Priority Order

| Priority | Fix | Impact |
|----------|-----|--------|
| P0 | Add `configChanges` to manifest | Fixes rotation issues #1, #2, #6; reduces ANR frequency |
| P0 | Convert Screen classes to `data object` or add unique keys | Fixes crash in #4 |
| P1 | Bridge web voice commands to VoiceOSCore speech engine | Fixes #3 |
| P2 | Optimize command bar toggle performance | Reduces ANR in #4 |
| P3 | Clarify search icon behavior | Fixes #5 |

---

## Commits to Investigate

The user reports "it was working about 6 commits ago." Relevant WebAvanue commits:
```
08f8a089 feat(WebAvanue): Compact history rows, landscape 2-column grid
ec6b996d feat(WebAvanue): Compact toolbar badges, text-based indicators
943afac8 refactor(WebAvanue): Rename "Desktop Mode" to "Advanced Mode"
4176a994 feat(WebAvanue): Toolbar UX overhaul, command bar improvements
```

The toolbar UX overhaul (`4176a994`) and compact toolbar badges (`ec6b996d`) are the most likely regression sources — they restructured the AddressBar and CommandBar, possibly introducing the recomposition overhead and layout changes that now trigger ANRs.

The `configChanges` absence may be longstanding but was masked when WebAvanue was a separate module/activity. The consolidation into `apps/avanues/` with a single `MainActivity` exposed this gap.
