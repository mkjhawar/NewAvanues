# iOS Plan: Avanues App with WebAvanue — 260212-V1

## Context

The Android Avanues app is a consolidated hub that launches sub-apps (WebAvanue, VoiceAvanue, VoiceCursor, etc.) from a dashboard. The iOS version must mirror this architecture: **Avanues is the entry point (hub/dashboard)**, **WebAvanue is the first launchable module**, and more modules will follow.

**Branch:** `IosVoiceOS-Development`
**iOS App Location:** `apps/iOS/Avanues/`
**KMP Modules Location:** `Modules/WebAvanue/src/iosMain/`, `Modules/SpeechRecognition/src/iosMain/`

---

## Android Architecture (Reference)

### Consolidated App Pattern
```
MainActivity (entry point)
  ↓ Detects launch mode via activity-alias:
  ↓   .VoiceAvanueAlias → AvanueMode.VOICE
  ↓   .WebAvanueAlias  → AvanueMode.BROWSER
  ↓   default          → AvanueMode.HUB
  ↓
NavHost (AvanueMode routing)
  ├── AvanueMode.HUB      → Hub Dashboard (app icons grid)
  ├── AvanueMode.BROWSER   → WebAvanue (BrowserApp composable)
  ├── AvanueMode.VOICE     → VoiceAvanue Home
  ├── AvanueMode.COMMANDS  → Voice Commands Manager
  ├── AvanueMode.SETTINGS  → UnifiedSettingsScreen
  ├── AvanueMode.ABOUT     → About Avanues
  ├── AvanueMode.VOS_SYNC  → VOS Sync Management
  └── AvanueMode.DEVELOPER_CONSOLE → Dev Tools
```

### WebAvanue Module (164 commonMain files, 40,391 lines)

| Layer | Files | Lines | Key Components |
|-------|-------|-------|----------------|
| UI Screens | 15 | 6,847 | BrowserScreen, AddressBar, TabSwitcher, SettingsScreen |
| ViewModels | 6 | 2,071 | TabViewModel, SettingsVM, SecurityVM, FavoriteVM, HistoryVM, DownloadVM |
| Repositories | 8 | 1,463 | Tab, Favorite, History, Download, Settings, Session, SitePermission |
| Dialogs | 18 | 4,232 | Auth, Bookmark, TabGroup, Security, VoiceCommands |
| Voice/Command | 7 | 2,626 | DOMScraperBridge, VoiceCommandGenerator, BrowserVoiceOSCallback |
| Theme/UI Utils | 24 | 2,100 | AppTheme, AppColors, WebAvanueColors |
| Models/Data | 35 | 2,500 | DOMElement, Tab, Favorite, History, Download models |
| Managers | 12 | 1,900 | SessionManager, DownloadQueue, PerformanceMonitor |
| RPC/Backend | 3 | 816 | AvuProtocol, WebAvanueService |
| Security | 24 | 2,000 | SecurityState, CertificateUtils |
| Expect/Actual | 10 | ~500 | WebView, Screenshot, FilePicker, Network, etc. |

### Voice → Browser Pipeline (10 Layers)
```
Speech Engine (Vivoka/AndroidSTT) → recognized phrase
  ↓ StaticCommandRegistry + CommandOrchestrator (command matching)
  ↓ ActionCoordinator (routes to handler by category)
  ↓ WebCommandHandler (extracts selector/xpath/actionType)
  ↓ IWebCommandExecutor interface (KMP decoupling)
  ↓ WebCommandExecutorImpl (builds JavaScript from WebAction)
  ↓ IJavaScriptExecutor (platform bridge)
  ↓ WebView.evaluateJavaScript(script) → DOM manipulation
  ↓ BrowserVoiceOSCallback.onDOMScraped() → generates commands
  ↓ Database persistence (ScrapedWebElement, GeneratedWebCommand)
```

