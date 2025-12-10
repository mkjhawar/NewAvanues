# Monorepo Registry System - Implementation Summary

**Date:** 2025-11-28
**Status:** ✅ Registry System Complete - Ready for Migration Spec
**Next Step:** Create comprehensive monorepo migration specification

---

## 🎯 What Was Accomplished

### 1. Finalized Monorepo Folder Structure

**Pattern:** Platform-First with app-based documentation

```
MainAvanues/
├── android/
│   ├── voiceos/
│   ├── ava/
│   ├── webavanue/
│   └── avaconnect/
├── ios/
│   ├── voiceos/
│   ├── ava/
│   └── webavanue/
├── desktop/
│   ├── voiceos/
│   ├── ava/
│   └── webavanue/
├── common/  (NO /libs - direct module placement)
│   ├── database/
│   ├── voice/
│   ├── nlu/
│   ├── llm/
│   └── ...
├── docs/
│   ├── voiceos/{Master, Platform}
│   ├── ava/{Master, Platform}
│   ├── webavanue/{Master, Platform}
│   ├── avaconnect/{Master, Platform}
│   ├── ideacode/
│   ├── migration/
│   └── archive/{android, ios, desktop}
├── scripts/
├── .ideacode/
├── .claude/
├── FOLDER-REGISTRY.md
├── PROJECT-INSTRUCTIONS.md
└── build.gradle.kts
```

**Key Decisions:**
- ✅ Platform-First organization (android/, ios/, desktop/)
- ✅ Removed `/common/libs` subfolder (direct placement in common/)
- ✅ App-based documentation in `docs/{AppName}/Master/` and `docs/{AppName}/Platform/`
- ✅ Archive by platform: `docs/archive/{platform}/`
- ✅ Hybrid build system (single Gradle multi-module with app flexibility)
- ✅ Target: 70%+ KMP code sharing

---

## 📄 Files Created

### 1. FOLDER-REGISTRY.md (Root Level)
**Location:** `/Volumes/M-Drive/Coding/AVA/FOLDER-REGISTRY.md`

**Purpose:** Master registry of ALL folders in monorepo - single source of truth

**Contents:**
- ✅ Root-level folders (android/, ios/, desktop/, common/, docs/, scripts/)
- ✅ Platform folders (android/voiceos/, ios/ava/, etc.)
- ✅ Common modules (common/database/, common/voice/, etc.)
- ✅ Documentation folders (docs/voiceos/Master/, docs/ava/Platform/android/, etc.)
- ✅ Script folders (scripts/migration/, scripts/build/)
- ✅ File naming conventions (specs, plans, architecture, timestamped)
- ✅ Enforcement checklist (5-step validation before folder creation)
- ✅ Validation command templates (for ideacode-mcp)

**Key Features:**
- 📋 Comprehensive folder inventory with purposes
- 🚨 Strict naming rules (kebab-case, no type prefixes)
- ✅ Enforcement checklist to prevent duplication
- 📝 File naming patterns for all document types
- 🔍 Quick reference tables for all folder categories

**Usage:**
```bash
# BEFORE creating any folder:
1. Open FOLDER-REGISTRY.md
2. Search for similar folder names
3. Check naming conventions
4. Verify folder doesn't already exist
5. Create folder and update registry
```

---

### 2. PROJECT-INSTRUCTIONS.md (Root Level)
**Location:** `/Volumes/M-Drive/Coding/AVA/PROJECT-INSTRUCTIONS.md`

**Purpose:** Centralized instructions for all apps and platforms - single reference point

**Contents:**
- 🌍 Global monorepo rules (code sharing, build system, team model, enforcement)
- 📱 VoiceOS instructions (Master + Platform-specific: Android, iOS, Desktop)
- 🤖 AVA instructions (Master + Platform-specific: Android, iOS, Desktop)
- 🌐 WebAvanue instructions (Master + Platform-specific: Android, iOS, Desktop)
- 🔌 AVAConnect instructions (Master + Platform-specific: Android, iOS)
- 📦 Common modules documentation (database, voice, nlu, llm, etc.)
- 🔧 Build system instructions (Gradle multi-module, hybrid approach)
- 📝 Documentation instructions (Master vs Platform, file naming, archival)
- 🗃️ Archive instructions (when to archive, structure, process)
- 🚀 Migration instructions (monorepo consolidation principles)

**Key Sections:**

#### VoiceOS
- Architecture: Voice-first, accessibility, plugin system (MagicCode)
- Core modules: voice, accessibility, speech, plugin, ipc, database
- 38 database tables (SQLDelight 2.0.1)
- Performance: <0.5s initialization (via embedding cache)
- Platform-specific features per platform

