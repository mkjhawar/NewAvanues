# VoiceOS Legacy Archive

**Archived:** 2026-01-19
**Reason:** Replaced by KMP VoiceOSCore implementation

## Contents

- `VoiceOSCore/` - Original Android-only VoiceOSCore (490 files)

## Why Archived

The LEGACY VoiceOSCore was a monolithic Android implementation with:
- 3,077-line God class (`VoiceOSService.kt`)
- Deep sub-package structure (100+ sub-packages)
- Android-only code (not KMP)

It has been replaced by the KMP `Modules/VoiceOSCore/` which:
- Separates business logic into `commonMain/` (cross-platform)
- Provides thin platform wrappers in `androidMain/`, `iosMain/`, `desktopMain/`
- Delegates to clean, focused components instead of monolithic service

## Reference

This code is preserved for reference when implementing edge cases or
debugging issues. It should NOT be used for new development.

### Key Files for Reference

| File | Purpose |
|------|---------|
| `VoiceOSCore/src/main/java/.../accessibility/VoiceOSService.kt` | Main accessibility service (3077 lines) |
| `VoiceOSCore/src/main/java/.../extractors/UIScrapingEngine.kt` | Node traversal logic |
| `VoiceOSCore/src/main/java/.../learnapp/` | LearnApp integration |
| `VoiceOSCore/src/main/java/.../overlays/` | UI overlay implementations |

### Do NOT Migrate

These should stay archived - they're app-level concerns or have been reimplemented:
- Activities and ViewModels
- Database DAOs (SQLDelight replaces)
- UI Overlays (app-level)
- Notification handling (app-level)

---

*See: `Docs/Analysis/VoiceOSCore-Independent-Analysis-260119.md` for full consolidation analysis*
