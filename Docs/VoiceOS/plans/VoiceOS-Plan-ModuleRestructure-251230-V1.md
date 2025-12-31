# VoiceOS Module Restructure Plan

**Document:** VoiceOS-Plan-ModuleRestructure-251230-V1.md
**Created:** 2025-12-30
**Status:** Planning (Execute after Phase 3 NLU Context Enhancement)
**Branch:** VoiceOS-Development

---

## Problem Statement

Current folder structure has significant redundancy and architectural issues:

1. **Redundant naming:** "VoiceOS/voiceoscore" repeated 3x in paths
2. **Misleading `/apps/` folder:** Contains libraries, not standalone apps
3. **Deep nesting:** 12+ levels to reach source files
4. **Restricted reusability:** Core modules nested under app-specific folders

**Example of redundancy:**
```
Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/ui/overlays/NumberOverlayManager.kt
         ↑              ↑                                    ↑              ↑
      VoiceOS      VoiceOSCore                          voiceoscore     overlays (also in filename)
```

---

## Target Architecture

### Principle: Separation of Concerns

| Location | Purpose | Example |
|----------|---------|---------|
| `android/{app}/` | Platform-specific app code | `android/voiceos/` |
| `ios/{app}/` | Platform-specific app code | `ios/voiceos/` |
| `Modules/{Library}/` | Reusable shared modules | `Modules/VoiceOSCore/` |

### Target Structure

```
NewAvanues/
├── android/
│   ├── voiceos/                    # VoiceOS Android app (UI, Activities)
│   ├── ava/                        # AVA Android app
│   └── avanues/                    # Avanues Android app
│
├── ios/
│   ├── voiceos/                    # VoiceOS iOS app
│   └── ava/                        # AVA iOS app
│
├── Modules/
│   ├── VoiceOSCore/                # Core voice/accessibility library (REUSABLE)
│   │   └── src/main/java/com/augmentalis/vos/
│   │
│   ├── SpeechRecognition/          # Speech recognition library (REUSABLE)
│   ├── DeviceManager/              # Device management library (REUSABLE)
│   ├── Database/                   # Shared database module (REUSABLE)
│   │
│   ├── VoiceOS/                    # VoiceOS-SPECIFIC modules only
│   │   ├── LearnApp/               # LearnApp feature module
│   │   └── JITLearning/            # JIT learning module
│   │
│   ├── AVA/                        # AVA-specific modules
│   └── WebAvanue/                  # WebAvanue-specific modules
│
└── Common/                         # KMP shared code
```

---

## Migration Decisions Required

### Folder-by-Folder Review

For each current folder, decide: **Root Module** or **App-Specific**?

| Current Path | Contents | Decision | New Path |
|--------------|----------|----------|----------|
| `Modules/VoiceOS/apps/VoiceOSCore/` | Core accessibility, overlays, scraping | TBD | `Modules/VoiceOSCore/` ? |
| `Modules/VoiceOS/apps/VoiceOS/` | Android app UI | TBD | `android/voiceos/` ? |
| `Modules/VoiceOS/apps/VoiceCursor/` | Cursor library | TBD | ? |
| `Modules/VoiceOS/apps/LearnApp/` | LearnApp feature | TBD | ? |
| `Modules/VoiceOS/libraries/SpeechRecognition/` | Speech lib | TBD | `Modules/SpeechRecognition/` ? |
| `Modules/VoiceOS/libraries/DeviceManager/` | Device lib | TBD | `Modules/DeviceManager/` ? |
| `Modules/VoiceOS/libraries/JITLearning/` | JIT learning | TBD | ? |
| `Modules/VoiceOS/libraries/LearnAppCore/` | LearnApp core | TBD | ? |
| `Modules/VoiceOS/libraries/UUIDCreator/` | VUID creator | TBD | `Modules/VUIDCreator/` ? |
| `Modules/VoiceOS/managers/` | Various managers | TBD | ? |
| `Modules/VoiceOS/core/database/` | SQLDelight DB | TBD | `Modules/Database/` ? |
| `Modules/VoiceOS/core/accessibility-types/` | Type definitions | TBD | ? |

### Decision Criteria

**Root Module (Modules/{Name}/):**
- Used by multiple apps (VoiceOS, AVA, Avanues)
- Generic functionality (speech, device, database)
- No app-specific dependencies

**App-Specific (Modules/{App}/{Name}/):**
- Only used by one app
- Has app-specific logic/dependencies
- Tightly coupled to app features

**Platform App (android/{app}/ or ios/{app}/):**
- UI code (Activities, Fragments, Composables)
- Platform-specific integrations
- App entry points

---

## Package Name Simplification

### Current (verbose)
```
com.augmentalis.voiceoscore.learnapp.database.repository
```

### Proposed (concise)
```
com.augmentalis.vos.learnapp.db
```

| Current | Proposed | Savings |
|---------|----------|---------|
| `voiceoscore` | `vos` | 8 chars |
| `database` | `db` | 6 chars |
| `repository` | `repo` | 6 chars |
| `accessibility` | `a11y` | 9 chars |

---

## Migration Steps

### Phase 1: Planning (This Document)
- [ ] Review each folder
- [ ] Decide Root vs App-Specific
- [ ] Document new paths

### Phase 2: Gradle Updates
- [ ] Update `settings.gradle.kts` with new module paths
- [ ] Update `build.gradle.kts` dependencies
- [ ] Verify composite builds still work

### Phase 3: File Moves
- [ ] Move folders to new locations
- [ ] Update package declarations
- [ ] Update all imports

### Phase 4: Verification
- [ ] Full build passes
- [ ] All tests pass
- [ ] Apps launch correctly

---

## Risks

| Risk | Mitigation |
|------|------------|
| Breaking imports | IDE refactoring tools |
| Gradle path errors | Test incrementally |
| Git history loss | Use `git mv` for moves |
| Merge conflicts | Do on clean branch |

---

## Timeline

| Phase | When |
|-------|------|
| Planning | After Phase 3 NLU complete |
| Execution | Dedicated refactoring session |
| Verification | Same session |

---

## Notes

- Keep VoiceOSCore name (user preference)
- Package shortening optional but recommended
- Do NOT start until Phase 3 NLU is complete and committed

---

**Next Action:** Complete Phase 3 NLU Context Enhancement, then return to this plan.
