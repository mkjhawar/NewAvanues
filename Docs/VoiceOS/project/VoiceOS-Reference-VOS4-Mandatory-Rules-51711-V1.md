<!--
Filename: Reference-VOS4-Mandatory-Rules.md
Created: 2025-10-15 02:49:32 PDT
Author: AI Documentation Agent
Purpose: VoiceOS 4 specific mandatory rules and requirements (extracted from MANDATORY-RULES-SUMMARY.md)
Last Modified: 2025-10-15 02:49:32 PDT
Version: v1.0.0
Changelog:
- v1.0.0 (2025-10-15): Initial extraction from MANDATORY-RULES-SUMMARY.md - VOS4-specific rules
-->

# VOS4 Mandatory Rules - Project-Specific Requirements

## Purpose

This document defines VOS4-SPECIFIC mandatory rules that supplement the universal zero-tolerance policies. All universal policies apply, plus these VOS4-specific requirements.

## 🚨 VOS4 Core Standards (Non-Negotiable)

### Architecture Requirements

**Direct implementation only** - NO interfaces (except approved exceptions)
- Zero virtual method call overhead
- Direct compilation optimization
- Simpler debugging and stack traces
- Exception process documented in Standards-VOS4-Architecture.md

**Namespace:** `com.augmentalis.*` ONLY
- Pattern: `com.augmentalis.[modulename]`
- NO `com.ai.*` - deprecated
- Consistency across all modules
- Professional company-based naming

**Database:** ObjectBox ONLY (no SQLite/Room)
- 10x faster than SQLite
- Type-safe compile-time checks
- No SQL query overhead
- Native binding performance

**Self-contained modules** - All components in same module
- Services declared where implemented
- Resources in module that uses them
- Permissions in module that needs them
- Independent build capability

### Performance Requirements (Mandatory)

| Metric | Requirement | Rationale |
|--------|-------------|-----------|
| Initialization | <1 second | User experience - app must feel instant |
| Module load | <50ms per module | Responsiveness - no lag between actions |
| Command recognition | <100ms latency | Real-time feel - voice commands must be instant |
| Memory (Vosk) | <30MB | Device compatibility - works on low-end devices |
| Memory (Vivoka) | <60MB | Device compatibility - balance features/memory |
| Battery drain | <2% per hour | All-day usage - users expect voice control all day |
| XR rendering | 90-120 FPS | No motion sickness - critical for AR/VR |

**Measurement Required**:
- All performance claims MUST be measured
- No estimates or "should be faster" statements
- Document measurement methodology
- Include test conditions

### Documentation Requirements (VOS4-Specific)

**ALL docs must include diagrams** (Mermaid + ASCII)
- System architecture diagrams
- Component interaction diagrams
- Sequence diagrams for flows
- State machine diagrams

**Living documents** - Update with every change
- Changelogs updated BEFORE commit
- Architecture docs reflect current state
- API docs match implementation
- Status docs show actual progress

**Visual documentation** - Flowcharts, sequences, UI layouts
- ASCII art for text-based diagrams
- Mermaid for complex visualizations
- Screenshots for UI components
- Wireframes for planned features

**Feature matrices** - For all refactors/merges
- Before/after comparison
- 100% functionality verification
- Performance impact documented
- Benefits clearly stated

## 📁 VOS4-Specific Documentation Locations

### Master Documents (Update if affected):
- `/docs/voiceos-master/architecture/` - System architecture
- `/docs/voiceos-master/standards/` - Project standards
- `/docs/voiceos-master/project-management/` - PM documents
- `/docs/voiceos-master/status/` - Project status
- `/coding/TODO/VOS4-TODO-Master-YYMMDD-HHMM.md` - Active tasks

### Module Documents (ALWAYS update for changes):
- `/docs/modules/[module]/[Module]-Changelog.md` **MANDATORY**
- `/docs/modules/[module]/architecture/` - Module design
- `/docs/modules/[module]/implementation/` - Implementation details
- `/docs/modules/[module]/reference/api/` - API documentation
- `/docs/modules/[module]/developer-manual/` - Developer guide

