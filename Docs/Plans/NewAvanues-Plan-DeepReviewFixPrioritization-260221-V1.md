# Deep Review Fix Prioritization Plan

**Module**: All (cross-repo)
**Branch**: VoiceOS-1M (base)
**Date**: 2026-02-21
**Source**: 32 deep review reports + 4 consolidated reviews (~1,100 total findings)
**Scope**: Critical + High findings only (estimated ~400 across all modules)

---

## Already Fixed (SpeechRecognition Phase 0 on VoiceOS-1M-SpeechEngine)

These 10 items were fixed in commit `7939e80e` and do NOT need rework:

| # | Finding | Status |
|---|---------|--------|
| 1 | Hardcoded Firebase credentials in SpeechRecognition | FIXED |
| 2 | Zip path traversal (zip slip) in FileZipManager | FIXED |
| 3 | Global crash handler overriding Crashlytics | FIXED |
| 4 | VOSK confidence sigmoid inversion | FIXED |
| 5 | Silence detection callback firing every 100ms | FIXED |
| 6 | Firebase singleton DCL missing @Volatile | FIXED |
| 7 | Desktop LISTENING without audio capture | FIXED |
| 8 | AndroidSTT destroy order (scope vs recognizer) | FIXED |
| 9 | setLanguage() not propagating to engine | FIXED |
| 10 | VoiceStateManager callback deadlock | FIXED |

Also fixed in `83bfdc50`: AI attribution removal (3 SpeechRecognition files).

---

## Fix Batches (Priority Order)

### BATCH 1: Security Vulnerabilities (P0 — Fix Immediately)

| ID | Module | Severity | Description | File |
|----|--------|----------|-------------|------|
| S1 | WebAvanue | CRITICAL | JS injection — `clickBySelector()`, `focusElement()`, `inputText()`, `scrollToElement()` inject unescaped strings into JavaScript | `WebAvanueVoiceOSBridge.kt` |
| S2 | WebAvanue | CRITICAL | JS injection — `findInPage()` injects raw query into JS (Android + iOS) | `AndroidWebViewController.kt:173`, `IOSWebView.kt:224` |
| S3 | WebAvanue | CRITICAL | AES/CBC with deterministic IV from MD5 — breaks confidentiality | `SecureScriptLoader.kt:72-73` |
| S4 | HTTPAvanue | CRITICAL | Static file middleware — no `..` traversal guard, arbitrary file read | `StaticFileMiddleware.kt:14` |
| S5 | HTTPAvanue | CRITICAL | Rate limiter trusts `X-Forwarded-For` without validation — trivial bypass | `RateLimitMiddleware.kt:18-19` |
| S6 | AI-LLM | CRITICAL | Path traversal in TAR extraction — no `../` validation | `ALMExtractor.kt:230` |
| S7 | AI-LLM | CRITICAL | Google AI API key leaked to logs as query parameter | `GoogleAIProvider.kt:318` |
| S8 | AI-LLM | CRITICAL | Ollama JSON injection — model name interpolated without escaping | `OllamaProvider.kt` |
| S9 | AI-ALC | CRITICAL | AVA3 `deriveNonce()` uses MD5 for AES-CTR nonce — broken crypto | `AVA3Decoder.kt:295` |
| S10 | AI-ALC | CRITICAL | AVA3 hash mismatch logs warning and continues — corrupted files accepted | `AVA3Decoder.kt:172-174` |
| S11 | AI-RAG | CRITICAL | `AONFileManager.verifySignature()` doesn't verify HMAC — tamper protection is theater | `AONFileManager.kt:452-481` |
| S12 | AI-RAG | CRITICAL | Hardcoded `MASTER_KEY` in source — forgeable signatures | `AONFileManager.kt:86-89` |
| S13 | Foundation | CRITICAL | `DEFAULT_HOST_KEY_MODE = "no"` — MITM-enabling default | `AvanuesSettings.kt:74` |
| S14 | VoiceOSCore | CRITICAL | SFTP `hostKeyChecking = "no"` in all 4 call sites | `VosSftpClient.kt:54` |
| S15 | Foundation | HIGH | `DesktopCredentialStore` uses Base64 not encryption | `DesktopCredentialStore.kt:26-29` |
| S16 | WebAvanue | HIGH | `SecureStorage` defaults to unencrypted plaintext | `SecureStorage.kt` |
| S17 | WebAvanue | HIGH | Credential operations logged to Logcat | `SecureStorage.kt` |
| S18 | WebAvanue | HIGH | Raw TCP server on port 50055 with no auth or TLS | `WebAvanueJsonRpcServer.kt` |
| S19 | PluginSystem | HIGH | Signature verification failure is non-blocking warning — unsigned plugins install | `PluginInstaller.kt:107-111` |
| S20 | AI-LLM | CRITICAL | Absolute developer machine paths hardcoded in ModelSpec | `ModelSelector.kt:46,58,102,112` |

