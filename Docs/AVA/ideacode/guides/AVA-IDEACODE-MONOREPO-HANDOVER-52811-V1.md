# IDEACODE Monorepo Migration - Handover Document

**Date:** 2025-11-28
**From:** Claude (Opus 4.5) - Current Session
**To:** Claude (Future Session) - ideacode-mcp Implementation
**Project:** MainAvanues Monorepo Consolidation
**Status:** Planning Complete → Implementation Ready

---

## 🎯 Executive Summary

This document hands over the complete monorepo migration planning work to enable:
1. **ideacode-mcp updates** - New commands for folder validation and monorepo management
2. **Existing repo cleanup** - Clean up VoiceOS, AVA, WebAvanue, MainAvanues
3. **New repo structure creation** - Create unified MainAvanues monorepo with IDEACODE compliance

**Key Deliverables Created:**
- FOLDER-REGISTRY.md (master folder registry)
- PROJECT-INSTRUCTIONS.md (centralized app/platform instructions)
- IDEACODE-MCP-MONOREPO-COMMANDS.md (MCP command specification)
- Monorepo Migration Agent (8-phase workflow)
- Validation rules (naming conventions, duplicate detection)

---

## 📋 Context: What Was Accomplished

### Session Summary

The user wanted to consolidate 4 repositories into a unified monorepo BEFORE proceeding with database consolidation (which has 87 tasks, 3.5 weeks of work planned).

**Repositories to Consolidate:**
| Repository | Location | Size | Content |
|------------|----------|------|---------|
| VoiceOS | `/Volumes/M-Drive/Coding/Warp/vos4/` | ~500 MB | 38 database tables, 30+ modules |
| AVA | `/Volumes/M-Drive/Coding/AVA/` | ~3 GB | 568 Kotlin files, 2.5 GB models |
| WebAvanue | `/Volumes/M-Drive/Coding/webavanue/` | ~200 MB | 175 Kotlin files, 407+ tests |
| MainAvanues | `/Volumes/M-Drive/Coding/avanue4/` | ~950 MB | Existing monorepo attempt (has duplication) |

**Target:** `/Volumes/M-Drive/Coding/AVA/` (will become MainAvanues monorepo)

### Key User Decisions

1. **Structure:** Platform-First (android/, ios/, desktop/, common/)
2. **No `/common/libs`** - Direct module placement under `common/`
3. **Documentation:** App-based with Master/Platform split
   - `docs/{AppName}/Master/` - Universal specs, architecture, vision
   - `docs/{AppName}/Platform/{platform}/` - Platform-specific docs
4. **Archive:** By platform `docs/archive/{android, ios, desktop}/`
5. **Build System:** Hybrid (single Gradle multi-module with app flexibility)
6. **Code Sharing:** 70%+ KMP target
7. **Team Model:** Mixed/solo developer (flexible for future)
8. **Registry System:** Centralized FOLDER-REGISTRY.md with strict enforcement

---

## 📁 Finalized Monorepo Structure

