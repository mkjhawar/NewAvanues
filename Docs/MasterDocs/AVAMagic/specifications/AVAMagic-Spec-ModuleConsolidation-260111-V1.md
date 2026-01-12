# Specification: AVAMagic Module Consolidation

**Version:** 1.0
**Date:** 2026-01-11
**Status:** Ready for Implementation
**Author:** Claude Analysis

---

## Executive Summary

Consolidate redundant modules within AVAMagic to reduce maintenance burden and eliminate code duplication:

| Consolidation | Source | Target | Action |
|---------------|--------|--------|--------|
| Code Generation | `MagicCode/` (35 files) | `AVACode/` (34 files) | Merge unique, delete source |
| UI Framework | `MagicUI/` (100+ files) | `AVAUI/` (100+ files) | Merge unique, delete source |

**Expected Outcome:** ~50% reduction in AVAMagic codebase size with zero functionality loss.

---

## Problem Statement

### Current State
- **MagicCode** and **AVACode** both contain VOS parsers and AST implementations
- **MagicUI** and **AVAUI** share ~80% identical folder structure and files
- Duplicate code increases maintenance effort and risk of divergent implementations
- No clear "source of truth" for UI components or code generation

### Impact
- Bug fixes must be applied in multiple locations
- New developers confused about which module to use
- Build times increased due to duplicate compilation
- Higher risk of inconsistent behavior across modules

---

## Functional Requirements

### FR-1: MagicCode → AVACode Migration

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-1.1 | Migrate `MagicCode/CLI/*` to `AVACode/cli/` | HIGH |
| FR-1.2 | Migrate `MagicCode/Generators/*` to `AVACode/generators/` | HIGH |
| FR-1.3 | Migrate `MagicCode/Forms/*` to `AVACode/forms/` | MEDIUM |
| FR-1.4 | Migrate `MagicCode/Workflows/*` to `AVACode/workflows/` | MEDIUM |
| FR-1.5 | Migrate `MagicCode/Templates/*` to `AVACode/templates/` | MEDIUM |
| FR-1.6 | Resolve duplicate VosParser (use AVACode version) | HIGH |
| FR-1.7 | Resolve duplicate AST nodes (use AVACode version) | HIGH |
| FR-1.8 | Update all imports to use `AVACode` package | HIGH |
| FR-1.9 | Delete `MagicCode/` folder after verification | HIGH |

### FR-2: MagicUI → AVAUI Migration

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-2.1 | Migrate `MagicUI/Renderers/Android/mappers/*` to AVAUI | HIGH |
| FR-2.2 | Migrate `MagicUI/Components/Display/*` to AVAUI | MEDIUM |
| FR-2.3 | Migrate `MagicUI/Components/Feedback/*` to AVAUI | MEDIUM |
| FR-2.4 | Migrate `MagicUI/Components/Floating/*` to AVAUI | MEDIUM |
| FR-2.5 | Migrate `MagicUI/Components/Input/*` to AVAUI | MEDIUM |
| FR-2.6 | Migrate `MagicUI/Components/Layout/*` to AVAUI | MEDIUM |
| FR-2.7 | Migrate `MagicUI/Components/Navigation/*` to AVAUI | MEDIUM |
| FR-2.8 | Remove duplicate folders (ARGScanner, Adapters, etc.) | HIGH |
| FR-2.9 | Update all imports to use `AVAUI` package | HIGH |
| FR-2.10 | Delete `MagicUI/` folder after verification | HIGH |

### FR-3: Build System Updates

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-3.1 | Update `settings.gradle.kts` to remove deleted modules | HIGH |
| FR-3.2 | Update module dependencies in `build.gradle.kts` files | HIGH |
| FR-3.3 | Ensure all builds pass after migration | HIGH |
| FR-3.4 | Run existing tests and fix failures | HIGH |

---

## Non-Functional Requirements

