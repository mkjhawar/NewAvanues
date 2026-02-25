# Chapter 112: Simplified Voice-First UI Shells

**Module**: Cockpit
**Branch**: `AvanueViews`
**Commit**: `d398c6aad`
**Date**: 2026-02-25
**Status**: Phase 1 Complete (commonMain + Android voice wiring)

---

## 1. Overview

The Cockpit UI previously exposed **15 raw layout modes** and a **13-state CommandBar hierarchy** to users, creating significant cognitive overload — especially on smart glasses with limited screen real estate. This chapter documents the Simplified Voice-First UI system that addresses this by introducing:

1. **ArrangementIntent** — 4 user-facing intents that auto-resolve to optimal layout modes
2. **ContextualActionProvider** — flat single-level action system replacing the hierarchical CommandBar
3. **Three new home screen shells** — alternative variations alongside the classic Dashboard
4. **Voice command integration** — arrangement intents + shell switching + shell-specific navigation

### Design Principles

- **Voice is THE primary input** — UI exists to support voice, not replace it
- **Progressive disclosure** — show only what's needed, reveal on demand
- **Context over chrome** — content fills the screen; navigation is invisible until summoned
- **One action, one screen** — each screen has ONE clear purpose
- **Glanceable** — smart glasses users can understand state in <2 seconds

---

## 2. ArrangementIntent (Layout Abstraction)

**File**: `Modules/Cockpit/src/commonMain/.../model/ArrangementIntent.kt`

Instead of exposing 15 raw `LayoutMode` values, users interact with 4 natural intents:

| Intent | Voice Command | Description | Resolves To |
|--------|--------------|-------------|-------------|
| FOCUS | "focus" | Single frame fills screen | FULLSCREEN |
| COMPARE | "compare" | Two frames side by side | SPLIT_LEFT, T_PANEL (phone) |
| OVERVIEW | "overview" | All frames in auto grid | GRID, MOSAIC, ROW (glass) |
| PRESENT | "present" | Showcase/carousel mode | CAROUSEL, TRIPTYCH |

### IntentResolver

The `IntentResolver` object maps intents to optimal `LayoutMode` based on:
- Frame count (1 → FULLSCREEN regardless of intent)
- Display profile (phone → T_PANEL for Compare, glass → ROW for Overview)
- Spatial availability (5 frames + spatial → SPATIAL_DICE)

```kotlin
val mode = IntentResolver.resolve(
    intent = ArrangementIntent.COMPARE,
    frameCount = 2,
    displayProfile = DisplayProfile.PHONE,
)
// Returns LayoutMode.T_PANEL (top/bottom split, better than side-by-side on phone)
```

### inferIntent()

Reverse mapping: given an existing `LayoutMode`, infer which `ArrangementIntent` the user likely meant. Used when restoring sessions or manually changing layout mode.

---

## 3. SimplifiedShellMode

**File**: `Modules/Cockpit/src/commonMain/.../model/SimplifiedShellMode.kt`

```kotlin
enum class SimplifiedShellMode {
    CLASSIC,       // Original DashboardLayout (tile grid + full CommandBar)
    AVANUE_VIEWS,  // Ambient card stream (Google Now-inspired)
    LENS,          // Command palette (Spotlight/Raycast-inspired)
    CANVAS,        // Spatial zen canvas (Figma/Vision Pro-inspired)
}
```

Default: `LENS` (lowest learning curve, best voice mapping)

The shell mode is stored in `CockpitViewModel.shellMode` StateFlow and controls which home screen variation renders when `layoutMode == DASHBOARD`.

---

## 4. Three Shell Variations

### 4.1 AvanueViews — Ambient Card Stream

**File**: `Modules/Cockpit/src/commonMain/.../ui/AvanueViewsStreamLayout.kt`

Philosophy: *"The UI whispers to you — cards surface when relevant, fade when done."*

**Card Priority System**:
- **P0 Active**: Most recent session (1 card max)
- **P1 Ambient**: Open sessions + top modules (2-3 cards)
- **P2 Suggestion**: Speech metrics, AI hints
- **P3 Ghost**: Voice command discovery hints

