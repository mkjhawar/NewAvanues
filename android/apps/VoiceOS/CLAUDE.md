# VOS4 Quick Reference Card

**Project:** VoiceOS (VOS4)
**Location:** `/Volumes/M Drive/Coding/VoiceOS`
**Branch:** vos4-legacyintegration
**Namespace:** `com.augmentalis.*`
**Database:** Room (KSP support)

---

## 🚨 CRITICAL: MANDATORY First Steps

**⚠️ THESE STEPS ARE MANDATORY - NOT OPTIONAL**

**1. ALWAYS Get Local Time First:**
```bash
date "+%Y-%m-%d %H:%M:%S %Z"  # For documentation
date "+%y%m%d-%H%M"            # For filename timestamps
```

**2. 🔴 MANDATORY: Task Initialization Protocol**
→ **READ FIRST**: `/Volumes/M Drive/Coding/Docs/agents/instructions/Protocol-Task-Initialization.md`
**Purpose**: Ensures correct instructions loaded, prevents errors from missing context
**Time**: 15-20 minutes (or 3-minute quick init for urgent tasks)
**Enforcement**: ZERO TOLERANCE - proceeding without initialization = protocol violation

**3. 🔴 MANDATORY: Zero-Tolerance Policies**
→ **READ SECOND**: `/Volumes/M Drive/Coding/Docs/agents/instructions/Reference-Zero-Tolerance-Policies.md`
**Purpose**: Critical rules that apply to ALL work
**Includes**: Task initialization, precompaction at 90%, deletion policies, functional equivalency

**4. 🔴 MANDATORY: Precompaction at 90% Context**
→ **AUTOMATIC TRIGGER**: `/Volumes/M Drive/Coding/Docs/agents/instructions/Protocol-Precompaction.md`
**Trigger**: When context usage reaches 90%
**Purpose**: Preserve detailed context for seamless continuation
**Enforcement**: ZERO TOLERANCE - proceeding past 90% without precompaction = protocol violation

**5. Read Master Bootstrap:**
→ `/Volumes/M Drive/Coding/Docs/agents/claude/CLAUDE.md`

**6. Load Universal Protocols (7 Core Files):**
→ `/Volumes/M Drive/Coding/Docs/agents/instructions/`
   - Protocol-Coding-Standards.md
   - Protocol-Documentation.md
   - Protocol-Commit.md
   - Protocol-Agent-Deployment.md
   - Protocol-Pre-Implementation-QA.md
   - Protocol-Precompaction.md (already read in step 4)
   - Protocol-IDEADEV-Universal-Framework.md (for complex features)

**7. Load VOS4-Specific Instructions:**
→ `/Volumes/M Drive/Coding/VoiceOS/Docs/ProjectInstructions/`
   - Protocol-VOS4-Coding-Standards.md
   - Protocol-VOS4-Documentation.md
   - Protocol-VOS4-Commit.md
   - Protocol-VOS4-Agent-Deployment.md

---

## 📋 VOS4 Core Protocols (Task-Specific)

**Universal Protocols (ALL projects):**
| Task | Protocol Location |
|------|------------------|
| **Coding Standards** | `/Coding/Docs/agents/instructions/Protocol-Coding-Standards.md` |
| **Documentation** | `/Coding/Docs/agents/instructions/Protocol-Documentation.md` |
| **Commits** | `/Coding/Docs/agents/instructions/Protocol-Commit.md` |
| **Agent Deployment** | `/Coding/Docs/agents/instructions/Protocol-Agent-Deployment.md` |
| **Q&A Sessions** | `/Coding/Docs/agents/instructions/Protocol-Pre-Implementation-QA.md` |
| **Precompaction (90%)** | `/Coding/Docs/agents/instructions/Protocol-Precompaction.md` |
| **IDEADEV (Complex Features)** | `/Coding/Docs/agents/instructions/Protocol-IDEADEV-Universal-Framework.md` |

