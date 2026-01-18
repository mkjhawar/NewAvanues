# VoiceOS Module Restructuring Plan

**Date:** 2026-01-18 | **Version:** V1 | **Author:** Claude

## Overview

This plan restructures all modules under `/Modules/VoiceOS/` to follow KMP flat folder structure and move them to appropriate locations based on their function.

## Current State Analysis

### VoiceOS Module Inventory (23 modules)

| Category | Count | Location |
|----------|-------|----------|
| Core KMP Libraries | 11 | `Modules/VoiceOS/core/` |
| Libraries | 6 | `Modules/VoiceOS/libraries/` |
| Managers | 5 | `Modules/VoiceOS/managers/` |
| Main App | 1 | `Modules/VoiceOS/VoiceOSCore/` |

### Module Status

| Module | Current Structure | KMP Status | Action |
|--------|------------------|------------|--------|
| **Core (11)** | | | |
| result | KMP flat | ✅ Ready | Keep in place |
| hash | KMP flat | ✅ Ready | Keep in place |
| constants | KMP flat | ✅ Ready | Keep in place |
| validation | KMP flat | ✅ Ready | Keep in place |
| exceptions | KMP flat | ✅ Ready | Keep in place |
| command-models | KMP flat | ✅ Ready | Keep in place |
| accessibility-types | KMP flat | ✅ Ready | Keep in place |
| voiceos-logging | KMP flat | ✅ Ready | Keep in place |
| text-utils | KMP flat | ✅ Ready | Keep in place |
| json-utils | KMP flat | ✅ Ready | Keep in place |
| database | KMP flat + SQLDelight | ✅ Ready | Keep in place |
| **Libraries (6)** | | | |
| PluginSystem | KMP (Android-only targets) | ⚠️ Generic | **MOVE to /Modules/** |
| UniversalIPC | KMP (Android-only targets) | ⚠️ Generic | **MOVE to /Modules/** |
| AvidCreator | Android-only | ❌ Needs KMP | **MOVE + Convert** |
| JITLearning | Android-only | ❌ DEPRECATED | **DELETE** |
| LearnAppCore | Android-only | ❌ DEPRECATED | **DELETE** |
| VoiceOsLogging | Android-only | ❌ DUPLICATE | **DELETE** |
| **Managers (5)** | | | |
| CommandManager | Android-only | ❌ VoiceOS-specific | Convert to KMP |
| HUDManager | Android-only | ❌ VoiceOS-specific | Convert to KMP |
| LocalizationManager | Android-only | ❌ VoiceOS-specific | Convert to KMP |
| VoiceDataManager | Android-only | ❌ VoiceOS-specific | Convert to KMP |
| LicenseManager | Android-only | ❌ Generic | **MOVE to /Modules/** |
| **Main App** | | | |
| VoiceOSCore | Android-only | N/A (app) | Keep as Android |

---

## Restructuring Phases

### Phase 1: Move General-Purpose Libraries to /Modules/

These modules are NOT VoiceOS-specific and should be reusable across projects.

#### 1.1 PluginSystem
- **From:** `Modules/VoiceOS/libraries/PluginSystem/`
- **To:** `Modules/PluginSystem/`
- **Package:** `com.augmentalis.magiccode.plugins` (unchanged)
- **Changes:** Update settings.gradle.kts path

#### 1.2 UniversalIPC
- **From:** `Modules/VoiceOS/libraries/UniversalIPC/`
- **To:** `Modules/UniversalIPC/`
- **Package:** `com.augmentalis.universalipc` (unchanged)
- **Note:** Already have `Modules/AvaMagic/IPC` with universal subpackage - need to consolidate

#### 1.3 AvidCreator
- **From:** `Modules/VoiceOS/libraries/AvidCreator/`
- **To:** `Modules/AvidCreator/`
- **Package:** `com.augmentalis.avidcreator` (unchanged)
- **Changes:**
  1. Move directory
  2. Convert build.gradle.kts to KMP multiplatform
  3. Create commonMain/androidMain/iosMain/desktopMain structure
  4. Move Android-only code to androidMain
  5. Extract platform-agnostic code to commonMain

#### 1.4 LicenseManager
- **From:** `Modules/VoiceOS/managers/LicenseManager/`
- **To:** `Modules/LicenseManager/`
- **Package:** `com.augmentalis.licensemanager` (unchanged)
- **Changes:**
  1. Move directory
  2. Convert build.gradle.kts to KMP multiplatform
  3. Create platform source sets

---

### Phase 2: Delete Deprecated/Duplicate Modules

#### 2.1 JITLearning (DEPRECATED)
- **Status:** Marked deprecated 2026-01-06
- **Replacement:** Voice:Core module
- **Action:** Archive to `archive/deprecated/VoiceOS-JITLearning-260118/`

#### 2.2 LearnAppCore (DEPRECATED)
- **Status:** Marked deprecated 2026-01-06
- **Replacement:** Voice:Core module
- **Action:** Archive to `archive/deprecated/VoiceOS-LearnAppCore-260118/`

#### 2.3 VoiceOsLogging (DUPLICATE)
- **Status:** Duplicate of `core:voiceos-logging`
- **Action:** Archive to `archive/deprecated/VoiceOS-VoiceOsLogging-260118/`
- **Update:** Any consumers to use `core:voiceos-logging`

---

### Phase 3: Convert VoiceOS Managers to KMP

These remain under VoiceOS but need KMP structure for cross-platform support.

#### 3.1 CommandManager
- **Location:** `Modules/VoiceOS/managers/CommandManager/` (unchanged)
- **Package:** `com.augmentalis.commandmanager`
- **Changes:**
  1. Convert build.gradle.kts to KMP multiplatform
  2. Create:
     - `src/commonMain/kotlin/com/augmentalis/commandmanager/` - interfaces, models
     - `src/androidMain/kotlin/com/augmentalis/commandmanager/` - Android impl
     - `src/iosMain/kotlin/com/augmentalis/commandmanager/` - iOS stubs
     - `src/desktopMain/kotlin/com/augmentalis/commandmanager/` - Desktop stubs

#### 3.2 HUDManager
- **Location:** `Modules/VoiceOS/managers/HUDManager/` (unchanged)
- **Package:** `com.augmentalis.hudmanager`
- **Changes:** Same as CommandManager

#### 3.3 LocalizationManager
- **Location:** `Modules/VoiceOS/managers/LocalizationManager/` (unchanged)
- **Package:** `com.augmentalis.localizationmanager`
- **Changes:** Same as CommandManager

#### 3.4 VoiceDataManager
- **Location:** `Modules/VoiceOS/managers/VoiceDataManager/` (unchanged)
- **Package:** `com.augmentalis.datamanager`
- **Changes:** Same as CommandManager

---

### Phase 4: UniversalIPC Consolidation

**Issue:** Two UniversalIPC implementations exist:
1. `Modules/VoiceOS/libraries/UniversalIPC/` - Original
2. `Modules/AvaMagic/IPC/src/commonMain/.../universal/` - Recently migrated

**Resolution:**
1. Keep `Modules/AvaMagic/IPC` as the unified IPC module (already has desktop/iOS stubs)
2. Archive `Modules/VoiceOS/libraries/UniversalIPC/`
3. Update all consumers to use `Modules/AvaMagic/IPC`

---

## Settings.gradle.kts Changes

### Remove
```kotlin
include(":Modules:VoiceOS:libraries:JITLearning")       // DEPRECATED
include(":Modules:VoiceOS:libraries:LearnAppCore")      // DEPRECATED
include(":Modules:VoiceOS:libraries:VoiceOsLogging")    // DUPLICATE
include(":Modules:VoiceOS:libraries:UniversalIPC")      // Consolidated into AvaMagic:IPC
```

### Add
```kotlin
include(":Modules:PluginSystem")       // Moved from VoiceOS/libraries
include(":Modules:AvidCreator")        // Moved from VoiceOS/libraries
include(":Modules:LicenseManager")     // Moved from VoiceOS/managers
```

### Keep (no change)
```kotlin
// VoiceOS Core Modules (all 11 keep existing paths)
include(":Modules:VoiceOS:core:database")
include(":Modules:VoiceOS:core:accessibility-types")
// ... (all 11 core modules)

// VoiceOS Managers (paths unchanged, just convert to KMP internally)
include(":Modules:VoiceOS:managers:HUDManager")
include(":Modules:VoiceOS:managers:CommandManager")
include(":Modules:VoiceOS:managers:VoiceDataManager")
include(":Modules:VoiceOS:managers:LocalizationManager")

// VoiceOS Main App
include(":Modules:VoiceOS:VoiceOSCore")
```

---

## Final Structure

```
Modules/
├── PluginSystem/              # NEW: Moved from VoiceOS/libraries
│   ├── build.gradle.kts       # KMP multiplatform
│   └── src/
│       ├── commonMain/
│       ├── androidMain/
│       ├── iosMain/
│       └── desktopMain/
│
├── AvidCreator/               # NEW: Moved from VoiceOS/libraries
│   ├── build.gradle.kts       # KMP multiplatform
│   └── src/
│       ├── commonMain/
│       ├── androidMain/
│       ├── iosMain/
│       └── desktopMain/
│
├── LicenseManager/            # NEW: Moved from VoiceOS/managers
│   ├── build.gradle.kts       # KMP multiplatform
│   └── src/
│       ├── commonMain/
│       ├── androidMain/
│       ├── iosMain/
│       └── desktopMain/
│
├── AvaMagic/
│   └── IPC/                   # Unified IPC (already exists, has UniversalIPC)
│
└── VoiceOS/
    ├── core/                  # 11 KMP modules (unchanged)
    │   ├── result/
    │   ├── hash/
    │   ├── constants/
    │   ├── validation/
    │   ├── exceptions/
    │   ├── command-models/
    │   ├── accessibility-types/
    │   ├── voiceos-logging/
    │   ├── text-utils/
    │   ├── json-utils/
    │   └── database/
    │
    ├── managers/              # 4 managers (converted to KMP)
    │   ├── CommandManager/
    │   ├── HUDManager/
    │   ├── LocalizationManager/
    │   └── VoiceDataManager/
    │
    ├── libraries/             # EMPTY after cleanup
    │   └── PluginSystem/      # Moved to /Modules/PluginSystem
    │
    └── VoiceOSCore/           # Main app (Android-only, unchanged)

archive/deprecated/
├── VoiceOS-JITLearning-260118/
├── VoiceOS-LearnAppCore-260118/
├── VoiceOS-VoiceOsLogging-260118/
└── VoiceOS-UniversalIPC-260118/
```

---

## Execution Order

1. **Phase 2 First** - Delete deprecated modules (safest, no dependencies)
2. **Phase 4** - Archive VoiceOS/libraries/UniversalIPC (already consolidated)
3. **Phase 1** - Move general-purpose libraries
4. **Phase 3** - Convert managers to KMP

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| Breaking consumer imports | Search-replace all import statements |
| Build failures | Test each phase individually |
| Missing dependencies | Verify dependency graph before moves |
| Git conflicts | Single worktree, atomic commits |

---

## Verification Steps

After each phase:
1. Run `./gradlew :Modules:VoiceOS:VoiceOSCore:assembleDebug`
2. Check for unresolved imports
3. Verify no duplicate module registrations

---

## Estimated Changes

| Metric | Count |
|--------|-------|
| Modules moved | 4 |
| Modules archived | 4 |
| Modules converted to KMP | 4 |
| Files affected | ~100 |
| settings.gradle.kts changes | 7 lines removed, 3 lines added |
