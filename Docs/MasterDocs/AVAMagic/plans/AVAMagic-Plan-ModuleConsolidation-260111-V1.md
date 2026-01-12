# Implementation Plan: AVAMagic Module Consolidation

**Spec:** `AVAMagic-Spec-ModuleConsolidation-260111-V1.md`
**Date:** 2026-01-11
**Status:** Ready for Execution

---

## Overview

| Metric | Value |
|--------|-------|
| Platforms | KMP (Android, iOS, Desktop, Web) |
| Swarm Recommended | No (single module refactoring) |
| Total Tasks | 18 |
| Estimated Effort | 2-3 hours |

---

## Phase 1: MagicCode → AVACode Migration

**Goal:** Migrate unique MagicCode components to AVACode, resolve duplicates

### Task 1.1: Create Target Directories in AVACode
```bash
mkdir -p AVACode/src/commonMain/kotlin/com/augmentalis/avacode/{cli,generators,forms,workflows,templates}
```

### Task 1.2: Migrate CLI Module
| Source | Target |
|--------|--------|
| `MagicCode/CLI/MagicCodeCLI.kt` | `AVACode/cli/AvaCodeCLI.kt` |
| `MagicCode/CLI/MagicCodeCLIImpl.kt` | `AVACode/cli/AvaCodeCLIImpl.kt` |
| `MagicCode/CLI/FileIO.kt` | `AVACode/cli/FileIO.kt` |
| `MagicCode/CLI/FileIO.jvm.kt` | `AVACode/cli/FileIO.jvm.kt` |

**Actions:**
- Copy files
- Update package declarations
- Update class names (MagicCode → AvaCode)
- Update internal imports

### Task 1.3: Migrate Generators Module
| Source | Target |
|--------|--------|
| `MagicCode/Generators/CodeGenerator.kt` | `AVACode/generators/CodeGenerator.kt` |
| `MagicCode/Generators/Kotlin/KotlinComposeGenerator.kt` | `AVACode/generators/KotlinComposeGenerator.kt` |
| `MagicCode/Generators/Swift/SwiftUIGenerator.kt` | `AVACode/generators/SwiftUIGenerator.kt` |
| `MagicCode/Generators/React/ReactTypeScriptGenerator.kt` | `AVACode/generators/ReactTypeScriptGenerator.kt` |

### Task 1.4: Migrate Forms Module
| Source | Target |
|--------|--------|
| `MagicCode/Forms/*.kt` (7 files) | `AVACode/forms/*.kt` |

### Task 1.5: Migrate Workflows Module
| Source | Target |
|--------|--------|
| `MagicCode/Workflows/*.kt` (6 files) | `AVACode/workflows/*.kt` |

### Task 1.6: Migrate Templates Module
| Source | Target |
|--------|--------|
| `MagicCode/Templates/Core/*.kt` (7 files) | `AVACode/templates/*.kt` |

### Task 1.7: Resolve Parser/AST Duplicates
- **Keep:** `AVACode/dsl/VosParser.kt`
- **Keep:** `AVACode/dsl/VosAstNode.kt`
- **Skip:** `MagicCode/Parser/VosParser.kt` (duplicate)
- **Skip:** `MagicCode/AST/MagicUINode.kt` (duplicate)
- **Evaluate:** `MagicCode/Parser/CompactSyntaxParser.kt` - merge if unique
- **Evaluate:** `MagicCode/Parser/JsonDSLParser.kt` - merge if unique

### Task 1.8: Update AVACode build.gradle.kts
- Add source sets for new directories
- Verify dependencies

### Task 1.9: Verify Phase 1 Build
```bash
./gradlew :Modules:AVAMagic:AVACode:compileKotlin
```

### Task 1.10: Delete MagicCode Folder
```bash
rm -rf Modules/AVAMagic/MagicCode
```
Update `settings.gradle.kts` to remove MagicCode module references.

---

## Phase 2: MagicUI → AVAUI Migration

**Goal:** Migrate unique MagicUI components to AVAUI, remove duplicates

### Task 2.1: Migrate Android Mappers (30+ files)
| Source | Target |
|--------|--------|
| `MagicUI/Components/Renderers/Android/mappers/*.kt` | `AVAUI/Renderers/Android/mappers/*.kt` |

