# App Consolidation Framework - Full Long-Term Solution

> **Branch**: `060226-1-consolidation-framework` (create from current HEAD of `claude/060226-avu-dsl-evolution`)
> **Save to**: `docs/plans/Consolidation/Consolidation-Plan-Framework-260206-V1.md`

## Context

The NewAvanues monorepo has significant code duplication across modules that impedes maintainability and blocks the consolidated Avanues app from being a well-coordinated platform. Before finishing the Avanues app (browser integration, launcher icons, deep links), we need the shared module foundation to be clean.

**Key discovery**: Several canonical shared modules ALREADY EXIST but aren't fully adopted:
- `Modules/Foundation` — has UiState, BaseViewModel, etc. but WebAvanue still keeps duplicates
- `Modules/Logging` — has PII-safe KMP logging but 4 other Logger implementations remain
- `Modules/AVA/core/Theme` — has AvaTheme KMP but WebAvanue keeps its own OceanTheme/AppTheme
- AVU DSL in VoiceOSCore commonMain has near-zero coupling to VoiceOSCore internals — extraction candidate

---

## Phase 1: WebAvanue State Management Deduplication

**Priority**: HIGHEST | **Risk**: LOW | **~12 files**

WebAvanue already depends on Foundation (`api(project(":Modules:Foundation"))` at line 65) but keeps 5 duplicate util files. This is purely dead code.

### Step 1.1: Upgrade Foundation ViewModelState for API parity

WebAvanue's copy has `inline fun update()` and `@PublishedApi internal val _state` (performance optimization). Merge this into Foundation before deleting WebAvanue's copy.

**Modify**: `Modules/Foundation/src/commonMain/.../state/ViewModelState.kt`
- `private val _state` -> `@PublishedApi internal val _state` (both ViewModelState and NullableState)
- `fun update(transform)` -> `inline fun update(transform)` (both classes)
- `fun ifPresent(block)` -> `inline fun ifPresent(block)` (NullableState)

### Step 1.2: Update WebAvanue imports

Change all `import com.augmentalis.webavanue.util.*` to `import com.augmentalis.foundation.state.*` + `import com.augmentalis.foundation.viewmodel.*` in these files:
- `Modules/WebAvanue/.../TabViewModel.kt`
- `Modules/WebAvanue/.../SecurityViewModel.kt`
- `Modules/WebAvanue/.../SettingsViewModel.kt`
- `Modules/WebAvanue/.../DownloadViewModel.kt`
- `Modules/WebAvanue/.../FavoriteViewModel.kt`
- `Modules/WebAvanue/.../HistoryViewModel.kt`

### Step 1.3: Delete WebAvanue duplicates

**Delete 5 files**:
- `Modules/WebAvanue/src/commonMain/.../util/UiState.kt`
- `Modules/WebAvanue/src/commonMain/.../util/ListState.kt`
- `Modules/WebAvanue/src/commonMain/.../util/SearchState.kt`
- `Modules/WebAvanue/src/commonMain/.../util/ViewModelState.kt`
- `Modules/WebAvanue/src/commonMain/.../util/BaseViewModel.kt`

### Verification
```bash
./gradlew :Modules:Foundation:compileKotlinAndroid
./gradlew :Modules:WebAvanue:compileKotlinAndroid
./gradlew :apps:avanues:assembleDebug
```

---

## Phase 2: Logger Consolidation (5 implementations -> 1)

**Priority**: HIGH | **Risk**: MEDIUM | **~35 files** | Independent of Phase 1

### Current State (5 Loggers)
| Module | Package | Type |
|--------|---------|------|
| `Modules/Logging` | `com.avanues.logging` | Interface + PII-safe + factory + platform actuals (**CANONICAL**) |
| `Modules/Utilities` | `com.augmentalis.ava.platform` | expect/actual object with tag-based API |
| `VoiceOSCore` | `com.augmentalis.voiceoscore.logging` | Interface + LogLevel + LoggerFactory + PII wrappers |
| `WebAvanue` | `com.augmentalis.webavanue` | Napier-based singleton with PII sanitization |
| `AvaMagic/AVACode` | `com.augmentalis.magicui.core` | Interface + ConsoleLogger |

### Step 2.1: Enhance `Modules/Logging` with missing features
- Add URL/filename sanitization to `PIIRedactionHelper.kt` (from WebAvanue)
- Add `LogCompat` object for tag-based bridge API (from Utilities pattern)

### Step 2.2: Add Logging dependency to modules
- `VoiceOSCore/build.gradle.kts` -> add `implementation(project(":Modules:Logging"))`
- `WebAvanue/build.gradle.kts` -> add `implementation(project(":Modules:Logging"))`