**Estimated effort**: 3-4 sessions
**Blast radius**: High — affects data integrity, user privacy, system security

---

### BATCH 2: Data Corruption & Silent Data Loss (P0)

| ID | Module | Severity | Description | File |
|----|--------|----------|-------------|------|
| D1 | Database | CRITICAL | `transaction()` never executes block — lambda cast not invoked | `VoiceOSDatabaseManager.kt:332-338` |
| D2 | Database | HIGH | `QualityMetricRepository` maps wrong DTO type — ClassCastException | `SQLDelightElementCommandRepository.kt:137` |
| D3 | Database | HIGH | `migrateV3ToV4()` is empty — FK migration never runs | `DatabaseMigrations.kt:214-227` |
| D4 | Database | HIGH | `updateSynonyms()` manual JSON string interpolation — special char corruption | `SQLDelightScrapedWebCommandRepository.kt:112` |
| D5 | Database | HIGH | Desktop schema creation crashes on 2nd launch — unconditional create() | `DatabaseFactory.desktop.kt:34` |
| D6 | Database | HIGH | `AppConsentHistoryRepository.insert()` returns table size not row ID | `SQLDelightAppConsentHistoryRepository.kt:37-39` |
| D7 | Foundation | CRITICAL | `ViewModelState.update()` non-atomic — concurrent race = data loss | `ViewModelState.kt:49-51` |
| D8 | Foundation | CRITICAL | `UserDefaultsSettingsStore.update()` non-atomic race | `UserDefaultsSettingsStore.kt:49-55` |
| D9 | Foundation | CRITICAL | `NumberToWords.convert(Long.MIN_VALUE)` overflows — infinite recursion | `NumberToWords.kt:205` |
| D10 | Cockpit | CRITICAL | Signature renderer never writes `signatureData` — drawn sig permanently lost | `ContentRenderer.kt:142` |
| D11 | VoiceOSCore | HIGH | Browser `forward` action calls `GLOBAL_ACTION_BACK` — navigates backward | `ActionFactory.kt:880-934` |
| D12 | VoiceOSCore | HIGH | `TextHandler` "delete" erases entire field — data loss | `TextHandler.kt:105-121` |
| D13 | VoiceOSCore | HIGH | `CommandStorage.deleteNamespace()` does not delete anything | `CommandPersistence.kt:190-202` |

**Estimated effort**: 2-3 sessions
**Blast radius**: High — silent data corruption in production

---

### BATCH 3: Crash Bugs & Deadlocks (P1)

