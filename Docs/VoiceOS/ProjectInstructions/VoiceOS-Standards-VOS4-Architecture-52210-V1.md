<!--
Filename: Standards-VOS4-Architecture.md
Created: 2025-10-15 02:49:32 PDT
Author: AI Documentation Agent
Purpose: VoiceOS 4 architecture standards and module organization (extracted from MASTER-STANDARDS.md)
Last Modified: 2025-10-15 02:49:32 PDT
Version: v1.0.0
Changelog:
- v1.0.0 (2025-10-15): Initial extraction from MASTER-STANDARDS.md - VOS4-specific architecture
-->

# VOS4 Architecture Standards - Project-Specific Requirements

## 🔴 CRITICAL: VOS4 Core Principles (ZERO TOLERANCE)

### 0. MANDATORY Multi-Agent Expertise Requirements
**ALL work MUST use specialized PhD-level agents for each domain:**
- **UI/UX Work:** PhD HCI expert with Material Design 3 & Compose knowledge
- **Architecture:** PhD Software Architecture expert with SOLID expertise
- **Audio/Speech:** PhD DSP expert with STT/TTS experience
- **Security:** PhD Cybersecurity expert with Android security knowledge
- **Database:** PhD Database Systems expert with ObjectBox expertise
- **ML/AI:** PhD Machine Learning expert with on-device inference knowledge
- **Research/Analysis:** Use agentic agents for multi-file searches

### 0.1. Check Master Inventories BEFORE Creating
**MANDATORY: Check for duplicates before creating ANYTHING**
```
BEFORE creating new file/class/function:
1. CHECK VOS4-Master-Inventory.md
2. CHECK [Module]-Master-Inventory.md
3. VERIFY no duplicates exist
4. CREATE only after verification
5. UPDATE inventories immediately
```

### 1. Direct Implementation Only
**NO INTERFACES, NO ABSTRACTIONS, ZERO OVERHEAD**

```kotlin
// ✅ CORRECT - Direct implementation
class CommandsManager(private val context: Context) {
    fun processCommand(text: String): Result { }
}

// ❌ WRONG - Interface abstraction
interface IModule { }
class CommandsManager : IModule { }
```

### 2. Namespace Convention
**MANDATORY: All modules use com.augmentalis.* pattern**
- ALL modules now use: `com.augmentalis.*`
- Pattern: `com.augmentalis.[modulename]`
- NO MORE `com.ai.*` - this is deprecated

```kotlin
// ✅ CORRECT - New standard
package com.augmentalis.commandsmanager
package com.augmentalis.speechrecognition
package com.augmentalis.voiceaccessibility
package com.augmentalis.datamanager
package com.augmentalis.voiceos  // Master app

// ❌ WRONG - Old patterns
package com.ai.commandsmanager  // DEPRECATED
package com.ai.anything  // NO LONGER VALID
package com.augmentalis.modules.commands  // No redundancy
```

### 3. Database Standard
**ObjectBox ONLY - No SQLite, No Room, No SharedPreferences for data**

```kotlin
// ✅ CORRECT - ObjectBox
@Entity
data class Command(@Id var id: Long = 0, var text: String)

// ❌ WRONG - SQLite/Room
@Entity(tableName = "commands")  // NO!
```

### 4. No Helper Methods
**Direct parameter access only**

```kotlin
// ✅ CORRECT - Direct access
val language = config?.language ?: "en-US"

// ❌ WRONG - Helper methods
fun getParameter(name: String) = parameters[name]  // NO!
```

### 5. Self-Contained Modules
**ALL components in same module - organized for efficiency**
- Services declared where implemented
- Resources in module that uses them
- Permissions in module that needs them
- Each module independently buildable
- Internal organization allowed if it improves performance
- Balance between flat and organized based on module size

## 🎯 VOS4 Performance Requirements (MANDATORY)

| Metric | Requirement | Reason |
|--------|------------|---------|
| Initialization | <1 second | User experience |
| Module load | <50ms per module | Responsiveness |
| Command recognition | <100ms latency | Real-time feel |
| Memory (Vosk) | <30MB | Device compatibility |
| Memory (Vivoka) | <60MB | Device compatibility |
| Battery drain | <2% per hour | All-day usage |
| XR rendering | 90-120 FPS | No motion sickness |

**CRITICAL**: NO TIMELINES OR ESTIMATES
- NEVER include "Week 1", "Day 1-2", "30 min", "2 hours" in plans
- NEVER promise completion dates or timeframes
- NEVER estimate effort or duration
- Focus on WHAT needs to be done, not WHEN

## ⚠️ VOS4-Specific Critical Pitfalls to Avoid

### NEVER DO:
1. **Don't create interfaces** - Direct implementation only
2. **Don't use wrong namespaces** - com.augmentalis.* pattern strictly
3. **Don't use SQLite/Room** - ObjectBox only for data
4. **Don't pipe gradle commands** - Causes "Task '2' not found" errors
5. **Don't add helper methods** - Direct access only
6. **Don't split module components** - Self-contained only

### ALWAYS DO:
1. **Use direct implementation** - Zero overhead
2. **Follow com.augmentalis.* namespace** - Consistency
3. **Use ObjectBox** - Performance
4. **Fix errors individually** - No batch scripts
5. **Keep modules self-contained** - Independence

## 📁 VOS4 Project Structure

