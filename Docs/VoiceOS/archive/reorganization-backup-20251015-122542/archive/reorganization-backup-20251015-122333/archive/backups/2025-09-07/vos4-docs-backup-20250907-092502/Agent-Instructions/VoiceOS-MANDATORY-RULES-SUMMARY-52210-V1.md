<!--
filename: MANDATORY-RULES-SUMMARY.md
created: 2025-01-27 18:05:00 PST
author: VOS4 Development Team
purpose: Quick reference for ALL mandatory rules - MUST BE INTERNALIZED
version: 1.0.0
priority: CRITICAL - READ FIRST
-->

# 🔴 MANDATORY RULES SUMMARY - INTERNALIZE IMMEDIATELY

## ⚠️ ZERO TOLERANCE VIOLATIONS (Immediate Rollback)

### 1. File/Folder Deletion
**NEVER delete files/folders without EXPLICIT written approval**
- Archive instead of delete
- Move to deprecated/ if needed
- Deletion requires user approval IN WRITING
- Document approval in commit message

### 2. Functional Equivalency
**ALL code mergers/refactors MUST be 100% functionally equivalent**
- UNLESS explicitly told otherwise by user
- Create feature comparison matrix before/after
- Maintain ALL existing features
- Keep ALL method signatures
- Preserve ALL functionality
- Enhancements OK but NEVER remove features

### 3. Documentation Updates
**ALL documentation MUST be updated BEFORE commits**
- Module changelog - MANDATORY for EVERY change
- Architecture diagrams and flowcharts
- UI layouts and wireframes (if changed)
- Visual documentation (sequences, data flows)
- Status and TODO updates
- Stage documentation WITH code in SAME commit

### 4. Commit Standards
**NO AI/tool references in commits**
- ❌ NO "Claude", "Anthropic", "AI" mentions
- ❌ NO "Generated with" statements
- ❌ NO "Co-Authored-By: Claude"
- ✅ Keep commits professional and tool-agnostic

## 📋 MANDATORY Pre-Commit Checklist

```
EVERY COMMIT MUST COMPLETE:
□ Functional equivalency verified (100%)
□ No files/folders deleted without approval
□ Module changelog updated (MANDATORY)
□ Visual documentation updated if created/changed:
  □ Architecture diagrams (Mermaid/ASCII)
  □ Flowcharts and sequences
  □ UI layouts and wireframes
  □ Component relationship diagrams
□ All affected master docs updated
□ Status and TODO items updated
□ Documentation staged WITH code
□ Commit message ready (no AI refs)
□ git status verified
```

## 🚨 VOS4 Core Standards (Non-Negotiable)

### Architecture
- **Direct implementation only** - NO interfaces (except approved exceptions)
- **Namespace:** `com.augmentalis.*` ONLY
- **Database:** ObjectBox ONLY (no SQLite/Room)
- **Self-contained modules** - All components in same module

### Performance Requirements
- Initialization: <1 second
- Module load: <50ms per module
- Command recognition: <100ms latency
- Memory: Vosk <30MB, Vivoka <60MB
- Battery: <2% per hour

### Documentation Requirements
- **ALL docs must include diagrams** (Mermaid + ASCII)
- **Living documents** - Update with every change
- **Visual documentation** - Flowcharts, sequences, UI layouts
- **Feature matrices** - For all refactors/merges

## 🔍 Quick Verification Commands

```bash
# Check for unapproved deletions
git status --porcelain | grep "^D"

# Verify documentation updated
git diff docs/modules/[module]/[Module]-Changelog.md

# Check all staged files
git status

# Review staged changes
git diff --cached
```

## 📁 Key Documentation Locations

### Master Documents (Update if affected):
- `/docs/ARCHITECTURE.md`
- `/docs/PRD.md`
- `/docs/ROADMAP.md`
- `/docs/INTERACTION_MAP.md`
- `/docs/TODO/VOS4-TODO-Master.md`

### Module Documents (ALWAYS update for changes):
- `/docs/modules/[module]/[Module]-Changelog.md` **MANDATORY**
- `/docs/modules/[module]/[Module]-Architecture.md`
- `/docs/modules/[module]/[Module]-Implementation-Status.md`
- `/docs/modules/[module]/[Module]-API-Reference.md`

## ⚡ One-Line Rule Reminders

- **Delete nothing without approval**
- **100% functional equivalency or rollback**
- **Document everything before committing**
- **Stage docs with code always**
- **No AI references ever**
- **Update visual documentation**
- **Changelogs are MANDATORY**

## 🔗 Full Documentation References

- **Complete Standards:** `/Agent-Instructions/MASTER-STANDARDS.md`
- **Pre-Commit Checklist:** `/Agent-Instructions/DOCUMENTATION-CHECKLIST.md`
- **Coding Patterns:** `/Agent-Instructions/CODING-GUIDE.md`
- **Recent Learnings:** `/Agent-Instructions/SESSION-LEARNINGS.md`

---

**CRITICAL:** These rules are MANDATORY. Zero tolerance for violations.
**REMEMBER:** If you change it, document it. If you delete it, get approval. If you merge it, maintain equivalency.