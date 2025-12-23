# VoiceOS Fix: AAR Packaging Issue Resolution

**Date:** 2025-12-23
**Type:** Build Fix
**Author:** Claude Code + Manoj Jhawar
**Status:** ✅ Resolved

---

## Problem

VoiceOSCore is a library module (produces AAR) that depends on local Vivoka VSDK AAR files. Gradle blocks this:

```
Error while evaluating property 'hasLocalAarDeps' of task ':Modules:VoiceOS:apps:VoiceOSCore:bundleDebugAar'.
Direct local .aar file dependencies are not supported when building an AAR.
```

**Root Cause:** Library modules cannot have direct local AAR dependencies because it creates broken AARs (classes and resources from local AARs are not packaged).

---

## Solution: VivokaSDK Wrapper Module

Created a wrapper library module that unpacks the Vivoka AAR files and exposes their contents as a standard Android library.

### Architecture Change

**Before:**
```
VoiceOSCore (library) → Direct AAR dependencies ❌
    ├── vivoka/vsdk-6.0.0.aar
    ├── vivoka/vsdk-csdk-asr-2.0.0.aar
    └── vivoka/vsdk-csdk-core-1.0.1.aar
```

**After:**
```
VoiceOSCore (library) → VivokaSDK (wrapper library) ✅
                             ├── libs/*.jar (unpacked classes)
                             └── jniLibs/*/* (native libraries)
```

---

## Implementation Steps

### 1. Created VivokaSDK Wrapper Module

**Location:** `Modules/VoiceOS/libraries/VivokaSDK/`

**Structure:**
```
VivokaSDK/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   └── jniLibs/
│       ├── arm64-v8a/       (81 native libs)
│       ├── armeabi-v7a/     (81 native libs)
│       ├── x86/             (empty)
│       └── x86_64/          (81 native libs)
├── libs/
│   ├── vsdk-6.0.0.jar       (98 KB)
│   ├── vsdk-csdk-asr-2.0.0.jar   (7 KB)
│   └── vsdk-csdk-core-1.0.1.jar  (540 B)
├── proguard-rules.pro
├── consumer-rules.pro
└── README.md
```

### 2. Unpacked Vivoka AARs

**Extraction Process:**
```bash
cd vivoka
mkdir extracted

# Extract each AAR (AARs are ZIP files)
unzip vsdk-6.0.0.aar -d extracted/vsdk-6.0.0
unzip vsdk-csdk-asr-2.0.0.aar -d extracted/vsdk-csdk-asr-2.0.0
unzip vsdk-csdk-core-1.0.1.aar -d extracted/vsdk-csdk-core-1.0.1

# Copy classes.jar files
cp extracted/vsdk-6.0.0/classes.jar ../Modules/VoiceOS/libraries/VivokaSDK/libs/vsdk-6.0.0.jar
cp extracted/vsdk-csdk-asr-2.0.0/classes.jar ../Modules/VoiceOS/libraries/VivokaSDK/libs/vsdk-csdk-asr-2.0.0.jar
cp extracted/vsdk-csdk-core-1.0.1/classes.jar ../Modules/VoiceOS/libraries/VivokaSDK/libs/vsdk-csdk-core-1.0.1.jar

# Copy native libraries
cp -r extracted/vsdk-csdk-asr-2.0.0/jni/* ../Modules/VoiceOS/libraries/VivokaSDK/src/main/jniLibs/
cp -r extracted/vsdk-csdk-core-1.0.1/jni/* ../Modules/VoiceOS/libraries/VivokaSDK/src/main/jniLibs/

# Clean up
rm -rf extracted
```

### 3. Updated Build Configuration

**File:** `Modules/VoiceOS/libraries/VivokaSDK/build.gradle.kts`

```kotlin
dependencies {
    // Vivoka VSDK - Unpacked from AARs to avoid AAR-in-AAR issues
    api(files("libs/vsdk-6.0.0.jar"))
    api(files("libs/vsdk-csdk-asr-2.0.0.jar"))
    api(files("libs/vsdk-csdk-core-1.0.1.jar"))

    // Native libraries are in src/main/jniLibs/ (automatically included)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
}
```

**File:** `Modules/VoiceOS/apps/VoiceOSCore/build.gradle.kts`

```kotlin
// Before
implementation(files("${rootDir}/vivoka/vsdk-6.0.0.aar"))
implementation(files("${rootDir}/vivoka/vsdk-csdk-asr-2.0.0.aar"))
implementation(files("${rootDir}/vivoka/vsdk-csdk-core-1.0.1.aar"))

// After
implementation(project(":Modules:VoiceOS:libraries:VivokaSDK"))
```

