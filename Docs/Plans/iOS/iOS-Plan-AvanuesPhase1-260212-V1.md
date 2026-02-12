# iOS Avanues Phase 1 — Implementation Plan

**Document:** iOS-Plan-AvanuesPhase1-260212-V1.md
**Date:** 2026-02-12
**Version:** V1
**Status:** DRAFT
**Branch:** IosVoiceOS-Development
**Spec:** iOS-Spec-AvanuesPhase1-260212-V1.md

---

## Implementation Summary

**Total effort:** ~35 working days (7 weeks)
**Code reuse:** ~80% via KMP shared modules
**Phases:** 7 sequential phases, some tasks parallelizable within phases

---

## Phase 0: Project Setup (3 days)

### 0.1 Create Xcode Project
- Create `Apps/iOS/Avanues/` directory structure
- Initialize Xcode project (SwiftUI App template)
- Bundle ID: `com.augmentalis.avanues.ios`
- Deployment target: iOS 16.0
- Supported devices: iPhone + iPad

### 0.2 KMP Integration
- Add KMP shared framework dependency via SPM or CocoaPods
- Configure Gradle to build iOS framework (`linkReleaseFrameworkIosArm64`)
- Install SKIE (Touchlab) for Swift interop
- Verify Kotlin/Native compilation for all shared modules
- Test basic KMP → Swift bridge (e.g., call a simple function)

### 0.3 Dependency Setup
- Add Koin 4.0+ for DI (replace Hilt)
- Add Decompose for navigation
- Configure SQLDelight NativeSqliteDriver
- Add Ktor Darwin engine for networking
- Add DataStore 1.1.0+ for settings

### 0.4 CI/CD Skeleton
- GitHub Actions workflow for iOS build
- Fastlane lane: build + test
- Code signing via Fastlane Match

### Files Created
```
Apps/iOS/Avanues/
├── Avanues.xcodeproj/
├── Avanues/
│   ├── AvanuesApp.swift
│   ├── Info.plist
│   ├── Assets.xcassets/
│   └── Preview Content/
├── AvanuesTests/
├── AvanuesUITests/
├── Fastfile
└── Matchfile

.github/workflows/ios-build.yml
```

### Dependencies to Add
```
// KMP Shared Module (via SPM or CocoaPods)
shared.framework (VoiceOSCore, WebAvanue, Database, NLU, AvanueUI, Foundation, AVID)

// Swift Packages
SKIE — co.touchlab:skie (via KMP gradle plugin)
Koin — io.insert-koin:koin-core (via KMP)
Decompose — com.arkivanov.decompose (via KMP)
```

### Acceptance
- [ ] `swift build` succeeds
- [ ] KMP shared module compiles for iOS
- [ ] Basic Swift → Kotlin bridge call works
- [ ] CI builds on GitHub Actions

---

## Phase 1: Core Browser (5 days)

### 1.1 WKWebView Integration (2 days)
- Create Compose MP `BrowserScreen` with `UIKitView` embedding WKWebView
- Implement `WKNavigationDelegate` for load progress, errors, redirects
- URL bar with voice input button
- Basic navigation: load URL, back, forward, refresh, stop
- Page title + favicon extraction

### 1.2 Tab Management (1.5 days)
- Reuse KMP `BrowserRepository` for tab state
- SwiftUI tab strip (horizontal scrolling)
- Open/close/switch tabs
- New tab page (search bar + favorites)

### 1.3 Bookmarks & History (1.5 days)
- Reuse KMP SQLDelight schemas (`BrowserTables.sq`)
- Bookmark add/remove/list
- History recording + search
- Favorites bar on new tab page

### Files Created/Modified
```
Avanues/
├── Screens/
│   ├── BrowserScreen.kt          (Compose MP — WKWebView + overlay)
│   └── NewTabScreen.kt           (Compose MP)
├── Views/
│   ├── TabStripView.swift         (SwiftUI)
│   ├── URLBarView.swift           (SwiftUI)
│   ├── BookmarkListView.swift     (SwiftUI)
│   └── HistoryListView.swift      (SwiftUI)
├── Bridge/
│   └── WKWebViewBridge.swift      (WKWebView delegate + JS)
└── ViewModels/
    └── BrowserViewModel.swift     (StateFlow → AsyncSequence via SKIE)

Modules/WebAvanue/src/iosMain/kotlin/
├── IosWebViewController.kt       (expect/actual — WKWebView controls)
├── IosNetworkStatusMonitor.kt     (NWPathMonitor)
└── IosCertificateHandler.kt       (SSL pinning)
```