**VOS4-Specific Protocols (overrides/extensions):**
| Task | Protocol Location |
|------|------------------|
| **VOS4 Coding** | `/vos4/Docs/ProjectInstructions/Protocol-VOS4-Coding-Standards.md` |
| **VOS4 Documentation** | `/vos4/Docs/ProjectInstructions/Protocol-VOS4-Documentation.md` |
| **VOS4 Commits** | `/vos4/Docs/ProjectInstructions/Protocol-VOS4-Commit.md` |
| **VOS4 Agents** | `/vos4/Docs/ProjectInstructions/Protocol-VOS4-Agent-Deployment.md` |
| **VOS4 Q&A** | `/vos4/Docs/ProjectInstructions/Protocol-VOS4-Pre-Implementation-QA.md` |

---

## 🚨 ZERO TOLERANCE: Git Commit Rules

**❌ NEVER INCLUDE AI/TOOL ATTRIBUTION IN COMMITS**

**FORBIDDEN in commit messages:**
- ❌ "Generated with Claude Code" or similar
- ❌ "Co-Authored-By: Claude" or any AI attribution
- ❌ "🤖" emoji or AI references
- ❌ Links to claude.com or AI tools
- ❌ ANY mention of AI assistance

**✅ CORRECT commit format:**
```bash
git commit -m "fix(Module): Brief description

Detailed explanation of what changed and why.

Files Changed:
- file1.kt - what changed
- file2.kt - what changed

Build Status: BUILD SUCCESSFUL"
```

**⚠️ ENFORCEMENT:** This is a ZERO TOLERANCE policy. Violating = immediate protocol violation.

---

## 📚 General Standards (All Projects)

**Master Bootstrap & Instructions:**
→ `/Volumes/M Drive/Coding/Docs/agents/claude/CLAUDE.md`
→ `/Volumes/M Drive/Coding/Docs/agents/instructions/`

**Key General Standards:**
- **Zero Tolerance Policies:** `Reference-Zero-Tolerance-Policies.md`
- **Development Core:** `Standards-Development-Core.md`
- **Documentation Workflow:** `Guide-Documentation-Structure.md`
- **AI Review Patterns:** `Reference-AI-Review-Patterns.md`
- **UI/Frontend Design:** See master CLAUDE.md

**Agent Files Location Guide:**
→ `/Volumes/M Drive/Coding/Docs/agents/AGENT-FILES-LOCATION-GUIDE.md`

---

## 📂 VOS4 Project Structure

**Master Template**: `/Volumes/M Drive/Coding/Docs/agents/instructions/Guide-Documentation-Structure.md`

```
/vos4/
├── docs/                          # 📚 ALL DOCUMENTATION & TRACKING
│   ├── master/                    # 🎯 Project-wide tracking
│   │   ├── changelogs/
│   │   │   ├── CHANGELOG-MASTER.md
│   │   │   ├── CHANGELOG-CURRENT.md
│   │   │   └── archives/
│   │   ├── status/
│   │   │   ├── PROJECT-STATUS-CURRENT.md
│   │   │   ├── PROJECT-STATUS-SUMMARY.md
│   │   │   └── archives/
│   │   ├── tasks/
│   │   │   ├── PROJECT-TODO-MASTER.md     # Master TODO
│   │   │   ├── PROJECT-TODO-PRIORITY.md   # Priority tasks
│   │   │   ├── PROJECT-TODO-BACKLOG.md    # Backlog
│   │   │   └── completed/                 # Completed tasks
│   │   └── inventories/
│   │
│   ├── planning/                  # Planning & architecture
│   │   ├── project/              # Requirements, roadmap
│   │   ├── architecture/         # System architecture
│   │   │   └── decisions/       # ADRs
│   │   ├── implementation/       # Implementation plans
│   │   └── features/             # Feature specs
│   │
│   ├── modules/                   # Module-specific docs (19 modules)
│   │   ├── VoiceAccessibility/
│   │   ├── VoiceCursor/
│   │   ├── CommandManager/
│   │   └── ... (all PascalCase)
│   │
│   ├── visuals/                   # Visual documentation
│   │   ├── system/               # System diagrams
│   │   ├── sequences/            # Sequence diagrams
│   │   └── technical/            # Technical diagrams
│   │
│   ├── templates/                 # Templates & standards
│   │   ├── document-templates/
│   │   └── standards/
│   │       └── NAMING-CONVENTIONS.md
│   │
│   ├── commits/                   # Commit documentation
│   │   ├── current/              # Active reviews
│   │   └── archives/             # Historical
│   │
│   ├── scripts/                   # Automation scripts
│   ├── Active/                    # Current work snapshots
│   ├── Archive/                   # Deprecated docs
│   ├── ProjectInstructions/       # VOS4-specific instructions
│   └── documentation-control/     # Doc management
│
├── modules/                       # 💻 CODE ONLY (no docs!)
│   ├── apps/                     # 4 apps
│   ├── libraries/                # 6 libraries
│   └── managers/                 # 5 managers
│
└── tests/                        # Test code
```

