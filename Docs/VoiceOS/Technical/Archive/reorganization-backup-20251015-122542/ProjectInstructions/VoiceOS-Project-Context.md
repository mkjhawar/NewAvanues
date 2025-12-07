<!--
filename: VoiceOS-Project-Context.md
created: 2025-10-15 02:18:28 PDT
author: VoiceOS Development Team
purpose: VoiceOS project-specific context and information for AI agents
last-modified: 2025-10-15 02:18:28 PDT
version: v1.0.0
changelog:
- 2025-10-15 02:18:28 PDT: Initial extraction from vos4/CLAUDE.md
-->

# VoiceOS Project Context

## Purpose
This file provides VoiceOS-specific context for AI agents working on the VoiceOS (vos4) project. It contains project structure, module organization, naming conventions, and VoiceOS-specific implementation details.

## Project Overview

### Basic Information
- **Project Name:** VoiceOS
- **Nickname:** VOS
- **Current Version:** Version 4 (vos4)
- **Repository Location:** `/Volumes/M Drive/Coding/Warp/vos4`
- **Default Branch:** VOS4 (STAY ON THIS BRANCH)
- **Main Branch:** main (for PRs)
- **Git Required:** Yes - Must have working git repository

### Core Principles
- **Key Principle:** Direct implementation, zero interfaces
- **Namespace Standard:** `com.augmentalis.*` (NEW STANDARD - NO MORE com.ai)
- **Database Standard:** Room (current standard due to KSP support)
- **Platform:** Android (Kotlin/Java)

### Project Description
VoiceOS is an Android-based voice control system that provides accessibility features, voice recognition, cursor control, and UI interaction capabilities. The project is organized into 15 modules across 3 categories: apps, libraries, and managers.

## Project Structure

### Root-Level Organization
```
/Volumes/M Drive/Coding/Warp/vos4/
├── coding/                        # 🎯 ALL ACTIVE DEVELOPMENT WORK - CHECK FIRST
│   ├── TODO/                     # Active tasks (🔴 MUST have timestamp in filename)
│   │   ├── VOS4-TODO-Master-251009-0230.md   # Overall project tasks
│   │   ├── [ModuleName]-TODO-YYMMDD-HHMM.md  # Module-specific tasks
│   │   └── [BranchName]-TODO-YYMMDD-HHMM.md  # Branch-specific tasks
│   ├── STATUS/                   # Current state - CHECK DAILY (🔴 MUST have timestamp)
│   │   ├── VOS4-Status-Current-251009-0230.md # Overall project status
│   │   ├── [ModuleName]-Status-YYMMDD-HHMM.md # Module-specific status
│   │   └── [BranchName]-Status-YYMMDD-HHMM.md # Branch-specific status
│   ├── ISSUES/                   # Active problems to fix
│   │   ├── CRITICAL/             # Fix immediately
│   │   ├── HIGH/                 # Fix soon
│   │   ├── MEDIUM/               # Fix when possible
│   │   └── LOW/                  # Fix when time permits
│   ├── DECISIONS/                # Architecture Decision Records
│   ├── planning/                 # Sprint planning, roadmaps
│   ├── reviews/                  # Code review tracking
│   └── metrics/                  # Development metrics
├── docs/                         # 📚 ALL DOCUMENTATION (see structure below)
│   ├── scripts/                  # 🔧 All automation scripts (agent-tools, build, audit, dev)
│   │   ├── agent-tools/         # AI agent automation scripts
│   │   ├── audit/               # Audit and compliance scripts
│   │   ├── build/               # Build and test automation
│   │   └── development/         # Development utilities
├── modules/                      # 💻 CODE FILES ONLY - NO DOCUMENTATION
│   ├── apps/                    # Application code (4 modules)
│   │   ├── VoiceAccessibility/  # Kotlin/Java source files
│   │   ├── VoiceCursor/         # Kotlin/Java source files
│   │   ├── VoiceRecognition/    # Kotlin/Java source files
│   │   └── VoiceUI/             # Kotlin/Java source files
│   ├── libraries/               # Library code (6 modules)
│   │   ├── DeviceManager/       # Kotlin/Java source files
│   │   ├── SpeechRecognition/   # Kotlin/Java source files
│   │   ├── Translation/         # Kotlin/Java source files
│   │   ├── UUIDManager/         # Kotlin/Java source files
│   │   ├── VoiceKeyboard/       # Kotlin/Java source files
│   │   └── VoiceUIElements/     # Kotlin/Java source files
│   └── managers/                # Manager code (5 modules)
│       ├── CommandManager/      # Kotlin/Java source files
│       ├── HUDManager/          # Kotlin/Java source files
│       ├── LicenseManager/      # Kotlin/Java source files
│       ├── LocalizationManager/ # Kotlin/Java source files
│       └── VoiceDataManager/    # Kotlin/Java source files
└── tests/                        # Test code only
```