**Files include:**
- AlertMapper.kt, AppBarMapper.kt, AvatarMapper.kt
- BadgeMapper.kt, BottomNavMapper.kt, BreadcrumbMapper.kt
- ChipMapper.kt, ConfirmMapper.kt, ContextMenuMapper.kt
- DialogMapper.kt, ModalMapper.kt, ProgressBarMapper.kt
- SnackbarMapper.kt, ToastMapper.kt
- feedback/AdvancedFeedbackMappers.kt
- input/*.kt (15+ files)

### Task 2.2: Migrate Unique Component Files
| Source | Target |
|--------|--------|
| `MagicUI/Components/Display/DisplayComponents.kt` | `AVAUI/Display/` |
| `MagicUI/Components/Feedback/FeedbackComponents.kt` | `AVAUI/Feedback/` |
| `MagicUI/Components/Floating/FloatingComponents.kt` | `AVAUI/Floating/` |
| `MagicUI/Components/Input/InputComponents.kt` | `AVAUI/Input/` |
| `MagicUI/Components/Layout/LayoutComponents.kt` | `AVAUI/Layout/` |
| `MagicUI/Components/Navigation/NavigationComponents.kt` | `AVAUI/Navigation/` |

### Task 2.3: Update Package Declarations
All migrated files: `com.augmentalis.magicui.*` → `com.augmentalis.avaui.*`

### Task 2.4: Update AVAUI build.gradle.kts
- Add source sets for new directories
- Verify dependencies

### Task 2.5: Verify Phase 2 Build
```bash
./gradlew :Modules:AVAMagic:AVAUI:Core:compileKotlin
./gradlew :Modules:AVAMagic:AVAUI:Renderers:Android:compileKotlin
```

### Task 2.6: Delete MagicUI Folder
```bash
rm -rf Modules/AVAMagic/MagicUI
```
Update `settings.gradle.kts` to remove MagicUI module references.

---

## Phase 3: Final Verification & Cleanup

### Task 3.1: Update settings.gradle.kts
Remove all `include()` statements for:
- `:Modules:AVAMagic:MagicCode:*`
- `:Modules:AVAMagic:MagicUI:*`

### Task 3.2: Full Build Verification
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./gradlew :Modules:AVAMagic:build
```

### Task 3.3: Run Tests
```bash
./gradlew :Modules:AVAMagic:allTests
```

### Task 3.4: Update MasterDocs
- Update `Docs/MasterDocs/AVAMagic/` with new structure
- Update CLASS-INDEX.ai.md
- Update PLATFORM-INDEX.ai.md

### Task 3.5: Commit Changes
```bash
git add -A
git commit -m "refactor(avamagic): consolidate MagicCode→AVACode, MagicUI→AVAUI

- Migrate CLI, Generators, Forms, Workflows, Templates to AVACode
- Migrate Android mappers and unique components to AVAUI
- Remove duplicate VosParser and AST implementations
- Delete MagicCode/ and MagicUI/ folders
- Update settings.gradle.kts module references

Reduces AVAMagic codebase by ~50% with zero functionality loss"
```

---

## Execution Order Summary

```
┌─────────────────────────────────────────────────────────────┐
│ Phase 1: MagicCode → AVACode                                │
│ Tasks 1.1-1.10 (10 tasks)                                   │
│ └── Checkpoint: AVACode builds ✓                            │
├─────────────────────────────────────────────────────────────┤
│ Phase 2: MagicUI → AVAUI                                    │
│ Tasks 2.1-2.6 (6 tasks)                                     │
│ └── Checkpoint: AVAUI builds ✓                              │
├─────────────────────────────────────────────────────────────┤
│ Phase 3: Verification & Cleanup                             │
│ Tasks 3.1-3.5 (5 tasks)                                     │
│ └── Checkpoint: Full build + tests ✓                        │
└─────────────────────────────────────────────────────────────┘
```

---

## Rollback Plan

If issues occur:
1. `git stash` current changes
2. `git checkout -- .` to restore
3. Review error logs
4. Fix and retry specific task

---

## Time Estimates

| Phase | Sequential | Notes |
|-------|------------|-------|
| Phase 1 | 45-60 min | File copying + imports |
| Phase 2 | 60-90 min | More files, mappers |
| Phase 3 | 15-30 min | Verification |
| **Total** | **2-3 hours** | |

---

*Plan created: 2026-01-11*
*Ready for: /i.implement or manual execution*
