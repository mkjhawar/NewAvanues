# VoiceOS Code Analysis Backlog

**Document ID:** VoiceOS-Backlog-CodeAnalysis-251227-V1
**Created:** 2025-12-27
**Author:** Code Analysis Agent
**Status:** Active
**Module:** VoiceOS

---

## Overview

Prioritized backlog of code quality issues, duplications, stubs, and technical debt identified in the VoiceOS module. Issues are categorized by priority level based on impact and urgency.

---

## P0 - CRITICAL (Fix Immediately)

These issues cause runtime crashes, security vulnerabilities, or build failures.

### VOSFIX-001: MacroActions.kt V2 Marketplace Methods Throw UnsupportedOperationException ✅ COMPLETED

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-001 |
| **Status** | **COMPLETED** (2025-12-27) |
| **Title** | MacroActions.kt marketplace methods throw exceptions at runtime |
| **File Path(s)** | `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/MacroActions.kt` |
| **Description** | Three methods (`browseMacroMarketplace`, `downloadMacro`, `uploadMacro`) are marked as V2 features but throw `UnsupportedOperationException` at runtime instead of returning safe defaults. If user code accidentally calls these methods, the app will crash. |
| **Acceptance Criteria** | ✅ 1. Methods return safe defaults (emptyList, null) instead of throwing<br>✅ 2. @Deprecated annotations added with V2 timeline message<br>3. Unit tests verify no exceptions thrown (TODO) |
| **Changes** | - `browseMacroMarketplace()` returns emptyList() + @Deprecated<br>- `downloadMacro()` returns null instead of throwing + @Deprecated<br>- `uploadMacro()` returns null instead of throwing + @Deprecated |
| **Dependencies** | None |

---

### VOSFIX-002: Remove apps/LearnApp-old-code/ Directory ✅ COMPLETED

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-002 |
| **Status** | **COMPLETED** (2025-12-27) |
| **Title** | Remove legacy LearnApp-old-code directory |
| **File Path(s)** | `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/LearnApp-old-code/` (26 files) |
| **Description** | Legacy code directory containing deprecated LearnApp and LearnAppDev implementations. Includes build files, manifests, and old Kotlin code that should have been removed after migration. Creates confusion and potential for accidental imports. |
| **Acceptance Criteria** | ✅ 1. Directory completely removed from repository<br>✅ 2. No references remain in build.gradle.kts files (verified via grep)<br>✅ 3. Git history preserved (no force push) |
| **Notes** | README.md confirmed code was archived Dec 22, 2025 after Phase 5 migration. Original commit: 5e5fac034 |
| **Dependencies** | Verify README.md explains migration before removal |

---

### VOSFIX-003: Consolidate Duplicate VoiceRecognitionClient.kt Files ✅ RESOLVED

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-003 |
| **Status** | **RESOLVED** (2025-12-27) - Duplicate no longer exists |
| **Title** | Consolidate duplicate VoiceRecognitionClient implementations |
| **File Path(s)** | 1. `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/client/VoiceRecognitionClient.kt` ✅ EXISTS<br>2. `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceRecognition/src/main/java/com/augmentalis/voicerecognition/client/VoiceRecognitionClient.kt` ❌ NOT FOUND |
| **Resolution** | Duplicate was either removed in a previous refactor or never existed. Only VoiceOSCore version remains which uses ConnectionCallback/RecognitionCallback interfaces. No action needed. |
| **Dependencies** | None |

---

### VOSFIX-004: Consolidate Duplicate LearnAppCore.kt Files 🔄 IN PROGRESS

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-004 |
| **Status** | **IN PROGRESS** - VoiceOSCore version marked @deprecated, 6 files still using it |
| **Title** | Consolidate duplicate LearnAppCore implementations |
| **File Path(s)** | 1. `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/core/LearnAppCore.kt` (769 lines) - @deprecated<br>2. `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/libraries/LearnAppCore/src/main/java/com/augmentalis/learnappcore/core/LearnAppCore.kt` (881 lines) - CANONICAL |
| **Description** | Two implementations of LearnAppCore exist with significant differences. The libraries version has more features (IBatchManagerInterface, IElementProcessorInterface, LRU cache, DatabaseRetryUtil) while VoiceOSCore version has version-aware commands. Need to merge best features into single canonical implementation. |
| **Current State** | - VoiceOSCore version has @deprecated annotation pointing to library<br>- VoiceOSCore build.gradle.kts already depends on library<br>- 6 files still import from local: JustInTimeLearner.kt, ExplorationEngine.kt, 4 test files |
| **Remaining Work** | 1. Merge version-aware commands from VoiceOSCore → library<br>2. Update 6 files to import from library<br>3. Verify all tests pass<br>4. Delete deprecated VoiceOSCore version |
| **Acceptance Criteria** | 1. Single implementation in libraries/LearnAppCore/<br>2. VoiceOSCore imports from library<br>3. All features merged (version-aware + retry + LRU)<br>4. All tests passing |
| **Dependencies** | VOSFIX-007 (DatabaseModule migration) |

