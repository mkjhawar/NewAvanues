# Pre-Compaction Protocol - Generic Template
**Purpose:** Standardized protocol for creating pre-compaction reports for any coding task
**Trigger:** User requests "precompaction" or "create precompaction report"
**Version:** 1.0.0
**Created:** 2025-01-28

## 🚨 WHEN TO CREATE A PRE-COMPACTION REPORT

Create a pre-compaction report when:
1. User explicitly requests "precompaction" or similar
2. Working on a multi-step implementation (>3 steps)
3. Complex debugging or refactoring in progress
4. Before any expected context reset/compaction
5. At significant milestones in long tasks

## 📝 FILE NAMING CONVENTION

### MANDATORY Format
**Format:** `MODULENAME/APPNAME-PRECOMPACTION-REPORT-YYMMDD-HHMM.md`

**Components:**
- **MODULENAME/APPNAME**: Module or application name (e.g., SPEECHRECOGNITION, VOS4, LEGACYAVENUE)
- **PRECOMPACTION-REPORT**: Fixed identifier for precompaction documents
- **YYMMDD**: Date in 6-digit format (year-month-day)
- **HHMM**: Time in 24-hour format (not 12-hour format)

### Primary Location:
```
/docs/Precompaction-Reports/MODULENAME-PRECOMPACTION-REPORT-YYMMDD-HHMM.md
```

### Directory Structure:
```
/docs/
├── Precompaction-Reports/     # All precompaction reports go here
│   ├── MODULENAME-PRECOMPACTION-REPORT-YYMMDD-HHMM.md
│   ├── Archive/               # Older reports that are no longer active
│   └── README.md              # Index of all reports
```

### Examples:
- `/docs/Precompaction-Reports/SPEECHRECOGNITION-PRECOMPACTION-REPORT-250903-1430.md`
- `/docs/Precompaction-Reports/VOS4-PRECOMPACTION-REPORT-250903-0845.md`
- `/docs/Precompaction-Reports/LEGACYAVENUE-PRECOMPACTION-REPORT-250903-1615.md`
- `/docs/Precompaction-Reports/VOICEACCESSIBILITY-PRECOMPACTION-REPORT-250903-0930.md`

## 📋 PRE-COMPACTION REPORT TEMPLATE