### Acceptance
- [ ] Can navigate to any URL
- [ ] Back/forward/refresh work
- [ ] Tabs open/close/switch
- [ ] Bookmarks persist across app restart
- [ ] History records visited pages

---

## Phase 2: Voice Recognition (4 days)

### 2.1 SFSpeechRecognizer Integration (2 days)
- Create `IosSpeechRecognizerService` implementing KMP `SpeechEngine` interface
- On-device recognition (requiresOnDeviceRecognition = true)
- Streaming results (partial + final)
- 5 locale support: en-US, es-ES, fr-FR, de-DE, hi-IN
- Handle authorization flow (mic permission + speech permission)

### 2.2 Voice Command Bar UI (1 day)
- SwiftUI floating mic button (bottom-right, draggable)
- Listening state: idle → listening → processing → result
- Visual feedback: pulsing animation while listening
- Transcription text display
- Cancel gesture (swipe away)

### 2.3 KMP Command Routing (1 day)
- Wire SFSpeechRecognizer output → KMP `VoiceCommandInterpreter`
- KMP `StaticCommandRegistry` processes command
- Route to appropriate handler (scroll, click, navigate, etc.)
- Multi-locale: KMP `CommandLoader` loads locale-specific commands

### Files Created/Modified
```
Modules/SpeechRecognition/src/iosMain/kotlin/
└── IosSpeechRecognizerService.kt  (Complete SFSpeechRecognizer wrapper)

Modules/VoiceOSCore/src/iosMain/kotlin/
└── IosVoiceCommandHandler.kt      (Command routing for iOS)

Avanues/
├── Services/
│   └── SpeechService.swift         (Swift bridge to KMP speech)
├── Views/
│   └── VoiceCommandBarView.swift   (Floating mic + feedback)
└── ViewModels/
    └── VoiceViewModel.swift        (Manages listening state)
```

### Acceptance
- [ ] Mic button starts/stops listening
- [ ] Speech recognized on-device in all 5 locales
- [ ] "go to google.com" navigates browser
- [ ] "scroll down" scrolls page
- [ ] Visual feedback during listening
- [ ] Locale switch in settings takes effect immediately

---

## Phase 3: DOM Scraping + Voice Overlays (5 days)

### 3.1 DOMScraperBridge.js Injection (2 days)
- Inject DOMScraperBridge.js into WKWebView via `WKUserScript`
- JS scrapes interactive elements (links, buttons, inputs, selects, etc.)
- Elements sent to Swift via `WKScriptMessageHandler`
- Parse element data: bounds, text, type, attributes, AVID
- Handle page navigation (re-inject on new page load)
- Handle dynamic content (MutationObserver for SPAs)

### 3.2 Voice Overlay Rendering (2 days)
- Overlay numbered labels on each scraped element
- Labels positioned relative to WKWebView content offset
- Scroll-aware positioning (labels move with content)
- Configurable overlay style (size, color, opacity)
- Theme-aware: use AvanueUI color tokens for overlay styling
- Hide/show overlay on voice command

### 3.3 Element Interaction (1 day)
- "click N" → evaluate JS to click element N
- "type [text] in N" → fill input field N
- "select N" → activate select/dropdown
- "scroll to N" → scroll element N into view
- Element disambiguation when multiple match

### Files Created/Modified
```
Avanues/
├── Bridge/
│   ├── DOMScraperBridge.js        (COPY from WebAvanue — identical)
│   ├── DOMMessageHandler.swift    (WKScriptMessageHandler)
│   └── ElementActionExecutor.swift (JS execution for clicks/types)
├── Views/
│   └── VoiceOverlayView.swift     (Numbered label overlay)
└── ViewModels/
    └── ScrapingViewModel.swift     (Element state management)

Modules/WebAvanue/src/iosMain/kotlin/
└── IosWebViewScraperBridge.kt     (KMP bridge for scraped elements)
```

