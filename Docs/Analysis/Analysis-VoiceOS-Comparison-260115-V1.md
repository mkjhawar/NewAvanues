# VoiceOSCoreNG vs VoiceOSCore - Functional Comparison

**Date:** 2026-01-15 | **Version:** V1

---

## Actual Line Counts

| Codebase | Lines of Code | Files |
|----------|---------------|-------|
| **VoiceOSCoreNG (NEW KMP)** | 73,371 | ~200 |
| **VoiceOSCore (OLD)** | 121,334 | ~500+ |
| **Ratio** | **60%** | ~40% |

**Correction:** My initial estimate of 15% was wrong. VoiceOSCoreNG is ~60% of old codebase by LOC.

---

## Flow Chart: VoiceOSCoreNG (NEW KMP)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         VoiceOSCoreNG Flow                                   │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────┐     ┌─────────────────────┐     ┌────────────────────────────┐
│  STARTUP     │────▶│  VoiceOSCoreNG.kt   │────▶│   Builder Pattern Init    │
└──────────────┘     │  (Main Facade)      │     │   - HandlerFactory        │
                     └─────────────────────┘     │   - SpeechEngineFactory   │
                               │                 │   - NLU/LLM Processors    │
                               ▼                 │   - CommandRegistry       │
                     ┌─────────────────────┐     └────────────────────────────┘
                     │   initialize()      │
                     └─────────────────────┘
                               │
           ┌───────────────────┼───────────────────┐
           ▼                   ▼                   ▼
┌────────────────┐   ┌────────────────┐   ┌────────────────┐
│ Create         │   │ Populate       │   │ Initialize     │
│ Handlers       │   │ Static Cmds    │   │ Speech Engine  │
│ (29 handlers)  │   │ (DB persist)   │   │ (STT/Vivoka)   │
└────────────────┘   └────────────────┘   └────────────────┘
           │                   │                   │
           └───────────────────┼───────────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        ActionCoordinator                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  processVoiceCommand(text, confidence)                               │   │
