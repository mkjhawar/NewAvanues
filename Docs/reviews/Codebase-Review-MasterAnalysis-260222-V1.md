# NewAvanues KMP Monorepo — Master Codebase Analysis
**Date:** 260222 | **Branch:** VoiceOS-1M-SpeechEngine | **Author:** Manoj Jhawar
**Scope:** 43 modules, 9 apps, ~2,970 .kt files, 83 .sq schemas
**Method:** 20-agent parallel SWARM review across 4 waves

---

## Health Dashboard

| Module | Files | Tier | Health | Score | P0 | P1 | P2+ | Key Risk |
|--------|-------|------|--------|-------|----|----|-----|----------|
| Foundation | 31 kt | 1 | YELLOW | 68 | 0 | 2 | 3 | Missing androidMain actuals (IFileSystem, IPermissionChecker) |
| Logging | 11 kt | 1 | GREEN | 82 | 0 | 0 | 3 | PII regex false positives; lazy eval defeated |
| Database | 264 kt, 60 sq | 1 | YELLOW | 65 | 1 | 4 | 5 | No migration system; parallel web command stores |
| AVID | 9 kt | 1 | YELLOW | 72 | 0 | 3 | 1 | Random hash fallback; platform default bug |
| AVU | 58 kt | 1 | YELLOW | 74 | 2 | 3 | 2 | ACD code collision; double-unescape bug |
| AvanueUI | 431 kt | 1 | RED | 38 | 42 | 22 | 15 | 42 render() stubs; iOS/JVM disabled; MaterialTheme violations |
| VoiceOSCore | 395 kt | 1 | YELLOW | 73 | 4 | 12 | 20 | ANI leak; dual registry; 131 Rule 7 violations |
| SpeechRecognition | 93 kt | 1 | YELLOW | 62 | 3 | 5 | 8 | Dead download button; audioQueue not rebuilt; scoped storage violation |
| WebAvanue | 281 kt | 1 | YELLOW | 61 | 3 | 5 | 9 | Command injection; wrong disambiguation; JS security theater |
| AI | 397 kt | 1 | RED | 42 | 9 | 18 | 37 | RAG embeddings garbage; iOS NLU broken; no rate limiting |
| Rpc | 200 kt | 2 | RED | 41 | 1 | 7 | 6 | PlatformClient.android is 5-method stub; resource leaks |
| IPC | 31 kt | 2 | RED | 38 | 4 | 9 | 5 | ConnectionManager is simulation; iOS non-functional |
| PluginSystem | 173 kt | 2 | YELLOW | 55 | 2 | 4 | 8 | DEX/AVP mismatch; permissions lost on restart |
| DeviceManager | 82 kt | 2 | YELLOW | 48 | 0 | 7 | 10 | Simulated audio devices; wrong battery fields; iOS capability lies |
| Cockpit | 47 kt | 2 | YELLOW | 62 | 0 | 4 | 4 | WorkflowSteps never loaded; Z-order race; DRY violation |
| HTTPAvanue | 69 kt | 2 | YELLOW | 60 | 2 | 3 | 7 | PADDED flag not stripped; SETTINGS overflow; HPACK encoder stale |
| Actions | 40 kt | 2 | GREEN | 78 | 0 | 1 | 2 | Partial init marks ready; substring false positives |
| Utilities | 20 kt | 3 | YELLOW | 55 | 0 | 2 | 5 | Manual init crash risk; duplicate concerns vs Foundation/Logging |
| Localization | 4 kt | 3 | RED | 35 | 1 | 2 | 3 | No iOS target; 36/42 languages empty; formatString bug |
| AvanuesShared | 3 kt | 3 | YELLOW | 50 | 0 | 1 | 3 | Empty Koin modules; missing module exports |
| AVACode | 10 kt | 3 | GREEN | 80 | 0 | 2 | 1 | System.currentTimeMillis in commonMain; mutable data class copy |
| VoiceKeyboard | 22 kt | 3 | RED | 30 | 1 | 3 | 4 | currentInputConnection self-assignment — entirely non-functional |
| VoiceCursor | 10 kt | 3 | GREEN | 75 | 0 | 1 | 2 | Best of voice input cluster; compound-state gap |
| VoiceDataManager | 11 kt | 3 | RED | 35 | 2 | 2 | 4 | Export/import stubs; MaterialTheme violations; AI Author |
| VoiceAvanue | 3 kt | 3 | RED | 25 | 3 | 0 | 1 | All 3 subsystems are empty TODO stubs |
| Voice/WakeWord | 13 kt | 3 | RED | 30 | 4 | 2 | 2 | PhonemeWakeWordDetector is complete no-op; 3 Rule 7 violations |
| VoiceIsolation | 7 kt | 3 | YELLOW | 70 | 0 | 2 | 3 | iOS/Desktop stubs report success falsely |
| WebSocket | 8 kt | 3 | YELLOW | 62 | 1 | 6 | 4 | connect() returns before ready; 3rd AVU escape impl |
| NoteAvanue | 16 kt | 3 | YELLOW | 65 | 0 | 5 | 4 | AVID gaps; heading detection broken; no desktop impl |
| PhotoAvanue | 15 kt | 3 | YELLOW | 60 | 2 | 3 | 3 | ModeChip no-op; OIS/EIS key confusion; no voice wiring |
| CameraAvanue | 2 kt | 3 | RED | 25 | 2 | 1 | 1 | Deprecated by PhotoAvanue; camera leak; should be removed |
| ImageAvanue | 4 kt | 3 | GREEN | 72 | 1 | 1 | 2 | Debug `|| true` artifact; missing Android controller |
| PDFAvanue | 5 kt | 3 | YELLOW | 50 | 2 | 3 | 2 | search/extractText stubs; fd leak on dispose; no AVID |
| VideoAvanue | 5 kt | 3 | YELLOW | 55 | 0 | 2 | 2 | Global mutable executor; no pagination |
| RemoteCast | 12 kt | 3 | YELLOW | 60 | 0 | 0 | 4 | release() ineffective; desktop stub; no mDNS |
| Gaze | 4 kt | 3 | YELLOW | 62 | 0 | 1 | 3 | StubGazeTracker.state creates new flow per access |
| Whisper | 12 kt | 3 | GREEN | 85 | 0 | 0 | 0 | Example code only; production Whisper in SpeechRecognition |
| LicenseManager | 7 kt | 3 | RED | 25 | 3 | 3 | 3 | Validator is hardcoded stub; plaintext key storage; broken security tests |
| LicenseSDK | 2 kt | 3 | YELLOW | 55 | 1 | 1 | 1 | parseIsoDate ignores input; grace period always wrong |
| LicenseValidation | 14 kt | 3 | YELLOW | 50 | 0 | 3 | 3 | iOS QR returns Obj-C description; Android busy-poll; requestPermission no-op |
| AnnotationAvanue | 9 kt | 3 | GREEN | 78 | 0 | 1 | 1 | Healthiest in cluster; Desktop eraser AlphaComposite bug only |
| AvidCreator | 42 kt | 3 | YELLOW | 60 | 0 | 2 | 3 | Volatile state in hash input; some dead code |
| AVA | 155 kt, 21 sq | 2 | YELLOW | 58 | 0 | 2 | 5 | Hardcoded LLM path; leading-wildcard LIKE scans; template responses |

