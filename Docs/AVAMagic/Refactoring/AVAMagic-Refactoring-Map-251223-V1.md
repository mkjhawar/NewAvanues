# AVAMagic Refactoring Map

**Module:** AVAMagic
**Date:** 2025-12-23
**Version:** 1.0
**Status:** Planning

---

## Objective

Restructure AVAMagic to follow NewAvanues standards with clear product naming:
- **Module Name:** AVAMagic
- **Products:** MagicUI (UI framework), MagicCode (code generation), MagicTools (dev tools)
- **Package Base:** `com.augmentalis.magicui.*` and `com.augmentalis.magiccode.*`

---

## Current State Analysis

### Top-Level Directories (17 total)
```
AVAMagic/
├── Code/              # Unclear purpose
├── CodeGen/           # Code generation → MagicCode
├── Components/        # UI components → MagicUI/Components
├── Core/              # Core utilities → Keep as Core/
├── Data/              # Data files → Keep as Data/
├── Docs/              # Documentation → Keep as Docs/
├── Examples/          # Examples → Keep as Examples/
├── IPC/               # IPC utilities → Keep as IPC/
├── Libraries/         # Libraries → Review and reorganize
├── Observability/     # Observability → Keep as Observability/
├── PluginRecovery/    # Plugin recovery → Keep as PluginRecovery/
├── Renderers/         # Platform renderers → MagicUI/Renderers
├── Templates/         # Templates → MagicCode/Templates OR MagicTools
├── UI/                # UI core → MagicUI/Core OR consolidate
└── VoiceIntegration/  # Voice integration → Keep as VoiceIntegration/
```

### Package Naming Issues (CRITICAL)

Current packages are HIGHLY inconsistent:

| File | Current Package | Issues |
|------|----------------|---------|
| CodeGen/Generators/ | `net.ideahq.avamagic.codegen` | Wrong domain! Should be `com.augmentalis` |
| UI/src/ | `com.augmentalis.avanues.avaui.dsl` | Has `avanues` typo/extra level |
| Templates/Core/ | `com.augmentalis.avanues.avamagic.templates` | Has `avanues` extra level |
| Renderers/iOSRenderer/ | `com.augmentalis.avamagic.renderer.ios` | Should be `magicui` not `avamagic` |
| PluginRecovery/ | `com.augmentalis.avamagic.plugin` | Should be standardized |
| VoiceIntegration/ | `com.augmentalis.avamagic.voice` | Should be standardized |
| IPC/DSLSerializer/ | `com.augmentalis.avamagic.ipc.dsl` | Should be standardized |
| Components/AssetManager/ | `com.augmentalis.universal.assetmanager` | Wrong prefix |
| Libraries/Preferences/ | `com.augmentalis.voiceos.preferences` | Wrong module! |
| Data/ | `com.augmentalis.voiceavanue.client` | Wrong module! |

---

## Target Structure

### Directory Structure

```
Modules/AVAMagic/
├── .claude/
├── .ideacode/
├── Docs/
│
├── Core/                          # Shared core utilities
│   └── src/commonMain/kotlin/com/augmentalis/magicui/core/
│
├── MagicUI/                       # MagicUI Product
│   ├── Theme/                     # Theme system (NEW - for Theme Creator)
│   │   └── src/commonMain/kotlin/com/augmentalis/magicui/theme/
│   │       ├── io/              # Import/export (NEW files we created)
│   │       ├── tokens/          # Design tokens
│   │       └── manager/         # Theme management
│   │
│   ├── Components/                # UI components (FROM Components/)
│   │   ├── Foundation/
│   │   ├── Phase2/
│   │   ├── Phase3/
│   │   └── Builder/
│   │
│   ├── DesignSystem/              # Design system (FROM UI/DesignSystem if exists)
│   │   └── src/commonMain/kotlin/com/augmentalis/magicui/design/
│   │
│   ├── DSL/                       # DSL (FROM UI/src)
│   │   └── src/commonMain/kotlin/com/augmentalis/magicui/dsl/
│   │
│   └── Renderers/                 # Platform renderers (FROM Renderers/)
│       ├── Android/
│       ├── iOS/
│       ├── Web/
│       └── Desktop/
│
├── MagicCode/                     # MagicCode Product
│   ├── Parser/                    # Parsers (FROM CodeGen/Parser)
│   │   └── src/commonMain/kotlin/com/augmentalis/magiccode/parser/
│   ├── Generator/                 # Generators (FROM CodeGen/Generators)
│   │   └── src/commonMain/kotlin/com/augmentalis/magiccode/generator/
│   └── Templates/                 # Templates (FROM Templates/)
│       └── src/commonMain/kotlin/com/augmentalis/magiccode/templates/
│
├── MagicTools/                    # Development Tools
│   └── ThemeCreator/              # Theme Creator (NEW)
│       ├── src/
│       └── src-tauri/
│
├── IPC/                           # Inter-process communication (KEEP)
│   └── src/commonMain/kotlin/com/augmentalis/magicui/ipc/
│
├── VoiceIntegration/              # Voice integration (KEEP)
│   └── src/commonMain/kotlin/com/augmentalis/magicui/voice/
│
├── PluginRecovery/                # Plugin recovery (KEEP)
│   └── src/commonMain/kotlin/com/augmentalis/magicui/plugins/
│
├── Libraries/                     # Shared libraries (REVIEW - may consolidate)
├── Data/                          # Data files (KEEP)
├── Examples/                      # Examples (KEEP)
└── Observability/                 # Observability (KEEP)
```