| ID | Module | Severity | Description | File |
|----|--------|----------|-------------|------|
| C1 | HTTPAvanue | CRITICAL | HTTP/2 concurrent streams share unsynchronized sink — frame corruption | `Http2Connection.kt:122,131,230` |
| C2 | HTTPAvanue | CRITICAL | Huffman decoder not implemented — browsers get garbage headers | `HpackDecoder.kt:108-114` |
| C3 | VoiceOSCore | CRITICAL | `DynamicCommandRegistry` ReentrantReadWriteLock in suspend funs — deadlock | `DynamicCommandRegistry.kt:84+` |
| C4 | VoiceOSCore | CRITICAL | `HUDManager.registerConsumer()` writes Compose state from arbitrary thread | `HUDManager.kt:100,184-205` |
| C5 | VoiceOSCore | CRITICAL | `SpatialRenderer` mutable maps without synchronization — ConcurrentModificationException | `SpatialRenderer.kt:32-33` |
| C6 | VoiceOSCore | CRITICAL | `HUDRenderer` accesses list from canvas + Default dispatcher — mixed threading | `HUDRenderer.kt:50-60` |
| C7 | AI-Chat | CRITICAL | Non-reentrant Mutex deadlock in `deleteConversation()` | `ConversationManagerDesktop.kt:204` |
| C8 | AI-Chat | CRITICAL | `json.encodeToString(Map<String, Any>)` throws SerializationException on every desktop export | `ExportCoordinatorDesktop.kt:300` |
| C9 | AI-LLM | CRITICAL | `TVMModelLoader` collects `.first()` before download completes — crash | `TVMModelLoader.kt:62` |
| C10 | AI-LLM | CRITICAL | KV cache no-op — every generation O(context_length) | `BackpressureStreamingManager.kt:323-338` |
| C11 | AI-NLU | CRITICAL | `ModelManager.detectBestModel()` throws in init — crashes before deployment | `ModelManager.kt:164` |
| C12 | AI-RAG | CRITICAL | `EmbeddingEncryptionManager.generateKey()` throws in init — constructor crash | `EmbeddingEncryptionManager.kt` |
| C13 | DeviceManager | CRITICAL | `SensorFusionManager` shared mutable state — concurrent sensor thread access | `SensorFusionManager.kt` |
| C14 | DeviceManager | CRITICAL | `CalibrationManager.calibrationData.add()` commented out — always throws | `CalibrationManager.kt:121` |
| C15 | DeviceManager | CRITICAL | `BiometricManager.authenticateWithDeviceCredential()` never calls callback — hangs | `BiometricManager.kt:976-987` |
| C16 | AvidCreator | CRITICAL | `runBlocking` on AIDL Binder thread — ANR | `AvidServiceBinder.kt:260,294` |
| C17 | AvidCreator | CRITICAL | `AccessibilityFingerprint` leaks AccessibilityNodeInfo — never recycled | `AccessibilityFingerprint.kt:128-164` |
| C18 | Apps-Avanues | CRITICAL | `runBlocking` in VoiceControlCallbacks — ANR/deadlock | `VoiceAvanueAccessibilityService.kt:275-317` |
| C19 | Apps-Legacy | CRITICAL | Race condition on `OverlayStateManager.avidToNumber` — unsynchronized LinkedHashMap | `OverlayStateManager.kt:239-240` |
| C20 | AVA | CRITICAL | `AVA3Decoder.decompress()` fixed 4x buffer — truncates incompressible data | `AVA3Decoder.kt:378-390` |
| C21 | AVA | CRITICAL | `AvaIntegrationBridge` CoroutineScope with no lifecycle — guaranteed leak + NPE | `AvaIntegrationBridge.kt:44` |
| C22 | Cockpit | CRITICAL | `Dispatchers.Main` in KMP commonMain — crashes Desktop | `CockpitViewModel.kt:39` |
| C23 | Cockpit | CRITICAL | `screenWidthPx`/`screenHeightPx` plain var read in coroutine — JVM data race | `SpatialViewportController.kt:55` |
| C24 | AVA | CRITICAL | `DialogQueueManager.removeFromQueue()` returns false inside launch lambda | `DialogQueueManager.kt:208-223` |

**Estimated effort**: 4-5 sessions
**Blast radius**: Medium-High — crashes, ANRs, deadlocks

---

### BATCH 4: Non-Functional Modules (Entire Module Stubs) (P2)