### Android-Only Files (54 files, 9,926 lines) — Need iOS Equivalents
| Category | Android Files | Lines | iOS Equivalent Needed |
|----------|--------------|-------|----------------------|
| WebView wrapper | WebViewContainer.android.kt | 1,413 | WKWebView UIViewRepresentable |
| WebView config | WebViewConfigurator.kt | 772 | WKWebViewConfiguration |
| JS bridge | WebAvanueVoiceOSBridge.kt | 277 | WKScriptMessageHandler |
| Download mgmt | WebAvanueDownloadManager.kt + queue | 840 | URLSession download tasks |
| Database driver | DatabaseDriver.kt | 269 | NativeSqliteDriver |
| Secure storage | SecureStorage.kt + Encryption | 424 | iOS Keychain |
| Network check | NetworkHelper.kt | 263 | NWPathMonitor |
| Certificate | CertificateUtils + Pinning | 343 | URLSession delegate |
| Screenshot | ScreenshotCapture.android.kt | ~150 | WKWebView snapshot API |
| Settings apply | SettingsApplicator.kt | 291 | WKWebView preferences |
| File picker | FilePicker.android.kt | 195 | UIDocumentPicker |
| XR | AndroidXRManager + related | 382 | ARKit (partial) |
| RPC server | WebAvanueJsonRpcServer.kt | 319 | iOS stub (no local RPC needed) |
| Lifecycle | WebViewLifecycle.kt | 326 | SwiftUI lifecycle |
| Memory | MemoryMonitor.kt | 215 | os_proc_available_memory |
| Context menu | ContextMenuHandler.kt | 186 | WKWebView context menu |
| JS security | SecureScriptLoader.kt | 332 | Bundle signature check |

---

## iOS Implementation Plan

### Phase 1: Avanues Hub Dashboard

**Goal:** Replicate the Android hub dashboard as the iOS app entry point with launchable sub-app icons.

#### 1.1 HubView.swift (NEW)
**Location:** `apps/iOS/Avanues/Avanues/Hub/HubView.swift`

Replaces the current placeholder. Grid of app icons that navigate to sub-apps:
- **WebAvanue** icon (globe) → BrowserView
- **VoiceAvanue** icon (mic) → placeholder (future)
- **Voice Commands** icon (list) → placeholder (future)
- **VOS Sync** icon (cloud) → placeholder (future)
- **Settings** icon (gear) → SettingsView
- **About** icon (info) → AboutView (already implemented)

Design:
- LazyVGrid with adaptive columns (2 on iPhone, 3 on iPad)
- Each icon: rounded rectangle with SF Symbol + label
- AvanueUI theme v5.1 colors (map ColorPalette → SwiftUI Color)
- SpatialVoice design language (glass/water effect backgrounds)
- "Avanues" branding header with version

#### 1.2 Update ContentView.swift
**Location:** `apps/iOS/Avanues/Avanues/ContentView.swift`

- Replace current NavigationStack with HubView as root
- Navigation: `NavigationStack` with `navigationDestination(for: AvanueMode)`
- Each mode navigates to its view
- Tab bar or sidebar for iPad (adaptive layout)

#### 1.3 AvanueMode.swift (NEW)
**Location:** `apps/iOS/Avanues/Avanues/Navigation/AvanueMode.swift`

Mirror the Android enum:
```swift
enum AvanueMode: String, Hashable {
    case hub, browser, voice, commands, settings, about, vosSyncManagement
    case developerConsole, developerSettings

    var label: String { ... }
    var icon: String { ... }  // SF Symbol name
}
```

### Phase 2: WebAvanue Browser — KMP Expect/Actual Implementations

**Goal:** Implement all 10 expect/actual declarations so the 164 commonMain files compile and run on iOS.

#### 2.1 IOSWebView.kt (COMPLETE — currently 50% done)
**Location:** `Modules/WebAvanue/src/iosMain/kotlin/com/augmentalis/webavanue/IOSWebView.kt`

Complete the existing stub to full production WKWebView wrapper:
- `WKWebView` creation with `WKWebViewConfiguration`
- `WKNavigationDelegate` for load lifecycle (didStart, didFinish, didFail, decidePolicyFor)
- `WKUIDelegate` for JavaScript alerts/confirms/prompts
- `WKScriptMessageHandler` for JS→Kotlin callbacks
- KVO observers: `estimatedProgress`, `URL`, `title`, `canGoBack`, `canGoForward`, `isLoading`
- StateFlow exposure: `currentUrl`, `pageTitle`, `loadingProgress`, `isLoading`, `canGoBack`, `canGoForward`
- Methods: `loadUrl()`, `reload()`, `goBack()`, `goForward()`, `evaluateJavaScript()`, `findInPage()`, `captureScreenshot()`
- Cookie management via `WKHTTPCookieStore`
- Private browsing via `WKWebViewConfiguration.websiteDataStore = .nonPersistent()`
- User agent customization for desktop mode