### Documentation Structure
```
docs/
├── modules/                      # Module-specific documentation
│   ├── voice-accessibility/      # Docs for VoiceAccessibility app
│   ├── voice-cursor/             # Docs for VoiceCursor app
│   ├── voice-recognition/        # Docs for VoiceRecognition app
│   ├── voice-ui/                 # Docs for VoiceUI app
│   ├── device-manager/           # Docs for DeviceManager library
│   ├── speech-recognition/       # Docs for SpeechRecognition library
│   ├── translation/              # Docs for Translation library
│   ├── uuid-manager/             # Docs for UUIDManager library
│   ├── voice-keyboard/           # Docs for VoiceKeyboard library
│   ├── voice-ui-elements/        # Docs for VoiceUIElements library
│   ├── command-manager/          # Docs for CommandManager
│   ├── hud-manager/              # Docs for HUDManager
│   ├── license-manager/          # Docs for LicenseManager
│   ├── localization-manager/     # Docs for LocalizationManager
│   └── voice-data-manager/       # Docs for VoiceDataManager
│
├── voiceos-master/               # System-level documentation
│   ├── architecture/             # Overall system architecture
│   ├── standards/                # Project-wide standards
│   ├── project-management/       # Overall project PM
│   ├── reference/                # System-wide references
│   ├── reports/                  # Analysis & migration reports
│   ├── metrics/                  # Performance metrics
│   └── guides/                   # System-wide guides
│
├── archive/                      # Old/deprecated docs
├── templates/                    # Documentation templates
└── documentation-control/        # Doc management tools
```

### Standard Module Documentation Structure
Each module folder in `/docs/modules/[module-name]/` contains:
```
[module-name]/
├── architecture/           # Module design & architecture
├── changelog/             # Module version history
├── developer-manual/      # How to develop this module
├── diagrams/              # Visual documentation
├── implementation/        # Implementation details
├── module-standards/      # Module-specific standards
├── project-management/    # Module PM docs
├── reference/
│   └── api/              # Module API documentation
├── roadmap/              # Module future plans
├── status/               # Module status history
├── testing/              # Test documentation
└── user-manual/          # User documentation
```

## Module Organization

### Application Modules (4)
Located in `/modules/apps/`

1. **VoiceAccessibility**
   - Documentation: `/docs/modules/voice-accessibility/`
   - Package: `com.augmentalis.voiceaccessibility`
   - Purpose: Accessibility scraping and UI interaction

2. **VoiceCursor**
   - Documentation: `/docs/modules/VoiceCursor/`
   - Package: `com.augmentalis.voicecursor`
   - Purpose: Voice-controlled cursor functionality

3. **VoiceRecognition**
   - Documentation: `/docs/modules/VoiceRecognition/`
   - Package: `com.augmentalis.voicerecognition`
   - Purpose: Voice recognition and processing

4. **VoiceUI**
   - Documentation: `/docs/modules/VoiceUI/`
   - Package: `com.augmentalis.voiceui`
   - Purpose: Voice user interface components

### Library Modules (6)
Located in `/modules/libraries/`

1. **DeviceManager**
   - Documentation: `/docs/modules/DeviceManager/`
   - Package: `com.augmentalis.devicemanager`
   - Purpose: Device-specific functionality management

2. **SpeechRecognition**
   - Documentation: `/docs/modules/SpeechRecognition/`
   - Package: `com.augmentalis.speechrecognition`
   - Purpose: Speech recognition engine integration

3. **Translation**
   - Documentation: `/docs/modules/Translation/`
   - Package: `com.augmentalis.translation`
   - Purpose: Language translation services

4. **UUIDManager**
   - Documentation: `/docs/modules/UUIDCreator/`
   - Package: `com.augmentalis.uuidmanager`
   - Purpose: UUID generation and management