### NFR-1: Zero Functionality Loss
All existing functionality must be preserved. No features removed.

### NFR-2: Build Compatibility
- Must compile on JDK 17
- Must support Android API 34
- Must support all KMP targets (Android, iOS, Desktop, Web)

### NFR-3: Import Compatibility
Provide clear mapping of old → new import paths for downstream consumers.

### NFR-4: Incremental Migration
Each phase must be independently verifiable with passing builds.

---

## Source File Inventory

### MagicCode Files to Migrate (Unique)

```
MagicCode/
├── CLI/                              → AVACode/cli/
│   ├── MagicCodeCLI.kt              → cli/AvaCodeCLI.kt
│   ├── MagicCodeCLIImpl.kt          → cli/AvaCodeCLIImpl.kt
│   └── FileIO.kt                    → cli/FileIO.kt
│
├── Generators/                       → AVACode/generators/
│   ├── Kotlin/KotlinComposeGenerator.kt
│   ├── Swift/SwiftUIGenerator.kt
│   ├── React/ReactTypeScriptGenerator.kt
│   └── CodeGenerator.kt
│
├── Forms/                            → AVACode/forms/
│   ├── DatabaseSchema.kt
│   ├── FieldDefinition.kt
│   ├── FormBinding.kt
│   ├── FormDefinition.kt
│   ├── ValidationRule.kt
│   └── examples/*.kt
│
├── Workflows/                        → AVACode/workflows/
│   ├── StepDefinition.kt
│   ├── WorkflowDefinition.kt
│   ├── WorkflowInstance.kt
│   ├── WorkflowPersistence.kt
│   └── examples/*.kt
│
└── Templates/Core/                   → AVACode/templates/
    ├── AppConfig.kt
    ├── AppTemplate.kt
    ├── BrandingConfig.kt
    ├── DatabaseConfig.kt
    ├── Feature.kt
    ├── TemplateGenerator.kt
    └── TemplateMetadata.kt
```

### MagicCode Files to Skip (Duplicates)

```
MagicCode/
├── Parser/VosParser.kt              # Use AVACode/dsl/VosParser.kt
├── Parser/CompactSyntaxParser.kt    # Evaluate if needed
├── Parser/JsonDSLParser.kt          # Evaluate if needed
└── AST/MagicUINode.kt               # Use AVACode/dsl/VosAstNode.kt
```

### MagicUI Files to Migrate (Unique)

```
MagicUI/Components/
├── Renderers/Android/mappers/       → AVAUI/Renderers/Android/mappers/
│   ├── AlertMapper.kt
│   ├── AppBarMapper.kt
│   ├── AvatarMapper.kt
│   ├── BadgeMapper.kt
│   ├── BottomNavMapper.kt
│   ├── BreadcrumbMapper.kt
│   ├── ChipMapper.kt
│   ├── ConfirmMapper.kt
│   ├── ContextMenuMapper.kt
│   ├── DialogMapper.kt
│   ├── ModalMapper.kt
│   ├── ProgressBarMapper.kt
│   ├── SnackbarMapper.kt
│   ├── ToastMapper.kt
│   ├── feedback/AdvancedFeedbackMappers.kt
│   └── input/*.kt (15+ mappers)
│
├── Display/DisplayComponents.kt     → AVAUI/Display/
├── Feedback/FeedbackComponents.kt   → AVAUI/Feedback/
├── Floating/FloatingComponents.kt   → AVAUI/Floating/
├── Input/InputComponents.kt         → AVAUI/Input/
├── Layout/LayoutComponents.kt       → AVAUI/Layout/
└── Navigation/NavigationComponents.kt → AVAUI/Navigation/
```

### MagicUI Files to Skip (Duplicates - Use AVAUI)