**File:** `settings.gradle.kts`

```kotlin
include(":Modules:VoiceOS:libraries:VivokaSDK")  // Vivoka VSDK wrapper module (AAR dependencies)
```

### 4. Added ProGuard Rules

**File:** `consumer-rules.pro`

```proguard
# Keep all Vivoka SDK classes
-keep class com.vivoka.** { *; }
-keep interface com.vivoka.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}
```

---

## Files Created

| File | Purpose |
|------|---------|
| `Modules/VoiceOS/libraries/VivokaSDK/build.gradle.kts` | Wrapper module build config |
| `Modules/VoiceOS/libraries/VivokaSDK/src/main/AndroidManifest.xml` | Minimal manifest |
| `Modules/VoiceOS/libraries/VivokaSDK/libs/*.jar` | Unpacked Java classes (3 files) |
| `Modules/VoiceOS/libraries/VivokaSDK/src/main/jniLibs/` | Native libraries (243 files) |
| `Modules/VoiceOS/libraries/VivokaSDK/proguard-rules.pro` | ProGuard rules |
| `Modules/VoiceOS/libraries/VivokaSDK/consumer-rules.pro` | Consumer ProGuard rules |
| `Modules/VoiceOS/libraries/VivokaSDK/README.md` | Module documentation |

---

## Files Modified

| File | Change |
|------|--------|
| `settings.gradle.kts` | Added VivokaSDK module include |
| `Modules/VoiceOS/apps/VoiceOSCore/build.gradle.kts` | Changed from AAR files to VivokaSDK dependency |

---

## Verification

### Build Tests

| Test | Result |
|------|--------|
| `:Modules:VoiceOS:libraries:VivokaSDK:assembleDebug` | ✅ BUILD SUCCESSFUL (20s) |
| `:Modules:VoiceOS:apps:VoiceOSCore:assembleDebug` | ✅ BUILD SUCCESSFUL (1m 32s) |
| `:Modules:VoiceOS:apps:VoiceOS:assembleDebug` | ✅ BUILD SUCCESSFUL (6m 2s) |

### Native Libraries

| Architecture | Count | Status |
|--------------|-------|--------|
| arm64-v8a | 81 libs | ✅ Packaged |
| armeabi-v7a | 81 libs | ✅ Packaged |
| x86_64 | 81 libs | ✅ Packaged |
| x86 | 0 libs | ⚠️ Empty (acceptable) |

---

## Alternative Solutions Considered

| Solution | Pros | Cons | Selected |
|----------|------|------|----------|
| **Local Maven Repository** | Standard approach | User rejected | ❌ |
| **Change VoiceOSCore to Application** | Simple | Breaks architecture (VoiceOS depends on it) | ❌ |
| **Wrapper Module (AAR)** | Clean separation | Same AAR-in-AAR issue | ❌ |
| **Wrapper Module (Unpacked)** | Clean + works | Manual unpacking | ✅ |

---

## Benefits

1. **Clean Architecture:** Separation of concerns - Vivoka SDK isolated in wrapper
2. **Reusability:** Other modules can depend on VivokaSDK wrapper
3. **Maintainability:** Clear documentation for updating Vivoka SDK versions
4. **Standard Build:** No Gradle hacks or workarounds
5. **Performance:** Native libraries properly packaged for all architectures

---

## Updating Vivoka SDK (Future)

When Vivoka releases a new version:

1. Download new AAR files to `vivoka/` directory
2. Run extraction script (documented in VivokaSDK/README.md)
3. Update version numbers in `VivokaSDK/build.gradle.kts`
4. Test builds

---

## Related Documentation

| Document | Path |
|----------|------|
| Wrapper Module README | `Modules/VoiceOS/libraries/VivokaSDK/README.md` |
| LearnApp Architecture | `Docs/VoiceOS/issues/VoiceOS-Changes-LearnApp-Architecture-Cleanup-5231202-V1.md` |

---

## Technical Details

### AAR File Structure

Vivoka AARs contained:
- `classes.jar` - Compiled Java/Kotlin classes
- `jni/` - Native libraries (arm64-v8a, armeabi-v7a, x86_64)
- `AndroidManifest.xml` - Manifest declarations
- `R.txt` - Resource identifiers
- `proguard.txt` - ProGuard rules

### Why Unpacking Works

1. **JAR Dependencies:** Libraries CAN have JAR file dependencies
2. **jniLibs:** Native libraries in `src/main/jniLibs/` are automatically packaged
3. **Standard Build:** Gradle treats wrapper as normal library with JAR deps

---

**Completion Date:** 2025-12-23
**Status:** ✅ Fully Resolved
**Build Status:** ✅ All builds passing