#### 2.2 IOSJavaScriptExecutor.kt (NEW)
**Location:** `Modules/WebAvanue/src/iosMain/kotlin/com/augmentalis/webavanue/IOSJavaScriptExecutor.kt`

Actual implementation of `IJavaScriptExecutor`:
```kotlin
actual class IOSJavaScriptExecutor(private val webView: WKWebView) : IJavaScriptExecutor {
    actual override suspend fun evaluateJavaScript(script: String): String? {
        return suspendCoroutine { cont ->
            webView.evaluateJavaScript(script) { result, error ->
                cont.resume(result?.toString())
            }
        }
    }
}
```

#### 2.3 IOSScreenshotCapture.kt (NEW)
**Location:** `Modules/WebAvanue/src/iosMain/kotlin/com/augmentalis/webavanue/IOSScreenshotCapture.kt`

Using WKWebView's `takeSnapshot(with:completionHandler:)` API.

#### 2.4 IOSFilePicker.kt (NEW)
**Location:** `Modules/WebAvanue/src/iosMain/kotlin/com/augmentalis/webavanue/IOSFilePicker.kt`

Using `UIDocumentPickerViewController` via Kotlin/Native interop.

#### 2.5 IOSNetworkChecker.kt (NEW)
**Location:** `Modules/WebAvanue/src/iosMain/kotlin/com/augmentalis/webavanue/IOSNetworkChecker.kt`

Using `NWPathMonitor` from `Network.framework`.

#### 2.6 IOSDatabaseDriver.kt (NEW)
**Location:** `Modules/WebAvanue/src/iosMain/kotlin/com/augmentalis/webavanue/IOSDatabaseDriver.kt`

SQLDelight `NativeSqliteDriver` for `BrowserDatabase.sq` (9 tables: tab, tab_group, favorite, favorite_tag, favorite_folder, history_entry, download, browser_settings, site_permission).

#### 2.7 IOSSecureStorage.kt (NEW)
**Location:** `Modules/WebAvanue/src/iosMain/kotlin/com/augmentalis/webavanue/IOSSecureStorage.kt`

iOS Keychain via `Security.framework` for sensitive data (passwords, certificates).

#### 2.8 IOSWebViewEngine.kt, IOSThemeConfig.kt, IOSVoiceCommandService.kt (NEW)
Remaining expect/actual implementations — relatively thin wrappers.

#### 2.9 build.gradle.kts Changes
Enable iOS targets in `Modules/WebAvanue/build.gradle.kts`:
- Uncomment iosX64, iosArm64, iosSimulatorArm64
- Add iosMain source set with dependencies:
  - `app.cash.sqldelight:native-driver`
  - `io.ktor:ktor-client-darwin`
  - `org.jetbrains.compose.runtime:runtime`

### Phase 3: WebAvanue Browser — SwiftUI Shell

**Goal:** Build the SwiftUI UI that wraps the KMP browser logic.

#### 3.1 BrowserView.swift (NEW)
**Location:** `apps/iOS/Avanues/Avanues/Browser/BrowserView.swift`

SwiftUI view hosting WKWebView:
- `UIViewRepresentable` wrapper for WKWebView (direct SwiftUI control, NOT KMP Compose)
- Address bar: TextField with URL input, domain display, lock icon for HTTPS
- Navigation toolbar: back, forward, reload/stop, share, tabs
- Loading progress bar (thin line at top, like Safari)
- Pull-to-refresh
- Tab management (TabSwitcherView equivalent)
- Bookmarks/history access
- Reading mode toggle
- Voice command status bar (shows active commands, mic indicator)

#### 3.2 WebViewCoordinator.swift (NEW)
**Location:** `apps/iOS/Avanues/Avanues/Browser/WebViewCoordinator.swift`