5. **VoiceKeyboard**
   - Documentation: `/docs/modules/VoiceKeyboard/`
   - Package: `com.augmentalis.voicekeyboard`
   - Purpose: Voice-controlled keyboard functionality

6. **VoiceUIElements**
   - Documentation: `/docs/modules/VoiceUI-elements/`
   - Package: `com.augmentalis.voiceuielements`
   - Purpose: Reusable voice UI components

### Manager Modules (5)
Located in `/modules/managers/`

1. **CommandManager**
   - Documentation: `/docs/modules/CommandManager/`
   - Package: `com.augmentalis.commandmanager`
   - Purpose: Voice command processing and routing

2. **HUDManager**
   - Documentation: `/docs/modules/HUDManager/`
   - Package: `com.augmentalis.hudmanager`
   - Purpose: Heads-up display management

3. **LicenseManager**
   - Documentation: `/docs/modules/LicenseManager/`
   - Package: `com.augmentalis.licensemanager`
   - Purpose: License validation and management

4. **LocalizationManager**
   - Documentation: `/docs/modules/LocalizationManager/`
   - Package: `com.augmentalis.localizationmanager`
   - Purpose: Multi-language support

5. **VoiceDataManager**
   - Documentation: `/docs/modules/VoiceDataManager/`
   - Package: `com.augmentalis.voicedatamanager`
   - Purpose: Voice data storage and retrieval

## Naming Conventions

### VoiceOS-Specific Naming Standards

**MANDATORY STANDARDS** - See `/docs/voiceos-master/standards/NAMING-CONVENTIONS.md` for complete reference

#### Quick Reference Table

| Artifact Type | Convention | Example |
|--------------|------------|---------|
| **Kotlin/Java files** | `PascalCase.kt` | `AccessibilityScrapingIntegration.kt` |
| **Documentation files** | `PascalCase-With-Hyphens-YYMMDD-HHMM.md` | `Architecture-Refactor-Roadmap-251010-0157.md` |
| **Code modules** | `PascalCase/` | `VoiceAccessibility/` |
| **Doc folders** | `kebab-case/` | `voice-accessibility/` |
| **Packages** | `lowercase.dot.separated` | `com.augmentalis.voiceaccessibility` |
| **Classes** | `PascalCase` | `VoiceCommandProcessor` |
| **Methods** | `camelCase` | `processVoiceCommand()` |
| **Variables** | `camelCase` | `elementHash` |
| **Constants** | `SCREAMING_SNAKE_CASE` | `MAX_DEPTH` |
| **XML layouts** | `lowercase_snake_case.xml` | `activity_main.xml` |
| **DB tables** | `lowercase_snake_case` | `scraped_elements` |
| **Scripts** | `kebab-case.sh` or `snake_case.sh` | `analyze-imports.sh` |

#### Code-to-Documentation Mapping

**CRITICAL:** Code modules use **PascalCase**, documentation folders use **kebab-case**

```
modules/apps/VoiceAccessibility/  → docs/modules/voice-accessibility/
modules/apps/VoiceCursor/         → docs/modules/VoiceCursor/
modules/apps/VoiceRecognition/    → docs/modules/VoiceRecognition/
modules/apps/VoiceUI/             → docs/modules/VoiceUI/
modules/libraries/DeviceManager/  → docs/modules/DeviceManager/
modules/libraries/SpeechRecognition/ → docs/modules/SpeechRecognition/
modules/libraries/Translation/    → docs/modules/Translation/
modules/libraries/UUIDManager/    → docs/modules/UUIDCreator/
modules/libraries/VoiceKeyboard/  → docs/modules/VoiceKeyboard/
modules/libraries/VoiceUIElements/ → docs/modules/VoiceUI-elements/
modules/managers/CommandManager/  → docs/modules/CommandManager/
modules/managers/HUDManager/      → docs/modules/HUDManager/
modules/managers/LicenseManager/  → docs/modules/LicenseManager/
modules/managers/LocalizationManager/ → docs/modules/LocalizationManager/
modules/managers/VoiceDataManager/ → docs/modules/VoiceDataManager/
```

#### Common Violations to Avoid

