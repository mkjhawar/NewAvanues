# MainAvanues Documentation Cleanup Plan

**Date:** 2025-11-25 09:30
**Purpose:** Reorganize /docs/ with proper naming and structure
**Framework:** IDEACODE v8.5

---

## 🎯 Objectives

1. Remove redundant/duplicate directories
2. Consolidate similar content
3. Apply proper naming nomenclature to ALL files
4. Organize by module following PROJECT-FILE-REGISTRY.md
5. Move WebAvanue files to proper locations

---

## 📋 Naming Nomenclature (MANDATORY)

### Standard Files
**Pattern:** `modulename-feature-description-YYYYMMDDHHMM.md`

**Examples:**
- `webavanue-webxr-implementation-status-202511231800.md`
- `webavanue-migration-complete-summary-202511250315.md`
- `voiceos-voice-recognition-setup-202511201400.md`

### Repository-Level Documents
**Pattern:** `mainavanues-description.md` or `LD-mainavanues-description.md`

**Examples:**
- `mainavanues-monorepo-best-practices.md`
- `LD-mainavanues-architecture.md` (Living Document)
- `LD-mainavanues-dependencies.md`

### Living Documents (LD)
**Pattern:** `LD-{repo/module}-description.md`

**Purpose:** Documents that are continuously updated

**Examples:**
- `LD-mainavanues-architecture.md`
- `LD-webavanue-api.md`
- `LD-voiceos-protocols.md`

### Module Documents
**Pattern:** `modulename-description.md`

**Examples:**
- `webavanue-readme.md`
- `webavanue-api.md`
- `voiceos-changelog.md`

---

## 🗂️ Current Structure Analysis

### Problems Identified

1. **Duplicate/Redundant Directories:**
   - `docs/webavanue/` AND `docs/web/apps/webavanue/` AND `docs/common/libs/webavanue/`
   - `docs/voiceos/` AND `docs/android/apps/voiceos/`
   - Multiple `ideacode/` folders scattered everywhere

2. **Inconsistent Naming:**
   - `WebAvanue-Build-Test-Results-202511250300.md` (PascalCase)
   - `ARCHITECTURE.md` (ALL CAPS)
   - `api-overview.md` (missing module prefix)

3. **Wrong Locations:**
   - WebAvanue features in `docs/common/libs/webavanue/ideacode/features/`
   - Should be in `docs/webavanue/ideacode/features/`

4. **Scattered Content:**
   - Documentation in 15+ different locations
   - No clear organization principle

---

## 🎯 Target Structure

```
docs/
├── project/                                    # Repository-level docs
│   ├── LD-mainavanues-architecture.md
│   ├── mainavanues-monorepo-best-practices.md
│   ├── mainavanues-git-access-guide.md
│   ├── mainavanues-monorepo-structure.md
│   ├── LD-mainavanues-dependencies.md
│   ├── mainavanues-setup.md
│   ├── mainavanues-testing.md
│   └── mainavanues-documentation-consolidation.md
│
├── webavanue/                                  # WebAvanue module
│   ├── LD-webavanue-readme.md
│   ├── LD-webavanue-api.md
│   ├── LD-webavanue-changelog.md
│   ├── specs/                                  # Feature specs
│   │   ├── webavanue-spec-scrolling-controls.md
│   │   ├── webavanue-spec-zoom-controls.md
│   │   ├── webavanue-spec-favorites-bar.md
│   │   ├── webavanue-spec-webxr-support.md
│   │   └── ...
│   ├── plans/                                  # Implementation plans
│   │   ├── webavanue-plan-scrolling-controls.md
│   │   ├── webavanue-plan-zoom-controls.md
│   │   └── ...
│   ├── proposals/                              # Feature proposals
│   │   ├── webavanue-proposal-touch-controls.md
│   │   ├── webavanue-proposal-qr-scanner.md
│   │   └── ...
│   └── archive/                                # Completed features
│       ├── webavanue-webxr-phase2-implementation-202511231650.md
│       └── ...
│
├── voiceos/                                    # VoiceOS module
│   ├── LD-voiceos-readme.md
│   ├── LD-voiceos-api.md
│   ├── specs/
│   ├── plans/
│   └── archive/
│
├── avaconnect/                                 # AvaConnect module
│   ├── LD-avaconnect-readme.md
│   └── ...
│
├── ava/                                        # AVA module
│   └── ...
│
├── develop/                                    # Development artifacts (temporary)
│   ├── webavanue/
│   │   ├── webavanue-build-results-202511250300.md
│   │   ├── webavanue-test-results-202511250315.md
│   │   ├── webavanue-git-history-verification-202511250350.md
│   │   └── webavanue-migration-complete-summary-202511250400.md
│   ├── voiceos/
│   └── avaconnect/
│
├── architecture/                               # Architecture docs
│   ├── decisions/                              # ADRs
│   │   ├── mainavanues-adr-001-use-kmp.md
│   │   ├── mainavanues-adr-002-monorepo-structure.md
│   │   └── ...
│   └── patterns/                               # Design patterns
│       ├── mainavanues-pattern-repository.md
│       └── mainavanues-pattern-viewmodel.md
│
├── operations/                                 # Operations & deployment
│   ├── mainavanues-deployment-guide.md
│   ├── mainavanues-monitoring-guide.md
│   └── runbooks/
│       ├── mainavanues-runbook-incident-response.md
│       └── ...
│
├── migration-analysis/                         # Migration docs (archive later)
│   ├── mainavanues-complete-migration-guide.md
│   ├── mainavanues-migration-checklist.md
│   └── mainavanues-migration-lessons-learned.md
│
└── archive/                                    # Archived docs
    └── 2024/
        └── ...
```