| ID | Module | Severity | Description |
|----|--------|----------|-------------|
| NF1 | IPC | CRITICAL | Entire module is stubs — Android/iOS/Desktop all throw or return failure. 20+ critical/high findings. **Decision: delete or implement?** |
| NF2 | Rpc | CRITICAL | Android/iOS/Desktop `PlatformClient.connect*()` all empty — connects to nothing. `PluginServiceGrpcClient` returns fake data. |
| NF3 | AI-ALC | CRITICAL | iOS `CoreMLRuntime.predict()`, macOS, Linux, Windows inference all return uniform distributions — non-functional on all non-Android platforms |
| NF4 | AI-NLU | CRITICAL | iOS `CoreMLModelManager.runInference()` returns zero-vector — all iOS classifications wrong |
| NF5 | AI-RAG | CRITICAL | `SimpleTokenizer.wordToId()` uses hash instead of BERT vocab — all embeddings semantically invalid |
| NF6 | AI-RAG | CRITICAL | `ChunkEmbeddingHandler.updateChunkEmbeddingInternal()` is complete no-op |
| NF7 | VoiceOSCore | CRITICAL | `VoiceOSRpcServer.bindService()` returns empty ServiceDefinition — no gRPC methods |
| NF8 | VoiceOSCore | CRITICAL | `loadTrustedSignatures()` is no-op — plugin trust system broken |
| NF9 | VoiceOSCore | HIGH | `NoteCommandHandler`, `AICommandHandler`, `CastCommandHandler` — all dispatch stubs or always-fail |
| NF10 | WebAvanue | CRITICAL | `WebViewFactory.createWebView()` throws NotImplementedError |
| NF11 | AvanueUI-State | CRITICAL | 14 `TODO("Platform rendering")` in Data module (Avatar, Table, DataGrid, etc.) |
| NF12 | AvanueUI-Core | CRITICAL | 27 DSL `render()` methods throw `TODO()` — uncatchable Error |
| NF13 | AvanueUI-Renderers | CRITICAL | 12 Android adapter composables have empty bodies |
| NF14 | AvanueUI-Renderers | CRITICAL | Desktop renderers show hardcoded strings ("Container", "Row content") |
| NF15 | AvanueUI-State | CRITICAL | `Date.now()` returns hardcoded `Date(2025, 12, 28)`, `Time.now()` returns noon |
| NF16 | AVA | CRITICAL | `ModelScanner.BASE_PATH = "/sdcard/ava-ai-models"` — blocked by scoped storage |

**Estimated effort**: Depends on scope decision (delete stubs vs implement)
**Decision needed**: Which non-functional modules are actually needed?

---

### BATCH 5: KMP Compilation Violations (P2)

These will cause **compile failures on non-JVM targets** (iOS, JS, Native):

| ID | Module | File | JVM-Only API Used |
|----|--------|------|-------------------|
| K1 | AvanueUI-Core | `Types3D.kt:123,128,133,213` | `Math.toRadians()` |
| K2 | AvanueUI-Core | `NotificationCenter.kt:113` | `System.currentTimeMillis()` |
| K3 | AvanueUI-Voice | `DatePickerHandler.kt` | `java.text.SimpleDateFormat`, `java.util.Calendar` |
| K4 | AvanueUI-Voice | `FileUploadHandler.kt` | `android.net.Uri` in commonMain |
| K5 | AvanueUI-Voice | `TimePickerHandler.kt` | `java.util.Calendar`, `java.util.Locale` |
| K6 | AvanueUI-Voice | `RangeSliderHandler.kt:1033` | `String.format()` |
| K7 | AvanueUI-Voice | `SliderHandler.kt:548` | `String.format()` |
| K8 | AvanueUI-Voice | `StepperHandler.kt:819,882` | `String.format()` |
| K9 | AvanueUI-Voice | `ToastHandler.kt` | `java.util.concurrent.ConcurrentLinkedDeque` |
| K10 | AvanueUI-Root | `ThemeRepository.kt:201,215,229,241` | `java.io.File` in commonMain |
| K11 | AvanueUI-State | `StateManager.kt:96` | `System.currentTimeMillis()` |
| K12 | AvanueUI-State | `InputComponents.kt:239-259` | Hardcoded Date/Time (see NF15) |
| K13 | AI-ALC/Teach | `TrainingAnalytics.kt:211` | `System.currentTimeMillis()` + `String.format()` |
| K14 | AI-NLU | `HybridIntentClassifier.kt:218-221` | `currentTimeMillis()` hardcoded to `0L` |
| K15 | PhotoAvanue | `PhotoAvanueScreen.kt:218,643` | `String.format()` (2 uses) |
| K16 | PhotoAvanue | `ProCameraState.kt:82` | `String.format()` |
| K17 | VideoAvanue | `VideoItem.kt:22-23` | `String.format()` |
| K18 | Cockpit | `CockpitViewModel.kt:39` | `Dispatchers.Main` (no main dispatcher on Desktop) |
| K19 | PluginSystem | iOS `PermissionStorage` | Completely different API than expect class |
| K20 | AvanueUI-Renderers | `OptimizedSwiftUIRenderer.kt` | `System.currentTimeMillis()` in iosMain |
| K21 | AVU | `ExecutionContextTest.kt:71` | Wrong package import — test won't compile |
| K22 | AVU | `AvuDslHighlighterTest.kt:140-169` | Wrong package imports |
| K23 | WebAvanue | iOS `WebViewPoolManager` | expect/actual API mismatch |

