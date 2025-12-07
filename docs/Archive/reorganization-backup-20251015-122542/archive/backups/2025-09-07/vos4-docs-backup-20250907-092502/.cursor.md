# VOS4 Cursor IDE Context & Instructions

## 🔴 MANDATORY: Read Instructions Based on Your Task

### ⚠️ MASTER RULES (READ FIRST - APPLIES TO ALL PROJECTS):
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/MASTER-AGENT-INSTRUCTIONS.md` - 🔴 START HERE - Universal rules
→ `/Volumes/M Drive/Coding/Warp/Agent-Instructions/MASTER-CODING-STANDARDS.md` - Universal coding standards

### 📋 VOS4-SPECIFIC INSTRUCTIONS:
→ `/docs/project-instructions/` - VOS4-specific rules and guidelines (TO BE CREATED)
→ `/Agent-Instructions/AI-INSTRUCTIONS-SEQUENCE.md` - VOS4 instruction reading order
→ `/Agent-Instructions/MASTER-AI-INSTRUCTIONS.md` - VOS4 implementation details
→ `/Agent-Instructions/MASTER-STANDARDS.md` - VOS4 standards
→ `/Agent-Instructions/DOCUMENTATION-CHECKLIST.md` - MANDATORY pre-commit checklist
→ `/Agent-Instructions/CURRENT-TASK-PRIORITY.md` - Current priority tasks
→ `/Agent-Instructions/MIGRATION-STATUS-2025-01-23.md` - ✅ Migration Complete

### 🚨 ZERO TOLERANCE POLICIES:
1. **NEVER delete files/folders without EXPLICIT written approval**
2. **ALL code mergers MUST be 100% functionally equivalent** (unless told otherwise)
3. **ALL documentation MUST be updated BEFORE commits** (including visuals)
4. **Stage documentation WITH code in SAME commit**
5. **NO AI/Claude references in commits**
6. **MANDATORY: COT/ROT/TOT analysis for ALL code issues** (present options unless told "work independently")

### Task-Specific Instructions:

#### For Coding:
→ `/Agent-Instructions/CODING-GUIDE.md` - Code patterns and examples
→ `/Agent-Instructions/SESSION-LEARNINGS.md` - Recent fixes and gotchas
→ `/Agent-Instructions/AI-REVIEW-ABBREVIATIONS.md` - Review patterns (CRT, COT, ROT, TOT)

#### For Documentation:
→ `/Agent-Instructions/DOCUMENTATION-GUIDE.md` - How to write/update docs

#### For Understanding Context:
→ `/Agent-Instructions/SESSION-LEARNINGS.md` - What's been done recently
→ `/docs/Status/Current/` - Current project state
→ `/docs/TODO/VOS4-TODO-Master.md` - Active tasks

## 📋 Quick Command Reference

### Workflow Commands:
- **UD** = Update Documents
- **SCP** = Stage, Commit & Push (MANDATORY: Update docs first, stage by category)
- **SUF** = Stage, Update & Full workflow
- **PRECOMPACTION** = Create pre-compaction report (see `/Agent-Instructions/PRECOMPACTION-PROTOCOL.md`)

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
- ❌ NO "Claude", "Anthropic", "AI", "Cursor" mentions
- ❌ NO "Generated with" statements  
- ❌ NO "Co-Authored-By: Claude/Cursor"
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
├── .warp.md                    # Warp IDE instructions
├── .cursor.md                  # This file - Cursor IDE instructions
├── README.md                   # Project overview
├── Agent-Instructions/         # VOS4-specific AI instructions
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
1. Read this file (.cursor.md)
2. Check master instructions
3. Review VOS4-specific rules
4. Check current task priorities

### Before Committing
1. Run all tests
2. Update ALL documentation
3. Stage by category
4. No AI references in commits

---
**Last Updated:** 2025-08-31 - Updated with complete project context for Cursor IDE
**Note:** Master rules in `/Volumes/M Drive/Coding/Warp/Agent-Instructions/`