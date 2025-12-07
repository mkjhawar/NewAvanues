/**
 * MASTER-AI-INSTRUCTIONS.md - Central Entry Point for All AI Agents
 * Path: /Agent-Instructions/MASTER-AI-INSTRUCTIONS.md
 * 
 * Created: 2025-01-21
 * Last Modified: 2025-09-03
 * Author: VOS4 Development Team
 * Version: 1.3.0
 * 
 * Purpose: Primary instruction hub for AI assistants working on VOS4
 * 
 * Changelog:
 * - v1.3.0 (2025-09-03): Added MANDATORY specialized agents & parallel processing requirements
 * - v1.2.0 (2025-08-29): Added MANDATORY git staging rules for multi-agent development
 * - v1.1.0 (2025-08-21): Added critical modular architecture principles and self-containment rules
 * - v1.0.0 (2025-01-21): Initial creation with comprehensive instructions
 */

# MASTER AI INSTRUCTIONS FOR VOS4

## 🚨 CRITICAL: READ THIS FIRST
**All AI agents (Claude, GPT, Copilot, etc.) MUST read this document before ANY work on VOS4**

## 🕐 MANDATORY: Timestamp Requirements
**CRITICAL**: All AI agents MUST follow these timestamp rules WITHOUT EXCEPTION

### 🚨 MANDATORY Timestamp Rules:
1. **ALWAYS use LOCAL MACHINE TIME** for all timestamps (MANDATORY)
2. **NEVER use UTC or other timezones** unless explicitly requested  
3. **Format:** YYYY-MM-DD HH:MM:SS PST/PDT (or local timezone)
4. **For file backups:** Use format `$(date +%Y%m%d-%H%M%S)` for local time
5. **For documentation headers:** Include both date and time in local timezone
6. **VIOLATION = CRITICAL ERROR:** Wrong timezone usage must be corrected immediately

### Implementation Commands:
1. **Before creating ANY timestamped report:**
   ```bash
   date "+%Y-%m-%d %H:%M:%S %Z"
   ```

2. **For filenames with timestamps:**
   ```bash
   echo "REPORT-$(date +%Y%m%d-%H%M%S).md"
   ```

3. **For documentation headers:**
   ```bash
   echo "Last Updated: $(date '+%Y-%m-%d %H:%M:%S %Z')"
   ```

4. **NEVER use remote/server time - ALWAYS check local time first**

## Quick Start Checklist
Before ANY task, consult these documents in order:

### 1. 📋 For ANY Task Type:
```
ALWAYS START HERE:
1. Read THIS file (MASTER-AI-INSTRUCTIONS.md)
2. Read .warp.md (project root) - Master project instructions
3. Read CLAUDE.md (project root) - Current development status
```

### 2. 🔍 Task-Specific Documentation:

#### For CODING Tasks:
```
BEFORE writing/modifying code:
1. /Agent-Instructions/CODING-STANDARDS.md
2. /Agent-Instructions/NAMESPACE-CLARIFICATION.md
3. /Agent-Instructions/AI-REVIEW-ABBREVIATIONS.md (for review patterns)
4. /docs/Planning/Architecture/[Module]/[Module]-PRD.md
5. /docs/Planning/Architecture/[Module]/TODO.md
```

#### For DOCUMENTATION Tasks:
```
BEFORE creating/modifying docs:
1. /Agent-Instructions/DOCUMENT-STANDARDS.md
2. /docs/DOCUMENT-CONTROL-MASTER.md
3. /Agent-Instructions/FILE-STRUCTURE-GUIDE.md
```

#### For ANALYSIS Tasks:
```
BEFORE analyzing:
1. /docs/Status/Current/VOS4-Status-Comprehensive.md
2. /docs/Status/Analysis/[Relevant-Analysis].md
3. /docs/Planning/Architecture/VOS4-Architecture-Master.md
```

#### For PLANNING Tasks:
```
BEFORE planning:
1. /docs/Planning/VOS4-Planning-Master.md
2. /docs/TODO/VOS4-TODO-Master.md
3. /docs/Planning/Architecture/VOS4-Roadmap-Master.md
```

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

## Core Principles (MANDATORY)

### 1. Modular Architecture (CRITICAL)
**Each module MUST be completely self-contained with ALL related components:**