**Key Rules:**
- ✅ CODE: `/modules/` = `.kt/.java/.xml` files ONLY
- ✅ DOCS: `/docs/` = ALL documentation, tracking, tasks
- ✅ NO `/coding/` folder - Everything in `/docs/` following master template
- ❌ NEVER mix code and documentation in same folder

---

## 🚀 VOS4 Development Methodology (IDEADEV)

**VOS4 uses a 3-tier approach based on task complexity:**

### Tier 1: Direct (Simple Tasks)
**When:** Single file, <30 min, well understood
```bash
"Fix null pointer in VoiceCommandProcessor.kt line 42"
```
**Skip:** Subagents, IDEADEV, formal planning

### Tier 2: Subagents (Medium Complexity) ⭐ RECOMMENDED
**When:** 2-3 modules, known approach, 1-3 hours
```bash
"@vos4-orchestrator Implement DatabaseManagerImpl TODO #3"
```
**Gets Automatically:**
- ✅ Testing by `@vos4-test-specialist` (BLOCKS if tests fail)
- ✅ Documentation by `@vos4-documentation-specialist`
- ✅ Quality gates enforced by orchestrator

### Tier 3: Full IDEADEV (Complex Features)
**When:** Multiple modules, >4 hours, high risk, unfamiliar domain

**SP(IDE)R Protocol:**
1. **Specify** (WHAT) → `/ideadev/specs/`
2. **Plan** (HOW) → `/ideadev/plans/`
3. **IDE Loop** (for EACH phase):
   - **I**mplement → Specialist builds feature
   - **D**efend → `@vos4-test-specialist` creates tests (MANDATORY)
   - **E**valuate → Verify requirements, get user approval
4. **Review** (Lessons) → `/ideadev/reviews/`

### VOS4 Subagents (8 Specialists)
Located in `/.claude/agents/`:
1. `@vos4-orchestrator` - Master router, enforces IDE Loop
2. `@vos4-android-expert` - Android platform
3. `@vos4-kotlin-expert` - Kotlin & coroutines
4. `@vos4-database-expert` - Room/KSP database
5. `@vos4-test-specialist` - PROACTIVE testing (auto-invoked)
6. `@vos4-architecture-reviewer` - Design review
7. `@vos4-documentation-specialist` - PROACTIVE docs (auto-invoked)
8. `@vos4-performance-analyzer` - Performance optimization

### Quick Decision Guide
```
Complexity < 30 min?     → Direct (no framework)
Complexity 1-3 hours?    → @vos4-orchestrator (subagents only)
Complexity > 4 hours?    → Full IDEADEV (Specify → Plan → IDE → Review)
```

**Universal Framework:** `/Volumes/M Drive/Coding/Docs/agents/instructions/Protocol-IDEADEV-Universal-Framework.md`
**VOS4 Usage Guide:** `/docs/Active/How-To-Use-IDEADEV-Framework-VOS4-251018-1906.md`
**VOS4 IDEADEV Folder:** `/ideadev/README.md`
**Setup Script (any project):** `/Volumes/M Drive/Coding/Docs/agents/instructions/setup-ideadev-universal.sh`

