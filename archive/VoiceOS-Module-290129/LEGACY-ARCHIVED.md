# VoiceOS Legacy Module Notice

**Date:** 2026-01-19

## VoiceOSCore Archived

The `VoiceOSCore/` folder has been archived to:
```
Archive/VoiceOS-Legacy-260119/VoiceOSCore/
```

## Replacement

Use the KMP VoiceOSCore at:
```
Modules/VoiceOSCore/
```

This is the new primary module with:
- `commonMain/` - Cross-platform business logic
- `androidMain/` - Android wiring (thin wrappers)
- `iosMain/` - iOS stubs (to be implemented)
- `desktopMain/` - Desktop stubs (to be implemented)

## Remaining in VoiceOS/

- `core/` - Shared KMP utilities (may be consolidated later)
- `managers/` - HUD, Command managers (may be app-level)
- `Docs/` - Documentation

---

*See: `Docs/Analysis/VoiceOSCore-Independent-Analysis-260119.md` for details*
