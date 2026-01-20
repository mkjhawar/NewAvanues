# Implementation Plan: Legacy Avanues Consolidation

**Date:** 2026-01-19 | **Version:** V1 | **Branch:** Legacy-Optimization

## Overview

| Attribute | Value |
|-----------|-------|
| **Platforms** | KMP (commonMain) |
| **Swarm Recommended** | No (linear dependencies) |
| **Estimated Tasks** | 18 |
| **Archival Required** | Yes |

---

## Phase 1: GlassmorphismUtils Consolidation (4 duplicate files → 1)

**Goal:** Consolidate 4 duplicate GlassmorphismUtils files in AvaMagic managers into single AvaUI location.

### Current Duplicates:
1. `managers/CommandManager/src/main/java/.../ui/GlassmorphismUtils.kt`
2. `managers/VoiceDataManager/src/main/java/.../ui/GlassmorphismUtils.kt`
3. `managers/LocalizationManager/src/main/java/.../ui/GlassmorphismUtils.kt`
4. `managers/LicenseManager/src/main/java/.../ui/GlassmorphismUtils.kt`

### Tasks:
| # | Task | Type |
|---|------|------|
| 1.1 | Read all 4 GlassmorphismUtils files and identify differences | Research |
| 1.2 | Create unified `AvaMagic/AvaUI/Foundation/src/commonMain/kotlin/.../GlassmorphismUtils.kt` | Create |
| 1.3 | Update CommandManager imports to use AvaUI/Foundation | Refactor |
| 1.4 | Update VoiceDataManager imports to use AvaUI/Foundation | Refactor |
| 1.5 | Update LocalizationManager imports to use AvaUI/Foundation | Refactor |
| 1.6 | Update LicenseManager imports to use AvaUI/Foundation | Refactor |
| 1.7 | Move original files to `archive/deprecated/managers-glassmorphism/` | Archive |

---

## Phase 2: Ocean Design System Migration (3 files)

**Goal:** Migrate Ocean Design components from Avanues to AvaMagic.

### Source Files:
- `Avanues/Web/common/webavanue/universal/.../OceanThemeExtensions.kt`
- `Avanues/Web/common/webavanue/universal/.../OceanDesignTokens.kt`
- `Avanues/Web/common/webavanue/universal/.../OceanComponents.kt`

### Tasks:
| # | Task | Type |
|---|------|------|
| 2.1 | Copy `OceanThemeExtensions.kt` → `AvaMagic/AvaUI/Theme/src/commonMain/kotlin/.../OceanThemeExtensions.kt` | Copy |
| 2.2 | Copy `OceanDesignTokens.kt` → `AvaMagic/AvaUI/DesignSystem/src/commonMain/kotlin/.../OceanDesignTokens.kt` | Copy |
| 2.3 | Copy `OceanComponents.kt` → `AvaMagic/AvaUI/Foundation/src/commonMain/kotlin/.../OceanComponents.kt` | Copy |
| 2.4 | Update package declarations in new files | Refactor |
| 2.5 | Archive originals to `archive/deprecated/Avanues-ocean-design/` | Archive |

---

## Phase 3: XR/AR Abstractions Migration (5 files)

**Goal:** Create new XR subsystem in AvaMagic with reusable XR abstractions.

### Source Files:
- `Avanues/Web/common/webavanue/universal/.../xr/CommonXRManager.kt`
- `Avanues/Web/common/webavanue/universal/.../xr/CommonCameraManager.kt` (if exists)
- `Avanues/Web/common/webavanue/universal/.../xr/CommonSessionManager.kt` (if exists)
- `Avanues/Web/common/webavanue/universal/.../xr/CommonPerformanceMonitor.kt` (if exists)
- `Avanues/Web/common/webavanue/universal/.../xr/CommonPermissionManager.kt` (if exists)

### Tasks:
| # | Task | Type |
|---|------|------|
| 3.1 | Create `AvaMagic/AvaUI/XR/` subsystem directory structure | Create |
| 3.2 | Create `AvaMagic/AvaUI/XR/build.gradle.kts` | Create |
| 3.3 | Create `AvaMagic/AvaUI/XR/README.md` | Create |
| 3.4 | Copy XR files to `AvaMagic/AvaUI/XR/src/commonMain/kotlin/.../` | Copy |
| 3.5 | Update package declarations in new files | Refactor |
| 3.6 | Archive originals to `archive/deprecated/Avanues-xr/` | Archive |