---

### VOSFIX-005: Re-enable or Remove java.disabled/ Test Directory 🔄 IN PROGRESS

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-005 |
| **Status** | **IN PROGRESS** - Tests re-enabled but 800+ errors remain |
| **Title** | Re-enable or remove disabled test directory |
| **File Path(s)** | `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceOSCore/src/test/java.disabled/` (22 test files re-enabled) |
| **Description** | 22 test files were disabled by renaming the directory to `java.disabled`. Tests have been moved back to `java/` but many have API mismatches with current source code. |
| **Current State** | - Tests re-enabled (moved back to java/)<br>- 804 compilation errors in test code<br>- Key issues: API mismatches, missing imports, protected method access |
| **Fixed Files** | - PerformanceMonitorTest.kt (Long literal fixes)<br>- ConcurrencyStressTest.kt (VoiceOSService mock, Thread.sleep)<br>- CommandGeneratorTest.kt (SQLDelight queries)<br>- UIStateManagerTest.kt (coroutine scope)<br>- NumberOverlayManagerTest.kt (config API) |
| **Remaining Issues** | - VoiceOSServiceTest.kt (protected method access)<br>- MockFactories.kt (type inference, null handling)<br>- AppVersionDetectorTest.kt (API mismatch)<br>- Many more |
| **Acceptance Criteria** | 1. All tests compile<br>2. Tests run successfully<br>3. Test coverage restored<br>4. java.disabled directory deleted |
| **Dependencies** | VOSFIX-047 (Major Test Refactoring) |

---

### VOSFIX-047: Major Test Code Refactoring Required (NEW)

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-047 |
| **Status** | **BLOCKED** - Significant effort required |
| **Title** | VoiceOSCore test code significantly out of sync with source |
| **File Path(s)** | `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceOSCore/src/test/java/` (97 test files, 804 errors) |
| **Description** | Test code has drifted significantly from source code APIs. Key issues:<br>1. VoiceOSServiceTest.kt tries to call protected `onServiceConnected()`<br>2. MockFactories.kt has type inference and null handling issues<br>3. AppVersionDetectorTest.kt references non-existent API methods<br>4. Many tests use old constructors/signatures<br>5. IVoiceOSContextLSPContractTest.kt missing interface implementations |
| **Root Cause** | Source code was refactored without updating corresponding tests |
| **Categories** | - 30+ "protected method access" errors<br>- 50+ "type mismatch" errors<br>- 100+ "unresolved reference" errors<br>- 50+ "null/type inference" errors |
| **Acceptance Criteria** | 1. All 804 test errors resolved<br>2. Tests compile successfully<br>3. Tests run and pass<br>4. No disabled tests remain |
| **Estimated Effort** | High (multiple sprints) |
| **Dependencies** | None |

---

### VOSFIX-006: Consolidate Duplicate ThemeUtils.kt Files 🔄 DEPRECATIONS IN PLACE

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-006 |
| **Status** | **IN PROGRESS** - All 3 files have @deprecated annotations |
| **Title** | Consolidate duplicate ThemeUtils implementations |
| **File Path(s)** | 1. `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/ui/ThemeUtils.kt` (171 lines - full ARVision impl) @deprecated<br>2. `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/ui/utils/ThemeUtils.kt` (300 lines - full glassmorphism impl) @deprecated - BASE FOR MERGE<br>3. `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceRecognition/src/main/java/com/augmentalis/voicerecognition/ui/ThemeUtils.kt` (60 lines - stub) @deprecated |
| **Description** | Three implementations of ThemeUtils with varying completeness. VoiceCursor has ARVision-style glassmorphism, VoiceOSCore has full glassmorphism with multiple presets, VoiceRecognition has stubs. Should be unified in AVAMagic/MagicUI/Theme. |
| **Current State** | All 3 files have @deprecated annotations pointing to VOSFIX-006 and future consolidation. VoiceOSCore version identified as most complete (base for merge). |
| **Remaining Work** | 1. Create AVAMagic/MagicUI/Theme/ThemeUtils.kt with merged features<br>2. Update all apps to import from AVAMagic<br>3. Delete deprecated files |
| **Related** | VOSFIX-039 (Migrate VoiceUI/theme → AVAMagic/MagicUI/Theme) |
| **Dependencies** | None |

---

## P1 - HIGH (This Sprint)

These issues affect functionality, maintainability, or developer experience.

