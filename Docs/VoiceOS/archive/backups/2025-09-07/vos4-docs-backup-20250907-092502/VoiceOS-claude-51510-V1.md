# VOS4 Agent Context & Instructions

## 🔴 MANDATORY: Read Instructions Based on Your Task

### ⚠️ MASTER RULES (READ FIRST - APPLIES TO ALL PROJECTS):
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/MASTER-AGENT-INSTRUCTIONS.md` - 🔴 START HERE - Universal rules
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/MASTER-CODING-STANDARDS.md` - Universal coding standards
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/SPECIALIZED-AGENTS-PROTOCOL.md` - 🔴 MANDATORY use of specialized agents
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/PRECOMPACTION-PROTOCOL.md` - 🔴 MANDATORY at 90% context

### 📋 VOS4-SPECIFIC INSTRUCTIONS:
→ `/docs/voiceos-master/standards/` - VOS4-specific rules and guidelines
→ `/docs/voiceos-master/standards/NAMING-CONVENTIONS.md` - 🔴 MANDATORY naming rules (NO REDUNDANCY)
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/AI-INSTRUCTIONS-SEQUENCE.md` - VOS4 instruction reading order
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/MASTER-AI-INSTRUCTIONS.md` - VOS4 implementation details
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/MASTER-STANDARDS.md` - VOS4 standards
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/DOCUMENTATION-CHECKLIST.md` - MANDATORY pre-commit checklist
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/CURRENT-TASK-PRIORITY.md` - Current priority tasks
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/MIGRATION-STATUS-2025-01-23.md` - ✅ Migration Complete

### 🚨 ZERO TOLERANCE POLICIES:
1. **NEVER delete files/folders without EXPLICIT written approval**
2. **ALL code mergers MUST be 100% functionally equivalent** (unless told otherwise)
3. **ALL documentation MUST be updated BEFORE commits** (including visuals)
4. **Stage documentation WITH code in SAME commit**
5. **NO AI/Claude references in commits**
6. **MANDATORY: COT/ROT/TOT analysis for ALL code issues** (present options unless told "work independently")
7. **MANDATORY: Create precompaction report at 90% (±5%) context - NO EXCEPTIONS**
8. **MANDATORY: Use multiple specialized agents for parallel tasks - ALWAYS**
9. **MANDATORY: NO documentation files in root folder - ALL docs go in /docs/ structure**

## 📂 PROJECT STRUCTURE & DOCUMENTATION

### Root-Level Folders (Quick Access):
```
/Volumes/M Drive/Coding/Warp/vos4/
├── coding/                        # 🎯 ALL ACTIVE DEVELOPMENT WORK - CHECK FIRST
│   ├── TODO/                     # Active tasks
│   │   ├── VOS4-TODO-Master.md   # Overall project tasks
│   │   └── [Module]-TODO.md      # Module-specific tasks
│   ├── STATUS/                   # Current state - CHECK DAILY
│   │   ├── VOS4-Status-Current.md # Overall project status
│   │   └── [Module]-Status.md    # Module-specific status
│   ├── ISSUES/                   # Active problems to fix
│   │   ├── CRITICAL/             # Fix immediately
│   │   ├── HIGH/                 # Fix soon
│   │   ├── MEDIUM/               # Fix when possible
│   │   └── LOW/                  # Fix when time permits
│   ├── DECISIONS/                # Architecture Decision Records
│   ├── planning/                 # Sprint planning, roadmaps
│   ├── reviews/                  # Code review tracking
│   └── metrics/                  # Development metrics
├── agent-tools/                   # 🔧 Python/shell scripts for AI
├── docs/                         # 📚 All documentation
├── modules/                      # 📦 All application and library modules
│   ├── apps/                    # Application modules
│   ├── libraries/                # Library modules
│   └── managers/                 # Manager modules
└── tests/                        # Test code
```

### Documentation Structure:
```
docs/
├── voiceos-master/               # System-level documentation
│   ├── architecture/             # System design, overview
│   ├── roadmap/                  # Future plans, milestones
│   ├── implementation/           # How it's built
│   ├── diagrams/                 # Visual documentation
│   ├── changelog/                # Version history
│   ├── testing/                  # Test plans, coverage
│   ├── status/                   # Detailed status reports
│   ├── developer-manual/         # Dev guides
│   ├── user-manual/              # User guides
│   ├── standards/                # Coding standards, conventions
│   ├── project-management/       # PM docs, reports
│   └── reference/                # Quick references
│       └── api/                  # System-wide API documentation
│
├── voice-cursor/                 # Module documentation (same structure)
│   ├── architecture/
│   ├── roadmap/
│   ├── implementation/
│   ├── diagrams/
│   ├── changelog/
│   ├── testing/
│   ├── status/
│   ├── developer-manual/
│   ├── user-manual/
│   ├── module-standards/
│   ├── project-management/
│   └── reference/
│       └── api/                  # Voice Cursor API documentation
│
├── speech-recognition/           # Each module has complete structure
├── device-manager/               # Each with api/ under reference/
├── voice-accessibility/
├── command-manager/
├── data-manager/
├── hud-manager/
├── localization-manager/
├── voice-ui/
├── vos-data-manager/
├── keyboard/
├── settings/                     # All 12 modules with full structure
├── templates/                    # Documentation templates
└── archive/                      # Old/deprecated documentation
```