```
❌ WRONG:
- accessibilityScrapingIntegration.kt   (camelCase - use PascalCase)
- uuid-hash-persistence.md              (missing timestamp)
- docs/modules/VoiceAccessibility/      (PascalCase - use kebab-case)
- com.augmentalis.VoiceAccessibility    (PascalCase in package - use lowercase)
- com.ai.voiceaccessibility             (old namespace - use com.augmentalis)

✅ CORRECT:
- AccessibilityScrapingIntegration.kt
- UUID-Hash-Persistence-251010-0912.md
- docs/modules/voice-accessibility/
- com.augmentalis.voiceaccessibility
```

### TODO & STATUS File Naming

#### TODO Files (`/coding/TODO/`)
**Naming Pattern:** `[Prefix]-TODO-[Suffix].md`

**Examples:**
- `VOS4-TODO-Master.md` - Overall project tasks
- `VoiceAccessibility-TODO.md` - Module-specific tasks (use exact module name)
- `SpeechRecognition-TODO.md` - Module tasks
- `CommandManager-TODO.md` - Manager module tasks
- `VOS4-LegacyIntegration-TODO.md` - Branch-specific tasks
- `VOSK-COMPLETION-CHECKLIST-250903.md` - Time-stamped completion checklists

#### STATUS Files (`/coding/STATUS/`)
**Naming Pattern:** `[Prefix]-Status-[Suffix].md`

**Examples:**
- `VOS4-Status-Current.md` - Overall current status
- `SpeechRecognition-Status.md` - Module-specific status
- `HUDManager-Status.md` - Manager module status
- `VOS4-LegacyIntegration-Status.md` - Branch-specific status
- `VOS4-Status-2025-09-07.md` - Date-specific status reports
- `BUILD-STATUS-2025-09-05.md` - Build-specific status

## VoiceOS Scripts and Automation

**Location:** `/docs/scripts/` - All automation scripts consolidated here

### Script Organization

```
docs/scripts/
├── agent-tools/      # AI agent automation (analyze, fix, organize)
├── audit/           # Compliance audits (documentation structure, etc.)
├── build/           # Build automation (tests, hooks, coverage)
└── development/     # Dev utilities (code indexer, converters)
```

### Key VoiceOS Scripts

**Agent Automation:**
- `agent-tools/analyze_imports.sh` - Analyze Kotlin imports
- `agent-tools/fix_warnings.sh` - Auto-fix compiler warnings
- `agent-tools/organize_imports.sh` - Organize imports

**Audits:**
- `audit/audit_docs_structure.sh` - Verify module documentation compliance

**Build:**
- `build/generate-test.sh` - Generate test boilerplate
- `build/setup-hooks.sh` - Setup Git hooks

**Usage:** See `/docs/scripts/README.md` for detailed documentation

## VoiceOS Documentation Workflow

### Documentation Location Rules

#### NEVER Place Documentation in Root Folder
- **❌ FORBIDDEN:** `/Volumes/M Drive/Coding/Warp/vos4/*.md` (except README.md, claude.md, BEF-SHORTCUTS.md)
- **✅ REQUIRED:** All documentation MUST go in `/docs/` structure
- **VIOLATION = CRITICAL ERROR:** Any analysis, report, or documentation file in root must be moved immediately

#### Correct Documentation Locations
- **Analysis Reports:** `/docs/voiceos-master/status/`
- **Build Reports:** `/docs/voiceos-master/project-management/build-reports/`
- **Architecture Docs:** `/docs/voiceos-master/architecture/`
- **Module Docs:** `/docs/[module-name]/`
- **Changelogs:** `/docs/[module-name]/changelog/`
- **API Docs:** `/docs/[module-name]/reference/api/`

### When to Update VoiceOS Documentation

**BEFORE Code Changes:**
- Check `/coding/TODO/[Module]-TODO.md` for task context
- Check `/docs/[module]/changelog/` for history
- Check `/coding/STATUS/[Module]-Status.md` for current state

**AFTER Code Changes:**
- Update `/docs/[module]/changelog/` with changes
- Update `/coding/STATUS/[Module]-Status.md` with progress
- Update `/coding/TODO/[Module]-TODO.md` with completion
- Update architecture/diagrams if structure changed

**Key Update Triggers:**