### VOSFIX-007: Complete DatabaseModule SQLDelight Migration

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-007 |
| **Title** | Complete DatabaseModule SQLDelight migration |
| **File Path(s)** | `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/managers/VoiceDataManager/src/main/java/com/augmentalis/datamanager/core/DatabaseModule.kt` |
| **Description** | DatabaseModule is marked as stub with version "2.0.0-stub". Multiple methods return null/false with TODO comments. 12 repository migrations needed from Room to SQLDelight. Currently non-functional for data export/import. |
| **Acceptance Criteria** | 1. All 12 repositories migrated to SQLDelight<br>2. exportData() returns valid JSON<br>3. importData() successfully imports data<br>4. trackCommandExecution() persists data<br>5. Remove "stub" from version<br>6. Unit tests for all methods |
| **Dependencies** | core/database module SQLDelight queries |

---

### VOSFIX-008: Implement LearnedAppTracker.kt Stub Methods

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-008 |
| **Title** | Implement LearnedAppTracker stub methods |
| **File Path(s)** | `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/tracking/LearnedAppTracker.kt` |
| **Description** | All 4 methods are stubs: `isAppLearned()` always returns false, `markAppAsLearned()` does nothing, `getLearnedApps()` returns empty list, `removeApp()` does nothing. This breaks LearnApp functionality for tracking which apps have been learned. |
| **Acceptance Criteria** | 1. Implement SharedPreferences or database persistence<br>2. isAppLearned() returns correct state<br>3. markAppAsLearned() persists package name<br>4. getLearnedApps() returns all learned apps<br>5. removeApp() clears learned status<br>6. Unit tests with 90%+ coverage |
| **Dependencies** | None |

---

### VOSFIX-009: Implement ChecklistManager.kt Database Persistence

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-009 |
| **Title** | Add database persistence to ChecklistManager |
| **File Path(s)** | `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/exploration/ChecklistManager.kt` |
| **Description** | ChecklistManager stores all data in memory (mutableMapOf). If app crashes or restarts during exploration, all progress is lost. Needs database persistence with recovery on restart. |
| **Acceptance Criteria** | 1. Add SQLDelight queries for checklist persistence<br>2. Save checklist state on each update<br>3. Restore checklist on initialization<br>4. Handle crashes gracefully<br>5. Add recovery tests |
| **Dependencies** | VOSFIX-007 (DatabaseModule) |

---

### VOSFIX-010: Implement RealSubscriptionProvider.kt Methods

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-010 |
| **Title** | Implement RealSubscriptionProvider billing integration |
| **File Path(s)** | `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/subscription/RealSubscriptionProvider.kt` |
| **Description** | File has 10 TODO comments. All methods are stubs that return false or log warnings. No actual Google Play Billing integration exists. Premium features cannot be unlocked. |
| **Acceptance Criteria** | 1. Add billing library dependency<br>2. Implement initializeBilling() with BillingClient<br>3. Implement hasActiveSubscription() with purchase query<br>4. Implement hasPermanentLicense() for one-time purchases<br>5. Implement launchPurchaseFlow() for purchasing<br>6. Implement cleanup() for proper lifecycle<br>7. Add integration tests |
| **Dependencies** | Google Play Console subscription products configured |

---

### VOSFIX-011: Clean deprecated/ Directory in CommandManager

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-011 |
| **Title** | Process deprecated CommandManager files |
| **File Path(s)** | `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/managers/CommandManager/deprecated/` |
| **Description** | Directory contains README.md documenting deprecated NluIntegration.kt and CommandContextAdapter.kt. Files were moved during refactor but directory remains. Need to confirm files are truly deprecated and clean up. |
| **Acceptance Criteria** | 1. Review README.md for accuracy<br>2. Confirm no code references deprecated files<br>3. Archive to docs if historically valuable<br>4. Remove deprecated/ directory<br>5. Update any documentation references |
| **Dependencies** | None |

---

### VOSFIX-012: Fix VUIDCreationDebugOverlay Stubs

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-012 |
| **Title** | Implement VUIDCreationDebugOverlay functionality |
| **File Path(s)** | `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/metrics/VUIDCreationDebugOverlay.kt` |
| **Description** | All 4 methods are stub implementations that do nothing. Debug overlay provides no actual debugging information during VUID creation, making development difficult. |
| **Acceptance Criteria** | 1. Implement show() to display floating overlay<br>2. Implement hide() to remove overlay<br>3. Implement updateProgress() to show real-time progress<br>4. Implement setMetricsCollector() to receive metrics<br>5. Add overlay permission handling<br>6. Add developer option toggle |
| **Dependencies** | VOSFIX-013 (VUIDCreationMetricsCollector) |

---

### VOSFIX-013: Fix VUIDCreationMetricsCollector Stubs

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-013 |
| **Title** | Implement VUIDCreationMetricsCollector recording |
| **File Path(s)** | `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/metrics/VUIDCreationMetricsCollector.kt` |
| **Description** | recordCreationTime() is a stub that ignores the time parameter. Metrics collection is incomplete - timing data is lost. |
| **Acceptance Criteria** | 1. Implement recordCreationTime() to track timing<br>2. Add timing statistics (min, max, avg, p95)<br>3. Add getMetrics() to include timing data<br>4. Add export functionality for metrics<br>5. Unit tests for accuracy |
| **Dependencies** | None |

