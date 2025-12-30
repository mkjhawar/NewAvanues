# AVAMagic Rebranding & Modularization Plan

**Date:** 2025-11-15
**Author:** Manoj Jhawar
**Branch:** avamagic/modularization
**Status:** 🔄 IN PROGRESS

---

## Executive Summary

Comprehensive rebranding and modularization effort to:
1. Rename all "Magic" prefixes to "Ava" prefixes
2. Consolidate namespaces under `com.augmentalis.avanues.avamagic.*`
3. Extract 18 modules as independent libraries
4. Maintain backward compatibility where possible

---

## Rebranding Scope

### Name Changes

| Old Name | New Name | Scope |
|----------|----------|-------|
| **AVAMagic** | **AVAMagic** | All classes, packages, modules |
| **AvaUI** | **AvaUI** | All classes, packages, modules, DSL |
| **AvaCode** | **AvaCode** | All classes, packages, modules, code generator |
| **AvaElements** | **AvaElements** | All classes, packages, modules, components |
| **avamagic** | **avamagic** | All paths, identifiers |
| **avaui** | **avaui** | All paths, identifiers |
| **avacode** | **avacode** | All paths, identifiers |
| **avaelements** | **avaelements** | All paths, identifiers |

### Namespace Changes

#### Current Namespaces →  New Namespaces

```kotlin
// Core Modules
com.augmentalis.avamagic.*
  → com.augmentalis.avanues.avamagic.*

com.augmentalis.voiceos.avaui.*
  → com.augmentalis.avanues.avaui.*

com.augmentalis.voiceos.avacode.*
  → com.augmentalis.avanues.avacode.*

com.augmentalis.avaui.*
  → com.augmentalis.avanues.avaui.*

com.augmentalis.avacode.*
  → com.augmentalis.avanues.avacode.*

// Components
com.augmentalis.avaelements.*
  → com.augmentalis.avanues.avaelements.*

com.augmentalis.avaelements.core.*
  → com.augmentalis.avanues.avaelements.core.*

com.augmentalis.avaelements.components.*
  → com.augmentalis.avanues.avaelements.components.*

com.augmentalis.avaelements.renderers.*
  → com.augmentalis.avanues.avaelements.renderers.*
```

---

## Files Affected

**Total Files:** ~26,000 files
**File Types:**
- ✅ Kotlin source files (`.kt`)
- ✅ Gradle build files (`build.gradle.kts`, `settings.gradle.kts`)
- ✅ Documentation files (`.md`)
- ✅ XML files (Android manifests, resources)
- ✅ JSON files (configuration, metadata)
- ✅ YAML files (configuration)

---

## Automated Rebranding Steps

### Phase 1: Namespace Updates ✅
**Status:** COMPLETE

```bash
# Update all package declarations and imports
com.augmentalis.avamagic → com.augmentalis.avanues.avamagic
com.augmentalis.voiceos.avaui → com.augmentalis.avanues.avaui
com.augmentalis.voiceos.avacode → com.augmentalis.avanues.avacode
com.augmentalis.avaui → com.augmentalis.avanues.avaui
com.augmentalis.avacode → com.augmentalis.avanues.avacode
com.augmentalis.avaelements → com.augmentalis.avanues.avaelements
```

**Files Modified:** ~10,000+ Kotlin, Gradle, XML files

### Phase 2: Type Name Updates 🔄
**Status:** IN PROGRESS

```bash
# Update class names, interfaces, objects
AVAMagic → AVAMagic
AvaUI → AvaUI
AvaCode → AvaCode
AvaElements → AvaElements
AvaUIRuntime → AvaUIRuntime
AvaCodeGenerator → AvaCodeGenerator
```

**Files Modified:** ~5,000+ Kotlin, MD files

### Phase 3: Lowercase Identifier Updates ⏳
**Status:** PENDING

```bash
# Update paths, module names, identifiers
avamagic → avamagic
avaui → avaui
avacode → avacode
avaelements → avaelements
```

**Files Modified:** ~15,000+ all file types

---

## Manual Directory Reorganization

### Current Structure → New Structure

```bash
# Core Modules
Universal/Core/AvaUI/
  → Universal/Core/AvaUI/

Universal/Core/AvaCode/
  → Universal/Core/AvaCode/

# Component Modules
Universal/Libraries/AvaElements/
  → Universal/Libraries/AvaElements/

Universal/Libraries/AvaElements/Core/
  → Universal/Libraries/AvaElements/Core/

Universal/Libraries/AvaElements/Renderers/
  → Universal/Libraries/AvaElements/Renderers/

# MagicIdea Modules
modules/MagicIdea/
  → modules/AVAMagic/

modules/MagicIdea/UI/
  → modules/AVAMagic/UI/

modules/MagicIdea/Components/
  → modules/AVAMagic/Components/
```