### Acceptance
- [ ] Elements scraped on page load and navigation
- [ ] Numbered overlays visible on interactive elements
- [ ] "click 5" clicks element #5
- [ ] "type hello in 3" fills input #3
- [ ] Overlays update on scroll and dynamic content
- [ ] Works on 95%+ of top-100 websites

---

## Phase 4: NLU + Semantic Commands (5 days)

### 4.1 CoreML Model Setup (2 days)
- Convert MobileBERT ONNX → CoreML (.mlpackage) using coremltools
- Bundle model in iOS app (or download on first launch)
- Complete `CoreMLModelManager.kt` iOS implementation (80% done)
- Implement BERT tokenizer for iOS (pure Kotlin or Swift bridge)
- Test inference on Apple Neural Engine

### 4.2 Intent Classification Pipeline (2 days)
- Wire CoreML inference → KMP `IntentClassifier`
- Define iOS intents: CLICK, SCROLL, NAVIGATE, SEARCH, READ, BOOKMARK, TAB, SETTINGS
- Entity extraction: numbers, URLs, text queries, element descriptions
- Confidence threshold: >= 0.7 execute, < 0.7 ask for clarification
- Fallback: if NLU fails, try literal command matching

### 4.3 Cloud LLM Integration (1 day)
- Wire KMP Ktor-based cloud providers (OpenAI, Claude) for iOS
- Send page context (scraped elements summary) + user utterance
- Parse structured command response
- Opt-in toggle in settings (privacy: "Send voice data to cloud AI")
- Graceful fallback when offline

### Files Created/Modified
```
Modules/AI/NLU/src/iosMain/kotlin/
├── BertTokenizer.kt               (Complete tokenizer impl)
├── IntentClassifier.kt             (Complete expect/actual)
├── ModelManager.kt                 (Download + cache management)
└── coreml/
    ├── CoreMLModelManager.kt       (Complete — 80% exists)
    └── CoreMLBackendSelector.kt    (ANE/GPU/CPU selection)

Modules/AI/LLM/build.gradle.kts     (Add iOS target)
Modules/AI/LLM/src/iosMain/kotlin/
└── IosLlmProvider.kt               (Ktor cloud API wrapper)

Avanues/
├── Resources/
│   └── Models/
│       └── mobilbert-intent.mlpackage  (Bundled CoreML model)
└── ViewModels/
    └── NLUViewModel.swift           (Intent routing)
```

### Model Conversion Script
```python
# scripts/convert_onnx_to_coreml.py
import coremltools as ct
import onnx

model = onnx.load("mobilebert-intent.onnx")
coreml_model = ct.converters.onnx.convert(model)
coreml_model.save("mobilebert-intent.mlpackage")
```

### Acceptance
- [ ] NLU classifies "search for running shoes" → SEARCH intent
- [ ] NLU classifies "go to the next page" → NAVIGATE intent
- [ ] Entity extraction: "click the blue button" → element="blue button"
- [ ] CoreML inference < 100ms on iPhone 13+
- [ ] Cloud LLM fallback works when NLU confidence < 0.7
- [ ] Works offline (NLU on-device, cloud gracefully unavailable)

---

## Phase 5: Safari Web Extension (5 days)

### 5.1 Extension Setup (1 day)
- Create Safari Web Extension target in Xcode
- manifest.json with permissions: nativeMessaging, activeTab, storage
- Content script: `<all_urls>`, run_at: document_end

### 5.2 Content Script (2 days)
- Port DOMScraperBridge.js for extension context
- Inject floating mic button (fixed position, bottom-right)
- Numbered voice overlays on interactive elements
- Command execution via JS (click, scroll, type, navigate)
- Message bridge to background.js

### 5.3 Native Messaging Bridge (1 day)
- background.js: relay messages between content.js and native app
- SafariWebExtensionHandler.swift: receive/send native messages
- Wire SFSpeechRecognizer for voice capture
- Handle background script lifecycle (non-persistent wake/sleep)

### 5.4 Popup & Settings (1 day)
- popup.html: extension settings (locale, overlay style, help)
- browser.storage for persisting extension preferences
- Help screen with voice command reference
- Link to open full Avanues app for advanced features

