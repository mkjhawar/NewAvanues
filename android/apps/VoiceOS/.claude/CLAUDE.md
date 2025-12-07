# VoiceOS - Project Instructions

This is the VoiceOS Android application.

---

## Build Publishing (MANDATORY)

**After EVERY successful build, run:**

| Build Type | Command |
|------------|---------|
| Debug | `./scripts/publish-build.sh` |
| Release | `./scripts/publish-build.sh release` |

---

## Output Location

`/Volumes/M-Drive/Coding/builds/VoiceOS/`

| Directory | Content |
|-----------|---------|
| debug/ | Debug APKs |
| release/ | Release APKs |

---

## Retention Policy

| Type | Keep on Disk | Git Tracked |
|------|--------------|-------------|
| Debug | Last 5 | Last 10 |
| Release | All | Last 5 |

---

## Build Commands

| Task | Command |
|------|---------|
| Debug build | `./gradlew assembleDebug` |
| Release build | `./gradlew assembleRelease` |
| Run tests | `./gradlew test` |
| Lint | `./gradlew lint` |

---

## Project-Specific Rules

| Rule | Requirement |
|------|-------------|
| Publish builds | ALWAYS after assembleDebug/Release |
| Voice features | Test on physical device |
| Permissions | Document in manifest comments |
| No Delete | Never delete working features without approval + pros/cons |

---

## Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Documents | `VoiceOS-Module-Description-YDDMMHH-V#.md` | `VoiceOS-NLU-Integration-5031215-V1.md` |
| Living Docs | `LD-VoiceOS-Module-Description-V#.md` | `LD-VoiceOS-Architecture-V1.md` |
| Specs | `VoiceOS-Spec-Feature-YDDMM-V#.md` | `VoiceOS-Spec-Commands-50312-V1.md` |
| Plans | `VoiceOS-Plan-Feature-YDDMM-V#.md` | `VoiceOS-Plan-Migration-50312-V1.md` |

---

## Inherited Rules

All rules from `/Volumes/M-Drive/Coding/.claude/CLAUDE.md` apply.

---

**Updated:** 2025-12-05 | **Version:** 10.3