### App Health

| App | Files | Health | Score | P0 | P1 | Key Risk |
|-----|-------|--------|-------|----|----|----------|
| apps/avanues (consolidated) | 48 kt | YELLOW | 58 | 4 | 5 | BootReceiver broken; no activity-alias; HiltWorkerFactory missing |
| Apps/voiceavanue | 12 kt | RED | 35 | 4 | 5 | AvanueTheme not wired; CursorOverlay empty; BootReceiver commented out |
| Apps/voiceavanue-legacy | 12 kt | RED | 20 | 1 | 1 | Exact duplicate of voiceavanue; should be deleted |
| android/apps/webavanue | 10 kt | RED | 30 | 3 | 4 | SQLCipher encryption is a no-op stub; all 8 E2E tests are no-ops |
| android/apps/webavanue-legacy | 10 kt | RED | 20 | 1 | 0 | Byte-for-byte copy of webavanue; should be deleted |
| android/apps/VoiceOS | 6 kt | YELLOW | 50 | 0 | 4 | runBlocking on main; dispose on cancelled scope; emoji in notification |
| android/apps/VoiceRecognition | 13 kt | YELLOW | 50 | 2 | 2 | Transcript overwrites; test engine mismatch |
| android/apps/VoiceOSIPCTest | 1 kt | GREEN | 70 | 0 | 0 | Clean test harness; minor Thread.sleep issue |
| android/apps/cockpit-mvp | 2 kt | RED | 15 | 0 | 0 | Incompilable — 10+ undefined symbols, no build file |
| BuildSystem (virtual) | — | YELLOW | 62 | 2 | 6 | AGP stale; 85% modules have zero tests; no convention plugins |

