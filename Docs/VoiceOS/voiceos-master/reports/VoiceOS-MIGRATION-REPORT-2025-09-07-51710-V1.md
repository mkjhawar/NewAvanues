# Documentation Migration Report - 2025-09-07

## Migration Strategy

### Old Structure → New Structure Mapping

#### System-wide Documentation (→ /docs/voiceos-master/)
- `/docs/analysis/` → `/docs/voiceos-master/reference/analysis/`
- `/docs/architecture/` (system-wide) → `/docs/voiceos-master/architecture/`
- Project management, planning documents → `/docs/voiceos-master/project-management/`

#### Active Work Tracking (→ /coding/)
- `/docs/Status/` → `/coding/STATUS/`
- `/docs/TODO/` → `/coding/TODO/`
- `/docs/issues/` → `/coding/ISSUES/`

#### Module-Specific Documentation
- Device detection/management → `/docs/device-manager/`
- Speech recognition → `/docs/speech-recognition/`
- Voice cursor → `/docs/voice-cursor/`
- Voice accessibility → `/docs/voice-accessibility/`
- Voice UI → `/docs/voice-ui/`
- HUD system → `/docs/hud-manager/`
- Data management → `/docs/data-manager/` or `/docs/vos-data-manager/`
- Localization → `/docs/localization-manager/`

#### Archive
- `/docs/deprecated-do-not-read/` → `/docs/archive/deprecated/`
- Old/obsolete files → `/docs/archive/`

## Files to Migrate

### Analysis Folder (21 files)