```
MainAvanues/
├── FOLDER-REGISTRY.md        # Master folder registry (MANDATORY READ)
├── PROJECT-INSTRUCTIONS.md   # Centralized app/platform instructions
├── README.md
├── build.gradle.kts          # Root Gradle build
├── settings.gradle.kts
│
├── android/                  # Android platform apps
│   ├── voiceos/
│   ├── ava/
│   ├── webavanue/
│   └── avaconnect/
│
├── ios/                      # iOS platform apps
│   ├── voiceos/
│   ├── ava/
│   └── webavanue/
│
├── desktop/                  # Desktop platform apps (macOS, Windows, Linux)
│   ├── voiceos/
│   ├── ava/
│   └── webavanue/
│
├── common/                   # KMP shared modules (NO /libs subfolder!)
│   ├── database/             # SQLDelight unified database
│   ├── voice/                # Voice recognition/synthesis
│   ├── accessibility/        # Accessibility features
│   ├── nlu/                  # Natural Language Understanding
│   ├── llm/                  # Large Language Model
│   ├── rag/                  # Retrieval-Augmented Generation
│   ├── cloud/                # Cloud sync services
│   ├── ipc/                  # Inter-Process Communication
│   ├── webview/              # WebView components
│   ├── browser-core/         # Browser engine
│   ├── speech/               # Speech processing
│   └── plugin/               # Plugin system
│
├── docs/
│   ├── voiceos/
│   │   ├── Master/           # Universal VoiceOS specs, architecture
│   │   └── Platform/
│   │       ├── android/
│   │       ├── ios/
│   │       └── desktop/
│   ├── ava/
│   │   ├── Master/
│   │   └── Platform/
│   │       ├── android/
│   │       ├── ios/
│   │       └── desktop/
│   ├── webavanue/
│   │   ├── Master/
│   │   └── Platform/
│   │       ├── android/
│   │       ├── ios/
│   │       └── desktop/
│   ├── avaconnect/
│   │   ├── Master/
│   │   └── Platform/
│   │       ├── android/
│   │       └── ios/
│   ├── ideacode/
│   │   ├── specs/
│   │   ├── protocols/
│   │   └── guides/
│   ├── migration/
│   └── archive/              # Obsolete docs by platform
│       ├── android/
│       ├── ios/
│       └── desktop/
│
├── scripts/
│   ├── migration/
│   └── build/
│
├── .ideacode/
│   ├── config.yml
│   ├── context/
│   └── agents/
│       └── monorepo-migration-refactor/  # Migration agent (created)
│
└── .claude/
    └── CLAUDE.md             # Updated with registry system
```

---

## 📄 Files Created This Session

### 1. FOLDER-REGISTRY.md
**Location:** `/Volumes/M-Drive/Coding/AVA/FOLDER-REGISTRY.md`
**Size:** ~6 KB
**Purpose:** Master registry of ALL folders - single source of truth

**Key Content:**
- Root-level folders table
- Platform folders table (android/voiceos/, ios/ava/, etc.)
- Common modules table (common/database/, common/voice/, etc.)
- Documentation folders table
- File naming conventions
- Enforcement checklist (5-step validation)
- Validation command templates

**Critical Rules:**
```
1. ALWAYS check this registry before creating folders
2. NEVER create folder variations (voice-recognition vs voice_recognition)
3. Follow kebab-case naming (lowercase-with-dashes)
4. NO type prefixes (authentication/ NOT feature-authentication/)
5. Update registry when adding folders
```

### 2. PROJECT-INSTRUCTIONS.md
**Location:** `/Volumes/M-Drive/Coding/AVA/PROJECT-INSTRUCTIONS.md`
**Size:** ~26 KB
**Purpose:** Centralized instructions for all apps and platforms

**Key Sections:**
- Global Monorepo Rules (code sharing, build system, team model)
- VoiceOS (Master + Platform instructions for Android/iOS/Desktop)
- AVA (Master + Platform instructions)
- WebAvanue (Master + Platform instructions)
- AVAConnect (Master + Platform instructions)
- Common Modules documentation
- Build System instructions (Gradle multi-module)
- Documentation instructions (Master vs Platform placement)
- Archive instructions (when to archive, structure)
- Migration instructions (monorepo consolidation)

### 3. IDEACODE-MCP-MONOREPO-COMMANDS.md
**Location:** `/Volumes/M-Drive/Coding/AVA/docs/ideacode/specs/IDEACODE-MCP-MONOREPO-COMMANDS.md`
**Size:** ~18 KB
**Purpose:** Specification for ideacode-mcp commands to implement

**Commands to Implement:**

#### `ideacode_validate_folder`
```typescript
async function ideacode_validate_folder(args: {
  folder_path: string;
  project_root?: string;
}): Promise<{
  status: 'valid' | 'warning' | 'error' | 'not_in_registry';
  message: string;
  suggestion?: string;
  existing_folder?: string;
}>
```
- Validates folder against FOLDER-REGISTRY.md
- Checks naming conventions (kebab-case, no type prefixes)
- Detects variations (case differences, dash/underscore)
- Returns actionable error messages

#### `ideacode_create_folder`
```typescript
async function ideacode_create_folder(args: {
  folder_path: string;
  purpose: string;
  force?: boolean;
  project_root?: string;
}): Promise<{
  status: 'created' | 'exists' | 'error';
  message: string;
  validation_result?: ValidationResult;
}>
```
- Creates folder with automatic validation
- Updates FOLDER-REGISTRY.md after creation
- Prevents creation of invalid folders