---

## Phase 4: Utilities Migration - DEFERRED

**Status:** DEFERRED - Gesture utilities are WebView-specific

### Decision:
- `GestureMapper.kt` - **KEEP in Avanues** or migrate to `/Modules/WebAvanue` later
- `GestureCoordinateResolver.kt` - **KEEP in Avanues** or migrate to `/Modules/WebAvanue` later
- `TransactionHelper.kt` - **TBD** (evaluate after P0-P2 complete)

### Rationale:
GestureMapper maps VoiceOS gestures to `window.AvanuesGestures.*` JavaScript calls - this is WebView/browser-specific functionality, not a general-purpose utility. It belongs with the WebAvanue browser module, not in the shared AvaMagic framework.

---

## Phase 5: Verify and Document

### Tasks:
| # | Task | Type |
|---|------|------|
| 5.1 | Update `AvaMagic/settings.gradle.kts` to include new modules (if needed) | Config |
| 5.2 | Run gradle sync to verify no compilation errors | Verify |
| 5.3 | Update `AI/CLASS-INDEX.ai.md` with new files | Docs |
| 5.4 | Update `AI/PLATFORM-INDEX.ai.md` with XR subsystem | Docs |
| 5.5 | Update migration analysis report with completion status | Docs |

---

## Archive Structure

```
archive/deprecated/
├── managers-glassmorphism/
│   ├── CommandManager-GlassmorphismUtils.kt
│   ├── VoiceDataManager-GlassmorphismUtils.kt
│   ├── LocalizationManager-GlassmorphismUtils.kt
│   └── LicenseManager-GlassmorphismUtils.kt
├── Avanues-ocean-design/
│   ├── OceanThemeExtensions.kt
│   ├── OceanDesignTokens.kt
│   └── OceanComponents.kt
└── Avanues-xr/
    ├── CommonXRManager.kt
    └── ... (other XR files)
```

**NOT Archived (staying in place):**
- `GestureMapper.kt` - WebView-specific, may move to `/Modules/WebAvanue`
- `GestureCoordinateResolver.kt` - WebView-specific, may move to `/Modules/WebAvanue`
- `TransactionHelper.kt` - TBD after P0-P2

---

## Conflict Analysis: Priority 3 Files vs VoiceOSCore

| Component | VoiceOSCore Equivalent | Conflict? | Notes |
|-----------|------------------------|-----------|-------|
| **GestureMapper** | `GestureHandler.kt` | **NO** | Different targets: WebView JS vs AccessibilityService |
| **GestureCoordinateResolver** | None | **NO** | Generic utility |
| **TransactionHelper** | None | **NO** | Generic utility |

**Conclusion:** No conflicts. These are complementary:
- `GestureMapper` = WebView gesture injection
- `GestureHandler` = Voice command → GestureConfig
- `AndroidGestureDispatcher` = AccessibilityService execution

---

## Exit Criteria

- [x] All 4 GlassmorphismUtils consolidated to single location (Phase 1) ✅ COMPLETED 2026-01-19
- [x] All manager imports updated and working (Phase 1) ✅ COMPLETED 2026-01-19
- [x] Ocean Design files in AvaMagic/AvaUI (Phase 2) ✅ COMPLETED 2026-01-19
- [x] XR subsystem created in AvaMagic/AvaUI/XR (Phase 3) ✅ COMPLETED 2026-01-19
- [x] All originals archived (not deleted) ✅ COMPLETED 2026-01-19
- [ ] Gradle sync succeeds (needs verification)
- [ ] MasterDocs updated

**Deferred (Phase 4):**
- [ ] Evaluate GestureMapper/GestureCoordinateResolver for `/Modules/WebAvanue`
- [ ] Evaluate TransactionHelper for `AvaMagic/Core/database`

---

## Notes

- **Copy, Don't Move:** All files are copied, originals archived
- **No Deletion:** Original Avanues module remains functional
- **Import Updates:** WebAvanue app may need import path updates (Phase 6 if needed)
