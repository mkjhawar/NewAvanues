# Implementation Plan: Folder Structure & Namespace Cleanup

**Date:** 2026-01-15 | **Version:** V1 | **Author:** Claude (Opus 4.5)

---

## Overview

This plan addresses the critical folder structure and namespace issues identified in `Analysis-FolderStructure-260115-V1.md`.

**Scope:** ~3,500 files across 4 major cleanup categories
**Approach:** Chain-of-thought reasoning, parallel swarm execution where safe

---

## Phase 1: Pre-Cleanup Verification (BLOCKING)

**Goal:** Ensure we don't lose unique code when deleting duplicates.

### Task 1.1: Diff VoiceOSCore Versions
```
Compare:
- Modules/AvaMagic/apps/VoiceOSCore/
- Modules/VoiceOS/apps/VoiceOSCore/
- android/apps/voiceoscoreng/

Output: List of files unique to each location
```

### Task 1.2: Identify Canonical Implementations
```
For each app in Modules/*/apps/:
- Check if equivalent exists in android/apps/
- Flag files that are UNIQUE to the wrong location
- These must be migrated before deletion
```

### Task 1.3: Create Backup Branch
```bash
git checkout -b backup/pre-cleanup-260115
git add -A
git commit -m "backup: Pre-cleanup snapshot"
git checkout Refactor-TempAll
```

---

## Phase 2: Delete Duplicate Apps (CRITICAL)

**Goal:** Remove 1,202 duplicate files from wrong locations.

### Task 2.1: Delete Modules/AvaMagic/apps/
**Prerequisites:** Phase 1 complete, unique files migrated

```bash
# After verification:
rm -rf Modules/AvaMagic/apps/

# Files removed: ~612
```

### Task 2.2: Delete Modules/VoiceOS/apps/
**Prerequisites:** Phase 1 complete, unique files migrated

```bash
# After verification:
rm -rf Modules/VoiceOS/apps/

# Files removed: ~590
```

### Task 2.3: Update settings.gradle.kts
Remove any include statements for deleted app modules.

### Task 2.4: Verify Build
```bash
./gradlew assembleDebug
```

---

## Phase 3: Namespace Consolidation (HIGH)

**Goal:** Unify fragmented namespaces.

### Task 3.1: Consolidate VoiceOS Namespaces
**Approach:** Rename packages, NOT move files

| From | To |
|------|----|
| `com.augmentalis.voiceos.*` | `com.augmentalis.voiceoscore.*` |
| `com.augmentalis.voiceoscoreng.*` | `com.augmentalis.voiceoscore.*` |

**Files affected:** ~1,485
**Method:** IDE refactor or sed script

```bash
# For each file with wrong package:
# 1. Update package declaration
# 2. Update all imports across codebase
```

### Task 3.2: Fix Wrong Root Packages
| From | To | Files |
|------|----|-------|
| `com.avanues.ui` | `com.augmentalis.avanues.ui` | ~10 |
| `com.avanues.utils` | `com.augmentalis.avanues.utils` | ~10 |
| `com.avanues.voiceos.*` | `com.augmentalis.voiceos.*` | ~28 |

**Location:** `Common/UI/`, `Common/Utils/`, `Docs/`

### Task 3.3: Fix WebAvanue Case
| From | To | Files |
|------|----|-------|
| `com.augmentalis.Avanues.web.*` | `com.augmentalis.webavanue.*` | ~289 |

**Location:** `Avanues/Web/`

### Task 3.4: Consolidate Element Libraries
**Decision required:** Keep separate or merge?

| Option A | Option B |
|----------|----------|
| Keep `avaelements` + `magicelements` separate | Merge all to `avaelements` |
| Less refactoring | Cleaner namespace |

### Task 3.5: Verify Imports
```bash
# Find broken imports after refactoring
./gradlew compileKotlin 2>&1 | grep "Unresolved reference"
```

---

## Phase 4: Flatten Nested Structures (MEDIUM)