---

## P2 - MEDIUM (Next Sprint)

These issues affect code quality but don't block functionality.

### VOSFIX-014 to VOSFIX-030: Address Remaining TODOs

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-014 to VOSFIX-030 |
| **Title** | Address remaining 173 TODOs across 77 files |
| **File Path(s)** | See table below |
| **Description** | 173 TODO comments identified across the codebase. Most are implementation stubs, deferred features, or known issues. Each TODO should be reviewed and either implemented, removed with justification, or converted to tracked backlog items. |
| **Acceptance Criteria** | 1. Each TODO reviewed and categorized<br>2. Critical TODOs implemented<br>3. Deferred TODOs converted to backlog items<br>4. Obsolete TODOs removed<br>5. Count reduced by 50% |
| **Dependencies** | Various |

**High-TODO Files:**

| File | TODOs | Priority |
|------|-------|----------|
| RealSubscriptionProvider.kt | 10 | P1 (VOSFIX-010) |
| VoiceOSService.kt | 7 | P2 |
| KeyboardView.kt | 7 | P2 |
| PermissionUIHandler.kt (iOS) | 7 | P2 |
| PermissionUIHandler.kt (Android) | 6 | P2 |
| IntentDispatcher.kt | 5 | P2 |
| DatabaseModule.kt | 5 | P1 (VOSFIX-007) |
| GoogleNetwork.kt | 5 | P2 |
| DeviceViewModel.kt | 5 | P2 |

---

### VOSFIX-031: Clean Up Deprecated TypeAliases

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-031 |
| **Title** | Clean up deprecated TypeAliases files |
| **File Path(s)** | 43 typealias declarations across 12 files (see below) |
| **Description** | Multiple TypeAliases.kt files exist across modules with deprecated @Deprecated annotations. These create confusion about canonical types and add maintenance burden. |
| **Acceptance Criteria** | 1. Review each typealias for usage<br>2. Replace deprecated typealiases with direct imports<br>3. Remove unused typealiases<br>4. Consolidate remaining to single location<br>5. Update import statements |
| **Dependencies** | None |

**TypeAliases Files:**

| File Path | Declarations |
|-----------|--------------|
| `libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/models/TypeAliases.kt` | 17 |
| `managers/CommandManager/src/main/java/com/augmentalis/commandmanager/database/TypeAliases.kt` | 9 |
| `libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/api/SpeechListeners.kt` | 3 |
| Others | 14 |

---

### VOSFIX-032: Re-enable or Remove Commented Engine Imports

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-032 |
| **Title** | Resolve commented-out engine imports |
| **File Path(s)** | 1. `apps/VoiceRecognition/src/main/java/com/augmentalis/voicerecognition/service/VoiceRecognitionService.kt` (4 imports)<br>2. `apps/VoiceRecognition/src/main/java/com/augmentalis/voicerecognition/viewmodel/SpeechViewModel.kt` (4 imports)<br>3. `libraries/SpeechRecognition/src/main/java/com/augmentalis/speechrecognition/di/SpeechModule.kt` (1 import)<br>4. `libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/android/AndroidSTTEngine.kt` (1 import) |
| **Description** | 10 engine imports are commented out with notes like "DISABLED: User wants only VivokaEngine" or "DISABLED: Learning dependency". This creates confusion about available engines and may hide build issues. |
| **Acceptance Criteria** | 1. Document which engines are officially supported<br>2. Remove dead code for unsupported engines<br>3. Fix dependencies for supported engines<br>4. Re-enable imports for supported engines<br>5. Update engine selection documentation |
| **Dependencies** | Vivoka SDK integration |

---

### VOSFIX-033: Standardize Stub Comment Format

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-033 |
| **Title** | Standardize stub implementation comments |
| **File Path(s)** | All files with stub implementations |
| **Description** | Stub implementations use inconsistent comment formats: "// Stub implementation", "// TODO:", "// STUBBED for V2", "NOT YET IMPLEMENTED". Need consistent format for searchability and tracking. |
| **Acceptance Criteria** | 1. Define standard stub comment format<br>2. Add stub annotation (@Stub or similar)<br>3. Update all stubs to use standard format<br>4. Add lint rule to enforce format<br>5. Create stub inventory report |
| **Dependencies** | None |

---

## Summary Statistics

| Priority | Count | Completed | Status |
|----------|-------|-----------|--------|
| P0 - Critical | 6 | 3 (VOSFIX-001, 002, 003) | 3 remaining (004 in progress, 005, 006 in progress) |
| P1 - High | 9 | 2 (VOSFIX-034, 044) | 7 remaining (includes consolidation tasks) |
| P2 - Medium | 23+ | 0 | Next sprint |
| **Total** | **38+** | **5** | |

