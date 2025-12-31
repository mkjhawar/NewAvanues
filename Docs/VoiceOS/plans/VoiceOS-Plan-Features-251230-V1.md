# VoiceOS Feature Implementation Plan

**Document:** VoiceOS-Plan-Features-251230-V1.md
**Created:** 2025-12-30
**Status:** Ready for Implementation
**Branch:** VoiceOS-Development

---

## Overview

| Metric | Value |
|--------|-------|
| **Platforms** | Android |
| **Total Phases** | 3 |
| **Total Tasks** | 18 |
| **Swarm Recommended** | No (single platform) |
| **Dependencies** | Existing VoiceOS infrastructure |

### Features to Implement

1. **Clickable Statistics** - Drill-down UI for learned apps/screens/commands
2. **Hierarchy Map Generator** - ASCII/JSON/Mermaid export of app navigation
3. **NLU Context Enhancement** - Non-actionable text capture for LLM navigation

---

## Phase 1: Clickable Statistics Feature

**Priority:** P0 (User-facing, immediate value)
**Estimated Tasks:** 7
**Design Doc:** `VoiceOS-Plan-ClickableStats-251230-V1.md`

### Data Layer (No UI Changes)

| # | Task | File | Effort |
|---|------|------|--------|
| 1.1 | Add data classes (LearnedAppSummary, ScreenDetail, CommandSummary, LearnedAppDetail) | `HomeViewModel.kt` | 1h |
| 1.2 | Update HomeUiState with drill-down fields | `HomeViewModel.kt` | 30m |
| 1.3 | Implement loadLearnedAppSummaries() | `HomeViewModel.kt` | 1h |
| 1.4 | Implement loadAppDetail() | `HomeViewModel.kt` | 1h |
| 1.5 | Implement resolveAppName() helper | `HomeViewModel.kt` | 30m |
| 1.6 | Add show/hide/select/back methods | `HomeViewModel.kt` | 30m |
| 1.7 | Update Koin module with PackageManager | `AppModule.kt` | 15m |

### UI Components

| # | Task | File | Effort |
|---|------|------|--------|
| 1.8 | Add new imports for BottomSheet, LazyColumn | `HomeScreen.kt` | 15m |
| 1.9 | Create ClickableStatRow composable | `HomeScreen.kt` | 30m |
| 1.10 | Update StatisticsCard with onAppsLearnedClick | `HomeScreen.kt` | 30m |
| 1.11 | Create LearnedAppItem composable | `HomeScreen.kt` | 45m |
| 1.12 | Create LearnedAppsBottomSheet composable | `HomeScreen.kt` | 1h |
| 1.13 | Create ScreenDetailCard composable | `HomeScreen.kt` | 45m |
| 1.14 | Create AppDetailBottomSheet composable | `HomeScreen.kt` | 1h |

### Integration

| # | Task | File | Effort |
|---|------|------|--------|
| 1.15 | Add bottom sheet states to HomeScreen | `HomeScreen.kt` | 30m |
| 1.16 | Connect clicks and navigation flow | `HomeScreen.kt` | 30m |
| 1.17 | Unit tests for ViewModel methods | `HomeViewModelTest.kt` | 1h |

**Phase 1 Total:** ~11 hours

---

## Phase 2: Hierarchy Map Generator

**Priority:** P1 (Developer/Power user feature)
**Estimated Tasks:** 8
**Design Doc:** `VoiceOS-Plan-HierarchyMap-251230-V1.md`

### Core Generator

| # | Task | File | Effort |
|---|------|------|--------|
| 2.1 | Create HierarchyModels.kt data classes | `learnapp/hierarchy/HierarchyModels.kt` | 1h |
| 2.2 | Create HierarchyMapGenerator.kt scaffold | `learnapp/hierarchy/HierarchyMapGenerator.kt` | 1h |
| 2.3 | Implement generateHierarchy() | `HierarchyMapGenerator.kt` | 2h |
| 2.4 | Implement toAsciiTree() | `HierarchyMapGenerator.kt` | 1.5h |
| 2.5 | Implement toJson() | `HierarchyMapGenerator.kt` | 1h |
| 2.6 | Implement toMermaid() | `HierarchyMapGenerator.kt` | 1h |

### UI Integration

| # | Task | File | Effort |
|---|------|------|--------|
| 2.7 | Add "View Hierarchy" button to AppDetailBottomSheet | `HomeScreen.kt` | 1h |
| 2.8 | Create HierarchyViewerDialog | `HomeScreen.kt` or new file | 1.5h |
| 2.9 | Implement export to file functionality | `HierarchyMapGenerator.kt` | 1h |
| 2.10 | Add share intent support | `HierarchyMapGenerator.kt` | 30m |

### Testing

