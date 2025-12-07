# Monorepo Migration Documentation

**Purpose:** Track all migrations from standalone repos into NewAvanues monorepo

**Structure:** Each repo has dedicated subfolder with complete migration documentation

---

## Folder Structure

| Folder | Purpose | Status |
|--------|---------|--------|
| VoiceOS/ | VoiceOS repo migration tracking | ✅ Phase 2 Complete |
| AVA/ | AVA repo migration tracking | 🔄 Pending |
| Avanues/ | Avanues repo migration tracking | 🔄 Pending |
| MainAvanues/ | MainAvanues (WebAvanue) migration tracking | 🔄 Pending |
| Overview/ | Cross-repo migration guides & strategy | 📝 Reference |

---

## Migration Order

1. **VoiceOS** - Complete ✅
   - Phase 1: Git subtree import (502 files)
   - Phase 2: Gradle restructure (1815+ files, 33 modules)
   - Branch: `voiceos-dev`
   - Docs: 5 files in VoiceOS/

2. **AVA** - Next
   - Exclude: /external-models (manual migration later)
   - Git ignore: Files >100MB
   - Branch: `ava-dev`

3. **Avanues** - After AVA
   - Standard migration process

4. **MainAvanues** - Final
   - Special handling as WebAvanue module
   - Update build.gradle for web app structure

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

---

Updated: 2025-12-06 | IDEACODE v10.3
