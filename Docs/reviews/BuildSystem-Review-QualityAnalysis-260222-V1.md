# Build System & Cross-Cutting Quality Report — 260222

## Summary

SCORE: 62/100 | HEALTH: YELLOW

The build system is structurally sound with a clean version catalog, parallel builds, and
properly namespaced KMP source sets. Major concerns are: (1) a complete absence of unit test
source sets across ~35 of ~50 active modules, (2) inconsistent compileSdk spread (34 vs 35
across modules), (3) hard-wired version strings bypassing the catalog in two modules,
(4) `android.enableJetifier=true` serving no function in a fully-AndroidX codebase, and
(5) zombie/disabled module entries in settings.gradle.kts whose build files still reference
dead project paths, creating a maintenance hazard. No circular dependencies were detected in
the active dependency graph.

---

## Build Configuration

| Key | Version | Latest (as of 260222) | Status |
|-----|---------|----------------------|--------|
| Kotlin | 2.1.0 | 2.1.20 | Slightly behind — 2.1.20 has coroutine debugger fixes |
| AGP (Android Gradle Plugin) | 8.2.0 | 8.9.0 | Outdated — 8.5+ has AGP-KSP stability improvements |
| Compose Multiplatform | 1.7.3 | 1.7.3 | Current |
| Compose BOM | 2024.12.01 | 2025.01.00 | One BOM release behind |
| Gradle | (inferred from AGP 8.2 compat) ~8.6 | 8.14 | Behind — aligns with AGP 8.2 requirement |
| KSP | 2.1.0-1.0.29 | 2.1.0-1.0.29 | Current for Kotlin 2.1.0 |
| SQLDelight | 2.0.1 | 2.0.2 | One patch behind |
| kotlinx-coroutines | 1.8.1 | 1.10.1 | Behind — 1.9+ has structured concurrency improvements |
| kotlinx-serialization | 1.6.0 | 1.8.0 | Behind — 1.7+ has CBOR fixes, polymorphism improvements |
| kotlinx-datetime | 0.5.0 | 0.6.1 | Behind — 0.6 adds ISO 8601 period parsing |
| Hilt | 2.54 | 2.56 | Slightly behind |
| Ktor | 2.3.7 | 3.1.2 | Major version behind — Ktor 3.x rewrites HTTP engine |
| TensorFlow Lite | 2.14.0 | 2.18.0 | Behind (AI module risk) |
| ONNX Runtime | 1.17.0 | 1.22.0 | Behind |
| Media3 | 1.2.1 | 1.5.1 | Behind — 1.4+ has HDR10 and DASH fixes |
| Coil | 2.6.0 | 3.1.0 | Major version behind — Coil 3 rewrites coroutine integration |
| gRPC | 1.62.2 | 1.71.0 | Behind — 1.65+ has critical flow control fix |
| Wire | 5.4.0 | 5.4.0 | Current |
| Dokka | 1.9.10 | 2.0.0 | Major version behind |
| Robolectric | 4.11.1 | 4.14.1 | Behind |
| MockK | 1.13.9 | 1.14.0 | One minor behind |
| androidx-lifecycle | 2.6.2 | 2.9.0 | 2 majors behind — 2.8+ has SavedStateHandle KMP support |
| androidx-core | 1.12.0 | 1.16.0 | Behind |
| androidx-navigation | 2.7.6 | 2.9.0 | Behind |
| androidx-security-crypto | 1.1.0-alpha06 | 1.1.0-alpha06 | Alpha — no stable release exists yet |
| kotlinx-atomicfu | 0.23.2 | 0.27.0 | Behind |
| material3-adaptive | 1.0.0-beta01 | 1.1.0 | Behind |
| jsch (SFTP) | 0.2.16 | 0.2.21 | Behind — 0.2.19 has EC key generation fix |

---

## Dependency Graph