#### `ideacode_check_naming`
```typescript
async function ideacode_check_naming(args: {
  name: string;
  type: 'folder' | 'file';
  context?: string;
}): Promise<{
  valid: boolean;
  violations: string[];
  suggestion?: string;
}>
```
- Checks naming convention compliance
- Detects type prefixes, PascalCase, underscores
- Returns violations with suggestions

#### `ideacode_registry_search`
```typescript
async function ideacode_registry_search(args: {
  query: string;
  type?: 'folder' | 'file' | 'all';
  project_root?: string;
}): Promise<{
  results: Array<{path: string, purpose: string, category: string}>;
  count: number;
}>
```
- Searches FOLDER-REGISTRY.md
- Returns matching entries with context

#### `ideacode_registry_update`
```typescript
async function ideacode_registry_update(args: {
  folder_path: string;
  purpose: string;
  category: string;
  shared_by?: string[];
  project_root?: string;
}): Promise<{
  status: 'updated' | 'already_exists' | 'error';
  message: string;
}>
```
- Updates FOLDER-REGISTRY.md with new folder
- Maintains alphabetical order within section
- Updates timestamp

### 4. Migration Agent Files
**Location:** `/Volumes/M-Drive/Coding/AVA/.ideacode/agents/monorepo-migration-refactor/`

**Files:**
- `README.md` - Quick start guide
- `agent.md` - Comprehensive agent definition (6.5 KB)
- `system-prompt.md` - Specialized AI prompt (21 KB)
- `workflow.yaml` - 8-phase execution workflow (8 KB)
- `tools.json` - Tool configuration with safety features (8 KB)
- `validation-rules.json` - Naming rules from FOLDER-REGISTRY.md (10 KB)

### 5. Updated .claude/CLAUDE.md
Added MONOREPO REGISTRY SYSTEM section with:
- References to FOLDER-REGISTRY.md and PROJECT-INSTRUCTIONS.md
- New MCP command references
- Updated pre-code checklist

### 6. Summary Documents
- `docs/MONOREPO-REGISTRY-SYSTEM-SUMMARY.md` - What was created
- `docs/IDEACODE-MONOREPO-HANDOVER.md` - This document

---

## 🔧 Implementation Tasks for ideacode-mcp

### Priority 1: Core Commands

**Task 1.1: Implement `ideacode_validate_folder`**
```
Location: ideacode-mcp/src/tools/validate-folder.ts
Dependencies: fs/promises, path
Input: folder_path, project_root
Output: {status, message, suggestion, existing_folder}

Steps:
1. Find project root (look for FOLDER-REGISTRY.md)
2. Read and parse FOLDER-REGISTRY.md (extract folders from tables)
3. Check naming convention:
   - Is it kebab-case? (pattern: ^[a-z0-9]+(-[a-z0-9]+)*$)
   - Has type prefix? (feature-, module-, lib-, spec-, data-)
   - Has uppercase? (check platform/app exact match)
4. Check registry:
   - Exact match → valid
   - Case variation → warning
   - Dash/underscore variation → warning
   - Not in registry → not_in_registry
   - Naming violation → error
5. Return result with actionable message
```

**Task 1.2: Implement `ideacode_create_folder`**
```
Location: ideacode-mcp/src/tools/create-folder.ts
Dependencies: fs/promises, path, validate-folder

Steps:
1. Call ideacode_validate_folder first
2. If validation fails → return error
3. If folder exists → return exists
4. If validation passes:
   a. Create folder with fs.mkdir
   b. Update FOLDER-REGISTRY.md
   c. Return created status
5. If force=true → warn and create anyway
```

**Task 1.3: Implement `ideacode_check_naming`**
```
Location: ideacode-mcp/src/tools/check-naming.ts

Steps:
1. Check for type prefixes (feature-, module-, etc.)
2. Check for PascalCase/camelCase (for folders and docs)
3. Check for underscores (should use dashes)
4. Check for uppercase (except allowed: README, CHANGELOG, etc.)
5. Return violations array with suggestions
```

**Task 1.4: Implement `ideacode_registry_search`**
```
Location: ideacode-mcp/src/tools/registry-search.ts

Steps:
1. Read FOLDER-REGISTRY.md
2. Parse all tables (extract folder paths and purposes)
3. Search query against paths and purposes
4. Return matching entries with context
```