1. **TODO Updates** - Mark tasks in_progress/completed
2. **STATUS Updates** - Daily progress and milestone completion
3. **CHANGELOG Updates** - After any code changes (with timestamp)
4. **ARCHITECTURE Updates** - When adding/changing/removing components (with timestamp)
5. **DIAGRAMS Updates** - When architecture/flow/UI changes (with timestamp)
6. **API Updates** - When adding/changing/removing public methods (with timestamp)
7. **DECISIONS Updates** - Create ADR for architectural decisions (with timestamp)
8. **DEVELOPER MANUAL Updates** - When adding features or changing patterns (with timestamp)

## VoiceOS Navigation Reference

### Essential Directory Navigation

```bash
# Project root
cd /Volumes/M\ Drive/Coding/Warp/vos4

# Active work tracking
cd /Volumes/M\ Drive/Coding/Warp/vos4/coding/TODO/
cd /Volumes/M\ Drive/Coding/Warp/vos4/coding/STATUS/
cd /Volumes/M\ Drive/Coding/Warp/vos4/coding/ISSUES/

# System documentation
cd /Volumes/M\ Drive/Coding/Warp/vos4/docs/voiceos-master/

# Module code (example: VoiceAccessibility)
cd /Volumes/M\ Drive/Coding/Warp/vos4/modules/apps/VoiceAccessibility/

# Module documentation (example: voice-accessibility)
cd /Volumes/M\ Drive/Coding/Warp/vos4/docs/modules/voice-accessibility/
```

### Key Separation Rules

- **CODE:** `/modules/` = .kt/.java/.xml files ONLY
- **DOCS:** `/docs/modules/` = .md/.png/.svg documentation ONLY
- **ACTIVE:** `/coding/` = Current work, TODOs, issues
- **NEVER** mix code and documentation in same folder

## VoiceOS Project Status

### Documentation Structure Status
- **Structure:** New compartmentalized structure (2025-02-07)
- **Module Organization:** All modules at same level in `/docs/`
- **Quick Access:** TODO and STATUS at root level
- **Containment:** Complete module self-containment

### Compliance Status
- **Naming Compliance:** 95% achieved
- **ALL_CAPS Files:** All fixed
- **Module Structure:** Standardized
- **Documentation:** Reorganized

### Current Branch
- **Active Branch:** vos4-legacyintegration
- **Default Branch:** VOS4
- **Main Branch:** main (for PRs)

## VoiceOS-Specific Workflow Commands

### Quick Command Shortcuts
- **UD** = Update Documents
- **SCP** = Stage, Commit & Push (see Protocol-VOS4-Commit.md)
- **SUF** = Stage, Update & Full workflow
- **CRT** = Combined Review Technique (COT+ROT+TOT)
- **PRECOMPACTION** = Create pre-compaction report at 90% context

### Navigation Shortcuts
```bash
# Navigation (from vos4 root)
cd coding/TODO/              # Active tasks
cd coding/STATUS/            # Current status
cd docs/voiceos-master/      # System docs
```

## Reference Documentation

For complete details, refer to:
- **Full Naming Conventions:** `/docs/voiceos-master/standards/NAMING-CONVENTIONS.md`
- **Coding Protocol:** `/Volumes/M Drive/Coding/Warp/vos4/Docs/ProjectInstructions/Protocol-VOS4-Coding-Standards.md`
- **Documentation Protocol:** `/Volumes/M Drive/Coding/Warp/vos4/Docs/ProjectInstructions/Protocol-VOS4-Documentation.md`
- **Commit Protocol:** `/Volumes/M Drive/Coding/Warp/vos4/Docs/ProjectInstructions/Protocol-VOS4-Commit.md`
- **Q&A Protocol:** `/Volumes/M Drive/Coding/Warp/vos4/Docs/ProjectInstructions/Protocol-VOS4-Pre-Implementation-QA.md`
- **Master Instructions:** `/Volumes/M Drive/Coding/Docs/AgentInstructions/Guide-Master-AI-Instructions.md`
- **Master Standards:** `/Volumes/M Drive/Coding/Docs/AgentInstructions/Standards-Development-Core.md`

---
**Document Information:**
- **Last Updated:** 2025-10-15 02:18:28 PDT
- **Version:** v1.0.0
- **Source:** Extracted from vos4/CLAUDE.md
- **Purpose:** Provide VoiceOS-specific project context for AI agents
- **Audience:** AI agents working on VoiceOS development
