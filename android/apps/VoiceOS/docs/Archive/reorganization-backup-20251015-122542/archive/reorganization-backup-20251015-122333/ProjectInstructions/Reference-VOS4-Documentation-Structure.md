<!--
Filename: Reference-VOS4-Documentation-Structure.md
Created: 2025-10-15 02:49:32 PDT
Author: AI Documentation Agent
Purpose: VoiceOS 4 specific documentation structure (extracted from DOCUMENT-ORGANIZATION-STRUCTURE.md)
Last Modified: 2025-10-15 02:49:32 PDT
Version: v1.0.0
Changelog:
- v1.0.0 (2025-10-15): Initial extraction from DOCUMENT-ORGANIZATION-STRUCTURE.md - VOS4 structure
-->

# VOS4 Documentation Structure - Project-Specific Organization

## Purpose

This document defines the VOS4-SPECIFIC documentation structure. It extends universal documentation principles with VOS4-specific requirements and organization.

## 📂 VOS4 Root-Level Structure

```
/Volumes/M Drive/Coding/Warp/vos4/
├── coding/                        # 🎯 ALL ACTIVE DEVELOPMENT WORK
│   ├── TODO/                     # Active tasks (MUST have timestamp)
│   ├── STATUS/                   # Current state (MUST have timestamp)
│   ├── ISSUES/                   # Active problems by priority
│   ├── DECISIONS/                # Architecture Decision Records
│   ├── planning/                 # Sprint planning, roadmaps
│   ├── reviews/                  # Code review tracking
│   └── metrics/                  # Development metrics
│
├── docs/                         # 📚 ALL DOCUMENTATION
│   ├── modules/                  # Module-specific docs (kebab-case)
│   ├── voiceos-master/           # System-level documentation
│   ├── archive/                  # Old/deprecated docs
│   ├── templates/                # Documentation templates
│   ├── documentation-control/    # Doc management tools
│   └── scripts/                  # Automation scripts
│
├── modules/                      # 💻 CODE FILES ONLY
│   ├── apps/                    # Application code (PascalCase)
│   ├── libraries/               # Library code (PascalCase)
│   └── managers/                # Manager code (PascalCase)
│
└── tests/                        # Test code only
```

## 📁 VOS4 Active Development Structure (`/coding/`)

### TODO Files - Task Tracking

**Location**: `/coding/TODO/`

**Naming Pattern**: `[Prefix]-TODO-[Suffix]-YYMMDD-HHMM.md`

**Examples**:
```
VOS4-TODO-Master-251009-0230.md              # Overall project tasks
VoiceAccessibility-TODO-251009-1145.md       # Module-specific tasks
VOS4-LegacyIntegration-TODO-251009-0830.md   # Branch-specific tasks
```

**Contents**:
- Pending tasks
- In-progress tasks
- Blocked tasks
- Completed tasks (archived periodically)

### STATUS Files - Progress Tracking

**Location**: `/coding/STATUS/`

**Naming Pattern**: `[Prefix]-Status-[Suffix]-YYMMDD-HHMM.md`

**Examples**:
```
VOS4-Status-Current-251009-0230.md           # Overall current status
SpeechRecognition-Status-251009-1430.md      # Module-specific status
VOS4-LegacyIntegration-Status-251009-0912.md # Branch-specific status
```

**Contents**:
- Current progress
- Recent accomplishments
- Active issues
- Next priorities
- Blockers/dependencies

### ISSUES Files - Problem Tracking

**Location**: `/coding/ISSUES/[PRIORITY]/`

**Priority Levels**:
```
/coding/ISSUES/
├── CRITICAL/     # Fix immediately - blocking work
├── HIGH/         # Fix soon - important but not blocking
├── MEDIUM/       # Fix when possible - technical debt
└── LOW/          # Fix when time permits - minor issues
```

**Naming**: `[Module]-[Issue]-YYMMDD-HHMM.md`

### DECISIONS Files - ADR Tracking

**Location**: `/coding/DECISIONS/`

**Naming**: `ADR-XXX-[Title]-YYMMDD-HHMM.md`

**Example**:
```
ADR-001-Direct-Implementation-251010-1131.md
ADR-002-ObjectBox-Only-251010-1142.md
ADR-003-Hash-Persistence-Strategy-251010-1156.md
```

**Contents** (standard ADR format):
- Context
- Decision
- Consequences
- Alternatives considered
- Status

## 📁 VOS4 Documentation Structure (`/docs/`)

### VoiceOS Master Documentation

**Location**: `/docs/voiceos-master/`