---

## Aggregate Statistics

| Metric | Value |
|--------|-------|
| Total modules reviewed | **43 of 43 (100%)** |
| Total apps reviewed | **9 of 9 (100%)** |
| Total findings | **~435** |
| P0 Critical | **~97** |
| P1 High | **~134** |
| P2+ Medium/Low | **~204** |
| GREEN modules | 7 (Logging, AVACode, Actions, VoiceCursor, ImageAvanue, Whisper, VoiceOSIPCTest) |
| YELLOW modules | 18 |
| RED modules | 12 (AvanueUI, AI, Rpc, IPC, Localization, VoiceKeyboard, VoiceDataManager, VoiceAvanue, Voice/WakeWord, CameraAvanue, cockpit-mvp, VoiceOSCore borderline) |
| Rule 7 violations | **~160+ files** (131 in VoiceOSCore, 10 in VoiceRecognition, 4 in Voice, 3 in VoiceDataManager, 5+ in AvanueUI) |
| Test coverage | **~15%** of modules have any tests |
| AVID compliance | **~20%** of UI screens have voice identifiers |

---

## Tier 1: Core Platform (Must be healthy for anything to work)

### Foundation | 31 kt | Tier 1
PURPOSE: Base KMP abstractions — state management (ViewModelState, ListState, UiState), coroutine ViewModel base, SHA-256, settings interfaces/codecs, platform abstractions (ISettingsStore, ICredentialStore, IFileSystem, IPermissionChecker).
WHY: Centralizes boilerplate every feature module needs. Canonical settings persistence contract.
DEPS: kotlinx.coroutines.core
CONSUMERS: VoiceOSCore, Cockpit, DeviceManager, AvanuesShared, most feature modules
KMP: commonMain, androidMain (partial), iosMain, desktopMain
KEY_CLASSES: ISettingsStore<T>, ICredentialStore, ViewModelState<T>, BaseViewModel, AvanuesSettingsCodec, HashUtils
HEALTH: YELLOW — Missing androidMain actuals for IFileSystem/IPermissionChecker; IosPermissionChecker always returns true; BaseViewModel.launchIO dispatches to Default not IO.

### Database | 264 kt, 60 sq | Tier 1
PURPOSE: Unified KMP SQLite persistence layer — 67 tables across scraping, commands, browser, notes, Cockpit, plugins, AVID, VOS, NLU.
WHY: Single SQLDelight database (voiceos.db) shared across all KMP targets, replacing fragmented Room databases.
DEPS: sqldelight-runtime, kotlinx-coroutines, kotlinx-serialization, kotlinx-datetime, AVID module
CONSUMERS: VoiceOSCore, Cockpit, NoteAvanue, WebAvanue, AI/NLU, PluginSystem, AVID, CommandManager, LearnApp
KMP: commonMain (all .sq + interfaces), androidMain (WAL+FK), iosMain, desktopMain
TABLES: 67 total (scraped_app, scraped_element, tab, history_entry, note_entity, CockpitSession, plugins, avid_elements, recognition_learning, etc.)
KEY_CLASSES: DatabaseDriverFactory, SQLDelightScrapedElementRepository, SQLDelightCommandRepository, SQLDelightPluginRepository
HEALTH: YELLOW — No migration system (deriveSchemaFromMigrations=false); parallel incompatible stores; commented-out FKs; Cockpit timestamps as TEXT.

