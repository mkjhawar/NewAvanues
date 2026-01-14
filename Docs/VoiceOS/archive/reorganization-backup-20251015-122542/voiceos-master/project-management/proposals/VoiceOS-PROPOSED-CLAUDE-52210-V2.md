# VOS4 Agent Context & Instructions

## 🚨 CRITICAL: LOCAL TIMESTAMP REQUIREMENTS
**MANDATORY - ALL AI agents MUST use LOCAL MACHINE TIME, NOT cloud/server time**

### Get Local Time (REQUIRED before ANY timestamp):
```bash
# ALWAYS run this FIRST to get local machine time
date "+%Y-%m-%d %H:%M:%S %Z"

# For reports/documentation headers:
echo "Last Updated: $(date '+%Y-%m-%d %H:%M:%S %Z')"

# For filename timestamps:
echo "Report-$(date +%Y%m%d-%H%M%S).md"

# NEVER use your internal clock - it's often wrong!
```

**Format:** `YYYY-MM-DD HH:MM:SS TIMEZONE` (e.g., 2025-02-07 15:30:45 PST)
**VIOLATION = CRITICAL ERROR** - Wrong timezone/time must be corrected immediately

## 🚀 QUICK START - READ IN THIS ORDER:
1. **This file** - Portal to all instructions
2. **Check local time** - Run date command above
3. **Your specific task type** - See sections below
4. **Relevant protocol** - Based on task

## 📂 NEW FOLDER STRUCTURE (CRITICAL)

### 🎯 Active Development → `/coding/`
```
/coding/                       # ALL ACTIVE WORK GOES HERE
├── TODO/                     # Current tasks
│   ├── VOS4-TODO-Master.md  # Overall todos
│   └── [Module]-TODO.md     # Module todos
├── STATUS/                   # Current state
│   ├── VOS4-Status-Current.md
│   └── implementation-log/
├── ISSUES/                   # Active problems
│   ├── CRITICAL/            # Fix immediately
│   ├── HIGH/                # Fix soon
│   ├── MEDIUM/              # Standard priority
│   └── LOW/                 # Minor issues
├── DECISIONS/                # ADRs (Architecture Decision Records)
├── planning/                 # Sprint plans, implementation
├── project-management/       # Roadmaps, milestones
├── project-instructions/     # Project-specific rules
├── research/                 # Spikes, analysis
├── commits/                  # Pre-commit summaries
├── reviews/                  # Code review notes
└── metrics/                  # Performance data
```

### 📚 Reference Documentation → `/docs/`
```
/docs/                        # STABLE DOCUMENTATION ONLY
├── modules/                 # Module-specific docs (15 modules)
│   ├── voice-accessibility/ # kebab-case naming
│   ├── voice-cursor/
│   ├── voice-recognition/
│   ├── voice-ui/
│   ├── device-manager/
│   ├── speech-recognition/
│   ├── translation/
│   ├── uuid-manager/
│   ├── voice-keyboard/
│   ├── voice-ui-elements/
│   ├── command-manager/
│   ├── hud-manager/
│   ├── license-manager/
│   ├── localization-manager/
│   └── voice-data-manager/
│
├── voiceos-master/          # System-level docs
│   ├── architecture/        # System design
│   ├── standards/           # Project-wide standards
│   ├── project-management/  # Overall PM
│   ├── reference/           # System references
│   ├── reports/             # Analysis reports
│   ├── metrics/             # Performance metrics
│   └── guides/              # System guides
│
├── archive/                 # Old/deprecated docs
├── templates/               # Doc templates
└── documentation-control/   # Doc management
```

### 🔑 CRITICAL: Where Files Go

**Goes in `/coding/`:**
- ✅ "I'm working on X" → `/coding/TODO/`
- ✅ "Current bug" → `/coding/ISSUES/`
- ✅ "Sprint plan" → `/coding/planning/`
- ✅ "Active research" → `/coding/research/`
- ✅ "Current status" → `/coding/STATUS/`

**Goes in `/docs/`:**
- ✅ "How does X work?" → `/docs/modules/[module-name]/reference/`
- ✅ "API documentation" → `/docs/modules/[module-name]/reference/api/`
- ✅ "Architecture" → `/docs/modules/[module-name]/architecture/`
- ✅ "Completed work" → `/docs/modules/[module-name]/implementation/`
- ✅ "System-wide docs" → `/docs/voiceos-master/`
- ✅ "Old content" → `/docs/archive/`

**NEVER in root folder:**
- ❌ No analysis reports in `/`
- ❌ No documentation files in `/`
- ❌ Only README.md, claude.md, BEF-SHORTCUTS.md allowed

## ⚡ SHORTCUTS & COMMANDS

### Essential Commands:
```bash
# Navigation (from project root)
cd /coding/TODO/              # Active tasks
cd /coding/STATUS/            # Current status
cd /docs/voiceos-master/      # System docs
cd /docs/[module-name]/       # Module docs

# Quick Commands  
UD    # Update Documents
SCP   # Stage, Commit & Push (docs first!)
SUF   # Stage, Update & Full workflow
CRT   # Combined Review Technique (COT+ROT+TOT)

# ALWAYS get local time first!
date "+%Y-%m-%d %H:%M:%S %Z"
```