`NSObject` coordinator for WKWebView delegates:
- `WKNavigationDelegate` — page load lifecycle
- `WKUIDelegate` — JS alerts/confirms
- `WKScriptMessageHandler` — receives messages from injected JS
- Triggers DOM scraping on `webView:didFinishNavigation:`
- Manages JavaScript injection of DOMScraperBridge script

#### 3.3 DOMScraper.swift (NEW)
**Location:** `apps/iOS/Avanues/Avanues/Browser/DOMScraper.swift`

Orchestrates DOM scraping:
- Gets `DOMScraperBridge.SCRAPER_SCRIPT` from AvanuesShared framework
- Injects via `WKUserContentController.addUserScript()` at `.atDocumentEnd`
- Registers `WKScriptMessageHandler` for "VoiceOSBridge" messages
- Parses JSON results → Swift `DOMScrapeResult` / `DOMElement` structs
- 2-second cooldown between scrapes (matching Android)
- Structure hash comparison to skip unchanged DOMs
- Calls KMP `VoiceCommandGenerator` for command generation
- Feeds commands to KMP `BrowserVoiceOSCallback`

#### 3.4 ElementOverlayView.swift (NEW)
**Location:** `apps/iOS/Avanues/Avanues/Browser/ElementOverlayView.swift`

Number badges on interactive elements:
- SwiftUI overlay positioned on top of WKWebView
- Maps `ElementBounds` from JS → screen coordinates (accounting for scroll offset, zoom level)
- Small rounded badges with numbers (1, 2, 3...)
- Semi-transparent background using AvanueUI theme colors
- Toggle: "show numbers" / "hide numbers" voice command
- Auto-hide after successful voice command execution
- Performance: only render visible elements (viewport intersection)

#### 3.5 TabManager.swift (NEW)
**Location:** `apps/iOS/Avanues/Avanues/Browser/TabManager.swift`

Tab management using KMP `TabViewModel`:
- Multiple tabs with WKWebView instances
- Tab switcher grid (similar to Safari)
- Tab groups (mirrors Android tab_group table)
- Private browsing tabs (non-persistent data store)
- Tab state persistence via SQLDelight

#### 3.6 BookmarkView.swift, HistoryView.swift, DownloadView.swift (NEW)
**Location:** `apps/iOS/Avanues/Avanues/Browser/`

Standard browser auxiliary views, backed by KMP repositories:
- BookmarkView: grid/list of favorites from `FavoriteRepository`
- HistoryView: chronological list from `HistoryRepository`
- DownloadView: download queue from `DownloadRepository`

### Phase 4: AppleSpeechEngine — SFSpeechRecognizer Integration

**Goal:** Implement SFSpeechRecognizer as a KMP speech engine (same pattern as Vivoka on Android).

#### 4.1 AppleSpeechEngine.kt (NEW)
**Location:** `Modules/SpeechRecognition/src/iosMain/kotlin/com/augmentalis/speechrecognition/AppleSpeechEngine.kt`

SFSpeechRecognizer integration via Kotlin/Native:
```
SFSpeechRecognizer (native iOS, platform.Speech.*)
  + AVAudioEngine (microphone input, platform.AVFAudio.*)
  + SFSpeechAudioBufferRecognitionRequest (streaming)
  → Callback: onResult(text, confidence, isPartial, alternatives)
  → Feeds into IosSpeechRecognitionService.onRecognitionResult()
  → ResultProcessor pipeline (shared KMP)
  → SharedFlow<RecognitionResult> (consumed by app)
```

Key implementation details:
- `SFSpeechRecognizer(locale: NSLocale(localeIdentifier: language))`
- `requiresOnDeviceRecognition = true` for offline mode (iOS 16+)
- `AVAudioEngine.inputNode.installTap()` for microphone audio
- `SFSpeechAudioBufferRecognitionRequest` for streaming
- Partial results via `shouldReportPartialResults = true`
- Authorization: `SFSpeechRecognizer.requestAuthorization()`
- Permissions: `NSMicrophoneUsageDescription` + `NSSpeechRecognitionUsageDescription` (already in Info.plist)
- Error mapping: `SFSpeechRecognizerAuthorizationStatus` → `SpeechError`
- Language switching: create new `SFSpeechRecognizer` with different `NSLocale`