**Goal:** Convert nested folders to flat + suffix naming.

### Task 4.1: Identify Files to Flatten
```
Modules/AVA/Chat/src/commonMain/kotlin/com/augmentalis/chat/
├── data/ChatRepository.kt     → ChatRepository.kt
├── domain/Message.kt          → MessageModel.kt
└── ui/ChatScreen.kt           → ChatScreen.kt
```

### Task 4.2: Move Files to Package Root
**For each module with nested folders:**
1. Move files to parent folder
2. Rename with appropriate suffix if needed
3. Update imports
4. Delete empty folders

### Task 4.3: Modules to Flatten
- [ ] Modules/Database/repositories/
- [ ] Modules/LLM/domain/
- [ ] Modules/AVID/core/
- [ ] Modules/AVA/core/Data/data/
- [ ] Modules/AVA/core/Domain/domain/
- [ ] Modules/AVA/Chat/data/, domain/, ui/
- [ ] Modules/AVA/Actions/handlers/
- [ ] Modules/VoiceOS/core/accessibility/

---

## Phase 5: Cleanup & Validation (LOW)

### Task 5.1: Remove Template Placeholders
```bash
# Find unresolved placeholders
grep -r "{{PACKAGE_NAME}}" --include="*.kt"
# Fix or delete these files
```

### Task 5.2: Update MasterDocs
- Update CLASS-INDEX.ai.md with new package locations
- Update PLATFORM-INDEX.ai.md with corrected structure

### Task 5.3: Final Verification
```bash
# No apps in Modules
find Modules -type d -name "apps"  # Should return nothing

# No wrong packages
grep -r "^package com\.avanues[^.]" --include="*.kt"  # Should return nothing
grep -r "^package com\.augmentalis\.voiceos[^c]" --include="*.kt"  # Should return nothing

# Build passes
./gradlew assembleDebug
./gradlew test
```

### Task 5.4: Commit Changes
```bash
git add -A
git commit -m "refactor: Clean up folder structure and namespaces

- Delete duplicate apps from Modules/AvaMagic/apps/ and Modules/VoiceOS/apps/
- Consolidate VoiceOS namespaces to com.augmentalis.voiceoscore
- Fix wrong root packages (com.avanues → com.augmentalis.avanues)
- Fix WebAvanue case (Avanues.web → webavanue)
- Flatten nested folder structures

Removes ~1,202 duplicate files
Fixes ~1,000+ namespace inconsistencies"
```

---

## Task Summary

| Phase | Tasks | Files Affected | Risk |
|-------|-------|----------------|------|
| 1. Verification | 3 | 0 | None |
| 2. Delete Duplicates | 4 | 1,202 | HIGH |
| 3. Namespace | 5 | 1,500+ | MEDIUM |
| 4. Flatten | 3 | 100+ | LOW |
| 5. Cleanup | 4 | 50+ | LOW |

**Total Tasks:** 19
**Estimated Parallel Swarm Agents:** 3-4

---

## Swarm Agent Allocation

| Agent | Phase | Tasks |
|-------|-------|-------|
| Agent 1 (Analysis) | 1 | Diff + identify unique files |
| Agent 2 (Deletion) | 2 | Delete duplicates after approval |
| Agent 3 (Namespace) | 3 | Package refactoring |
| Agent 4 (Structure) | 4 | Flatten folders |

**Sequential dependencies:**
- Phase 2 requires Phase 1 approval
- Phase 3-4 can run in parallel after Phase 2

---

## Decision Points (Require User Input)

1. **VoiceOS canonical:** Which is the source of truth?
   - `voiceoscore` (1,384 files)
   - `voiceos` (1,192 files)
   - `voiceoscoreng` (293 files)

2. **Element libraries:** Merge or keep separate?
   - `avaelements` (560 files)
   - `magicelements` (130 files)

3. **Delete confirmation:** Approve deletion of 1,202 files?

---

**Document Version:** 1.0
**Last Updated:** 2026-01-15
**Status:** Ready for Execution