---

## Directory Mapping

| Current Directory | New Directory | Action | Priority |
|-------------------|---------------|--------|----------|
| `UI/ThemeManager/` (NEW) | `MagicUI/Theme/` | Create new | 🔴 High |
| `Components/` | `MagicUI/Components/` | Move | 🔴 High |
| `Renderers/` | `MagicUI/Renderers/` | Move | 🔴 High |
| `UI/src/` (DSL) | `MagicUI/DSL/` | Move | 🔴 High |
| `CodeGen/` | `MagicCode/` | Move & rename subdirs | 🔴 High |
| `Templates/` | `MagicCode/Templates/` | Move | 🔴 High |
| `Core/` | `Core/` | Review & update packages | 🟡 Medium |
| `IPC/` | `IPC/` | Update packages | 🟡 Medium |
| `VoiceIntegration/` | `VoiceIntegration/` | Update packages | 🟡 Medium |
| `PluginRecovery/` | `PluginRecovery/` | Update packages | 🟡 Medium |
| `Libraries/` | Review each | Consolidate or keep | 🟢 Low |
| `Data/` | `Data/` | Fix packages (wrong module!) | 🔴 High |
| `Code/` | Investigate | Determine purpose | 🟢 Low |

---

## Package Mapping

### MagicUI Packages

| Current Package | New Package | Files Affected |
|----------------|-------------|----------------|
| `com.augmentalis.avanues.avaui.dsl` | `com.augmentalis.magicui.dsl` | ~10 |
| `com.augmentalis.avamagic.renderer.*` | `com.augmentalis.magicui.renderers.*` | ~15 |
| `com.augmentalis.ideamagic.components.*` | `com.augmentalis.magicui.components.*` | ~30 |
| `com.augmentalis.universal.assetmanager` | `com.augmentalis.magicui.assets` | ~5 |
| `com.augmentalis.avamagic.ipc.*` | `com.augmentalis.magicui.ipc.*` | ~5 |
| `com.augmentalis.avamagic.voice.*` | `com.augmentalis.magicui.voice.*` | ~3 |
| `com.augmentalis.avamagic.plugin.*` | `com.augmentalis.magicui.plugins.*` | ~2 |
| NEW | `com.augmentalis.magicui.theme.*` | 3 (files we created) |
| NEW | `com.augmentalis.magicui.design.*` | TBD |

### MagicCode Packages

| Current Package | New Package | Files Affected |
|----------------|-------------|----------------|
| `net.ideahq.avamagic.codegen.*` | `com.augmentalis.magiccode.generator.*` | ~10 |
| `com.augmentalis.avanues.avamagic.templates.*` | `com.augmentalis.magiccode.templates.*` | ~5 |

### Packages to Fix (Wrong Module!)

| Current Package | Issue | Action |
|----------------|-------|--------|
| `com.augmentalis.voiceos.preferences` | In AVAMagic but uses VoiceOS package | Move to VoiceOS OR rename |
| `com.augmentalis.voiceavanue.client` | In AVAMagic but uses VoiceAvanue package | Move OR rename |
| `com.augmentalis.voiceavanue.service` | In AVAMagic but uses VoiceAvanue package | Move OR rename |

---

## File Inventory

### Recently Created Files (Not Yet Moved)

| File | Current Location | Target Location |
|------|------------------|-----------------|
| `ThemeIO.kt` | `UI/ThemeManager/.../ideamagic/ui/thememanager/io/` | `MagicUI/Theme/.../magicui/theme/io/` |
| `W3CTokenParser.kt` | `UI/ThemeManager/.../ideamagic/ui/thememanager/io/parsers/` | `MagicUI/Theme/.../magicui/theme/io/parsers/` |
| `MagicUIParser.kt` | `UI/ThemeManager/.../ideamagic/ui/thememanager/io/parsers/` | `MagicUI/Theme/.../magicui/theme/io/parsers/` |

### Estimated File Counts by Category

| Category | Estimated Files | Action Needed |
|----------|----------------|---------------|
| Components | ~50 | Move + update packages |
| Renderers | ~20 | Move + update packages |
| CodeGen | ~15 | Move + update packages |
| Templates | ~10 | Move + update packages |
| UI/DSL | ~10 | Move + update packages |
| Theme (new) | 3 | Move + update packages |
| IPC | ~5 | Update packages |
| Voice | ~3 | Update packages |
| Plugin | ~2 | Update packages |
| **TOTAL** | **~118 files** | - |