│  │                                                                      │   │
│  │  Priority Order:                                                     │   │
│  │  1. Dynamic Command (VUID exact match) ──▶ Execute via targetVuid   │   │
│  │  2. Dynamic Command (Fuzzy match) ───────▶ Execute with threshold   │   │
│  │  3. Static Handler (system commands) ────▶ Route to handler         │   │
│  │  4. NLU Classification (BERT) ───────────▶ Semantic match           │   │
│  │  5. LLM Interpretation ──────────────────▶ Natural language         │   │
│  │  6. Voice Interpreter (legacy) ──────────▶ Keyword mapping          │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Handler Execution                                   │
│                                                                             │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌──────────┐ │
│  │ System    │  │Navigation │  │   UI      │  │  Input    │  │   App    │ │
│  │ Handler   │  │ Handler   │  │ Handler   │  │ Handler   │  │ Handler  │ │
│  │ (back,    │  │ (scroll,  │  │ (click,   │  │ (type,    │  │ (open,   │ │
│  │  home)    │  │  swipe)   │  │  tap)     │  │  dictate) │  │  launch) │ │
│  └───────────┘  └───────────┘  └───────────┘  └───────────┘  └──────────┘ │
│                                                                             │
│  + ComposeHandler, FlutterHandler, ReactNativeHandler, WebViewHandler...   │
└─────────────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     Platform Executors (Android)                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │AndroidUIExecutor │  │AndroidAppLauncher│  │AndroidNavExecutor│          │
│  │ (AccessibilityApi│  │ (PackageManager) │  │ (Global actions) │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
└─────────────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────────────┐
│                     Screen Change / Learning Flow                            │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────┐     ┌─────────────────────┐     ┌────────────────────────────┐
│ Accessibility│────▶│  Screen Extraction  │────▶│    Element Processing      │
│ Event        │     │  (Platform)         │     │                            │
└──────────────┘     └─────────────────────┘     │  ┌────────────────────┐   │
                               │                 │  │ ElementParser      │   │
                               ▼                 │  │ (parse HTML/JSON)  │   │
                     ┌─────────────────────┐     │  └────────────────────┘   │
                     │ UnifiedExtractor    │     │  ┌────────────────────┐   │
                     │ - extractFromHtml   │     │  │ CommandGenerator   │   │
                     │ - extractFromA11y   │     │  │ (create commands)  │   │
                     └─────────────────────┘     │  └────────────────────┘   │
                               │                 │  ┌────────────────────┐   │
                               ▼                 │  │ ScreenFingerprinter│   │
                     ┌─────────────────────┐     │  │ (hash generation)  │   │
                     │ CommandRegistry     │     │  └────────────────────┘   │
                     │ - update(commands)  │     └────────────────────────────┘
                     │ - findByPhrase()    │
                     │ - fuzzy match       │
                     └─────────────────────┘
                               │
                               ▼
                     ┌─────────────────────┐
                     │ Speech Engine       │
                     │ - updateCommands()  │
                     │ (grammar update)    │
                     └─────────────────────┘


┌─────────────────────────────────────────────────────────────────────────────┐
│                     JIT Learning Flow (LearnApp)                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────┐     ┌─────────────────────┐     ┌────────────────────────────┐
│ User Taps    │────▶│  JITLearner         │────▶│   CommandLearner           │
│ Unknown Btn  │     │  - shouldLearn()    │     │   - learnCommand()         │
└──────────────┘     │  - requestLearning()│     │   - generateAliases()      │
                     └─────────────────────┘     │   - suggestAliases()       │
                               │                 └────────────────────────────┘
                               ▼
                     ┌─────────────────────┐     ┌────────────────────────────┐
                     │ IConsentProvider    │────▶│   ICommandPersistence      │
                     │ (user approval)     │     │   - insertBatch()          │
                     └─────────────────────┘     │   - save to database       │
                                                 └────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────────────┐
│                     Exploration Engine Flow                                  │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────┐     ┌─────────────────────┐     ┌────────────────────────────┐
│ Start        │────▶│ ExplorationEngine   │────▶│   Screen Capture           │
│ Exploration  │     │ - start(package)    │     │   - captureScreen()        │
└──────────────┘     └─────────────────────┘     │   - generateScreenHash()   │
                               │                 │   - detectFramework()      │
                               ▼                 └────────────────────────────┘
                     ┌─────────────────────┐
                     │ Statistics          │     ┌────────────────────────────┐
                     │ - screenCount       │     │   FrameworkDetector        │
                     │ - uniqueElements    │     │   - Flutter                │
                     │ - actionableElements│     │   - Compose                │
                     │ - progress (0-1.0)  │     │   - ReactNative            │
                     └─────────────────────┘     │   - Native                 │
                               │                 └────────────────────────────┘
                               ▼
                     ┌─────────────────────┐
                     │ ExplorationStats    │
                     │ (return to caller)  │
                     └─────────────────────┘
```

---

## Flow Chart: VoiceOSCore (OLD Android)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         VoiceOSCore Flow (OLD)                               │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────┐     ┌─────────────────────┐     ┌────────────────────────────┐
│  STARTUP     │────▶│  VoiceOSService.kt  │────▶│   ServiceDependencies      │
│  (Android    │     │  (AccessibilitySvc) │     │   - dbManager              │
│  OS binds)   │     └─────────────────────┘     │   - speechEngineManager    │
└──────────────┘               │                 │   - uiScrapingEngine       │
                               ▼                 │   - lifecycleCoordinator   │
                     ┌─────────────────────┐     │   - integrationCoordinator │
                     │ onServiceConnected()│     │   - commandDispatcher      │
                     └─────────────────────┘     │   - eventRouter            │
                               │                 └────────────────────────────┘
           ┌───────────────────┼───────────────────┐
           ▼                   ▼                   ▼
┌────────────────┐   ┌────────────────┐   ┌────────────────┐
│ Database       │   │ Integration    │   │ Event Router   │
│ Manager        │   │ Coordinator    │   │ Setup          │
│ (SQLDelight)   │   │ (lazy init)    │   │                │
└────────────────┘   └────────────────┘   └────────────────┘
           │                   │                   │
           └───────────────────┼───────────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      onAccessibilityEvent()                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  EventRouter.routeEvent(event)                                       │   │
│  │                                                                      │   │
│  │  1. Check service ready ──▶ Queue if not ready                      │   │
│  │  2. Check memory pressure ──▶ Filter low priority events            │   │
│  │  3. Trigger LearnApp init (first event)                             │   │
│  │  4. Process queued events                                            │   │
│  │  5. Route to handlers by event type                                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                               │
           ┌───────────────────┼───────────────────┐
           ▼                   ▼                   ▼
┌────────────────┐   ┌────────────────┐   ┌────────────────┐
│ WINDOW_STATE   │   │ WINDOW_CONTENT │   │ VIEW_CLICKED   │
│ CHANGED        │   │ CHANGED        │   │                │
└────────────────┘   └────────────────┘   └────────────────┘
           │                   │                   │
           └───────────────────┼───────────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     IntegrationCoordinator                                   │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │LearnAppIntegration│  │ScrapingIntegration│ │WebCommandCoord   │          │
│  │(1861 lines)       │  │(hash-based)        │ │(browser cmds)    │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
│  ┌──────────────────┐  ┌──────────────────┐                                 │
│  │CommandDiscovery   │  │JITLearningService │                                │
│  │Integration        │  │(foreground svc)   │                                │
│  └──────────────────┘  └──────────────────┘                                 │
└─────────────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      CommandDispatcher                                       │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  dispatchCommand(command, confidence)                                │   │
│  │                                                                      │   │
│  │  TIERED EXECUTION:                                                   │   │
│  │  RENAME TIER: "rename X to Y" ──▶ RenameCommandHandler              │   │
│  │  WEB TIER: Browser detected ────▶ WebCommandCoordinator             │   │
│  │  TIER 1: CommandManager ────────▶ Primary handler                   │   │
│  │  TIER 2: VoiceCommandProcessor ─▶ Database lookup                   │   │
│  │  TIER 3: ActionCoordinator ─────▶ Legacy fallback                   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────────────┐
│                     LearnApp Exploration (OLD)                               │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────┐     ┌─────────────────────────────┐
│ User starts  │────▶│ ExplorationEngineRefactored │  (816 lines)
│ "Learn App"  │     └─────────────────────────────┘
└──────────────┘                   │
                   ┌───────────────┼───────────────┐
                   ▼               ▼               ▼
         ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
         │ DFSExplorer │  │ Element     │  │ Element     │
         │ (596 lines) │  │ Clicker     │  │ Registrar   │
         │ depth-first │  │ (405 lines) │  │ (418 lines) │
         └─────────────┘  └─────────────┘  └─────────────┘
                   │               │               │
                   └───────────────┼───────────────┘
                                   ▼
         ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
         │ Danger      │  │ Exploration │  │ Exploration │
         │ Detector    │  │ Metrics     │  │ Notifier    │
         │ (227 lines) │  │ (197 lines) │  │ (216 lines) │
         └─────────────┘  └─────────────┘  └─────────────┘
```

---

## Function Comparison Table

| Feature | VoiceOSCoreNG (NEW) | VoiceOSCore (OLD) | Status |
|---------|---------------------|-------------------|--------|
| **COMMAND PROCESSING** ||||
| Voice command parsing | ✅ ActionCoordinator (685 lines) | ✅ CommandDispatcher (465 lines) | **NEW HAS MORE** |
| Tiered command execution | ✅ (6 tiers: Dynamic→Static→NLU→LLM→Voice) | ✅ (5 tiers: Rename→Web→CM→VCP→AC) | **BOTH COMPLETE** |
| Fuzzy matching | ✅ CommandMatcher with synonyms | ✅ Basic matching | **NEW HAS MORE** |
| NLU/BERT classification | ✅ INluProcessor interface + Android impl | ❌ Not implemented | **NEW ONLY** |
| LLM natural language | ✅ ILlmProcessor interface + Android impl | ❌ Not implemented | **NEW ONLY** |
| Dynamic commands (VUID) | ✅ CommandRegistry + updateDynamicCommands | ✅ CommandManager + VoiceCommandProcessor | **BOTH COMPLETE** |
| Static system commands | ✅ StaticCommandRegistry (all system cmds) | ✅ ActionCoordinator | **BOTH COMPLETE** |
| **HANDLERS** ||||
| System (back, home, etc.) | ✅ SystemHandler | ✅ ActionCoordinator | **BOTH COMPLETE** |
| Navigation (scroll, swipe) | ✅ NavigationHandler, GestureHandler | ✅ Built into ActionCoordinator | **BOTH COMPLETE** |
| UI (click, tap, focus) | ✅ UIHandler | ✅ UIScrapingEngine | **BOTH COMPLETE** |
| Input (type, dictate) | ✅ InputHandler | ✅ Inline handling | **BOTH COMPLETE** |
| App (open, launch) | ✅ AppHandler + AndroidAppLauncher | ✅ InstalledAppsManager | **BOTH COMPLETE** |
| Framework-specific | ✅ Compose, Flutter, ReactNative, Unity, WebView handlers | ❌ Basic only | **NEW HAS MORE** |
| **EXTRACTION** ||||
| Screen fingerprinting | ✅ IScreenFingerprinter + ScreenState | ✅ AppHashCalculator, ElementHasher | **BOTH COMPLETE** |
| Element extraction | ✅ UnifiedExtractor, ElementParser | ✅ UIScrapingEngine | **BOTH COMPLETE** |
| Command generation | ✅ CommandGenerator (347 lines) | ✅ CommandGenerator (scraping/) | **BOTH COMPLETE** |
| Dynamic content detection | ✅ isDynamicContentScreen() | ✅ Built into scraping | **BOTH COMPLETE** |
| Popup detection | ✅ PopupInfo, PopupType enum | ✅ Built into accessibility | **BOTH COMPLETE** |
| **LEARNAPP / JIT** ||||
| JIT Learner | ✅ JITLearner (398 lines) | ✅ JIT Learning Service | **BOTH COMPLETE** |
| Command Learner | ✅ CommandLearner (244 lines) | ✅ LearnAppIntegration (1861 lines) | **OLD HAS MORE** |
| Consent Provider | ✅ IConsentProvider interface | ✅ ConsentDialogManager | **BOTH COMPLETE** |
| Exploration Engine | ✅ ExplorationEngine (316 lines) | ✅ ExplorationEngineRefactored (816 lines) | **OLD HAS MORE** |
| DFS Explorer | ❌ Not in KMP | ✅ DFSExplorer (596 lines) | **OLD ONLY** |
| Element Clicker | ❌ Not in KMP | ✅ ElementClicker (405 lines) | **OLD ONLY** |
| Element Registrar | ❌ Not in KMP | ✅ ElementRegistrar (418 lines) | **OLD ONLY** |
| Danger Detector | ✅ DangerousElementDetector (common) | ✅ DangerDetector (227 lines) | **BOTH COMPLETE** |
| **SPEECH** ||||
| Speech engine abstraction | ✅ ISpeechEngine interface | ✅ SpeechEngineManager | **BOTH COMPLETE** |
| Android STT | ✅ AndroidSTTEngineImpl | ✅ Built into SpeechEngineManager | **BOTH COMPLETE** |
| Vivoka SDK | ✅ VivokaAndroidEngine | ✅ Integration exists | **BOTH COMPLETE** |
| Vosk (offline) | ✅ VoskEngineImpl | ✅ Integration exists | **BOTH COMPLETE** |
| Azure Speech | ✅ AzureEngineImpl | ❌ Not implemented | **NEW ONLY** |
| Google Cloud | ✅ GoogleCloudEngineImpl | ❌ Not implemented | **NEW ONLY** |
| **PERSISTENCE** ||||
| Command persistence | ✅ ICommandPersistence interface | ✅ SQLDelight database | **BOTH COMPLETE** |
| Screen hash repository | ✅ ScreenHashRepository interface | ✅ Database entities | **BOTH COMPLETE** |
| Static command storage | ✅ IStaticCommandPersistence | ✅ Built into DB | **BOTH COMPLETE** |
| **OVERLAYS** ||||
| Number overlay | ✅ NumberedSelectionOverlay | ✅ NumberOverlay | **BOTH COMPLETE** |
| Command status overlay | ✅ CommandStatusOverlay | ✅ OverlayManager | **BOTH COMPLETE** |
| Context menu overlay | ✅ ContextMenuOverlay | ✅ Built in | **BOTH COMPLETE** |
| Confidence overlay | ✅ ConfidenceOverlay | ❌ Not separate | **NEW ONLY** |
| Theme system | ✅ OverlayTheme, YamlThemeParser | ✅ Theme support | **BOTH COMPLETE** |
| **ANDROID-ONLY (Not KMP)** ||||
| AccessibilityService | ❌ N/A (use old) | ✅ VoiceOSService.kt | **OLD ONLY** |
| IPC/AIDL | ❌ N/A (use old) | ✅ VoiceOSIPCService | **OLD ONLY** |
| Event routing | ❌ N/A (use old) | ✅ EventRouter (543 lines) | **OLD ONLY** |
| Service lifecycle | ❌ N/A (use old) | ✅ LifecycleCoordinator | **OLD ONLY** |

---

## What's Actually Working in VoiceOSCoreNG

| Category | Status | Evidence |
|----------|--------|----------|
| Command processing | ✅ WORKING | ActionCoordinator handles all command routing |
| Handler execution | ✅ WORKING | 29 handlers with platform executors |
| Fingerprinting | ✅ WORKING | ScreenFingerprinter, ElementFingerprint |
| Extraction | ✅ WORKING | UnifiedExtractor, ElementParser |
| Command generation | ✅ WORKING | CommandGenerator with list index support |
| JIT learning | ✅ WORKING | JITLearner with persistence |
| Speech engines | ✅ WORKING | 5 engine implementations |
| Overlays | ✅ WORKING | Multiple overlay types with theming |
| NLU/LLM | ✅ WORKING | BERT + LLM fallback chain |

---

## What's NOT in VoiceOSCoreNG (And Should Stay in App)

| Component | Reason |
|-----------|--------|
| VoiceOSService.kt | Android AccessibilityService - cannot be KMP |
| VoiceOSIPCService | Android AIDL IPC - cannot be KMP |
| EventRouter | Ties into Android accessibility events |
| IntegrationCoordinator | Wires up Android-specific integrations |
| LifecycleCoordinator | Android service lifecycle |

---

## What OLD Code is Likely Unused/Dead

Based on the duplicate locations and file sizes, likely dead code:
- Multiple copies in `Modules/AvaMagic/apps/VoiceOSCore/` (duplicate of android/apps/)
- Many "examples" folders with sample implementations
- Test mocks and fixtures
- Disabled files (`.disabled` extension)
- Backup files (`.backup`, `.stub-backup`)

---

## Recommended Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         TARGET ARCHITECTURE                                  │
└─────────────────────────────────────────────────────────────────────────────┘

android/apps/VoiceOS/                    Modules/VoiceOSCoreNG/
┌──────────────────────┐                ┌──────────────────────────────────┐
│ THIN ENTRY POINT     │                │ ALL BUSINESS LOGIC (KMP)         │
│                      │                │                                  │
│ VoiceOSService.kt    │───delegates───▶│ VoiceOSCoreNG.kt (facade)       │
│  - onAccessibility   │                │ ActionCoordinator               │
│    Event()           │                │ CommandRegistry                 │
│  - Android lifecycle │                │ All Handlers                    │
│                      │                │ Fingerprinting                  │
│ EventRouter.kt       │───routes────▶  │ Extraction                      │
│ (thin wrapper)       │                │ JIT Learning                    │
│                      │                │ Speech engines                  │
│ IntegrationCoord.kt  │───wires───▶    │ Overlays                        │
│ (Android wiring)     │                │ Persistence interfaces          │
└──────────────────────┘                └──────────────────────────────────┘
         │                                           │
         │                                           │
         ▼                                           ▼
┌──────────────────────┐                ┌──────────────────────────────────┐
│ Android-Specific     │                │ Platform Implementations         │
│                      │                │ (androidMain/)                   │
│ - AIDL/IPC           │                │                                  │
│ - Broadcast receivers│                │ - AndroidUIExecutor              │
│ - Activity/Fragment  │                │ - AndroidAppLauncher             │
│ - Hilt modules       │                │ - AndroidCommandPersistence      │
│                      │                │ - AndroidNluProcessor            │
└──────────────────────┘                └──────────────────────────────────┘
```

---

## Summary

**VoiceOSCoreNG is more complete than initially assessed:**
- 73,371 LOC (60% of old codebase)
- Has fingerprinting, extraction, command generation, JIT learning
- Has NLU/LLM integration that OLD doesn't have
- Has 5 speech engine implementations vs OLD's 3
- Has framework-specific handlers (Flutter, Compose, etc.)

**What OLD has that NEW doesn't:**
- Advanced DFS exploration (~1,400 lines) - could be migrated to KMP
- More mature LearnAppIntegration (~1,800 lines) - Android-specific wiring
- Event routing infrastructure - must stay in Android app

**Recommendation:** The architecture is CORRECT. VoiceOSCoreNG should contain all business logic. The app should be a thin entry point. The OLD code in duplicate locations should be deleted after verifying the 12 files migrated today work correctly.