## 📝 MANDATORY DOCUMENTATION WORKFLOW

### 🔴 MANDATORY: Documentation Location Rules

#### NEVER Place Documentation in Root Folder:
- **❌ FORBIDDEN:** `/Volumes/M Drive/Coding/Warp/vos4/*.md` (except README.md, claude.md, BEF-SHORTCUTS.md)
- **✅ REQUIRED:** All documentation MUST go in `/docs/` structure
- **VIOLATION = CRITICAL ERROR:** Any analysis, report, or documentation file in root must be moved immediately

#### Correct Documentation Locations:
- **Analysis Reports:** `/docs/voiceos-master/status/`
- **Build Reports:** `/docs/voiceos-master/project-management/build-reports/`
- **Architecture Docs:** `/docs/voiceos-master/architecture/`
- **Module Docs:** `/docs/[module-name]/`
- **Changelogs:** `/docs/[module-name]/changelog/`
- **API Docs:** `/docs/[module-name]/reference/api/`

### When to Update Documentation:

#### 1. **TODO Updates** (`/coding/TODO/`)
- **WHEN:** Starting any new task
- **UPDATE:** Mark task as `in_progress` in appropriate TODO file
- **WHEN:** Completing any task
- **UPDATE:** Mark task as `completed` with completion date
- **WHEN:** Finding new work needed
- **UPDATE:** Add new TODO items immediately

#### 2. **STATUS Updates** (`/coding/STATUS/`)
- **WHEN:** Daily at start of work
- **UPDATE:** Current progress in VOS4-Status-Current.md
- **WHEN:** Completing major milestone
- **UPDATE:** Module status and overall status
- **WHEN:** Encountering blockers
- **UPDATE:** Add to ISSUES folder and update status

#### 3. **CHANGELOG Updates** (`/docs/[module]/changelog/`)
- **WHEN:** BEFORE making code changes
- **CHECK:** Existing changelog for context
- **WHEN:** AFTER making code changes
- **UPDATE:** Date, what changed, why it changed
- **FORMAT:** `YYYY-MM-DD: [Component] - Change description (Reason)`

#### 4. **ARCHITECTURE Updates** (`/docs/[module]/architecture/`)
- **WHEN:** Adding new components
- **UPDATE:** Architecture diagrams and documentation
- **WHEN:** Changing system design
- **UPDATE:** Design decisions and rationale
- **WHEN:** Removing components
- **UPDATE:** Mark as deprecated, explain why

#### 5. **DIAGRAMS Updates** (`/docs/[module]/diagrams/`)
- **WHEN:** Architecture changes
- **UPDATE:** System/module diagrams
- **WHEN:** Flow changes
- **UPDATE:** Sequence and flow diagrams
- **WHEN:** UI changes
- **UPDATE:** UI mockups and wireframes

#### 6. **API Updates** (`/docs/[module]/reference/api/`)
- **WHEN:** Adding new public methods
- **UPDATE:** API documentation in module's api/ folder
- **WHEN:** Changing method signatures
- **UPDATE:** Mark old as deprecated, document new
- **WHEN:** Removing methods
- **UPDATE:** Deprecation notices with migration guide

#### 7. **DECISIONS Updates** (`/coding/DECISIONS/`)
- **WHEN:** Making architectural decisions
- **CREATE:** New ADR (Architecture Decision Record)
- **FORMAT:** ADR-XXX-Title.md
- **INCLUDE:** Context, Decision, Consequences, Alternatives

## 🚀 MANDATORY: Specialized Agents & Parallel Processing

### When to Use Multiple Specialized Agents (REQUIRED):
1. **Phase Transitions** - Deploy agents for each subphase in parallel
2. **Independent Tasks** - Run non-dependent tasks simultaneously
3. **Analysis & Implementation** - Analyze next phase while implementing current
4. **Documentation Updates** - Update different docs in parallel
5. **Testing & Development** - Test completed work while developing next features

### Parallel Execution Rules:
- **ALWAYS** use parallel agents when tasks are independent
- **ALWAYS** use specialized agents for their domain (coding, testing, docs)
- **MAXIMIZE** throughput by running multiple subphases in parallel
- **Example**: While testing Phase 1.1c, start analyzing Phase 1.2a

