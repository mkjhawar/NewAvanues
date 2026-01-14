# Implementation Plan: AVA Package Structure Cleanup

## Overview

| Attribute | Value |
|-----------|-------|
| **Platforms** | Android (KMP modules) |
| **Swarm Recommended** | Yes (8 modules, 305+ files) |
| **Total Files** | 305 Kotlin files + 66 NLU files |
| **Import Changes** | ~450 cross-module imports |
| **Unique Packages** | 64 packages to rename |
| **Risk Level** | Medium (refactoring only, no logic changes) |

---

## Scope Analysis

### Files Per Module

| Module | Files | Priority | Complexity |
|--------|-------|----------|------------|
| memory | 7 | 1 (first) | Low |
| WakeWord | 12 | 2 | Low |
| Teach | 18 | 3 | Low |
| Actions | 40 | 4 | Medium |
| Chat | 47 | 5 | Medium |
| RAG | 78 | 6 | High |
| LLM | 103 | 7 | High |
| Shared/NLU | 66 | 8 (last) | High |
| **Total** | **371** | | |

### Change Metrics

| Metric | Count |
|--------|-------|
| Package declarations to update | ~305 |
| Cross-module imports to update | ~450 |
| build.gradle.kts namespace updates | 8 |
| Test file updates | ~50 |
| App module import updates | ~100 |

---

## Phases

### Phase 1: Setup & Analysis
**Duration estimate: N/A (preparation)**

| Task | Description | Validation |
|------|-------------|------------|
| 1.1 | Create feature branch `feature/package-restructure` | Branch exists |
| 1.2 | Run baseline test suite, record pass count | Document baseline |
| 1.3 | Generate package inventory CSV | File at `/tmp/ava-package-inventory.csv` |
| 1.4 | Create migration mapping JSON | File at `Docs/AVA/migrations/package-mapping.json` |
| 1.5 | Identify Hilt modules and reflection usage | Document findings |

### Phase 2: memory Module (Smallest First)
**Files: 7 | Risk: Low**

| Task | Description | Validation |
|------|-------------|------------|
| 2.1 | Create new directory structure `com/augmentalis/memory/` | Directories exist |
| 2.2 | Update package declarations in all 7 files | `grep "^package" returns new pattern` |
| 2.3 | Move files to new locations | Files in new path |
| 2.4 | Update build.gradle.kts namespace | `namespace = "com.augmentalis.memory"` |
| 2.5 | Update imports in other modules referencing memory | `grep` returns 0 old refs |
| 2.6 | Verify build compiles | `./gradlew :memory:compileDebugKotlin` SUCCESS |
| 2.7 | Remove old directories | No orphaned files |
| 2.8 | Commit changes | Atomic commit for module |

### Phase 3: WakeWord Module
**Files: 12 | Risk: Low**

| Task | Description | Validation |
|------|-------------|------------|
| 3.1 | Create new directory structure `com/augmentalis/wakeword/` | Directories exist |
| 3.2 | Update package declarations in all 12 files | New pattern |
| 3.3 | Move files to new locations | Files moved |
| 3.4 | Update build.gradle.kts namespace | Updated |
| 3.5 | Update cross-module imports | No old refs |
| 3.6 | Verify build | SUCCESS |
| 3.7 | Remove old directories | Clean |
| 3.8 | Commit changes | Atomic commit |

### Phase 4: Teach Module
**Files: 18 | Risk: Low**

| Task | Description | Validation |
|------|-------------|------------|
| 4.1 | Create new directory structure `com/augmentalis/teach/` | Directories exist |
| 4.2 | Update package declarations | New pattern |
| 4.3 | Move files | Files moved |
| 4.4 | Update build.gradle.kts | Updated |
| 4.5 | Update cross-module imports | No old refs |
| 4.6 | Verify build | SUCCESS |
| 4.7 | Cleanup | Clean |
| 4.8 | Commit | Atomic commit |