```
/VOS4/
├── Agent-Instructions/     # These instructions (root level)
├── app/                   # Master app (com.augmentalis.voiceos)
├── apps/                  # Standalone application modules
│   ├── SpeechRecognition/ # 6 engines complete
│   ├── VoiceAccessibility/# Direct command execution
│   └── VoiceUI/          # Overlay system
├── managers/             # System managers
│   ├── CommandsManager/  # 70+ commands
│   └── DataManager/      # ObjectBox integration
├── libraries/            # Shared libraries
│   └── UUIDManager/      # UUID targeting
└── docs/                 # Documentation
```

## 📝 VOS4 Document Naming Convention

### MANDATORY Format for AI Notes & Tracking Documents
**Format:** `MODULENAME/APPNAME-WhatItIs-YYMMDD-HHMM.md`

**Examples:**
- `SPEECHRECOGNITION-MIGRATION-STATUS-250903-1430.md`
- `VOS4-BUILD-STATUS-250903-0430.md`
- `LEGACYAVENUE-INVENTORY-250903-0425.md`
- `SPEECHRECOGNITION-IMPLEMENTATION-GUIDE-250903-1615.md`
- `VOS4-ARCHITECTURE-DIAGRAM-250903-0930.md`

**Components:**
- **MODULENAME/APPNAME**: Module or application name (e.g., SPEECHRECOGNITION, VOS4, LEGACYAVENUE)
- **WhatItIs**: Brief description of what the file contains (e.g., MIGRATION-STATUS, BUILD-STATUS, INVENTORY)
- **YYMMDD**: Date in 6-digit format (year-month-day)
- **HHMM**: Time in 24-hour format (not 12-hour format)

**Apply to:**
- All documents in `/docs/ainotes/`
- Migration tracking documents
- Error logs and analysis reports
- Test results and reports
- Session-specific documentation
- Status reports and tracking documents
- Implementation guides
- Architecture diagrams
- Any temporary or session-specific files

**Document Header Template:**
```markdown
# [Document Title]
**File:** MODULENAME/APPNAME-WhatItIs-YYMMDD-HHMM.md
**Module/App:** [Module or application name]
**Created:** YYYY-MM-DD HH:MM (24-hour format)
**Purpose:** [Why this document exists]
```

## 🔧 VOS4 Testing Commands

```bash
# Build specific module (NO PIPES!)
./gradlew :apps:VoiceUI:assembleRelease
./gradlew :managers:CommandsManager:compileDebugKotlin

# Full build
./gradlew clean build

# Run tests
./gradlew test
```

## 🔄 Interface Exception Process (VOS4-Specific)

### When Interfaces May Be Justified:

While VOS4 follows a zero-overhead, direct implementation architecture, there are rare cases where interfaces may provide genuine value. Before implementing an interface:

#### 1. ANALYSIS REQUIRED:
- Identify the specific problem that requires an interface
- Document why direct implementation cannot solve it
- Calculate the actual overhead (memory, performance)
- List the benefits that outweigh the overhead

#### 2. VALID EXCEPTION CASES:
- **Plugin Architecture**: Multiple implementations that must be runtime-swappable
- **External API Contracts**: When interfacing with external systems that require it
- **Test Doubles**: For unit testing complex dependencies (mock/stub)
- **Future Extensibility**: When KNOWN future requirements will need polymorphism

#### 3. APPROVAL PROCESS:
1. **Document the Case**: Create a clear analysis showing:
   - Why an interface is necessary
   - Overhead costs (memory, performance)
   - Alternative solutions considered
   - Benefits that justify the overhead

2. **Present for Review**:
   - Share analysis with user/team
   - Explain the trade-offs
   - Wait for explicit approval

3. **Implementation**:
   - Mark interface with exception comment
   - Include approval date and approver
   - Keep interface minimal (only required methods)

#### 4. EXAMPLE EXCEPTION DOCUMENTATION:
```kotlin
/**
 * EXCEPTION TO VOS4 ZERO-OVERHEAD RULE:
 * This interface is maintained because [specific reason]
 *
 * Analysis: [Link to analysis document or summary]
 * Approved by: [User/Team Member]
 * Date: [YYYY-MM-DD]
 * Review Date: [When to re-evaluate if still needed]
 */
interface IExampleInterface {
    // Minimal required methods only
}
```

#### 5. ONGOING REVIEW:
- Exceptions reviewed quarterly
- If no longer needed, remove immediately
- Document the removal in changelog

---
**Remember:** The default is still DIRECT IMPLEMENTATION. Interfaces are the exception, not the rule.

## 📍 VOS4 Working Directory Rules

- **VOS4 folder:** `/Volumes/M Drive/Coding/Warp/VOS4` - READ/WRITE
- **vos3-dev folder:** READ-ONLY reference
- **vos2 folder:** READ-ONLY reference
- **Other VOS* folders:** READ-ONLY reference

**Branch:** VOS4 (STAY ON THIS BRANCH)

## 🔗 VOS4-Specific Architecture Decisions

### Why Direct Implementation?
- **Performance:** No virtual method calls
- **Clarity:** See exactly what happens
- **Debugging:** Simpler stack traces
- **Size:** Smaller APK (no interface definitions)

### Why ObjectBox Only?
- **Speed:** 10x faster than SQLite
- **Simplicity:** No SQL queries
- **Type safety:** Compile-time checks
- **Relations:** Automatic lazy loading

### Why com.augmentalis.* Namespace?
- **Consistency:** All modules use same pattern
- **Professional:** Company name-based
- **Clarity:** No ambiguity
- **No conflicts:** Unique namespace

---

**Note:** These are VOS4-SPECIFIC architecture standards. General development standards are in Standards-Development-Core.md.