**Task 1.5: Implement `ideacode_registry_update`**
```
Location: ideacode-mcp/src/tools/registry-update.ts

Steps:
1. Read FOLDER-REGISTRY.md
2. Check if folder already in registry
3. If not present:
   a. Find correct section (Platform, Common, Docs, etc.)
   b. Insert entry maintaining alphabetical order
   c. Update "Last Updated" timestamp
4. Write updated file
5. Return status
```

### Priority 2: Integration

**Task 2.1: Register tools in MCP server**
```typescript
// ideacode-mcp/src/index.ts
import { validateFolder } from './tools/validate-folder';
import { createFolder } from './tools/create-folder';
import { checkNaming } from './tools/check-naming';
import { registrySearch } from './tools/registry-search';
import { registryUpdate } from './tools/registry-update';

// Register in tool list
```

**Task 2.2: Update IDEACODE commands**
```
Update /develop, /implement, /fix to use folder validation
Add pre-code check for FOLDER-REGISTRY.md
```

**Task 2.3: Add monorepo templates**
```
Create template FOLDER-REGISTRY.md for new monorepo projects
Create template PROJECT-INSTRUCTIONS.md
```

### Priority 3: Testing

**Test Cases:**
- ✅ Valid folder (in registry)
- ✅ Valid folder (not in registry)
- ❌ Invalid folder (PascalCase)
- ❌ Invalid folder (type prefix)
- ⚠️ Folder variation (case difference)
- ⚠️ Folder variation (dash/underscore)
- ✅ Create folder with validation
- ✅ Update registry after creation

---

## 🧹 Existing Repo Cleanup Plan

### Step 1: Backup All Repos

```bash
# Create timestamped backups
TIMESTAMP=$(date +%Y%m%d%H%M%S)
cp -r /Volumes/M-Drive/Coding/Warp/vos4/ /Volumes/M-Drive/Coding/BACKUP-vos4-$TIMESTAMP/
cp -r /Volumes/M-Drive/Coding/AVA/ /Volumes/M-Drive/Coding/BACKUP-AVA-$TIMESTAMP/
cp -r /Volumes/M-Drive/Coding/webavanue/ /Volumes/M-Drive/Coding/BACKUP-webavanue-$TIMESTAMP/
cp -r /Volumes/M-Drive/Coding/avanue4/ /Volumes/M-Drive/Coding/BACKUP-avanue4-$TIMESTAMP/
```

### Step 2: Create New Monorepo Structure

```bash
# Use AVA location as new monorepo
cd /Volumes/M-Drive/Coding/AVA/

# Create platform folders
mkdir -p android/{voiceos,ava,webavanue,avaconnect}
mkdir -p ios/{voiceos,ava,webavanue}
mkdir -p desktop/{voiceos,ava,webavanue}

# Create common modules (NO /libs!)
mkdir -p common/{database,voice,accessibility,nlu,llm,rag,cloud,ipc,webview,browser-core,speech,plugin}

# Create docs structure
mkdir -p docs/{voiceos,ava,webavanue,avaconnect}/{Master,Platform/{android,ios,desktop}}
mkdir -p docs/ideacode/{specs,protocols,guides}
mkdir -p docs/{migration,archive/{android,ios,desktop}}

# Create scripts
mkdir -p scripts/{migration,build}

# Create .ideacode
mkdir -p .ideacode/{config,context,agents}
```

### Step 3: Execute Migration Agent

```bash
# Activate migration agent
# Read: .ideacode/agents/monorepo-migration-refactor/agent.md
# Follow: 8-phase workflow in workflow.yaml

# Phase 0: Analysis (autonomous)
# Phase 1: Plan (autonomous)
# Phase 2-6: Migration (supervised, batched approvals)
# Phase 7: Validation (autonomous)
# Phase 8: Finalization (supervised)
```

### Step 4: Verify Structure

```bash
# Validate all folders against FOLDER-REGISTRY.md
ideacode_validate_folder "android/voiceos"
ideacode_validate_folder "common/database"
ideacode_validate_folder "docs/ava/Master"

# Check no naming violations
find . -type d | while read dir; do
  ideacode_check_naming "$dir" --type folder
done
```

### Step 5: Clean Up Old Repos