### Infrastructure / Foundation Layer (no internal project deps)
```
:Modules:Logging                 — leaf (no project deps)
:Modules:Foundation              — leaf
:Modules:AVID                    — leaf
:Modules:Utilities               — leaf
:Modules:LicenseSDK              — depends on :Modules:Utilities
:Modules:AVU                     — depends on :Modules:Logging
:Modules:Database                — depends on :Modules:AVID
:Modules:IPC                     — depends on :Modules:AVU, :Modules:DeviceManager
```

### AVA Core Layer
```
:Modules:AVA:core:Utils          — leaf
:Modules:AVA:core:Domain         — depends on :Modules:AVA:core:Utils
:Modules:AVA:core:Data           — depends on :Domain, :Utils, :Modules:AVID
```

### AI Layer
```
:Modules:AI:NLU                  — depends on :AVA:core:Utils, :Domain, :Data
:Modules:AI:LLM                  — depends on :AVA:core:Domain/Utils/Data, :AI:NLU
:Modules:AI:RAG                  — depends on :AVA:core:Utils/:Domain/:Data, :AI:LLM, :Modules:AvanueUI
:Modules:AI:Chat                 — depends on :AI:NLU, :AI:RAG, :AI:LLM, :Actions, :AVA:core:*, :AvanueUI
:Modules:AI:Memory               — commonMain only (no project deps)
:Modules:AI:Teach                — depends on :AVA:core:Domain/:Utils, :AvanueUI
```

### UI / Design Layer
```
:Modules:AvanueUI:Core           — leaf
:Modules:AvanueUI:CoreTypes      — leaf
:Modules:AvanueUI:Theme          — depends on :AvanueUI:Core
:Modules:AvanueUI:Input          — depends on :AvanueUI:Core
:Modules:AvanueUI:Display        — depends on :AvanueUI:Core
:Modules:AvanueUI:Feedback       — depends on :AvanueUI:Core
:Modules:AvanueUI:Layout         — depends on :AvanueUI:Core
:Modules:AvanueUI:Navigation     — depends on :AvanueUI:Core
:Modules:AvanueUI:Floating       — depends on :AvanueUI:Core
:Modules:AvanueUI:Data           — depends on :AvanueUI:Core
:Modules:AvanueUI:Voice          — (test deps only)
:Modules:AvanueUI:StateManagement — depends on :AvanueUI:Core
:Modules:AvanueUI:ARGScanner     — depends on :AvanueUI:Core
:Modules:AvanueUI:AssetManager   — depends on :AvanueUI:Core
:Modules:AvanueUI:VoiceCommandRouter — depends on :AvanueUI:ARGScanner, :AvanueUI:Core
:Modules:AvanueUI:AvanueLanguageServer — no active project deps (all commented out)
:Modules:AvanueUI:AvanueUIVoiceHandlers — depends on :VoiceOSCore, :Modules:Logging
:Modules:AvanueUI (root)         — depends on :Modules:Foundation
:Modules:AVACode                 — depends on :AvanueUI:Core
```

### Voice / Speech Layer
```
:Modules:VoiceIsolation          — leaf (no project deps)
:Modules:SpeechRecognition       — depends on :AI:NLU, :AvanueUI, :DeviceManager, :VoiceIsolation
:Modules:VoiceCursor             — depends on :AvanueUI, :DeviceManager
:Modules:Gaze                    — depends on :Foundation, :VoiceCursor
:Modules:Voice:WakeWord          — depends on :AVA:core:Domain/:Utils
:Modules:VoiceKeyboard           — depends on :SpeechRecognition, :Localization
:Modules:Localization            — (no project deps visible in common search)
:Modules:VoiceDataManager        — depends on :Modules:Database
```

### VoiceOS Core
```
:Modules:VoiceOSCore             — depends on :Foundation, :Logging, :AVID, :AVU,
                                   :SpeechRecognition, :Database, :DeviceManager,
                                   :Localization, :VoiceDataManager, :AvanueUI,
                                   :AI:NLU, :AI:LLM, :AVA:core:Utils, :Rpc,
                                   :VoiceCursor, :vivoka:Android
```

