# VoiceOSCore Handover: Compile Fixes + AVU DSL Evolution

**Date:** 2026-02-06
**Modules:** VoiceOSCore, VoiceDataManager
**Branch (fixes):** `claude/040226-21-consolidated-master-EQyzV`
**Branch (next work):** `claude/060226-avu-dsl-evolution`
**Priority:** High

---

## Executive Summary

Two-session effort that resolved all compile errors across VoiceOSCore and VoiceDataManager modules (300+ errors reduced to zero), followed by architectural planning for AVU DSL Evolution. All compile fixes are committed and pushed. A new branch is ready for AVU DSL implementation work.

---

## 1. Completed Work: Compile Fixes

### Commit
- **Hash:** `f6112393`
- **Branch:** `claude/040226-21-consolidated-master-EQyzV`
- **Stats:** 40 files changed, 1,264 insertions, 4,513 deletions
- **Status:** Pushed to remote

### Changes by Category

#### A. VoiceDataManager Module (5 files)
| File | Fix |
|------|-----|
| `VosDataViewModel.kt` | Added missing imports: `DataStatistics`, `StorageInfo`, `StorageLevel` from `com.augmentalis.datamanager` |
| `VosDataManagerActivity.kt` | Cascading fix from ViewModel imports |
| `DataImporter.kt` | Minor import fix |
| `ConfidenceTrackingRepository.kt` | Import path correction |
| `RecognitionLearningRepository.kt` | Import path correction |

#### B. VoiceOSCore - Duplicate File Removal (9 files deleted)
Entire `voicedatamanager/` directory removed from VoiceOSCore - these were copies of VoiceDataManager module files with wrong package declarations:
```
DELETED: managers/voicedatamanager/core/DatabaseManager.kt
DELETED: managers/voicedatamanager/core/DatabaseModule.kt
DELETED: managers/voicedatamanager/io/DataExporter.kt (+.disabled)
DELETED: managers/voicedatamanager/io/DataImporter.kt (+.disabled)
DELETED: managers/voicedatamanager/repositories/ConfidenceTrackingRepository.kt
DELETED: managers/voicedatamanager/repositories/RecognitionLearningRepository.kt
DELETED: managers/voicedatamanager/ui/GlassmorphismUtils.kt
DELETED: managers/voicedatamanager/ui/VosDataManagerActivity.kt
DELETED: managers/voicedatamanager/ui/VosDataViewModel.kt
```

#### C. VoiceOSCore - Import Fixes
| File | Fix |
|------|-----|
| `CommandManagerActivity.kt` | Added `glassMorphism`, `DepthLevel`, `GlassMorphismConfig` imports from `com.augmentalis.datamanager.ui` |
| `LocalizationManagerActivity.kt` | Added `glassMorphism` import |
| `SettingsDialog.kt` | Fixed import path from `.localizationmanager.ui.glassMorphism` to `.datamanager.ui.glassMorphism` |
| `CommandPersistence.kt` | Fixed package path: `.database.sqldelight.CommandUsageEntity` → `.managers.commandmanager.database.sqldelight.CommandUsageEntity` |

#### D. VoiceOSCore - Code Fixes
| File | Fix |
|------|-----|
| `VoiceUIStubs.kt` | Fixed `TARGET_FPS_HIGH` const declaration order (moved before property that references it); expanded HUD subsystem implementations |
| `CommandManagerActivity.kt` | Added `else` branch to exhaustive `when` on `CommandCategory` |
| `CursorActions.kt` | Removed VoiceCursorAPI bridge, implemented actions directly |
| `CursorCommandHandler.kt` | Updated to use direct CursorActions |
| `VoiceOSRpcServer.kt` | Fixed gRPC proto type stubs compile errors |
| `VoiceOSAvuRpcServer.kt` | Fixed AVU RPC server references |
| `VoiceOSJsonRpcServer.kt` | Fixed JSON RPC server references |
| `AvuProtocol.kt` | Fixed AVU protocol encoder/decoder |
| `CommandModels.kt` | Minor type corrections |
| `ISpeechEngine.kt` | Interface alignment |
| `PlatformActuals.kt` (desktop + iOS) | Added missing platform actual declarations |

#### E. VoiceOSCore - Removed Unused
| File | Reason |
|------|--------|
| `NumberToWords.kt` | 779-line utility, unused by any code |
| `StaticCommandRegistry.kt` | Removed unused entries (13 lines) |

---

## 2. Known Architecture Issues (Not Fixed - Future Work)

### Issue 1: glassMorphism Hardcoded to DataManager
- `Modifier.glassMorphism()` lives in `VoiceDataManager/ui/GlassmorphismUtils.kt`
- VoiceOSCore imports from `com.augmentalis.datamanager.ui.glassMorphism`
- **Problem:** UI theming is module-specific, not modular
- **Recommendation:** Create shared UI module (AvaUI/MagicUI) with theme-agnostic components

### Issue 2: Fake gRPC Server
- `VoiceOSRpcServer.kt` has hand-written proto data classes (not protoc-generated)
- No real serialization, method descriptors, or wire format
- gRPC server will NOT work at runtime
- **Recommendation:** Remove gRPC stubs, keep JSON-RPC + AVU RPC