```
voiceos-master/
├── architecture/              # Overall system architecture
│   ├── System-Architecture-YYMMDD-HHMM.md
│   ├── Module-Dependencies-YYMMDD-HHMM.md
│   ├── Data-Flow-Design-YYMMDD-HHMM.md
│   └── Integration-Architecture-YYMMDD-HHMM.md
│
├── standards/                 # Project-wide standards
│   ├── NAMING-CONVENTIONS.md
│   ├── CODING-STANDARDS.md
│   ├── DOCUMENTATION-STANDARDS.md
│   └── TESTING-STANDARDS.md
│
├── project-management/        # Overall project PM
│   ├── roadmap/
│   ├── milestones/
│   ├── sprints/
│   └── build-reports/
│
├── reference/                 # System-wide references
│   ├── api/
│   ├── glossary/
│   └── troubleshooting/
│
├── reports/                   # Analysis & migration reports
│   ├── Analysis-[Topic]-YYMMDD-HHMM.md
│   └── Migration-[Topic]-YYMMDD-HHMM.md
│
├── metrics/                   # Performance metrics
│   └── Performance-Metrics-YYMMDD-HHMM.md
│
├── guides/                    # System-wide guides
│   ├── Getting-Started.md
│   ├── Development-Setup.md
│   └── Deployment-Guide.md
│
└── diagrams/                  # System-level diagrams
    ├── System-Overview-YYMMDD-HHMM.mmd
    └── Module-Interactions-YYMMDD-HHMM.mmd
```

### Module-Specific Documentation

**Location**: `/docs/modules/[module-name]/` (kebab-case)

**VOS4 Module Mapping** (Code → Docs):
```
Code (PascalCase)                    Documentation (kebab-case)
────────────────────────────────────────────────────────────────
/modules/apps/VoiceAccessibility/  → /docs/modules/voice-accessibility/
/modules/apps/VoiceCursor/         → /docs/modules/VoiceCursor/
/modules/apps/VoiceRecognition/    → /docs/modules/VoiceRecognition/
/modules/apps/VoiceUI/             → /docs/modules/VoiceUI/
/modules/libraries/DeviceManager/  → /docs/modules/DeviceManager/
/modules/libraries/SpeechRecognition/ → /docs/modules/SpeechRecognition/
/modules/libraries/Translation/    → /docs/modules/Translation/
/modules/libraries/UUIDManager/    → /docs/modules/UUIDCreator/
/modules/libraries/VoiceKeyboard/  → /docs/modules/VoiceKeyboard/
/modules/libraries/VoiceUIElements/ → /docs/modules/VoiceUI-elements/
/modules/managers/CommandManager/  → /docs/modules/CommandManager/
/modules/managers/HUDManager/      → /docs/modules/HUDManager/
/modules/managers/LicenseManager/  → /docs/modules/LicenseManager/
/modules/managers/LocalizationManager/ → /docs/modules/LocalizationManager/
/modules/managers/VoiceDataManager/ → /docs/modules/VoiceDataManager/
```

**Standard Module Documentation Structure**:
```
[module-name]/                          # kebab-case folder
├── architecture/                       # Module design
│   ├── [Module]-Architecture-YYMMDD-HHMM.md
│   ├── Component-Design-YYMMDD-HHMM.md
│   └── Integration-Points-YYMMDD-HHMM.md
│
├── changelog/                          # Version history
│   ├── [Module]-Changelog.md          # Current changelog
│   └── [Module]-Changelog-YYMMDD-HHMM.md  # Archived versions
│
├── developer-manual/                   # How to develop
│   ├── Getting-Started.md
│   ├── API-Usage.md
│   ├── Common-Patterns.md
│   └── Troubleshooting.md
│
├── diagrams/                           # Visual documentation
│   ├── [Module]-Architecture-YYMMDD-HHMM.mmd
│   ├── [Module]-Sequence-YYMMDD-HHMM.mmd
│   └── [Module]-Flow-YYMMDD-HHMM.mmd
│
├── implementation/                     # Implementation details
│   ├── Implementation-Status-YYMMDD-HHMM.md
│   ├── Component-Details-YYMMDD-HHMM.md
│   └── Integration-Guide-YYMMDD-HHMM.md
│
├── module-standards/                   # Module-specific standards
│   ├── Coding-Standards.md
│   └── Testing-Standards.md
│
├── project-management/                 # Module PM docs
│   ├── Roadmap.md
│   ├── Milestones.md
│   └── Sprint-Planning.md
│
├── reference/
│   └── api/                           # Module API documentation
│       ├── [Class]-API-YYMMDD-HHMM.md
│       └── API-Reference-YYMMDD-HHMM.md
│
├── roadmap/                            # Module future plans
│   └── [Module]-Roadmap-YYMMDD-HHMM.md
│
├── status/                             # Module status history
│   └── [Module]-Status-YYMMDD-HHMM.md
│
├── testing/                            # Test documentation
│   ├── Test-Plan.md
│   ├── Test-Results-YYMMDD-HHMM.md
│   └── Coverage-Report-YYMMDD-HHMM.md
│
└── user-manual/                        # User documentation
    ├── User-Guide.md
    └── Feature-Documentation.md
```

## 📁 VOS4 Scripts Structure (`/docs/scripts/`)

**Location**: `/docs/scripts/`

```
scripts/
├── agent-tools/              # AI agent automation
│   ├── analyze_imports.sh
│   ├── fix_warnings.sh
│   └── organize_imports.sh
│
├── audit/                    # Compliance audits
│   ├── audit_docs_structure.sh
│   ├── check_naming.sh
│   └── verify_tests.sh
│
├── build/                    # Build automation
│   ├── generate-test.sh
│   ├── setup-hooks.sh
│   └── coverage-report.sh
│
└── development/              # Dev utilities
    ├── code-indexer.sh
    └── dependency-analyzer.sh
```