#### 4.2 Wire IosSpeechRecognitionService.kt (MODIFY)
**Location:** `Modules/SpeechRecognition/src/iosMain/kotlin/com/augmentalis/speechrecognition/IosSpeechRecognitionService.kt`

Replace TODO stubs with real AppleSpeechEngine integration:
- `initialize()` → create AppleSpeechEngine, request permissions
- `startListening()` → engine.startListening(mode)
- `stopListening()` → engine.stopListening()
- `setLanguage()` → engine.changeLocale(locale)
- Collect engine result/error flows → emit to service flows
- Wire through ResultProcessor for command matching

### Phase 5: Voice → Browser Integration

**Goal:** Connect the speech engine to the browser for voice-controlled browsing.

#### 5.1 CommandRouter.swift (NEW)
**Location:** `apps/iOS/Avanues/Avanues/Browser/CommandRouter.swift`

Routes recognized voice text to browser actions:
- Observes `IosSpeechRecognitionService.resultFlow` (via KMP framework)
- Uses KMP `VoiceCommandGenerator.findMatches(phrase)` for element matching
- Uses KMP `WebCommandExecutorImpl.executeWebAction(action)` for execution
- Handles numeric commands: "click 3" → element at index 3
- Handles navigation: "go back", "refresh", "go to apple.com"
- Handles scrolling: "scroll down", "scroll to top"
- Handles text: "type hello world", "select all", "copy"
- Handles gestures: "swipe left", "zoom in" (via gestures.js)
- Shows feedback: command text, success/failure indicator

#### 5.2 VoiceStatusBar.swift (NEW)
**Location:** `apps/iOS/Avanues/Avanues/Browser/VoiceStatusBar.swift`

UI feedback for voice state:
- Mic indicator (listening/processing/idle)
- Current command count ("42 voice commands available")
- Last executed command display
- Voice confidence indicator
- Error messages (permissions, timeout, etc.)

#### 5.3 VoiceToggle Integration
- Mic button in browser toolbar toggles listening
- Long-press for dictation mode
- Voice feedback sounds (optional)
- "Mute voice" / "wake up voice" commands (matching Android)

### Phase 6: Settings & Polish

#### 6.1 SettingsView.swift (NEW)
**Location:** `apps/iOS/Avanues/Avanues/Settings/SettingsView.swift`

Mirror the Android UnifiedSettingsScreen:
- Voice settings (locale, engine, confidence threshold)
- Browser settings (default search engine, JavaScript toggle, cookies)
- Theme settings (palette, style, appearance — 3-axis v5.1)
- Developer settings (VOS sync, debug overlays)
- About section

#### 6.2 AvanueUI Theme Bridge (Swift)
Map KMP AvanueUI v5.1 tokens to SwiftUI:
- `AvanueColorPalette` → SwiftUI `Color` sets
- `MaterialMode` → SwiftUI view modifiers (glass blur, water gradient, etc.)
- `AppearanceMode` → `@Environment(\.colorScheme)`

---

## Expect/Actual Implementation Checklist

| # | Expect Interface (commonMain) | Android Actual | iOS Actual Needed | Priority |
|---|-------------------------------|---------------|-------------------|----------|
| 1 | `WebView` (IOSWebView) | WebViewContainer.android.kt | IOSWebView.kt (50% done) | P0 |
| 2 | `IJavaScriptExecutor` | AndroidJavaScriptExecutor | IOSJavaScriptExecutor.kt | P0 |
| 3 | `ScreenshotCapture` | Android Bitmap | WKWebView.takeSnapshot | P2 |
| 4 | `FilePicker` | ActivityResultContract | UIDocumentPicker | P2 |
| 5 | `NetworkChecker` | ConnectivityManager | NWPathMonitor | P1 |
| 6 | `WebViewPoolManager` | Fragment pool | WKWebView pool | P1 |
| 7 | `WebViewEngine` | Android WebView | WKWebView wrapper | P0 |
| 8 | `ThemeConfig` | Compose theme | SwiftUI Environment | P1 |
| 9 | `VoiceCommandService` | VoiceOS bridge | KMP bridge | P0 |
| 10 | `BlurEffect` | RenderEffect | UIBlurEffect | P2 |