**Estimated effort**: 2 sessions (mostly mechanical — replace JVM APIs with KMP equivalents)
**Fix pattern**: `System.currentTimeMillis()` → `Clock.System.now().toEpochMilliseconds()`, `String.format()` → manual formatting or expect/actual, `java.io.File` → `kotlinx.io` or expect/actual

---

### BATCH 6: Threading & Concurrency (P2)

| ID | Module | Description | File |
|----|--------|-------------|------|
| T1 | IPC | `connections` map accessed from 10+ locations without mutex | `ConnectionManager.kt:79` |
| T2 | WebAvanue | `commandGenerator` collections accessed concurrently without sync | `BrowserVoiceOSCallback.kt` |
| T3 | WebAvanue | `WebViewPool.get()` not synchronized — race during eviction | `WebViewLifecycle.kt` |
| T4 | WebAvanue | `blockedCount` non-volatile in AdBlocker + TrackerBlocker | `AdBlocker.kt`, `TrackerBlocker.kt` |
| T5 | VoiceOSCore | Two `GlobalScope.launch()` calls in CommandCache — coroutine leak | `CommandCache.kt:134,375` |
| T6 | HTTPAvanue | `SseConnectionManager` mutable map without sync | `SseEmitter.kt:72` |
| T7 | HTTPAvanue | `activeConnections` mutableSet without sync | `HttpServer.kt:69-71` |
| T8 | AI-LLM | `isGenerating` plain non-atomic var — data race | `CloudLLMProvider.kt:134` |
| T9 | AI-LLM | `DownloadJob` mutable fields without mutex | `ModelDownloadManager.kt:547` |
| T10 | AI-NLU | `HybridClassifier` mutable fields without sync | `HybridClassifier.kt:53-64` |
| T11 | AI-NLU | `CommandMatchingService` collections without sync | `CommandMatchingService.kt:62-69` |
| T12 | AI-ALC | `isGenerating`, `shouldStop` plain vars — race | `ALCEngineAndroid.kt` |
| T13 | AVU | `AvuCodeRegistry` global mutableMap unsynchronized | `AvuCodeRegistry.kt:31-32` |
| T14 | AVU | `PluginRegistry` maps without sync | `PluginRegistry.kt:22-23` |
| T15 | AvidCreator | `SQLDelightAvidRepositoryAdapter.loadCache()` double-populate race | `SQLDelightAvidRepositoryAdapter.kt` |
| T16 | AvidCreator | `nameIndex.getOrPut()` returns mutable set — concurrent race | `AvidCreator.kt` |
| T17 | DeviceManager | `IMUManager` pre-allocated rotation matrices — concurrent buffer corruption | `IMUManager.kt:89-91` |
| T18 | DeviceManager | `IMUManager.onSensorChanged()` spawns 120 coroutines/sec | `IMUManager.kt:342` |
| T19 | Foundation | `JavaPreferencesSettingsStore` TOCTOU race on `isUpdating` | `JavaPreferencesSettingsStore.kt:39-45` |
| T20 | Actions | `voiceOSService` not @Volatile — data race | `VoiceOSConnection.kt:102` |

**Estimated effort**: 3 sessions
**Fix pattern**: `@Volatile`, `Mutex`, `ConcurrentHashMap`, `stateIn` with proper scope

---

### BATCH 7: Theme Violations — `MaterialTheme` instead of `AvanueTheme` (P3)

Pervasive Rule 3 violations. **30+ files** across:

| Module | Approximate Count |
|--------|-------------------|
| AvanueUI-Renderers (Android) | 15+ files with `MaterialTheme.colorScheme.*` |
| AvanueUI-Root/ThemeBuilder | 13 references |
| AvanueUI-State/Adapters | 4 references |
| VoiceOSCore (CommandManagerActivity, LocalizationManagerActivity) | 3 activities |
| AvidCreator (AvidManagerActivity) | 1 activity |
| AI-Chat (ChatScreen snackbar) | 1 file |
| AI-Teach | All UI files |
| Apps-Legacy (VoiceOS, WebAvanue, VoiceRecognition) | 10+ files |
| Apps-Avanues (VoiceAvanue theme) | 2 files |

