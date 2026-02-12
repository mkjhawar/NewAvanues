# iOS Avanues Phase 1 — Specification

**Document:** iOS-Spec-AvanuesPhase1-260212-V1.md
**Date:** 2026-02-12
**Version:** V1
**Status:** DRAFT
**Branch:** IosVoiceOS-Development

---

## 1. Overview

Build **Avanues for iOS** — a voice-controlled web browser with semantic command understanding, shipping as a single iOS app that includes both an **in-app WKWebView browser** and a **Safari Web Extension** for voice control in Safari.

### Product Vision
Bring the Avanues voice-first web browsing experience to iOS. Users speak commands to navigate, click, scroll, search, and interact with any webpage — hands-free.

### Key Differentiator
No competitor offers a voice-controlled web browser on the iOS App Store. Apple's built-in Voice Control is generic and not optimized for web interaction. Avanues fills this gap with domain-specific voice commands, numbered element overlays, and semantic understanding via NLU.

---

## 2. Requirements

### Functional Requirements

| ID | Requirement | Priority | Source |
|----|------------|----------|--------|
| FR-01 | Voice-controlled web navigation (go to URL, back, forward, refresh) | P0 | Core |
| FR-02 | DOM scraping with numbered voice targets on interactive elements | P0 | WebAvanue |
| FR-03 | Voice commands: "click [N]", "scroll up/down", "go back", "search [query]" | P0 | VoiceOSCore |
| FR-04 | Tab management (open tab, close tab, switch tab N) | P0 | WebAvanue |
| FR-05 | Speech recognition via SFSpeechRecognizer (on-device, 5 locales) | P0 | SpeechRecognition |
| FR-06 | Multi-locale support (en-US, es-ES, fr-FR, de-DE, hi-IN) | P1 | VoiceOSCore |
| FR-07 | VOS command file loading (.web.vos) | P1 | VoiceOSCore |
| FR-08 | Bookmarks, history, favorites bar | P1 | WebAvanue |
| FR-09 | NLU intent classification for semantic commands | P1 | AI/NLU |
| FR-10 | Cloud LLM integration (OpenAI/Claude API) for natural language queries | P1 | AI/LLM |
| FR-11 | Reading mode with voice readback (AVSpeechSynthesizer) | P2 | WebAvanue |
| FR-12 | Safari Web Extension with floating mic button + voice commands | P2 | Safari Extension |
| FR-13 | Private browsing mode | P2 | WebAvanue |
| FR-14 | Download management | P2 | WebAvanue |
| FR-15 | VOS SFTP sync for crowd-sourced command profiles | P3 | VoiceOSCore |
| FR-16 | Siri Shortcuts ("Open [site] in Avanues") | P3 | iOS native |
| FR-17 | Settings: theme (palette, style, appearance), voice locale, sync | P1 | AvanueUI |

### Non-Functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-01 | Voice command latency (mic tap to action) | < 1.5 seconds |
| NFR-02 | DOM scraping time (page elements extracted) | < 500ms |
| NFR-03 | App cold start to usable | < 2 seconds |
| NFR-04 | Memory footprint | < 150 MB active |
| NFR-05 | iOS version support | iOS 16.0+ |
| NFR-06 | Device support | iPhone, iPad |
| NFR-07 | Accessibility | Full VoiceOver compatibility |
| NFR-08 | Code reuse from Android (KMP) | >= 60% |
| NFR-09 | App Store approval | First submission |
| NFR-10 | Privacy | On-device speech by default, no data sent without consent |

---

## 3. User Scenarios

### Scenario 1: First-Time Voice Web Browsing
- **Given**: User launches Avanues for the first time
- **When**: App shows brief onboarding (3 screens: mic permission, speech recognition permission, quick tutorial)
- **Then**: User is on the browser home screen with search bar and voice button
- **When**: User taps mic button and says "go to wikipedia"
- **Then**: App navigates to wikipedia.org, scrapes DOM, shows numbered overlays on links
- **When**: User says "click 5"
- **Then**: App clicks element #5, page navigates, new overlays appear

### Scenario 2: Semantic Voice Command (NLU)
- **Given**: User is on a shopping site with search bar, cart, menu
- **When**: User says "search for running shoes"
- **Then**: NLU classifies intent as SEARCH, extracts entity "running shoes"
- **Then**: App finds search input via DOM scraping, fills it, submits form
- **Then**: Results page loads with new numbered overlays

### Scenario 3: Safari Extension Voice Control
- **Given**: User has enabled the Avanues Safari Extension
- **When**: User is browsing in Safari, taps floating mic button (injected by extension)
- **Then**: Extension sends native message to containing app, SFSpeechRecognizer starts
- **When**: User says "scroll down" then "click the login button"
- **Then**: Extension executes scroll via JS, then finds and clicks login element

### Scenario 4: Multi-Locale Usage
- **Given**: User switches locale to es-ES in settings
- **When**: User says "haz clic en tres" (click three)
- **Then**: App recognizes Spanish command, clicks element #3
- **When**: User says "desplazar hacia abajo" (scroll down)
- **Then**: Page scrolls down