### Step 2.3: Migrate VoiceOSCore
Replace `com.augmentalis.voiceoscore.logging.Logger` -> `com.avanues.logging.Logger` across all consuming files.

**Delete** (~7 files): Logger.kt, LogLevel.kt, LoggerFactory.kt, PIIRedactionHelper.kt from VoiceOSCore logging/ + desktop platform actuals

### Step 2.4: Migrate WebAvanue
Replace `com.augmentalis.webavanue.Logger` -> `com.avanues.logging.LoggerFactory` in ~9 consuming files.
**Delete**: `Modules/WebAvanue/.../Logger.kt`

### Step 2.5: Migrate Utilities Logger
Modify Utilities' `Logger.kt` (all platforms) to delegate to `com.avanues.logging.LoggerFactory`.
Add `implementation(project(":Modules:Logging"))` to Utilities build.gradle.kts.

### Step 2.6: Delete AvaMagic Logger
No consumers found. **Delete**: `AvaMagic/AVACode/commonMain/.../Logger.kt`

### Verification
```bash
./gradlew :Modules:Logging:compileKotlinAndroid
./gradlew :Modules:VoiceOSCore:compileKotlinAndroid
./gradlew :Modules:WebAvanue:compileKotlinAndroid
./gradlew :apps:avanues:assembleDebug
```

---

## Phase 3: AVU DSL Extraction to Standalone Module

**Priority**: HIGH (strategic) | **Risk**: MEDIUM | **32 files moved** | Depends on Phase 2

### Why Extract
29 commonMain DSL files have ZERO coupling to VoiceOSCore internals. The only dependency is `currentTimeMillis()` (trivially replaced with `kotlinx.datetime`). Extracting enables:
- Independent testing/versioning
- Use by WebAvanue, AvaMagic, Cockpit, future modules
- Plugin ecosystem independence from voice engine

### Step 3.1: Create `Modules/AVUDsl/build.gradle.kts`
- KMP module: Android + conditional iOS + Desktop
- Dependencies: `kotlinx.coroutines.core`, `kotlinx.datetime`, `project(":Modules:Logging")`
- Package: `com.avanues.avudsl`

### Step 3.2: Move 29 commonMain files
From `VoiceOSCore/src/commonMain/.../dsl/` to `AVUDsl/src/commonMain/.../`:
- `ast/` (1 file), `lexer/` (2), `parser/` (2), `interpreter/` (8), `plugin/` (7), `registry/` (2), `tooling/` (4), `migration/` (3)

Package rename: `com.augmentalis.voiceoscore.dsl.*` -> `com.avanues.avudsl.*`

### Step 3.3: Replace `currentTimeMillis` coupling
In 4 files, replace `import com.augmentalis.voiceoscore.currentTimeMillis` with `kotlinx.datetime.Clock.System.now().toEpochMilliseconds()`

### Step 3.4: Move platform dispatchers
- `DesktopAvuDispatcher.kt` -> `AVUDsl/src/desktopMain/` (no VoiceOSCore coupling)
- `IosAvuDispatcher.kt` -> `AVUDsl/src/iosMain/` (no VoiceOSCore coupling)
- `AndroidAvuDispatcher.kt` -> **STAYS in VoiceOSCore** (imports HandlerRegistry, IHandler, etc.)

### Step 3.5: Update settings.gradle.kts
Add: `include(":Modules:AVUDsl")`

### Step 3.6: Update VoiceOSCore build.gradle.kts
Add: `api(project(":Modules:AVUDsl"))` (transitive for consumers)

### Step 3.7: Update AndroidAvuDispatcher + VoiceOSCore internal imports
Change `com.augmentalis.voiceoscore.dsl.*` -> `com.avanues.avudsl.*`

### Verification
```bash
./gradlew :Modules:AVUDsl:compileKotlinAndroid
./gradlew :Modules:VoiceOSCore:compileKotlinAndroid
./gradlew :apps:avanues:assembleDebug
```

---

## Phase 4: Foundation Enhancement (Hash + Time utilities)

**Priority**: MEDIUM | **Risk**: LOW | **~8 files** | Independent of Phase 3

### Step 4.1: Enable multi-platform targets in Foundation
Add Desktop target (jvm), conditional iOS targets to Foundation/build.gradle.kts (matching VoiceOSCore pattern).

### Step 4.2: Add `kotlinx-datetime` to Foundation
Add `implementation(libs.kotlinx.datetime)` to commonMain dependencies.

### Step 4.3: Add TimeUtils
New file: `Modules/Foundation/src/commonMain/.../util/TimeUtils.kt`
```kotlin
object TimeUtils {
    fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
```

### Step 4.4: Add HashUtils
New file: `Modules/Foundation/src/commonMain/.../util/HashUtils.kt`
Merge SHA-256 (from VoiceOSCore) + MD5 (from AVA/core/Data).
Platform actuals for digest algorithms.

