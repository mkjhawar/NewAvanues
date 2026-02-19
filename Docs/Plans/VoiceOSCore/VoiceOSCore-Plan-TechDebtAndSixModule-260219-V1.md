# Implementation Plan: Tech Debt + Six-Module Completion
**Date:** 2026-02-19 | **Branch:** Cockpit-Development | **Mode:** .yolo .swarm

## Overview
5 work items prioritized by code proximity and impact. Tech debt items cluster in VoiceOSCore (shared files), BOM cleanup is mechanical, six-module work continues existing plans.

**Strategy:** VoiceOSCore proximity cluster → BOM cleanup → Six-module remaining

---

## Phase 1: VoiceOSCore Tech Debt Cluster (3 items, high proximity)

### 1.1 Ordinal Command Fix (P0 — Broken Functionality)
**Problem:** `generateListIndexCommands()` in `CommandGenerator.kt:205` filters by `listIndex >= 0`, rejecting ALL non-target app elements. Ordinals ("first", "second") never generated for general apps. Target apps map ordinals to extraction index instead of visual position.

**Fix:**
- Remove `listIndex >= 0` filter at line 205
- Replace `.groupBy { it.listIndex }` with spatial sorting (top→left)
- Use `visualIndex` (like `generateNumericCommands()` already does correctly)
- Add `"visualIndex"` to metadata

**Files:** `Modules/VoiceOSCore/src/commonMain/kotlin/.../command/CommandGenerator.kt` (lines 193-260)
**Complexity:** Low (20 lines, pattern exists in same file)
**Risk:** Low — follows proven pattern from numeric commands

### 1.2 CommandMatcher Fuzzy Scan Optimization (P1 — Performance)
**Problem:** O(n log n) sort on every cache miss, no similarity caching, no early exit.

**Fix (3-layer):**
1. **Early exit:** If any score ≥ 0.95, return immediately (skip sort)
2. **LRU cache:** Cache (input, threshold) → result, ~100 entries
3. **Pre-filter:** Use word intersection to reduce candidate set before scoring

**Files:** `Modules/VoiceOSCore/src/commonMain/kotlin/.../command/CommandMatcher.kt` (lines 46-136, 192-220)
**Complexity:** Medium (50 lines, cache + early exit + pre-filter)
**Risk:** Low — additive optimization, no behavioral change

### 1.3 Migrate runBlocking → Suspend Variants (P1 — Thread Safety)
**Problem:** 11 call sites use `runBlocking` for CommandRegistry methods that now have suspend variants.

**Migration tiers:**
- **Tier 1 (Easy, 4 sites):** VoiceOSAccessibilityService lines 868, 891, 919, 927 — already in `serviceScope.launch`, just swap method names
- **Tier 2 (Medium, 5 sites):** CommandOrchestrator lines 75, 158, 225, 234, 243 — make functions suspend
- **Tier 3 (Medium, 2 sites):** ActionCoordinator lines 206, 214 — make methods suspend, fix inconsistency with `dispose()` which already uses `clearSuspend()`

**Files:**
- `VoiceOSAccessibilityService.kt` (4 sites)
- `CommandOrchestrator.kt` (5 sites)
- `ActionCoordinator.kt` (2 sites)
**Complexity:** Medium (11 sites, cascading suspend propagation)
**Risk:** Medium — suspend propagation may touch callers; test on-device

---

## Phase 2: Build System Cleanup

### 2.1 Hardcoded BOM Cleanup (P1 — Build Health)
**Problem:** 10 build.gradle.kts files use hardcoded `compose-bom` versions instead of `libs.compose.bom`.