### VoiceOSCore | 395 kt | Tier 1
PURPOSE: Voice OS engine — 11 command handlers, accessibility service, scraping pipeline, overlay system, element fingerprinting, screen context, command dispatch.
WHY: The core runtime that makes voice control work across all apps.
DEPS: Foundation, Database, AVID, AVU, Logging, kotlinx-coroutines
CONSUMERS: All apps, Cockpit, WebAvanue
KMP: commonMain (pipeline logic), androidMain (handlers, accessibility, scraping)
KEY_CLASSES: VoiceOSAccessibilityService, AndroidScreenExtractor, HandlerRegistry, CommandGenerator, OverlayStateManager, ElementFingerprint
HEALTH: YELLOW — ANI memory leak in handleScreenChange; dual command registry architecture; 131 Rule 7 violations; phrase collisions between handlers; ttsReady data race.

### AI | 397 kt | Tier 1
PURPOSE: 7 sub-modules — NLU (intent matching), RAG (retrieval), LLM (provider abstraction), Memory, Chat, Teach, ALC.
WHY: Powers voice understanding, semantic search, conversational AI, and learning.
DEPS: ONNX Runtime, kotlinx-coroutines, kotlinx-serialization, Ktor
CONSUMERS: VoiceOSCore, apps
KMP: commonMain, androidMain (full), iosMain (broken), desktopMain (partial)
KEY_CLASSES: UnifiedLearningService, NLUClassifier, RAGPipeline, LLMProvider, MemoryManager
HEALTH: RED — RAG embeddings are garbage (hashCode not BERT vocab); iOS NLU produces zero-vectors; no rate limiting; hardcoded absolute paths; model checksums are TODO strings.

---

## Tier 2: Feature Infrastructure

### AvanueUI | 431 kt | Tier 1 (should be, but RED health)
PURPOSE: Design system — DSL component model, Theme v5.1 (3-axis), platform renderers, voice command router.
WHY: Single source of truth for design tokens, typography, colors; DSL → Android/iOS/Desktop rendering.
DEPS: Compose, Material3, Coil, kotlinx-serialization, LSP4J (JVM)
CONSUMERS: VoiceOSCore, Cockpit, DeviceManager, all apps
KMP: Android only in practice (iOS/JVM targets commented out in 8 sub-modules)
KEY_CLASSES: Theme, ColorScheme, GlassAvanue, Components, MagicDp/MagicSp, VoiceCommandRouter
HEALTH: RED — 42 render() stubs throwing TODO; 60+ MaterialTheme violations; CloudThemeRepository 9 no-ops; zero AVID on Compose components; KMP promise unfulfilled.

### PluginSystem | 173 kt | Tier 2
PURPOSE: Plugin lifecycle manager — 8-step load lifecycle, dependency resolution (DFS cycle detection), permission sandbox, checkpoint/rollback, hot-reload.
WHY: Enables .avp text file plugins to extend VoiceOS.
DEPS: kotlinx-coroutines, kotlinx-serialization
CONSUMERS: VoiceOSCore, apps
KMP: commonMain, androidMain, jvmMain, desktopMain, jsMain
KEY_CLASSES: PluginLoader, PluginRegistry, DependencyResolver, PluginSandbox, TransactionManager
HEALTH: YELLOW — DEX classloader incompatible with .avp text files; permissions lost on restart; valueOf crashes on bad manifests.