**Responsive Layout**:
| Display Profile | Columns | FAB Position |
|----------------|---------|-------------|
| Glass (all) | 1 | N/A (voice only) |
| Phone | 1 | Bottom center |
| Tablet | 2 | Bottom end |
| Glass HD | 2 | Bottom end |

**Key Components**:
- `StreamCard` — adapts visual weight to priority (P0 is larger)
- `GhostHintCard` — teaches voice commands ("Open [module name]")
- `StreamHeader` — minimal AVA orb indicator

### 4.2 Lens — Command Palette

**File**: `Modules/Cockpit/src/commonMain/.../ui/LensLayout.kt`

Philosophy: *"Everything is one voice command or one keystroke away."*

**Architecture**:
- Central `LensBar` (BasicTextField) with "What next?" placeholder
- `LensResult` model with categories: MODULES, RECENT, COMMANDS
- `buildLensResults()` performs fuzzy matching across modules and sessions
- `LensGhostHints` show recent items when Lens is inactive
- Voice indicator (bottom-right) shows listening state

**Responsive Layout**:
| Display Profile | Lens Width | Padding |
|----------------|-----------|---------|
| Glass Micro | 95% | 8dp |
| Phone | 90% (max 400dp) | 16dp |
| Tablet | 60% (max 500dp) | 32dp |
| Glass HD | 80% (max 600dp) | 16dp |

### 4.3 Canvas — Spatial Zen

**File**: `Modules/Cockpit/src/commonMain/.../ui/ZenCanvasLayout.kt`

Philosophy: *"Your workspace is an infinite calm canvas. Content exists in space."*

**Architecture**:
- Infinite 2D plane with pinch-zoom (0.5x – 4.0x) and pan gestures
- Module islands positioned in honeycomb spiral layout
- `DotGridBackground` provides subtle spatial context
- `ZoomRail` slider + buttons at bottom for manual zoom control
- `ZoomLevelBadge` shows current level ("Level 1 · 100%")
- `GlassCanvasHud` — minimal HUD for smart glasses

**Island Positioning**: `computeIslandPositions()` uses honeycomb spiral:
- Center island at (0, 0)
- Ring 1: 6 islands at 60° intervals, 1x radius
- Ring 2: 6 islands at 30° offset, 2x radius
- Ensures maximum spacing with organic feel

---

## 5. ContextualActionBar

**File**: `Modules/Cockpit/src/commonMain/.../ui/ContextualActionBar.kt`

Replaces the full CommandBar for simplified shells. Features:
- Flat horizontal row of `AvanueChip` actions (5-6 per content type)
- "More" chip opens the full `CommandBar` (fallback)
- AnimatedVisibility with slide/fade transitions
- Content-type-specific: web actions for web frames, note actions for notes, etc.

### ContextualActionProvider

**File**: `Modules/Cockpit/src/commonMain/.../model/ContextualActionProvider.kt`

Single-level action system:
- `topActionsForContent(typeId)` → 5-6 most-used actions
- `allActionsForContent(typeId)` → full grouped list (Content, Frame, Layout, Tools)
- `searchActions(typeId, query)` → fuzzy search for Lens integration

Supports all 17 content types (Web, PDF, Image, Video, Note, Camera, Whiteboard, Terminal, Map, etc.)

---

## 6. Voice Commands

### New CommandActionTypes (commonMain)

| Action Type | Voice Phrase | Description |
|-------------|-------------|-------------|
| LAYOUT_FOCUS | "focus" | Single frame fills screen |
| LAYOUT_COMPARE | "compare" | Two frames side by side |
| LAYOUT_OVERVIEW | "overview" | All frames in grid |
| LAYOUT_PRESENT | "present" | Showcase/carousel |
| SHELL_CLASSIC | "classic mode" | Switch to tile Dashboard |
| SHELL_AVANUE_VIEWS | "stream mode" | Switch to card stream |
| SHELL_LENS | "lens mode" | Switch to command palette |
| SHELL_CANVAS | "canvas mode" | Switch to spatial canvas |
| STREAM_NEXT_CARD | "next card" | AvanueViews: next card |
| STREAM_PREVIOUS_CARD | "previous card" | AvanueViews: prev card |
| CANVAS_ZOOM_IN | "canvas zoom in" | Canvas: zoom in |
| CANVAS_ZOOM_OUT | "canvas zoom out" | Canvas: zoom out |

### Handler Wiring

