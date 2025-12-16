# VoiceOS - Module Instructions

Parent Repository: NewAvanues
Module: VoiceOS

---

## SCOPE

Work within VoiceOS module only.
For cross-module changes, check with user first.

---

## INHERITED RULES

1. Parent repo rules: `/Volumes/M-Drive/Coding/NewAvanues/.claude/CLAUDE.md`
2. Global rules: `/Volumes/M-Drive/Coding/.claude/CLAUDE.md`

---

## DOCUMENTATION LOCATIONS

**Living Docs:** `/Volumes/M-Drive/Coding/NewAvanues/Docs/VoiceOS/LivingDocs/LD-*.md`
**Registries:** `Modules/VoiceOS/.ideacode/registries/`
- FOLDER-REGISTRY.md - Folder structure for this module
- FILE-REGISTRY.md - File naming for this module
- COMPONENT-REGISTRY.md - Components in this module

**Check Registries FIRST** before creating files or folders.

---

## MODULE-SPECIFIC RULES

| Rule | Requirement |
|------|-------------|
| Database | SQLDelight ONLY (not Room) |
| UI | Jetpack Compose + Material Design 3 |
| Accessibility | Use Android Accessibility API |
| Voice Input | Integrate with NLU module |
| Testing | 90%+ coverage for accessibility features |

---

## KEY COMPONENTS

- **Accessibility Service** - Main service for voice commands
- **Voice Recognition** - Integration with NLU
- **UI Overlays** - Visual feedback for users
- **Database** - SQLDelight for local storage
- **Command Learning** - JIT learning system

---

## DEPENDENCIES

**Internal:**
- `Common/Core` - Core utilities
- `Common/Libraries` - Shared Android libraries
- `NLU` - Intent recognition

**External:**
- Android Accessibility API
- Jetpack Compose
- SQLDelight

See: `/Volumes/M-Drive/Coding/NewAvanues/.ideacode/registries/CROSS-MODULE-DEPENDENCIES.md`

---

## FILE NAMING

| Type | Pattern | Example |
|------|---------|---------|
| Living Docs | `LD-VOS-{Desc}-V#.md` | `LD-VOS-Feature-V1.md` |
| Specs | `VOS-Spec-{Feature}-YDDMM-V#.md` | `VOS-Spec-Voice-51215-V1.md` |
| Kotlin | `{Module}{Component}.kt` | `VoiceOSAccessibilityService.kt` |

---

## BUILDING & TESTING

```bash
# Build
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:assembleDebug

# Test
./gradlew :Modules:VoiceOS:core:database:test

# Install
adb install -r app.apk
```

---

Updated: 2025-12-15 | Version: 12.0.0