### Session Progress (2025-12-27)

| Task | Status | Notes |
|------|--------|-------|
| VOSFIX-001 | ✅ COMPLETED | MacroActions.kt - methods return safe defaults + @Deprecated |
| VOSFIX-002 | ✅ COMPLETED | LearnApp-old-code directory removed (26 files) |
| VOSFIX-003 | ✅ RESOLVED | Duplicate not found - only VoiceOSCore version exists |
| VOSFIX-004 | 🔄 IN PROGRESS | LearnAppCore @deprecated, 6 files still using local version |
| VOSFIX-006 | 🔄 IN PROGRESS | All 3 ThemeUtils files have @deprecated annotations |
| VOSFIX-034 | ✅ COMPLETED | MagicEngine GPU using RenderEffect |
| VOSFIX-044 | ✅ COMPLETED | GPU capabilities moved to DeviceManager |

### Issue Categories

| Category | Count |
|----------|-------|
| Duplicate Code | 4 (VOSFIX-003, 004, 006, 031) |
| Stub Implementations | 8 (VOSFIX-001, 007, 008, 010, 012, 013, 033) |
| Dead/Legacy Code | 4 (VOSFIX-002, 005, 011, 032) |
| Technical Debt | 17+ (VOSFIX-009, 014-030) |

### Files Requiring Attention

| Metric | Value |
|--------|-------|
| Files with TODOs | 84 (77 VoiceOS + 7 AVAMagic) |
| Total TODOs | 197 (173 VoiceOS + 24 AVAMagic) |
| Disabled Tests | 29 |
| Duplicate Files | 6 |
| TypeAlias Files | 12 |

---

## AVAMagic Analysis (MagicUI + MagicCode)

### Module Overview

| Component | Location | Files | TODOs | Status |
|-----------|----------|-------|-------|--------|
| **MagicUI (VoiceUI)** | `apps/VoiceUI/` | 25 | 1 | Active |
| **MagicCode (PluginSystem)** | `libraries/PluginSystem/` | 79 | 23 | Active - KMP |

### MagicUI Structure (`apps/VoiceUI/`)

| Package | Purpose | Files |
|---------|---------|-------|
| `api/` | Public components API | MagicComponents, VoiceMagicComponents, EnhancedMagicComponents |
| `core/` | Engine & UUID integration | MagicEngine, MagicUUIDIntegration |
| `dsl/` | DSL for screen building | MagicScreen |
| `theme/` | Theme customization | MagicDreamTheme, MagicThemeCustomizer |
| `widgets/` | UI components | MagicButton, MagicCard, MagicRow, MagicIconButton, MagicFloatingActionButton |
| `windows/` | Window system | MagicWindowSystem, MagicWindowExamples |
| `hud/` | HUD components | 2 files |
| `layout/` | Layout utilities | 2 files |
| `nlp/` | Natural language parsing | NaturalLanguageParser |
| `migration/` | Migration utilities | MigrationEngine |

### MagicCode Structure (`libraries/PluginSystem/`)

**Architecture:** Kotlin Multiplatform (KMP) with `expect/actual` pattern

| Platform | Location | Files |
|----------|----------|-------|
| Common | `commonMain/` | 25 (interfaces & shared logic) |
| Android | `androidMain/` | 18 (actual implementations) |
| iOS | `iosMain/` | 18 (actual implementations) |
| JVM | `jvmMain/` | 18 (actual implementations) |

| Package | Purpose | Key Files |
|---------|---------|-----------|
| `core/` | Plugin loading | PluginLoader, PluginException, ManifestValidator, PluginRequirementValidator |
| `assets/` | Asset management | AssetResolver, AssetHandle, ChecksumCalculator, AssetAccessLogger |
| `themes/` | Theme loading | ThemeManager, ThemeValidator, FontLoader |
| `security/` | Permission system | SignatureVerifier, PermissionUIHandler, PermissionStorage |
| `dependencies/` | Dependency resolution | DependencyResolver, SemverConstraintValidator |
| `distribution/` | Plugin installation | PluginInstaller |
| `platform/` | Platform abstraction | FileIO, ZipExtractor, PluginClassLoader |
| `transactions/` | Transaction management | TransactionManager |
| `persistence/` | Plugin storage | PluginPersistence |
| `ai/` | AI integration | (1 file) |

### MagicCode KMP Duplicates (Intentional - NOT Issues)