After migration is verified:

```bash
# Archive old repos (don't delete yet)
mv /Volumes/M-Drive/Coding/Warp/vos4/ /Volumes/M-Drive/Coding/ARCHIVED-vos4/
mv /Volumes/M-Drive/Coding/webavanue/ /Volumes/M-Drive/Coding/ARCHIVED-webavanue/
mv /Volumes/M-Drive/Coding/avanue4/ /Volumes/M-Drive/Coding/ARCHIVED-avanue4/

# Keep backups for 30 days, then delete
# rm -rf /Volumes/M-Drive/Coding/BACKUP-*/
# rm -rf /Volumes/M-Drive/Coding/ARCHIVED-*/
```

---

## 🆕 Creating New Repos with Same Structure

### Option A: Use ideacode-mcp Command (After Implementation)

```bash
# Initialize new monorepo with IDEACODE structure
ideacode_init_monorepo "NewProject" --structure platform-first

# This should:
# 1. Create folder structure per FOLDER-REGISTRY.md template
# 2. Create FOLDER-REGISTRY.md from template
# 3. Create PROJECT-INSTRUCTIONS.md from template
# 4. Create .claude/CLAUDE.md with registry references
# 5. Create build.gradle.kts with multi-module setup
```

### Option B: Manual Creation (Template Files)

**Step 1: Create folder structure**
```bash
mkdir -p NewProject
cd NewProject

# Create all folders per structure above
mkdir -p android/{app1,app2}
mkdir -p ios/{app1,app2}
mkdir -p desktop/{app1,app2}
mkdir -p common/{database,shared}
mkdir -p docs/{app1,app2}/{Master,Platform/{android,ios,desktop}}
mkdir -p docs/ideacode/{specs,protocols,guides}
mkdir -p docs/{migration,archive/{android,ios,desktop}}
mkdir -p scripts/{migration,build}
mkdir -p .ideacode/{config,context,agents}
mkdir -p .claude
```

**Step 2: Copy template files**
```bash
# Copy FOLDER-REGISTRY.md template (customize for your apps)
cp /Volumes/M-Drive/Coding/ideacode/templates/FOLDER-REGISTRY-TEMPLATE.md ./FOLDER-REGISTRY.md

# Copy PROJECT-INSTRUCTIONS.md template (customize for your apps)
cp /Volumes/M-Drive/Coding/ideacode/templates/PROJECT-INSTRUCTIONS-TEMPLATE.md ./PROJECT-INSTRUCTIONS.md

# Copy CLAUDE.md template
cp /Volumes/M-Drive/Coding/ideacode/templates/CLAUDE-MONOREPO-TEMPLATE.md ./.claude/CLAUDE.md
```

**Step 3: Customize templates**
- Update app names in FOLDER-REGISTRY.md
- Update app sections in PROJECT-INSTRUCTIONS.md
- Update references in .claude/CLAUDE.md

---

## 📝 Naming Conventions (Quick Reference)

### Folder Names
| Rule | Valid | Invalid |
|------|-------|---------|
| **Kebab-case** | `voice-recognition` | `VoiceRecognition`, `voice_recognition` |
| **No type prefixes** | `authentication` | `feature-authentication`, `module-auth` |
| **Platform exact** | `android` | `Android`, `ANDROID` |
| **App exact** | `voiceos` | `VoiceOS`, `voice-os` |
| **No trailing slash** | `common/voice` | `common/voice/` |

### File Names
| Type | Pattern | Example |
|------|---------|---------|
| Specs | `{feature}-spec.md` | `voice-recognition-spec.md` |
| Plans | `{feature}-plan.md` | `database-consolidation-plan.md` |
| Tasks | `{feature}-tasks.md` | `monorepo-migration-tasks.md` |
| Architecture | `{component}-architecture.md` | `nlu-architecture.md` |
| Timestamped | `{name}-YYYYMMDDHHMM.md` | `adr-010-20251128.md` |
| Kotlin | `PascalCase.kt` | `VoiceManager.kt` |

### UPPERCASE Exceptions
Only these files: `README.md`, `CHANGELOG.md`, `REGISTRY.md`, `CLAUDE.md`, `LICENSE`, `FOLDER-REGISTRY.md`, `PROJECT-INSTRUCTIONS.md`