### Files Created
```
Avanues/
├── AvanuesSafariExtension/
│   ├── Info.plist
│   ├── SafariWebExtensionHandler.swift
│   ├── Resources/
│   │   ├── manifest.json
│   │   ├── background.js
│   │   ├── content.js          (DOMScraperBridge + mic button + overlays)
│   │   ├── content.css
│   │   ├── popup.html
│   │   ├── popup.js
│   │   ├── popup.css
│   │   └── images/
│   │       ├── icon-16.png
│   │       ├── icon-32.png
│   │       ├── icon-48.png
│   │       └── icon-128.png
│   └── AvanuesSafariExtension.entitlements
```

### Acceptance
- [ ] Extension appears in Settings → Safari → Extensions
- [ ] Floating mic button visible on Safari pages
- [ ] Tap mic → voice recognized → command executed
- [ ] "click 3" clicks element #3 in Safari
- [ ] "scroll down" scrolls page in Safari
- [ ] Popup shows settings and help
- [ ] Works on 90%+ of websites

---

## Phase 6: Settings + Theme (3 days)

### 6.1 Settings Screen (1.5 days)
- Reuse Compose MP `UnifiedSettingsScreen` where possible
- OR create SwiftUI equivalent with same structure
- Sections:
  - **Voice**: locale selection, sensitivity, always-show-overlay toggle
  - **Theme**: palette (SOL/LUNA/TERRA/HYDRA), style (Glass/Water/Cupertino/MountainView), appearance (Light/Dark/Auto)
  - **Browser**: default search engine, private browsing default, clear data
  - **AI**: NLU on/off, cloud LLM on/off, API key entry
  - **Safari Extension**: enable instructions, permissions guide
  - **About**: version, credits, "Designed and Created in California with Love."

### 6.2 Theme System Port (1 day)
- Port AvanueUI color tokens to SwiftUI `Color` extensions
- Map AvanueColorPalette values (SOL/LUNA/TERRA/HYDRA) to SwiftUI colors
- Map MaterialMode to SwiftUI styling (Glass blur, Water gradients, Cupertino minimal, MountainView standard)
- AppearanceMode: Light/Dark/Auto via `@Environment(\.colorScheme)`
- DataStore persistence for theme preferences

### 6.3 VOS File Management (0.5 days)
- File picker to load .web.vos files from Files app
- KMP VosParser processes loaded files
- Commands registered in StaticCommandRegistry
- List loaded VOS profiles in settings

### Files Created/Modified
```
Avanues/
├── Views/
│   ├── SettingsView.swift          (Main settings)
│   ├── ThemeSettingsView.swift     (Palette + style + appearance)
│   ├── VoiceSettingsView.swift     (Locale, sensitivity)
│   └── VosManagerView.swift        (VOS file management)
├── Theme/
│   ├── AvanueColors.swift          (Token port: Color extensions)
│   ├── AvanueTheme.swift           (Environment-based theme)
│   └── GlassModifier.swift         (Glass blur effect for SwiftUI)
└── ViewModels/
    └── SettingsViewModel.swift      (DataStore bridge)
```

### Acceptance
- [ ] All 4 palettes render correctly
- [ ] All 4 material styles apply (glass blur, water gradient, etc.)
- [ ] Light/Dark/Auto appearance works
- [ ] Locale change takes effect immediately
- [ ] VOS files load and register commands
- [ ] Settings persist across app restart

---

## Phase 7: Polish + App Store (5 days)

### 7.1 Onboarding Flow (1 day)
- 3-screen SwiftUI onboarding
  - Screen 1: "Voice-First Browsing" — feature overview
  - Screen 2: Mic + Speech permissions request
  - Screen 3: Quick tutorial (animated demo of voice commands)
- Skip option, show only on first launch

### 7.2 Accessibility (1 day)
- Full VoiceOver labels on all interactive elements
- Dynamic Type support (respect user font size)
- Reduce Motion support (disable animations)
- High Contrast support
- Accessibility audit with Xcode Accessibility Inspector

### 7.3 Testing (2 days)
- Unit tests: KMP shared logic (run on iOS simulator)
- UI tests: XCUITest for critical flows
  - App launch → navigate to URL → voice command → element click
  - Tab management
  - Settings changes
  - Safari extension activation