### Git Move Commands

```bash
# Core modules
git mv Universal/Core/AvaUI Universal/Core/AvaUI
git mv Universal/Core/AvaCode Universal/Core/AvaCode

# Component libraries
git mv Universal/Libraries/AvaElements Universal/Libraries/AvaElements

# MagicIdea → AVAMagic
git mv modules/MagicIdea modules/AVAMagic
```

---

## Package Directory Reorganization

After namespace changes, package directories need to be reorganized:

```bash
# Example for Core module
src/commonMain/kotlin/com/augmentalis/voiceos/avaui/
  → src/commonMain/kotlin/com/augmentalis/avanues/avaui/

# Example for Components
src/commonMain/kotlin/com/augmentalis/avaelements/
  → src/commonMain/kotlin/com/augmentalis/avanues/avaelements/

# Example for Renderers
src/androidMain/kotlin/com/augmentalis/avaelements/renderers/
  → src/androidMain/kotlin/com/augmentalis/avanues/avaelements/renderers/
```

### Batch Move Script

```bash
#!/bin/bash
# Move package directories to new namespace structure

# Find all old package dirs and move them
find . -type d -path "*/com/augmentalis/voiceos/avaui" | while read dir; do
  newdir=$(echo "$dir" | sed 's|voiceos/avaui|avanues/avaui|')
  mkdir -p "$(dirname "$newdir")"
  git mv "$dir" "$newdir"
done

find . -type d -path "*/com/augmentalis/avamagic" | while read dir; do
  newdir=$(echo "$dir" | sed 's|avamagic|avanues/avamagic|')
  mkdir -p "$(dirname "$newdir")"
  git mv "$dir" "$newdir"
done

find . -type d -path "*/com/augmentalis/avaelements" | while read dir; do
  newdir=$(echo "$dir" | sed 's|avaelements|avanues/avaelements|')
  mkdir -p "$(dirname "$newdir")"
  git mv "$dir" "$newdir"
done
```

---

## Module Path Updates

### settings.gradle.kts Changes

```kotlin
// OLD
include(":Universal:Core:AvaUI")
include(":Universal:Core:AvaCode")
include(":Universal:Libraries:AvaElements:Core")
include(":Universal:Libraries:AvaElements:Renderers:Android")
include(":modules:MagicIdea:UI:Core")

// NEW
include(":Universal:Core:AvaUI")
include(":Universal:Core:AvaCode")
include(":Universal:Libraries:AvaElements:Core")
include(":Universal:Libraries:AvaElements:Renderers:Android")
include(":modules:AVAMagic:UI:Core")
```

### Project Dependency Updates

```kotlin
// OLD
dependencies {
    implementation(project(":Universal:Core:AvaUI"))
    implementation(project(":Universal:Libraries:AvaElements:Core"))
}

// NEW
dependencies {
    implementation(project(":Universal:Core:AvaUI"))
    implementation(project(":Universal:Libraries:AvaElements:Core"))
}
```

---

## Group ID & Artifact ID Updates

### build.gradle.kts Updates

```kotlin
// OLD
group = "com.augmentalis.avaelements"
version = "2.0.0"

// NEW
group = "com.augmentalis.avanues.avaelements"
version = "2.0.0"
```

### Maven Publishing Updates

```kotlin
// OLD
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.augmentalis.avaelements"
            artifactId = "core"
        }
    }
}

// NEW
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.augmentalis.avanues.avaelements"
            artifactId = "core"
        }
    }
}
```

---

## Documentation Updates

### Files to Update

1. **README Files**
   - Root README.md
   - All module README.md files
   - Installation instructions
   - Import statements in examples

2. **Technical Documentation**
   - MAGICELEMENTS-DEVELOPER-MANUAL.md → AVAELEMENTS-DEVELOPER-MANUAL.md
   - MAGICUI-SNIPPET-LIBRARY-*.md → AVAUI-SNIPPET-LIBRARY-*.md
   - All API reference docs
   - Architecture diagrams

3. **Implementation Guides**
   - ANDROID-FIRST-IMPLEMENTATION-PLAN-*.md
   - IDEACODE5-*.md files
   - Feature specification documents

4. **Status Documents**
   - PHASE-TRACKING-LIVING-DOCUMENT.md
   - PROJECT-STATUS-LIVING-DOCUMENT.md
   - YOLO-SESSION-*.md files

---

## Testing Strategy