### Scenario 5: Natural Language Chat (Cloud LLM)
- **Given**: User is on a complex page and doesn't know exact command
- **When**: User says "find the contact email on this page"
- **Then**: Cloud LLM analyzes page content + user intent
- **Then**: App scrolls to and highlights the contact email element

---

## 4. Architecture

### High-Level

```
+--------------------------------------------------+
|              iOS Avanues App (SwiftUI)             |
|                                                    |
|  +----------+  +-----------+  +----------------+  |
|  | Onboard  |  | Settings  |  | Voice Command  |  |
|  | (SwiftUI)|  | (Compose  |  | Bar (SwiftUI)  |  |
|  +----------+  |  MP)      |  +----------------+  |
|                +-----------+                       |
|  +----------------------------------------------+  |
|  |        Browser View (Compose MP)              |  |
|  |  +------------------------------------------+|  |
|  |  |  WKWebView + DOMScraperBridge.js         ||  |
|  |  |  Voice Overlay (numbered labels)          ||  |
|  |  +------------------------------------------+|  |
|  +----------------------------------------------+  |
|                                                    |
|  +----------------------------------------------+  |
|  |        Safari Web Extension                   |  |
|  |  content.js | background.js | popup.html      |  |
|  +----------------------------------------------+  |
+--------------------------------------------------+
            |                    |
   +--------+--------+  +-------+--------+
   |  KMP Shared      |  | iOS Platform   |
   |  (Kotlin/Native) |  | (Swift/ObjC)   |
   |                  |  |                |
   | VoiceOSCore      |  | SFSpeechRec    |
   | WebAvanue        |  | WKWebView      |
   | Database         |  | Keychain       |
   | NLU              |  | CoreML         |
   | AvanueUI tokens  |  | AVAudioSession |
   | Ktor networking  |  | URLSession     |
   +------------------+  +----------------+
```

### Technology Stack

| Layer | Technology | Notes |
|-------|-----------|-------|
| App Shell | SwiftUI | Navigation, onboarding, voice bar |
| Browser Screen | Compose Multiplatform | WKWebView via UIKitView, shared with Android |
| Settings Screen | Compose Multiplatform | Reuse UnifiedSettingsScreen |
| Shared Logic | KMP (Kotlin/Native) | VoiceOSCore, WebAvanue, Database, NLU |
| Swift Interop | SKIE (Touchlab) | Flow->AsyncSequence, sealed->enum |
| DI | Koin 4.0+ | Cross-platform, replaces Hilt |
| Navigation | Decompose | Lifecycle-aware, SwiftUI interop |
| Database | SQLDelight (NativeSqliteDriver) | 52 .sq schemas shared |
| Networking | Ktor (Darwin engine) | Cloud LLM APIs |
| Settings Storage | DataStore 1.1.0+ | Theme, locale, sync preferences |
| Speech | SFSpeechRecognizer | On-device, 5 locales |
| NLU Inference | CoreML | Intent classification, entity extraction |
| Web Engine | WKWebView | DOM scraping via JS injection |
| CI/CD | GitHub Actions + Fastlane | Build, sign, TestFlight |

### Module Reuse Map

| Module | commonMain Files | iosMain Files | Reuse % |
|--------|-----------------|---------------|---------|
| VoiceOSCore | 209 | 8 (expand to ~20) | 90% |
| WebAvanue | 164 | 3 (expand to ~15) | 85% |
| Database | 52 .sq schemas | 1 driver | 99% |
| AvanueUI | Full token system | 0 (tokens only) | 70% |
| AI/NLU | Full pipeline | 7 (expand to ~12) | 65% |
| SpeechRecognition | Abstractions | 3 (expand to ~5) | 60% |
| Foundation | Full utilities | Ready | 95% |
| AVID | Full ID system | Ready | 95% |
| **Weighted Average** | | | **~80%** |

---

## 5. What's NOT in Scope (iOS Limitations)

| Feature | Reason | Alternative |
|---------|--------|------------|
| System-wide voice control | iOS sandbox — Apple only | In-app + Safari extension |
| App UI scraping (Tier 3) | No AccessibilityService | Web scraping only |
| Always-listening background mic | iOS restricts background audio | Tap-to-talk |
| Cursor overlay on other apps | No overlay permission | In-app overlays only |
| App launch by voice | No PackageManager | Siri Shortcuts (P3) |
| Local LLM inference | CoreML conversion needed | Cloud API (Phase 1) |
| Local RAG embeddings | No ONNX on iOS easily | Cloud embeddings (Phase 2) |

---

## 6. 4-Tier Voice System (iOS Adaptation)

| Tier | Android | iOS | Status |
|------|---------|-----|--------|
| Tier 1: AVID | Voice IDs on UI elements | Same (in-app only) | FULL |
| Tier 2: Voice: prefix | Developer-annotated commands | Same (web pages) | FULL |
| Tier 3: App scraping | AccessibilityService | **NOT POSSIBLE** | REMOVED |
| Tier 4: VOS profiles | .web.vos file loading | Same | FULL |