### Phase 5: Actions Module
**Files: 40 | Risk: Medium**

| Task | Description | Validation |
|------|-------------|------------|
| 5.1 | Create directory structure with subpackages | `executor/`, `registry/` |
| 5.2 | Update package declarations (40 files) | New pattern |
| 5.3 | Update internal imports within Actions | Consistent |
| 5.4 | Move files preserving subpackage structure | Files moved |
| 5.5 | Update build.gradle.kts | Updated |
| 5.6 | Update cross-module imports (Actions is heavily referenced) | No old refs |
| 5.7 | Verify build | SUCCESS |
| 5.8 | Cleanup and commit | Atomic commit |

### Phase 6: Chat Module
**Files: 47 | Risk: Medium**

| Task | Description | Validation |
|------|-------------|------------|
| 6.1 | Create directory structure | `state/`, `components/`, `coordinator/`, `dialogs/`, `tts/`, `settings/` |
| 6.2 | Apply subpackage flattening rules | `ui.state` → `state`, `ui.components` → `components` |
| 6.3 | Update package declarations (47 files) | New pattern |
| 6.4 | Update internal imports within Chat | Consistent |
| 6.5 | Move files | Files moved |
| 6.6 | Update build.gradle.kts | Updated |
| 6.7 | Update test files (`src/test/`) | Test packages updated |
| 6.8 | Update cross-module imports | No old refs |
| 6.9 | Verify build and tests | SUCCESS, tests pass |
| 6.10 | Cleanup and commit | Atomic commit |

### Phase 7: RAG Module
**Files: 78 | Risk: High**

| Task | Description | Validation |
|------|-------------|------------|
| 7.1 | Create directory structure | `data/`, `handlers/`, `clustering/`, `domain/`, `embeddings/`, `parser/`, `search/`, `cache/`, `ui/` |
| 7.2 | Apply subpackage flattening | `data.handlers` → `handlers`, `data.clustering` → `clustering` |
| 7.3 | Update package declarations (78 files) | New pattern |
| 7.4 | Update internal imports | Consistent |
| 7.5 | Move files | Files moved |
| 7.6 | Update build.gradle.kts | Updated |
| 7.7 | Update cross-module imports | No old refs |
| 7.8 | Verify build | SUCCESS |
| 7.9 | Cleanup and commit | Atomic commit |

### Phase 8: LLM Module
**Files: 103 | Risk: High (largest module)**

| Task | Description | Validation |
|------|-------------|------------|
| 8.1 | Analyze subpackage structure | Document current layout |
| 8.2 | Create directory structure | `api/`, `domain/`, etc. |
| 8.3 | Update package declarations (103 files) | New pattern |
| 8.4 | Update internal imports | Consistent |
| 8.5 | Move files | Files moved |
| 8.6 | Update build.gradle.kts | Updated |
| 8.7 | Update cross-module imports | No old refs |
| 8.8 | Verify build | SUCCESS |
| 8.9 | Cleanup and commit | Atomic commit |

### Phase 9: Shared/NLU Module
**Files: 66 | Risk: High (shared dependency)**

| Task | Description | Validation |
|------|-------------|------------|
| 9.1 | Create directory structure | `embeddings/`, `inference/`, `locale/` |
| 9.2 | Update package declarations | `com.augmentalis.nlu.*` |
| 9.3 | Move files | Files moved |
| 9.4 | Update build.gradle.kts | Updated |
| 9.5 | **CRITICAL:** Update ALL modules that import NLU | Chat, RAG, Actions, App |
| 9.6 | Verify full project build | SUCCESS |
| 9.7 | Cleanup and commit | Atomic commit |

### Phase 10: App Module Updates
**Files: ~25 | Risk: Medium**