### Issue 3: RPC Strategy
- Three competing RPC implementations: gRPC (broken), JSON-RPC (works), AVU (works)
- **Recommendation:** Consolidate to AVU RPC + JSON-RPC fallback

---

## 3. AVU DSL Evolution Plan (Ready for Implementation)

### Status: Plan Complete, Awaiting Implementation

### Plan Location
- **Claude plan file:** `/Users/manoj_mbpm14/.claude/plans/mighty-plotting-rose.md`
- **Target workdoc:** `Docs/Plans/AVU-DSL-Evolution-Plan-260206-V1.md` (to be created)

### Architecture: Three-Layer System
```
Layer 3: AVU Runtime (Interpreter)     ← NEW (commonMain KMP)
Layer 2: AVU DSL (File Format v2.2)    ← NEW (commonMain KMP)
Layer 1: AVU Wire Protocol (IPC v2.2)  ← EXISTING (extended)
```

### Key Decisions Made
1. **Plugin paradigm:** `.avp` text files (declarative) replace APK/JAR loading (App Store compliant)
2. **Code declaration:** Every AVU file declares 3-letter codes in `codes:` header section
3. **Interpreter:** commonMain KMP, tree-walking evaluator with sandbox
4. **Plugin lifecycle:** DISCOVERY → VALIDATION → PERMISSION GRANT → REGISTRATION → ACTIVATION

### Documents to Create
1. **Workdoc:** `Docs/Plans/AVU-DSL-Evolution-Plan-260206-V1.md`
2. **Developer Manual Chapters 81-87** (continuing from existing Ch80 in `Docs/AVA/ideacode/guides/`):
   - Ch81: AVU Protocol Overview (Three-Layer Architecture)
   - Ch82: AVU Wire Protocol (Layer 1)
   - Ch83: AVU DSL Syntax (Layer 2, EBNF grammar)
   - Ch84: AVU Code Registry (Namespaces, permissions)
   - Ch85: AVU Runtime Interpreter (Layer 3, parser, AST, sandbox)
   - Ch86: AVU Plugin System (.avp format, lifecycle, marketplace)
   - Ch87: AVU Migration Guide (MacroDSL.kt → AVU DSL)

### Existing Related Chapters
| Chapter | Title | Relationship |
|---------|-------|-------------|
| Ch37 | Universal Format v2.0 | Base AVU file format |
| Ch51 | 3Letter JSON Schema | Code format reference |
| Ch67 | Avanues Plugin Development | SUPERSEDED by Ch86 |
| Ch68 | Workflow Engine Architecture | SUPERSEDED by Ch85 |
| Ch76 | RPC Module Architecture | Extended by new RPC strategy |
| Ch80 | AVU Codec v2.2 | Foundation - AvuEscape, AvuCodeRegistry, AvuHeader |

### Implementation Phases
1. **Parser & AST** (CRITICAL) - Lexer, parser, AST in `commonMain/dsl/`
2. **Interpreter** (CRITICAL) - Tree-walking evaluator with sandbox
3. **Code Registry Extension** (HIGH) - Namespace support, CodePermissionMap
4. **Plugin Loader** (HIGH) - PluginLoader, PluginRegistry, PluginSandbox
5. **Migration Utilities** (MEDIUM) - MacroDslMigrator
6. **iOS & Desktop Dispatchers** (MEDIUM)
7. **UI & Tooling** (LOW) - DSL editor, marketplace

---

## 4. Git State

| Item | Value |
|------|-------|
| **Fixes branch** | `claude/040226-21-consolidated-master-EQyzV` |
| **Fixes commit** | `f6112393` |
| **Fixes pushed** | Yes (remote) |
| **New work branch** | `claude/060226-avu-dsl-evolution` |
| **Branched from** | `f6112393` (same commit) |
| **Working tree** | Clean |
| **Build status** | VoiceOSCore: BUILD SUCCESSFUL, VoiceDataManager: BUILD SUCCESSFUL |

---

## 5. Next Session Action Items

### Immediate (AVU DSL Work)
1. Create workdoc at `Docs/Plans/AVU-DSL-Evolution-Plan-260206-V1.md`
2. Create Developer Manual Chapters 81-87 in `Docs/AVA/ideacode/guides/`
3. Follow naming convention: `Developer-Manual-Chapter{N}-{Title}.md`
4. Begin Phase 1 implementation: Lexer + Parser + AST in `commonMain/dsl/`

### Important Context
- Developer manual chapters are in `Docs/AVA/ideacode/guides/` (NOT standalone)
- Highest existing chapter: 80 (AVU Codec v2.2)
- Two naming patterns exist: `Developer-Manual-Chapter{N}-{Title}.md` and `AVA-Developer-Manual-Chapter{N}-{Title}-{DATECODE}-V{VERSION}.md`
- No plugins exist yet (clean slate for new plugin system)
- Existing `PluginManager.kt` (~1059 lines) uses DexClassLoader - to be deprecated
- Existing `MacroDSL.kt` uses Kotlin builder pattern - to be superseded by AVU DSL
