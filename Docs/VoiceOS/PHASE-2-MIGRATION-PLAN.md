# VoiceOS Phase 2 Migration Plan

Date: 2025-12-06
Status: **Ready for Execution**

---

## Overview

Move shared libraries and modules from app-specific folders to monorepo Common/ structure, updating all references.

---

## Migration Map - Libraries (129 files)

### Source → Destination

| Source | Destination | Files | Type |
|--------|-------------|-------|------|
| `android/apps/VoiceOS/libraries/IntentManager/` | `Common/Libraries/VoiceOS/IntentManager/` | ~30 | Shared Library |
| `android/apps/VoiceOS/libraries/NLUCore/` | `Common/Libraries/VoiceOS/NLUCore/` | ~40 | Shared Library |
| `android/apps/VoiceOS/libraries/Utils/` | `Common/Libraries/VoiceOS/Utils/` | ~25 | Shared Library |
| `android/apps/VoiceOS/libraries/VoiceCore/` | `Common/Libraries/VoiceOS/VoiceCore/` | ~34 | Shared Library |

### Namespace Updates

| Old Namespace | New Namespace |
|--------------|---------------|
| `com.voiceos.libraries.intentmanager` | `com.avanues.common.voiceos.intentmanager` |
| `com.voiceos.libraries.nlucore` | `com.avanues.common.voiceos.nlucore` |
| `com.voiceos.libraries.utils` | `com.avanues.common.voiceos.utils` |
| `com.voiceos.libraries.voicecore` | `com.avanues.common.voiceos.voicecore` |

### Gradle Updates Required

**Before:**
```kotlin
// android/apps/VoiceOS/settings.gradle.kts
include(":libraries:IntentManager")
include(":libraries:NLUCore")
include(":libraries:Utils")
include(":libraries:VoiceCore")
```

**After:**
```kotlin
// settings.gradle.kts (root)
include(":Common:Libraries:VoiceOS:IntentManager")
include(":Common:Libraries:VoiceOS:NLUCore")
include(":Common:Libraries:VoiceOS:Utils")
include(":Common:Libraries:VoiceOS:VoiceCore")
```

**App Dependencies Before:**
```kotlin
// android/apps/VoiceOS/app/build.gradle.kts
implementation(project(":libraries:IntentManager"))
implementation(project(":libraries:NLUCore"))
```

**App Dependencies After:**
```kotlin
// android/apps/VoiceOS/app/build.gradle.kts
implementation(project(":Common:Libraries:VoiceOS:IntentManager"))
implementation(project(":Common:Libraries:VoiceOS:NLUCore"))
```

---

## Migration Map - Modules (886 files)

### Source → Destination

| Source | Destination | Files | Type |
|--------|-------------|-------|------|
| `android/apps/VoiceOS/modules/accessibility/` | `Modules/VoiceOS/Accessibility/` | ~150 | Feature Module |
| `android/apps/VoiceOS/modules/automation/` | `Modules/VoiceOS/Automation/` | ~200 | Feature Module |
| `android/apps/VoiceOS/modules/commands/` | `Modules/VoiceOS/Commands/` | ~250 | Feature Module |
| `android/apps/VoiceOS/modules/voice/` | `Modules/VoiceOS/Voice/` | ~286 | Feature Module |

### Namespace Updates

| Old Namespace | New Namespace |
|--------------|---------------|
| `com.voiceos.modules.accessibility` | `com.avanues.modules.voiceos.accessibility` |
| `com.voiceos.modules.automation` | `com.avanues.modules.voiceos.automation` |
| `com.voiceos.modules.commands` | `com.avanues.modules.voiceos.commands` |
| `com.voiceos.modules.voice` | `com.avanues.modules.voiceos.voice` |

### Gradle Updates Required

**Before:**
```kotlin
// android/apps/VoiceOS/settings.gradle.kts
include(":modules:accessibility")
include(":modules:automation")
include(":modules:commands")
include(":modules:voice")
```