### Sequential Execution (When Required):
- Same file modifications (avoid conflicts)
- Dependent tasks (output feeds input)
- Critical path items (order matters)

## 🔄 CRITICAL: Agent-Instructions Synchronization Rule

**WHENEVER you update ANY file in `/Volumes/M Drive/Coding/Warp/Agent-Instructions/`:**
1. **IMMEDIATELY copy the updated file to `/Volumes/M Drive/Coding/Warp/vos4/Agent-Instructions/`**
2. **This applies even if you're not actively using the VOS4 Agent-Instructions folder**
3. **Purpose:** Maintains backward compatibility and ensures consistency across all environments
4. **Command:** `cp "/Volumes/M Drive/Coding/Warp/Agent-Instructions/[filename]" "/Volumes/M Drive/Coding/Warp/vos4/Agent-Instructions/"`
5. **DO NOT skip this step** - Other agents/tools may still reference the VOS4 folder

## 📋 Quick Command Reference

### Workflow Commands:
- **UD** = Update Documents
- **SCP** = Stage, Commit & Push (MANDATORY: Update docs first, stage by category)
- **SUF** = Stage, Update & Full workflow
- **PRECOMPACTION** = Create pre-compaction report (see `/Volumes/M Drive/Coding/Warp/Agent-Instructions/PRECOMPACTION-PROTOCOL.md`)

### 🚨 MANDATORY Commit Rules:
When told to "stage files":
1. **FIRST:** Update/create ALL required documentation
2. **STAGE BY CATEGORY:**
   - Commit 1: All documentation files together
   - Commit 2: Code by module/app (group similar items)
   - Commit 3: Other changes if any
3. **SCP Command:** Stage → Commit → Push (with docs updated FIRST)
4. **NEVER:** Mix documentation and code in same commit (unless small fix)

### AI Review Patterns:
- **COT** = Chain of Thought (linear reasoning)
- **ROT** = Reflection on Thought (evaluation)
- **TOT** = Tree of Thought (explore alternatives)
- **CRT** = Combined Review Technique (full analysis with options)

## 📋 Living Document Reminder

**BEFORE Code Changes:** 
- Check `/coding/TODO/[Module]-TODO.md` for task context
- Check `/docs/[module]/changelog/` for history
- Check `/coding/STATUS/[Module]-Status.md` for current state

**AFTER Code Changes:**
- Update `/docs/[module]/changelog/` with changes
- Update `/coding/STATUS/[Module]-Status.md` with progress
- Update `/coding/TODO/[Module]-TODO.md` with completion
- Update architecture/diagrams if structure changed

**BEFORE Commits:** 
- Verify all documentation updated
- Run through DOCUMENTATION-CHECKLIST.md
- Ensure no AI references in commit messages

## ⚠️ CRITICAL: Pre-Commit MANDATORY Checklist

**BEFORE ANY COMMIT - ALL MUST BE COMPLETED:**
1. ✅ Functional equivalency verified (100% unless approved otherwise)
2. ✅ NO files/folders deleted without written approval
3. ✅ ALL affected documentation updated:
   - Module changelog (MANDATORY)
   - Architecture diagrams/flowcharts (if changed)
   - UI layouts/wireframes (if changed)
   - Status and TODO updates
4. ✅ Documentation staged WITH code changes
5. ✅ Visual documentation updated (diagrams, sequences, flows)

**NEVER include AI/tool references in commits:**
- ❌ NO "Claude", "Anthropic", "AI" mentions
- ❌ NO "Generated with" statements  
- ❌ NO "Co-Authored-By: Claude"
- ✅ Keep commits professional and tool-agnostic

## 🔧 Quick Reference

**Location:** `/Volumes/M Drive/Coding/Warp/vos4`
**Branch:** VOS4 (STAY ON THIS BRANCH)
**Git Required:** Yes - Must have working git repository
**Key Principle:** Direct implementation, zero interfaces
**Namespace:** `com.augmentalis.*` (NEW STANDARD - NO MORE com.ai)
**Database:** Room (migrating from ObjectBox)

## 📊 Project Status Overview

**Documentation Structure:** New compartmentalized structure (2025-02-07)
- All modules at same level in `/docs/`
- TODO and STATUS at root level for quick access
- Complete module self-containment

**Compliance Status:** 95% naming compliance achieved
- All ALL_CAPS files fixed
- Module structure standardized
- Documentation reorganized

---
**Last Updated:** 2025-02-07 - Complete documentation restructure, new folder organization
**Previous Update:** 2025-02-06 - Documentation cleanup completed (95% compliance)
**Note:** This is the authoritative AI instruction file. CLAUDE.MD (uppercase) has been deprecated.