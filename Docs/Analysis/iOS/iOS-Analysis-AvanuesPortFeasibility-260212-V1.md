# iOS Avanues Port Feasibility Analysis

**Document:** iOS-Analysis-AvanuesPortFeasibility-260212-V1.md
**Date:** 2026-02-12
**Version:** V1
**Status:** RESEARCH COMPLETE

---

## Executive Summary

This analysis investigates the feasibility of porting the Avanues Android app (VoiceOSCore + WebAvanue) to iOS. The core finding: **iOS cannot replicate Android's system-wide accessibility scraping**, but **voice-controlled web browsing (WebAvanue) is 100% feasible** with excellent code reuse via KMP.

---

## 1. iOS Platform Capabilities vs Android

### What iOS CAN Do

| Capability | iOS API | Notes |
|------------|---------|-------|
| On-device speech recognition | `SFSpeechRecognizer` (iOS 10+) | 50+ languages, streaming, on-device since iOS 13 |
| Advanced voice commands | `SpeechAnalyzer` (iOS 26, 2026) | Purpose-built for command detection, faster |
| On-device NLU | `NaturalLanguage.framework` | Intent classification, entity extraction |
| WebView DOM scraping | `WKWebView` + JS injection | Identical capability to Android WebView |
| Voice overlay on web | `WKWebView` content overlay | Full floating button/label support |
| Custom voice intents | `App Intents` / `SiriKit` | Siri integration for app launch |
| Background audio | `AVAudioSession` | Recording category, limited background |
| KMP shared code | Kotlin/Native + Compose MP | Production stable since Nov 2023 |

### What iOS CANNOT Do (Hard Limits)

