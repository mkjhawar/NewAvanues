<!--
filename: AI-INSTRUCTIONS-SEQUENCE.md
created: 2025-08-30
author: VOS4 Development Team
purpose: Properly sequenced reading order for AI agents
location: /Agent-Instructions/
priority: READ THIS FIRST
-->

# AI Instructions - Proper Reading Sequence

## 🚨 CRITICAL: Follow This Exact Reading Order

This document provides the optimal sequence for reading VOS4 instructions to ensure proper understanding and context.

## 📚 Reading Sequence for New AI Agents

### Phase 1: Core Understanding (MANDATORY - Read First)
**Read these in exact order:**

1. **MASTER-AI-INSTRUCTIONS.md** 
   - Central entry point
   - Overview of all systems
   - Quick reference guide

2. **MASTER-STANDARDS.md**
   - Core principles (ZERO TOLERANCE rules)
   - Functional equivalency requirements
   - Performance standards
   - Multi-agent requirements

3. **CURRENT-TASK-PRIORITY.md**
   - Current sprint tasks
   - Recent completions
   - Immediate priorities

4. **MIGRATION-STATUS-2025-01-23.md**
   - Namespace migration status
   - Module completion tracking

### Phase 2: Development Standards (Before ANY Code Work)
**Read these before writing/modifying code:**

5. **NAMESPACE-CLARIFICATION.md**
   - CRITICAL: com.augmentalis.* namespace rules
   - Package naming conventions
   - Migration from com.ai

6. **CODING-GUIDE.md** (formerly CODING-STANDARDS.md)
   - Direct implementation patterns
   - No interfaces rule
   - Code style guidelines
   - COT/ROT/TOT analysis requirements

7. **AI-REVIEW-ABBREVIATIONS.md**
   - COT, ROT, TOT, CRT patterns
   - When to use each review type
   - Analysis requirements

### Phase 3: Documentation Standards (Before Creating Docs)
**Read these before creating/modifying documentation:**

8. **DOCUMENTATION-GUIDE.md** (formerly DOCUMENT-STANDARDS.md)
   - Documentation format standards
   - Header templates
   - Visual requirements (diagrams)

9. **DOCUMENTATION-CHECKLIST.md**
   - Pre-commit mandatory checklist
   - What to update before commits

10. **FILE-STRUCTURE-GUIDE.md**
    - Project organization
    - Module structure
    - File locations

### Phase 4: Specialized Topics (As Needed)

#### For Multi-Agent Work:
11. **MULTI-AGENT-REQUIREMENTS.md**
    - PhD-level expertise requirements
    - Specialized agent domains

12. **AGENTIC-AGENT-INSTRUCTIONS.md**
    - When to deploy agentic agents
    - Multi-file search patterns

#### For Session Learning:
13. **SESSION-LEARNINGS.md**
    - Recent fixes and solutions
    - Common gotchas
    - Lessons learned

#### For Specific Workflows:
14. **PRECOMPACTION-PROTOCOL.md**
    - Pre-compaction reporting
    - Archive procedures

15. **CODE_INDEX_SYSTEM.md**
    - Master inventory system
    - Component tracking

#### Legacy Reference (Read-Only):
16. **VOS3-DESIGN-SYSTEM.md**
    - visionOS/iOS design patterns
    - Reference only - not for VOS4

17. **VOS3-PROJECT-SPECIFIC.md**
    - VOS3 specific requirements
    - Reference only - not for VOS4

## 🎯 Quick Reference by Task Type

### "I need to write code"
Read in order:
1. MASTER-STANDARDS.md
2. NAMESPACE-CLARIFICATION.md
3. CODING-GUIDE.md
4. AI-REVIEW-ABBREVIATIONS.md
5. Module-specific documentation in /docs/

### "I need to fix a bug"
Read in order:
1. MASTER-STANDARDS.md (Section: COT/ROT/TOT Analysis)
2. AI-REVIEW-ABBREVIATIONS.md
3. SESSION-LEARNINGS.md
4. CODING-GUIDE.md

### "I need to create documentation"
Read in order:
1. DOCUMENTATION-GUIDE.md
2. DOCUMENTATION-CHECKLIST.md
3. FILE-STRUCTURE-GUIDE.md

### "I need to understand the architecture"
Read in order:
1. MASTER-AI-INSTRUCTIONS.md
2. /docs/Planning/Architecture/VOS4-Architecture-Master.md
3. FILE-STRUCTURE-GUIDE.md

### "I need to commit changes"
Read in order:
1. DOCUMENTATION-CHECKLIST.md
2. MASTER-STANDARDS.md (Section: Commit Procedures)

## 📍 Key Locations

### Primary Instruction Directory:
`/Agent-Instructions/` - All active AI instructions

### Deprecated/Archived:
`/docs/AI-Instructions/` - DO NOT USE (duplicates removed)

### Main Project Files:
- `/claude.md` - Project context
- `/.warp.md` - Master project instructions

### Documentation:
- `/docs/` - All project documentation
- `/docs/modules/` - Module-specific docs
- `/docs/Planning/` - Architecture and planning
- `/docs/Status/` - Current status reports

## 🚨 Critical Rules Summary

### ZERO TOLERANCE:
1. **Namespace:** com.augmentalis.* ONLY
2. **No interfaces** - Direct implementation
3. **ObjectBox only** - No SQLite/Room
4. **100% functional equivalency** in refactoring
5. **Never delete** without explicit approval
6. **Update docs BEFORE commits**
7. **COT/ROT/TOT analysis** for all issues

### Performance Targets:
- Initialization: <1 second
- Module load: <50ms
- Command recognition: <100ms
- Memory: <30MB (Vosk) / <60MB (Vivoka)
- Battery: <2% per hour

## 🔄 Update History

- 2025-08-30: Created comprehensive reading sequence
- Consolidated from duplicate /docs/AI-Instructions/
- Organized by task type and priority

---

**REMEMBER:** Always start with MASTER-AI-INSTRUCTIONS.md and MASTER-STANDARDS.md before any work!