## 🔴 MANDATORY RULES - ZERO TOLERANCE

### NEVER DO (Instant Critical Error):
1. **❌ Use cloud/server time (ALWAYS use local machine time)**
2. **❌ Put files in root folder (except README, claude, BEF-SHORTCUTS)**
3. **❌ Delete files/folders without EXPLICIT written approval**
4. **❌ Mix documentation and code in same commit**
5. **❌ Include "Claude", "AI", "Anthropic" in commits**
6. **❌ Skip COT/ROT/TOT analysis for code issues**
7. **❌ Use com.ai.* namespace (use com.augmentalis.*)**

### ALWAYS DO (Mandatory):
1. **✅ Get local time with `date` command FIRST**
2. **✅ Include timezone in ALL timestamps**
3. **✅ Put active work in `/coding/`**
4. **✅ Put stable docs in `/docs/`**
5. **✅ Update documentation BEFORE commits**
6. **✅ Stage files by category (docs, code, tests)**
7. **✅ Use specialized agents for parallel tasks**
8. **✅ Maintain 100% functional equivalency**
9. **✅ Create precompaction report at 90% context**

## 📋 TASK-SPECIFIC INSTRUCTIONS

### For CODING Tasks:
→ `/Agent-Instructions/VOS4-CODING-PROTOCOL.md`
- COT/ROT/TOT analysis requirements
- Functional equivalency rules
- Module self-containment
- Kotlin patterns & examples
- com.augmentalis.* namespace

### For DOCUMENTATION Tasks:
→ `/Agent-Instructions/VOS4-DOCUMENTATION-PROTOCOL.md`
- File naming: [Module]-[Topic]-[Type].md
- Time format: YYYY-MM-DD HH:MM:SS TIMEZONE
- Update triggers & workflow
- Correct folder placement

### For AGENT DEPLOYMENT:
→ `/Agent-Instructions/VOS4-AGENT-PROTOCOL.md`
- When to use multiple agents
- Parallel vs sequential execution
- Task tool configuration

### For GIT/COMMITS:
→ `/Agent-Instructions/VOS4-COMMIT-PROTOCOL.md`
- Stage by category rules
- Commit message format
- No AI references policy

### At 90% Context:
→ `/Agent-Instructions/PRECOMPACTION-PROTOCOL.md`
- MANDATORY precompaction report
- What to include/exclude

## 🎯 CURRENT PRIORITIES
→ `/coding/TODO/VOS4-TODO-Master.md`
→ `/coding/STATUS/VOS4-Status-Current.md`

## 📝 Document Placement Quick Guide

| Content Type | Location | Example |
|-------------|----------|---------|
| Active TODO | `/coding/TODO/` | VoiceCursor-TODO.md |
| Current bug | `/coding/ISSUES/[PRIORITY]/` | Bug-12345.md |
| Sprint plan | `/coding/planning/sprint-plans/` | Sprint-2025-09.md |
| Build report | `/coding/planning/build-reports/` | Build-Analysis.md |
| Module architecture | `/docs/modules/VoiceCursor/architecture/` | Design.md |
| Module API | `/docs/modules/VoiceCursor/reference/api/` | API.md |
| Module changelog | `/docs/modules/VoiceCursor/changelog/` | CHANGELOG.md |
| System architecture | `/docs/voiceos-master/architecture/` | System-Design.md |
| Old docs | `/docs/archive/2025/` | Old-Report.md |

## 🚨 CRITICAL REMINDERS

### Before ANY Task:
1. Get local time: `date "+%Y-%m-%d %H:%M:%S %Z"`
2. Check current branch: `git status`
3. Read relevant protocol file
4. Check `/coding/TODO/` for context
5. Verify correct folder for new files

### Before ANY Commit:
1. Update all affected documentation
2. Place docs in correct folders
3. Stage by category (docs → code → tests)
4. No AI/tool references in messages
5. Verify functional equivalency

### Creating New Files:
1. Active work → `/coding/`
2. Stable docs → `/docs/`
3. System-wide → `/docs/voiceos-master/`
4. Module-specific → `/docs/[module-name]/`
5. NEVER in root folder

## 🔧 Quick Reference

**Project Root:** `/Volumes/M Drive/Coding/Warp/vos4/`
**Namespace:** `com.augmentalis.*` (NOT com.ai)
**Database:** ObjectBox (NOT Room)
**Time:** Local machine time with timezone (NOT UTC/cloud)
**Branch:** Check with `git status`
**Pattern:** Direct implementation (NO interfaces)

---
**Last Updated:** Run `date "+%Y-%m-%d %H:%M:%S %Z"` for current time
**Note:** This is your main portal. Protocol files contain detailed rules.
**CRITICAL:** Always use local machine time, never cloud/server time!