### Active Development (Check FIRST):
- `/coding/TODO/` - Current tasks with timestamps
- `/coding/STATUS/` - Current state with timestamps
- `/coding/ISSUES/` - Active problems by priority
- `/coding/DECISIONS/` - Architecture decision records

## 🔍 VOS4-Specific Verification Commands

```bash
# Check VOS4 namespace compliance
grep -r "package com.ai" modules/ --include="*.kt"
# Should return NOTHING - all should be com.augmentalis

# Verify ObjectBox usage (not Room/SQLite)
grep -r "@Entity" modules/ --include="*.kt" | grep -v "io.objectbox"
# Should return NOTHING - all entities should use ObjectBox

# Check for interface violations
grep -r "interface I[A-Z]" modules/ --include="*.kt"
# Should return NOTHING (except approved exceptions)

# Verify documentation updates
ls -la docs/modules/*/changelog/ | grep "$(date +%Y-%m-%d)"
# Should show today's changelog updates if code changed
```

## 📝 VOS4 Commit Workflow

### Stage Files Command Workflow:

When told to "stage files" or "commit":

1. **FIRST**: Update/create ALL required documentation
   - Module changelogs (MANDATORY)
   - Architecture docs if structure changed
   - API docs if interfaces changed
   - Status and TODO updates

2. **SECOND**: Stage by category
   - Commit 1: Documentation files
   - Commit 2: Code files by module/app
   - Commit 3: Config/build files if any

3. **NEVER**: Mix categories in one commit
   - Exception: Small doc fix with code fix (use judgment)

### SCP Command (Stage, Commit & Push):

1. Update ALL documentation FIRST (see above)
2. Stage files by category (docs, then code)
3. Commit with clear messages (no tool refs)
4. Push to branch (usually vos4 or vos4-[feature])
5. **MANDATORY**: Docs must be updated BEFORE staging

### VOS4 Commit Message Format:

```
type(scope): Brief description

- Detail 1 (what changed)
- Detail 2 (why it changed)
- Impact/benefit (user-facing or performance)

Module: [ModuleName if applicable]
```

**Types**: feat, fix, refactor, docs, test, build, perf
**Scopes**: module names, component names, or "core"

## 🎯 VOS4-Specific File Organization Rules

### Code Location Rules:

**NEVER** place code in wrong location:
- ❌ `/docs/` - ONLY documentation
- ❌ `/coding/` - ONLY active development tracking
- ✅ `/modules/apps/` - Application modules
- ✅ `/modules/libraries/` - Library modules
- ✅ `/modules/managers/` - Manager modules

### Documentation Location Rules:

**NEVER** place docs in code folders:
- ❌ `/modules/[module]/README.md` - Move to `/docs/modules/[module]/`
- ❌ `/modules/[module]/CHANGELOG.md` - Move to `/docs/modules/[module]/changelog/`
- ✅ `/docs/modules/[module]/` - All module documentation

### Naming Convention Rules:

**Code files**: PascalCase.kt
```
✅ AccessibilityScrapingIntegration.kt
❌ accessibility_scraping_integration.kt
```

**Documentation files**: PascalCase-With-Hyphens-YYMMDD-HHMM.md
```
✅ Architecture-Refactor-Roadmap-251010-0157.md
❌ architecture-refactor-roadmap.md (missing timestamp)
```

**Package names**: lowercase.dot.separated
```
✅ com.augmentalis.voiceaccessibility
❌ com.augmentalis.VoiceAccessibility (no PascalCase in packages)
```

## 🚀 VOS4 Multi-Agent Requirements

### When to Deploy Multiple Agents (MANDATORY):