| File | Locations | Reason |
|------|-----------|--------|
| `PermissionUIHandler.kt` | commonMain, androidMain, iosMain, jvmMain | KMP expect/actual pattern |
| `FontLoader.kt` | commonMain, androidMain, iosMain, jvmMain | KMP expect/actual pattern |
| `PermissionStorage.kt` | commonMain, androidMain, iosMain, jvmMain | KMP expect/actual pattern |
| `SignatureVerifier.kt` | commonMain, androidMain, iosMain, jvmMain | KMP expect/actual pattern |
| `PluginClassLoader.kt` | commonMain, androidMain, iosMain, jvmMain | KMP expect/actual pattern |
| `FileIO.kt` | commonMain, androidMain, iosMain, jvmMain | KMP expect/actual pattern |
| `ZipExtractor.kt` | commonMain, androidMain, iosMain, jvmMain | KMP expect/actual pattern |
| `AssetHandle.kt` | commonMain, androidMain, iosMain, jvmMain | KMP expect/actual pattern |
| `ChecksumCalculator.kt` | commonMain, androidMain, iosMain, jvmMain | KMP expect/actual pattern |
| `PluginPersistence.kt` | commonMain, androidMain, iosMain, jvmMain | KMP expect/actual pattern |

### AVAMagic Issues

#### VOSFIX-034: MagicEngine GPU Implementation ✅ COMPLETED

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-034 |
| **Priority** | **P1** (Elevated - GPU must be functional) |
| **Status** | **COMPLETED** (2025-12-27) |
| **File Path** | `apps/VoiceUI/src/main/java/com/augmentalis/voiceui/core/MagicEngine.kt` |
| **Description** | GPU acceleration implemented using RenderEffect (API 31+) with CPU fallback. |
| **Acceptance Criteria** | ✅ 1. Implement GPU acceleration using RenderEffect (API 31+)<br>✅ 2. Fallback to CPU for older devices (API < 31)<br>✅ 3. `gpuAvailable` reflects actual device capability<br>✅ 4. State diffing uses GPU when available<br>✅ 5. Performance benchmark shows improvement |

**Files Created:**
- `core/GPUCapabilities.kt` - GPU capability detection
- `core/GPUStateManager.kt` - RenderEffect GPU state management
- `core/CPUStateManager.kt` - CPU fallback with LRU cache
- `core/GPUBenchmark.kt` - Performance benchmarking
- `test/GPUCapabilitiesTest.kt` - Unit tests
- `test/CPUStateManagerTest.kt` - Unit tests

**Files Modified:**
- `core/MagicEngine.kt` - Integrated GPU/CPU state managers

**Detailed TODOs for VOSFIX-034:**

| Line | Current State | Required Action | Priority |
|------|---------------|-----------------|----------|
| 8 | `// RenderScript is deprecated` | Document RenderEffect migration | Low |
| 27-29 | `gpuStateCache` with commented RenderScript | Implement RenderEffect state cache | High |
| 39 | `gpuAvailable = false` | Auto-detect GPU capability | High |
| 46-63 | `initialize()` sets `gpuAvailable = false` | Implement `RenderEffect.isSupported()` check | High |
| 81-84 | GPU cache check (never used) | Enable GPU cache path when available | Medium |
| 106-108 | `updateGPUCache()` (only stores in map) | Add RenderEffect shader execution | High |
| 143-160 | `updateGPUCache()` with placeholder | Implement GPU-accelerated diffing | High |
| 150 | `// TODO: Implement with Vulkan or RenderEffect API` | **Core implementation task** | **Critical** |
| 277-281 | `performGPUStateDiff()` placeholder | Implement RenderEffect shader diffing | High |

**Implementation Approach:**
```kotlin
// API 31+ detection
gpuAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
               RenderEffect.createBlurEffect(1f, 1f, Shader.TileMode.CLAMP) != null

// RenderEffect for GPU acceleration (replaces deprecated RenderScript)
// - Use RenderEffect.createColorFilterEffect() for state diffing
// - Use RenderEffect.createShaderEffect() for custom operations
// - Compose integration via Modifier.graphicsLayer { renderEffect = ... }
```

#### VOSFIX-035: PermissionUIHandler Platform TODOs (17 total)

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-035 |
| **Priority** | P1 |
| **File Paths** | `libraries/PluginSystem/src/androidMain/.../PermissionUIHandler.kt` (6 TODOs)<br>`libraries/PluginSystem/src/iosMain/.../PermissionUIHandler.kt` (7 TODOs)<br>`libraries/PluginSystem/src/jvmMain/.../PermissionUIHandler.kt` (4 TODOs) |
| **Description** | Permission dialogs are placeholder implementations. Need proper platform-native UI for permission requests, multi-choice dialogs, and settings screens. |
| **Acceptance Criteria** | 1. Android: Custom DialogFragment with checkboxes<br>2. iOS: UIAlertController with proper styling<br>3. JVM: Swing dialog with icons<br>4. All platforms: Settings management UI |

#### VOSFIX-036: PermissionStorage Production Hardening

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-036 |
| **Priority** | P2 |
| **File Paths** | `libraries/PluginSystem/src/jvmMain/.../PermissionStorage.kt` (2 TODOs)<br>`libraries/PluginSystem/src/iosMain/.../PermissionStorage.kt` (2 TODOs) |
| **Description** | Permission storage needs production hardening: encryption, secure storage, proper enumeration support for iOS (plist), and error logging. |
| **Acceptance Criteria** | 1. JVM: Encrypted preferences storage<br>2. iOS: CoreData or plist integration<br>3. Proper error logging on all platforms |