| Task | Description | Validation |
|------|-------------|------------|
| 10.1 | Update MainActivity.kt imports | No old refs |
| 10.2 | Update Navigation imports | No old refs |
| 10.3 | Update Hilt DI modules | No old refs |
| 10.4 | Update ViewModels | No old refs |
| 10.5 | Update AndroidManifest.xml if needed | Valid |
| 10.6 | Verify full build | `./gradlew :app:assembleDebug` SUCCESS |
| 10.7 | Commit | Atomic commit |

### Phase 11: Final Validation
**Duration estimate: N/A (verification)**

| Task | Description | Validation |
|------|-------------|------------|
| 11.1 | Run full unit test suite | Pass count matches baseline |
| 11.2 | Verify no orphaned files with old packages | `grep "ava.features"` returns 0 |
| 11.3 | Verify no empty directories | `find -type d -empty` returns 0 |
| 11.4 | Compare APK size | Within 1% of baseline |
| 11.5 | Create final merge commit | All changes squashed or preserved |
| 11.6 | Update documentation | README, CLAUDE.md if needed |

---

## Package Transformation Summary

### Before → After

```
com.augmentalis.ava.features.chat.ui.state    → com.augmentalis.chat.state
com.augmentalis.ava.features.chat.ui.components → com.augmentalis.chat.components
com.augmentalis.ava.features.rag.data.handlers → com.augmentalis.rag.handlers
com.augmentalis.ava.features.llm.api          → com.augmentalis.llm.api
com.augmentalis.ava.features.nlu.embeddings   → com.augmentalis.nlu.embeddings
```

### Unchanged (core/* packages)

```
com.augmentalis.ava.core.data      → UNCHANGED
com.augmentalis.ava.core.domain    → UNCHANGED
com.augmentalis.ava.core.theme     → UNCHANGED
com.augmentalis.ava.core.common    → UNCHANGED
```

---

## Time Estimates

| Execution Mode | Estimated Duration | Notes |
|----------------|-------------------|-------|
| Sequential | 4-6 hours | One module at a time, full verification |
| Parallel (Swarm) | 1-2 hours | 3-4 agents working on independent modules |
| Savings | 3-4 hours | 60-70% time reduction |

### Swarm Distribution (if parallel)

| Agent | Modules | Files |
|-------|---------|-------|
| Agent 1 | memory, WakeWord, Teach | 37 |
| Agent 2 | Actions, Chat | 87 |
| Agent 3 | RAG | 78 |
| Agent 4 | LLM, NLU | 169 |
| Coordinator | App module, Final validation | ~25 |

---

## Risk Matrix

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Hilt DI compilation errors | Medium | High | Check @Module annotations before moving |
| Missing import update | Medium | Medium | Run full grep after each module |
| Test failures | Low | Medium | Run tests after each phase |
| Reflection breaking | Low | High | Search for Class.forName, document |
| ProGuard rules | Low | High | Check proguard-rules.pro |

---

## Rollback Plan

```bash
# If migration fails at any point:
git stash
git checkout main -- Modules/
git checkout main -- android/apps/ava/

# Or revert specific module:
git checkout HEAD~1 -- Modules/AVA/{module}/
```

---

## Success Criteria

| Criterion | Target | Validation Command |
|-----------|--------|-------------------|
| Build passes | SUCCESS | `./gradlew :app:assembleDebug` |
| Unit tests pass | Same count as baseline | `./gradlew testDebugUnitTest` |
| No old packages | 0 references | `grep -r "ava.features" --include="*.kt" \| wc -l` |
| Import depth reduced | Avg 4 segments | Package analysis script |
| APK size unchanged | ±1% | Compare APK sizes |

---

## Next Steps

1. **Create tasks?** → Will generate TodoWrite items for tracking
2. **Proceed to implement?** → Will execute Phase 1 immediately

---

**Plan Version:** 1.0
**Created:** 2025-12-18
**Spec Source:** `Docs/AVA/instructions/AVA-Instruction-PackageRestructure-251218-V1.md`