1. **Independent module work** - Different modules can be developed in parallel
2. **Documentation + Code** - One agent writes code, another updates docs
3. **Testing + Development** - Test completed features while developing next
4. **Analysis + Implementation** - Analyze architecture while implementing features
5. **Multi-file refactoring** - Coordinate changes across multiple files

### VOS4 Agent Specialization:

- **Architecture Agent**: PhD-level software architecture expertise
- **Speech Agent**: PhD-level DSP and speech recognition expertise
- **UI Agent**: PhD-level HCI and Material Design expertise
- **Database Agent**: PhD-level database systems expertise (ObjectBox focus)
- **Android Agent**: PhD-level Android platform expertise

### Agent Coordination:

- Clear task boundaries defined
- Shared status document updated
- No file conflicts (different modules)
- Regular synchronization points
- Combined final review

## 📋 VOS4 Working Directory Context

**Primary Location**: `/Volumes/M Drive/Coding/Warp/vos4`
**Branch**: Usually `vos4` or `vos4-[feature-name]`
**Reference Projects** (READ-ONLY):
- `/Volumes/M Drive/Coding/Warp/vos3-dev` - VOS3 reference
- `/Volumes/M Drive/Coding/Warp/vos2` - VOS2 reference
- `/Volumes/M Drive/Coding/Warp/avanue4` - Legacy Avenue reference

**Agent Instructions**:
- Primary: `/Volumes/M Drive/Coding/Warp/Agent-Instructions/`
- Local copy: `/Volumes/M Drive/Coding/Warp/vos4/Agent-Instructions/`
- **CRITICAL**: Synchronize both when updating

## 🎯 VOS4-Specific Gotchas

### Build System:
- **DON'T**: Pipe Gradle commands (`./gradlew build | grep error`)
- **DO**: Run directly (`./gradlew build`)
- **WHY**: Piping breaks Gradle task detection

### ObjectBox:
- **DON'T**: Use KSP (not supported)
- **DO**: Use KAPT (`kotlin-kapt` plugin)
- **WHY**: ObjectBox compiler requires KAPT

### Module Structure:
- **DON'T**: Create deep nested packages
- **DO**: Flatten when single file per folder
- **WHY**: Reduces navigation complexity

### Performance Claims:
- **DON'T**: State unverified metrics ("50% faster")
- **DO**: Measure and document ("Measured 47% faster in benchmark X")
- **WHY**: Credibility and accuracy

### Interfaces:
- **DON'T**: Create interfaces by default
- **DO**: Request exception approval if genuinely needed
- **WHY**: Zero-overhead architecture principle

## ⚡ VOS4 Quick Rules Reference

- **Architecture**: Direct implementation, com.augmentalis.*, ObjectBox only
- **Performance**: <1s init, <50ms module load, <100ms command recognition
- **Documentation**: Update BEFORE commit, diagrams required, 100% current
- **Commits**: No tool refs, stage by category, changelogs mandatory
- **Multi-agent**: Use for parallel work, coordinate via status docs
- **Verification**: Namespace check, ObjectBox check, interface check

## 🔗 Related Documentation

- **Universal Policies**: `/Volumes/M Drive/Coding/Docs/AgentInstructions/Reference-Zero-Tolerance-Policies.md`
- **Development Standards**: `/Volumes/M Drive/Coding/Docs/AgentInstructions/Standards-Development-Core.md`
- **VOS4 Architecture**: `/Volumes/M Drive/Coding/Warp/vos4/Docs/ProjectInstructions/Standards-VOS4-Architecture.md`
- **Session Learnings**: `/Volumes/M Drive/Coding/Warp/vos4/Docs/ProjectInstructions/Reference-VOS4-Session-Learnings.md`

---

**CRITICAL**: These VOS4-specific rules supplement universal policies. ALL universal zero-tolerance policies also apply to VOS4.

**REMEMBER**: VOS4 = Direct implementation + com.augmentalis namespace + ObjectBox + Self-contained modules + <1s performance.
