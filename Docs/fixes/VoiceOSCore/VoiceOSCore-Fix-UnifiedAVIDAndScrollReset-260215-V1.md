# VoiceOSCore Fix: Unified AVID System + Scroll Reset

**Module:** VoiceOSCore + AVID
**Type:** Fix
**Date:** 2026-02-15
**Version:** V1
**Branch:** IosVoiceOS-Development

---

## Summary

Two related fixes to the overlay badge numbering system:

1. **AVID Unification**: Replaced the overlay-specific AVID generator (`dyn_hash`) with the command-level `ElementFingerprint` system (`BTN:hash8`). Added `packageName` to the hash input for cross-app uniqueness and VOS export portability.

2. **Universal Scroll/Navigation Detection**: Extended the structural-change-ratio approach to ALL apps (previously only target apps). Non-target apps with overlay ON no longer reset numbering on scroll.

---

## Bug 1: Dual AVID Systems + Missing packageName

### Problem
Two separate AVID generation paths existed:
- **Command AVID** (`ElementFingerprint`): Format `BTN:a3f2e1c9`, used for commands/export/JIT
- **Overlay AVID** (`OverlayItemGenerator.generateContentAvid`): Format `dyn_a3f2b1c0`, used for badge numbering

Neither included `packageName` in the hash, meaning identical buttons in different apps produced the same AVID. This breaks VOS export portability — exported .vos files couldn't distinguish elements across apps.

### Fix
1. Added `packageName` parameter to `Fingerprint.forElement()` in AVID module
2. Updated `ElementFingerprint.generate()` to pass `packageName` through
3. Removed `OverlayItemGenerator.generateContentAvid()` entirely
4. Overlay now uses `ElementFingerprint.fromElementInfo(element, packageName)` — same as commands
5. Added `packageName` parameter to `generateForListApp()` and `generateForAllClickable()`
6. Updated `DynamicCommandGenerator.processScreen()` to pass packageName

### Files Changed
| File | Change |
|------|--------|
| `Modules/AVID/.../Fingerprint.kt` | Added `packageName` param to `forElement()` |
| `Modules/VoiceOSCore/.../ElementFingerprint.kt` | Pass `packageName` to `Fingerprint.forElement()` |
| `Modules/VoiceOSCore/.../OverlayItemGenerator.kt` | Removed `generateContentAvid()`, use `ElementFingerprint`, added `packageName` param |
| `apps/avanues/.../DynamicCommandGenerator.kt` | Pass `packageName` to overlay generator |

### AVID Format (Unified)
```
Before: dyn_a3f2b1c0  (overlay) / BTN:a3f2e1c9 (commands) — no packageName
After:  BTN:7c4d2a1e   (everywhere)              — hash includes packageName
```

---

## Bug 2: Scroll Resets Numbering in Non-Target Apps

### Problem
`OverlayNumberingExecutor.handleScreenContext()` had separate logic:
- Target apps: use `structuralChangeRatio` to distinguish scroll from navigation
- Non-target apps: `isNewScreen && !isTargetApp` → **always reset**

This meant scrolling in ANY non-target app with overlay mode ON would clear and renumber all badges.

### Fix
Made structural-change-ratio universal:
1. `DynamicCommandGenerator.processScreen()`: Calculate ratio for ALL `isNewScreen` cases (not just target apps)
2. `handleScreenContext()`: Unified branch: `isNewScreen && structuralChangeRatio > 0.4` → reset (navigation). Otherwise → preserve (scroll)

### Files Changed
| File | Change |
|------|--------|
| `Modules/VoiceOSCore/.../OverlayNumberingExecutor.kt` | Unified reset logic, removed target-app-only branch |
| `apps/avanues/.../DynamicCommandGenerator.kt` | Calculate structuralChangeRatio for all apps |

### Logic Change
```
Before:
  if (isAppChange) → reset
  else if (isNewScreen && !isTargetApp) → ALWAYS reset  ← BUG
  else if (isNewScreen && isTargetApp && ratio > 0.4) → reset

After:
  if (isAppChange) → reset
  else if (isNewScreen && ratio > 0.4) → reset (ALL apps)
  // else: scroll → preserve numbering
```