---

## Database Tables (All via SQLDelight — iOS gets NativeSqliteDriver)

### WebAvanue Browser Database (BrowserDatabase.sq)
| Table | Columns | Purpose |
|-------|---------|---------|
| tab | id, url, title, favicon, pinned, group_id, created_at | Tab state |
| tab_group | id, name, color, created_at | Tab groups |
| favorite | id, url, title, favicon, folder_id, position | Bookmarks |
| favorite_tag | favorite_id, tag | Bookmark tags |
| favorite_folder | id, name, parent_id, position | Bookmark folders |
| history_entry | id, url, title, visited_at, visit_count | Visit history |
| download | id, url, filename, size, progress, state, created_at | Downloads |
| browser_settings | key, value | 60+ config keys |
| site_permission | domain, permission_type, allowed | Per-site permissions |

### Web Scraping Database (shared)
| Table | Purpose |
|-------|---------|
| scraped_websites | URL metadata, structure hash, access tracking |
| scraped_web_elements | Element metadata (tag, xpath, text, bounds, clickable) |
| generated_web_commands | Derived voice commands per element |
| vos_file_registry | .web.vos file tracking for SFTP sync |

---

## Feature Parity Matrix

| Feature | Android | iOS Plan | Parity |
|---------|---------|----------|--------|
| Hub dashboard with app icons | ✅ | Phase 1 | 100% |
| WebAvanue browser | ✅ | Phase 3 | 95% |
| Address bar + navigation | ✅ | Phase 3.1 | 100% |
| Tab management + groups | ✅ | Phase 3.5 | 100% |
| Bookmarks + folders | ✅ | Phase 3.6 | 100% |
| History | ✅ | Phase 3.6 | 100% |
| Downloads | ✅ | Phase 3.6 | 85% |
| DOM scraping + element numbering | ✅ | Phase 3.3-3.4 | 95% |
| Voice commands (click/scroll/type) | ✅ | Phase 5.1 | 95% |
| 45+ gesture actions | ✅ | Via gestures.js | 100% |
| Speech recognition | Vivoka/AndroidSTT | SFSpeechRecognizer | 110% |
| Multi-locale (5→50+ languages) | ✅ | Phase 4.1 | 110% |
| Reading mode | ✅ | Phase 3 | 95% |
| Settings | ✅ | Phase 6.1 | 90% |
| AvanueUI theme v5.1 | ✅ | Phase 6.2 | 90% |
| VOS profiles (.web.vos) | ✅ | Via KMP | 100% |
| SFTP sync | ✅ | Via KMP | 95% |
| Find in page | ✅ | WKWebView API | 100% |
| Private browsing | ✅ | nonPersistentDataStore | 100% |
| Desktop mode | ✅ | User agent switch | 100% |
| Certificate pinning | ✅ | URLSession delegate | 95% |
| Cross-app scraping (Tier 3) | ✅ | ❌ IMPOSSIBLE | 0% |
| System cursor overlay | ✅ | ❌ IMPOSSIBLE | 0% |
| System controls (vol/wifi/bt) | ✅ | ⚠️ PARTIAL | 45% |

---

## New Files Summary

### Swift (apps/iOS/Avanues/Avanues/)
| File | Phase | Purpose |
|------|-------|---------|
| Hub/HubView.swift | 1 | Dashboard with app icons |
| Navigation/AvanueMode.swift | 1 | Navigation mode enum |
| Browser/BrowserView.swift | 3 | WKWebView + address bar + toolbar |
| Browser/WebViewCoordinator.swift | 3 | WKWebView delegate coordinator |
| Browser/DOMScraper.swift | 3 | JS injection + result parsing |
| Browser/ElementOverlayView.swift | 3 | Number badges on elements |
| Browser/TabManager.swift | 3 | Tab lifecycle management |
| Browser/BookmarkView.swift | 3 | Bookmarks UI |
| Browser/HistoryView.swift | 3 | History UI |
| Browser/DownloadView.swift | 3 | Downloads UI |
| Browser/CommandRouter.swift | 5 | Voice → browser action routing |
| Browser/VoiceStatusBar.swift | 5 | Voice feedback UI |
| Settings/SettingsView.swift | 6 | Unified settings |
| Theme/AvanueUIBridge.swift | 6 | KMP theme → SwiftUI colors |