---

## 📛 VOS4 Naming Conventions

**Quick Reference:** `/docs/voiceos-master/standards/NAMING-CONVENTIONS.md`

| Type | Convention | Example |
|------|-----------|---------|
| Kotlin/Java | `PascalCase.kt` | `AccessibilityScrapingIntegration.kt` |
| Documentation | `PascalCase-With-Hyphens-YYMMDD-HHMM.md` | `Architecture-Refactor-251015-0912.md` |
| Code Modules | `PascalCase/` | `VoiceAccessibility/` |
| Doc Module Folders | `PascalCase/` (same as code) | `VoiceAccessibility/` |
| System Doc Folders | `kebab-case/` | `voiceos-master/` |
| Packages | `lowercase.dot.separated` | `com.augmentalis.voiceaccessibility` |
| Classes | `PascalCase` | `VoiceCommandProcessor` |
| Methods | `camelCase` | `processVoiceCommand()` |
| Constants | `SCREAMING_SNAKE_CASE` | `MAX_DEPTH` |

**Code-to-Documentation Mapping:**
```
modules/apps/VoiceAccessibility/  → docs/modules/VoiceAccessibility/
modules/managers/CommandManager/  → docs/modules/CommandManager/
modules/libraries/UUIDCreator/    → docs/modules/UUIDCreator/
```

---

## 🎯 VOS4 Module List (20 Total)

**Code modules and their matching doc folders (names must be IDENTICAL):**

**Apps (5):**
1. LearnApp → LearnApp
2. VoiceCursor → VoiceCursor
3. VoiceOSCore → VoiceOSCore
4. VoiceRecognition → VoiceRecognition
5. VoiceUI → VoiceUI

**Libraries (9):**
6. DeviceManager → DeviceManager
7. MagicElements → MagicElements
8. MagicUI → MagicUI
9. SpeechRecognition → SpeechRecognition
10. Translation → Translation
11. UUIDCreator → UUIDCreator
12. VoiceKeyboard → VoiceKeyboard
13. VoiceOsLogger → VoiceOsLogger
14. VoiceUIElements → VoiceUIElements

**Managers (5):**
15. CommandManager → CommandManager
16. HUDManager → HUDManager
17. LicenseManager → LicenseManager
18. LocalizationManager → LocalizationManager
19. VoiceDataManager → VoiceDataManager

**Note:** VoiceAccessibility is NOT a separate module - it's part of VoiceOSCore

---

## 🚨 VOS4-Specific Critical Rules

### Mandatory Q&A Before Implementation
→ See: `/vos4/Docs/ProjectInstructions/Protocol-VOS4-Pre-Implementation-QA.md`
- Present 2-4 options with pros/cons
- ONE question at a time
- Wait for user approval before coding

### Documentation Timestamps (VOS4 Requirement)
```bash
# Get timestamp
date "+%y%m%d-%H%M"

# Example filename
VOS4-Status-Current-251015-0912.md
```
**Rule:** When updating timestamped files, CREATE NEW FILE with new timestamp

### VOS4 Architecture Principles
- Direct implementation, zero interfaces (unless strategic value)
- ObjectBox for persistence (Room for future)
- `com.augmentalis.*` namespace (NOT com.ai)
- Module self-containment
- Performance-first design

---

## 📋 Quick Commands

```bash
# Navigation
cd /coding/TODO/              # Active tasks
cd /coding/STATUS/            # Current status
cd /docs/voiceos-master/      # System docs

# Local time (ALWAYS run first!)
date "+%Y-%m-%d %H:%M:%S %Z"

# Timestamp for filenames
date "+%y%m%d-%H%M"
```

**Workflow Abbreviations:**
- **UD** = Update Documents
- **SCP** = Stage, Commit & Push
- **CRT** = Combined Review Technique (COT+ROT+TOT)

---

