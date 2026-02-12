# VoiceOSCore-Fix-KMPNumberingExecutor-260212-V1

**Module:** VoiceOSCore / Avanues App
**Type:** Fix
**Date:** 2026-02-12
**Version:** 1
**Branch:** VoiceOSCore-KotlinUpdate
**Status:** Implemented

---

## Summary

Wires the existing KMP `NumbersOverlayExecutor` interface with per-container numbering, replacing the flat global `avidToNumber` map in `OverlayStateManager`. This enables:

1. "numbers on/off/auto" voice commands through the KMP handler pipeline (`NumbersOverlayHandler`)
2. Per-container scoped numbering (handles multi-scroll-area screens)
3. Centralized numbering lifecycle logic in one executor
4. iOS port path: implement `NumbersOverlayExecutor` in Swift, get numbering free

## Problem

The overlay numbering system had three issues:

1. **Flat global numbering**: A single `avidToNumber` map in `OverlayStateManager` with no container scoping. Multi-scroll-area screens would have a single shared counter.
2. **Scattered reset logic**: App-change reset was in `DynamicCommandGenerator`, clear logic in `OverlayStateManager` — making the lifecycle hard to reason about.
3. **KMP handler not wired**: `NumbersOverlayHandler` existed in KMP but was never registered with `ActionCoordinator`, so "numbers on/off/auto" voice commands didn't work through the handler pipeline.

## Solution

### New: `OverlayNumberingExecutor.kt`

- Implements `NumbersOverlayExecutor` (KMP interface) at the app layer
- Per-container numbering: `Map<containerAvid, LinkedHashMap<avid, number>>`
- Centralizes screen transition logic (`handleScreenContext()`)
- Non-suspend methods for DynamicCommandGenerator, suspend methods for voice command handler
- Trims per-container cache at 500 entries

### Modified: `DynamicCommandGenerator.kt`

- Accepts `OverlayNumberingExecutor` in constructor
- Removed `lastPackageName` — executor tracks this
- Uses `executor.handleScreenContext()` for reset decisions
- Uses `executor.assignNumbers()` then `OverlayStateManager.updateNumberedOverlayItems()` (simple setter)

### Modified: `OverlayStateManager.kt`

- Removed `avidToNumber`, `maxAssignedNumber`, `updateNumberedOverlayItemsIncremental()`, `trimCacheIfNeeded()`
- Simplified `clearOverlayItems()` (no numbering state to clear)
- Kept `updateNumberedOverlayItems()` as simple setter (already existed)

### Modified: `VoiceAvanueAccessibilityService.kt`

- Creates `OverlayNumberingExecutor` and passes to `DynamicCommandGenerator`
- Registers `NumbersOverlayHandler` with `ActionCoordinator` after VoiceOSCore init

## Architecture (After)

```
DynamicCommandGenerator(executor)
  -> executor.handleScreenContext(packageName, isTargetApp, isNewScreen)  <- lifecycle
  -> executor.assignNumbers(items, containerAvid)                        <- per-container numbering
  -> OverlayStateManager.updateNumberedOverlayItems(items)               <- simple setter

Voice: "numbers on" -> HandlerRegistry -> NumbersOverlayHandler -> executor.setNumbersMode()
```

## Logic Matrix

| Scenario | App Change | Target App | Reset? | Why |
|----------|-----------|------------|--------|-----|
| Calculator -> Home | yes | no | YES | Different app |
| Home -> Gmail | yes | yes | YES | Different app |
| Gmail inbox -> email | no | yes | NO | List app, scroll preserved |
| Gmail scroll | no (same hash) | yes | SKIP | Same screen |
| Google -> search results | no | no | YES | Non-list app, new screen |

## Files Changed

| File | Action |
|------|--------|
| `apps/avanues/.../service/OverlayNumberingExecutor.kt` | Created (~155 lines) |
| `apps/avanues/.../service/DynamicCommandGenerator.kt` | Modified (executor injection, lifecycle delegation) |
| `apps/avanues/.../service/OverlayStateManager.kt` | Modified (removed numbering logic, ~40 lines removed) |
| `apps/avanues/.../service/VoiceAvanueAccessibilityService.kt` | Modified (executor creation, handler registration) |

## Verification

1. App switching: Numbers restart from 1 on every app change
2. Gmail scroll: Numbers preserved (1,2,3 -> scroll -> 4,5,6 -> scroll back -> 1,2,3)
3. Same-app navigation (non-target): Numbers restart on screen change
4. Voice "numbers off" -> badges hide, "numbers on" -> badges show from 1
5. Voice "numbers auto" -> badges show only for target/list apps
6. Mode persistence survives app changes