### Kotlin (Modules/*/src/iosMain/)
| File | Phase | Purpose |
|------|-------|---------|
| WebAvanue/.../IOSWebView.kt | 2 | Complete WKWebView wrapper |
| WebAvanue/.../IOSJavaScriptExecutor.kt | 2 | evaluateJavaScript actual |
| WebAvanue/.../IOSScreenshotCapture.kt | 2 | WKWebView snapshot |
| WebAvanue/.../IOSFilePicker.kt | 2 | UIDocumentPicker |
| WebAvanue/.../IOSNetworkChecker.kt | 2 | NWPathMonitor |
| WebAvanue/.../IOSDatabaseDriver.kt | 2 | NativeSqliteDriver |
| WebAvanue/.../IOSSecureStorage.kt | 2 | iOS Keychain |
| WebAvanue/.../IOSWebViewEngine.kt | 2 | WebView engine wrapper |
| WebAvanue/.../IOSThemeConfig.kt | 2 | Theme bridge |
| WebAvanue/.../IOSVoiceCommandService.kt | 2 | Voice command bridge |
| SpeechRecognition/.../AppleSpeechEngine.kt | 4 | SFSpeechRecognizer engine |
| SpeechRecognition/.../IosSpeechRecognitionService.kt | 4 | Wire engine (MODIFY) |

### Build Config Changes
| File | Change |
|------|--------|
| Modules/WebAvanue/build.gradle.kts | Enable iOS targets, add iosMain source set |
| Modules/AvanuesShared/build.gradle.kts | Add WebAvanue export if not already |

---

## Execution Order

```
Phase 1 (Hub Dashboard)          ← Start here, testable immediately
  → 1.1 AvanueMode.swift
  → 1.2 HubView.swift
  → 1.3 Update ContentView.swift

Phase 2 (KMP Expect/Actual)      ← Foundation for browser
  → 2.1-2.9 All iOS actual implementations
  → 2.10 build.gradle.kts changes
  → VERIFY: ./gradlew :Modules:AvanuesShared:podspec builds

Phase 3 (Browser UI)             ← Visual browser, testable without voice
  → 3.1 BrowserView.swift + WebViewCoordinator.swift
  → 3.3 DOMScraper.swift (JS injection)
  → 3.4 ElementOverlayView.swift (number badges)
  → 3.5 TabManager.swift
  → 3.6 BookmarkView, HistoryView, DownloadView
  → VERIFY: Browse web, see numbered elements

Phase 4 (Speech Engine)          ← Voice recognition
  → 4.1 AppleSpeechEngine.kt
  → 4.2 Wire IosSpeechRecognitionService.kt
  → VERIFY: Mic button → speech recognized → text shown

Phase 5 (Voice → Browser)       ← The magic: "click 5" works
  → 5.1 CommandRouter.swift
  → 5.2 VoiceStatusBar.swift
  → VERIFY: Say "click 3" → element 3 clicked

Phase 6 (Settings & Polish)
  → 6.1 SettingsView.swift
  → 6.2 AvanueUI theme bridge
  → VERIFY: Change theme → UI updates
```

---

## Verification Plan

1. **Hub**: Launch app → see dashboard with WebAvanue icon → tap → browser opens
2. **Browser basics**: Enter URL → page loads → back/forward/reload work → tabs work
3. **DOM scraping**: Page loads → JS scraper runs → elements detected (check log count)
4. **Element overlay**: Tap "show numbers" → badges appear on interactive elements
5. **Voice recognition**: Tap mic → speak → recognized text appears
6. **Voice commands**: Say "click 3" → element 3 clicked; "scroll down" → page scrolls
7. **Multi-locale**: Switch to Spanish → "haz clic en tres" works
8. **Bookmarks**: Add bookmark → appears in bookmarks view → tap to navigate
9. **History**: Browse pages → history populated → search works
10. **Settings**: Change theme → app appearance updates; change search engine → works
11. **Private mode**: Open private tab → no history saved → data cleared on close
12. **KMP framework**: Verify AvanuesShared.framework exports WebAvanue module correctly