## 🔍 VOS4 Quick Location Reference

### Find Documents Quickly

| What You Need | Where to Look | Examples |
|--------------|---------------|----------|
| **Active tasks** | `/coding/TODO/` | `VOS4-TODO-Master-YYMMDD-HHMM.md` |
| **Current status** | `/coding/STATUS/` | `VOS4-Status-Current-YYMMDD-HHMM.md` |
| **Active issues** | `/coding/ISSUES/[PRIORITY]/` | `CRITICAL/SpeechEngine-Crash-YYMMDD-HHMM.md` |
| **Architecture decisions** | `/coding/DECISIONS/` | `ADR-001-Topic-YYMMDD-HHMM.md` |
| **System architecture** | `/docs/voiceos-master/architecture/` | `System-Architecture-YYMMDD-HHMM.md` |
| **Project standards** | `/docs/voiceos-master/standards/` | `NAMING-CONVENTIONS.md` |
| **Module docs** | `/docs/modules/[module]/` | `/docs/modules/SpeechRecognition/` |
| **Module changelog** | `/docs/modules/[module]/changelog/` | `SpeechRecognition-Changelog.md` |
| **API docs** | `/docs/modules/[module]/reference/api/` | `VoskEngine-API-YYMMDD-HHMM.md` |
| **Diagrams** | `/docs/modules/[module]/diagrams/` | `Architecture-YYMMDD-HHMM.mmd` |
| **Scripts** | `/docs/scripts/[category]/` | `/docs/scripts/agent-tools/` |

## 📝 VOS4-Specific Naming Rules

### Code Files (in `/modules/`)
- **Kotlin/Java**: `PascalCase.kt` or `PascalCase.java`
- **XML layouts**: `lowercase_snake_case.xml`
- **Resources**: Standard Android naming

### Documentation Files (in `/docs/`)
- **General docs**: `PascalCase-With-Hyphens.md`
- **Timestamped docs**: `PascalCase-With-Hyphens-YYMMDD-HHMM.md`
- **Standards**: `ALL-CAPS-WITH-HYPHENS.md`

### Folder Naming
- **Code folders**: `PascalCase/` (e.g., `/modules/apps/VoiceAccessibility/`)
- **Doc folders**: `kebab-case/` (e.g., `/docs/modules/voice-accessibility/`)
- **Special folders**: `PascalCase/` or `UPPERCASE/` (e.g., `/coding/TODO/`)

## 🚨 VOS4-Specific Documentation Requirements

### MANDATORY Before ANY Commit:

1. **Update changelogs** - ALWAYS
   - `/docs/modules/[module]/changelog/[Module]-Changelog.md`
   - Format: `YYYY-MM-DD: [Component] - Change description (Reason)`

2. **Update architecture docs** - If structure changed
   - Create NEW timestamped file
   - Archive old version
   - Update references

3. **Update API docs** - If interfaces changed
   - Document new methods
   - Mark deprecated methods
   - Provide migration guides

4. **Update status/TODO** - ALWAYS
   - Mark completed tasks
   - Add new issues
   - Update progress

5. **Include diagrams** - If architecture/flow changed
   - Mermaid diagrams
   - ASCII art alternatives
   - Screenshot/mockups for UI

### Timestamp Update Rule:

**When updating timestamped files**:
1. Get local time: `date "+%y%m%d-%H%M"`
2. Create NEW file with new timestamp
3. DON'T edit original timestamped file
4. Update all references
5. Archive old file

**Example**:
```bash
# Original file
docs/modules/voice-accessibility/architecture/Integration-251010-1126.md

# Create new version
date "+%y%m%d-%H%M"  # Output: 251015-0249
cp Integration-251010-1126.md Integration-251015-0249.md

# Edit new file (not old one)
# Update references to point to new file
# Archive old file
mv Integration-251010-1126.md ../archives/
```

## 📋 VOS4 Documentation Workflow

### Daily Workflow:
1. Check `/coding/STATUS/` for current state
2. Check `/coding/TODO/` for active tasks
3. Update status as work progresses
4. Mark completed tasks
5. Add new issues to `/coding/ISSUES/`

### Before Coding:
1. Check module changelog for history
2. Check module architecture for design
3. Check developer manual for patterns
4. Check API docs for interfaces

### After Coding:
1. Update module changelog (MANDATORY)
2. Update architecture docs (if changed)
3. Update API docs (if changed)
4. Update diagrams (if changed)
5. Update status and TODO
6. Stage docs WITH code

### Before Commit:
1. Run checklist from Reference-Zero-Tolerance-Policies.md
2. Verify all docs updated
3. Check no tool references in commit message
4. Stage by category: docs → code → tests

---

**Note**: This is VOS4-SPECIFIC structure. Universal documentation principles are in Guide-Documentation-Structure.md.