- Top-100 website compatibility testing (DOM scraping)
- Performance profiling with Instruments

### 7.4 App Store Submission (1 day)
- App Store Connect metadata (description, screenshots, keywords)
- Category: Utilities > Web Browser (primary), Accessibility (secondary)
- Privacy nutrition labels
- App review notes explaining accessibility purpose
- TestFlight beta distribution first

### Files Created
```
Avanues/
├── Onboarding/
│   ├── OnboardingView.swift
│   ├── PermissionRequestView.swift
│   └── TutorialView.swift
├── AvanuesTests/
│   ├── VoiceCommandTests.swift
│   ├── DOMScrapingTests.swift
│   └── NLUIntentTests.swift
└── AvanuesUITests/
    ├── BrowserFlowTests.swift
    ├── VoiceFlowTests.swift
    └── SettingsFlowTests.swift
```

### Acceptance
- [ ] Onboarding completes without crash
- [ ] VoiceOver navigates all screens
- [ ] Dynamic Type scales properly
- [ ] All unit tests pass
- [ ] All UI tests pass
- [ ] Top-20 websites scrape correctly
- [ ] TestFlight build distributed
- [ ] App Store submission accepted

---

## Timeline

```
Week 1: Phase 0 (Setup) + Phase 1 start (Browser)
Week 2: Phase 1 complete (Browser) + Phase 2 (Voice Recognition)
Week 3: Phase 2 complete + Phase 3 (DOM Scraping + Overlays)
Week 4: Phase 3 complete + Phase 4 start (NLU)
Week 5: Phase 4 complete (NLU + Cloud LLM) + Phase 5 start (Safari Extension)
Week 6: Phase 5 complete + Phase 6 (Settings + Theme)
Week 7: Phase 7 (Polish + Testing + App Store)
```

### Parallelization Opportunities
- Phase 4 (NLU model conversion) can start in Week 1 (Python script, independent)
- Phase 5 (Safari extension content.js) can start in Week 3 (reuses DOMScraperBridge)
- Phase 6 (theme token port) can start in Week 1 (independent of app)

### With Parallelization: ~5-6 weeks

---

## Risk Mitigation

| Risk | Mitigation | Contingency |
|------|-----------|-------------|
| KMP iOS compilation issues | Use stable KMP 2.1.0, test in Phase 0 | Fall back to Swift-only for problematic modules |
| WKWebView JS injection limits | Test DOMScraperBridge on top-100 sites early | Add site-specific workarounds |
| CoreML model conversion failures | Test conversion in Phase 0 | Use cloud NLU only |
| App Store rejection | Clear accessibility purpose, no private APIs | Address reviewer feedback, resubmit |
| Safari extension memory limit (6 MB) | Minimal JS, no heavy libraries | Reduce overlay complexity |
| SKIE incompatibility | Pin SKIE version, test early | Fall back to manual wrappers |

---

## File Inventory (All New Files)