**Estimated effort**: 2-3 sessions (mechanical search-and-replace with verification)
**Fix pattern**: `MaterialTheme.colorScheme.*` → `AvanueTheme.colors.*`, wrap in `AvanueThemeProvider`

---

### BATCH 8: AI Attribution Violations — Rule 7 (P3)

| ID | Module | File | Attribution Text |
|----|--------|------|-----------------|
| A1 | AI-LLM | `TemplateResponseGenerator.kt:25` | "Author: Claude Code (Agent 3)" |
| A2 | AI-LLM | `LLMContextBuilder.kt:18` | "Author: Claude Code (Agent 3)" |
| A3 | AI-LLM | `CommandInterpretation.kt:10-11` | "Author: Claude (VoiceOSCore AI Integration Phase 2)" |
| A4 | AI-NLU | `OnnxSessionManager.kt:37` | "@author Manoj Jhawar / Claude AI" |
| A5 | Apps-iOS | 5 Chat Swift files | "@author iOS RAG Chat Integration Specialist (Agent 1)" |
| A6 | Actions | `ActionsManager.kt:56` | "@author AVA AI Team" |

**Estimated effort**: 30 minutes (simple deletion)

---

### BATCH 9: Resource Leaks & Lifecycle (P3)

| ID | Module | Description | File |
|----|--------|-------------|------|
| L1 | Foundation | `JavaPreferencesSettingsStore` listener never removed | `JavaPreferencesSettingsStore.kt:36-45` |
| L2 | Foundation | `UserDefaultsSettingsStore` observer never removed | `UserDefaultsSettingsStore.kt:37-47` |
| L3 | AI-NLU | `IntentClassifier` creates fresh DB connections in 10+ methods | `IntentClassifier.kt:568-622` |
| L4 | AI-ALC | `HttpClient` created but never closed | `BaseCloudProvider.kt` |
| L5 | AI-RAG | `ONNXEmbeddingProvider.close()` never closes `ortEnvironment` | `ONNXEmbeddingProvider.android.kt` |
| L6 | AI-LLM | `dispose()` stub — TVM native resources never released | `TVMRuntime.kt:390-391` |
| L7 | Rpc | `EpollEventLoopGroup` thread pool never shut down (2 locations) | `PluginServiceGrpcClient.kt:484`, `AvaGrpcClient.kt:262` |
| L8 | DeviceManager | `IMUDataPool.acquire()` never calls `release()` — pool drains | `IMUDataPool.kt` |
| L9 | VideoAvanue | ExoPlayer listener added but never removed | `VideoPlayer.kt:107-116` |
| L10 | NoteAvanue | `NoteRAGIndexer` private CoroutineScope — lifecycle leak | `NoteRAGIndexer.kt:43` |
| L11 | SmallModules | `Files.walk()` Stream never closed — file descriptor leak | `DesktopImageController.kt:60` |
| L12 | PluginSystem | `PluginLifecycleManager.manage()` Job discarded — never cancelled | `PluginLifecycleManager.kt:69-84` |

**Estimated effort**: 2 sessions

---

### BATCH 10: Hardcoded Stubs & Fake Data in Production (P3)

