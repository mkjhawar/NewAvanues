# VoiceOS Module Consolidation - Future Work Report

**Date:** 2026-01-21
**Author:** Claude (AI Assistant)
**Version:** 1.0
**Branch:** VoiceOSCore-ScrapingUpdate
**Related Document:** `Handover-ModuleReorganization-260121.md`

---

## Executive Summary

This report details the remaining work to complete the VoiceOS module consolidation. Two major phases remain: **Database Consolidation** and **Managers Consolidation**. The database migration is the critical path - managers consolidation depends on it completing first.

**Estimated Scope:**
- ~300 files to relocate/update
- ~50 build.gradle.kts files to modify
- 2-3 focused sessions to complete

---

## Table of Contents

1. [Current State](#1-current-state)
2. [Phase 3: Database Consolidation](#2-phase-3-database-consolidation)
3. [Phase 5: Managers Consolidation](#3-phase-5-managers-consolidation)
4. [Phase 6: Final Cleanup](#4-phase-6-final-cleanup)
5. [Dependency Graph](#5-dependency-graph)
6. [Risk Assessment](#6-risk-assessment)
7. [Verification Checklist](#7-verification-checklist)
8. [Quick Reference Commands](#8-quick-reference-commands)

---

## 1. Current State

### 1.1 Completed Work

| Phase | Task | Status |
|-------|------|--------|
| 1 | VUID → AVID rename | ✅ Done (83 files) |
| 2 | Archive legacy VoiceOS app | ✅ Done |
| 4 | Disable redundant utility modules | ✅ Done (9 modules) |
| - | Build verification | ✅ Passing |

### 1.2 Current settings.gradle.kts State

```kotlin
// ACTIVE - Still included
include(":Modules:VoiceOS:core:database")          // CRITICAL
include(":Modules:VoiceOS:core:command-models")    // Used by managers

// ACTIVE - Managers still included
include(":Modules:VoiceOS:managers:HUDManager")
include(":Modules:VoiceOS:managers:CommandManager")
include(":Modules:VoiceOS:managers:VoiceDataManager")
include(":Modules:VoiceOS:managers:LocalizationManager")

// DISABLED - Already consolidated into VoiceOSCore
// include(":Modules:VoiceOS:core:accessibility-types")
// include(":Modules:VoiceOS:core:result")
// include(":Modules:VoiceOS:core:voiceos-logging")
// include(":Modules:VoiceOS:core:hash")
// include(":Modules:VoiceOS:core:json-utils")
// include(":Modules:VoiceOS:core:constants")
// include(":Modules:VoiceOS:core:exceptions")
// include(":Modules:VoiceOS:core:text-utils")
// include(":Modules:VoiceOS:core:validation")
```

### 1.3 Modules Depending on VoiceOS/core/database

| Module | Dependency Type |
|--------|-----------------|
| `Modules/VoiceOSCore` | implementation |
| `android/apps/voiceoscoreng` | implementation |
| `Modules/AvidCreator` | implementation |
| `Modules/VoiceOS/managers/CommandManager` | implementation |
| `Modules/VoiceOS/managers/VoiceDataManager` | implementation |
| `Modules/VoiceOS/managers/LocalizationManager` | implementation |
| `Modules/AvaMagic/managers/*` | implementation |
| `Modules/AvaMagic/LearnAppCore` | implementation |

---

## 2. Phase 3: Database Consolidation

### 2.1 Overview

**Goal:** Move VoiceOS database code to the centralized `Modules/Database/` module.

**Current Location:** `Modules/VoiceOS/core/database/`
**Target Location:** `Modules/Database/src/commonMain/kotlin/com/augmentalis/database/voiceos/`

**Files Affected:** ~220 files
**Dependencies to Update:** ~10 build.gradle.kts files

### 2.2 Current Database Structure

```
Modules/VoiceOS/core/database/
├── build.gradle.kts
└── src/
    ├── commonMain/
    │   ├── kotlin/com/augmentalis/database/
    │   │   ├── dto/                    # 30+ DTOs
    │   │   ├── repositories/           # 30+ repositories
    │   │   ├── DatabaseFactory.kt
    │   │   └── VoiceOSDatabase.kt
    │   └── sqldelight/
    │       └── com/augmentalis/database/
    │           ├── app/                # App tables
    │           ├── avid/               # AVID tables
    │           ├── scraping/           # Scraping tables
    │           ├── settings/           # Settings tables
    │           └── *.sq                # SQL schema files
    ├── androidMain/
    │   └── kotlin/.../DatabaseFactory.android.kt
    ├── iosMain/
    │   └── kotlin/.../DatabaseFactory.ios.kt
    ├── jvmMain/
    │   └── kotlin/.../DatabaseFactory.jvm.kt
    └── jvmTest/
        └── kotlin/.../                 # Repository tests
```

### 2.3 Target Database Structure

```
Modules/Database/
├── build.gradle.kts                    # UPDATE: Add SQLDelight config for voiceos
└── src/
    ├── commonMain/
    │   ├── kotlin/com/augmentalis/database/
    │   │   ├── core/                   # Existing shared infrastructure
    │   │   ├── web/                    # Existing web tables (4 files)
    │   │   └── voiceos/                # NEW: VoiceOS tables
    │   │       ├── dto/                # Move from VoiceOS/core/database
    │   │       ├── repositories/       # Move from VoiceOS/core/database
    │   │       └── VoiceOSDatabaseFactory.kt
    │   └── sqldelight/
    │       ├── web/                    # Existing
    │       └── voiceos/                # NEW: Move from VoiceOS/core/database
    ├── androidMain/                    # Merge platform-specific
    ├── iosMain/                        # Merge platform-specific
    └── jvmMain/                        # Merge platform-specific
```

### 2.4 Step-by-Step Migration

#### Step 1: Create Target Folder Structure
```bash
# Create voiceos folders in Database module
mkdir -p Modules/Database/src/commonMain/kotlin/com/augmentalis/database/voiceos/dto
mkdir -p Modules/Database/src/commonMain/kotlin/com/augmentalis/database/voiceos/repositories
mkdir -p Modules/Database/src/commonMain/sqldelight/voiceos
```

#### Step 2: Move SQLDelight Schema Files
```bash
# Move SQL schema files
mv Modules/VoiceOS/core/database/src/commonMain/sqldelight/com/augmentalis/database/*.sq \
   Modules/Database/src/commonMain/sqldelight/voiceos/

# Move subdirectory schemas
mv Modules/VoiceOS/core/database/src/commonMain/sqldelight/com/augmentalis/database/app/ \
   Modules/Database/src/commonMain/sqldelight/voiceos/app/
mv Modules/VoiceOS/core/database/src/commonMain/sqldelight/com/augmentalis/database/avid/ \
   Modules/Database/src/commonMain/sqldelight/voiceos/avid/
mv Modules/VoiceOS/core/database/src/commonMain/sqldelight/com/augmentalis/database/scraping/ \
   Modules/Database/src/commonMain/sqldelight/voiceos/scraping/
mv Modules/VoiceOS/core/database/src/commonMain/sqldelight/com/augmentalis/database/settings/ \
   Modules/Database/src/commonMain/sqldelight/voiceos/settings/
```

#### Step 3: Move Kotlin Source Files
```bash
# Move DTOs (keep package names - no refactoring needed)
mv Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/dto/*.kt \
   Modules/Database/src/commonMain/kotlin/com/augmentalis/database/voiceos/dto/

# Move repositories
mv Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/*.kt \
   Modules/Database/src/commonMain/kotlin/com/augmentalis/database/voiceos/repositories/

# Move factory
mv Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/DatabaseFactory.kt \
   Modules/Database/src/commonMain/kotlin/com/augmentalis/database/voiceos/
```

#### Step 4: Move Platform-Specific Files
```bash
# Android
mv Modules/VoiceOS/core/database/src/androidMain/kotlin/com/augmentalis/database/*.kt \
   Modules/Database/src/androidMain/kotlin/com/augmentalis/database/voiceos/

# iOS
mv Modules/VoiceOS/core/database/src/iosMain/kotlin/com/augmentalis/database/*.kt \
   Modules/Database/src/iosMain/kotlin/com/augmentalis/database/voiceos/

# JVM
mv Modules/VoiceOS/core/database/src/jvmMain/kotlin/com/augmentalis/database/*.kt \
   Modules/Database/src/jvmMain/kotlin/com/augmentalis/database/voiceos/
```

#### Step 5: Move Tests
```bash
mv Modules/VoiceOS/core/database/src/jvmTest/kotlin/com/augmentalis/database/*.kt \
   Modules/Database/src/jvmTest/kotlin/com/augmentalis/database/voiceos/
```

#### Step 6: Update Database/build.gradle.kts

Add SQLDelight configuration for VoiceOS tables:

```kotlin
sqldelight {
    databases {
        create("AvanuesDatabase") {
            packageName.set("com.augmentalis.database")
            // Include both web and voiceos schemas
            srcDirs(
                "src/commonMain/sqldelight/web",
                "src/commonMain/sqldelight/voiceos"
            )
        }
    }
}
```

#### Step 7: Update Package Declarations

Files may need package updates. Check if any files have:
```kotlin
// OLD
package com.augmentalis.database

// MAY NEED TO BE (check each file)
package com.augmentalis.database.voiceos
```

#### Step 8: Update All Dependent build.gradle.kts Files

**Change FROM:**
```kotlin
implementation(project(":Modules:VoiceOS:core:database"))
```

**Change TO:**
```kotlin
implementation(project(":Modules:Database"))
```

**Files to update:**
1. `Modules/VoiceOSCore/build.gradle.kts`
2. `android/apps/voiceoscoreng/build.gradle.kts`
3. `Modules/AvidCreator/build.gradle.kts`
4. `Modules/VoiceOS/managers/CommandManager/build.gradle.kts`
5. `Modules/VoiceOS/managers/VoiceDataManager/build.gradle.kts`
6. `Modules/VoiceOS/managers/LocalizationManager/build.gradle.kts`
7. `Modules/AvaMagic/managers/CommandManager/build.gradle.kts`
8. `Modules/AvaMagic/managers/VoiceDataManager/build.gradle.kts`
9. `Modules/AvaMagic/managers/LocalizationManager/build.gradle.kts`
10. `Modules/AvaMagic/LearnAppCore/build.gradle.kts`

#### Step 9: Update Import Statements

Search for and update imports if package paths changed:
```bash
# Find all files importing from old database package
grep -r "import com.augmentalis.database" --include="*.kt" Modules/ android/
```

#### Step 10: Update settings.gradle.kts

Comment out the old database module:
```kotlin
// include(":Modules:VoiceOS:core:database")  // MIGRATED to :Modules:Database
```

#### Step 11: Verify Build
```bash
./gradlew clean
./gradlew :Modules:Database:compileDebugKotlin
./gradlew :Modules:VoiceOSCore:compileDebugKotlin
./gradlew :android:apps:voiceoscoreng:assembleDebug
```

### 2.5 Potential Issues

| Issue | Solution |
|-------|----------|
| Package name conflicts | Keep original `com.augmentalis.database` package for compatibility |
| SQLDelight schema conflicts | Ensure table names are unique across web and voiceos schemas |
| Generated code paths | May need to update imports for SQLDelight generated classes |
| Circular dependencies | Check that Database doesn't depend on VoiceOSCore |

---

## 3. Phase 5: Managers Consolidation

### 3.1 Overview

**Goal:** Merge VoiceOS managers into VoiceOSCore using flat package structure.

**Prerequisite:** Phase 3 (Database Consolidation) must be complete first.

**Modules to Merge:**
1. `Modules/VoiceOS/managers/CommandManager/` (~35 files)
2. `Modules/VoiceOS/managers/HUDManager/` (~10 files)
3. `Modules/VoiceOS/managers/VoiceDataManager/` (~12 files)
4. `Modules/VoiceOS/managers/LocalizationManager/` (~8 files)

### 3.2 Current Manager Structure

```
Modules/VoiceOS/managers/
├── CommandManager/
│   ├── build.gradle.kts
│   └── src/main/java/com/augmentalis/commandmanager/
│       ├── CommandManager.kt
│       ├── validation/CommandValidator.kt
│       ├── routing/IntentDispatcher.kt
│       ├── registry/CommandRegistry.kt
│       ├── registry/DynamicCommandRegistry.kt
│       ├── actions/*.kt (12+ action classes)
│       ├── history/CommandHistory.kt
│       ├── loader/*.kt (3 loader classes)
│       ├── processor/CommandProcessor.kt
│       ├── cache/CommandCache.kt
│       ├── context/*.kt (6 context classes)
│       └── ui/*.kt (3 UI classes)
│
├── HUDManager/
│   ├── build.gradle.kts
│   └── src/main/java/com/augmentalis/hudmanager/
│       └── *.kt
│
├── VoiceDataManager/
│   ├── build.gradle.kts
│   └── src/main/java/com/augmentalis/voicedatamanager/
│       └── *.kt
│
└── LocalizationManager/
    ├── build.gradle.kts
    └── src/main/java/com/augmentalis/localizationmanager/
        └── *.kt
```

### 3.3 Target Structure (Flat in VoiceOSCore)

```
Modules/VoiceOSCore/src/
├── commonMain/kotlin/com/augmentalis/voiceoscore/
│   ├── ... (existing 187 files)
│   │
│   │ # From CommandManager (flatten - no subfolders)
│   ├── CommandManager.kt
│   ├── CommandValidator.kt
│   ├── IntentDispatcher.kt
│   ├── CommandRegistry.kt           # May conflict - rename to LegacyCommandRegistry.kt
│   ├── DynamicCommandRegistry.kt    # May conflict
│   ├── CommandHistory.kt
│   ├── CommandProcessor.kt
│   ├── CommandCache.kt
│   ├── ContextManager.kt
│   ├── ContextMatcher.kt
│   ├── ContextRule.kt
│   ├── ContextSuggester.kt
│   ├── PreferenceLearner.kt
│   ├── CommandContextAdapter.kt
│   ├── ActionFactory.kt
│   ├── BaseAction.kt
│   ├── AppActions.kt
│   ├── DictationActions.kt
│   ├── DragActions.kt
│   ├── EditingActions.kt
│   ├── GestureActions.kt
│   ├── MacroActions.kt
│   ├── NavigationActions.kt
│   ├── NotificationActions.kt
│   ├── OverlayActions.kt
│   ├── ScrollActions.kt
│   ├── ShortcutActions.kt
│   ├── SystemActions.kt
│   ├── TextActions.kt
│   ├── VolumeActions.kt
│   ├── DatabaseCommandResolver.kt
│   ├── UnifiedJSONParser.kt
│   ├── VOSFileParser.kt
│   │
│   │ # From HUDManager
│   ├── HUDManager.kt
│   ├── HUD*.kt (other HUD files)
│   │
│   │ # From VoiceDataManager
│   ├── VoiceDataManager.kt
│   ├── VoiceData*.kt (other files)
│   │
│   │ # From LocalizationManager
│   ├── LocalizationManager.kt
│   └── Localization*.kt (other files)
│
└── androidMain/kotlin/com/augmentalis/voiceoscore/
    └── ... (Android-specific manager implementations if any)
```

### 3.4 Step-by-Step Migration

#### Step 1: Identify Naming Conflicts

Check for files that already exist in VoiceOSCore:
```bash
# List existing files
ls Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/ | sort

# Compare with manager files
ls Modules/VoiceOS/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/*.kt
```

**Known Conflicts:**
- `CommandRegistry.kt` - exists in both (may have different implementations)
- `CommandProcessor.kt` - may exist

#### Step 2: Rename Conflicting Files

Before moving, rename files that conflict:
```bash
# Example - rename legacy version
mv Modules/VoiceOS/managers/CommandManager/src/.../CommandRegistry.kt \
   Modules/VoiceOS/managers/CommandManager/src/.../LegacyCommandRegistry.kt
```

#### Step 3: Update Package Declarations

All manager files need package updates:

**FROM:**

```kotlin
package com.augmentalis.voiceoscore
package com.augmentalis.commandmanager.actions
package com.augmentalis.commandmanager.validation
// etc.
```

**TO:**
```kotlin
package com.augmentalis.voiceoscore
```

Use sed or IDE refactoring:
```bash
# Update package declarations
find Modules/VoiceOS/managers -name "*.kt" -exec sed -i '' \
  's/package com\.augmentalis\.commandmanager.*/package com.augmentalis.voiceoscore/' {} \;

find Modules/VoiceOS/managers -name "*.kt" -exec sed -i '' \
  's/package com\.augmentalis\.hudmanager/package com.augmentalis.voiceoscore/' {} \;

find Modules/VoiceOS/managers -name "*.kt" -exec sed -i '' \
  's/package com\.augmentalis\.voicedatamanager/package com.augmentalis.voiceoscore/' {} \;

find Modules/VoiceOS/managers -name "*.kt" -exec sed -i '' \
  's/package com\.augmentalis\.localizationmanager/package com.augmentalis.voiceoscore/' {} \;
```

#### Step 4: Update Import Statements

Update imports within manager files:
```bash
# Update internal imports
find Modules/VoiceOS/managers -name "*.kt" -exec sed -i '' \
  's/import com\.augmentalis\.commandmanager\./import com.augmentalis.voiceoscore./' {} \;
```

#### Step 5: Move Files to VoiceOSCore

```bash
# Move CommandManager files (flattened)
for file in Modules/VoiceOS/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/*.kt; do
  mv "$file" Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/
done

# Move subdirectory files (flattened)
for dir in actions validation routing registry history loader processor cache context ui; do
  for file in Modules/VoiceOS/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/$dir/*.kt; do
    [ -f "$file" ] && mv "$file" Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/
  done
done

# Move HUDManager
mv Modules/VoiceOS/managers/HUDManager/src/main/java/com/augmentalis/hudmanager/*.kt \
   Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/

# Move VoiceDataManager
mv Modules/VoiceOS/managers/VoiceDataManager/src/main/java/com/augmentalis/voicedatamanager/*.kt \
   Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/

# Move LocalizationManager
mv Modules/VoiceOS/managers/LocalizationManager/src/main/java/com/augmentalis/localizationmanager/*.kt \
   Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/
```

#### Step 6: Update Imports Across Codebase

Update all imports in other modules:
```bash
# Find and update imports
grep -rl "import com.augmentalis.commandmanager" --include="*.kt" . | \
  xargs sed -i '' 's/import com\.augmentalis\.commandmanager\./import com.augmentalis.voiceoscore./'

grep -rl "import com.augmentalis.hudmanager" --include="*.kt" . | \
  xargs sed -i '' 's/import com\.augmentalis\.hudmanager\./import com.augmentalis.voiceoscore./'

grep -rl "import com.augmentalis.voicedatamanager" --include="*.kt" . | \
  xargs sed -i '' 's/import com\.augmentalis\.voicedatamanager\./import com.augmentalis.voiceoscore./'

grep -rl "import com.augmentalis.localizationmanager" --include="*.kt" . | \
  xargs sed -i '' 's/import com\.augmentalis\.localizationmanager\./import com.augmentalis.voiceoscore./'
```

#### Step 7: Update VoiceOSCore/build.gradle.kts

Add any dependencies that managers had:
```kotlin
// Review manager build.gradle.kts files for dependencies to add
// Example: If CommandManager used Hilt, add Hilt to VoiceOSCore
```

#### Step 8: Update settings.gradle.kts

Comment out manager modules:
```kotlin
// MIGRATED to VoiceOSCore (2026-01-XX)
// include(":Modules:VoiceOS:managers:HUDManager")
// include(":Modules:VoiceOS:managers:CommandManager")
// include(":Modules:VoiceOS:managers:VoiceDataManager")
// include(":Modules:VoiceOS:managers:LocalizationManager")
```

#### Step 9: Verify Build
```bash
./gradlew clean
./gradlew :Modules:VoiceOSCore:compileDebugKotlin
./gradlew :android:apps:voiceoscoreng:assembleDebug
```

### 3.5 Potential Issues

| Issue | Solution |
|-------|----------|
| Class name conflicts | Rename with prefix (e.g., `LegacyCommandRegistry`) |
| Android-specific code | Move to `androidMain/` instead of `commonMain/` |
| Hilt/DI dependencies | May need to add Hilt to VoiceOSCore or refactor |
| UI classes | May need Android-specific handling |

---

## 4. Phase 6: Final Cleanup

### 4.1 Archive Modules/VoiceOS Folder

After all migrations complete:
```bash
# Move entire VoiceOS folder to archive
mv Modules/VoiceOS archive/deprecated/VoiceOS-Modules-260121/

# Create deprecation notice
cat > archive/deprecated/VoiceOS-Modules-260121/DEPRECATED.md << 'EOF'
# VoiceOS Modules - DEPRECATED

**Archived Date:** 2026-01-XX
**Reason:** Consolidated into VoiceOSCore and Database modules

## Migration Summary

| Original Module | New Location |
|-----------------|--------------|
| core/database | Modules/Database/voiceos/ |
| core/* (utilities) | Modules/VoiceOSCore/ (already had copies) |
| managers/* | Modules/VoiceOSCore/ |

## Rollback

If needed, restore and update settings.gradle.kts.
EOF
```

### 4.2 Update Documentation

1. Update `Docs/MasterDocs/AI/PLATFORM-INDEX.ai.md`
2. Update `Docs/MasterDocs/AI/CLASS-INDEX.ai.md`
3. Update module READMEs
4. Update `MASTER-INDEX.md`

### 4.3 Final settings.gradle.kts State

```kotlin
// VoiceOS - FULLY CONSOLIDATED (2026-01-XX)
// All code migrated to:
//   - Modules/VoiceOSCore (logic + managers)
//   - Modules/Database (persistence)
//
// include(":Modules:VoiceOS:core:database")
// include(":Modules:VoiceOS:core:command-models")
// include(":Modules:VoiceOS:managers:HUDManager")
// include(":Modules:VoiceOS:managers:CommandManager")
// include(":Modules:VoiceOS:managers:VoiceDataManager")
// include(":Modules:VoiceOS:managers:LocalizationManager")
```

---

## 5. Dependency Graph

### 5.1 Before Consolidation

```
voiceoscoreng
    └── VoiceOSCore
    │       └── VoiceOS/core/database
    └── VoiceOS/core/database

AvidCreator
    └── VoiceOS/core/database

VoiceOS/managers/CommandManager
    ├── VoiceOS/core/database
    └── VoiceOS/core/command-models

VoiceOS/managers/VoiceDataManager
    └── VoiceOS/core/database

VoiceOS/managers/LocalizationManager
    └── VoiceOS/core/database
```

### 5.2 After Consolidation

```
voiceoscoreng
    └── VoiceOSCore
    │       └── Database
    └── Database

AvidCreator
    └── Database

(Managers merged into VoiceOSCore)
```

---

## 6. Risk Assessment

### 6.1 High Risk Items

| Risk | Impact | Mitigation |
|------|--------|------------|
| SQLDelight schema migration | Build failure | Test schema independently first |
| Import statement misses | Runtime errors | Comprehensive grep search |
| Class name conflicts | Compile errors | Pre-check and rename |
| Database driver conflicts | Runtime crashes | Test on device after migration |

### 6.2 Medium Risk Items

| Risk | Impact | Mitigation |
|------|--------|------------|
| Package visibility changes | Compile warnings | Use `internal` modifier appropriately |
| Test breakage | CI failure | Run full test suite |
| AvaMagic module breakage | Parallel system broken | Update AvaMagic managers too |

### 6.3 Rollback Strategy

```bash
# If migration fails, restore from git
git checkout VoiceOSCore-ScrapingUpdate -- Modules/VoiceOS/
git checkout VoiceOSCore-ScrapingUpdate -- Modules/Database/
git checkout VoiceOSCore-ScrapingUpdate -- settings.gradle.kts
```

---

## 7. Verification Checklist

### 7.1 After Database Migration

- [ ] `./gradlew :Modules:Database:compileDebugKotlin` passes
- [ ] `./gradlew :Modules:Database:jvmTest` passes
- [ ] `./gradlew :Modules:VoiceOSCore:compileDebugKotlin` passes
- [ ] `./gradlew :android:apps:voiceoscoreng:assembleDebug` passes
- [ ] App installs and runs on device
- [ ] Database operations work (save/load commands)

### 7.2 After Managers Migration

- [ ] `./gradlew :Modules:VoiceOSCore:compileDebugKotlin` passes
- [ ] No duplicate class errors
- [ ] `./gradlew :android:apps:voiceoscoreng:assembleDebug` passes
- [ ] Voice commands work on device
- [ ] HUD displays correctly
- [ ] Localization works

### 7.3 After Final Cleanup

- [ ] `Modules/VoiceOS/` folder archived
- [ ] `settings.gradle.kts` has no VoiceOS includes
- [ ] Full clean build passes
- [ ] Documentation updated
- [ ] No orphaned references in codebase

---

## 8. Quick Reference Commands

### Build Commands

```bash
# Clean build
./gradlew clean

# Compile specific modules
./gradlew :Modules:Database:compileDebugKotlin
./gradlew :Modules:VoiceOSCore:compileDebugKotlin
./gradlew :android:apps:voiceoscoreng:compileDebugKotlin

# Full app build
./gradlew :android:apps:voiceoscoreng:assembleDebug

# Run tests
./gradlew :Modules:Database:jvmTest
./gradlew :Modules:VoiceOSCore:testDebugUnitTest
```

### Search Commands

```bash
# Find imports
grep -r "import com.augmentalis.voiceos" --include="*.kt" Modules/ android/

# Find dependencies
grep -r "Modules:VoiceOS" --include="*.kts" .

# Count files
find Modules/VoiceOS/core/database -name "*.kt" | wc -l
```

### File Operations

```bash
# Create folder structure
mkdir -p path/to/folder

# Move files
mv source/* destination/

# Update package in files
sed -i '' 's/old.package/new.package/' file.kt
```

---

## Appendix: Session Planning

### Recommended Session Breakdown

**Session 1: Database Migration (~2-3 hours)**
1. Create folder structure
2. Move SQLDelight schemas
3. Move Kotlin source files
4. Update build.gradle.kts files
5. Update settings.gradle.kts
6. Verify build
7. Test on device

**Session 2: Managers Migration (~2-3 hours)**
1. Identify naming conflicts
2. Update package declarations
3. Move files to VoiceOSCore
4. Update imports across codebase
5. Update settings.gradle.kts
6. Verify build
7. Test on device

**Session 3: Final Cleanup (~1 hour)**
1. Archive Modules/VoiceOS
2. Update documentation
3. Final verification
4. Clean up handover docs

---

**Document End**

*For questions or issues during migration, reference the handover document or git history.*
