# ⚠️ DEPRECATED - This file has been superseded

**Status:** DEPRECATED as of 2025-10-15
**New Location:** `/Volumes/M Drive/Coding/Warp/Agent-Instructions/VOS4-DOCUMENTATION-PROTOCOL.md`
**Reason:** Consolidated into VOS4-DOCUMENTATION-PROTOCOL.md
**Archived By:** Documentation Consolidation Agent

This file is kept for historical reference only. DO NOT use for new development.

---

[Original content below]

<!--
filename: DOCUMENTATION-CHECKLIST.md
created: 2025-01-27 17:50:00 PST
author: VOS4 Development Team
purpose: Mandatory checklist for documentation updates before commits
last-modified: 2025-01-27 17:50:00 PST
version: 1.0.0
-->

# 📋 MANDATORY Documentation Checklist - BEFORE EVERY COMMIT

## 🔴 CRITICAL RULES

1. **NEVER delete files/folders without explicit written approval**
2. **ALL code mergers/refactors MUST be 100% functionally equivalent (unless explicitly told otherwise)**
3. **ALL documentation MUST be updated BEFORE staging code changes**
4. **Stage documentation WITH code changes in same commit**

## ✅ Pre-Commit Checklist

### 1️⃣ Code Changes Validation
- [ ] **Functional Equivalency Verified**
  - [ ] All original features preserved
  - [ ] All method signatures maintained
  - [ ] No functionality removed (unless approved)
  - [ ] Feature comparison matrix created
  - [ ] 100% backward compatibility confirmed

- [ ] **File/Folder Preservation**
  - [ ] NO files deleted without approval
  - [ ] NO folders removed without permission
  - [ ] Archives created instead of deletions
  - [ ] User approval documented if any deletions

### 2️⃣ Master Documentation Updates (if affected)
- [ ] `/docs/ARCHITECTURE.md` - System design, module dependencies
- [ ] `/docs/PRD.md` - Product requirements, module status
- [ ] `/docs/ROADMAP.md` - Timeline, completed tasks
- [ ] `/docs/INTERACTION_MAP.md` - Module interactions
- [ ] `/docs/Status/Current/` - Current project state
- [ ] `/docs/TODO/VOS4-TODO-Master.md` - Active tasks

### 3️⃣ Module Documentation Updates (ALWAYS for affected modules)
- [ ] `/docs/modules/[module]/[Module]-Changelog.md` **MANDATORY**
- [ ] `/docs/modules/[module]/[Module]-Developer-Manual.md`
- [ ] `/docs/modules/[module]/[Module]-Architecture.md`
- [ ] `/docs/modules/[module]/[Module]-Implementation-Status.md`
- [ ] `/docs/modules/[module]/[Module]-API-Reference.md`
- [ ] `/docs/modules/[module]/[Module]-Master-Inventory.md`
- [ ] `/docs/modules/[module]/README.md` (if significant changes)

### 4️⃣ Visual Documentation Updates (MANDATORY if created/changed)
- [ ] **Architecture Diagrams**
  - [ ] System architecture diagrams (Mermaid/ASCII)
  - [ ] Module interaction diagrams
  - [ ] Component relationship diagrams
  - [ ] Data flow diagrams
  
- [ ] **Flowcharts & Sequences**
  - [ ] Process flowcharts
  - [ ] Sequence diagrams for APIs
  - [ ] State machine diagrams
  - [ ] Decision trees
  
- [ ] **UI/UX Documentation**
  - [ ] UI layouts and wireframes
  - [ ] Screen flow diagrams
  - [ ] Component hierarchy diagrams
  - [ ] Accessibility flow charts
  
- [ ] **Technical Diagrams**
  - [ ] Class diagrams (if applicable)
  - [ ] Database schema diagrams
  - [ ] Network topology diagrams
  - [ ] Deployment diagrams

### 4️⃣ Planning Documents (if plans affected)
- [ ] `/docs/Planning/Architecture/` - Architecture plans
- [ ] `/docs/Planning/Implementation/` - Implementation strategies
- [ ] Module-specific TODO files
- [ ] Feature specification documents