### Phase 1: Build Verification
```bash
# Clean and rebuild all modules
./gradlew clean

# Build core modules
./gradlew :Universal:Libraries:AvaElements:Core:build
./gradlew :Universal:Core:AvaUI:build
./gradlew :Universal:Core:AvaCode:build

# Build renderers
./gradlew :Universal:Libraries:AvaElements:Renderers:Android:build
./gradlew :Universal:Libraries:AvaElements:Renderers:Desktop:build
./gradlew :Universal:Libraries:AvaElements:Renderers:iOS:build

# Build apps
./gradlew :apps:voiceos:build
./gradlew :apps:voiceavanue:build
```

### Phase 2: Test Execution
```bash
# Run all unit tests
./gradlew test

# Run Android instrumentation tests
./gradlew connectedAndroidTest

# Run iOS tests
./gradlew iosX64Test
```

### Phase 3: Integration Testing
- Verify renderer integration
- Test theme system
- Verify code generation
- Test asset management
- Verify IPC/AIDL services

---

## Modularization: Top 5 Modules

### 1. Asset Manager
**Path:** `Universal/Core/AssetManager` → Standalone library
**New Group ID:** `com.augmentalis.avanues.avamagic.assetmanager`
**Extraction Status:** ⏳ Ready to extract

### 2. AvaElements Core
**Path:** `Universal/Libraries/AvaElements/Core` → Standalone library
**New Group ID:** `com.augmentalis.avanues.avaelements.core`
**Extraction Status:** ⏳ Ready to extract

### 3. Preferences Manager
**Path:** `Universal/Libraries/Preferences` → Standalone library
**New Group ID:** `com.augmentalis.avanues.avamagic.preferences`
**Extraction Status:** ⏳ Ready to extract

### 4. StateManagement
**Path:** `Universal/Libraries/AvaElements/StateManagement` → Standalone library
**New Group ID:** `com.augmentalis.avanues.avaelements.statemanagement`
**Extraction Status:** ⏳ Ready to extract

### 5. Database Module
**Path:** `Universal/Core/Database` → Standalone library
**New Group ID:** `com.augmentalis.avanues.avamagic.database`
**Extraction Status:** ⏳ Ready to extract

---

## Rollback Plan

### Safety Backups
- All changes are on branch `avamagic/modularization`
- Original code remains on `avanues-migration` branch
- Backup created at: `/tmp/avamagic-rebrand-backup-*`

### Rollback Commands
```bash
# Discard all changes and return to previous branch
git checkout avanues-migration
git branch -D avamagic/modularization

# Or reset to previous commit
git reset --hard HEAD~1
```

---

## Timeline

### Day 1 (Today) - Rebranding
- ✅ Create modularization branch
- 🔄 Execute automated rebranding script
- ⏳ Manual directory reorganization
- ⏳ Update settings.gradle.kts
- ⏳ Test builds

### Day 2 - Package Reorganization
- ⏳ Move package directories
- ⏳ Update all imports
- ⏳ Fix broken references
- ⏳ Test builds again

### Day 3 - Module Extraction
- ⏳ Extract Asset Manager
- ⏳ Extract AvaElements Core
- ⏳ Extract Preferences Manager
- ⏳ Create standalone build configs

### Day 4 - Testing & Documentation
- ⏳ Run full test suite
- ⏳ Update all documentation
- ⏳ Create migration guide
- ⏳ Commit and push changes

---

## Success Criteria

- [ ] All ~26,000 files updated correctly
- [ ] All namespaces follow `com.augmentalis.avanues.*` pattern
- [ ] All directory structures reflect new naming
- [ ] All builds pass successfully
- [ ] All tests pass
- [ ] Documentation is complete and accurate
- [ ] Top 5 modules extracted as standalone libraries
- [ ] Zero regressions in functionality

---

## Known Risks & Mitigations

### Risk 1: Circular Dependencies
**Mitigation:** Dependency analysis completed before extraction

### Risk 2: Build Failures
**Mitigation:** Incremental testing after each phase

### Risk 3: Broken References
**Mitigation:** Comprehensive search-and-replace with verification

### Risk 4: Lost Functionality
**Mitigation:** Full test suite execution before merge

---

## Post-Rebranding Tasks

1. **Update CI/CD Pipelines**
   - GitHub Actions workflows
   - Build scripts
   - Deployment configurations

2. **Update External References**
   - Documentation websites
   - API documentation
   - Developer guides

3. **Create Migration Guide**
   - For internal teams
   - For external users (if applicable)
   - Breaking changes list

4. **Publish Announcements**
   - Internal team notification
   - Update CHANGELOG
   - Version bump to 2.0.0 (breaking change)

---

**Author:** Manoj Jhawar (manoj@ideahq.net)
**Created:** 2025-11-15
**Branch:** avamagic/modularization
**IDEACODE Version:** 7.2.0