---

## 🔄 File Movements

### 1. Repository-Level Documents → docs/project/

| Current | New |
|---------|-----|
| `docs/ARCHITECTURE.md` | `docs/project/LD-mainavanues-architecture.md` |
| `docs/SETUP.md` | `docs/project/mainavanues-setup.md` |
| `docs/TESTING.md` | `docs/project/mainavanues-testing.md` |
| `docs/DOCUMENTATION-CONSOLIDATION.md` | `docs/project/mainavanues-documentation-consolidation.md` |
| `docs/README.md` | `docs/project/mainavanues-readme.md` |
| `docs/project/monorepo-best-practices.md` | `docs/project/mainavanues-monorepo-best-practices.md` |
| `docs/project/monorepo-structure.md` | `docs/project/mainavanues-monorepo-structure.md` |
| `docs/project/git-access-guide.md` | `docs/project/mainavanues-git-access-guide.md` |

### 2. WebAvanue Files → docs/webavanue/

#### From docs/common/libs/webavanue/

| Current | New |
|---------|-----|
| `docs/common/libs/webavanue/README.md` | `docs/webavanue/LD-webavanue-readme.md` |
| `docs/common/libs/webavanue/ideacode/features/001-*/spec.md` | `docs/webavanue/specs/webavanue-spec-scrolling-controls.md` |
| `docs/common/libs/webavanue/ideacode/features/001-*/plan.md` | `docs/webavanue/plans/webavanue-plan-scrolling-controls.md` |
| `docs/common/libs/webavanue/ideacode/features/001-*/proposal.md` | `docs/webavanue/proposals/webavanue-proposal-scrolling-controls.md` |
| (All feature folders 001-012) | Move to appropriate subdirs |

#### From docs/web/apps/webavanue/

| Current | New |
|---------|-----|
| `docs/web/apps/webavanue/dev-overview.md` | `docs/webavanue/webavanue-dev-overview.md` |

#### From docs/webavanue/ (consolidate to root)

Already in correct place, just rename files.

#### From docs/develop/webavanue/ (keep as-is, just rename)

| Current | New |
|---------|-----|
| `docs/develop/webavanue/WebAvanue-Build-Test-Results-202511250300.md` | `docs/develop/webavanue/webavanue-build-results-202511250300.md` |
| `docs/develop/webavanue/WebAvanue-Complete-Test-Results-202511250315.md` | `docs/develop/webavanue/webavanue-test-results-complete-202511250315.md` |
| `docs/develop/webavanue/WebAvanue-Git-History-Verification-202511250350.md` | `docs/develop/webavanue/webavanue-git-history-verification-202511250350.md` |
| `docs/develop/webavanue/WebAvanue-Migration-Complete-Summary.md` | `docs/develop/webavanue/webavanue-migration-complete-summary-202511250400.md` |