```
✅ CORRECT - Self-contained module:
/modules/apps/VoiceAccessibility/
├── AndroidManifest.xml          # Service declarations
├── src/main/java/com/ai/        # Implementation classes  
├── res/values/strings.xml       # Module-specific strings
├── res/xml/config.xml           # Module-specific configs
└── All related functionality

❌ WRONG - Split across modules:
- Service declared in main app
- Implementation in module
- Resources split between modules
- Cross-module dependencies
```

**Module Self-Containment Rules:**
- **Android Components**: Services, activities, receivers declared in same module as implementation
- **Resources**: Strings, XML configs, drawables in same module that uses them  
- **Permissions**: Required permissions declared in module that needs them
- **Dependencies**: Minimize cross-module dependencies
- **Testing**: Each module should be buildable and testable independently

### 2. Direct Implementation Pattern
```kotlin
// ✅ CORRECT - Direct implementation
class CommandsModule(context: Context) {
    fun processCommand(text: String): Result
}

// ❌ WRONG - Interface abstraction
interface IModule { }
class CommandsModule : IModule { }
```

### 3. Namespace Convention
```kotlin
// Master app ONLY
package com.augmentalis.voiceos

// ALL other modules
package com.ai  // ai = Augmentalis Inc (NOT artificial intelligence)
```

### 4. Zero Overhead Policy
- NO unnecessary interfaces
- NO adapter classes
- NO helper methods
- NO redundant abstractions
- Direct parameter access ONLY

### 5. Database Standard
```kotlin
// MANDATORY: ObjectBox for ALL data persistence
@Entity
data class Entity(@Id var id: Long = 0)

// NO SQLite, NO Room, NO SharedPreferences for data
```

## Document Locations Quick Reference

### 📁 Primary Instructions
| Document | Location | Purpose |
|----------|----------|---------|
| .warp.md | /VOS4/ | Master project instructions |
| CLAUDE.md | /VOS4/ | Current development status |
| DOCUMENT-CONTROL-MASTER.md | /docs/ | Documentation index and standards |

### 📁 AI Instructions
| Document | Location | Purpose |
|----------|----------|---------|
| MASTER-AI-INSTRUCTIONS.md | /Agent-Instructions/ | THIS FILE - Start here |
| CODING-STANDARDS.md | /Agent-Instructions/ | Code style and patterns |
| DOCUMENT-STANDARDS.md | /Agent-Instructions/ | Documentation format |
| NAMESPACE-CLARIFICATION.md | /Agent-Instructions/ | Package naming rules |
| FILE-STRUCTURE-GUIDE.md | /Agent-Instructions/ | Project structure guide |

### 📁 Architecture & Planning
| Document | Location | Purpose |
|----------|----------|---------|
| VOS4-Architecture-Master.md | /docs/Planning/Architecture/ | System architecture |
| VOS4-PRD-Master.md | /docs/Planning/Architecture/ | Product requirements |
| VOS4-Roadmap-Master.md | /docs/Planning/Architecture/ | Development roadmap |
| VOS4-Implementation-Master.md | /docs/Planning/Architecture/ | Implementation guide |

### 📁 Status & Analysis
| Document | Location | Purpose |
|----------|----------|---------|
| VOS4-Status-Comprehensive.md | /docs/Status/Current/ | Current system status |
| VOS4-Analysis-Critical.md | /docs/Status/Analysis/ | Critical analysis |
| VOS4-Analysis-PerformanceOverhead.md | /docs/Status/Analysis/ | Performance metrics |

### 📁 Module Documentation
| Module | Location |
|--------|----------|
| Apps | /docs/Planning/Architecture/Apps/[AppName]/ |
| Managers | /docs/Planning/Architecture/Managers/[ManagerName]/ |
| Libraries | /docs/Planning/Architecture/Libraries/[LibraryName]/ |

## Task-Specific Workflows

### 🔧 Before Writing Code:
1. Check module's TODO.md file
2. Review module's PRD.md
3. Read CODING-STANDARDS.md
4. Verify namespace rules in NAMESPACE-CLARIFICATION.md
5. Check existing implementation patterns

### 📝 Before Creating Documentation:
1. Read DOCUMENT-STANDARDS.md
2. Check DOCUMENT-CONTROL-MASTER.md for naming
3. Use correct header template
4. Update DOCUMENT-CONTROL-MASTER.md after creation

### 🔍 Before Analysis:
1. Review current status documents
2. Check existing analysis documents
3. Follow analysis format from existing docs
4. Update relevant TODO files

