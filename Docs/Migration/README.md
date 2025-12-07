# Monorepo Migration Documentation

**Purpose:** Track all migrations from standalone repos into NewAvanues monorepo

**Structure:** Each repo has dedicated subfolder with complete migration documentation

---

## Folder Structure

| Folder | Purpose | Status |
|--------|---------|--------|
| VoiceOS/ | VoiceOS repo migration tracking | ✅ Phase 2 Complete |
| WebAvanue/ | MainAvanues repo (WebAvanue project) migration tracking | 🔄 Next |
| AVA/ | AVA repo migration tracking | 🔄 Pending |
| Avanues/ | Avanues repo migration tracking | 🔄 Pending |
| Overview/ | Cross-repo migration guides & strategy | 📝 Reference |

## NewAvanues Monorepo Structure

```
NewAvanues/
├── Avanues/                    # Brand folder for all Avanues products
│   ├── Web/                   # WebAvanue (from MainAvanues repo)
│   ├── AVA/                   # AVA Assistant (from AVA repo)
│   └── [Other]/               # From Avanues repo
├── android/apps/VoiceOS/      # VoiceOS Android app ✅
├── Modules/VoiceOS/           # VoiceOS feature modules ✅
├── Common/
│   ├── Libraries/VoiceOS/     # VoiceOS KMP libraries ✅
│   └── ThirdParty/            # Shared third-party code ✅
└── Docs/
    ├── VoiceOS/               # VoiceOS documentation ✅
    └── Migration/             # Migration tracking
```

---

## Migration Order & Requirements

### VoiceOS - Complete ✅
- **Repo:** VoiceOS
- **Destination:** `android/apps/VoiceOS/`, `Modules/VoiceOS/`, `Common/Libraries/VoiceOS/`
- Phase 1: Git subtree import (502 files)
- Phase 2: Gradle restructure (1815+ files, 33 modules)
- Branch: `voiceos-dev`
- Docs: 5 files in VoiceOS/
- **Special:** Do NOT migrate /voiceos files from other repos (would duplicate)

### MainAvanues (WebAvanue) - Next (Changed Order)
- **Repo name:** MainAvanues
- **Project name:** WebAvanue
- **Destination:** `Avanues/Web/`
- Modules: android/apps/webavanue, common/webavanue/*
- KMP web application with Android/iOS/Desktop targets
- **Branding:** Under `Avanues/` folder for brand consistency
- **Special:** Do NOT migrate /voiceos files from this repo
- **Config updates:** Update all IDEACODE config references to new path

### AVA - After WebAvanue
- **Repo name:** AVA
- **Project name:** AVA
- **Destination:** `Avanues/AVA/`
- **EXCLUDE:** /external-models initially (18GB, 10+ files >100MB)
- **MOVE:** external-models AFTER migration (not copy)
- **Git ignore:** Files >100MB before moving external-models
- **Note:** User will manually migrate external-models later
- **Branding:** Under `Avanues/` folder for brand consistency
- **Special:** Do NOT migrate /voiceos files from this repo

### Avanues - Final
- **Repo name:** Avanues
- **Project name:** TBD (need clarification)
- **Destination:** `Avanues/?/`
- Standard migration process
- **Branding:** Under `Avanues/` folder for brand consistency
- **Special:** Do NOT migrate /voiceos files from this repo

---

## Document Types Per Repo

Each repo folder contains:

| Document | Purpose |
|----------|---------|
| MIGRATION-MAP.md | Source→Destination file/folder mapping |
| MIGRATION-ANALYSIS.md | File count verification, discrepancy analysis |
| PHASE-*-PLAN.md | Detailed execution plan per phase |
| PHASE-*-STATUS.md | Real-time status during phase execution |
| PHASE-*-COMPLETE.md | Final report with verification |

---

## Key Principles

1. **Preserve History:** Git subtree for one-time import
2. **Verify Everything:** Migration maps before execution
3. **Document Everything:** Comprehensive tracking per repo
4. **Test Continuously:** Gradle sync after each phase
5. **Clean Structure:** Follow FOLDER-REGISTRY.md
6. **Rename All Docs:** All .md files follow IDEACODE convention (except CLAUDE.md)
7. **Consolidate Files:** Move to proper folder structure per registry
8. **Update Registries:** FILE-REGISTRY.md and FOLDER-REGISTRY.md after each migration
9. **No Duplication:** Do NOT migrate /voiceos folders from AVA/Avanues/MainAvanues repos

---

Updated: 2025-12-06 | IDEACODE v10.3