#### AVA
- Architecture: Privacy-first, 95% cache hit, dual model (MobileBERT-384 + mALBERT-768)
- Core modules: nlu, llm, rag, database, cloud
- 11 database tables (SQLDelight 2.0.1)
- Performance: <0.2s NLU init, <5ms FTS search
- Model files: 2.5 GB (embeddings, llm, wakeword)

#### WebAvanue
- Architecture: Privacy-first, 95% KMP sharing
- Core modules: browser-core, webview, database, cloud
- 7 database tables (SQLDelight)
- Performance: <50ms tab switching, 20x faster favorites
- Test coverage: 407+ tests, 90%+ coverage

#### AVAConnect
- Architecture: Universal IPC, service discovery
- Core modules: ipc, cloud, database
- Platform support: Android, iOS (Desktop planned)

**Usage:**
```bash
# At session start:
1. Read PROJECT-INSTRUCTIONS.md
2. Navigate to your app section (VoiceOS, AVA, WebAvanue, AVAConnect)
3. Read Master instructions (universal architecture)
4. Read Platform instructions (platform-specific details)
5. Follow naming conventions and folder structure
```

---

### 3. IDEACODE-MCP-MONOREPO-COMMANDS.md
**Location:** `/Volumes/M-Drive/Coding/AVA/docs/ideacode/specs/IDEACODE-MCP-MONOREPO-COMMANDS.md`

**Purpose:** Specification for ideacode-mcp to implement monorepo folder validation commands

**Commands Specified:**

#### `ideacode_validate_folder`
Validate folder path before creation
- Returns: valid | warning | error | not_in_registry
- Checks: naming convention, registry presence, variations

#### `ideacode_create_folder`
Create folder with automatic validation and registry update
- Validates first using ideacode_validate_folder
- Creates folder if valid
- Updates FOLDER-REGISTRY.md automatically

#### `ideacode_check_naming`
Check filename or folder name follows conventions
- Detects: type prefixes, PascalCase, underscores, uppercase
- Returns: violations + suggestions

#### `ideacode_registry_search`
Search FOLDER-REGISTRY.md for folders/files
- Search by: folder name, app name, module name
- Returns: matching entries with context

#### `ideacode_registry_update`
Update FOLDER-REGISTRY.md with new folder entry
- Adds entry to appropriate section
- Maintains alphabetical order
- Updates timestamp

**Implementation Guide:**
- Language: TypeScript (for ideacode-mcp MCP server)
- Dependencies: fs/promises, path, marked (optional), @modelcontextprotocol/sdk
- Pseudo-code provided for key functions
- Error messages with clear, actionable suggestions
- Integration with IDEACODE workflow

**Next Step:** Implement these commands in ideacode-mcp server

---

### 4. Updated .claude/CLAUDE.md
**Location:** `/Volumes/M-Drive/Coding/AVA/.claude/CLAUDE.md`

**Changes:**
- ✅ Added MONOREPO REGISTRY SYSTEM section at top
- ✅ Updated pre-code checklist to include FOLDER-REGISTRY.md
- ✅ Added references to new MCP commands
- ✅ Included link to IDEACODE-MCP-MONOREPO-COMMANDS.md

**New Instructions:**
```
BEFORE ANY FILE OPERATION:
1. Read: FOLDER-REGISTRY.md (MANDATORY)
2. Read: PROJECT-INSTRUCTIONS.md (app/platform instructions)
3. Run: ideacode_validate_folder "{folder_path}"
4. Check: Naming conventions (kebab-case, no type prefixes)
```

---

## 🎯 User Decisions Captured

Based on user answers to the structure validation questions:

### 1. Folder Structure
**Decision:** Platform-First
- Top-level: `android/`, `ios/`, `desktop/`
- Apps within platforms: `android/voiceos/`, `android/ava/`, etc.
- **Modification:** Remove `/common/libs` → use `common/` directly

### 2. Documentation Structure
**Decision:** App-based with Master/Platform split
- Master: `docs/{AppName}/Master/` (universal specs, architecture, vision, flows)
- Platform: `docs/{AppName}/Platform/{platform}/` (platform-specific docs)
- Archive: `docs/archive/{platform}/` (obsolete docs by platform)

### 3. Team Model
**Decision:** Mixed/solo developer
- Structure supports both platform teams and feature teams
- Flexible organization

### 4. Code Sharing Strategy
**Decision:** High sharing (70%+ target)
- Share as much as possible via KMP
- Business logic, database, models, domain code shared
- Platform-specific: UI, platform APIs, native integrations
- Exception: Apps may differ due to backend platform requirements

### 5. Build System
**Decision:** Hybrid approach
- Root: Single Gradle multi-module build (build.gradle.kts)
- Shared modules: Centralized in `common/` with unified dependencies
- App builds: Can have independent configurations when needed