### 5️⃣ Status and Progress Updates
- [ ] Implementation status updated
- [ ] TODO items marked as completed
- [ ] New issues/blockers documented
- [ ] Progress percentages updated
- [ ] Milestone achievements noted

## 📝 Commit Message Requirements

### Format:
```
type(scope): Brief description

- List key changes
- Document what was updated
- Note any documentation changes

Author: [Name]
```

### NEVER Include:
- ❌ "Claude", "Anthropic", "AI" references
- ❌ "Generated with [tool]" statements
- ❌ "Co-Authored-By: Claude"
- ✅ Keep professional and tool-agnostic

## 🔄 Staging Process

1. **Update Documentation FIRST**
   ```bash
   # Check what needs updating
   git status
   
   # Update all affected docs
   # Then stage documentation
   git add docs/
   ```

2. **Stage Code Changes WITH Documentation**
   ```bash
   # Stage specific code files
   git add [specific-files]
   
   # NEVER use git add . (except in CodeImport/)
   ```

3. **Verify Everything Staged**
   ```bash
   # Check all changes are staged
   git status
   
   # Review what will be committed
   git diff --cached
   ```

4. **Commit with Descriptive Message**
   ```bash
   git commit -m "type(scope): Description
   
   - Updated [Module]-Changelog.md
   - Modified architecture documentation
   - Marked TODO items complete
   
   Author: [Name]"
   ```

## 🚨 Common Mistakes to Avoid

1. **Committing code without documentation updates**
2. **Deleting files without approval**
3. **Removing functionality without permission**
4. **Forgetting to update changelogs**
5. **Not staging documentation with code**
6. **Using git add . outside CodeImport/**
7. **Including AI tool references in commits**
8. **Not verifying functional equivalency**

## 📊 Quick Reference Tables

### Documentation Priority Matrix
| Change Type | Changelog | Dev Manual | Architecture | API Ref | Status |
|------------|-----------|------------|--------------|---------|---------|
| Bug Fix | ✅ MUST | If needed | No | If API changed | ✅ MUST |
| Feature Add | ✅ MUST | ✅ MUST | If design changed | ✅ MUST | ✅ MUST |
| Refactor | ✅ MUST | If usage changed | If structure changed | If API changed | ✅ MUST |
| Import/Merge | ✅ MUST | ✅ MUST | ✅ MUST | ✅ MUST | ✅ MUST |
| Performance | ✅ MUST | No | No | No | ✅ MUST |

### Functional Equivalency Checklist
| Aspect | Before | After | Equivalent? |
|--------|--------|-------|-------------|
| Features | List all | List all | ✅ Must match |
| Methods | Count & list | Count & list | ✅ Must match |
| Parameters | Document all | Document all | ✅ Must match |
| Return types | Note all | Note all | ✅ Must match |
| Behavior | Test results | Test results | ✅ Must match |

## 🔍 Verification Commands

```bash
# Check for uncommitted documentation
find docs -name "*.md" -newer .git/index

# List all module documentation
ls -la docs/modules/*/

# Check changelog was updated
git diff docs/modules/[module]/[Module]-Changelog.md

# Verify no unapproved deletions
git status --porcelain | grep "^D"
```

## ⚡ Quick Checklist (Copy/Paste)

```
Pre-Commit Documentation Check:
- [ ] Functional equivalency verified (100%)
- [ ] No files/folders deleted without approval
- [ ] Master docs updated (if affected)
- [ ] Module changelog updated (MANDATORY)
- [ ] Module docs updated (as needed)
- [ ] Status/TODO updated
- [ ] Documentation staged with code
- [ ] Commit message ready (no AI refs)
- [ ] git status verified
```

---

**Remember:** Documentation is NOT optional. It's MANDATORY for every commit.
**Rule:** If you changed it, document it. If you built it, explain it. If you fixed it, log it.