---

## Class Renaming

### No Class Renaming Needed!

**Decision:** Keep `MagicUI` and `MagicCode` class names as-is
- `MagicUIParser` → `MagicUIParser` ✅ (no change)
- `MagicUIRuntime` → `MagicUIRuntime` ✅ (no change)
- Only package paths change, not class names

---

## Breaking Changes

### Package Name Changes (With Aliases)

All package changes will have type aliases for backwards compatibility:

```kotlin
// Core/compat/src/commonMain/kotlin/com/augmentalis/magicui/compat/Deprecated.kt

@Deprecated("Use com.augmentalis.magicui.dsl instead")
typealias OldDslType = com.augmentalis.magicui.dsl.NewDslType
```

### Deep Link Changes

| Old | New | Backwards Compatible? |
|-----|-----|-----------------------|
| N/A | `magicui://theme?data=...` | N/A (new feature) |

---

## Build File Changes

### settings.gradle.kts

```kotlin
// OLD
include(":Modules:AVAMagic:UI")
include(":Modules:AVAMagic:Components:Foundation")
include(":Modules:AVAMagic:Renderers:Android")
include(":Modules:AVAMagic:CodeGen:Generators")

// NEW
include(":Modules:AVAMagic:Core")
include(":Modules:AVAMagic:MagicUI:Theme")
include(":Modules:AVAMagic:MagicUI:Components:Foundation")
include(":Modules:AVAMagic:MagicUI:Renderers:Android")
include(":Modules:AVAMagic:MagicCode:Generator")
include(":Modules:AVAMagic:MagicTools:ThemeCreator")
```

### Module Dependencies

Update all `implementation(project(...))` references

---

## Estimated Effort

### Time Breakdown

| Phase | Task | Estimated Time |
|-------|------|----------------|
| 1 | Analysis & Planning | ✅ 30 min (done) |
| 2 | Create new directory structure | 15 min |
| 3 | Move MagicUI files | 45 min |
| 4 | Move MagicCode files | 30 min |
| 5 | Update all package declarations | 60 min |
| 6 | Update all import statements | 45 min |
| 7 | Update build files | 30 min |
| 8 | Fix misplaced packages (VoiceOS/VoiceAvanue) | 20 min |
| 9 | Create compatibility aliases | 20 min |
| 10 | Verification & testing | 45 min |
| **TOTAL** | | **~5.5 hours** |

### File Modification Estimate

- **118 Kotlin files** to update (packages + imports)
- **~15 build files** to update
- **~10 documentation files** to update
- **1 compatibility file** to create

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Build breaks | High | Test after each major phase |
| Missing imports | Medium | Use IDE refactoring tools where possible |
| Wrong packages (VoiceOS/VoiceAvanue) | Medium | Decide: move files OR rename packages |
| Broken dependencies | High | Update settings.gradle.kts carefully |
| Lost backwards compatibility | Low | Type aliases maintain compatibility |

---

## Critical Decisions Needed

### 1. Libraries/Preferences with VoiceOS package

**Current:** `Libraries/Preferences/` uses `com.augmentalis.voiceos.preferences`

**Options:**
- A) Move to VoiceOS module (proper home)
- B) Rename package to `com.augmentalis.magicui.preferences`

**Recommendation:** Option A - Move to VoiceOS

### 2. Data/ with VoiceAvanue package

**Current:** `Data/` uses `com.augmentalis.voiceavanue.client`

**Options:**
- A) Move to WebAvanue module
- B) Rename package to `com.augmentalis.magicui.data`

**Recommendation:** Option B - Rename (if used by MagicUI)

### 3. Code/ directory purpose

**Current:** Empty or unclear purpose

**Options:**
- A) Remove if empty
- B) Investigate and consolidate into MagicCode/

**Recommendation:** Investigate first

---

## Verification Checklist

Post-refactoring verification:

- [ ] All builds succeed (`./gradlew :Modules:AVAMagic:build`)
- [ ] All tests pass (`./gradlew :Modules:AVAMagic:test`)
- [ ] No `net.ideahq.*` packages remain
- [ ] No `com.augmentalis.avanues.*` packages remain (typo)
- [ ] No `com.augmentalis.avamagic.*` packages remain (use magicui/magiccode)
- [ ] VoiceOS/VoiceAvanue packages resolved
- [ ] All imports resolve
- [ ] settings.gradle.kts updated
- [ ] Documentation updated
- [ ] Compatibility file created

---

## Next Steps

1. **Get approval** for this refactoring map
2. **Decide** on Libraries/Preferences and Data/ packages
3. **Create backup branch:** `refactor/avamagic-structure-251223`
4. **Execute** refactoring phases 2-10
5. **Verify** all checks pass
6. **Document** completion in refactoring report

---

**Status:** Awaiting approval to proceed
**Created:** 2025-12-23