| File | Current Version | Status |
|------|----------------|--------|
| Modules/DeviceManager | 2024.12.01 | Matches catalog, just hardcoded |
| Modules/AvidCreator | 2024.02.00 | 10 months outdated |
| Modules/AI/RAG | 2023.10.01 | 14 months outdated |
| android/apps/ava-legacy | 2023.10.01 | 14 months outdated |
| android/apps/VoiceRecognition | 2024.06.00 | 6 months outdated |
| android/apps/VoiceUI | 2024.02.00 | 10 months outdated |
| Modules/VoiceDataManager | 2024.06.00 | 6 months outdated |
| Modules/VoiceKeyboard | 2024.06.00 | 6 months outdated |
| Modules/LicenseManager | 2024.02.00 | 10 months outdated |
| Modules/AI/Teach | 2024.06.00 | 6 months outdated |

**Fix:** Replace all with `platform(libs.compose.bom)`. Remove explicit Compose artifact versions (let BOM manage).

**Catalog reference:** `compose-bom = "2024.12.01"` in `gradle/libs.versions.toml`
**Complexity:** Low (mechanical find-replace, 10 files)
**Risk:** Low — only version alignment, no API changes

---

## Phase 3: Six-Module Completion (Remaining Work)

### Current Status (from spec analysis)

| Module | % Done | Remaining Work |
|--------|--------|----------------|
| AnnotationAvanue | 85% | iOS controller only |
| ImageAvanue | 70% | iOS controller only |
| VideoAvanue | 30% | Android UI (ExoPlayer), Desktop UI, iOS |
| RemoteCast | 60% | Foreground service wiring, mDNS discovery |
| AI | 40% | ActionCoordinator dispatch routing, provider dedup |
| AVA Overlay | 70% | Directory restructure to KMP source sets |

### 3.1 VideoAvanue Android UI (P0 — Missing Core Feature)
- ExoPlayer Composable integration with AndroidView
- VideoControlBar (timeline, speed, fullscreen, loop toggles)
- VideoGalleryScreen (MediaStore video grid)
- Cockpit ContentRenderer Video branch

### 3.2 AI ActionCoordinator Dispatch (P1 — Low Risk, High Value)
- Wire 5 AI command types into handler flow
- Already designed, just needs dispatch routing in ActionCoordinator

### 3.3 RemoteCast Service Wiring (P2 — Complex)
- CastCaptureService foreground service with MediaProjection
- Connect to existing MJPEG-over-TCP pipeline

### 3.4 AVA Overlay Directory Restructure (P2 — Cleanup)
- Move `src/main/java/` → `src/androidMain/kotlin/`
- Verify theme migration completeness

### 3.5 iOS Implementations (P3 — Future)
- AnnotationAvanue, ImageAvanue, VideoAvanue, RemoteCast iOS controllers
- Deferred until Android/Desktop fully stable

---

## Execution Order (Optimized for Code Proximity)

| Step | Item | Est. Lines | Proximity Benefit |
|------|------|-----------|-------------------|
| 1 | 1.1 Ordinal fix | ~30 | Opens CommandGenerator.kt |
| 2 | 1.2 CommandMatcher opt | ~60 | Same package, adjacent file |
| 3 | 1.3 runBlocking migration | ~40 | Same module, shared callers |
| 4 | 2.1 BOM cleanup | ~30 | Independent, mechanical |
| 5 | 3.2 AI dispatch | ~20 | ActionCoordinator already open from 1.3 |
| 6 | 3.1 VideoAvanue UI | ~300 | Separate module, largest item |
| 7 | 3.3 RemoteCast service | ~100 | Separate module |
| 8 | 3.4 AVA restructure | ~50 | Directory moves |

**Swarm candidates:** Steps 1-3 (VoiceOSCore cluster) + Step 4 (BOM) can run in parallel
**Sequential dependencies:** Step 5 depends on Step 3 (ActionCoordinator changes)

---

## Commit Strategy (.yolo mode)
- Commit after each phase completes
- Phase 1: `fix(VoiceOSCore): Ordinal commands + CommandMatcher cache + suspend migration`
- Phase 2: `build: Migrate hardcoded compose-bom to version catalog`
- Phase 3: Per-module commits