**After:**
```kotlin
// settings.gradle.kts (root)
include(":Modules:VoiceOS:Accessibility")
include(":Modules:VoiceOS:Automation")
include(":Modules:VoiceOS:Commands")
include(":Modules:VoiceOS:Voice")
```

---

## Migration Map - Documentation (182 files)

| Source | Destination | Reason |
|--------|-------------|--------|
| `android/apps/VoiceOS/docs/` | `Docs/VoiceOS/Technical/` | Consolidate docs |
| `android/apps/VoiceOS/protocols/` | `Docs/VoiceOS/Protocols/` | Protocol docs |

---

## Migration Map - Other Folders

| Source | Destination | Files | Action |
|--------|-------------|-------|--------|
| `android/apps/VoiceOS/tools/` | `scripts/voiceos/` | Scripts | Move |
| `android/apps/VoiceOS/templates/` | `Shared/Templates/VoiceOS/` | Templates | Move |
| `android/apps/VoiceOS/tests/` | `android/apps/VoiceOS/app/src/test/` | Tests | Merge |
| `android/apps/VoiceOS/Vosk/` | `Common/ThirdParty/Vosk/` | 3rd party | Move |
| `android/apps/VoiceOS/vivoka/` | `Common/ThirdParty/Vivoka/` | 3rd party | Move |

---

## Execution Steps

### Step 1: Create Root Gradle Files

```bash
# Create root settings.gradle.kts
cat > settings.gradle.kts << 'EOF'
rootProject.name = "NewAvanues"

// Android Apps
include(":android:apps:VoiceOS:app")
include(":android:apps:AVA:app")
include(":android:apps:Avanues:app")
include(":android:apps:AvaConnect:app")

// Common Libraries - VoiceOS
include(":Common:Libraries:VoiceOS:IntentManager")
include(":Common:Libraries:VoiceOS:NLUCore")
include(":Common:Libraries:VoiceOS:Utils")
include(":Common:Libraries:VoiceOS:VoiceCore")

// Modules - VoiceOS
include(":Modules:VoiceOS:Accessibility")
include(":Modules:VoiceOS:Automation")
include(":Modules:VoiceOS:Commands")
include(":Modules:VoiceOS:Voice")

// Third Party
include(":Common:ThirdParty:Vosk")
include(":Common:ThirdParty:Vivoka")
EOF
```

### Step 2: Move Libraries

```bash
mkdir -p Common/Libraries/VoiceOS
mv android/apps/VoiceOS/libraries/* Common/Libraries/VoiceOS/
rmdir android/apps/VoiceOS/libraries
```

### Step 3: Move Modules

```bash
mkdir -p Modules/VoiceOS
mv android/apps/VoiceOS/modules/* Modules/VoiceOS/
rmdir android/apps/VoiceOS/modules
```

### Step 4: Update Namespaces

For each moved library/module:
```bash
# Example for IntentManager
find Common/Libraries/VoiceOS/IntentManager -name "*.kt" -exec sed -i '' \
  's/package com.voiceos.libraries.intentmanager/package com.avanues.common.voiceos.intentmanager/g' {} \;

find Common/Libraries/VoiceOS/IntentManager -name "*.kt" -exec sed -i '' \
  's/import com.voiceos.libraries/import com.avanues.common.voiceos/g' {} \;
```

### Step 5: Update build.gradle.kts Files

**Each library needs:**
```kotlin
// Common/Libraries/VoiceOS/IntentManager/build.gradle.kts
plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.avanues.common.voiceos.intentmanager"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    // Updated references to other libraries
    implementation(project(":Common:Libraries:VoiceOS:Utils"))
}
```

### Step 6: Update App Dependencies