---

## 🚨 Enforcement Rules

### Folder Naming Conventions

| Rule | Valid | Invalid |
|------|-------|---------|
| **Kebab-case** | `voice-recognition` | `VoiceRecognition`, `voice_recognition` |
| **No type prefixes** | `authentication` | `feature-authentication`, `module-auth` |
| **Platform exact** | `android` | `Android`, `ANDROID` |
| **App exact** | `voiceos` | `VoiceOS`, `voice-os` |
| **Lowercase** | `docs` | `Docs`, `DOCS` |

**Exceptions (UPPERCASE allowed):**
- README.md
- CHANGELOG.md
- REGISTRY.md
- CLAUDE.md
- LICENSE
- FOLDER-REGISTRY.md
- PROJECT-INSTRUCTIONS.md

### File Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Specs | `{feature}-spec.md` | `voice-recognition-spec.md` |
| Plans | `{feature}-plan.md` | `database-consolidation-plan.md` |
| Tasks | `{feature}-tasks.md` | `monorepo-migration-tasks.md` |
| Architecture | `{component}-architecture.md` | `nlu-architecture.md` |
| Timestamped | `{name}-YYYYMMDDHHMM.md` | `adr-010-202511280930.md` |

---

## ✅ Pre-Code Checklist (Updated)

**BEFORE creating any folder:**
```
1. ✅ Read FOLDER-REGISTRY.md
2. ✅ Search for existing folder with similar name
3. ✅ Check naming convention (kebab-case, no type prefixes)
4. ✅ Verify folder pattern matches location (platform/app, common/module, etc.)
5. ✅ Run: ideacode_validate_folder "{folder_path}" (when MCP commands available)
6. ✅ Create folder only if validation passes
7. ✅ Update FOLDER-REGISTRY.md after creation
```

**BEFORE creating any file:**
```
1. ✅ Check file naming convention (kebab-case for docs, PascalCase for Kotlin)
2. ✅ Verify NO type prefixes (use `auth-spec.md` NOT `spec-auth.md`)
3. ✅ Check if file already exists
4. ✅ Place in correct folder per FOLDER-REGISTRY.md
```

---

## 🚀 Next Steps

### Immediate (Ready Now)

1. **Create Monorepo Migration Specification**
   - Use `/specify` command with detailed migration requirements
   - Reference FOLDER-REGISTRY.md for target structure
   - Reference PROJECT-INSTRUCTIONS.md for app details
   - Include systematic file placement rules
   - Include obsolete file removal criteria
   - Include documentation preservation/archival rules

2. **Create Migration Plan**
   - Use `/plan` on the migration spec
   - Break down into phases (repo-by-repo or category-by-category)
   - Estimate effort
   - Define success criteria

3. **Create Migration Tasks**
   - Generate detailed task list from plan
   - Track progress with TodoWrite

### Future (After Migration Spec)

4. **Implement ideacode-mcp Commands**
   - Use `IDEACODE-MCP-MONOREPO-COMMANDS.md` as specification
   - Implement in ideacode-mcp server (TypeScript)
   - Test with FOLDER-REGISTRY.md
   - Deploy updated MCP server

5. **Execute Monorepo Migration**
   - Follow migration plan
   - Use FOLDER-REGISTRY.md for folder creation
   - Update PROJECT-INSTRUCTIONS.md as needed
   - Preserve all docs, specs, architecture, .mmd files
   - Archive obsolete docs to `docs/archive/{platform}/`

6. **Resume Database Consolidation**
   - After monorepo complete
   - Execute database consolidation spec 012 (87 tasks, 3.5 weeks)
   - VoiceOS → AVA → WebAvanue sequential migration

---

## 📊 Key Metrics

### Registry System

| Metric | Value |
|--------|-------|
| **Folders Documented** | ~60+ (root, platform, common, docs, scripts) |
| **Apps Documented** | 4 (VoiceOS, AVA, WebAvanue, AVAConnect) |
| **Platforms Documented** | 3 (Android, iOS, Desktop) |
| **File Naming Patterns** | 6 (specs, plans, tasks, architecture, guides, timestamped) |
| **Naming Rules** | 5 (kebab-case, no prefixes, platform exact, app exact, lowercase) |

### Project Instructions

| Metric | Value |
|--------|-------|
| **Global Rules** | 4 (code sharing, build system, team model, enforcement) |
| **Apps Detailed** | 4 (VoiceOS, AVA, WebAvanue, AVAConnect) |
| **Platforms per App** | 2-3 (Android, iOS, Desktop) |
| **Common Modules** | 12+ (database, voice, nlu, llm, rag, etc.) |
| **Documentation Sections** | 8 (Master, Platform, IDEACODE, Migration, Archive, Build, etc.) |

