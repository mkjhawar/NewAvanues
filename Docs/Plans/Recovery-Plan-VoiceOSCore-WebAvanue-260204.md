# Recovery Plan: VoiceOSCore & WebAvanue Consolidation

**Date**: 2026-02-04
**Branch**: claude/040226-1-module-consolidation-EQyzV
**Status**: Planning
**Priority**: HIGH

---

## Problem Summary

The merge commit `16e421e6` (Merge claude/refactor-command-generator-zBr2W into VoiceOSCore-CodeCompliance) introduced issues:

1. **Import errors** - Package references broken after merge
2. **Missing dependencies** - Module references lost
3. **Type conflicts** - Duplicate/missing type definitions
4. **Archived files** - Some code moved to archive may still be needed

---

## Affected Files Analysis

### Files Fixed Post-Merge

| Commit | Files Fixed | Issue |
|--------|-------------|-------|
| `142d352f` | CommandModels.kt, MissingTypes.kt, GlassmorphismUtils (3), HUDManager.kt, VoiceUIStubs.kt | Import errors |
| `b601a400` | build.gradle.kts, EditingActions.kt, MacroActions.kt, libs.versions.toml | Missing dependencies |
| `f3adf4ad` | DeviceManagerActivity.kt, LicenseManagerActivity.kt, CommandManager.kt, ActionFactory.kt | More missing dependencies |

### Files Removed (May Need Recovery)

| File | Location | Status | Action Needed |
|------|----------|--------|---------------|
| TypeAliases.kt | VoiceOSCore/types/ | Removed | ✓ Intentional - was unused |
| MissingTypes.kt | VoiceOSCore/types/ | Removed | ✓ Intentional - consolidated |
| CommandManager/* | Modules/VoiceOS/managers/ | Archived | Check if needed |
| HUDManager/* | Modules/VoiceOS/managers/ | Archived | Check if needed |
| VoiceDataManager/* | Modules/VoiceOS/managers/ | Archived | Check if needed |
| VoiceOS app | android/apps/VoiceOS/ | Archived | ✓ Legacy - not needed |

---

## Recovery Tasks

### Phase 1: Verify Current State (Immediate)

```bash
# 1. Check for compilation errors
./gradlew :Modules:VoiceOSCore:compileKotlinAndroid

# 2. Check for missing imports
grep -r "import.*voiceos\." Modules/VoiceOSCore --include="*.kt" | grep -v "voiceoscore"

# 3. Check for broken references
grep -r "Modules:VoiceOS:" settings.gradle.kts
```

### Phase 2: Recover Missing Code (If Needed)

#### Option A: Recover from Archive
```bash
# If managers are needed, copy back from archive
cp -r archive/VoiceOS-Module-260204/managers/HUDManager/* Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/managers/hudmanager/

# Update package declarations
find Modules/VoiceOSCore -name "*.kt" -exec sed -i 's/package com.augmentalis.voiceos/package com.augmentalis.voiceoscore/g' {} \;
```

#### Option B: Recover from Git History
```bash
# Get file from before merge
git show b00a4b85:Modules/VoiceOS/managers/CommandManager/src/main/kotlin/path/to/File.kt > recovered_file.kt
```

### Phase 3: Validate Build

```bash
# Full build validation
./gradlew clean build --no-daemon

# Run tests
./gradlew :Modules:VoiceOSCore:test
./gradlew :Modules:WebAvanue:test
```

---

## Code That Should Exist in VoiceOSCore

Based on the consolidation plan, VoiceOSCore should contain:

### Core Types (commonMain)
```
src/commonMain/kotlin/com/augmentalis/voiceoscore/
├── CommandModels.kt           ✓ Exists (with CommandCategory)
├── QuantizedTypes.kt          ✓ Check exists
├── ElementType.kt             ✓ Check exists
├── ActionTypes.kt             ✓ Check exists
└── types/
    └── DirectionTypes.kt      ✓ Exists (ScrollDirection, VolumeDirection)
```

### Managers (androidMain)
```
src/androidMain/kotlin/com/augmentalis/voiceoscore/managers/
├── commandmanager/            ✓ Check exists
│   ├── CommandManager.kt
│   ├── actions/
│   └── loader/
├── hudmanager/                ✓ Check exists
│   ├── HUDManager.kt
│   ├── core/
│   ├── rendering/
│   └── spatial/
└── localizationmanager/       ✓ Check exists
```

### Assets (androidMain)
```
src/androidMain/assets/
├── filters/                   ✓ Added in consolidation
│   ├── en-US/
│   ├── de-DE/
│   ├── es-ES/
│   └── fr-FR/
├── commands/                  ? Check if needed
└── categories/                ? Check if needed
```

---

## Verification Checklist

### VoiceOSCore Module
- [ ] `CommandModels.kt` has all required types
- [ ] `CommandCategory` enum exists and is complete
- [ ] All managers compile without errors
- [ ] Filter assets are in place
- [ ] No broken import statements
- [ ] build.gradle.kts has all dependencies

### WebAvanue Module
- [ ] StateFlow utilities exist (`util/`)
- [ ] All ViewModels compile
- [ ] Repository pattern intact
- [ ] No broken references to old packages

### Integration
- [ ] VoiceOSCore → WebAvanue dependency works
- [ ] Shared types accessible from both modules
- [ ] gRPC/RPC services functional

---

## Rollback Plan

If recovery fails, rollback to pre-merge state:

```bash
# Create backup of current state
git stash

# Reset to before merge
git checkout b00a4b85

# Create recovery branch
git checkout -b recovery-pre-merge-260204

# Cherry-pick only the good commits
git cherry-pick <good-commit-hash>
```

---

## Post-Recovery Tasks

1. **Update Documentation**
   - Update module structure docs
   - Document what was recovered

2. **Add Integration Tests**
   - Test VoiceOSCore + WebAvanue integration
   - Test all managers function correctly

3. **Clean Up Archive**
   - Remove duplicate code from archive
   - Keep only truly deprecated code

---

## Files to Verify Exist

Run this to check critical files:

```bash
# Critical VoiceOSCore files
ls -la Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/CommandModels.kt
ls -la Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/managers/commandmanager/CommandManager.kt
ls -la Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/managers/hudmanager/HUDManager.kt
ls -la Modules/VoiceOSCore/src/androidMain/assets/filters/en-US/

# Critical WebAvanue files
ls -la Modules/WebAvanue/src/commonMain/kotlin/com/augmentalis/webavanue/util/BaseViewModel.kt
ls -la Modules/WebAvanue/src/commonMain/kotlin/com/augmentalis/webavanue/util/UiState.kt
```

---

## Summary

| Task | Priority | Status |
|------|----------|--------|
| Verify VoiceOSCore compiles | HIGH | Pending |
| Check all managers exist | HIGH | Pending |
| Verify WebAvanue intact | HIGH | Pending |
| Recover missing files if needed | MEDIUM | Pending |
| Full build validation | HIGH | Pending |
| Update documentation | LOW | Pending |

---

*Recovery plan created by Claude Code Assistant*