### Step 4.5: Migrate consumers
- Delete `VoiceOSCore/.../utils/HashUtils.kt` + platform actuals
- Update imports in VoiceOSCore consumers

### Verification
```bash
./gradlew :Modules:Foundation:compileKotlinAndroid
./gradlew :Modules:VoiceOSCore:compileKotlinAndroid
```

---

## Phase 5: Theme Consolidation (WebAvanue -> AvaTheme)

**Priority**: MEDIUM | **Risk**: HIGH (22 files reference OceanTheme) | **~30 files** | After Phases 1-2

### Strategy: Merge Ocean Color Tokens INTO AvaTheme as a Variant

WebAvanue's OceanTheme is NOT a simple duplicate of AvaTheme — it's a different color palette (Ocean Blue Glassmorphism). The approach is to bring OceanTheme colors into AvaTheme as a reusable color variant.

### Step 5.1: Add OceanColorTokens to AvaTheme module
New file: `Modules/AVA/core/Theme/.../OceanColorTokens.kt` — all color constants from WebAvanue's OceanTheme.

### Step 5.2: Move shared extensions to AvaTheme
- `WebAvanue/OceanThemeExtensions.kt` -> `AVA/core/Theme/`
- Remaining `GlassmorphicComponents` -> `AVA/core/Theme/` (if not already there)

### Step 5.3: Add AvaTheme dependency to WebAvanue
`WebAvanue/build.gradle.kts` -> `api(project(":Modules:AVA:core:Theme"))`

### Step 5.4: Update 22 WebAvanue files
Change `com.augmentalis.webavanue.OceanTheme` -> `com.augmentalis.ava.core.theme.OceanColorTokens`

### Step 5.5: Keep WebAvanue AppTheme temporarily
AppTheme has ThemeType switching logic specific to WebAvanue — it stays but delegates to AvaTheme internally.

### Step 5.6: Delete from WebAvanue
- `OceanTheme.kt`, `OceanThemeExtensions.kt` (moved to AvaTheme)

### Verification
```bash
./gradlew :Modules:AVA:core:Theme:compileKotlinAndroid
./gradlew :Modules:WebAvanue:compileKotlinAndroid
./gradlew :apps:avanues:assembleDebug
```

---

## Phase 6: Network Consolidation (DEFERRED)

**Priority**: LOW | WebAvanue's `NetworkChecker` and Utilities' `NetworkMonitor` serve different scopes. Consolidate into Foundation when more modules need unified networking.

---

## Phase Dependency Graph

```
Phase 1 (State)     ─┐
                     ├──> Phase 5 (Theme)
Phase 2 (Logger)    ─┤
                     └──> Phase 3 (AVU DSL) ──> Phase 6 (Network, deferred)
Phase 4 (Hash/Time) ── independent
```

Phases 1 & 2: **parallel** (no dependency)
Phase 3: after Phase 2 (DSL uses Logger)
Phase 4: **parallel** with anything
Phase 5: after Phases 1 & 2 (stability)

---

## Branch Strategy

1. Create `060226-1-consolidation-framework` from current HEAD
2. Implement Phases 1-5 with intermediate commits per phase
3. Verify full build after each phase: `./gradlew :apps:avanues:assembleDebug`
4. Merge into `claude/060226-avu-dsl-evolution` after all phases pass
5. Then continue Avanues Consolidated App work (browser integration, launcher icons, deep links)

---

## Store Compliance Notes (Cross-cutting)

- `.avp` plugin files are TEXT parsed by lexer — **NOT compiled code** — Play Store compliant
- No `ClassLoader`, `DexFile`, or `Runtime.exec()` usage
- `SandboxConfig` enforces resource limits on plugin execution (step count, timeout, nesting)
- `RECORD_AUDIO`, `SYSTEM_ALERT_WINDOW`, `BIND_ACCESSIBILITY_SERVICE` need data safety declarations
- All permissions requested only in app manifest, not library manifests
- `minSdk=26` for shared modules, `minSdk=29` for VoiceOSCore

---

## Summary

| Phase | Description | Files Modified | Files Deleted | Files Moved | Risk |
|-------|------------|---------------|---------------|-------------|------|
| 1 | State Mgmt Dedup | 7 | 5 | 0 | LOW |
| 2 | Logger Consolidation | ~25 | ~10 | 0 | MEDIUM |
| 3 | AVU DSL Extraction | ~5 | 0 | 31 | MEDIUM |
| 4 | Hash/Time Utils | ~8 | ~3 | 0 | LOW |
| 5 | Theme Consolidation | ~30 | 4 | 2 | HIGH |
| 6 | Network (deferred) | - | - | - | - |
| **Total** | | **~75** | **~22** | **33** | |