### MCP Commands

| Metric | Value |
|--------|-------|
| **Commands Specified** | 5 (validate, create, check, search, update) |
| **Validation Rules** | 6+ (kebab-case, no prefixes, variations, etc.) |
| **Error Messages** | Clear, actionable with examples |
| **Implementation Status** | Specification complete, ready for coding |

---

## 🔍 Files Summary

| File | Size | Purpose | Status |
|------|------|---------|--------|
| `FOLDER-REGISTRY.md` | ~6 KB | Master folder registry | ✅ Complete |
| `PROJECT-INSTRUCTIONS.md` | ~26 KB | Centralized app/platform instructions | ✅ Complete |
| `docs/ideacode/specs/IDEACODE-MCP-MONOREPO-COMMANDS.md` | ~18 KB | MCP command specification | ✅ Complete |
| `.claude/CLAUDE.md` | ~4 KB | Updated with registry system | ✅ Updated |
| `docs/MONOREPO-REGISTRY-SYSTEM-SUMMARY.md` | This file | Summary document | ✅ Complete |

**Total:** 5 files created/updated

---

## ✨ Benefits

### Prevents Problems
- ❌ No more folder duplication (e.g., `voice-recognition/` vs `voice_recognition/`)
- ❌ No more naming inconsistencies (PascalCase vs kebab-case)
- ❌ No more type prefixes (feature-, module-, etc.)
- ❌ No more lost documentation (clear archival process)

### Enables Efficiency
- ✅ Single reference point for all app/platform instructions
- ✅ Fast validation before folder creation
- ✅ Clear guidelines for file placement
- ✅ Consistent naming across entire monorepo

### Supports Scale
- ✅ Handles 4 apps, 3 platforms, 90+ modules
- ✅ Supports solo developer or multiple teams
- ✅ Enables 70%+ KMP code sharing
- ✅ Future-proof for additional apps/platforms

---

## 🎓 How to Use This System

### For AI Assistants (Claude Code)

**At session start:**
1. Read `FOLDER-REGISTRY.md`
2. Read `PROJECT-INSTRUCTIONS.md`
3. Read `.claude/CLAUDE.md` (updated with registry references)

**Before creating folders:**
1. Check `FOLDER-REGISTRY.md` for existing folders
2. Validate naming convention (kebab-case, no type prefixes)
3. Run `ideacode_validate_folder` (when available)
4. Update registry after creation

**Before creating files:**
1. Check file naming convention
2. Place in correct folder per registry
3. Follow documentation structure from `PROJECT-INSTRUCTIONS.md`

### For Developers

**Starting work on an app:**
1. Open `PROJECT-INSTRUCTIONS.md`
2. Navigate to your app section (VoiceOS, AVA, WebAvanue, AVAConnect)
3. Read Master instructions (universal architecture)
4. Read Platform instructions (your platform: Android, iOS, Desktop)
5. Follow folder structure from `FOLDER-REGISTRY.md`

**Creating new modules:**
1. Check `FOLDER-REGISTRY.md` for similar modules
2. Use kebab-case naming (e.g., `common/auth` not `common/AuthModule`)
3. Update registry with new module
4. Document in `PROJECT-INSTRUCTIONS.md` (Common Modules section)

---

## 📚 References

### Created Files
- `FOLDER-REGISTRY.md` - Master folder registry
- `PROJECT-INSTRUCTIONS.md` - Centralized app/platform instructions
- `docs/ideacode/specs/IDEACODE-MCP-MONOREPO-COMMANDS.md` - MCP command spec
- `.claude/CLAUDE.md` - Updated with registry system
- `docs/MONOREPO-REGISTRY-SYSTEM-SUMMARY.md` - This file

### IDEACODE Framework
- `/Volumes/M-Drive/Coding/ideacode/docs/ideacode/protocols/Protocol-File-Organization.md`
- `/Volumes/M-Drive/Coding/ideacode/updateideas/foldernaming.md`
- `/Volumes/M-Drive/Coding/ideacode/docs/migration/directory-structure-v2-to-v8.5.md`

### Previous Work
- Database consolidation spec: `docs/ideacode/specs/012-database-consolidation-merge/spec.md` (ON HOLD)
- Repository scans: AVA, VoiceOS, WebAvanue, MainAvanues (completed)

---

**Status:** ✅ Registry System Complete
**Next Command:** `/specify "Migrate VoiceOS, AVA, WebAvanue, and MainAvanues repositories into unified monorepo with IDEACODE-compliant folder structure, systematic file placement, obsolete file removal, and documentation preservation/archival"`
**Ready to Proceed:** Yes
