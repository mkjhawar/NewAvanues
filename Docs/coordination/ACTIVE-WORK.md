# Active Work Coordination

**Last Updated:** 2026-01-13 11:30 UTC
**Protocol:** Check this file before modifying any listed files.

---

## Terminal A: AVID System Implementation (THIS TERMINAL)

**Branch:** `Refactor-VUID` (branched from `Refactor-AvaMagic`)
**Status:** PLANNING COMPLETE - Ready for Implementation
**Mission:** Consolidate all VUID/UUID into unified AVID system

### Completed Work

- [x] AVID format specification (`AVID-Format-Specification-260113-V1.md`)
- [x] AVTR type registry specification (`AVTR-Format-Specification-260113-V1.md`)
- [x] AVID usage guide (`AVID-Usage-Guide-260113-V1.md`)
- [x] System architecture spec (`VUID-VID-Specification-260113-V1.md`)
- [x] Example files (`social-apps.avid`, `voiceos-standard.avtr`)
- [x] Implementation TODO (`TODO-AVID-Implementation-260113.md`)

### Files I Will Modify (DO NOT TOUCH)

```
# VUID Module (exclusive)
Modules/VUID/**                                    RESERVED

# Files to delete
Common/uuidcreator/                                PENDING DELETE
Common/Libraries/uuidcreator/                      PENDING DELETE
Modules/AVAMagic/Libraries/UUIDCreator/            PENDING DELETE

# VoiceOSCoreNG (import changes only)
Modules/VoiceOSCoreNG/src/commonMain/kotlin/.../common/VUIDGenerator.kt      DELETE
Modules/VoiceOSCoreNG/src/commonMain/kotlin/.../common/VUIDTypeCode.kt       DELETE
Modules/VoiceOSCoreNG/src/commonMain/kotlin/.../CommandRegistry.kt           IMPORT CHANGE
Modules/VoiceOSCoreNG/src/commonMain/kotlin/.../ActionCoordinator.kt         IMPORT CHANGE

# Gradle (VUID entries only)
settings.gradle.kts                                ✓ DONE (VUID entry exists)
Modules/VoiceOSCoreNG/build.gradle.kts             PENDING (add VUID dep)
Modules/AVA/core/Data/build.gradle.kts             ✓ DONE (has VUID dep)
```

### Implementation Phases

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Delete 3 duplicate directories | READY |
| 2 | Update Modules/VUID structure | READY |
| 3 | Implement core classes | READY |
| 4 | Database schema (SQLDelight) | READY |
| 5 | Update dependencies | READY |
| 6 | Deprecate old implementations | READY |
| 7 | Testing | PENDING |

---

## Terminal B: [OTHER WORK]

**Branch:** ?
**Status:** ?
**Mission:** ?

### Files Being Modified

```
# List files here so Terminal A knows to avoid them
```

---

## Coordination Protocol

1. **Before modifying a file:** Check if it's listed above
2. **If conflict:** Add your file to your section, wait for other to finish
3. **When done:** Update status to COMPLETE, remove from list
4. **Handshake:** Create `docs/coordination/READY-{terminal}.md` when ready to merge

---

## Instructions for Terminal B

Copy this to Terminal B:

```
Read /Volumes/M-Drive/Coding/NewAvanues/docs/coordination/ACTIVE-WORK.md

Terminal A has completed AVID system planning and is ready for implementation.

Reserved paths (DO NOT MODIFY):
- Modules/VUID/**
- Common/uuidcreator/
- Common/Libraries/uuidcreator/
- Modules/AVAMagic/Libraries/UUIDCreator/
- VoiceOSCoreNG VUID-related files

Please:
1. Add your mission and files to the "Terminal B" section
2. Avoid modifying files listed under "Terminal A"
3. When you finish a phase, update ACTIVE-WORK.md
4. If you need a file Terminal A owns, create docs/coordination/REQUEST-B.md
```

---

## Key Documents Created

| Document | Path |
|----------|------|
| AVID Format Spec | `Modules/VUID/docs/AVID-Format-Specification-260113-V1.md` |
| AVTR Type Registry Spec | `Modules/VUID/docs/AVTR-Format-Specification-260113-V1.md` |
| Usage Guide | `Modules/VUID/docs/AVID-Usage-Guide-260113-V1.md` |
| System Architecture | `Modules/VUID/docs/VUID-VID-Specification-260113-V1.md` |
| Implementation TODO | `Modules/VUID/docs/TODO-AVID-Implementation-260113.md` |
| Example .avid | `Modules/VUID/docs/examples/social-apps.avid` |
| Example .avtr | `Modules/VUID/docs/examples/voiceos-standard.avtr` |

---

## Merge Order

1. Terminal B finishes first (if on same branch)
2. Terminal A rebases and continues
3. Or: Work on separate branches, merge sequentially