| Android Capability | iOS Equivalent | Status |
|-------------------|----------------|--------|
| `AccessibilityService` (read other apps' UI) | None | **IMPOSSIBLE** - iOS sandboxing |
| `TYPE_ACCESSIBILITY_OVERLAY` (draw over other apps) | None | **IMPOSSIBLE** - Apple only |
| Foreground service (always-listening) | Limited background modes | **RESTRICTED** - no indefinite mic |
| `performGlobalAction()` (system gestures) | None | **IMPOSSIBLE** - Apple only |
| App enumeration (`PackageManager`) | None | **IMPOSSIBLE** - sandbox |
| Screen scraping of third-party apps | None | **IMPOSSIBLE** - Apple blocks |

### Critical iOS Limitation Explained

Apple's security model fundamentally prevents third-party apps from:
1. Reading the accessibility tree of OTHER apps (only your own app's UI)
2. Drawing overlays on top of other apps
3. Injecting gestures into other apps
4. Running indefinitely in background listening for voice

Only Apple's built-in assistive technologies (VoiceOver, Voice Control, Switch Control) have these capabilities. There is no API, entitlement, or workaround to achieve this.

---

## 2. Current Android Architecture Portability

### Modules Already KMP-Ready for iOS

| Module | commonMain | iosMain | Status |
|--------|-----------|---------|--------|
| VoiceOSCore | 275+ files | 5 stubs | iOS targets declared, stubs only |
| Database (SQLDelight) | 20+ .sq files | Driver exists | Ready — just enable target |
| DeviceManager | Full abstractions | Implementation exists | Ready to use |
| Foundation | Full utilities | Ready | Ready to use |
| AVID | ID system | Ready | Ready to use |
| AvanueUI | Design tokens | Compose-only | Tokens portable, components need SwiftUI |
| SpeechRecognition | Abstractions | Stubs | Needs iOS Speech.framework bridge |
| WebAvanue | 150+ files (95%) | Commented out | Phase 2 — needs WKWebView wrapper |
| Localization | DataStore + JSON | Ready | Ready via KMP |
| AVU | Codec + DSL | Ready | Ready to use |

### What Transfers Directly (No Changes)

- Command parsing system (StaticCommandRegistry, CommandWordDetector)
- VOS file format (.app.vos, .web.vos) — parsing, import/export
- Multi-locale support (5 locales: en, es, fr, de, hi)
- Database schema and repositories (SQLDelight)
- Element models, fingerprinting, deduplication
- Synonym system (SynonymParser, SynonymBinaryFormat)
- SFTP sync protocol (VosSftpClient concept — needs iOS networking)

### What Must Be Reimplemented for iOS

| Component | Android Impl | iOS Impl Needed |
|-----------|-------------|-----------------|
| Screen scraping | AccessibilityNodeInfo tree | N/A (impossible for other apps) |
| Web scraping | DOMScraperBridge.js in WebView | DOMScraperBridge.js in WKWebView (SAME JS) |
| Gesture dispatch | AccessibilityService.performGlobalAction | WKWebView.evaluateJavaScript (web only) |
| Speech engine | Vivoka, Google STT, Whisper | SFSpeechRecognizer / SpeechAnalyzer |
| App UI | Jetpack Compose | SwiftUI (or Compose MP for simpler screens) |
| Overlays | TYPE_ACCESSIBILITY_OVERLAY | In-app floating views only |
| Background listening | Foreground service | AVAudioSession (limited) |
| Credential storage | EncryptedSharedPreferences | iOS Keychain Services |

---

## 3. Recommended iOS App: "Avanues for iOS"

### Scope: Voice-Controlled Web Browser + VOS Command System

**Feature Matrix:**

| Feature | Feasibility | Implementation |
|---------|-------------|----------------|
| Voice-controlled web browsing | FULL | WKWebView + JS injection + Speech.framework |
| DOM scraping + voice targets | FULL | Reuse DOMScraperBridge.js verbatim |
| VOS command system | FULL | KMP shared (StaticCommandRegistry) |
| Multi-locale support | FULL | KMP shared (5 locales) |
| VOS file loading (.web.vos) | FULL | KMP shared (VosParser) |
| SFTP VOS sync | FULL | Port to iOS networking (URLSession) |
| Bookmarks, history, tabs | FULL | KMP shared (BrowserRepository) |
| Reading mode | FULL | KMP shared (ReadingModeExtractor) |
| Download management | FULL | iOS-specific (URLSession) |
| Private browsing | FULL | KMP shared (PrivateBrowsingManager) |
| 4-Tier voice (Tier 1,2,4) | PARTIAL | No Tier 3 (app scraping impossible) |
| Always-listening | NO | iOS restricts background mic |
| System-wide voice control | NO | iOS sandbox prevents this |
| App scraping (Tier 3) | NO | iOS sandbox prevents this |
| Cursor control of other apps | NO | iOS sandbox prevents this |

### Architecture

```
Apps/iOS/Avanues/
├── AvanuesApp.swift              -- SwiftUI app entry
├── ContentView.swift             -- Navigation (AvanueMode)
├── Views/
│   ├── BrowserView.swift         -- WKWebView + voice overlay
│   ├── VoiceCommandBar.swift     -- Floating mic button + results
│   ├── SettingsView.swift        -- Theme, voice, sync settings
│   ├── VosSyncView.swift         -- SFTP sync management
│   └── AboutView.swift           -- Credits, version
├── Services/
│   ├── SpeechRecognizerService.swift  -- iOS Speech.framework wrapper
│   ├── DOMScraperBridge.swift    -- WKWebView JS injection
│   ├── VoiceCommandRouter.swift  -- Routes commands to actions
│   └── SFTPSyncService.swift     -- VOS sync via URLSession
├── Bridge/
│   └── KMPBridge.swift           -- Kotlin shared module integration
└── Resources/
    ├── DOMScraperBridge.js       -- Identical to Android version
    └── VOS/                      -- Bundled .web.vos files
```

### KMP Integration (Shared Kotlin → iOS)

```
Modules/VoiceOSCore/src/iosMain/
├── IosSpeechEngine.kt           -- SFSpeechRecognizer wrapper via expect/actual
├── IosVoiceCommandHandler.kt    -- iOS-specific command routing
└── IosCredentialStore.kt        -- Keychain wrapper for SFTP creds

Modules/WebAvanue/src/iosMain/
├── WKWebViewWrapper.kt          -- WKWebView Compose/SwiftUI integration
├── IosWebViewController.kt      -- Tab management, navigation
├── IosNetworkStatusMonitor.kt   -- NWPathMonitor
└── IosCertificateHandler.kt     -- SSL pinning for WKWebView
```

---

## 4. iOS Speech Recognition Deep Dive

### Option A: SFSpeechRecognizer (Available Now, iOS 13+)

```swift
// On-device, streaming, 50+ languages
let recognizer = SFSpeechRecognizer(locale: Locale(identifier: "en-US"))
let request = SFSpeechAudioBufferRecognitionRequest()
request.requiresOnDeviceRecognition = true // Privacy-first

recognizer.recognitionTask(with: request) { result, error in
    if let result = result {
        let command = result.bestTranscription.formattedString
        // Route to KMP VoiceCommandInterpreter
    }
}
```

**Capabilities:**
- On-device (no internet required) since iOS 13
- Streaming real-time results
- 50+ languages (matches our 5 locales)
- 1000 recognition requests per hour per device
- Works while app is in foreground

### Option B: SpeechAnalyzer (iOS 26, 2026 — New)

```swift
// Purpose-built for voice commands, faster
let analyzer = SpeechAnalyzer(locale: .init(identifier: "en_US"))
let config = SpeechAnalyzer.Configuration()

for try await event in analyzer.start(configuration: config) {
    switch event {
    case .speechRecognized(let result):
        let command = result.bestTranscription
        // Route to KMP
    }
}
```

**Advantages over SFSpeechRecognizer:**
- Optimized for voice commands (not dictation)
- Faster response time
- Long-form audio support
- Better noise handling

### Recommendation: Start with SFSpeechRecognizer, migrate to SpeechAnalyzer when iOS 26 is baseline.

---

## 5. WKWebView Voice Control (The Core Feature)

### How It Works (Identical Pattern to Android)

1. **User opens web page** in WKWebView
2. **DOMScraperBridge.js** injected via `WKUserScript`
3. **JS scrapes interactive elements** (links, buttons, inputs, etc.)
4. **Elements sent to Swift** via `WKScriptMessageHandler`
5. **Voice overlay rendered** on top of WKWebView (numbered labels)
6. **User speaks command** ("click 3", "scroll down", "go back")
7. **SFSpeechRecognizer** captures text
8. **KMP CommandWordDetector** matches command
9. **JS executed** via `webView.evaluateJavaScript()` to perform action

### Key Advantage
The DOMScraperBridge.js is **platform-agnostic**. The same JavaScript that scrapes DOM on Android WebView works identically on iOS WKWebView. This is the highest-value code reuse.

---

## 6. Implementation Phases

### Phase 1: WebAvanue iOS (4-6 weeks)

**Goal:** Voice-controlled web browser on iOS

| Week | Deliverable |
|------|-------------|
| 1 | Xcode project setup, KMP integration, basic SwiftUI shell |
| 2 | WKWebView + DOMScraperBridge.js + voice overlay |
| 3 | SFSpeechRecognizer + KMP command routing |
| 4 | Tab management, bookmarks, history (KMP shared) |
| 5 | VOS file loading, multi-locale, settings |
| 6 | Polish, testing, App Store submission |

**Code Reuse Estimate:** ~60% (all commonMain KMP + identical JS)

### Phase 2: Enhanced Voice Features (2-3 weeks)

| Feature | Implementation |
|---------|---------------|
| VOS SFTP sync | Port sync protocol to iOS URLSession |
| Siri Shortcuts | App Intents for "Open [site] in Avanues" |
| Reading mode voice | TTS via AVSpeechSynthesizer |
| Voice search | Speech → search query → navigate |
| Accessibility labels | Full VoiceOver compatibility |

### Phase 3: In-App Voice Control (2-3 weeks)

| Feature | Implementation |
|---------|---------------|
| Voice control of Avanues settings | UIAccessibility tree of own app |
| Voice navigation between screens | AvanueMode routing via voice |
| Form filling via voice | JS injection for web forms |
| Voice bookmarking | "Bookmark this page" command |

### Phase 4: Advanced (Future, Requires iOS 26+)

| Feature | Implementation |
|---------|---------------|
| SpeechAnalyzer migration | Faster voice command detection |
| Safari Web Extension | Voice control in Safari (complex architecture) |
| Live Activities | Voice status in Dynamic Island |
| WidgetKit | Quick voice actions from home screen |

---

## 7. App Store Considerations

### Category: Utilities > Accessibility

### Required Privacy Descriptions (Info.plist)
- `NSMicrophoneUsageDescription` — "Used for voice commands to control web browsing"
- `NSSpeechRecognitionUsageDescription` — "Used to convert your speech to text for voice commands"

### App Store Review Risks
- **LOW RISK:** Voice-controlled browser is a legitimate accessibility app
- **MEDIUM RISK:** Must clearly explain accessibility purpose in review notes
- **NO RISK:** No private APIs, no entitlement abuse, no jailbreak required

### Competitive Landscape
- No major voice-controlled browser exists on iOS App Store
- Apple's built-in Voice Control works but is generic (not optimized for web)
- Opportunity to be first-to-market in this niche

---

## 8. Effort Estimate

| Component | Effort | Reuse |
|-----------|--------|-------|
| Xcode project + KMP setup | 3 days | N/A |
| SwiftUI navigation shell | 2 days | Partial (AvanueMode reuse) |
| WKWebView + DOMScraperBridge | 3 days | 90% (JS identical) |
| SFSpeechRecognizer integration | 2 days | N/A (iOS-specific) |
| KMP bridge (shared commands) | 2 days | 95% (commonMain) |
| Voice overlay UI | 3 days | 50% (concept, not code) |
| Tab management | 2 days | 80% (KMP shared) |
| Bookmarks/history | 1 day | 90% (SQLDelight) |
| Settings screen | 2 days | 60% (theme tokens port) |
| VOS file loading | 1 day | 95% (KMP shared) |
| SFTP sync | 3 days | 70% (protocol shared) |
| Testing + polish | 5 days | N/A |
| App Store submission | 2 days | N/A |
| **TOTAL** | **~30 working days** | **~65% avg reuse** |

---

## 9. Technology Decisions

| Decision | Recommendation | Rationale |
|----------|---------------|-----------|
| UI Framework | SwiftUI (not Compose MP) | Native iOS feel, better App Store review |
| Speech | SFSpeechRecognizer → SpeechAnalyzer | Stable now, upgrade later |
| Web engine | WKWebView | Only option on iOS (no Blink/Gecko) |
| Shared code | KMP (Kotlin/Native) | Already setup, 275+ shared files |
| Database | SQLDelight (native driver) | Already shared, iOS driver available |
| Networking | URLSession (iOS native) | Better than Ktor for iOS-specific needs |
| Credential storage | Keychain Services | iOS standard, AES-256 |
| Theme | SwiftUI Color + AvanueUI tokens | Port token values, not Compose components |
| CI/CD | Xcode Cloud or Fastlane | Standard iOS tooling |

---

## 10. Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| WKWebView JS limitations | LOW | HIGH | Test DOMScraperBridge.js early |
| KMP iOS compilation issues | MEDIUM | MEDIUM | Use stable KMP 2.1.0, test frequently |
| App Store rejection | LOW | HIGH | Clear accessibility purpose, no private APIs |
| SFSpeechRecognizer rate limits | LOW | MEDIUM | Cache results, batch processing |
| SwiftUI WKWebView integration | MEDIUM | MEDIUM | Use UIViewRepresentable wrapper |
| User expects Android parity | HIGH | MEDIUM | Clear messaging: "Web voice control" not "system voice control" |

---

## References

- iOS Speech Framework: developer.apple.com/documentation/speech
- WKWebView: developer.apple.com/documentation/webkit/wkwebview
- KMP iOS: kotlinlang.org/docs/multiplatform-mobile-getting-started.html
- Existing spec: `Docs/WebAvanue/MasterSpecs/WebAvanue-Spec-EnhancedWebView-51209-V1.md`
- KMP analysis: `Docs/Project/NewAvanues-KMP-Analysis-60101-V1.md`
- VoiceOSCoreNG phases 10-15: `Docs/VoiceOS/specifications/VoiceOS-Spec-Phases10-15-KMPMigration-60106-V1.md`