### 📅 Before Planning:
1. Review VOS4-Planning-Master.md
2. Check current sprint in TODO folder
3. Review roadmap and timeline
4. Update planning documents

## Common Pitfalls to Avoid

### ❌ DON'T:
1. Create interfaces unless absolutely necessary
2. Use wrong namespaces (must be com.ai.*)
3. Create adapter or helper classes
4. Use SQLite/Room (ObjectBox only)
5. Skip reading the documentation
6. Create redundant folders
7. Reference VOS3 for new development
8. Pipe gradle commands (causes errors)

### ✅ DO:
1. Use direct implementation pattern
2. Follow com.ai.* namespace strictly
3. Access parameters directly
4. Use ObjectBox for all data
5. Read relevant docs before starting
6. Follow existing patterns
7. Work in VOS4 folder only
8. Fix compilation errors individually

## Critical Requirements

### MANDATORY Git Staging Rules (MULTI-AGENT ENVIRONMENT)
⚠️ **CRITICAL**: Multiple agents work on the same repository simultaneously
- **ONLY stage files you have personally worked on, modified, or created**
- **Use `git add <specific-file-path>` for each file individually**
- **NEVER use `git add .` or `git add -A` as it will stage other agents' work**
- **Always verify with `git status` before committing**
- **If you accidentally stage wrong files, use `git reset <file>` to unstage**

### User Approval Required For:
- Architectural changes
- File moves or renames
- Package restructuring
- Major refactoring
- Interface additions
- Database changes

### Always Include in Code:
```kotlin
/**
 * FileName.kt - Brief description
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: YYYY-MM-DD
 */
```

## Performance Targets
- Initialization: <1 second
- Module load: <50ms per module
- Command recognition: <100ms
- Memory: <30MB (Vosk) or <60MB (Vivoka)
- Battery: <2% per hour active

## Working Directories
- **Primary**: `/Volumes/M Drive/Coding/Warp/VOS4`
- **Legacy (READ-ONLY)**: `/vos3-dev`, `/vos2`
- **Branch**: vos3-development

## Document Update Process
When you modify ANY document:
1. Update version number
2. Add changelog entry
3. Update Last Modified date
4. Update DOCUMENT-CONTROL-MASTER.md if needed

## Quick Commands
```bash
# Navigate to project
cd "/Volumes/M Drive/Coding/Warp/VOS4"

# Build commands (NO PIPES!)
./gradlew build
./gradlew :apps:SpeechRecognition:build
./gradlew :managers:CommandsMGR:build

# Git workflow (CRITICAL - MULTI-AGENT ENVIRONMENT)
# ⚠️ MANDATORY: Only stage files YOU have worked on
git status  # Always check what's changed first
git add <specific-file-path>  # Add ONLY your files one by one
# NEVER use: git add . or git add -A (will stage other agents' work!)
git commit -m "feat: Description"
git push
```

## AI Command Abbreviations
### Workflow Commands:
- **UD** = Update Documents
- **SCP** = Stage, Commit & Push (created files only)
- **SUF** = Stage, Update & Full workflow

### Review Patterns:
- **COT** = Chain of Thought (linear analysis)
- **ROT** = Reflection (evaluation)
- **TOT** = Train of Thought (alternatives)
- **CRT** = Combined Review (full analysis with options)

See [AI-REVIEW-ABBREVIATIONS.md](./AI-REVIEW-ABBREVIATIONS.md) for details

## Emergency References
If confused about:
- **Architecture**: Read VOS4-Architecture-Master.md
- **Current Status**: Read VOS4-Status-Comprehensive.md
- **What to do**: Read VOS4-TODO-Master.md
- **Naming**: Read NAMESPACE-CLARIFICATION.md
- **Documentation**: Read DOCUMENT-CONTROL-MASTER.md

## Remember
- **ai** = Augmentalis Inc (NOT artificial intelligence)
- **Direct implementation** = No unnecessary abstractions
- **ObjectBox only** = No other database solutions
- **Read first** = Documentation before action
- **User approval** = Required for major changes

---

## FINAL INSTRUCTION
**ALWAYS consult the relevant documentation BEFORE taking any action.**
**When in doubt, ask the user for clarification.**

---

*This is the master instruction file. All other instructions supplement this document.*
*Last Updated: 2025-09-03*
*Version: 1.3.0 - Added mandatory specialized agents & parallel processing requirements*
