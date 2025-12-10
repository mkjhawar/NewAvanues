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
- Device detection/management → `/docs/modules/DeviceManager/`
- Speech recognition → `/docs/modules/SpeechRecognition/`
- Voice cursor → `/docs/modules/VoiceCursor/`
- Voice accessibility → `/docs/modules/VoiceOSCore/accessibility/`
- Voice UI → `/docs/voice-ui/`
- HUD system → `/docs/hud-manager/`
- Data management → `/docs/modules/VoiceDataManager/` or `/docs/vos-data-manager/`
- Localization → `/docs/localization-manager/`

#### Archive
- `/docs/deprecated-do-not-read/` → `/docs/archive/deprecated/`
- Old/obsolete files → `/docs/archive/`

## Files to Migrate

### Analysis Folder (21 files)