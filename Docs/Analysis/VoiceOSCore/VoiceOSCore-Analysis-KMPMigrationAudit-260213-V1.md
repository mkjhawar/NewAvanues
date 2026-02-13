# KMP Migration Audit — NewAvanues Full Codebase

**Date:** 2026-02-13
**Branch:** VoiceOSCore-KotlinUpdate
**Scope:** All 31+ modules + apps/avanues app (46 files)

---

## Executive Summary

The NewAvanues monorepo contains **31 modules** and **1 consolidated app** (apps/avanues). Of these:

- **24 modules** are already full KMP (commonMain + androidMain + iosMain/desktopMain)
- **3 modules** are partially KMP (structure exists, missing targets)
- **7 modules** are Android-only and need migration
- **~40% of app-level code** is pure business logic extractable to KMP
- **~60% of app-level code** is intrinsically Android (services, accessibility, Compose UI)

**Estimated total effort for full cross-platform parity: 16-24 weeks**

---

## Part 1: Module-by-Module Status

### Already Full KMP (No Work Needed) — 24 Modules

| Module | Source Sets | Notes |
|--------|-----------|-------|
| AvanueUI | common/android/ios/desktop | Design system, tokens, components |
| AVID | common/android/ios/desktop | Voice identifiers |
| AvidCreator | common/android/ios/desktop | AVID authoring |
| AVU | common/android/ios/desktop | Universal codec |
| Database | common/android/ios/desktop | SQLDelight, 21 .sq schemas |
| DeviceManager | common/android/ios/desktop | Device detection, IMU, audio |
| Foundation | common/android/ios/desktop | Base utilities, StateFlow, ViewModel |
| Gaze | common/android/ios/desktop | Eye tracking |
| IPC | common/android/ios/desktop | Inter-process communication |
| LicenseValidation | common/android/ios/desktop | License checking |
| Localization | common/android/ios/desktop | i18n/l10n |
| Logging | common/android/ios/desktop | Structured logging |
| PluginSystem | common/android/ios/desktop | .avp plugin runtime |
| Rpc | common/android/ios/desktop | RPC protocol |
| SpeechRecognition | common/android/ios/desktop | STT abstraction layer |
| Utilities | common/android/ios/desktop | Shared utilities |
| VoiceCursor | common/android/ios/desktop | Cursor control overlay |
| VoiceDataManager | common/android/ios/desktop | Voice data persistence |
| VoiceIsolation | common/android/ios/desktop | Audio isolation |
| VoiceOSCore | common/android/ios/desktop | Core voice command pipeline |
| WebAvanue | common/android/ios/desktop | Browser engine + DOM scraping |
| AVA/core/Utils | common/android/ios/desktop | Initializable, Result types |
| AVA/core/Domain | common/android/ios/desktop | Pure domain models |
| AVA/core/Data | common/android/ios/desktop | SQLDelight + repository pattern |

### Already Full KMP — AI Sub-Modules (3)

| Module | Source Sets | Notes |
|--------|-----------|-------|
| AI/ALC | common/android/ios/desktop/macos/linux/mingw | 7 targets! TVM (Android) + CoreML (iOS) |
| AI/NLU | common/android/ios/desktop | ONNX + TFLite + SQLDelight |
| AI/Memory | common/android/ios/desktop | Memory persistence layer |

### Partially KMP (Structure Exists, Missing Targets) — 3 Modules

| Module | Current State | What's Missing | Effort |
|--------|--------------|----------------|--------|
| **VoiceAvanue** | common + android only | iosMain, desktopMain | 1-2 weeks |
| **AI/LLM** | common + android + desktop | iosMain (needs CoreML bridge) | 3-4 weeks |
| **WebSocket** | common + android + ios (sparse) | Desktop, flesh out iOS impl | 1 week |

### Android-Only (Need Full Migration) — 7 Modules