### Cockpit | 47 kt | Tier 2
PURPOSE: Multi-window session hub — 17 frame content types, 13 layout modes, SQLDelight persistence, traffic-light controls.
WHY: The convergence point where all "avenues" meet in a spatial UI.
DEPS: Foundation, Database, kotlinx-serialization
CONSUMERS: apps/avanues
KMP: commonMain, androidMain, desktopMain
KEY_CLASSES: CockpitViewModel, CockpitSession, FrameContent, LayoutEngine, FrameWindow
HEALTH: YELLOW — WorkflowSteps never loaded; Z-order race on concurrent addFrame; Android/Desktop repos 360 lines duplicated.

### HTTPAvanue | 69 kt | Tier 2
PURPOSE: KMP HTTP server library — HTTP/1.1 + HTTP/2, WebSocket, HPACK, flow control.
WHY: Powers on-device HTTP server for RemoteCast, local API, and inter-device communication.
DEPS: kotlinx-coroutines
CONSUMERS: RemoteCast, VoiceOSCore
KMP: commonMain
KEY_CLASSES: HttpServer, Http2Connection, HpackEncoder, HpackDecoder, WebSocket
HEALTH: YELLOW — PADDED flag not stripped (corrupts padded frames); SETTINGS overflow; HPACK encoder not updated on renegotiation; busy-poll flow control.

### SpeechRecognition | 93 kt | Tier 1
PURPOSE: Multi-engine speech abstraction — Android (5 engines: Whisper, Google Cloud STT v2, Android STT, Vosk, Vivoka), iOS (Apple Speech + Whisper), Desktop (Whisper).
WHY: Core voice input pipeline for the entire OS.
DEPS: ONNX Runtime (Whisper), Google Cloud Speech, kotlinx-coroutines
CONSUMERS: VoiceOSCore, apps
KMP: commonMain, androidMain, iosMain, desktopMain
KEY_CLASSES: SpeechRecognitionService, WhisperEngine, GoogleCloudStreamingClient, WhisperVAD, ConfidenceScorer
HEALTH: YELLOW — Dead download button; audioQueue closed permanently on reconnect; scoped storage violation; Thread.sleep in IO callback.

### WebAvanue | 281 kt | Tier 1
PURPOSE: Web browser with DOM scraping for voice control — DOMScraperBridge, JS bridge, tab management, 45 browser commands.
WHY: Enables voice control of any web page via DOM element discovery.
DEPS: Foundation, Database, VoiceOSCore (via callbacks)
CONSUMERS: apps/avanues, android/apps/webavanue
KMP: commonMain + androidMain (iOS/Desktop disabled)
KEY_CLASSES: DOMScraperBridge, BrowserVoiceOSCallback, TabViewModel, SecureScriptLoader, WebAvanueVoiceOSBridge
HEALTH: YELLOW — selectDisambiguationOption indexes wrong; sendScrapeResult injection; SecureScriptLoader Base64 not AES; 6 scroll stubs.

---

## Tier 3: Content & Peripheral Modules

### NoteAvanue | 16 kt
PURPOSE: Voice-first rich text editor with SQLDelight persistence, compose-rich-editor, dictation, RAG indexing.
HEALTH: YELLOW (65) — AVID gaps on all interactive elements; heading detection broken; no desktop impl.

### PhotoAvanue | 15 kt
PURPOSE: Full CameraX pipeline — GPS EXIF, zoom/exposure, flash, extensions (Bokeh/HDR/Night), pro mode (ISO/shutter/focus/WB/RAW).
HEALTH: YELLOW (60) — ModeChip onClick is empty (cannot switch modes); OIS/EIS Camera2 key confusion; no voice wiring.

### CameraAvanue | 2 kt
PURPOSE: Deprecated camera composable superseded by PhotoAvanue.
HEALTH: RED (25) — Camera HAL leak; strict subset of PhotoAvanue. **Recommend removal.**

### ImageAvanue | 4 kt
PURPOSE: Image viewer with pinch-zoom, filters, gallery navigation, voice commands. Desktop controller complete.
HEALTH: GREEN (72) — Debug `|| true` artifact; BLUR/SHARPEN filters return null.

