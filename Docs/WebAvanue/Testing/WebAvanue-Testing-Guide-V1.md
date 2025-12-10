# WebAvanue Testing Guide V1

**Version:** 1.0
**Date:** 2025-12-09
**Status:** Active
**Module:** WebAvanue Browser

---

## Table of Contents

1. [Settings Features Testing](#settings-features-testing)
2. [Headless Mode Testing](#headless-mode-testing)
3. [APIs and Intents](#apis-and-intents)
4. [Test Procedures](#test-procedures)
5. [Automation Scripts](#automation-scripts)

---

## Settings Features Testing

### 1. Search Suggestions

**Feature Path:** Settings > General > Search Suggestions

| Test Case | Steps | Expected Result | Priority |
|-----------|-------|----------------|----------|
| Enable search suggestions | 1. Open Settings<br>2. Navigate to General<br>3. Enable "Search Suggestions" | Search bar shows suggestions as user types | High |
| Disable search suggestions | 1. Open Settings<br>2. Disable "Search Suggestions" | No suggestions appear in search bar | High |
| Voice search toggle | 1. Enable "Voice Search"<br>2. Tap microphone icon in search bar | Voice recognition starts, accepts voice input | Medium |
| Search engine selection | 1. Tap "Search Engine"<br>2. Select from: Google, DuckDuckGo, Bing, Brave, Ecosia | Selected engine used for searches | High |

**Settings Keys:**
- `searchSuggestions`: Boolean
- `voiceSearch`: Boolean
- `defaultSearchEngine`: SearchEngine enum (GOOGLE, DUCKDUCKGO, BING, BRAVE, ECOSIA, CUSTOM)

---

### 2. New Tab Page

**Feature Path:** Settings > General > New Tab Page

| Test Case | Steps | Expected Result | Priority |
|-----------|-------|----------------|----------|
| Blank page | Select "Blank page" | New tabs open with blank white page | Medium |
| Home page | Select "Home page" | New tabs open with configured homepage | Medium |
| Top sites | Select "Top sites" | New tabs show grid of most visited sites | High |
| Most visited | Select "Most visited" | New tabs show list of frequently accessed sites | Medium |
| Speed dial | Select "Speed dial" | New tabs show customizable speed dial grid | Low |
| News feed | Select "News feed" | New tabs show curated news articles | Low |

**Settings Keys:**
- `newTabPage`: NewTabPage enum (BLANK, HOME_PAGE, TOP_SITES, MOST_VISITED, SPEED_DIAL, NEWS_FEED)
- `homePage`: String (URL)
- `restoreTabsOnStartup`: Boolean

---

### 3. Appearance

**Feature Path:** Settings > Appearance

| Test Case | Steps | Expected Result | Priority |
|-----------|-------|----------------|----------|
| Theme: System | Select "System default" | App follows system dark/light mode | High |
| Theme: Light | Select "Light" | App uses light theme regardless of system | High |
| Theme: Dark | Select "Dark" | App uses dark theme regardless of system | High |
| Theme: Auto | Select "Auto (time-based)" | Theme changes based on time of day | Medium |
| Font size: Tiny | Select "Tiny" (0.75x) | All text renders at 75% scale | Medium |
| Font size: Small | Select "Small" (0.875x) | All text renders at 87.5% scale | Medium |
| Font size: Medium | Select "Medium" (1.0x) | Default text size | High |
| Font size: Large | Select "Large" (1.125x) | Text renders at 112.5% scale | Medium |
| Font size: Huge | Select "Huge" (1.25x) | Text renders at 125% scale | Medium |
| Show images | Toggle off | Web pages load without images | Medium |
| Force zoom | Enable | Zoom works on all pages including those that disable it | Medium |
| Initial page scale | Set slider (50%-200%) | Pages load at specified scale percentage | Low |

**Settings Keys:**
- `theme`: Theme enum (LIGHT, DARK, SYSTEM, AUTO)
- `fontSize`: FontSize enum (TINY, SMALL, MEDIUM, LARGE, HUGE)
- `showImages`: Boolean
- `forceZoom`: Boolean
- `initialScale`: Float (0.5-2.0)

---

### 4. Privacy & Security

**Feature Path:** Settings > Privacy & Security

| Test Case | Steps | Expected Result | Priority |
|-----------|-------|----------------|----------|
| Enable JavaScript | Toggle on | JavaScript executes on web pages | Critical |
| Disable JavaScript | Toggle off | JavaScript blocked, many sites break | High |
| Enable cookies | Toggle on | Sites can store cookies | Critical |
| Disable cookies | Toggle off | Cookie storage blocked | High |
| Block pop-ups | Enable | Pop-up windows prevented | High |
| Block ads | Enable | Advertisements filtered from pages | High |
| Block trackers | Enable | Cross-site tracking prevented | High |
| Do Not Track header | Enable | DNT header sent with requests | Medium |
| Enable WebRTC | Toggle on | Real-time communication features work | Medium |
| Disable WebRTC | Toggle off | WebRTC blocked (prevents IP leaks) | Medium |
| Clear cache on exit | Enable | Browser cache cleared when app closes | Medium |
| Clear history on exit | Enable | Browsing history cleared on app close | Medium |
| Clear cookies on exit | Enable | All cookies deleted on app close | Medium |
| Site permissions | Navigate to sub-screen | Manage camera, microphone, location per-site | High |

**Settings Keys:**
- `enableJavaScript`: Boolean
- `enableCookies`: Boolean
- `blockPopups`: Boolean
- `blockAds`: Boolean
- `blockTrackers`: Boolean
- `doNotTrack`: Boolean
- `enableWebRTC`: Boolean
- `clearCacheOnExit`: Boolean
- `clearHistoryOnExit`: Boolean
- `clearCookiesOnExit`: Boolean

**Privacy Presets:**
```kotlin
// Privacy mode preset
BrowserSettings.privacyMode()
// Applies: blockPopups=true, blockAds=true, blockTrackers=true,
// doNotTrack=true, clearCacheOnExit=true, clearHistoryOnExit=true,
// clearCookiesOnExit=true, enableWebRTC=false,
// defaultSearchEngine=DUCKDUCKGO, searchSuggestions=false
```

---

### 5. Advanced

**Feature Path:** Settings > Advanced

| Test Case | Steps | Expected Result | Priority |
|-----------|-------|----------------|----------|
| Desktop mode | Enable | User agent changes to desktop, desktop layouts shown | High |
| Desktop mode: Default zoom | Set slider (50%-200%) | Desktop pages load at specified zoom | Medium |
| Desktop mode: Window width | Set slider (800-1920px) | Simulated viewport width applied | Low |
| Desktop mode: Window height | Set slider (600-1200px) | Simulated viewport height applied | Low |
| Desktop mode: Auto-fit zoom | Enable | Zoom auto-adjusts to fit content in viewport | Medium |
| Voice commands | Enable | Voice control activated for browser | High |
| Voice dialog auto-close | Enable | Voice dialog closes after command execution | Medium |
| Voice dialog delay | Set slider (500-5000ms) | Dialog closes after specified delay | Low |
| Navigate to XR Settings | Tap navigation item | Opens WebXR settings screen | Medium |
| AR Layout Preview | Tap navigation item | Opens AR spatial layout demo | Low |

**Settings Keys:**
- `useDesktopMode`: Boolean
- `desktopModeDefaultZoom`: Int (50-200)
- `desktopModeWindowWidth`: Int (800-1920)
- `desktopModeWindowHeight`: Int (600-1200)
- `desktopModeAutoFitZoom`: Boolean
- `enableVoiceCommands`: Boolean
- `voiceDialogAutoClose`: Boolean
- `voiceDialogAutoCloseDelayMs`: Long (500-5000)

---

### 6. Downloads

**Feature Path:** Settings > Downloads

| Test Case | Steps | Expected Result | Priority |
|-----------|-------|----------------|----------|
| Default download path | Leave path empty/null | Files download to system default location | High |
| Custom download path | Set custom path | Files download to specified directory | High |
| Ask download location | Enable | Prompt appears before each download | Medium |
| Download over WiFi only | Enable | Downloads blocked on cellular data | Medium |
| Download over WiFi only | Disable | Downloads allowed on any connection | Medium |

**Settings Keys:**
- `downloadPath`: String? (null = default)
- `askDownloadLocation`: Boolean
- `downloadOverWiFiOnly`: Boolean

---

### 7. Performance

**Feature Path:** Settings > Performance

| Test Case | Steps | Expected Result | Priority |
|-----------|-------|----------------|----------|
| Hardware acceleration | Enable | GPU used for rendering (smoother, faster) | High |
| Hardware acceleration | Disable | CPU-only rendering (compatibility mode) | Medium |
| Preload pages | Enable | Pages load in background for faster access | Medium |
| Preload pages | Disable | Pages load only when accessed | Medium |
| Data saver | Enable | Pages compressed, data usage reduced | Medium |
| Data saver | Disable | Full quality pages | High |
| Text reflow | Enable | Text reformats automatically when zooming | Medium |
| Text reflow | Disable | Text layout remains fixed when zooming | Low |

**Settings Keys:**
- `hardwareAcceleration`: Boolean
- `preloadPages`: Boolean
- `dataSaver`: Boolean
- `textReflow`: Boolean
- `autoPlay`: AutoPlay enum (ALWAYS, WIFI_ONLY, NEVER, ASK)

**Performance Preset:**
```kotlin
// Performance mode preset
BrowserSettings.performanceMode()
// Applies: showImages=false, enableJavaScript=false,
// hardwareAcceleration=true, preloadPages=false,
// dataSaver=true, autoPlay=NEVER, blockAds=true
```

---

### 8. Sync

**Feature Path:** Settings > Sync

| Test Case | Steps | Expected Result | Priority |
|-----------|-------|----------------|----------|
| Enable sync | Toggle "Sync Enabled" on | Sync service activates | High |
| Disable sync | Toggle "Sync Enabled" off | All sync stops | High |
| Sync bookmarks | Enable (when sync enabled) | Bookmarks sync across devices | High |
| Sync history | Enable (when sync enabled) | Browsing history syncs | Medium |
| Sync passwords | Enable (when sync enabled) | Saved passwords sync | High |
| Sync settings | Enable (when sync enabled) | Browser settings sync | Medium |
| Selective sync | Enable only bookmarks | Only bookmarks sync, others don't | Medium |

**Settings Keys:**
- `syncEnabled`: Boolean (master switch)
- `syncBookmarks`: Boolean (requires syncEnabled)
- `syncHistory`: Boolean (requires syncEnabled)
- `syncPasswords`: Boolean (requires syncEnabled)
- `syncSettings`: Boolean (requires syncEnabled)

**Dependencies:**
- All sync sub-options only visible when `syncEnabled=true`

---

### 9. Voice & AI

**Feature Path:** Settings > Voice & AI

| Test Case | Steps | Expected Result | Priority |
|-----------|-------|----------------|----------|
| AI summaries | Enable | AI-powered page summaries available | Medium |
| AI summaries | Disable | Summary feature unavailable | Low |
| AI translation | Enable | AI translation available for pages | Medium |
| AI translation | Disable | Translation feature unavailable | Low |
| Read aloud | Enable | Text-to-speech available | Medium |
| Read aloud | Disable | TTS unavailable | Low |

**Settings Keys:**
- `enableVoiceCommands`: Boolean (from Advanced)
- `aiSummaries`: Boolean
- `aiTranslation`: Boolean
- `readAloud`: Boolean

---

### 10. Command Bar

**Feature Path:** Settings > Command Bar

| Test Case | Steps | Expected Result | Priority |
|-----------|-------|----------------|----------|
| Auto-hide command bar | Enable | Command bar hides after timeout | Medium |
| Auto-hide command bar | Disable | Command bar remains visible | Medium |
| Auto-hide delay | Set slider (3000-30000ms) | Bar hides after specified delay | Low |
| Auto-hide delay: 3 seconds | Set to 3000ms | Bar hides after 3 seconds | Low |
| Auto-hide delay: 30 seconds | Set to 30000ms | Bar hides after 30 seconds | Low |

**Settings Keys:**
- `commandBarAutoHide`: Boolean
- `commandBarAutoHideDelayMs`: Long (3000-30000, default 10000)

---

### 11. WebXR

**Feature Path:** Settings > WebXR

| Test Case | Steps | Expected Result | Priority |
|-----------|-------|----------------|----------|
| Enable WebXR | Toggle master switch on | WebXR API available to web pages | High |
| Disable WebXR | Toggle master switch off | WebXR API blocked | Medium |
| Enable AR | Enable (when WebXR enabled) | immersive-ar sessions allowed | High |
| Enable VR | Enable (when WebXR enabled) | immersive-vr sessions allowed | High |
| XR Performance: High Quality | Select | 90fps target, high battery drain | Medium |
| XR Performance: Balanced | Select | 60fps target, balanced usage | High |
| XR Performance: Battery Saver | Select | 45fps target, reduced effects | Medium |
| XR auto-pause timeout | Set slider (10-120 min) | Session pauses after inactivity | Medium |
| Show FPS indicator | Enable | Frame rate displayed in XR session | Low |
| Require WiFi for XR | Enable | XR sessions blocked on cellular | Medium |
| Require WiFi for XR | Disable | XR sessions allowed on any connection | Medium |

**Settings Keys:**
- `enableWebXR`: Boolean (master switch)
- `enableAR`: Boolean (requires enableWebXR)
- `enableVR`: Boolean (requires enableWebXR)
- `xrPerformanceMode`: XRPerformanceMode enum (HIGH_QUALITY, BALANCED, BATTERY_SAVER)
- `xrAutoPauseTimeout`: Int (10-120 minutes)
- `xrShowFPSIndicator`: Boolean
- `xrRequireWiFi`: Boolean

**Performance Modes:**
- **HIGH_QUALITY**: 90fps target, maximum quality, higher battery drain
- **BALANCED**: 60fps target, balanced quality/performance (default)
- **BATTERY_SAVER**: 45fps target, reduced effects, lower battery usage

**Dependencies:**
- All WebXR sub-settings only visible when `enableWebXR=true`
- AR and VR switches require WebXR enabled

---

## Headless Mode Testing

### Overview

Headless mode allows WebAvanue to run without UI for automated testing, scraping, or background processing.

### Desktop Headless Mode

**Platform:** Desktop (JVM/JavaFX)

**Test Cases:**

| Test Case | Command/API | Expected Result | Priority |
|-----------|-------------|----------------|----------|
| Initialize headless WebView | `DesktopWebView(config, headless=true)` | WebView initialized without window | High |
| Load URL headless | `webView.loadUrl("https://example.com")` | Page loads, no visual display | High |
| Execute JavaScript | `webView.evaluateJavaScript("document.title")` | Returns page title | High |
| Extract HTML | `webView.getPageHtml()` | Returns full HTML source | High |
| Navigate back/forward | `webView.goBack()`, `webView.goForward()` | Navigation works without UI | Medium |
| Handle cookies | `webView.setCookie()`, `webView.getCookies()` | Cookie management functional | Medium |
| Screenshot capture | `webView.captureScreenshot()` | Bitmap generated without display | Low |
| Dispose WebView | `webView.dispose()` | Resources cleaned up | High |

**Configuration:**
```kotlin
val config = WebViewConfig(
    initialUrl = "https://example.com",
    javaScriptEnabled = true,
    domStorageEnabled = true,
    headless = true // Enable headless mode
)
val webView = DesktopWebView(config)
```

**Test Example:**
```kotlin
@Test
fun testHeadlessMode() = runTest {
    val config = WebViewConfig(
        initialUrl = "https://www.example.com",
        javaScriptEnabled = true,
        headless = true
    )
    val webView = DesktopWebView(config)

    // Wait for page load
    webView.isLoading.first { !it }

    // Verify loaded
    assertEquals("https://www.example.com", webView.currentUrl.value)

    webView.dispose()
}
```

### Android Headless Testing

**Platform:** Android

**Note:** Android WebView requires Activity context, true headless not supported. Use instrumentation tests instead.

**Test Approach:**
- Use Android instrumented tests with hidden Activity
- Set WebView visibility to GONE
- Test in background Service

---

## APIs and Intents

### Voice Command API

**File:** `WebAvanueActionMapper.kt`

**Command Categories:**

#### 1. Scrolling Commands

| Command ID | Parameters | Action | Example |
|------------|-----------|--------|---------|
| `SCROLL_UP` | None | Scroll up one page | "Scroll up" |
| `SCROLL_DOWN` | None | Scroll down one page | "Scroll down" |
| `SCROLL_LEFT` | None | Scroll left | "Scroll left" |
| `SCROLL_RIGHT` | None | Scroll right | "Scroll right" |
| `SCROLL_TOP` | None | Jump to top of page | "Go to top" |
| `SCROLL_BOTTOM` | None | Jump to bottom of page | "Go to bottom" |

#### 2. Navigation Commands

| Command ID | Parameters | Action | Example |
|------------|-----------|--------|---------|
| `GO_BACK` | None | Navigate back in history | "Go back" |
| `GO_FORWARD` | None | Navigate forward in history | "Go forward" |
| `NAVIGATE_FORWARD` | None | Alias for GO_FORWARD | "Navigate forward" |
| `RELOAD_PAGE` | None | Reload current page | "Reload page" |
| `ACTION_REFRESH` | None | Alias for RELOAD_PAGE | "Refresh" |

#### 3. Zoom Commands

| Command ID | Parameters | Action | Example |
|------------|-----------|--------|---------|
| `ZOOM_IN` | None | Increase zoom by 10% | "Zoom in" |
| `ZOOM_OUT` | None | Decrease zoom by 10% | "Zoom out" |
| `PINCH_OPEN` | None | Alias for ZOOM_IN | "Pinch open" |
| `PINCH_CLOSE` | None | Alias for ZOOM_OUT | "Pinch close" |
| `RESET_ZOOM` | None | Reset zoom to 100% | "Reset zoom" |
| `SET_ZOOM_LEVEL` | `level: Int` (50-200) | Set specific zoom percentage | "Set zoom to 150" |

#### 4. Desktop Mode Commands

| Command ID | Parameters | Action | Example |
|------------|-----------|--------|---------|
| `DESKTOP_MODE` | None | Enable desktop user agent | "Enable desktop mode" |
| `MOBILE_MODE` | None | Enable mobile user agent | "Enable mobile mode" |

#### 5. Page Control Commands

| Command ID | Parameters | Action | Example |
|------------|-----------|--------|---------|
| `FREEZE_PAGE` | None | Toggle page freeze (stop JS) | "Freeze page" |
| `CLEAR_COOKIES` | None | Clear all cookies | "Clear cookies" |

#### 6. Tab Commands

| Command ID | Parameters | Action | Example |
|------------|-----------|--------|---------|
| `NEW_TAB` | None | Create new blank tab | "New tab" |
| `CLOSE_TAB` | None | Close active tab | "Close tab" |

#### 7. Bookmark Commands

| Command ID | Parameters | Action | Example |
|------------|-----------|--------|---------|
| `ADD_BOOKMARK` | None | Bookmark current page | "Add bookmark" |

#### 8. Gesture Commands (Legacy)

| Command ID | Parameters | Action | Example |
|------------|-----------|--------|---------|
| `SINGLE_CLICK` | None | Perform click at center | "Click" |
| `DOUBLE_CLICK` | None | Perform double-click | "Double click" |
| `DRAG_START` | None | Begin drag gesture | "Start drag" |
| `DRAG_STOP` | None | End drag gesture | "Stop drag" |
| `SELECT` | None | Select text/element | "Select" |

#### 9. Universal Gesture Commands (IPC)

**Prefix:** `GESTURE_*`

All VoiceOS gesture types supported (80+ gestures).

**Parameters:**
- `x: Float` - X coordinate (-1 for center)
- `y: Float` - Y coordinate (-1 for center)
- `modifiers: Int` - Modifier keys bitmask

**Examples:**
- `GESTURE_SINGLE_TAP` with x=100, y=200
- `GESTURE_DOUBLE_TAP` with x=-1, y=-1 (center)
- `GESTURE_SWIPE_UP` with x=0, y=0, modifiers=0

### Intent Integration (Android)

**VoiceOS Integration:**

WebAvanue registers with VoiceOS IntentDispatcher on startup. Commands routed via:

1. VoiceOS loads commands from `assets/localization/commands/{locale}.json`
2. IntentDispatcher routes browser category commands to WebAvanueActionMapper
3. Mapper executes actions via TabViewModel/WebViewController

**Architecture Reference:**
- Developer Manual: Chapter 46
- ADR-007: Command Routing Architecture

### API Response Format

```kotlin
sealed class ActionResult {
    data class Success(val message: String) : ActionResult()
    data class Error(val message: String) : ActionResult()

    companion object {
        fun success(message: String = "Action completed") = Success(message)
        fun error(message: String) = Error(message)
    }
}
```

**Example Usage:**
```kotlin
val mapper = WebAvanueActionMapper(tabViewModel, webViewController)
val result = mapper.executeAction("ZOOM_IN")
when (result) {
    is ActionResult.Success -> println(result.message)
    is ActionResult.Error -> println("Error: ${result.message}")
}
```

---

## Test Procedures

### Manual Testing Checklist

#### Settings Test Suite (Complete)

**Time:** ~45 minutes
**Priority:** High

1. ☐ Search Suggestions (5 settings)
2. ☐ New Tab Page (6 options)
3. ☐ Appearance (10 settings)
4. ☐ Privacy & Security (13 settings)
5. ☐ Advanced (11 settings)
6. ☐ Downloads (3 settings)
7. ☐ Performance (5 settings)
8. ☐ Sync (5 settings)
9. ☐ Voice & AI (3 settings)
10. ☐ Command Bar (2 settings)
11. ☐ WebXR (7 settings)

**Total Settings:** 70 individual toggles/options

#### Voice Commands Test Suite

**Time:** ~30 minutes
**Priority:** High

1. ☐ Scrolling (6 commands)
2. ☐ Navigation (5 commands)
3. ☐ Zoom (6 commands)
4. ☐ Desktop/Mobile Mode (2 commands)
5. ☐ Page Control (2 commands)
6. ☐ Tabs (2 commands)
7. ☐ Bookmarks (1 command)
8. ☐ Gestures (5 legacy + 3 IPC examples)

**Total Commands:** ~32 voice commands

#### Headless Mode Test Suite

**Time:** ~15 minutes
**Priority:** Medium
**Platform:** Desktop only

1. ☐ Initialize headless WebView
2. ☐ Load URL without UI
3. ☐ Execute JavaScript
4. ☐ Extract HTML content
5. ☐ Navigate back/forward
6. ☐ Cookie management
7. ☐ Screenshot capture
8. ☐ Proper disposal

**Total Tests:** 8 headless scenarios

### Automated Testing

#### Unit Tests

**Location:** `universal/tests/domain/model/`

- `BrowserSettingsTest.kt` - Settings data model tests
- `FavoriteTest.kt` - Bookmark model tests
- `HistoryEntryTest.kt` - History model tests
- `TabTest.kt` - Tab model tests

#### Integration Tests

**Location:** `universal/src/androidTest/kotlin/`

- `BrowserBugFixesTest.kt` - Regression tests
- `SecurityFeaturesIntegrationTest.kt` - Security feature tests
- `WebXRSupportTest.kt` - WebXR API tests

#### Platform Tests

**Desktop:**
- `DesktopWebViewTest.kt` - Desktop WebView tests (includes headless)

**iOS:**
- `IOSWebViewTest.kt` - iOS WebView tests

**Android:**
- `AndroidWebViewTest.kt` - Android WebView tests

---

## Automation Scripts

### Bash Script: Complete Settings Test

```bash
#!/bin/bash
# test-all-settings.sh

echo "WebAvanue Settings Test Suite"
echo "=============================="

# Test Search Suggestions
echo "Testing Search Suggestions..."
# Add adb commands or automation framework calls

# Test New Tab Page
echo "Testing New Tab Page..."

# ... continue for all categories

echo "Test suite complete!"
```

### Kotlin Script: Voice Command Test

```kotlin
// VoiceCommandTestSuite.kt
class VoiceCommandTestSuite {
    private val mapper = WebAvanueActionMapper(tabViewModel, webViewController)

    suspend fun runAllTests() {
        testScrollingCommands()
        testNavigationCommands()
        testZoomCommands()
        testTabCommands()
        // ... etc
    }

    private suspend fun testScrollingCommands() {
        val commands = listOf(
            "SCROLL_UP", "SCROLL_DOWN", "SCROLL_LEFT",
            "SCROLL_RIGHT", "SCROLL_TOP", "SCROLL_BOTTOM"
        )
        commands.forEach { cmd ->
            val result = mapper.executeAction(cmd)
            assert(result is ActionResult.Success)
        }
    }
}
```

### Python Script: Headless Testing

```python
#!/usr/bin/env python3
# headless_test.py

import subprocess
import json

def run_headless_test(url):
    """Run headless browser test on given URL"""
    result = subprocess.run([
        "./gradlew",
        ":Modules:WebAvanue:coredata:desktopTest",
        "--tests", "DesktopWebViewTest.testHeadlessMode"
    ], capture_output=True, text=True)

    return result.returncode == 0

if __name__ == "__main__":
    urls = [
        "https://www.google.com",
        "https://www.example.com",
        "https://www.wikipedia.org"
    ]

    for url in urls:
        print(f"Testing {url}...")
        success = run_headless_test(url)
        print(f"{'✓' if success else '✗'} {url}")
```

---

## Test Coverage Summary

| Category | Manual Tests | Automated Tests | Coverage |
|----------|-------------|-----------------|----------|
| Settings | 70 | 4 unit tests | ~15% |
| Voice Commands | 32 | 0 | 0% |
| Headless Mode | 8 | 3 platform tests | ~40% |
| WebXR | 7 | 1 integration test | ~20% |
| Privacy/Security | 13 | 1 integration test | ~10% |

**Total Test Count:** 130 manual test cases
**Automated Coverage:** ~15% overall

**Recommendations:**
1. Increase automated test coverage to 70%+
2. Add end-to-end tests for critical user flows
3. Implement CI/CD test automation
4. Create visual regression tests for UI changes

---

## Appendix

### Settings Data Model Reference

```kotlin
data class BrowserSettings(
    // Display (8)
    val theme: Theme,
    val fontSize: FontSize,
    val forceZoom: Boolean,
    val showImages: Boolean,
    val useDesktopMode: Boolean,
    val initialScale: Float,
    val desktopModeDefaultZoom: Int,
    val desktopModeWindowWidth: Int,
    val desktopModeWindowHeight: Int,
    val desktopModeAutoFitZoom: Boolean,

    // Privacy (10)
    val blockPopups: Boolean,
    val blockAds: Boolean,
    val blockTrackers: Boolean,
    val doNotTrack: Boolean,
    val clearCacheOnExit: Boolean,
    val clearHistoryOnExit: Boolean,
    val clearCookiesOnExit: Boolean,
    val enableCookies: Boolean,
    val enableJavaScript: Boolean,
    val enableWebRTC: Boolean,

    // Search (3)
    val defaultSearchEngine: SearchEngine,
    val searchSuggestions: Boolean,
    val voiceSearch: Boolean,

    // Navigation (4)
    val homePage: String,
    val newTabPage: NewTabPage,
    val restoreTabsOnStartup: Boolean,
    val openLinksInBackground: Boolean,
    val openLinksInNewTab: Boolean,

    // Downloads (3)
    val downloadPath: String?,
    val askDownloadLocation: Boolean,
    val downloadOverWiFiOnly: Boolean,

    // Sync (5)
    val syncEnabled: Boolean,
    val syncBookmarks: Boolean,
    val syncHistory: Boolean,
    val syncPasswords: Boolean,
    val syncSettings: Boolean,

    // Advanced (5)
    val hardwareAcceleration: Boolean,
    val preloadPages: Boolean,
    val dataSaver: Boolean,
    val autoPlay: AutoPlay,
    val textReflow: Boolean,

    // Voice & AI (4)
    val enableVoiceCommands: Boolean,
    val aiSummaries: Boolean,
    val aiTranslation: Boolean,
    val readAloud: Boolean,

    // UI State (4)
    val voiceDialogAutoClose: Boolean,
    val voiceDialogAutoCloseDelayMs: Long,
    val commandBarAutoHide: Boolean,
    val commandBarAutoHideDelayMs: Long,

    // WebXR (7)
    val enableWebXR: Boolean,
    val enableAR: Boolean,
    val enableVR: Boolean,
    val xrPerformanceMode: XRPerformanceMode,
    val xrAutoPauseTimeout: Int,
    val xrShowFPSIndicator: Boolean,
    val xrRequireWiFi: Boolean
)
```

**Total:** 70 settings

### Test Environment Setup

**Required:**
- Android SDK 34+
- Kotlin 2.1.0+
- Gradle 8.9+
- JDK 17+

**Optional:**
- AR/XR capable device (for WebXR tests)
- Multiple devices (for sync tests)

### Reporting Issues

**Template:**
```markdown
## Issue Title

**Category:** Settings / Voice Commands / Headless / WebXR
**Priority:** Critical / High / Medium / Low
**Platform:** Android / iOS / Desktop

**Steps to Reproduce:**
1.
2.
3.

**Expected Result:**

**Actual Result:**

**Settings State:**
```json
{
  "enableJavaScript": true,
  "theme": "DARK"
}
```

**Logs:**
```
[Error output]
```
```

---

**Document End**

**Next Steps:**
1. Review and approve testing procedures
2. Implement automated test coverage
3. Schedule regular test runs
4. Update documentation as features evolve

**Maintained by:** WebAvanue Team
**Contact:** [Repository Issues](https://github.com/augmentalis/webavanue/issues)