---

## 🔗 Key File Locations (Copy to ideacode-mcp)

**Files to copy to ideacode framework:**

| File | Source | Destination |
|------|--------|-------------|
| MCP Command Spec | `AVA/docs/ideacode/specs/IDEACODE-MCP-MONOREPO-COMMANDS.md` | `ideacode/ideacode-mcp/docs/specs/` |
| Folder Registry Template | `AVA/FOLDER-REGISTRY.md` | `ideacode/templates/FOLDER-REGISTRY-TEMPLATE.md` |
| Project Instructions Template | `AVA/PROJECT-INSTRUCTIONS.md` | `ideacode/templates/PROJECT-INSTRUCTIONS-TEMPLATE.md` |
| CLAUDE.md Template | `AVA/.claude/CLAUDE.md` | `ideacode/templates/CLAUDE-MONOREPO-TEMPLATE.md` |
| Validation Rules | `AVA/.ideacode/agents/monorepo-migration-refactor/validation-rules.adat` | `ideacode/ideacode-mcp/src/rules/` |
| Migration Agent | `AVA/.ideacode/agents/monorepo-migration-refactor/*` | `ideacode/agents/templates/monorepo-migration/` |

---

## ✅ Verification Checklist

### Before Starting ideacode-mcp Updates:

- [ ] Read IDEACODE-MCP-MONOREPO-COMMANDS.md for full specification
- [ ] Read FOLDER-REGISTRY.md to understand structure
- [ ] Read validation-rules.json for naming rules
- [ ] Review agent files for workflow understanding

### After Implementing Commands:

- [ ] Test ideacode_validate_folder with valid/invalid folders
- [ ] Test ideacode_create_folder creates folder and updates registry
- [ ] Test ideacode_check_naming detects violations
- [ ] Test ideacode_registry_search finds folders
- [ ] Test ideacode_registry_update adds entries correctly

### After Monorepo Migration:

- [ ] All folders match FOLDER-REGISTRY.md 100%
- [ ] All folder names use kebab-case
- [ ] No type prefixes in folder names
- [ ] All documentation preserved (specs, ADRs, .mmd files)
- [ ] All builds pass (Gradle for all apps/platforms)
- [ ] Zero file duplicates
- [ ] FOLDER-REGISTRY.md up-to-date
- [ ] PROJECT-INSTRUCTIONS.md accurate

---

## 📞 Questions & Clarifications

If you have questions about this handover:

1. **For structure decisions:** Review user's answers in PROJECT-INSTRUCTIONS.md header
2. **For naming rules:** Check validation-rules.json
3. **For MCP commands:** Read IDEACODE-MCP-MONOREPO-COMMANDS.md
4. **For migration workflow:** Read agent.md and workflow.yaml
5. **For safety features:** Check tools.json

**Key Contacts:**
- User: Manoj Jhawar (project owner)
- Framework: IDEACODE v9.0

---

## 🚀 Recommended Next Steps

### For ideacode-mcp Developer (Priority Order):

1. **Read** IDEACODE-MCP-MONOREPO-COMMANDS.md thoroughly
2. **Implement** ideacode_validate_folder (most critical)
3. **Implement** ideacode_create_folder
4. **Implement** ideacode_check_naming
5. **Implement** ideacode_registry_search
6. **Implement** ideacode_registry_update
7. **Create** monorepo templates (FOLDER-REGISTRY, PROJECT-INSTRUCTIONS)
8. **Add** ideacode_init_monorepo command (optional, uses templates)
9. **Update** /develop, /implement to use folder validation
10. **Test** all commands with the MainAvanues monorepo

### For Monorepo Migration (After MCP Commands Ready):

1. **Activate** migration agent (read agent.md)
2. **Execute** Phase 0 (autonomous analysis)
3. **Review** analysis report
4. **Approve** Phase 1 (migration plan)
5. **Execute** Phases 2-6 (supervised migration)
6. **Validate** Phase 7 (build verification)
7. **Finalize** Phase 8 (commits and cleanup)
8. **Archive** old repositories

---

**Handover Status:** ✅ COMPLETE
**Documents Created:** 7 files
**Total Documentation:** ~80 KB
**Ready for:** ideacode-mcp implementation + monorepo migration

**Good luck! 🚀**