### PDFAvanue | 5 kt
PURPOSE: Android PdfRenderer-based viewer with zoom, page navigation.
HEALTH: YELLOW (50) — search()/extractText() return empty; file descriptor leak in onDispose; no AVID.

### VideoAvanue | 5 kt
PURPOSE: ExoPlayer-based video playback with voice commands.
HEALTH: YELLOW (55) — Global mutable executor breaks multi-window; no gallery pagination.

### RemoteCast | 12 kt
PURPOSE: Smart glasses bidirectional casting — MJPEG/WebRTC, CAST/VOC/CMD protocols.
HEALTH: YELLOW (60) — release() ineffective when scope cancelled; desktop is silent stub.

### Localization | 4 kt
PURPOSE: KMP language manager — 42-language declaration, key-based translation, StateFlow language changes.
HEALTH: RED (35) — No iOS target at all; 36/42 languages have zero translations; formatString argument ordering bug.

### VoiceKeyboard | 22 kt
PURPOSE: Voice-driven keyboard input method.
HEALTH: RED (30) — currentInputConnection self-assignment makes keyboard entirely non-functional.

### VoiceCursor | 10 kt
PURPOSE: Cursor accessibility control with head-tracking math.
HEALTH: GREEN (75) — Best-designed module in voice input cluster.

### VoiceDataManager | 11 kt
PURPOSE: Voice data persistence and management UI.
HEALTH: RED (35) — Export/import are stubs returning null/false; MaterialTheme violations; "Author: AI Assistant".

### VoiceAvanue | 3 kt
PURPOSE: Unified voice module entry point with CommandSystem, BrowserSystem, RpcSystem.
HEALTH: RED (25) — All 3 subsystems are empty TODO bodies that falsely report initialized.

### Voice/WakeWord | 13 kt
PURPOSE: Wake word detection service with foreground lifecycle.
HEALTH: RED (30) — PhonemeWakeWordDetector is complete no-op; 3 "author: Claude Code" violations.

### VoiceIsolation | 7 kt
PURPOSE: Audio pipeline — noise suppression, echo cancellation, gain control.
HEALTH: YELLOW (70) — Android is production-quality; iOS/Desktop stubs falsely report success.

### AVU | 58 kt
PURPOSE: Wire protocol + DSL + interpreter for voice command automation.
HEALTH: YELLOW (74) — ACD code collision; double-unescape; lexer dash bug; @Synchronized no-op on K/N.

### AVACode | 10 kt
PURPOSE: Form DSL with validation, database schema generation, workflow orchestration.
HEALTH: GREEN (80) — System.currentTimeMillis in commonMain; mutable data class copy shares references.

### AVID | 9 kt
PURPOSE: Deterministic voice identifiers for UI elements — fingerprint hash + sequential global/local IDs.
HEALTH: YELLOW (72) — Random hash fallback; platform defaults to ANDROID; hashCode not deterministic on K/N.

### Gaze | 4 kt
PURPOSE: Eye/gaze tracking abstraction (ML Kit deferred).
HEALTH: YELLOW (62) — StubGazeTracker.state creates new MutableStateFlow per access.

### DeviceManager | 82 kt
PURPOSE: Device detection, capabilities, sensors, IMU, audio, Bluetooth, WiFi scanning.
HEALTH: YELLOW (48) — Simulated audio devices; wrong BatteryManager fields; iOS capabilities hardcoded wrong.

### IPC | 31 kt
PURPOSE: Inter-process communication with AVU wire format.
HEALTH: RED (38) — ConnectionManager is pure simulation; iOS returns explicit failures; unsafe cast in subscribe.

### WebSocket | 8 kt
PURPOSE: WebSocket client with reconnection and keep-alive.
HEALTH: YELLOW (62) — connect() returns success before onOpen; URL splitting bug; 3rd AVU escape duplicate.

### Rpc | 200 kt
PURPOSE: gRPC over TCP + UDS, proto generation, registry-aware routing.
HEALTH: RED (41) — PlatformClient.android is 5 empty TODO stubs; fire-and-forget close; shutdown hook accumulation.