```kotlin
// android/apps/VoiceOS/app/build.gradle.kts
dependencies {
    // Old: implementation(project(":libraries:IntentManager"))
    // New:
    implementation(project(":Common:Libraries:VoiceOS:IntentManager"))
    implementation(project(":Common:Libraries:VoiceOS:NLUCore"))
    implementation(project(":Common:Libraries:VoiceOS:Utils"))
    implementation(project(":Common:Libraries:VoiceOS:VoiceCore"))

    // Modules
    implementation(project(":Modules:VoiceOS:Accessibility"))
    implementation(project(":Modules:VoiceOS:Automation"))
    implementation(project(":Modules:VoiceOS:Commands"))
    implementation(project(":Modules:VoiceOS:Voice"))
}
```

### Step 7: Update Imports in App Code

```bash
# Update all imports in app code
find android/apps/VoiceOS/app -name "*.kt" -exec sed -i '' \
  's/import com.voiceos.libraries/import com.avanues.common.voiceos/g' {} \;

find android/apps/VoiceOS/app -name "*.kt" -exec sed -i '' \
  's/import com.voiceos.modules/import com.avanues.modules.voiceos/g' {} \;
```

### Step 8: Update AndroidManifest.xml

```xml
<!-- No changes needed - package name stays same for app -->
<!-- Only internal library namespaces change -->
```

### Step 9: Move Documentation

```bash
mkdir -p Docs/VoiceOS/Technical Docs/VoiceOS/Protocols
mv android/apps/VoiceOS/docs/* Docs/VoiceOS/Technical/
mv android/apps/VoiceOS/protocols/* Docs/VoiceOS/Protocols/
rmdir android/apps/VoiceOS/docs android/apps/VoiceOS/protocols
```

### Step 10: Move Other Folders

```bash
mkdir -p scripts/voiceos Shared/Templates/VoiceOS Common/ThirdParty
mv android/apps/VoiceOS/tools/* scripts/voiceos/
mv android/apps/VoiceOS/templates/* Shared/Templates/VoiceOS/
mv android/apps/VoiceOS/Vosk Common/ThirdParty/
mv android/apps/VoiceOS/vivoka Common/ThirdParty/Vivoka
```

### Step 11: Verify Build

```bash
cd android/apps/VoiceOS
./gradlew :app:assembleDebug
```

### Step 12: Update FILE-REGISTRY.md

Add all moved files with new locations.

### Step 13: Update FOLDER-REGISTRY.md

Update with new Common/Modules structure.

---

## Validation Checklist

- [ ] All 129 library files moved to Common/Libraries/VoiceOS/
- [ ] All 886 module files moved to Modules/VoiceOS/
- [ ] All namespaces updated (com.avanues.common.voiceos.*)
- [ ] All imports updated in app code
- [ ] settings.gradle.kts created at root
- [ ] All build.gradle.kts files updated
- [ ] App builds successfully
- [ ] No broken dependencies
- [ ] FILE-REGISTRY.md updated
- [ ] FOLDER-REGISTRY.md updated
- [ ] Git committed with proper message

---

## Rollback Plan

If build fails:
```bash
git reset --hard HEAD~1  # Undo last commit
# Or restore from backup
```

Backup created before Phase 2:
```bash
git tag voiceos-phase1-backup
```

---

## Expected Outcome

**Final Structure:**
```
NewAvanues/
├── android/apps/VoiceOS/
│   └── app/                        # App code only (2308 files)
├── Common/Libraries/VoiceOS/       # 129 files
│   ├── IntentManager/
│   ├── NLUCore/
│   ├── Utils/
│   └── VoiceCore/
├── Modules/VoiceOS/                # 886 files
│   ├── Accessibility/
│   ├── Automation/
│   ├── Commands/
│   └── Voice/
├── Common/ThirdParty/
│   ├── Vosk/
│   └── Vivoka/
├── Docs/VoiceOS/
│   ├── Technical/                  # 182 files
│   └── Protocols/
├── scripts/voiceos/                # Build scripts
└── Shared/Templates/VoiceOS/       # Templates
```

---

**Ready to execute?** (Y/n)