### iOS App (Swift + SwiftUI)
```
Apps/iOS/Avanues/
├── Avanues.xcodeproj/
├── Avanues/
│   ├── AvanuesApp.swift
│   ├── Info.plist
│   ├── Assets.xcassets/
│   ├── Onboarding/
│   │   ├── OnboardingView.swift
│   │   ├── PermissionRequestView.swift
│   │   └── TutorialView.swift
│   ├── Screens/
│   │   ├── BrowserScreen.kt           (Compose MP)
│   │   └── NewTabScreen.kt            (Compose MP)
│   ├── Views/
│   │   ├── TabStripView.swift
│   │   ├── URLBarView.swift
│   │   ├── BookmarkListView.swift
│   │   ├── HistoryListView.swift
│   │   ├── VoiceCommandBarView.swift
│   │   ├── VoiceOverlayView.swift
│   │   ├── SettingsView.swift
│   │   ├── ThemeSettingsView.swift
│   │   ├── VoiceSettingsView.swift
│   │   └── VosManagerView.swift
│   ├── ViewModels/
│   │   ├── BrowserViewModel.swift
│   │   ├── VoiceViewModel.swift
│   │   ├── ScrapingViewModel.swift
│   │   ├── NLUViewModel.swift
│   │   └── SettingsViewModel.swift
│   ├── Services/
│   │   └── SpeechService.swift
│   ├── Bridge/
│   │   ├── WKWebViewBridge.swift
│   │   ├── DOMScraperBridge.js
│   │   ├── DOMMessageHandler.swift
│   │   └── ElementActionExecutor.swift
│   ├── Theme/
│   │   ├── AvanueColors.swift
│   │   ├── AvanueTheme.swift
│   │   └── GlassModifier.swift
│   └── Resources/
│       └── Models/
│           └── mobilebert-intent.mlpackage
├── AvanuesSafariExtension/
│   ├── Info.plist
│   ├── SafariWebExtensionHandler.swift
│   ├── AvanuesSafariExtension.entitlements
│   └── Resources/
│       ├── manifest.json
│       ├── background.js
│       ├── content.js
│       ├── content.css
│       ├── popup.html
│       ├── popup.js
│       ├── popup.css
│       └── images/ (icons)
├── AvanuesTests/
│   ├── VoiceCommandTests.swift
│   ├── DOMScrapingTests.swift
│   └── NLUIntentTests.swift
├── AvanuesUITests/
│   ├── BrowserFlowTests.swift
│   ├── VoiceFlowTests.swift
│   └── SettingsFlowTests.swift
├── Fastfile
└── Matchfile
```

### KMP Module Additions (iosMain)
```
Modules/VoiceOSCore/src/iosMain/kotlin/
├── IosVoiceCommandHandler.kt       (NEW)

Modules/WebAvanue/src/iosMain/kotlin/
├── IosWebViewController.kt         (NEW)
├── IosNetworkStatusMonitor.kt      (NEW)
├── IosCertificateHandler.kt        (NEW)
└── IosWebViewScraperBridge.kt      (NEW)

Modules/SpeechRecognition/src/iosMain/kotlin/
└── IosSpeechRecognizerService.kt    (COMPLETE existing stub)

Modules/AI/NLU/src/iosMain/kotlin/
├── BertTokenizer.kt                 (COMPLETE existing stub)
├── IntentClassifier.kt              (COMPLETE existing stub)
├── ModelManager.kt                  (COMPLETE existing stub)
└── coreml/
    ├── CoreMLModelManager.kt        (COMPLETE — 80% exists)
    └── CoreMLBackendSelector.kt     (COMPLETE existing stub)

Modules/AI/LLM/build.gradle.kts      (ADD iOS targets)
Modules/AI/LLM/src/iosMain/kotlin/
└── IosLlmProvider.kt                (NEW — cloud API wrapper)
```

### CI/CD
```
.github/workflows/ios-build.yml      (NEW)
scripts/convert_onnx_to_coreml.py    (NEW)
```

**Total new files:** ~55 Swift/SwiftUI + ~10 KMP iosMain + 8 Safari extension + 3 CI/tooling = **~76 files**

---

## Phase 2 Roadmap (Post-Launch)

After Phase 1 ships, the following features are planned:

| Feature | Effort | Priority |
|---------|--------|----------|
| RAG with cloud embeddings | 2-3 weeks | P1 |
| VOS SFTP sync | 1 week | P2 |
| Siri Shortcuts integration | 3 days | P2 |
| Reading mode + TTS | 1 week | P2 |
| Local RAG (CoreML embeddings) | 3 weeks | P3 |
| Local LLM (CoreML Llama) | 4 weeks | P3 |
| iPad split-view optimization | 1 week | P2 |
| Mac Catalyst port | 2 weeks | P3 |
| Apple Vision Pro XR colors | 1 week | P3 |
| SpeechAnalyzer migration (iOS 26) | 1 week | P2 |

---

## Dependencies Between Phases

```
Phase 0 (Setup)
    |
    +---> Phase 1 (Browser) ---> Phase 3 (DOM Scraping) ---> Phase 5 (Safari Ext)
    |                                     |
    +---> Phase 2 (Voice) ---------------+---> Phase 4 (NLU)
    |
    +---> Phase 6 (Settings/Theme) — can start anytime after Phase 0
    |
    +---> Phase 7 (Polish) — after all others complete
```