## 🔧 Before You Start

**Checklist:**
- [ ] Run `date` to get local time
- [ ] Read master CLAUDE.md for general standards
- [ ] Read VOS4-specific protocol for your task
- [ ] Check `/coding/TODO/` for task context
- [ ] Check `/coding/STATUS/` for current state
- [ ] Check module's `/docs/[module]/changelog/` for history

---

## 📝 After You Finish

**Documentation Updates (MANDATORY):**
- [ ] Update `/docs/[module]/changelog/` with changes
- [ ] Update `/coding/STATUS/` with progress (new timestamped file)
- [ ] Update `/coding/TODO/` with completion
- [ ] Update architecture/diagrams if structure changed
- [ ] Create status report in `/docs/Active/` (timestamped)

**Commit Rules:**
- [ ] Documentation updated FIRST
- [ ] Stage by category (docs → code → tests)
- [ ] NO AI/tool references in commits
- [ ] Verify functional equivalency

→ See: `/vos4/Docs/ProjectInstructions/Protocol-VOS4-Commit.md`

---

## 🔗 Related Documentation

**Current Work:**
- `/coding/TODO/VOS4-TODO-Master-[timestamp].md`
- `/coding/STATUS/VOS4-Status-Current-[timestamp].md`
- `/docs/Active/` (recent status reports)

**Standards & References:**
- `/docs/voiceos-master/standards/NAMING-CONVENTIONS.md`
- `/docs/voiceos-master/architecture/` (system architecture)
- `/Coding/Docs/agents/AGENT-FILES-LOCATION-GUIDE.md`

**Scripts & Automation:**
- `/docs/scripts/` (all automation scripts)
- `/docs/scripts/README.md` (script documentation)

---

## 💡 Pro Tips

1. **Module docs are self-contained:** Each module in `/docs/modules/[name]/` has complete documentation
2. **Active/Archive model:** Current work in `Active/`, completed in `Archive/`
3. **Diagrams with architecture:** Located in `Architecture/Diagrams/[type]/`
4. **Timestamp everything:** Use `date "+%y%m%d-%H%M"` for all new docs
5. **Check AGENT-FILES-LOCATION-GUIDE.md** when looking for instruction files

---

## 🆘 Quick Help

**Can't find something?**
1. Check `/docs/modules/[module-name]/` for module docs
2. Check `/coding/` for active work
3. Check `/Coding/Docs/agents/AGENT-FILES-LOCATION-GUIDE.md` for instructions
4. Check master CLAUDE.md for general standards

**Need more detail?**
- This is a QUICK REFERENCE - detailed instructions are in protocol files
- All VOS4 protocols: `/vos4/Docs/ProjectInstructions/`
- All general standards: `/Coding/Docs/agents/instructions/`

---

**Version:** 2.1.0 (Added IDEADEV Methodology)
**Last Updated:** 2025-10-19 00:20:57 PDT
**Previous Version:** 2.0.0 (Streamlined Quick Reference)
**Changelog:**
- v2.1.0 (2025-10-19): Added IDEADEV methodology section
  - Added 3-tier approach (Direct, Subagents, Full IDEADEV)
  - Added VOS4 subagent architecture (8 specialists)
  - Added SP(IDE)R protocol overview
  - Added quick decision guide for complexity assessment
  - Added references to detailed IDEADEV documentation
- v2.0.0 (2025-10-15): Streamlined to quick reference card format
  - Removed redundant general content (now in master files)
  - Added clear references to where standards are located
  - Reduced from 560+ lines to ~280 lines
  - Focused on VOS4-specific information only
  - Made quick reference card instead of comprehensive guide

---

**For detailed general standards**, see:
→ `/Volumes/M Drive/Coding/Docs/agents/claude/CLAUDE.md`
→ `/Volumes/M Drive/Coding/Docs/agents/instructions/`

**For VOS4-specific detailed protocols**, see:
→ `/Volumes/M Drive/Coding/VoiceOS/Docs/ProjectInstructions/`
