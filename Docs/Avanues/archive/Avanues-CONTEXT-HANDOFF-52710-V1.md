# Context Protocol: VoiceAvanue Platform Restructuring
**Session Date**: 2025-10-27 03:18-04:45 PST
**Status**: In Progress - Specification Complete, Ready for Planning
**Next Action**: Execute `/idea.plan` for spec 003

---

## 🎯 CRITICAL DECISIONS MADE

### 1. Platform Naming & Structure ✅ FINALIZED

| Aspect | Value | Rationale |
|--------|-------|-----------|
| **Project Directory Name** | VoiceAvanue | Platform project container |
| **Code Namespace** | com.augmentalis.voiceos | Consistent with VoiceOS brand |
| **Platform Purpose** | Pre-compiled runtime libraries | App Store compliant, no dynamic code |
| **Main Applications** | VoiceOS, AVA AI | Use platform via Gradle composite build |

### 2. Architecture Pattern ✅ FINALIZED

**Three-Tier System:**

```
Tier 1: VoiceAvanue Platform (Pre-compiled runtime)
├── runtime/libraries/AvaUI/         (DSL → UI at runtime)
├── runtime/libraries/AvaCode/       (DSL → Source code export)
└── runtime/libraries/[15+ others]     (Migrated from avenue-redux)

Tier 2: Applications (Consume platform via composite build)
├── VoiceOS/                           (Voice-controlled OS app)
└── AVA AI/                            (AI assistant + micro-app generator)

Tier 3: Configuration (YAML/DSL files - not executable code)
└── VoiceAvanue/apps/                  (App definitions, layouts)
```

### 3. Key Distinction: AVA AI vs NLP ✅ CLARIFIED

**User Correction Applied:**
- **AVA AI** = Full AI functionality (NLU, NLP, LLM, vision, training, micro-app generation)
- **NLP** = Natural Language Processing (subset of AVA AI capabilities)
- AVA AI is a complete application, not just an NLP module

### 4. AvaUI vs AvaCode ✅ FINALIZED

| Library | Purpose | Input | Output | When |
|---------|---------|-------|--------|------|
| **AvaUI** | Runtime UI interpretation | DSL/YAML | Live UI (Button instances) | Runtime |
| **AvaCode** | Code generation | DSL/YAML | Source files (.kt, .swift) | Build/Export |

---

## 📁 REPOSITORY STATE

### Current Location
- **Directory**: `/Volumes/M Drive/Coding/AvaCode` → **RENAME TO** `/Volumes/M Drive/Coding/VoiceAvanue`
- **Branch**: `003-platform-architecture-restructure`
- **Git Status**: Spec created, ready for planning

### Files Created This Session

**Specification:**
- `/specs/003-platform-architecture-restructure/spec.md` (30 FRs, 5 user stories, 10 success criteria)
- `/specs/003-platform-architecture-restructure/checklists/requirements.md` (Validation: ALL PASSED ✅)

**Existing Work (Phase 1-2 from spec 002):**
- 11 files in `runtime/plugin-system/src/commonMain/kotlin/.../plugins/ui/`
- Namespace: `com.augmentalis.avacode.plugins.ui.*`
- **REQUIRES UPDATE TO**: `com.augmentalis.voiceos.avaui.*`

### Files to Update After Rename

**Configuration Files:**
1. `settings.gradle.kts` → `rootProject.name = "VoiceAvanue"`
2. `gradle.properties` → `GROUP=com.augmentalis.voiceos`
3. `.ideacode/memory/principles.md` → Update project name references
4. `CLAUDE.md` → Update project name
5. `README.md` → Update if exists

**Spec Files:**
1. `specs/002-avaui-uik-enhancements/spec.md` → Update namespace references
2. `specs/003-platform-architecture-restructure/spec.md` → Update if needed (already uses correct names)

---

## 🗺️ RELATED CODEBASES