| ID | Module | Description | File |
|----|--------|-------------|------|
| F1 | VoiceOSCore | `handlerSupportsApp()` always returns true | `IntentDispatcher.kt:213` |
| F2 | VoiceOSCore | `calculateAverageFPS()` hardcoded to 60.0f | `HUDManager.kt:644-647` |
| F3 | VoiceOSCore | `parseOrientationData()` always returns zero-orientation | `SpatialRenderer.kt:247-256` |
| F4 | VoiceOSCore | `calibrateSpatialMapping()` returns fake success | `HUDManager.kt:517-529` |
| F5 | VoiceOSCore | IMU data hardcoded to zeros in HUDManager | `HUDManager.kt:363-378` |
| F6 | VoiceOSCore | `PreferenceLearner.updatePriorities()` is logging-only no-op | `PreferenceLearner.kt:298-320` |
| F7 | VoiceOSCore | `autoResolveConflicts()` always returns false | `DynamicCommandRegistry.kt:580-587` |
| F8 | VoiceOSCore | Voice Test uses random hardcoded command | `CommandViewModel.kt:142-172` |
| F9 | DeviceManager | `scanBluetoothDevices()` returns hardcoded fake data | `DeviceViewModel.kt:464` |
| F10 | DeviceManager | `scanWiFiNetworks()` returns hardcoded fake data | `DeviceViewModel.kt:492` |
| F11 | DeviceManager | `ComplementaryFilter`/`KalmanFilter` return identity quaternion | `SensorFusionManager.kt` |
| F12 | AvidCreator | `mockElements`/`mockHistory` production mock data | `AvidViewModel.kt` |
| F13 | AvidCreator | `delay(500)` artificial loading simulation | `AvidViewModel.kt` |
| F14 | Cockpit | `onGenerateSummary = { /* TODO */ }` — button is no-op | `ContentRenderer.kt:203` |
| F15 | Cockpit | `onMinimize = {}`/`onMaximize = {}` — controls are silent no-ops | `LayoutEngine.kt:663` |
| F16 | Cockpit | CommandBar wrong icons for 5 actions | `CommandBar.kt:179,200,205,206` |
| F17 | Apps-Legacy | `getScreenBounds()` hardcoded to 1920x1080 | `MagicWindowSystem.kt:554-556` |
| F18 | Logging | `PIIRedactionHelper` NAME_PATTERN matches every 2-word phrase | `PIIRedactionHelper.kt:51-54` |
| F19 | Logging | ZIP_CODE_PATTERN matches port numbers and error codes | `PIIRedactionHelper.kt:63-66` |

**Estimated effort**: 3 sessions

---

## Summary

| Batch | Priority | Category | Finding Count | Sessions | Status |
|-------|----------|----------|---------------|----------|--------|
| 1 | P0 | Security Vulnerabilities | 20 | 3-4 | **DONE** (260221, `1bc545a7`) |
| 2 | P0 | Data Corruption / Loss | 12 | 1 | **DONE** (260221, `91a6ce18`) — D2 false positive |
| 3 | P1 | Crashes & Deadlocks | 24 | 4-5 | Pending |
| 4 | P2 | Non-Functional Modules | 16 | Decision needed | Pending |
| 5 | P2 | KMP Compilation | 23 | 2 | Pending |
| 6 | P2 | Threading / Concurrency | 20 | 3 | Pending |
| 7 | P3 | Theme Violations | 30+ | 2-3 | Pending |
| 8 | P3 | AI Attribution | 53 | 0.5 | **DONE** (260221, `2b6cd3c8` + `0bb87e3f`) |
| 9 | P3 | Resource Leaks | 12 | 2 | Pending |
| 10 | P3 | Hardcoded Stubs | 19 | 3 | Pending |
| | | **TOTAL** | **~185** | **~25** | **3/10 done** |

---

## Recommended Execution Order

1. **Batch 8** first (30 min) — AI attribution is zero-tolerance, trivial to fix
2. **Batch 1** (S1-S20) — Security is highest blast radius
3. **Batch 2** (D1-D13) — Data corruption affects all users
4. **Batch 3** (C1-C24) — Crashes degrade UX
5. **Batch 5** (K1-K23) — KMP violations block iOS/Desktop builds
6. **Batch 4** — Requires decision on which modules to keep vs delete
7. **Batches 6-10** — Lower urgency, fix during feature work

## Decision Points Required

1. **IPC Module** — Delete entirely or implement? Currently 100% stub.
2. **Rpc PlatformClient** — All platforms are empty connect stubs. Delete or implement?
3. **AI-ALC non-Android** — iOS/macOS/Linux/Windows inference all return uniform distributions. Accept as future work or delete?
4. **AvanueUI DSL** — 27+ `TODO()` throws in Core + 14 in Data + 12 empty adapter composables. Is the DSL renderer system still needed or is it abandoned?
5. **PluginSystem** — Pervasive stubs (version validation, persistence, class loading). Keep or shelve?