iOS effectively operates as **3-Tier Voice** (Tier 1 + 2 + 4), focused entirely on web content.

---

## 7. Safari Extension Scope

### Included in Phase 1 (P2)

| Component | Description |
|-----------|-------------|
| manifest.json | Permissions: nativeMessaging, activeTab, storage |
| content.js | DOMScraperBridge.js + floating mic button + voice overlays |
| background.js | Native messaging bridge (non-persistent) |
| popup.html | Settings: locale, overlay style, help |
| SafariWebExtensionHandler.swift | Bridge to SFSpeechRecognizer in containing app |

### Architecture
- Content script injects floating mic button on all pages
- User taps mic -> native message -> app starts SFSpeechRecognizer
- Transcription flows back -> content script executes DOM command
- Latency: ~200ms (warm) to ~2.5s (cold background script)

### Limitation
- 6 MB memory limit — no AI models, lightweight JS only
- Non-persistent background — stateless design
- Commands: click, scroll, navigate, search (basic set)
- NLU/semantic commands only available in in-app browser (not extension)

---

## 8. NLU + Semantic Commands (iOS)

### Phase 1 Scope
- **Intent classification** via CoreML (MobileBERT converted from ONNX)
- Intents: CLICK, SCROLL, NAVIGATE, SEARCH, READ, BOOKMARK, TAB, SETTINGS
- **Entity extraction**: numbers, URLs, search queries, element descriptions
- On-device inference (Apple Neural Engine when available)

### What This Enables
Instead of rigid "click 3", users can say:
- "find the search bar and type running shoes" -> SEARCH intent, entity="running shoes"
- "go to the next page" -> NAVIGATE intent, entity="next"
- "read this article to me" -> READ intent
- "bookmark this" -> BOOKMARK intent

### Cloud LLM Fallback
When NLU confidence is low or command is complex:
- Send page context + user utterance to Cloud LLM (OpenAI/Claude)
- LLM returns structured command
- Requires internet + user consent

---

## 9. Success Criteria

- [ ] Voice-controlled web browsing works end-to-end (mic -> speech -> command -> DOM action)
- [ ] DOM scraping with numbered overlays on 95%+ of websites
- [ ] 5 locales operational (en-US, es-ES, fr-FR, de-DE, hi-IN)
- [ ] NLU intent classification with >= 85% accuracy on core intents
- [ ] Safari extension functional (basic voice commands in Safari)
- [ ] Tab management, bookmarks, history working
- [ ] VOS .web.vos file loading working
- [ ] Settings: theme (4 palettes x 4 styles x 3 appearances), locale selection
- [ ] App Store submission accepted
- [ ] VoiceOver fully compatible (all interactive elements labeled)
- [ ] Cold start < 2 seconds on iPhone 13+
- [ ] Voice command latency < 1.5 seconds

---

## 10. Privacy & Compliance

| Data | Handling |
|------|---------|
| Voice audio | On-device only via SFSpeechRecognizer, never transmitted |
| Speech transcription | On-device processing, not stored unless user enables history |
| Browsing history | Local SQLDelight only, encrypted at rest |
| Cloud LLM queries | Opt-in only, sent via HTTPS, no PII |
| VOS files | Local storage, SFTP sync opt-in |
| Crash analytics | Apple standard only (no third-party) |

### Info.plist Declarations
- `NSMicrophoneUsageDescription`: "Used for voice commands to control web browsing hands-free"
- `NSSpeechRecognitionUsageDescription`: "Converts your speech to text for voice commands. Processing happens on your device."

---

## 11. Target Platforms

| Device | Support | Notes |
|--------|---------|-------|
| iPhone (iOS 16+) | Full | Primary target |
| iPad (iPadOS 16+) | Full | Adaptive layout via AvanueUI DisplayProfile |
| Mac (Catalyst) | Deferred | Phase 2 consideration |
| Apple Vision Pro | Deferred | Phase 3 (XR color palettes exist) |

---

## References

- `docs/analysis/iOS/iOS-Analysis-AvanuesPortFeasibility-260212-V1.md`
- `docs/analysis/iOS/iOS-Analysis-ArchitectureDecisions-260212-V1.md`
- `docs/analysis/iOS/iOS-Analysis-NLU-RAG-Feasibility-260212-V1.md`
- `docs/analysis/iOS/iOS-Analysis-SafariWebExtension-260212-V1.md`
- `Docs/Analysis/VoiceOSCore/VoiceOSCore-Analysis-iOSVoiceControlCapabilities-260212-V1.md`
- `Docs/WebAvanue/MasterSpecs/WebAvanue-Spec-EnhancedWebView-51209-V1.md`
- `Docs/VoiceOS/specifications/VoiceOS-Spec-Phases10-15-KMPMigration-60106-V1.md`