| Module | Plugin | Key Blockers | Difficulty | Effort |
|--------|--------|-------------|-----------|--------|
| **Actions** | multiplatform (setup only) | AIDL IPC, Intent routing, Hilt | Moderate | 3-4 weeks |
| **Voice/WakeWord** | android-only | NDK/JNI, SpeechRecognizer, DataStore, Parcelable | High | 4-6 weeks |
| **AVA/Overlay** | android-only | Compose UI, WindowManager, SpeechRecognizer | High | 6-8 weeks |
| **AVACode** | multiplatform (jvmMain only) | JVM-specific, no Android or iOS | Low | 1-2 weeks |
| **AvanuesShared** | android + ios (.podspec) | CocoaPods bridge, mixed structure | Moderate | 2-3 weeks |
| **LicenseManager** | android-only | Test-only module | Low | 1 week |
| **LicenseSDK** | multiplatform (commonMain only) | Incomplete, needs platform impls | Low | 1-2 weeks |
| **VoiceKeyboard** | android-only | InputMethodService (no iOS equiv) | Very High | 8+ weeks |

### AI Sub-Modules Needing Work (3)

| Module | Current State | Key Blockers | Effort |
|--------|-------------|-------------|--------|
| **AI/Chat** | KMP structure, Compose-heavy | UI in androidMain, Hilt | 2-3 weeks |
| **AI/RAG** | KMP structure, Compose + doc parsing | PDFBox, JSoup, Apache POI | 3-4 weeks |
| **AI/Teach** | KMP structure, Compose-heavy | No iosMain yet | 2-3 weeks |

---

## Part 2: App-Level Analysis (apps/avanues — 46 files)

### Must Stay Android (8 files)

These are intrinsically tied to Android framework APIs with no cross-platform equivalent:

| File | Android APIs | Why It Can't Move |
|------|-------------|-------------------|
| `VoiceAvanueAccessibilityService.kt` | AccessibilityService, AccessibilityNodeInfo | Platform accessibility tree traversal |
| `VoiceRecognitionService.kt` | android.app.Service, Notification | Foreground service lifecycle |
| `RpcServerService.kt` | android.app.Service, NotificationManager | Foreground service + notifications |
| `CommandOverlayService.kt` | WindowManager, PixelFormat, Service | System overlay window management |
| `BootReceiver.kt` | BroadcastReceiver | Boot completion intent |
| `ElementExtractor.kt` | AccessibilityNodeInfo, Rect | Accessibility tree traversal |
| `ScreenCacheManager.kt` | AccessibilityNodeInfo, Resources | Screen fingerprinting via a11y |
| `AccessibilityClickDispatcher.kt` | GestureDescription, Path | Gesture dispatch via a11y service |

### Partially Migratable (9 files)

Business logic extractable via `expect/actual` or interface abstraction:

| File | What Moves to commonMain | What Stays on Android | Interface Needed |
|------|-------------------------|----------------------|-----------------|
| `AvanuesSettingsRepository.kt` | `data class AvanuesSettings`, key constants, migration logic | DataStore `edit {}` blocks, `Flow.map` transforms | `ISettingsStore` |
| `AvanuesDataStore.kt` | Schema definitions, Flow-based read API | DataStore initialization, Context | `IPreferenceStore` |
| `DeveloperPreferences.kt` | `data class DeveloperSettings`, defaults | DataStore operations | Reuse `ISettingsStore` |
| `SftpCredentialStore.kt` | Credential store logic, interface | EncryptedSharedPreferences | `ICredentialStore` |
| `DashboardViewModel.kt` | State aggregation, permission logic | Context.getSystemService, PackageManager | `IPermissionChecker` |
| `UnifiedSettingsViewModel.kt` | Sorting, searching, filtering (63 lines, almost all portable) | `@HiltViewModel` annotation | None (pure logic) |
| `VosSyncViewModel.kt` | Sync orchestration, state mgmt, messages | Environment.getExternalStorage | `IFileSystem` |
| `ComposableSettingsProvider.kt` | `ModuleSettingsProvider` interface contract | `@Composable` rendering | Extract interface |
| `BrowserEntryViewModel.kt` | Repository injection pattern | `@HiltViewModel` | None (thin wrapper) |

### Fully Migratable to KMP (7 files)

Pure business logic with no/minimal Android dependencies:

| File | Current Size | Destination Module | Notes |
|------|-------------|-------------------|-------|
| `DynamicCommandGenerator.kt` | ~60 lines | VoiceOSCore commonMain | Only Android dep is `Resources` for density |
| `OverlayItemGenerator.kt` | ~80 lines | VoiceOSCore commonMain | Badge generation algorithms |
| `OverlayStateManager.kt` | ~100 lines | VoiceOSCore commonMain | State management, no Android APIs |
| `AppModule.kt` | 96 lines | Keep in app, extract factories | Hilt ceremony + factory patterns |
| `SettingsModule.kt` | 71 lines | Keep in app | Pure Hilt multibinding wiring |
| `SyncModule.kt` | 53 lines | Keep in app, extract init logic | Sync manager factory |
| `VosSyncWorker.kt` | ~60 lines | Extract sync execution to VoiceOSCore | WorkManager scheduler stays Android |

### UI-Only / Compose Screens (15 files)

Stay Android. On iOS these become SwiftUI equivalents; on Desktop, Compose Desktop:

| Package | Files | Notes |
|---------|-------|-------|
| `ui/home/` | HomeScreen.kt | Dashboard composables |
| `ui/hub/` | HubDashboardScreen.kt | Master hub with 3 modes |
| `ui/browser/` | (VM only) | WebAvanue integration |
| `ui/settings/` | UnifiedSettingsScreen.kt, GlassesSettingsLayout.kt, OssLicenseRegistry.kt, 5 providers | Adaptive settings system |
| `ui/sync/` | VosSyncScreen.kt, PhraseSuggestionDialog.kt | VOS sync UI |
| `ui/developer/` | DeveloperSettingsScreen.kt, DeveloperConsoleScreen.kt | Dev-only screens |
| `ui/about/` | AboutScreen.kt | Credits & info |

---

## Part 3: Cross-Cutting Abstractions Needed

To enable KMP migration, these interface abstractions should be created in **Foundation** or **VoiceOSCore commonMain**:

| Interface | Purpose | Android Impl | iOS Impl |
|-----------|---------|-------------|----------|
| `ISettingsStore` | Preference read/write with Flow | DataStore | UserDefaults + Combine |
| `ICredentialStore` | Encrypted credential storage | EncryptedSharedPreferences | Keychain |
| `IPermissionChecker` | Runtime permission checking | PackageManager + ActivityCompat | Info.plist + AVFoundation |
| `IFileSystem` | Platform file paths & I/O | Environment + ContentResolver | FileManager |
| `INotificationManager` | Local notification delivery | NotificationManager + channels | UNUserNotificationCenter |
| `IBackgroundScheduler` | Periodic background work | WorkManager | BGTaskScheduler |
| `IOverlayWindow` | Floating window management | WindowManager + TYPE_APPLICATION_OVERLAY | UIWindow layer |
| `ISpeechRecognizerFactory` | Platform STT engine creation | android.speech.SpeechRecognizer | SFSpeechRecognizer |
| `IAccessibilityBridge` | UI element discovery | AccessibilityService + NodeInfo | UIAccessibility APIs |

---

## Part 4: Recommended Migration Roadmap

### Phase 1: Foundation Abstractions (2-3 weeks)
**Priority: Highest — unblocks all other phases**

1. Create `ISettingsStore` in Foundation commonMain
2. Create `ICredentialStore` in Foundation commonMain
3. Create `IPermissionChecker` in Foundation commonMain
4. Create `IFileSystem` in Foundation commonMain
5. Move `AvanuesSettings` data class to Foundation commonMain
6. Move `DeveloperSettings` data class to Foundation commonMain

### Phase 2: App Logic Extraction (2-3 weeks)
**Priority: High — enables shared ViewModel/state logic**

1. Move `OverlayStateManager` to VoiceOSCore commonMain
2. Move `DynamicCommandGenerator` algorithms to VoiceOSCore commonMain
3. Extract `UnifiedSettingsViewModel` logic to Foundation commonMain
4. Extract `VosSyncViewModel` orchestration to VoiceOSCore commonMain
5. Extract `VosSyncWorker` sync execution to VoiceOSCore commonMain