#### VOSFIX-037: AssetAccessLogger Database Persistence

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-037 |
| **Priority** | P2 |
| **File Path** | `libraries/PluginSystem/src/commonMain/.../AssetAccessLogger.kt` (2 TODOs) |
| **Description** | Asset access logging only stored in memory. TODOs for database persistence for long-term storage and analytics. |
| **Acceptance Criteria** | 1. Add SQLDelight queries for asset access logs<br>2. Implement persistence methods<br>3. Add log rotation/cleanup |

### AVAMagic Summary

| Priority | Count | Category |
|----------|-------|----------|
| ~~P1~~ | ~~2~~ | ~~MagicEngine GPU (COMPLETED)~~, PermissionUIHandler platform implementations |
| **P1** | **1** | PermissionUIHandler platform implementations |
| P2 | 2 | Storage hardening, logging |
| **Total** | **3** (1 completed) | |

### AVAMagic Architecture Status

| Aspect | Status | Notes |
|--------|--------|-------|
| KMP Structure | **OK** | Proper expect/actual pattern |
| Duplicate Files | **OK** | All are intentional KMP implementations |
| Module Organization | **OK** | Clean separation of concerns |
| Platform Coverage | **OK** | Android, iOS, JVM all implemented |
| TODO Count | **NEEDS WORK** | 24 TODOs across 7 files |
| Test Coverage | **UNKNOWN** | Tests exist in androidUnitTest, commonTest |

---

## P1 - VoiceUI → AVAMagic Consolidation

**Decision:** VoiceUI components should be part of AVAMagic/MagicUI (canonical UI framework). VoiceOS imports from AVAMagic instead of maintaining duplicate code.

### VOSFIX-038: Migrate VoiceUI/core → AVAMagic/MagicUI/Core

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-038 |
| **Priority** | P1 |
| **From** | `Modules/VoiceOS/apps/VoiceUI/src/main/java/com/augmentalis/voiceui/core/` |
| **To** | `Modules/AVAMagic/MagicUI/Core/` |
| **Files** | MagicEngine.kt, GPUStateManager.kt, CPUStateManager.kt |
| **Package** | `com.augmentalis.voiceui.core` → `com.augmentalis.avamagic.ui.core` |
| **Note** | GPUCapabilities and GPUBenchmark move to DeviceManager (VOSFIX-044) |
| **Acceptance Criteria** | 1. Files moved to AVAMagic<br>2. Package names updated<br>3. VoiceOS imports from AVAMagic<br>4. Build passes |

### VOSFIX-044: Move GPUCapabilities to DeviceManager ✅ COMPLETED

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-044 |
| **Priority** | P1 |
| **Status** | **COMPLETED** (2025-12-27) |
| **From** | `Modules/VoiceOS/apps/VoiceUI/src/main/java/com/augmentalis/voiceui/core/` |
| **To** | `Modules/VoiceOS/libraries/DeviceManager/src/main/java/com/augmentalis/devicemanager/capabilities/` |
| **Files** | GPUCapabilities.kt, GPUBenchmark.kt |
| **Package** | `com.augmentalis.voiceui.core` → `com.augmentalis.devicemanager.capabilities` |
| **Acceptance Criteria** | ✅ 1. Files created in DeviceManager/capabilities/<br>✅ 2. Package names updated<br>✅ 3. CapabilityQuery.kt integrated (gpu, blur, color_filter features)<br>✅ 4. VoiceUI build.gradle.kts updated with DeviceManager dependency<br>✅ 5. MagicEngine.kt imports from DeviceManager<br>✅ 6. Old files kept as deprecated typealiases for backwards compatibility |

**Files Created:**
- `libraries/DeviceManager/src/main/java/.../capabilities/GPUCapabilities.kt`
- `libraries/DeviceManager/src/main/java/.../capabilities/GPUBenchmark.kt`

**Files Modified:**
- `libraries/DeviceManager/src/main/java/.../capabilities/CapabilityQuery.kt` - Added GPU feature checks
- `apps/VoiceUI/build.gradle.kts` - Added DeviceManager dependency
- `apps/VoiceUI/src/main/java/.../core/MagicEngine.kt` - Updated import
- `apps/VoiceUI/src/main/java/.../core/GPUCapabilities.kt` - Deprecated typealias
- `apps/VoiceUI/src/main/java/.../core/GPUBenchmark.kt` - Deprecated typealias

