<!--
filename: VOS4-App-Documentation-Context-251019-0104.md
created: 2025-10-19 01:04:56 PDT
author: AI Documentation Agent
copyright: Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
purpose: VOS4-specific documentation context (references universal guide)
last-modified: 2025-10-19 01:04:56 PDT
version: 2.0.0
-->

# VOS4 App Documentation Context

**Project:** VoiceOS (VOS4)
**Last Updated:** 2025-10-19 01:04:56 PDT
**Purpose:** VOS4-specific context for AI agents working on documentation
**CLAUDE.md Version:** 2.1.0 (IDEADEV methodology integrated)

**⚠️ IMPORTANT:** This document is VOS4-SPECIFIC ONLY. For universal documentation standards, IDEADEV methodology, and general best practices, see:

→ **Universal Guide:** `/Volumes/M Drive/Coding/Docs/agents/instructions/Guide-App-Documentation-Context.md`

This document contains ONLY VOS4-specific information:
- VOS4 project overview
- 8 VOS4 subagents
- 19 VOS4 modules
- VOS4 current status
- VOS4-specific file locations

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [VOS4 Project Overview](#vos4-project-overview)
3. [VOS4 Subagent Architecture](#vos4-subagent-architecture)
4. [VOS4 Module Structure](#vos4-module-structure)
5. [Current Project Status](#current-project-status)
6. [VOS4 Key Reference Documents](#vos4-key-reference-documents)
7. [VOS4-Specific File Locations](#vos4-specific-file-locations)

---

## Quick Start

### Mandatory First Steps (VOS4-Specific)

```bash
# 1. Get local time (MANDATORY)
date "+%Y-%m-%d %H:%M:%S %Z"  # For headers
date "+%y%m%d-%H%M"            # For filenames

# 2. Check current VOS4 context
cd /Volumes/M Drive/Coding/vos4
git status
git log --oneline -5

# 3. Load VOS4 standards
# Read: /vos4/CLAUDE.md (v2.1.0 - VOS4 quick reference)
# Read: Universal Guide (/Coding/Docs/agents/instructions/Guide-App-Documentation-Context.md)
# Read: This file (VOS4-specific context)
```

### VOS4 Quick References

| Need | Location |
|------|----------|
| **Universal Documentation Guide** | `/Coding/Docs/agents/instructions/Guide-App-Documentation-Context.md` |
| **VOS4 Quick Reference** | `/vos4/CLAUDE.md` (v2.1.0) |
| **VOS4 IDEADEV Guide** | `/vos4/ideadev/README.md` |
| **VOS4 Phased Review** | `/docs/Active/VOS4-Comprehensive-Phased-Review-251019-0024.md` |
| **VOS4 Work Summary** | `/docs/Active/Work-Summary-Oct17-19-2025-251019-0034.md` |
| **VOS4 Module Docs** | `/docs/modules/[ModuleName]/` |
| **VOS4 Active Work** | `/docs/Active/` |

---

## VOS4 Project Overview

### What is VOS4?

**VOS4 (VoiceOS)** is a comprehensive voice-controlled Android accessibility service that enables hands-free device operation through:

- **Voice commands** for any UI element
- **Cursor control** via voice/gestures
- **Automatic app learning** and command generation
- **Context-aware** intelligent assistance

### VOS4 Key Statistics

| Metric | Value |
|--------|-------|
| **Total Modules** | 19 (5 apps, 9 libraries, 5 managers) |
| **Codebase Size** | 855 Kotlin files |
| **Database** | Room with KSP (12 databases) |
| **Namespace** | `com.augmentalis.*` |
| **Branch** | voiceosservice-refactor |
| **Test Coverage** | 100% (47/47 tests passing) |

### VOS4 Architecture Tiers

```
VOS4 Architecture
├── Apps (5)        - VoiceOSCore, LearnApp, VoiceCursor, VoiceRecognition, VoiceUI
├── Libraries (9)   - SpeechRecognition, DeviceManager, UUIDCreator, etc.
└── Managers (5)    - CommandManager, VoiceDataManager, HUDManager, etc.
```

### Current Phase

**Phase 3: User Interaction Tracking** (IN PROGRESS)
- ✅ Phase 3.1: Database layer (UserInteraction table)
- ✅ Phase 3.2: Accessibility tracking (event capture)
- ✅ Phase 3.3: State-aware commands (preference learning)
- ✅ Phase 3.4: Privacy & battery optimization
- 📋 Phase 3.5: ML model training (FUTURE)

---

## VOS4 Subagent Architecture

### 8 VOS4-Specific Specialized Agents

Located in `/.claude/agents/`:

1. **@vos4-orchestrator** - Master router, enforces IDE Loop for VOS4
2. **@vos4-android-expert** - Android AccessibilityService platform expertise
3. **@vos4-kotlin-expert** - Kotlin coroutines/Flow/KSP expertise
4. **@vos4-database-expert** - Room with KSP database expertise
5. **@vos4-test-specialist** ⭐ - PROACTIVE testing (auto-invoked, BLOCKS failures)
6. **@vos4-architecture-reviewer** - VOS4 design review (performance-first, direct implementation)
7. **@vos4-documentation-specialist** ⭐ - PROACTIVE VOS4 docs (auto-invoked)
8. **@vos4-performance-analyzer** - VOS4 performance optimization

### VOS4 Proactive Agents

**These agents are automatically invoked in VOS4 (you don't need to ask):**

- **@vos4-test-specialist** - Automatically creates tests during Defend phase
- **@vos4-documentation-specialist** - Automatically updates VOS4 documentation after changes

### VOS4 Subagent Workflow Example

```
User: "@vos4-orchestrator Implement DatabaseManagerImpl TODO #3"
    ↓
Orchestrator routes to @vos4-database-expert
    ↓
@vos4-database-expert implements feature (VOS4 coding standards)
    ↓
@vos4-test-specialist AUTOMATICALLY creates tests (MANDATORY)
    ↓
Tests MUST pass (quality gate: APPROVE or BLOCK)
    ↓
@vos4-documentation-specialist AUTOMATICALLY updates VOS4 docs
    ↓
Orchestrator verifies phase complete
    ↓
Ready for commit (VOS4 commit protocol)
```

### VOS4 3-Tier Approach

**See Universal Guide** for IDEADEV methodology details. VOS4-specific usage:

**Tier 1: Direct (VOS4 Simple Tasks)**
- **Example:** "Fix null pointer in VoiceCommandProcessor.kt line 42"
- **VOS4 Rule:** Use VOS4 coding standards (no interfaces unless strategic)

**Tier 2: Subagents (VOS4 Medium Complexity) ⭐ RECOMMENDED**
- **Example:** "@vos4-orchestrator Implement DatabaseManagerImpl TODO #3"
- **VOS4 Enforcement:**
  - Testing by @vos4-test-specialist (BLOCKS if tests fail)
  - Documentation by @vos4-documentation-specialist (VOS4 doc standards)
  - VOS4 architecture principles enforced

**Tier 3: Full IDEADEV (VOS4 Complex Features)**
- **Example:** Phase 3 User Interaction Tracking (documented in `/ideadev/`)
- **VOS4 IDEADEV Location:** `/vos4/ideadev/`

---

## VOS4 Module Structure

### VOS4 Module List (19 Total)

**Apps (5):**
1. **VoiceOSCore** - Main accessibility service (VoiceOSService)
2. **LearnApp** - Deep app exploration and learning
3. **VoiceCursor** - Voice-controlled cursor system
4. **VoiceRecognition** - Speech recognition engine
5. **VoiceUI** - UI components and overlays

**Libraries (9):**
6. **SpeechRecognition** - Speech recognition core library
7. **DeviceManager** - Device information and management
8. **UUIDCreator** - UUID generation utilities
9. **VoiceKeyboard** - Voice input method editor (IME)
10. **VoiceOsLogger** - Logging system
11. **VoiceUIElements** - Reusable UI component library
12. **Translation** - Multi-language support
13. **MagicElements** - UI element manipulation utilities
14. **MagicUI** - UI framework extensions

**Managers (5):**
15. **CommandManager** - Voice command routing and execution
16. **VoiceDataManager** - Data persistence and synchronization
17. **HUDManager** - Heads-up display management
18. **LocalizationManager** - Localization and i18n
19. **LicenseManager** - License validation and management

### VOS4 Code-to-Documentation Mapping

**CRITICAL:** Code module names and doc folder names MUST be IDENTICAL (PascalCase):

```
modules/apps/VoiceOSCore/      → docs/modules/VoiceOSCore/
modules/apps/LearnApp/         → docs/modules/LearnApp/
modules/managers/CommandManager/ → docs/modules/CommandManager/
modules/libraries/UUIDCreator/ → docs/modules/UUIDCreator/
```

### VOS4 Standard Module Documentation

Each VOS4 module in `/docs/modules/[ModuleName]/` should have:

```
/docs/modules/VoiceOSCore/
├── changelog/              # What changed, when, why
│   └── changelog-2025-10-251019-0104.md
├── architecture/           # How it's designed
│   ├── Architecture-Overview.md
│   └── diagrams/          # Mermaid/ASCII diagrams
├── api/                   # Public API documentation
│   └── API-Reference.md
├── testing/               # Test documentation
│   └── Test-Strategy.md
├── developer-manual/      # How to develop
└── user-manual/           # How to use (if applicable)
```

---

## Current Project Status

### VOS4 Phase Completion

- ✅ **Phase 1:** Accessibility layer infrastructure (COMPLETE)
- ✅ **Phase 2:** UI scraping and command generation (COMPLETE)
- 🔄 **Phase 3:** User interaction tracking (IN PROGRESS - 80% complete)
- 📋 **Phase 4:** Advanced voice recognition (PLANNED)
- 📋 **Phase 5:** XR/AR integration (FUTURE)

### VOS4 Recent Work (Oct 17-19, 2025)

**Oct 17:** SOLID refactoring (Phases 1-7), 47 unit tests fixed
**Oct 18:** AI context inference, Phase 3 implementation, UUID integration
**Oct 19:** IDEADEV documentation, comprehensive phased review

**Commits:** 46 commits over 3 days
**Lines Changed:** +64,216 lines added, -21,974 lines deleted

### VOS4 Outstanding Issues

**Critical:**
- ❌ Issue #3: VoiceCursor IMU conflict (4-5 hours to fix)
- ⚠️ DatabaseManagerImpl TODOs (9 remaining, 10-12 hours)

**Completed:**
- ✅ Issue #1: UUID integration (FIXED in Oct 18)
- ✅ All 47 unit tests (FIXED in Oct 17)

### VOS4 Current Branch Status

**Branch:** voiceosservice-refactor
**Status:** Up-to-date with remote
**Commits ahead:** 0 (all pushed)
**Last commit:** docs(vos4): Add IDEADEV methodology and comprehensive project documentation

---

## VOS4 Key Reference Documents

### VOS4 Must-Read Documents

1. **CLAUDE.md (v2.1.0)** - VOS4 quick reference with IDEADEV
   - Location: `/vos4/CLAUDE.md`
   - Updated: 2025-10-19
   - Contains: VOS4 3-tier approach, VOS4 subagent architecture, VOS4 naming conventions

2. **VOS4 IDEADEV README** - VOS4-specific IDEADEV workflow
   - Location: `/vos4/ideadev/README.md`
   - Contains: VOS4 SP(IDE)R protocol, VOS4 examples, VOS4 templates

3. **VOS4 Comprehensive Phased Review** - Complete VOS4 project overview
   - Location: `/docs/Active/VOS4-Comprehensive-Phased-Review-251019-0024.md`
   - Size: 1,270 lines
   - Contains: VOS4 module breakdown, features, use cases, TODO analysis

4. **VOS4 Work Summary Oct 17-19** - Recent VOS4 development history
   - Location: `/docs/Active/Work-Summary-Oct17-19-2025-251019-0034.md`
   - Size: 811 lines
   - Contains: VOS4 3-day detailed breakdown, 46 commits, achievements

### VOS4 IDEADEV Documentation

**Phase 3 IDEADEV Documents (Example):**
- Spec: `/ideadev/specs/0001-phase3-interaction-tracking.md` (205 lines)
- Plan: `/ideadev/plans/0001-phase3-interaction-tracking.md` (322 lines)
- Review: `/ideadev/reviews/0001-phase3-interaction-tracking.md` (358 lines)

### VOS4 Module Documentation Examples

**VoiceOSCore:**
- `/docs/modules/VoiceOSCore/changelog/changelog-2025-10-251019-0020.md`
- `/docs/modules/VoiceOSCore/Phase3-Integration-Complete-251019-0020.md`

**LearnApp:**
- `/docs/Active/LearnApp-And-Scraping-Systems-Complete-Analysis-251017-0606.md`

---

## VOS4-Specific File Locations

### VOS4 Documentation Hierarchy

```
/vos4/docs/
├── Active/                    # Current VOS4 work (timestamped files)
├── Archive/                   # Completed/deprecated VOS4 work
├── modules/                   # VOS4 module-specific docs (19 modules)
│   ├── VoiceOSCore/          # Main app
│   ├── LearnApp/             # Learning app
│   ├── CommandManager/       # Command manager
│   └── ... (16 more modules)
├── planning/                  # VOS4 planning & architecture
├── visuals/                   # VOS4 diagrams
└── ProjectInstructions/       # VOS4-specific protocols
```

### VOS4 IDEADEV Location

```
/vos4/ideadev/
├── specs/                  # VOS4 WHAT to build
│   ├── 0001-phase3-interaction-tracking.md
│   └── templates/spec-template.md
│
├── plans/                  # VOS4 HOW to build
│   ├── 0001-phase3-interaction-tracking.md
│   └── templates/plan-template.md
│
├── reviews/                # VOS4 Lessons learned
│   ├── 0001-phase3-interaction-tracking.md
│   └── templates/review-template.md
│
└── README.md              # VOS4 IDEADEV guide
```

### VOS4 File Location Quick Reference

| VOS4 Document Type | Location | Example |
|-------------------|----------|---------|
| **VOS4 Active Work** | `/docs/Active/` | `VOS4-Status-Topic-251019-0104.md` |
| **VOS4 Module Docs** | `/docs/modules/[Name]/` | `VoiceOSCore/changelog/` |
| **VOS4 IDEADEV Specs** | `/ideadev/specs/` | `0001-phase3-tracking.md` |
| **VOS4 IDEADEV Plans** | `/ideadev/plans/` | `0001-phase3-tracking.md` |
| **VOS4 IDEADEV Reviews** | `/ideadev/reviews/` | `0001-phase3-tracking.md` |
| **VOS4 Architecture** | `/docs/modules/[Name]/architecture/` | `Architecture-Overview.md` |
| **VOS4 Diagrams** | `/docs/modules/[Name]/architecture/diagrams/` | `system-flow.mmd` |

### VOS4 Subagent Quick Reference

| VOS4 Subagent | When to Use | Auto-Invoked? |
|---------------|-------------|---------------|
| **@vos4-orchestrator** | Route VOS4 work to specialists | No (manual) |
| **@vos4-android-expert** | VOS4 Android platform work | No (via orchestrator) |
| **@vos4-kotlin-expert** | VOS4 Kotlin/coroutines work | No (via orchestrator) |
| **@vos4-database-expert** | VOS4 database work | No (via orchestrator) |
| **@vos4-test-specialist** | VOS4 testing (Defend phase) | **YES (automatic)** |
| **@vos4-architecture-reviewer** | VOS4 design review | No (manual) |
| **@vos4-documentation-specialist** | VOS4 documentation | **YES (automatic)** |
| **@vos4-performance-analyzer** | VOS4 performance work | No (manual) |

---

## VOS4-Specific Best Practices

### VOS4 Architecture Principles

1. **Direct implementation** - Zero interfaces (unless strategic value)
2. **ObjectBox for persistence** - Room for future (currently migrating)
3. **`com.augmentalis.*` namespace** - NOT com.ai
4. **Module self-containment** - Minimal dependencies
5. **Performance-first design** - <100ms command recognition, <1s init

### VOS4 Naming Conventions

**See:** `/docs/voiceos-master/standards/NAMING-CONVENTIONS.md`

| VOS4 Type | Convention | Example |
|-----------|-----------|---------|
| Kotlin/Java | `PascalCase.kt` | `VoiceCommandProcessor.kt` |
| VOS4 Documentation | `PascalCase-With-Hyphens-YYMMDD-HHMM.md` | `VOS4-Status-Current-251019-0104.md` |
| VOS4 Code Modules | `PascalCase/` | `VoiceOSCore/` |
| VOS4 Doc Folders | `PascalCase/` (same as code) | `VoiceOSCore/` |
| VOS4 Packages | `lowercase.dot.separated` | `com.augmentalis.voiceoscore` |

### VOS4 Commit Protocol

**See:** `/vos4/Docs/ProjectInstructions/Protocol-VOS4-Commit.md`

1. **Documentation updated FIRST** (before code commit)
2. **Stage by category** (docs → code → tests)
3. **NO AI/tool references** in commit messages
4. **Verify functional equivalency** (VOS4 behavior unchanged)
5. **Use conventional commits** (feat:, fix:, docs:, refactor:)

---

## Conclusion

This VOS4-specific context document provides everything needed for VOS4 documentation work:

- ✅ VOS4 project overview (19 modules, 855 files)
- ✅ VOS4 subagent architecture (8 specialists)
- ✅ VOS4 module structure and locations
- ✅ VOS4 current status (Phase 3, Oct 17-19 work)
- ✅ VOS4 key reference documents
- ✅ VOS4-specific file locations and conventions

**For universal documentation standards, IDEADEV methodology, and general best practices**, see:
→ `/Volumes/M Drive/Coding/Docs/agents/instructions/Guide-App-Documentation-Context.md`

**Use BOTH documents together:**
1. **Universal Guide** - IDEADEV methodology, documentation standards, patterns
2. **This Document** - VOS4-specific modules, subagents, locations, current status

---

**Document Status:** COMPLETE ✅
**Version:** 2.0.0 (VOS4-specific only, references universal guide)
**Previous Version:** 1.0.0 (contained duplicated universal content)
**Changelog:**
- v2.0.0 (2025-10-19 01:04): Split to VOS4-specific only, added universal guide reference
- v1.0.0 (2025-10-19 00:45): Initial version (contained universal + VOS4 content)

**Next Review:** 2025-11-19 (monthly review)
**Maintained By:** AI Documentation Agent
**Contact:** Manoj Jhawar (maintainer)