### Avenue-Redux (Source for Migration)
**Location**: `/Volumes/M Drive/Coding/Avanue/avenue-redux/`
**Status**: Production VoiceOS code with 15+ modules
**Modules to Migrate**:
1. keyboard/* (complete keyboard system) → VoiceAvanue/runtime/libraries/VoiceKeyboard/
2. voiceos-accessibility/ → VoiceAvanue/runtime/libraries/Accessibility/
3. voiceos-browser/ → VoiceAvanue/runtime/libraries/Browser/
4. voiceos-cloud/ → VoiceAvanue/runtime/libraries/CloudStorage/
5. voiceos-filemanager/ → VoiceAvanue/runtime/libraries/FileManager/
6. voiceos-task/ → VoiceAvanue/runtime/libraries/TaskManager/
7. voiceos-notepad/ → VoiceAvanue/runtime/libraries/Notepad/
8. remote-control/ → VoiceAvanue/runtime/libraries/RemoteControl/
9. bottom-command-bar/ → VoiceAvanue/runtime/libraries/CommandBar/
10. augmentalis_theme/ → VoiceAvanue/runtime/libraries/AvaUI/theme/ (merge)
11. color_picker/ → VoiceAvanue/runtime/libraries/ColorPicker/
12. app-preferences/ → VoiceAvanue/runtime/libraries/Preferences/
13. voiceos-common/ → Merge into DeviceManager
14. voiceos-logger/ → Merge into VoiceOsLogging
15. voiceos/ (core) → Merge into SpeechRecognition

### AVA AI Repository
**Location**: `/Volumes/M Drive/Coding/AVA AI/`
**Status**: Active development, hybrid architecture
**Key Features**:
- ONNX NLU engine (DistilBERT/MobileBERT)
- MLC LLM (Gemma 2B/7B)
- Teach-Ava training system
- Faiss vector search (RAG)
- Constitutional AI (7 principles)
- Smart glasses support (8+ devices)

**Integration Needed**:
- Add composite build: `includeBuild("../VoiceAvanue")`
- Create microapp/ package for generating VoiceAvanue apps
- Use AvaUI for AVA's own interface
- Use AvaCode library for code export

### VoiceOS Application
**Location**: Currently embedded in avenue-redux → **EXTRACT TO** `/Volumes/M Drive/Coding/VoiceOS/`
**Status**: Needs restructuring as standalone app
**Dependencies on VoiceAvanue**:
- AvaUI (for UI)
- SpeechRecognition (for voice)
- VoiceKeyboard (for input)
- Accessibility (for a11y service)
- Browser, Notepad, TaskManager, FileManager, CloudStorage, RemoteControl, CommandBar

---

## 📊 SPEC 003 SUMMARY

### User Stories (5 total)

**P1 Stories (MVP):**
1. **US1 - Core Platform Foundation**: Create AvaUI/AvaCode library structure, move Phase 1-2 files
2. **US2 - Avenue-Redux Library Migration**: Migrate 15+ modules to VoiceAvanue platform

**P2 Stories:**
3. **US3 - VoiceOS Application Restructure**: Rebuild VoiceOS using platform libraries
4. **US4 - AVA AI Integration**: Integrate AVA for micro-app generation

**P3 Stories:**
5. **US5 - Documentation and Migration Guides**: Comprehensive developer docs

### Success Criteria (10 measurable outcomes)
- SC-001: All 11 Phase 1-2 files compile in new AvaUI location (0 errors)
- SC-002: Platform build completes in <5 minutes
- SC-003: VoiceOS builds and runs with all avenue-redux features
- SC-004: AVA AI generates 3+ micro-app YAMLs
- SC-005: New developer onboards in <2 hours
- SC-006: Zero code duplication (verified via static analysis)
- SC-007: All Phase 1-2 tests pass (package imports only changed)
- SC-008: 15+ runtime libraries exist
- SC-009: Composite build resolves in <30 seconds
- SC-010: 20% codebase size reduction

### Functional Requirements (30 total)
- FR-001 to FR-005: Platform structure
- FR-006 to FR-010: Avenue-redux migration
- FR-011 to FR-015: VoiceOS restructure
- FR-016 to FR-021: AVA AI integration
- FR-022 to FR-025: Build system
- FR-026 to FR-030: Documentation

---

## 🎬 NEXT STEPS (IMMEDIATE)

### Step 1: Complete Directory Rename ⏳ IN PROGRESS
```bash
cd "/Volumes/M Drive/Coding"
mv AvaCode VoiceAvanue
cd VoiceAvanue
```

### Step 2: Update Configuration Files
Execute spec 003 task list via `/idea.plan` → `/idea.tasks` → `/idea.implement`

### Step 3: IDEACODE Workflow (Option B)
1. **`/idea.plan`** - Create implementation plan for spec 003
   - Include rename as Task 1
   - Include namespace updates
   - Include library migrations
   - Include composite build setup

2. **`/idea.tasks`** - Generate task breakdown
   - Organize by user story priority (P1 first)
   - Identify parallel vs sequential tasks

3. **`/idea.implement`** - Execute implementation
   - Follow IDE Loop (Implement → Defend → Evaluate → Commit)
   - Track progress via TodoWrite

---

## 🔧 TECHNICAL DETAILS

### Composite Build Pattern

**VoiceOS/settings.gradle.kts:**
```kotlin
rootProject.name = "VoiceOS"

includeBuild("../VoiceAvanue") {
    dependencySubstitution {
        substitute(module("com.augmentalis.voiceos:avaui"))
            .using(project(":runtime:libraries:AvaUI"))
        substitute(module("com.augmentalis.voiceos:speechrecognition"))
            .using(project(":runtime:libraries:SpeechRecognition"))
        // ... all other libraries
    }
}

include(":app")
```

**Benefit**: VoiceOS automatically builds and includes VoiceAvanue libraries without publishing to Maven. Changes in VoiceAvanue are immediately available in VoiceOS builds.

### Namespace Migration Pattern

**Before:**
```kotlin
package com.augmentalis.avacode.plugins.ui.core
import com.augmentalis.avacode.plugins.ui.theme.ThemeConfig
```

**After:**
```kotlin
package com.augmentalis.voiceos.avaui.core
import com.augmentalis.voiceos.avaui.theme.ThemeConfig
```

### KMP Source Set Structure

```
runtime/libraries/[LibraryName]/
├── build.gradle.kts                    (KMP configuration)
└── src/
    ├── commonMain/kotlin/com/augmentalis/voiceos/[library]/
    ├── androidMain/kotlin/com/augmentalis/voiceos/[library]/
    ├── iosMain/kotlin/com/augmentalis/voiceos/[library]/
    └── commonTest/kotlin/com/augmentalis/voiceos/[library]/
```

---

## 📚 KEY DOCUMENTS REFERENCED

**Architecture Documents:**
- `AvanueRT_Master_Architecture_Document.md` (27,000+ lines)
- `AvanueRT_Part2_Runtime_Specifications.md`
- `avenue-redux/README.md` (138KB - VoiceOS modules)
- `AVA AI/README.md` (AVA architecture)

**Project Documents:**
- `.ideacode/memory/principles.md` (v1.0.0 - AvaCode constitution)
- `CLAUDE.md` (v3.1.0 - Quick reference)
- `specs/002-avaui-uik-enhancements/` (Phase 1-2 work)

**Existing Code:**
- 11 Phase 1-2 files (ComponentModel, ComponentPosition, PluginComponent, Logger, Result, ThemeConfig, IMUOrientationData, MotionProcessor, LayoutFormat, LayoutLoader, SecurityIndicator)
- ComponentModelTest.kt (20+ test cases)
- Core-Abstractions.md (744 lines documentation)

---

## ⚠️ CRITICAL REMINDERS

### Don't Forget
1. **Namespace is VoiceOS**, not VoiceAvanue (project name ≠ namespace)
2. **AVA AI is full AI system**, not just NLP module
3. **Composite build** links apps to platform without publishing
4. **AvaUI ≠ AvaCode** (runtime interpreter vs code generator)
5. **Avenue-redux has 15+ modules** to migrate (not just keyboard)

### Git Safety
- Only stage/commit files YOU created/modified
- Use explicit paths: `git add path/to/file.kt`
- Never use `git add .` or `git add -A`
- Verify with `git status` and `git diff --cached` before commit

### Zero-Tolerance Policies
- NO AI/Claude/Anthropic references in commits
- ALWAYS use local machine time (not server time)
- ALL commits: "Created by Manoj Jhawar, manoj@ideahq.net"
- NO !! (null assertion) operator in production code
- Documentation BEFORE commits

---

## 📊 SESSION STATISTICS

**Time Spent**: ~90 minutes
**Decisions Made**: 8 major architectural decisions
**Files Created**: 2 (spec.md, requirements.md checklist)
**Lines Written**: ~450 lines (spec + checklist)
**Validation Status**: ✅ ALL QUALITY GATES PASSED

**Token Usage**: ~135K / 200K (67% used, 33% remaining)

---

## 🚀 RESUMPTION CHECKLIST

**When resuming this work:**

- [ ] Verify directory renamed: `ls "/Volumes/M Drive/Coding/VoiceAvanue"`
- [ ] Check git branch: `git branch` (should be on 003-platform-architecture-restructure)
- [ ] Review this document completely
- [ ] Read spec 003: `specs/003-platform-architecture-restructure/spec.md`
- [ ] Execute: `/idea.plan` to create implementation plan
- [ ] Execute: `/idea.tasks` to generate task breakdown
- [ ] Execute: `/idea.implement` to start work

**First Task After Resumption:**
```bash
# If rename not done yet:
cd "/Volumes/M Drive/Coding"
mv AvaCode VoiceAvanue
cd VoiceAvanue
git status

# Then proceed with IDEACODE workflow
```

---

**Created by Manoj Jhawar, manoj@ideahq.net**
**Session End**: 2025-10-27 04:45 PST
**Context Protocol Version**: 1.0.0