```
MagicUI/
├── Components/ARGScanner/           # Duplicate
├── Components/Adapters/             # Duplicate
├── Components/AssetManager/         # Duplicate
├── Components/Foundation/           # Duplicate
├── Components/IPCConnector/         # Duplicate
├── Components/StateManagement/      # Duplicate
├── Components/TemplateLibrary/      # Duplicate
├── Components/ThemeBuilder/         # Duplicate
├── Components/VoiceCommandRouter/   # Duplicate
├── Core/                            # Use AVAUI/Core
├── CoreTypes/                       # Use AVAUI/CoreTypes
├── DesignSystem/                    # Use AVAUI/DesignSystem
├── Foundation/                      # Use AVAUI/Foundation
├── StateManagement/                 # Use AVAUI/StateManagement
├── Theme/                           # Use AVAUI/Theme
├── ThemeBridge/                     # Use AVAUI/ThemeBridge
└── UIConvertor/                     # Use AVAUI/UIConvertor
```

---

## Acceptance Criteria

### AC-1: MagicCode Migration Complete
- [ ] All unique MagicCode files exist in AVACode
- [ ] All imports updated to new package paths
- [ ] `./gradlew :Modules:AVAMagic:AVACode:build` passes
- [ ] `MagicCode/` folder deleted
- [ ] No references to `MagicCode` in codebase

### AC-2: MagicUI Migration Complete
- [ ] All unique MagicUI files exist in AVAUI
- [ ] All imports updated to new package paths
- [ ] `./gradlew :Modules:AVAMagic:AVAUI:Core:build` passes
- [ ] `MagicUI/` folder deleted
- [ ] No references to `MagicUI` in codebase (except historical docs)

### AC-3: Full Build Verification
- [ ] `./gradlew build` completes without errors
- [ ] All existing tests pass
- [ ] No new compiler warnings introduced

### AC-4: Documentation Updated
- [ ] MasterDocs updated with new structure
- [ ] Import migration guide created for downstream consumers

---

## Out of Scope

| Item | Reason |
|------|--------|
| VUID/UUID Consolidation | Deferred - VoiceOSCoreNG dependencies need separate analysis |
| MagicTools/ changes | LSP server is independent, no changes needed |
| apps/ changes | VoiceOS, VoiceUI apps unchanged in this phase |
| Common/AvaElements | Separate library, not part of this consolidation |

---

## Dependencies

| Dependency | Type | Impact |
|------------|------|--------|
| Gradle 8.x | Build | Required for KMP |
| JDK 17 | Build | Required for Android |
| Kotlin 1.9+ | Build | Required for KMP |
| VoiceOSCoreNG | Consumer | May need import updates |

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Missing file during migration | Medium | High | File inventory validation |
| Import path errors | High | Medium | Automated find/replace |
| Build failures | Medium | High | Incremental migration with checkpoints |
| Test failures | Medium | Medium | Run tests after each phase |
| Downstream breakage | Low | High | Document import changes |

---

## Appendix: Package Name Changes

### MagicCode → AVACode

| Old Package | New Package |
|-------------|-------------|
| `net.ideahq.ideamagic.codegen.cli` | `com.augmentalis.avacode.cli` |
| `net.ideahq.ideamagic.codegen.generators` | `com.augmentalis.avacode.generators` |
| `com.augmentalis.magiccode.forms` | `com.augmentalis.avacode.forms` |
| `com.augmentalis.magiccode.workflows` | `com.augmentalis.avacode.workflows` |
| `com.augmentalis.ideamagic.templates` | `com.augmentalis.avacode.templates` |

### MagicUI → AVAUI

| Old Package | New Package |
|-------------|-------------|
| `com.augmentalis.magicui.components.*` | `com.augmentalis.avaui.components.*` |
| `com.augmentalis.magicelements.renderer.android` | `com.augmentalis.avaui.renderer.android` |
| `com.augmentalis.ideamagic.components.*` | `com.augmentalis.avaui.components.*` |

---

*Specification created: 2026-01-11*
*Ready for: /i.plan*