### Phase 3: Android-Only Module Migration (6-10 weeks)
**Priority: Medium — expands platform reach**

| Order | Module | Effort | Rationale |
|-------|--------|--------|-----------|
| 1 | **Actions** | 3-4 wk | Lowest risk, high value, depends on already-KMP modules |
| 2 | **AI/Chat** | 2-3 wk | Separate UI from state management, reuse patterns from Phase 2 |
| 3 | **AI/RAG** | 3-4 wk | Document parsing needs platform alternatives (iOS: PDFKit, Core Spotlight) |
| 4 | **AI/LLM** | 3-4 wk | Create iosMain with CoreML inference bridge (follow ALC's pattern) |
| 5 | **AI/Teach** | 2-3 wk | Compose separation, create iosMain |

### Phase 4: High-Complexity Modules (8-14 weeks)
**Priority: Lower — significant rewrite required**

| Order | Module | Effort | Rationale |
|-------|--------|--------|-----------|
| 1 | **Voice/WakeWord** | 4-6 wk | NDK/JNI → iOS Framework bridge, duplicate code cleanup |
| 2 | **AVA/Overlay** | 6-8 wk | Full UI rewrite per platform (Compose → SwiftUI) |
| 3 | **VoiceKeyboard** | 8+ wk | InputMethodService has no iOS equivalent (iOS keyboard extensions are completely different) |

### Phase 5: Cleanup & Consolidation (2 weeks)
1. Migrate **AVACode** jvmMain → commonMain
2. Complete **LicenseSDK** platform implementations
3. Consolidate **AvanuesShared** into proper KMP structure
4. Retire **LicenseManager** or fold into LicenseValidation
5. Flesh out **WebSocket** iOS/desktop implementations

---

## Part 5: Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Accessibility tree traversal has no iOS equivalent | High | iOS uses UIAccessibility APIs — different paradigm. Create `IAccessibilityBridge` with platform-specific discovery strategies |
| Compose UI can't be shared to iOS | Medium | Accept per-platform UI. Share ViewModels + state. Use Compose Multiplatform for Desktop |
| AIDL/Intent IPC is Android-exclusive | Medium | iOS uses XPC/URL schemes. Abstract behind `IServiceBridge` |
| WorkManager has no iOS equivalent | Low | iOS BGTaskScheduler is simpler but functional. Abstract behind `IBackgroundScheduler` |
| NDK native libraries (WakeWord) | High | Requires building iOS Frameworks from same C/C++ source. Verify source availability |
| Hilt DI is Android-only | Low | Use interface-based DI in commonMain. Koin or manual DI for iOS |

---

## Part 6: Module Dependency Graph (Migration Order)

```
Foundation (KMP) ← ISettingsStore, IPermissionChecker, IFileSystem
    ↑
AVA/core/* (KMP) ← Already done
    ↑
Database (KMP) ← Already done
    ↑
VoiceOSCore (KMP) ← OverlayStateManager, DynamicCommandGenerator
    ↑
Actions (migrate) ← IServiceBridge, ActionCommand abstraction
    ↑
AI/* (migrate) ← Separate UI from logic
    ↑
Voice/WakeWord (migrate) ← ISpeechRecognizerFactory, NDK bridge
    ↑
AVA/Overlay (migrate) ← IOverlayWindow, platform UI
    ↑
apps/avanues (Android) ← Thin shell over KMP modules
apps/ios (future) ← SwiftUI shell over same KMP modules
```

---

## Summary Statistics

| Category | Count | % of Codebase |
|----------|-------|---------------|
| Already Full KMP | 27 modules | ~70% |
| Partially KMP | 3 modules | ~8% |
| Android-Only (needs migration) | 7 modules | ~18% |
| Container/Meta modules | 3 modules | ~4% |

| App Code Category | Files | Lines (est.) |
|-------------------|-------|-------------|
| Must stay Android | 8 | ~800 |
| Partially migratable | 9 | ~600 |
| Fully migratable | 7 | ~450 |
| UI-only (Compose) | 15 | ~1,200 |
| Root (App/Activity) | 2 | ~250 |
| **Total** | **46** | **~3,300** |
