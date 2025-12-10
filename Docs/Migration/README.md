# Monorepo Migration Documentation

**Purpose:** Track all migrations from standalone repos into NewAvanues monorepo

**Structure:** Each repo has dedicated subfolder with complete migration documentation

---

## Folder Structure

| Folder | Purpose | Status |
|--------|---------|--------|
| VoiceOS/ | VoiceOS repo migration tracking | ✅ Complete |
| WebAvanue/ | MainAvanues repo (WebAvanue project) migration tracking | ✅ Complete |
| AVA/ | AVA repo migration tracking | ✅ Complete |
| Avanues/ | Avanues repo migration tracking | 🔄 Pending |
| Overview/ | Cross-repo migration guides & strategy | 📝 Reference |

## NewAvanues Monorepo Structure

```
NewAvanues/
├── android/apps/
│   ├── VoiceOS/                # VoiceOS Android app + Gradle root ✅
│   │   ├── app/                # Main app module
│   │   ├── tests/              # Test modules
│   │   └── vivoka/             # Vivoka SDK AAR files
│   └── webavanue/              # WebAvanue Android app + Gradle root ✅
│       └── app/                # Android app module
├── Modules/
│   ├── VoiceOS/                # VoiceOS product-specific modules ✅
│   │   ├── apps/               # VoiceOSCore, VoiceCursor, VoiceUI, etc.
│   │   ├── libraries/          # SpeechRecognition, PluginSystem, UniversalIPC, etc.
│   │   ├── managers/           # CommandManager, VoiceDataManager, etc.
│   │   └── core/               # KMP core utilities (result, hash, database, etc.)
│   └── WebAvanue/              # WebAvanue KMP modules ✅
│       ├── universal/          # 95% shared KMP code
│       └── coredata/           # Data layer with SQLDelight
├── Common/                     # Cross-product shared code
│   └── ThirdParty/             # Shared third-party code (Vosk) ✅
├── Avanues/                    # Brand folder for other Avanues products
│   └── AVA/                    # AVA Assistant (from AVA repo) 🔄
└── Docs/
    ├── VoiceOS/                # VoiceOS documentation ✅
    └── Migration/              # Migration tracking ✅
```

**Note:** `Common/` is for cross-PRODUCT shared code (used by VoiceOS, WebAvanue, AVA).
Product-specific code goes in `Modules/{Product}/`.

---

## Migration Order & Requirements

### VoiceOS - Complete ✅
- **Repo:** VoiceOS
- **Destination:** `android/apps/VoiceOS/`, `Modules/VoiceOS/`
- Phase 1: Git subtree import (502 files)
- Phase 2: Gradle restructure (1815+ files, 33 modules)
- Phase 3: Structure fixes (UniversalIPC KMP, Vivoka SDK, leakcanary reflection)
- Branch: `Development`
- Docs: See `VoiceOS-Migration-Issues-Fixes.md`
- **Build Status:** ✅ assembleDebug + assembleRelease SUCCESS
- **Special:** Do NOT migrate /voiceos files from other repos (would duplicate)

### MainAvanues (WebAvanue) - Complete ✅
- **Repo name:** MainAvanues
- **Project name:** WebAvanue
- **Destination:** `android/apps/webavanue/`, `Modules/WebAvanue/`
- Phase 1: Copy files to monorepo structure (9 .kt app files + 389 .kt module files)
- Branch: `WebAvanue-Development`
- Docs: See `WebAvanue/MIGRATION-COMPLETE.md`
- **Build Status:** ✅ assembleDebug + assembleRelease SUCCESS
- **Special:** VoiceOS folders were empty (0 .kt files) - no exclusion needed

### AVA - Complete ✅
- **Repo name:** AVA
- **Project name:** AVA AI
- **Destination:** `android/apps/ava/`, `Modules/AVA/`
- Phase 1: Copy files to monorepo structure
- Phase 2: Update Gradle paths from `:common:*` to new structure
- Phase 3: Build verification
- Branch: `AVA-Development`
- Docs: See `AVA/MIGRATION-COMPLETE.md`
- **Build Status:** ✅ assembleDebug SUCCESS
- **Special:** voiceos files in AVA are AVA-specific client code (VoiceOSQueryProvider, etc.)

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

Updated: 2025-12-07 | IDEACODE v10.3.1