```markdown
# [Module/App Name] - Pre-Compaction Report
**File:** MODULENAME-PRECOMPACTION-REPORT-YYMMDD-HHMM.md
**Generated:** [YYYY-MM-DD HH:MM] (24-hour format)
**Project Type:** [Implementation/Bug Fix/Refactor/Feature/Migration]
**Module(s):** [List affected modules]
**Critical:** This document MUST be read FIRST after any compaction event

## 🚨 MANDATORY POST-COMPACTION RECOVERY STEPS

### Step 1: Reingest All Instructions
**CRITICAL - DO THIS FIRST:**
1. Read `/Volumes/M Drive/Coding/Warp/VOS4/claude.md`
2. Read `/Volumes/M Drive/Coding/Warp/VOS4/Agent-Instructions/MASTER-STANDARDS.md`
3. Read `/Volumes/M Drive/Coding/Warp/VOS4/Agent-Instructions/MANDATORY-RULES-SUMMARY.md`
4. Read `/Volumes/M Drive/Coding/Warp/VOS4/Agent-Instructions/DOCUMENTATION-CHECKLIST.md`
5. Read `/Volumes/M Drive/Coding/Warp/VOS4/Agent-Instructions/SESSION-LEARNINGS.md`
6. Read `/Volumes/M Drive/Coding/Warp/VOS4/Agent-Instructions/PRECOMPACTION-PROTOCOL.md`

### Step 2: Read Project-Specific Documents
**IN THIS ORDER:**
1. **This document** - [full path]
2. **Project TODO** - [path if exists]
3. **Project Checklist** - [path if exists]
4. **Related Documentation** - [list paths]
5. **Module Changelog** - [path]

### Step 3: Compare and Restore Context
1. Compare this report with internal compaction summary
2. Add missing context back to memory
3. Verify current state matches this report
4. Check git status for uncommitted changes

---

## 📊 PROJECT STATUS SUMMARY

### Overall Progress: [X]% Complete
- **Total Tasks:** [X of Y]
- **Current Task:** [Describe what's in progress]
- **Blockers:** [List any blockers or None]
- **Critical Issues:** [Describe the main problem being solved]

### Completed Tasks:
[List completed work with brief descriptions]

### In Progress:
[Current task details with specific line numbers/files]

### Pending Tasks:
[Remaining work items in priority order]

---

## 🎯 THE PROBLEM & SOLUTION

### Problem Statement:
[Clear description of what problem is being solved]

### Root Cause:
[Technical explanation of why the problem exists]

### Solution Approach:
[Step-by-step approach being taken]

### Critical Implementation Details:
[Key technical details that must not be forgotten]

---

## 🔗 CRITICAL FILE LOCATIONS

### Primary Working Files:
\```
[Full paths to main files being modified]
Example:
/Volumes/M Drive/Coding/Warp/VOS4/[path]/[file].kt (lines X-Y being modified)
\```

### Reference Files:
\```
[Files being referenced or compared against]
\```

### Documentation Files:
\```
[All related documentation file paths]
\```

### Test Files:
\```
[Test files if applicable]
\```

---

## 💾 UNCOMMITTED CHANGES

### Modified Files:
\```bash
# Output of: git status --short
[List any uncommitted changes]
\```

### Key Changes Made:
[Describe important uncommitted changes with line numbers]

---

## 🔑 KEY CONTEXT TO PRESERVE

### Technical Decisions:
[List any important technical decisions made]

### Discovered Issues:
[Problems found during implementation]

### Working Solutions:
[Solutions that have been verified to work]

### Failed Approaches:
[Things tried that didn't work - prevent repetition]

---

## 📌 CRITICAL IMPLEMENTATION NOTES

### Must Remember:
[Bullet points of critical information]

### Special Considerations:
[Edge cases, gotchas, special handling required]

### Dependencies:
[External dependencies or requirements]

---

## 🎯 NEXT IMMEDIATE ACTIONS

### Task to Resume:
[Exact task that should be resumed]

### Specific Steps:
1. [Step 1 with file:line if applicable]
2. [Step 2]
3. [Step 3]

### Expected Outcome:
[What should happen when these steps are complete]

---

## ⚠️ RECOVERY VERIFICATION CHECKLIST

After reading this document, verify:
- [ ] All instruction files reingested
- [ ] Current task identified correctly
- [ ] Progress percentage accurate
- [ ] Working files located and accessible
- [ ] Uncommitted changes understood
- [ ] Critical context restored
- [ ] Ready to continue exactly where left off

---

## 📝 AGENT MEMORY REQUIREMENTS

**Minimum Context Required:**
[List the absolute minimum information needed to continue]

**Full Context Preferred:**
[Additional context that would be helpful]

**Do NOT:**
[Things to avoid repeating or doing]

---

## 🔄 GIT STATUS SNAPSHOT

\```bash
# Branch:
git branch --show-current

# Last 3 commits:
git log --oneline -3

# Uncommitted changes:
git status

# Diff summary:
git diff --stat
\```

---

**END OF PRE-COMPACTION REPORT**
*Generated: [Timestamp]*
*Next Update: [When to update]*
```

## 🚀 QUICK CREATION STEPS

When user says "precompaction" or "create precompaction report":

1. **Identify Current Project:**
   - What module(s) are affected?
   - What's the main task/fix/feature?
   - What's the current progress?

2. **Create Report Using Template:**
   - Use naming convention above
   - Fill all sections with current context
   - Include actual git status output
   - List real file paths and line numbers

3. **Include Everything Active:**
   - All files being modified
   - All documentation created
   - All decisions made
   - All problems encountered

4. **Stage and Commit:**
   ```bash
   git add [precompaction-report-path]
   git commit -m "docs: Create pre-compaction report for [Project Name]
   
   - Document current progress: [X]% complete
   - Capture all working context and decisions
   - Include recovery instructions and file locations
   - Add git status snapshot and next actions"
   ```

5. **Push Immediately:**
   ```bash
   git push origin [branch]
   ```

## 📋 CHECKLIST FOR COMPLETENESS

Before finalizing the report, ensure:
- [ ] All working files listed with full paths
- [ ] Current progress percentage calculated
- [ ] Git status captured
- [ ] Uncommitted changes documented
- [ ] Next actions clearly defined
- [ ] Recovery checklist included
- [ ] All project-specific documents referenced
- [ ] Critical implementation details preserved
- [ ] File naming convention followed

## 💡 TIPS FOR EFFECTIVE REPORTS

1. **Be Specific:** Include line numbers, function names, exact error messages
2. **Show Context:** Include code snippets for complex changes
3. **List Decisions:** Document WHY choices were made
4. **Include Commands:** Show exact commands to run
5. **Reference Everything:** Link to all related documents
6. **Snapshot State:** Include actual output, not descriptions

## 🔧 CUSTOMIZATION

Adapt the template based on project type:
- **Bug Fixes:** Focus on symptoms, root cause, attempted solutions
- **Features:** Emphasize requirements, design decisions, integration points
- **Refactors:** Document before/after state, migration steps
- **Debugging:** Include error traces, hypothesis, test results
- **Migrations:** List compatibility issues, data transformations, rollback plans

---

**Remember:** The goal is to be able to continue EXACTLY where you left off, even with zero prior memory of the project. Treat your future self as a complete stranger to the current work.