| # | Task | File | Effort |
|---|------|------|--------|
| 2.11 | Unit tests for generator | `HierarchyMapGeneratorTest.kt` | 1.5h |
| 2.12 | Integration tests with real database | `HierarchyMapIntegrationTest.kt` | 1h |

**Phase 2 Total:** ~13 hours

---

## Phase 3: NLU Context Enhancement

**Priority:** P2 (Future LLM integration)
**Estimated Tasks:** 6
**Design Doc:** `VoiceOS-Plan-NLUContext-251230-V1.md` (pending)

### Screen Context Capture

| # | Task | File | Effort |
|---|------|------|--------|
| 3.1 | Add non-actionable text extraction to UIScrapingEngine | `UIScrapingEngine.kt` | 2h |
| 3.2 | Add screenContext field to ScreenContextDTO | `ScreenContextDTO.kt` | 30m |
| 3.3 | Update SQLDelight schema for screen_context text | `VoiceOS.sq` | 1h |
| 3.4 | Implement context summarization (token-efficient) | `UIScrapingEngine.kt` | 1.5h |

### LLM Integration

| # | Task | File | Effort |
|---|------|------|--------|
| 3.5 | Update AIContextSerializer to include screen text | `AIContextSerializer.kt` | 1h |
| 3.6 | Update AVUQuantizerIntegration prompts | `AVUQuantizerIntegration.kt` | 1h |
| 3.7 | Add navigation context to LLM prompts | `AIContext.kt` | 1h |

### Testing

| # | Task | File | Effort |
|---|------|------|--------|
| 3.8 | Test context capture with real apps | Manual testing | 1h |

**Phase 3 Total:** ~9 hours

---

## Summary

| Phase | Feature | Tasks | Hours | Priority |
|-------|---------|-------|-------|----------|
| 1 | Clickable Statistics | 17 | 11h | P0 |
| 2 | Hierarchy Map Generator | 12 | 13h | P1 |
| 3 | NLU Context Enhancement | 8 | 9h | P2 |
| **Total** | | **37** | **33h** | |

---

## Implementation Order

```
Phase 1: Clickable Statistics (P0)
├── 1.1-1.7: Data layer (ViewModel changes)
├── 1.8-1.14: UI components (Composables)
└── 1.15-1.17: Integration & testing

Phase 2: Hierarchy Map Generator (P1)
├── 2.1-2.6: Core generator implementation
├── 2.7-2.10: UI integration
└── 2.11-2.12: Testing

Phase 3: NLU Context (P2)
├── 3.1-3.4: Screen context capture
├── 3.5-3.7: LLM integration
└── 3.8: Testing
```

---

## Files to Modify/Create

### Modify

| File | Changes |
|------|---------|
| `HomeViewModel.kt` | Data classes, UI state, methods |
| `HomeScreen.kt` | Bottom sheets, composables |
| `AppModule.kt` | Koin DI updates |
| `UIScrapingEngine.kt` | Non-actionable text capture |
| `AIContextSerializer.kt` | Screen context export |
| `AVUQuantizerIntegration.kt` | Updated prompts |

### Create

| File | Purpose |
|------|---------|
| `learnapp/hierarchy/HierarchyModels.kt` | Hierarchy data classes |
| `learnapp/hierarchy/HierarchyMapGenerator.kt` | Map generation logic |
| `HomeViewModelTest.kt` | ViewModel unit tests |
| `HierarchyMapGeneratorTest.kt` | Generator unit tests |

---

## Dependencies

All phases use existing infrastructure:

| Dependency | Status |
|------------|--------|
| VoiceOSDatabaseManager | EXISTS |
| IScrapedAppRepository | EXISTS |
| IScreenContextRepository | EXISTS |
| IGeneratedCommandRepository | EXISTS |
| IScreenTransitionRepository | EXISTS |
| NavigationGraph | EXISTS |
| AIContextSerializer | EXISTS |

**No new external dependencies required.**

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Large apps cause UI lag | Use LazyColumn, background loading |
| Bottom sheet performance | Use `skipPartiallyExpanded = true` |
| Database query slowness | Run all queries on Dispatchers.IO |
| Memory with many elements | Pagination for 1000+ commands |

---

## Commit Strategy

| Phase | Commit Message |
|-------|----------------|
| 1 | `feat(voiceos): add clickable statistics drill-down UI` |
| 2 | `feat(voiceos): add hierarchy map generator with ASCII/JSON/Mermaid export` |
| 3 | `feat(voiceos): enhance NLU context with non-actionable text capture` |

---

## Next Steps

1. **Approve plan**
2. **Run `/i.implement .phase 1`** to start Phase 1
3. **Build and test** after each phase
4. **Commit** after each phase passes tests

---

**Approval:**

| Role | Name | Date |
|------|------|------|
| Plan Author | Claude Code | 2025-12-30 |
| Technical Review | Pending | |
| Implementation | Pending | |
