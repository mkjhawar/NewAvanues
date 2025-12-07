# URGENT: Coding Folder Reorganization Based on Master Template
**Date**: 2025-10-15 15:51 PDT
**Status**: CORRECTION - Following Master Instructions

---

## ❌ CRITICAL ERROR IN PREVIOUS ANALYSIS

**Previous Assumption**: /coding/ folder should exist for "active work"
**CORRECT STRUCTURE** (per master instructions): NO /coding/ folder - everything under /docs/

**Master Template**: `/Volumes/M Drive/Coding/Docs/agents/instructions/Guide-Documentation-Structure.md`

---

## ✅ CORRECT Structure (Per Master Template)

```
/vos4/docs/
├── README.md                      # Documentation index
├── INDEX.md                       # Complete document index
│
├── master/                        # Project-wide tracking
│   ├── changelogs/
│   │   ├── CHANGELOG-MASTER.md
│   │   ├── CHANGELOG-CURRENT.md
│   │   └── archives/
│   ├── status/
│   │   ├── PROJECT-STATUS-CURRENT.md
│   │   ├── PROJECT-STATUS-SUMMARY.md
│   │   └── archives/
│   ├── tasks/
│   │   ├── PROJECT-TODO-MASTER.md
│   │   ├── PROJECT-TODO-PRIORITY.md
│   │   ├── PROJECT-TODO-BACKLOG.md
│   │   └── completed/
│   └── inventories/
│       └── PROJECT-MASTER-INVENTORY.md
│
├── planning/
│   ├── project/                   # Requirements, roadmap
│   ├── architecture/              # System architecture
│   │   └── decisions/            # Architecture Decision Records (ADRs)
│   ├── implementation/            # Implementation plans
│   └── features/                  # Feature specifications
│
├── modules/                       # Module-specific docs (19 modules)
│   ├── VoiceAccessibility/
│   ├── CommandManager/
│   └── ... (all PascalCase)
│
├── visuals/                       # Visual documentation
│   ├── system/
│   ├── sequences/
│   └── technical/
│
├── templates/                     # Templates & standards ✅ EXISTS
│   ├── document-templates/
│   └── standards/
│       └── NAMING-CONVENTIONS.md
│
├── commits/                       # Commit documentation
│   ├── current/
│   └── archives/
│
├── scripts/                       # Automation ✅ EXISTS
└── Archive/                       # Deprecated docs ✅ EXISTS
```

---

## 🔄 Required Migration from /coding/ to /docs/

### 1. TODO Files → /docs/master/tasks/

**From**: `/coding/TODO/` (35 files)
**To**: `/docs/master/tasks/`

**Mapping**:
```
/coding/TODO/VOS4-TODO-Master-*.md           → /docs/master/tasks/PROJECT-TODO-MASTER.md
/coding/TODO/[Module]-TODO-*.md              → /docs/modules/[Module]/[Module]-Tasks.md
/coding/TODO/NEXT-STEPS-*.md                 → /docs/master/tasks/PROJECT-TODO-PRIORITY.md
/coding/TODO/[Feature]-Plan-*.md             → /docs/planning/implementation/
```

### 2. STATUS Files → /docs/master/status/

**From**: `/coding/STATUS/` (136 files)
**To**: `/docs/master/status/` (recent) + `/docs/master/status/archives/` (old)

**Mapping**:
```
Recent (last 7 days):
/coding/STATUS/Status-VOS4-Project-*.md      → /docs/master/status/PROJECT-STATUS-CURRENT.md
/coding/STATUS/Complete-Implementation-*.md   → /docs/master/status/PROJECT-STATUS-SUMMARY.md

Module-specific:
/coding/STATUS/[Module]-Status-*.md          → /docs/modules/[Module]/[Module]-Status.md

Historical (>7 days):
All older files                               → /docs/master/status/archives/[YYYY-MM]/
```

### 3. ISSUES → /docs/planning/project/ OR /docs/master/tasks/

**From**: `/coding/ISSUES/` (11 files)
**To**: `/docs/master/tasks/` (add to TODO backlog)

**Mapping**:
```
/coding/ISSUES/CRITICAL/*.md                  → /docs/master/tasks/PROJECT-TODO-PRIORITY.md (top priority)
/coding/ISSUES/HIGH/*.md                      → /docs/master/tasks/PROJECT-TODO-PRIORITY.md (high)
/coding/ISSUES/MEDIUM/*.md                    → /docs/master/tasks/PROJECT-TODO-BACKLOG.md
/coding/ISSUES/LOW/*.md                       → /docs/master/tasks/PROJECT-TODO-BACKLOG.md
```

### 4. DECISIONS → /docs/planning/architecture/decisions/

**From**: `/coding/DECISIONS/` (8 files)
**To**: `/docs/planning/architecture/decisions/`

**Rename to ADR format**:
```
/coding/DECISIONS/*.md                        → /docs/planning/architecture/decisions/ADR-NNN-[Topic].md
```

### 5. reviews → /docs/planning/implementation/ OR /docs/commits/

**From**: `/coding/reviews/` (9 files)
**To**: `/docs/commits/current/` (if commit-related) OR `/docs/planning/implementation/`

### 6. planning → /docs/planning/

**From**: `/coding/planning/` (10 files)
**To**: Various locations under `/docs/planning/`

**Mapping**:
```
/coding/planning/*-Implementation-Guide.md    → /docs/planning/implementation/
/coding/planning/*-Developer-Guide.md         → /docs/modules/[Module]/[Module]-Developer-Guide.md
/coding/planning/build-reports/*.md           → /docs/planning/project/ (or master/status/archives/)
/coding/planning/Refactoring/*.md             → /docs/planning/architecture/
```

### 7. commits → /docs/commits/

**From**: `/coding/commits/` (2 files)
**To**: `/docs/commits/archives/`

### 8. Root Analysis Files → /docs/Active/ OR /docs/planning/

**From**: `/coding/*.md` (4 files)
**To**: `/docs/Active/` (temporary) OR appropriate permanent location

---

## 🗑️ Remove Empty Folders

After migration:
- ❌ Delete entire `/coding/` folder
- ❌ Remove from CLAUDE.md references
- ❌ Remove from all documentation

---

## 🔧 Implementation Steps

1. **Create missing /docs/ folders** following master template:
   ```
   mkdir -p docs/master/{changelogs,status/{archives},tasks/{completed},inventories}
   mkdir -p docs/planning/{project,architecture/decisions,implementation,features}
   mkdir -p docs/visuals/{system,sequences,technical}
   mkdir -p docs/commits/{current,archives}
   mkdir -p docs/templates/{document-templates,standards}
   ```

2. **Migrate TODO files** to /docs/master/tasks/ and /docs/modules/

3. **Migrate STATUS files** (recent to master/status/, old to archives/)

4. **Migrate DECISIONS** to /docs/planning/architecture/decisions/ as ADRs

5. **Migrate reviews** to appropriate locations

6. **Migrate planning files** to /docs/planning/

7. **Migrate commits** to /docs/commits/archives/

8. **Update CLAUDE.md** to remove /coding/ references

9. **Delete /coding/ folder** entirely

10. **Commit changes** by category

---

## 🚨 URGENT ACTION REQUIRED

**Status**: Awaiting user confirmation to proceed with CORRECT reorganization

**This supersedes**: `Coding-Folder-Consolidation-Analysis-251015-1549.md` (INCORRECT)

**Follow**: Master template at `/Volumes/M Drive/Coding/Docs/agents/instructions/Guide-Documentation-Structure.md`

---

**Ready to execute**: Full migration from /coding/ to /docs/ following master template