### 3. VoiceOS Files → docs/voiceos/

Consolidate:
- `docs/android/apps/voiceos/` → `docs/voiceos/`
- `docs/voiceos/` (keep as main location)

### 4. Architecture Docs → docs/architecture/

Already mostly correct, just rename files.

---

## 🗑️ Directories to Remove

### Redundant Directories (DELETE after moving content)

```bash
# Remove after consolidation
docs/android/apps/webavanue/        # → docs/webavanue/
docs/web/apps/webavanue/            # → docs/webavanue/
docs/common/libs/webavanue/         # → docs/webavanue/
docs/android/apps/voiceos/          # → docs/voiceos/
docs/android/apps/ava/              # → docs/ava/
docs/android/apps/avaconnect/       # → docs/avaconnect/
docs/android/apps/avanues/          # Keep only if has unique content

# Remove empty parent dirs
docs/android/apps/                  # (if empty)
docs/android/                       # (if empty)
docs/web/apps/                      # (if empty)
docs/web/                           # (if empty)
docs/common/libs/                   # (if empty)
docs/common/                        # (if empty)

# Remove redundant organizational dirs
docs/bugs/                          # → Use GitHub Issues instead
docs/changelogs/                    # → {module}/LD-{module}-changelog.md
docs/decisions/                     # → docs/architecture/decisions/
docs/demos/                         # → docs/{module}/demos/ or archive
docs/developer/                     # → docs/project/ or docs/manuals/
docs/ideacode/                      # → /ideacode/ repo or docs/{module}/
docs/specs/                         # → docs/{module}/specs/
docs/status/                        # → docs/develop/{module}/
docs/summaries/                     # → docs/develop/ or archive
docs/user/                          # → docs/manuals/user/
```

---

## 📝 Renaming Rules

### Feature Documents from IDEACODE

**Current pattern:**
```
docs/common/libs/webavanue/ideacode/features/
  001-port-legacy-scrolling-controls-to-webavanue/
    spec.md
    plan.md
    proposal.md
```

**New pattern:**
```
docs/webavanue/
  specs/webavanue-spec-scrolling-controls.md
  plans/webavanue-plan-scrolling-controls.md
  proposals/webavanue-proposal-scrolling-controls.md
```

### Living Documents

Add `LD-` prefix to continuously updated docs:
- `README.md` → `LD-{module}-readme.md`
- `API.md` → `LD-{module}-api.md`
- `CHANGELOG.md` → `LD-{module}-changelog.md`
- `ARCHITECTURE.md` → `LD-mainavanues-architecture.md`

### Timestamped Documents

Add timestamp to one-time documents:
- `WebAvanue-Migration-Complete-Summary.md` → `webavanue-migration-complete-summary-202511250400.md`

---

## ✅ Cleanup Steps

### Phase 1: Create New Structure
```bash
# Create standard directories
mkdir -p docs/{webavanue,voiceos,avaconnect,ava}/{specs,plans,proposals,archive}
mkdir -p docs/develop/{webavanue,voiceos,avaconnect,ava}
mkdir -p docs/architecture/{decisions,patterns}
mkdir -p docs/operations/runbooks
```

### Phase 2: Move & Rename Files (with git mv)
- Move repository docs to docs/project/ with proper naming
- Move WebAvanue docs to docs/webavanue/ with proper naming
- Move VoiceOS docs to docs/voiceos/ with proper naming
- Rename all files to follow nomenclature

### Phase 3: Remove Redundant Directories
```bash
# After verifying all content moved
git rm -r docs/android/
git rm -r docs/web/
git rm -r docs/common/
# ... etc
```

### Phase 4: Update References
- Update PROJECT-FILE-REGISTRY.md
- Update CLAUDE.md
- Update ideacode documentation
- Update ideacode-mcp documentation

---

## 🎯 Success Criteria

- [ ] All files follow naming nomenclature
- [ ] No duplicate directories
- [ ] WebAvanue files consolidated in docs/webavanue/
- [ ] Repository docs in docs/project/
- [ ] Living documents prefixed with LD-
- [ ] Timestamped documents have YYYYMMDDHHMM
- [ ] PROJECT-FILE-REGISTRY.md updated
- [ ] IDEACODE documentation updated

---

**Next:** Execute cleanup plan with git mv to preserve history
