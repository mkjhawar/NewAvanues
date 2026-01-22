# VoiceOS Legacy App - DEPRECATED

**Archived Date:** 2026-01-21
**Reason:** Replaced by voiceoscoreng
**Original Location:** `android/apps/VoiceOS/`

## Why Archived

This standalone VoiceOS app has been deprecated in favor of the voiceoscoreng app which:

1. **Uses active VoiceOSCore module** - References the current KMP module structure
2. **Has better developer tooling** - Debug overlay FAB, AVID viewer, command browser
3. **Is actively maintained** - All recent voice control development happens here
4. **Compiles successfully** - This legacy app references archived modules that no longer exist

## Build Status

This app **cannot build** because it references:
- `Modules:VoiceOS:VoiceOSCore` (archived, moved to `Modules/VoiceOSCore`)
- Various deprecated module paths

## Replacement

Use **voiceoscoreng** at `android/apps/voiceoscoreng/` for all VoiceOS development.

## Rollback

If needed, restore with:
```bash
mv archive/deprecated/VoiceOS-LegacyApp-260121/ android/apps/VoiceOS/
```

Then update `settings.gradle.kts` to re-include the module.
