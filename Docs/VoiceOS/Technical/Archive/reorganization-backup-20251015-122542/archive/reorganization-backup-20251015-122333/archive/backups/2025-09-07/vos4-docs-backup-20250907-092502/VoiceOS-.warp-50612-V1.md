# VOS4 Agent Context & Instructions

## 🔴 MANDATORY: Read Instructions Based on Your Task

### ⚠️ MASTER RULES (READ FIRST - APPLIES TO ALL PROJECTS):
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/MASTER-AGENT-INSTRUCTIONS.md` - 🔴 START HERE - Universal rules
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/MASTER-CODING-STANDARDS.md` - Universal coding standards
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/SPECIALIZED-AGENTS-PROTOCOL.md` - 🔴 MANDATORY use of specialized agents
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/PRECOMPACTION-PROTOCOL.md` - 🔴 MANDATORY at 90% context

### 📋 VOS4-SPECIFIC INSTRUCTIONS:
→ `/docs/project-instructions/` - VOS4-specific rules and guidelines
→ `/docs/project-instructions/NAMING-CONVENTIONS.md` - 🔴 MANDATORY naming rules (NO REDUNDANCY)
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

### Efficiency Targets:
- Phase 0: Reduced from 1 week to 45 minutes using parallel agents
- Aim for 60-80% time reduction through parallelization
- Deploy 3-5 specialized agents when possible

### Autonomous Execution:
- Continue through phases without waiting for approval UNLESS:
  - Architectural decisions needed
  - Unclear requirements
  - Errors that block progress
  - Explicitly told to wait
- Update tracking documents continuously
- Commit after each subphase completion

### 🔄 CRITICAL: Agent-Instructions Synchronization Rule
**WHENEVER you update ANY file in `/Volumes/M Drive/Coding/Warp/Agent-Instructions/`:**
1. **IMMEDIATELY copy the updated file to `/Volumes/M Drive/Coding/Warp/vos4/Agent-Instructions/`**
2. **This applies even if you're not actively using the VOS4 Agent-Instructions folder**
3. **Purpose:** Maintains backward compatibility and ensures consistency across all environments
4. **Command:** `cp "/Volumes/M Drive/Coding/Warp/Agent-Instructions/[filename]" "/Volumes/M Drive/Coding/Warp/vos4/Agent-Instructions/"`
5. **DO NOT skip this step** - Other agents/tools may still reference the VOS4 folder

### Task-Specific Instructions:

#### For Coding:
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/CODING-GUIDE.md` - Code patterns and examples
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/SESSION-LEARNINGS.md` - Recent fixes and gotchas
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/AI-REVIEW-ABBREVIATIONS.md` - Review patterns (CRT, COT, ROT, TOT)

#### For Documentation:
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/DOCUMENTATION-GUIDE.md` - How to write/update docs

#### For Understanding Context:
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/SESSION-LEARNINGS.md` - What's been done recently
→ `/docs/Status/Current/` - Current project state
→ `/docs/TODO/VOS4-TODO-Master.md` - Active tasks

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
- **ROT** = Reflection (evaluation)
- **TOT** = Train of Thought (explore alternatives)
- **CRT** = Combined Review (full analysis with options)

## 📋 Living Document Reminder

**BEFORE Code Changes:** Check `/docs/modules/[module]/[Module]-Changelog.md`
**AFTER Code Changes:** Update changelog with: Date - What - Why
**BEFORE Commits:** Verify all documentation updated

## ⚠️ CRITICAL: Pre-Commit MANDATORY Checklist

**BEFORE ANY COMMIT - ALL MUST BE COMPLETED:**
1. ✅ Functional equivalency verified (100% unless approved otherwise)
2. ✅ NO files/folders deleted without written approval
3. ✅ ALL affected documentation updated:
   - Module changelog (MANDATORY)
   - Architecture diagrams/flowcharts
   - UI layouts/wireframes (if changed)
   - Status and TODO updates
4. ✅ Documentation staged WITH code changes
5. ✅ Visual documentation updated (diagrams, sequences, flows)

**NEVER include AI/tool references in commits:**
- ❌ NO "Claude", "Anthropic", "AI" mentions
- ❌ NO "Generated with" statements  
- ❌ NO "Co-Authored-By: Claude"
- ✅ Keep commits professional and tool-agnostic

## 📋 Project Overview

VOS4 is a modular voice SDK platform designed for Android applications, providing multi-provider voice recognition (Vivoka, Vosk, Google) with advanced accessibility features and 19 language support.

### Core Components
- **VoiceCore** - Core voice recognition engine with provider abstraction
- **VivokaProvider** - High-accuracy commercial voice provider
- **VoskProvider** - Offline voice recognition provider
- **GoogleProvider** - Google Cloud Speech integration
- **AccessibilityCore** - Android accessibility service integration

## 🔧 Quick Reference

**Location:** `/Volumes/M Drive/Coding/Warp/VOS4`
**Branch:** VOS4 (STAY ON THIS BRANCH)
**Git Required:** Yes - Must have working git repository
**Key Principle:** Direct implementation, zero interfaces
**Namespace:** `com.augmentalis.*` (NEW STANDARD - NO MORE com.ai)
**Database:** ObjectBox ONLY

## 📁 Project Structure

```
/VOS4/
├── claude.md                   # Claude AI entry point
├── .warp.md                    # This file - Warp IDE instructions
├── .cursor.md                  # Cursor IDE instructions
├── README.md                   # Project overview
├── Agent-Instructions/         # (DEPRECATED - Use /Volumes/M Drive/Coding/Warp/Agent-Instructions/ instead)
├── docs/                       # Documentation
├── apps/                       # Application modules
├── modules/                    # Core SDK modules
└── tests/                      # Test suites
```

## 🎯 Current Focus

### Active Development
1. Voice provider integration optimization
2. Accessibility service enhancements
3. Performance improvements
4. Documentation updates

### Performance Targets
- Startup: <500ms
- Provider switching: <100ms
- Command recognition: <80ms
- Memory: <25MB (Vosk) or <50MB (Vivoka)
- Battery: <1.5% per hour

## 🔄 Development Process

### Before Starting Work
1. Read this file (.warp.md)
2. Check master instructions
3. Review VOS4-specific rules
4. Check current task priorities

### When Updating Agent-Instructions
1. **Primary Location:** Always update `/Volumes/M Drive/Coding/Warp/Agent-Instructions/`
2. **Sync Step:** IMMEDIATELY copy to `/Volumes/M Drive/Coding/Warp/vos4/Agent-Instructions/`
3. **Verification:** Confirm both locations have identical content
4. **No Exceptions:** This applies to ALL instruction file updates

### Before Creating Code
1. Check for existing implementation
2. Run COT/ROT analysis
3. Consider simpler approach
4. Document decision

### Before Committing
1. Run all tests
2. Check performance impact
3. Update ALL documentation
4. Stage by category
5. If Agent-Instructions were updated, verify sync to VOS4 folder

## 📋 Notes

- This is a modular SDK - keep components decoupled
- Focus on performance and minimal footprint
- All modules must be independently testable
- Direct implementation pattern (no unnecessary abstractions)

---
**Last Updated:** 2025-09-03 - Added mandatory specialized agents & parallel processing requirements
**Note:** Universal instructions are in `/Volumes/M Drive/Coding/Warp/Agent-Instructions/`, project-specific in `/docs/project-instructions/`
**CRITICAL:** Always sync universal Agent-Instructions updates to VOS4 folder for backward compatibility