### Whisper | 12 kt
PURPOSE: Vendored whisper.cpp Android demo (example code only).
HEALTH: GREEN (85) — Not production code; actual Whisper integration is in SpeechRecognition.

---

## Cross-Cutting Observations

### 1. AVU Escape Incompatibility (3 modules)
Three independent implementations: `AvuEscape` (percent-encoding), `AndroidIPCManager` (backslash), `AvuSyncMessage` (percent-encoding). Backslash vs percent causes wire corruption when messages cross module boundaries.

### 2. Rule 7 Epidemic (~160+ files)
AI attribution scattered across the codebase — "VOS4 Development Team" (131+ files), "Claude Code" (4 files), "AI Assistant" (1 file). All must be removed or changed to "Manoj Jhawar".

### 3. AVID Coverage Gap
~80% of UI screens have zero voice identifiers. The AVID system is well-designed but adoption is minimal.

### 4. Desktop/iOS Stub Pattern
Many modules declare iOS/Desktop targets but implement stubs that report success. Callers cannot distinguish real from fake functionality. Stubs should return failure or throw.

### 5. Test Desert
~85% of modules have zero tests. Critical dependencies (Foundation, Database, AVID, AVU, VoiceOSCore) have no unit tests despite being pure KMP logic suitable for commonTest.

### 6. System.currentTimeMillis() in commonMain
Found in: AVACode, WebAvanue (8 files), BrowserVoiceOSCallback, CommandBarAutoHide. Blocks iOS compilation. Must use expect/actual or kotlinx.datetime.

---

## Per-Module Quality Reports Index

| Report | Location |
|--------|----------|
| Foundation + Logging + Utilities + Localization + AvanuesShared | `Foundation-Infra-Review-QualityAnalysis-260222-V1.md` |
| Database | `Database-Review-QualityAnalysis-260222-V1.md` |
| AvanueUI | `AvanueUI-Review-QualityAnalysis-260222-V1.md` |
| AVID + AVU + AVACode | `AVID-AVU-AVACode-Review-QualityAnalysis-260222-V1.md` |
| DeviceManager + Gaze + VoiceIsolation | `DeviceManager-Gaze-VoiceIso-Review-QualityAnalysis-260222-V1.md` |
| IPC + WebSocket + Rpc | `IPC-WebSocket-Rpc-Review-QualityAnalysis-260222-V1.md` |
| VoiceOSCore — Handlers | `VoiceOSCore-Handlers-Review-QualityAnalysis-260222-V1.md` |
| VoiceOSCore — Scraping | `VoiceOSCore-Scraping-Review-QualityAnalysis-260222-V1.md` |
| WebAvanue | `WebAvanue-Review-QualityAnalysis-260222-V1.md` |
| SpeechRecognition + VoiceIsolation + Whisper + Voice | `Speech-Review-QualityAnalysis-260222-V1.md` |
| PluginSystem + Cockpit | `PluginSystem-Cockpit-Review-QualityAnalysis-260222-V1.md` |
| NoteAvanue + PhotoAvanue + CameraAvanue + ImageAvanue + PDFAvanue | `ContentModules-Review-QualityAnalysis-260222-V1.md` |
| HTTPAvanue + RemoteCast + VideoAvanue | `HTTP-RemoteCast-Video-Review-QualityAnalysis-260222-V1.md` |
| VoiceKeyboard + VoiceCursor + VoiceDataManager + VoiceAvanue + Actions | `VoiceInput-Review-QualityAnalysis-260222-V1.md` |
| AI (NLU + RAG + LLM + Memory + Chat + Teach + ALC) | `AI-Review-QualityAnalysis-260222-V1.md` |
| Avanues Consolidated App | `AvanuesApp-Review-QualityAnalysis-260222-V1.md` |
| Test/Utility Apps | `TestApps-Review-QualityAnalysis-260222-V1.md` |
| Build System | `BuildSystem-Review-QualityAnalysis-260222-V1.md` |

---

Author: Manoj Jhawar | 260222 | Full Codebase Review V1
