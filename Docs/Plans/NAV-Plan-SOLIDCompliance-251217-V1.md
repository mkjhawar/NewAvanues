# Implementation Plan: SOLID Compliance & Validation Fixes

## Overview
- **Scope:** WebAvanue-Development branch
- **Focus:** SOLID violations + validation items by proximity
- **Swarm Recommended:** No (focused scope)
- **Estimated:** 12 tasks, 4-6 hours

---

## Priority Matrix (By Proximity to WebAvanue)

| Priority | Item | Module | Type |
|----------|------|--------|------|
| P1 | BottomCommandBar.kt (2,282 lines) | WebAvanue | SOLID-SRP |
| P1 | BrowserScreen.kt (1,481 lines) | WebAvanue | SOLID-SRP |
| P1 | TabViewModel.kt (1,383 lines) | WebAvanue | SOLID-SRP |
| P1 | BrowserRepositoryImpl.kt (1,354 lines) | WebAvanue | SOLID-SRP |
| P2 | Missing CLAUDE.md | WebAvanue/coredata | Validation |
| P2 | Missing CLAUDE.md | WebAvanue/universal | Validation |
| P3 | AVAMagic missing CLAUDE.md | Modules/AVAMagic | Validation |
| P3 | Shared missing CLAUDE.md | Modules/Shared | Validation |

---

## Phase 1: WebAvanue Validation Fixes (Quick Wins)

### Task 1.1: Create CLAUDE.md for WebAvanue/coredata
```
Modules/WebAvanue/coredata/.claude/CLAUDE.md
```
- Module scope definition
- Inherit from parent WebAvanue CLAUDE.md
- Define coredata-specific rules

### Task 1.2: Create CLAUDE.md for WebAvanue/universal
```
Modules/WebAvanue/universal/.claude/CLAUDE.md
```
- UI module scope
- Compose/KMP patterns
- Testing requirements

---

## Phase 2: WebAvanue SOLID Refactoring

### Task 2.1: Split BottomCommandBar.kt (2,282 → <500 each)

**Current:** God class with all command bar logic

**Extract to:**
| New File | Responsibility | Est. Lines |
|----------|----------------|------------|
| `CommandBarState.kt` | State management | 200 |
| `CommandBarActions.kt` | Action handlers | 300 |
| `CommandBarUI.kt` | Composable UI | 400 |
| `CommandBarAnimations.kt` | Animation logic | 200 |
| `CommandInputHandler.kt` | Input processing | 300 |

### Task 2.2: Split BrowserScreen.kt (1,481 → <400 each)

**Extract to:**
| New File | Responsibility | Est. Lines |
|----------|----------------|------------|
| `BrowserScreenState.kt` | Screen state | 150 |
| `BrowserScreenContent.kt` | Main content | 350 |
| `BrowserScreenToolbar.kt` | Toolbar composable | 200 |
| `BrowserScreenDialogs.kt` | Dialog handlers | 250 |

### Task 2.3: Split TabViewModel.kt (1,383 → <400 each)

**Extract to:**
| New File | Responsibility | Est. Lines |
|----------|----------------|------------|
| `TabState.kt` | Tab state models | 150 |
| `TabOperations.kt` | Tab CRUD operations | 300 |
| `TabNavigationHandler.kt` | Navigation logic | 250 |
| `TabPersistenceHandler.kt` | Save/restore | 200 |

### Task 2.4: Split BrowserRepositoryImpl.kt (1,354 → <400 each)

**Extract to:**
| New File | Responsibility | Est. Lines |
|----------|----------------|------------|
| `HistoryRepository.kt` | History operations | 300 |
| `BookmarkRepository.kt` | Bookmark operations | 300 |
| `TabRepository.kt` | Tab persistence | 250 |
| `SettingsRepository.kt` | Settings storage | 200 |

---

## Phase 3: Other Validation Fixes (Lower Priority)

### Task 3.1: Create CLAUDE.md for Modules/AVAMagic
### Task 3.2: Create CLAUDE.md for Modules/Shared

---

## Execution Order

```
Phase 1 (15 min)     Phase 2 (3-4 hrs)           Phase 3 (15 min)
┌─────────────┐      ┌──────────────────────┐    ┌─────────────┐
│ Task 1.1    │      │ Task 2.1 (CommandBar)│    │ Task 3.1    │
│ Task 1.2    │  →   │ Task 2.2 (Browser)   │ →  │ Task 3.2    │
└─────────────┘      │ Task 2.3 (TabVM)     │    └─────────────┘
                     │ Task 2.4 (Repository)│
                     └──────────────────────┘
```

---

## Success Criteria

| Metric | Before | After | Target |
|--------|--------|-------|--------|
| Max file size | 2,282 | <500 | ✓ |
| CLAUDE.md coverage | 71% | 100% | ✓ |
| SOLID-SRP score | 60 | 85+ | ✓ |

---

## Time Estimates

| Mode | Duration | Notes |
|------|----------|-------|
| Sequential | 4-6 hours | One task at a time |
| Parallel (Swarm) | 2-3 hours | Phase 2 tasks in parallel |

---

## Commands to Execute

```bash
# Phase 1: Quick validation fixes
/i.implement Phase1 .yolo

# Phase 2: SOLID refactoring (recommend interactive)
/i.refactor .solid "BottomCommandBar"
/i.refactor .solid "BrowserScreen"
/i.refactor .solid "TabViewModel"
/i.refactor .solid "BrowserRepositoryImpl"

# Phase 3: Remaining validation
/i.repo .fix
```

---

**Created:** 2025-12-17
**Branch:** WebAvanue-Development
**Author:** Claude (IDEACODE)