### Content / Media Layer
```
:Modules:PDFAvanue               — depends on :Foundation, :Logging, :AvanueUI
:Modules:ImageAvanue             — depends on :Foundation, :Logging, :AvanueUI, :VoiceOSCore
:Modules:VideoAvanue             — depends on :Foundation, :Logging, :AvanueUI, :VoiceOSCore
:Modules:NoteAvanue              — depends on :Foundation, :Logging, :Database, :AvanueUI,
                                   :VoiceOSCore, :AI:RAG
:Modules:PhotoAvanue             — depends on :Foundation, :Logging, :AvanueUI
:Modules:AnnotationAvanue        — depends on :Foundation, :Logging, :AvanueUI
:Modules:RemoteCast              — depends on :Foundation, :Logging, :HTTPAvanue, :AvanueUI
:Modules:HTTPAvanue              — depends on :Modules:Logging
```

### Orchestration Layer
```
:Modules:Cockpit                 — depends on :Foundation, :Logging, :AVID, :Database,
                                   :AvanueUI, :WebAvanue, :PDFAvanue, :ImageAvanue,
                                   :VideoAvanue, :NoteAvanue, :PhotoAvanue, :RemoteCast,
                                   :AnnotationAvanue, :VoiceOSCore, :DeviceManager, :VoiceCursor
:Modules:WebAvanue               — depends on :Foundation, :AvanueUI, :Logging, :Database,
                                   :AVID, :VoiceOSCore
:Modules:VoiceAvanue             — depends on :Foundation, :AVID, :AVU, :Database, :Logging,
                                   :VoiceCursor, :SpeechRecognition, :Rpc, :DeviceManager,
                                   :Localization, :AI:NLU, :AI:LLM, :AVA:core:Utils,
                                   :vivoka:Android
:Modules:PluginSystem            — depends on :VoiceOSCore, :Rpc, :Database
:Modules:AvidCreator             — depends on :AVID, :Database, :AvanueUI
:Modules:DeviceManager           — depends on :AvanueUI (androidMain)
:Modules:AvanuesShared           — depends on :VoiceOSCore, :Database, :Foundation, :AVID,
                                   :SpeechRecognition, :Logging
```

### Apps
```
:apps:avanues                    — depends on VoiceAvanue, Foundation, Gaze, VoiceOSCore,
                                   WebAvanue, VoiceCursor, AVID, AVU, DeviceManager,
                                   SpeechRecognition, Cockpit, all content modules,
                                   Database, AVA:core:Utils, AvanueUI, AI:NLU, AI:LLM
:apps:voiceavanue                — same as :apps:avanues (legacy copy)
```

---

## Circular Dependencies

No circular dependencies detected in the active module graph. The DAG flows cleanly from
foundation leaves upward to content/orchestration layers.

**Potential near-cycles to watch:**

1. `:Modules:AvanueUI:AvanueUIVoiceHandlers` depends on `:Modules:VoiceOSCore`, which
   depends on `:Modules:AvanueUI`. This is a two-hop indirect cycle through the AvanueUI
   root module and the AvanueUIVoiceHandlers sub-module. It is NOT a true cycle because
   VoiceOSCore depends on the AvanueUI ROOT, and AvanueUIVoiceHandlers is a SEPARATE
   sub-project — Gradle treats them as distinct nodes. However, semantically this creates a
   tight coupling that makes both modules harder to test in isolation.

2. `:Modules:CameraAvanue` depends on `:Modules:AI:RAG`, which depends on
   `:Modules:AI:LLM`, which depends on `:Modules:AI:NLU`. Camera module pulling in the
   entire AI stack is architecturally suspect — it suggests AI inference is being done
   inside the camera module rather than by the calling app.

---

## Test Coverage

All test sources below are in the **active** `Modules/` tree (archive backups excluded).

