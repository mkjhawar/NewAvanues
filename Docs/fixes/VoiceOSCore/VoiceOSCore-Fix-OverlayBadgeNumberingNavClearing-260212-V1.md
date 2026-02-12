# VoiceOSCore Fix: Overlay Badge Numbering + Navigation Clearing

**Date**: 2026-02-12
**Branch**: IosVoiceOS-Development
**Status**: Implemented, build verified

## Bugs Fixed

### Bug 1: Wrong order on scroll-back
**Symptom**: Scrolling back to top in list apps shows high sequential numbers (13, 14, 15...) instead of visual position (1, 2, 3...).
**Root Cause**: `OverlayNumberingExecutor.assignNumbers()` used sticky AVID-to-number mapping via `containerMap.getOrPut()`. Items that scrolled off-screen retained their original numbers when they reappeared.
**Fix**: Changed to position-based 1-N assignment. Each call sorts by visual position (top→left) and assigns sequential numbers. No sticky mapping.

### Bug 2: Badges persist on fragment navigation
**Symptom**: In apps using Fragment transitions (not Activity), old badges bleed through to new screens.
**Root Cause**: Fragment transitions don't fire `TYPE_WINDOW_STATE_CHANGED`, so `onInAppNavigation()` never triggers. Meanwhile, `TYPE_VIEW_SCROLLED` fires during transition animation → `onScrollSettled` → `refreshOverlayBadges(fromScroll=true)` → `processScreen` skips clearing because `fromScroll=true`.
**Fix**:
1. Cancel `pendingScrollRefreshJob` when content change events are debouncing (prevents stale scroll badges during transitions)
2. Remove `fromScroll` parameter entirely — structural-change-ratio in `handleScreenContext()` handles navigation detection generically for both Activity and Fragment transitions
3. Lower `MAJOR_NAVIGATION_THRESHOLD` from 0.6 to 0.4 to catch subtler transitions

## Files Modified

| File | Change |
|------|--------|
| `apps/avanues/.../OverlayNumberingExecutor.kt` | Position-based 1-N numbering + threshold 0.6→0.4 |
| `apps/avanues/.../DynamicCommandGenerator.kt` | Remove `fromScroll` param + clearing block |
| `apps/avanues/.../VoiceAvanueAccessibilityService.kt` | Remove `fromScroll` from `refreshOverlayBadges()` |
| `Modules/VoiceOSCore/.../VoiceOSAccessibilityService.kt` | Cancel `pendingScrollRefreshJob` during content changes |

## Verification Steps

1. Build: `./gradlew :Modules:VoiceOSCore:compileDebugKotlin :apps:avanues:compileDebugKotlin` — PASSED
2. Deploy to device, enable accessibility service
3. List app: scroll down → scroll back up → numbers should be 1-N from top
4. Fragment navigation: tap list item → badges should clear → detail view gets fresh badges from 1
5. Scroll in list: badges should update with sequential numbers on each scroll
