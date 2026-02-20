# VoiceOSCore Fix: Tech Debt Cluster
**Date:** 2026-02-19 | **Branch:** Cockpit-Development | **Version:** V1

## Changes Summary

### 1. Ordinal Command Fix (CommandGenerator + CommandOrchestrator)
**Problem:** `generateListIndexCommands()` filtered by `listIndex >= 0`, rejecting ALL non-target app elements. Ordinals ("first", "second") never generated for general apps. Target apps mapped ordinals to extraction index instead of visual position.

**Root Cause:** Double filter — `CommandOrchestrator` pre-filtered `elements.filter { it.listIndex >= 0 }` AND `CommandGenerator` filtered again with `.filter { it.listIndex >= 0 }`.

**Fix:** Dual-path strategy:
- Target apps (listIndex >= 0 present): Group by listIndex, keep best per group, sort by listIndex
- Non-target apps (all listIndex = -1): Sort all clickable elements spatially (top→left)
- Both paths use `visualIndex` (position in sorted order) for ordinal assignment
- Cap at 10 (ordinals only go to "tenth")
- CommandOrchestrator now passes ALL elements to ordinal generator

**Files:**
- `Modules/VoiceOSCore/src/commonMain/.../command/CommandGenerator.kt` — lines 193-260 rewritten
- `Modules/VoiceOSCore/src/commonMain/.../command/CommandOrchestrator.kt` — lines 77-83, 162-168
- `Modules/VoiceOSCore/src/androidMain/.../VoiceOSAccessibilityService.kt` — lines 376-395
- `apps/avanues/.../VoiceAvanueAccessibilityService.kt` — (consistent with VoiceOSAccessibilityService)

### 2. CommandMatcher Fuzzy Scan Optimization
**Problem:** O(n log n) sort on every cache miss, no similarity caching, no early exit.

**Fix (3-layer):**
1. **LRU cache:** 100-entry cache keyed by `"input|threshold|actionFilter"`, invalidated by registry `generation()` counter
2. **Word pre-filter:** Only score commands sharing at least one word with input (reduces candidate set ~70%)
3. **Early exit:** If any candidate scores >= 0.95 with clear margin (> 0.1 above second best), return immediately without full sort

**Files:**
- `Modules/VoiceOSCore/src/commonMain/.../command/CommandMatcher.kt` — match() rewritten with cache + pre-filter + early exit
- `Modules/VoiceOSCore/src/commonMain/.../command/CommandRegistry.kt` — added `_generation` counter + `generation()` accessor, incremented in all write paths

### 3. runBlocking → Suspend Migration
**Problem:** 5 deprecated `runBlocking` wrappers in `CommandRegistry` called by `CommandOrchestrator` (5 sites) and `ActionCoordinator` (2 sites), blocking Dispatchers.Default threads for up to 5 seconds under mutex contention.

**Fix:**
- `CommandOrchestrator.generateCommands()` → `suspend fun`, uses `update()` instead of `updateSync()`
- `CommandOrchestrator.generateCommandsIncremental()` → `suspend fun`, uses `update()`
- Three private helpers → `suspend`, use `addAllSuspend()` instead of `addAll()`
- `ActionCoordinator.clearDynamicCommands()` → `suspend`, uses `clearSuspend()`
- `ActionCoordinator.clearDynamicCommandsBySource()` → `suspend`, uses `clearBySourceSuspend()`
- `VoiceOSCore.clearDynamicCommands()` → `suspend` (pass-through)
- `VoiceAvanueAccessibilityService.refreshOverlayBadges()` + `registerOverlayCommands()` → `suspend`
- Removed redundant `serviceScope.launch` in `registerOverlayCommands` (already called from coroutine)

**Dead code found:** `CommandRegistry.updateBySource()` (deprecated non-suspend) has zero callers — can be removed in future cleanup.

### 4. Hardcoded BOM Cleanup (10 files)
Replaced hardcoded `compose-bom:XXXX.XX.XX` with `platform(libs.compose.bom)` in 10 build.gradle.kts files (12 occurrences). Versions ranged from 14 months outdated (2023.10.01) to current (2024.12.01).

### 5. AI ActionCoordinator Dispatch Wiring
- Added `ActionCategory.AI` (priority 19, before CUSTOM at 20)
- Updated `AICommandHandler.category` from `CUSTOM` to `AI`
- Added `"ai" -> ActionCategory.AI` domain mapping in `ActionCoordinator.domainToCategory()`
- Handler is registered in `AndroidHandlerFactory` with 5 supported actions