| Module | Unit Tests | Integration Tests | Status |
|--------|-----------|-------------------|--------|
| AI:NLU | commonTest (4 files) | androidTest (9 files), androidInstrumentedTest (5 files) | GOOD |
| AI:LLM | androidUnitTest (9 files) | androidInstrumentedTest (6 files) | GOOD |
| AI:RAG | commonTest (9 files) | androidTest (11 files) | GOOD |
| AI:Chat | none | androidTest (4 files) | PARTIAL |
| AI:Memory | commonTest (2 files) | none | PARTIAL |
| AI:Teach | androidUnitTest (1 file) | none | MINIMAL |
| AI:ALC | none | none | MISSING |
| WebAvanue | commonTest (5 files), androidUnitTest (2 files) | androidInstrumentedTest (10 files) | GOOD |
| DeviceManager | commonTest (1 file), androidUnitTest (3 files) | androidTest (1 file) | GOOD |
| AvidCreator | none | androidTest (2 files) | PARTIAL |
| AVA:core:Data | none | androidTest (4 files) | PARTIAL |
| AVA:core:Domain | none | androidTest (4 files) | PARTIAL |
| AVA:core:Utils | none | androidUnitTest (1 file) | MINIMAL |
| LicenseManager | none | androidTest (1 file) | MINIMAL |
| VoiceOSCore | commonTest only (2 deps) | androidInstrumentedTest (2 files) | MINIMAL — critical module with almost no tests |
| SpeechRecognition | none visible | none | MISSING — critical module |
| Foundation | none | none | MISSING |
| Database | none | none | MISSING |
| Logging | none | none | MISSING |
| AVID | none | none | MISSING |
| AVU | none | none | MISSING |
| Cockpit | none | none | MISSING |
| AvanueUI (all sub-modules) | none | none | MISSING |
| VoiceCursor | none | none | MISSING |
| Gaze | none | none | MISSING |
| VoiceDataManager | none | none | MISSING |
| Localization | none | none | MISSING |
| HTTPAvanue | none | none | MISSING |
| RemoteCast | none | none | MISSING |
| NoteAvanue | none | none | MISSING |
| PDFAvanue | none | none | MISSING |
| ImageAvanue | none | none | MISSING |
| VideoAvanue | none | none | MISSING |
| PhotoAvanue | none | none | MISSING |
| AnnotationAvanue | none | none | MISSING |
| PluginSystem | none | none | MISSING |
| Actions | none | none | MISSING |
| Rpc | none | none | MISSING |
| apps/* | none | none | MISSING |

**Test coverage estimate: ~15% of active modules have any tests. ~5% of modules have commonTest
(platform-independent) tests. The AI module group accounts for ~80% of all existing tests.**

---

## Deprecated Dependencies

| Dependency | Catalog Alias | Current Version | Note |
|-----------|--------------|-----------------|------|
| `android.enableJetifier=true` | gradle.properties L9 | — | Jetifier migrates support-library → AndroidX. This project is fully AndroidX. Flag is dead weight, adds ~2s to clean builds |
| `androidx.fragment:fragment-ktx:1.6.2` | HARDCODED (bypasses catalog) | 1.6.2 | Used in VoiceOSCore and DeviceManager androidMain. Version not in libs.versions.toml — bypasses central version control. Current is 1.8.6 |
| `kotlinx-serialization 1.6.0` | `kotlinx-serialization` | 1.6.0 | 1.7.x has breaking sealed class deserialization fixes in certain edge cases |
| `dokka 1.9.10` | `dokka` | 1.9.10 | Dokka 2.0.0 released — 1.9.x will not receive fixes |
| `ktor 2.3.7` | `ktor` | 2.3.7 | Ktor 3.x is stable since late 2024 with rewritten OkHttp engine. Upgrading is a semver-breaking change |
| `coil 2.6.0` | `coil` | 2.6.0 | Coil 3 released — 2.x is maintenance only |
| `neo4j 5.15.0` | `neo4j` | 5.15.0 | Comment in catalog says "LearnAppDev only" — LearnApp is archived. Dependency should be removed |
| `okhttp 4.12.0` | `okhttp` | 4.12.0 | Comment says "LearnAppDev only" — LearnApp is archived. Dependency should be removed |
| `compileSdk = 34` | multiple modules | — | Android 15 (API 35) is current. 21 of ~50 active modules still use SDK 34. Apps correctly use 35 but library modules lag |
| `androidx-lifecycle 2.6.2` | `androidx-lifecycle` | 2.6.2 | 2.9.0 out — 2.8+ has KMP-compatible SavedStateHandle and `repeatOnLifecycle` improvements |
| `androidx-navigation 2.7.6` | `androidx-navigation` | 2.7.6 | 2.9.0 available |
| `gRPC 1.62.2` | `grpc` | 1.62.2 | 1.71.0 out — 1.65+ has critical HTTP/2 flow control fixes relevant given recent HTTPAvanue work |

---

## Build Health Issues

### P0 — Critical

**P0-1: `ksp.useKSP2=false` is a ticking clock**
- File: `gradle.properties:19`
- KSP2 is the default backend in KSP 2.1.x. The `ksp.useKSP2=false` override suppresses it
  because KSP2 cannot resolve types across KMP compiled output during Hilt annotation
  processing. This workaround is **documented in agent memory as a known issue** but has no
  tracking issue or migration plan. When KSP2 compatibility is fixed upstream (or when Hilt
  adds proper KMP2 support), this flag must be removed — leaving it indefinitely means future
  KSP updates may silently break with no obvious cause.
- Risk: Upgrading KSP without removing this flag will produce misleading build output.

**P0-2: `settings.gradle.kts` includes `:Modules:VoiceAvanue` which also depends on
         most of the same modules as `:apps:avanues` — duplicate dependency surface**
- File: `settings.gradle.kts:72`, `apps/avanues/build.gradle.kts:120`
- `:Modules:VoiceAvanue` is included as a reusable module AND `:apps:avanues` depends on it.
  But `:apps:avanues` also directly re-declares many of the same transitive deps that
  VoiceAvanue already exposes via `api()`. This creates a bloated dependency declaration
  with no single source of truth. If VoiceAvanue's API changes, the app may silently
  still compile using its own duplicate declaration.

### P1 — High

**P1-1: Fragment-ktx hardcoded versions bypass the catalog**
- Files: `Modules/VoiceOSCore/build.gradle.kts:108`, `Modules/DeviceManager/build.gradle.kts:87`
- `implementation("androidx.fragment:fragment-ktx:1.6.2")` — version pinned inline, not via
  `libs.versions.toml`. This breaks the monorepo convention and means `./gradlew dependencyUpdates`
  will not flag this dependency. Current release is 1.8.6. Add to catalog and reference via alias.

**P1-2: Inconsistent compileSdk across modules (34 vs 35)**
- ~21 library modules use `compileSdk = 34` while apps and some modules use 35.
- A library compiled against SDK 34 cannot use APIs introduced in API 35. When an app at
  SDK 35 calls into a library compiled at SDK 34, the library's `@RequiresApi` annotations
  and SDK 35 type stubs are absent from the library's compilation unit. This does not cause
  a runtime crash, but it means SDK 35 APIs used inside library code will produce a
  "call requires API level 35" lint warning even when the library is only called from SDK 35
  contexts. The inconsistency is noise in the lint output.
- Fix: Centralize `compileSdk` into a single variable in root build or a shared convention plugin.

**P1-3: No convention plugins / buildSrc**
- `buildSrc/` does not exist. All module build files repeat the same ~20-line android block
  (namespace, compileSdk, minSdk, jvmTarget, buildFeatures). At 50+ modules, this is 1000+
  lines of duplicated boilerplate. Any global change (e.g., raising minSdk from 29 to 31)
  requires editing every build file by hand.
- Fix: Create `buildSrc/` with a `com.augmentalis.kmp-library` convention plugin. JetBrains'
  own tooling docs recommend this pattern for monorepos.

**P1-4: `android.enableJetifier=true` is dead weight**
- File: `gradle.properties:9`
- Jetifier rewrites support-library references in all third-party AARs. This project uses no
  Android Support Library (all AndroidX). Jetifier adds ~2 seconds to clean builds and can
  occasionally misrewrite valid AndroidX bytecode. It should be removed.

**P1-5: No test source sets in 35+ active modules**
- Critical infrastructure modules (Foundation, Database, Logging, AVID, AVU, VoiceOSCore,
  SpeechRecognition, Cockpit, AvanueUI, HTTPAvanue, RemoteCast) have zero tests.
- VoiceOSCore and SpeechRecognition are production-critical and among the most complex modules
  in the repo. Their absence of tests directly increases the probability of regressions when
  the 50+ known stubs (tracked in agent memory) are eventually implemented.

**P1-6: Zombie catalog entries — `neo4j` and `okhttp` for archived module**
- File: `gradle/libs.versions.toml:59-61`
- `neo4j = "5.15.0"` and `okhttp = "4.12.0"` are annotated "LearnAppDev only". LearnApp
  is archived and no active module references these libraries. The catalog entries create
  confusion and will be included in version update scans.

### P2 — Medium

**P2-1: No commonTest in the majority of KMP modules**
- Only AI:NLU, AI:RAG, AI:Memory, WebAvanue have commonTest source sets.
- KMP's primary value is compile-once-test-once logic in commonMain. Without commonTest,
  there is no CI gate on shared business logic across platforms.

**P2-2: `RepositoriesMode.FAIL_ON_PROJECT_REPOS` is correct but conflicts with
         `buildscript {}` block in root build.gradle.kts**
- File: `build.gradle.kts:9-14`, `settings.gradle.kts:21`
- `dependencyResolutionManagement` with `FAIL_ON_PROJECT_REPOS` correctly centralizes
  repositories. However, the root `build.gradle.kts` still has an explicit `buildscript {
  repositories { google(); mavenCentral() } }` block. This is redundant since `settings`
  already configures `pluginManagement`. The `buildscript {}` block in `build.gradle.kts`
  is a legacy pattern that can be removed.

**P2-3: `material3-adaptive` pinned to `1.0.0-beta01`**
- File: `gradle/libs.versions.toml:39`
- `1.1.0` stable is available. Beta version in production is a risk for behavioral changes
  between beta and stable — especially in `ListDetailPaneScaffold` which is used by
  UnifiedSettingsScreen.

**P2-4: `androidx-security-crypto 1.1.0-alpha06` is alpha-pinned in production**
- File: `gradle/libs.versions.toml:25`
- No stable `1.1.x` has shipped. The module provides `EncryptedSharedPreferences` used in
  credential storage. Running alpha in production is acceptable only with explicit awareness.
  The alpha tag should be in a comment noting that no stable exists, or it should be
  replaced with a BouncyCastle-based implementation (which is already a dependency).

**P2-5: Two-hop semantic cycle: AvanueUI → VoiceOSCore → AvanueUI:AvanueUIVoiceHandlers**
- `:Modules:VoiceOSCore` depends on `:Modules:AvanueUI` (root).
- `:Modules:AvanueUI:AvanueUIVoiceHandlers` depends on `:Modules:VoiceOSCore`.
- While not a Gradle circular dependency (different project paths), this creates a
  semantic coupling loop: the UI module knows about voice commands, and the voice module
  knows about the UI theme. This violates layered architecture.

**P2-6: `CameraAvanue` depends on `:Modules:AI:RAG` — AI inference in a media module**
- File: `Modules/CameraAvanue/build.gradle.kts:76`
- A camera/capture module should have no dependency on the RAG (retrieval-augmented
  generation) AI module. This pulls in the entire NLU + LLM + core stack through RAG's
  transitive dependencies. AI analysis of camera content belongs in the calling app, not
  in the camera library.

**P2-7: `kotlin.mpp.applyDefaultHierarchyTemplate=false`**
- File: `gradle.properties:14`
- Disabling the default KMP source set hierarchy template means all intermediate source
  set relationships (e.g., `iosMain`, `nativeMain`, `jvmMain`) must be declared manually
  in every module. This was necessary pre-Kotlin 2.0 but is now an anti-pattern. With
  Kotlin 2.1, the default template is stable and handles all common tier structures. The
  flag increases boilerplate in every module's sourceSets block.

**P2-8: iOS targets behind optional property flag — risky for CI**
- VoiceOSCore, SpeechRecognition, and others compile iOS targets only when
  `kotlin.mpp.enableNativeTargets=true` is set OR when task names contain "ios/Framework".
  This means a standard `./gradlew build` on CI does NOT validate iOS compilation.
  A broken iOS build will only surface when explicitly targeted, not on every PR.

### P3 — Low / Style

**P3-1: Gradle parallel=true + caching=true but no build scan configured**
- Parallel builds and caching are enabled, which is correct. However, no Gradle Build Scan
  plugin or Develocity configuration is present. Build scan output would help diagnose
  why certain modules cause cache misses.

**P3-2: `dokka` plugin declared at root but no documentation generation task**
- `alias(libs.plugins.dokka) apply false` is declared but no subproject applies it.
- Dokka documentation is never generated. Either apply it to modules that need API docs,
  or remove the catalog entry and root declaration.

**P3-3: `TYPESAFE_PROJECT_ACCESSORS` feature preview enabled**
- `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` in settings is a Gradle 7.0+ feature
  that has been stable since Gradle 8.x. The `enableFeaturePreview` call is no longer
  necessary for Gradle 8.6+. Remove it.

**P3-4: `voiceavanue` and `voiceavanue-legacy` both in settings and both compiled**
- `apps/voiceavanue` and `apps/voiceavanue-legacy` have near-identical build files. Legacy
  apps consuming active modules is a maintenance hazard — a breaking change in a module
  must be maintained in both. Legacy apps should depend only on a pinned snapshot or be
  removed from active compilation.

---

## Recommendations

1. **Create buildSrc convention plugins** to eliminate 1000+ lines of repeated android/kotlin
   configuration. Start with `kmp-library.gradle.kts` and `android-app.gradle.kts`. This is
   the single highest-leverage build system improvement available.

2. **Set a uniform `compileSdk = 35` across all active library modules.** Use a shared
   `val compileSdk: Int by rootProject.extra` in root `build.gradle.kts`, referenced in
   every subproject. This eliminates the 34/35 inconsistency and makes future SDK bumps
   a one-line change.

3. **Remove `android.enableJetifier=true`** from `gradle.properties`. Verify no third-party
   AARs require it (a `./gradlew dependencies | grep support` check suffices). Removing it
   saves ~2s on clean builds.

4. **Add `fragment-ktx` to `libs.versions.toml`** as `androidx-fragment-ktx = "1.8.6"` and
   replace the hardcoded inline strings in VoiceOSCore and DeviceManager build files.

5. **Remove dead catalog entries** for `neo4j` and `okhttp` (LearnApp archived). These
   create noise in dependency update tools.

6. **Add commonTest source sets to Foundation, Database, AVID, AVU, Logging, and
   VoiceOSCore** as the highest-priority test coverage targets. These are the dependency
   foundation that all other modules build on — a defect here has monorepo-wide blast radius.

7. **Upgrade Ktor from 2.3.7 → 3.x** on a dedicated branch. It is a semver break but the
   migration guide is well-documented. Delay increases the delta.

8. **Replace `material3-adaptive:1.0.0-beta01`** with `1.1.0` stable.

9. **Plan KSP2 migration.** The `ksp.useKSP2=false` workaround should be tracked as a
   formal tech-debt item. When Hilt publishes KMP/KSP2-compatible metadata, this flag
   must be the first thing removed — not the last.

10. **Remove the legacy `buildscript {}` block** from root `build.gradle.kts`. The
    `pluginManagement {}` in `settings.gradle.kts` handles all plugin resolution. The
    redundant `buildscript` block is a legacy pattern from pre-settings plugin management.

11. **Enable iOS targets unconditionally in CI** (or accept that iOS compilation is never
    validated). The current opt-in pattern means an iOS breakage will ship undetected.

12. **Decouple CameraAvanue from AI:RAG.** Move AI-triggered analysis into the calling app
    or a dedicated coordinator. The camera module should only capture, not classify.