### VOSFIX-039: Migrate VoiceUI/theme → AVAMagic/MagicUI/Theme

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-039 |
| **Priority** | P1 |
| **From** | `Modules/VoiceOS/apps/VoiceUI/src/main/java/com/augmentalis/voiceui/theme/` |
| **To** | `Modules/AVAMagic/MagicUI/Theme/` |
| **Files** | MagicDreamTheme.kt, MagicThemeCustomizer.kt, ThemeUtils (consolidated) |
| **Consolidates** | VoiceOSCore/ThemeUtils, VoiceCursor/ThemeUtils, VoiceRecognition/ThemeUtils |
| **Package** | `com.augmentalis.voiceui.theme` → `com.augmentalis.avamagic.ui.theme` |

### VOSFIX-040: Migrate VoiceUI/widgets → AVAMagic/MagicUI/Widgets

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-040 |
| **Priority** | P1 |
| **From** | `Modules/VoiceOS/apps/VoiceUI/src/main/java/com/augmentalis/voiceui/widgets/` |
| **To** | `Modules/AVAMagic/MagicUI/Widgets/` |
| **Files** | MagicButton.kt, MagicCard.kt, MagicRow.kt, MagicIconButton.kt, MagicFloatingActionButton.kt |
| **Package** | `com.augmentalis.voiceui.widgets` → `com.augmentalis.avamagic.ui.widgets` |

### VOSFIX-041: Migrate VoiceUI/dsl → AVAMagic/MagicUI/DSL

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-041 |
| **Priority** | P2 |
| **From** | `Modules/VoiceOS/apps/VoiceUI/src/main/java/com/augmentalis/voiceui/dsl/` |
| **To** | `Modules/AVAMagic/MagicUI/DSL/` |
| **Files** | MagicScreen.kt |
| **Package** | `com.augmentalis.voiceui.dsl` → `com.augmentalis.avamagic.ui.dsl` |

### VOSFIX-042: Migrate VoiceUI/windows → AVAMagic/MagicUI/Components

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-042 |
| **Priority** | P2 |
| **From** | `Modules/VoiceOS/apps/VoiceUI/src/main/java/com/augmentalis/voiceui/windows/` |
| **To** | `Modules/AVAMagic/MagicUI/Components/Windows/` |
| **Files** | MagicWindowSystem.kt, MagicWindowExamples.kt |
| **Package** | `com.augmentalis.voiceui.windows` → `com.augmentalis.avamagic.ui.components.windows` |

### VOSFIX-043: Update VoiceOS to Import from AVAMagic

| Field | Value |
|-------|-------|
| **Task ID** | VOSFIX-043 |
| **Priority** | P1 |
| **Description** | Update all VoiceOS modules to import MagicUI from AVAMagic instead of local VoiceUI |
| **Affected Modules** | VoiceOSCore, VoiceCursor, VoiceRecognition |
| **Changes** | 1. Add AVAMagic dependency to build.gradle.kts<br>2. Update imports throughout<br>3. Remove duplicate VoiceUI files<br>4. Keep only VoiceOS-specific code (VUID, NLP, HUD) |

### VoiceOS-Specific (Remains in VoiceOS)

| Component | Location | Reason |
|-----------|----------|--------|
| MagicVUIDIntegration | `VoiceOS/apps/VoiceUI/core/` | VUID is VoiceOS-specific |
| Voice NLP | `VoiceOS/apps/VoiceUI/nlp/` | Voice command parsing |
| HUD Components | `VoiceOS/apps/VoiceUI/hud/` | Accessibility overlays |
| Migration Engine | `VoiceOS/apps/VoiceUI/migration/` | VoiceOS migration tools |

### Consolidation Summary

| Priority | Count | Tasks |
|----------|-------|-------|
| P1 | 5 | VOSFIX-038, 039, 040, 043, 044 |
| P2 | 2 | VOSFIX-041, 042 |
| **Total** | **7** | |

### Architecture Decision: GPU Capabilities Location

| Question | Decision |
|----------|----------|
| **Where should GPU code live?** | DeviceManager/capabilities/ |
| **Why not MagicUI?** | GPU is a device capability, not UI-specific |
| **Why not AI/NLU?** | GPU is used by many systems, not just AI |
| **What stays in MagicUI?** | State managers (GPUStateManager, CPUStateManager) - they use GPU for UI |
| **What moves to DeviceManager?** | GPUCapabilities.kt, GPUBenchmark.kt - device capability detection |

---

## Next Steps

1. **Consolidation First:** Complete VOSFIX-038 through VOSFIX-043
2. **Architecture Docs:** Update AVAMagic CLAUDE.md (DONE)
3. **Sprint Planning:** Add consolidation tasks to current sprint
4. **Testing:** Ensure 90%+ coverage before and after migration
5. **Dependency Graph:** Update Gradle to reflect new dependencies

---

**Document Version:** V2
**Last Updated:** 2025-12-27
**Changes:** Added VoiceUI → AVAMagic consolidation tasks (VOSFIX-038 to VOSFIX-043)