- **Android**: `CockpitCommandHandler.kt` — phrases added, dispatches via `ModuleCommandCallbacks.cockpitExecutor`
- **iOS**: `IosCockpitCommandHandler.kt` — phrases added, dispatches via `IosCockpitControllerHolder`
- **Executor**: `executeCockpitCommand()` in `CockpitScreen.kt` — calls `IntentResolver.resolve()` for arrangement intents, `viewModel.setShellMode()` for shell switching

---

## 7. CockpitScreenContent Integration

The shell is wired via `when (state.shellMode)` in `CockpitScreenContent.kt`:

```kotlin
when (state.shellMode) {
    SimplifiedShellMode.CLASSIC -> DashboardLayout(...)
    SimplifiedShellMode.AVANUE_VIEWS -> AvanueViewsStreamLayout(...)
    SimplifiedShellMode.LENS -> LensLayout(...)
    SimplifiedShellMode.CANVAS -> ZenCanvasLayout(...)
}
```

The CommandBar switches based on shell mode:
- **Classic** or **Dashboard**: Full `CommandBar` (13-state hierarchy)
- **Simplified shells**: Flat `ContextualActionBar` (5-6 chips + More)

---

## 8. Files Created / Modified

### New Files (7)
| File | Lines | Purpose |
|------|-------|---------|
| `model/ArrangementIntent.kt` | 185 | 4 intents + IntentResolver |
| `model/ContextualActionProvider.kt` | 191 | Flat action system |
| `model/SimplifiedShellMode.kt` | 67 | Shell mode enum |
| `ui/AvanueViewsStreamLayout.kt` | 471 | Ambient card stream |
| `ui/ContextualActionBar.kt` | 111 | Flat action chip bar |
| `ui/LensLayout.kt` | 576 | Command palette |
| `ui/ZenCanvasLayout.kt` | 554 | Spatial zen canvas |

### Modified Files (14)
| File | Change |
|------|--------|
| `model/CockpitScreenState.kt` | Added `shellMode` parameter |
| `ui/CockpitScreenContent.kt` | Shell mode routing + ContextualActionBar |
| `viewmodel/CockpitViewModel.kt` | `shellMode` StateFlow + `setShellMode()` |
| `ui/CommandBar.kt` | Fix Sort icon, add FILE_ACTIONS |
| `ui/MinimizedTaskbar.kt` | Add missing File branch |
| `CommandActionType.kt` | 12 new cockpit action types |
| `CockpitCommandHandler.kt` | 10 new voice phrases |
| `IosCockpitCommandHandler.kt` | 10 new voice phrases + dispatch |
| `CockpitScreen.kt` (Android) | Arrangement intent + shell executor |

---

## 9. Remaining Work

| Phase | Description | Status |
|-------|-------------|--------|
| Phase 1 | commonMain models + shells + voice wiring | DONE |
| Phase 2 | Android platform (ContentRenderer wiring for shells) | Pending |
| Phase 3 | Web/JS platform shell rendering | Pending |
| Phase 4 | Settings UI for shell mode selection | Pending |
| Phase 5 | Glass-specific optimizations (paginated AvanueViews, voice-only Lens) | Pending |
| Phase 6 | Canvas: persistent island positions, drag-to-rearrange | Pending |
| Phase 7 | Lens: command execution (not just navigation), result preview pane | Pending |

---

## 10. Cognitive Load Comparison

| Metric | Before | After (Lens) |
|--------|--------|-------------|
| Home screen elements | 10+ tiles + sessions + templates | 1 search bar + 3 ghost hints |
| Layout choices | 15 raw modes | 4 intents (auto-resolved) |
| CommandBar depth | 3 levels (MAIN → FRAME → WEB) | 1 level (flat chips + More) |
| Interactive elements on home | ~25 | ~6 |
| Voice command discoverability | Hidden in command bar | Ghost text on every card/result |
| Smart glasses usability | Truncated 5-button bar | Full voice, 1 card at a time |

---

*Extends Chapter 97 (Cockpit SpatialVoice Multi-Window)*
*Related: Chapter 110 (Unified Command Architecture)*
*Plan: `Docs/Plans/CockpitUI/CockpitUI-Plan-SimplifiedVoiceFirstUI-260225-V1.md`*
