# VivokaSDK Wrapper Module

**Purpose:** Wrapper module for Vivoka VSDK dependencies to resolve AAR-in-AAR build issues.

---

## Problem

VoiceOSCore is a library module that needs Vivoka VSDK (provided as AAR files). Gradle does not allow library modules to have direct local AAR dependencies because it creates broken AARs.

**Error:**
```
Direct local .aar file dependencies are not supported when building an AAR.
```

---

## Solution

This wrapper module unpacks the Vivoka AAR files and exposes their contents as a standard Android library:

1. **JAR Files** (`libs/`):
   - `vsdk-6.0.0.jar` - Main Vivoka SDK classes
   - `vsdk-csdk-asr-2.0.0.jar` - ASR (Automatic Speech Recognition) classes
   - `vsdk-csdk-core-1.0.1.jar` - Core SDK classes

2. **Native Libraries** (`src/main/jniLibs/`):
   - `arm64-v8a/` - 64-bit ARM native libraries
   - `armeabi-v7a/` - 32-bit ARM native libraries
   - `x86/` - 32-bit x86 native libraries (empty)
   - `x86_64/` - 64-bit x86 native libraries

---

## Architecture

```
VoiceOS App
    └── VoiceOSCore (library)
            └── VivokaSDK (wrapper library)
                    ├── libs/*.jar (unpacked classes)
                    └── jniLibs/*/* (native libraries)
```

---

## Original AAR Files

Located in: `/Volumes/M-Drive/Coding/NewAvanues/vivoka/`

- `vsdk-6.0.0.aar` (128 KB)
- `vsdk-csdk-asr-2.0.0.aar` (37 MB)
- `vsdk-csdk-core-1.0.1.aar` (34 MB)

---

## Updating Vivoka SDK

To update to a new version:

1. Extract new AAR files:
   ```bash
   cd vivoka
   mkdir -p extracted
   unzip vsdk-NEW-VERSION.aar -d extracted/vsdk-NEW-VERSION
   ```

2. Copy classes and native libraries:
   ```bash
   cp extracted/vsdk-NEW-VERSION/classes.jar ../Modules/VoiceOS/libraries/VivokaSDK/libs/vsdk-NEW-VERSION.jar
   cp -r extracted/vsdk-NEW-VERSION/jni/* ../Modules/VoiceOS/libraries/VivokaSDK/src/main/jniLibs/
   ```

3. Update `build.gradle.kts` dependencies to reference new JAR version

4. Clean up:
   ```bash
   rm -rf extracted
   ```

---

## Usage

Other modules should depend on this wrapper, **not** the raw AAR files:

```kotlin
// ✅ Correct
implementation(project(":Modules:VoiceOS:libraries:VivokaSDK"))

// ❌ Wrong (causes AAR-in-AAR error)
implementation(files("${rootDir}/vivoka/vsdk-6.0.0.aar"))
```

---

**Created:** 2025-12-23
**Author:** Claude Code + Manoj Jhawar
**Related Issue:** AAR packaging error in VoiceOSCore library module
