# VoiceOS External SDK Integration - Developer Guide

**Version:** 1.0
**Date:** 2025-12-23
**Author:** Manoj Jhawar + Claude Code
**Audience:** Developers integrating external ASR/SDK dependencies
**Status:** Published
**Related:** VoiceOS-Infrastructure-Components-Developer-Manual-51222-V1.md

---

## Table of Contents

| Section | Title |
|---------|-------|
| 1 | [Introduction](#1-introduction) |
| 2 | [AAR Packaging Limitation](#2-aar-packaging-limitation) |
| 3 | [The Vivoka VSDK Case Study](#3-the-vivoka-vsdk-case-study) |
| 4 | [Solution: Wrapper Module Pattern](#4-solution-wrapper-module-pattern) |
| 5 | [Implementation Guide](#5-implementation-guide) |
| 6 | [Applying to Other ASR Systems](#6-applying-to-other-asr-systems) |
| 7 | [Troubleshooting](#7-troubleshooting) |
| A | [Appendix: Alternative Solutions](#appendix-a-alternative-solutions) |

---

## 1. Introduction

### 1.1 Problem Statement

When integrating external speech recognition SDKs (ASR) or other third-party Android libraries distributed as AAR files into VoiceOSCore, you may encounter a critical Gradle build limitation:

> **"Direct local .aar file dependencies are not supported when building an AAR"**

### 1.2 Why This Matters

VoiceOSCore is configured as an Android **library module** (`com.android.library`) because it's a reusable component consumed by the main VoiceOS application. Library modules cannot have direct local AAR file dependencies in Gradle.

### 1.3 Scope

This guide covers:
- Understanding the AAR-in-AAR limitation
- Proven solution pattern (wrapper module with unpacked AARs)
- Step-by-step implementation
- Application to other ASR systems (Google, Azure, Whisper, Vosk, etc.)

---

## 2. AAR Packaging Limitation

### 2.1 Technical Background

**AAR (Android Archive)** is a binary distribution format for Android libraries containing:
- Compiled classes (`classes.jar`)
- Android resources
- AndroidManifest.xml
- Native libraries (JNI `.so` files)
- ProGuard rules

**The Limitation:**
When building a library module (AAR), Gradle does not package contents from local AAR dependencies. This creates broken AARs where classes and resources from dependencies are missing.

### 2.2 VoiceOS Architecture Context

```
VoiceOS App (Application Module)
    └── VoiceOSCore (Library Module) ❌ Cannot have direct AAR deps
            └── External ASR SDK (AAR file)
```

**Why VoiceOSCore is a Library:**
- VoiceOS main app depends on VoiceOSCore
- VoiceOSCore provides reusable accessibility service functionality
- Must remain a library for architectural modularity

### 2.3 Error Manifestation

```bash
> Task :Modules:VoiceOS:apps:VoiceOSCore:bundleDebugAar FAILED

* What went wrong:
Error while evaluating property 'hasLocalAarDeps' of task
':Modules:VoiceOS:apps:VoiceOSCore:bundleDebugAar'.

> Direct local .aar file dependencies are not supported when
building an AAR. The resulting AAR would be broken because the
classes and Android resources from any local .aar file dependencies
would not be packaged in the resulting AAR.
```

---

## 3. The Vivoka VSDK Case Study

### 3.1 Background

Vivoka VSDK is an offline speech recognition engine distributed as three AAR files:
- `vsdk-6.0.0.aar` (128 KB) - Main SDK classes
- `vsdk-csdk-asr-2.0.0.aar` (37 MB) - ASR engine with native libraries
- `vsdk-csdk-core-1.0.1.aar` (34 MB) - Core SDK with native libraries

**Total:** 71 MB of AAR files with 243 native libraries across 3 architectures.

### 3.2 Original (Broken) Approach

```kotlin
// VoiceOSCore/build.gradle.kts
dependencies {
    // ❌ This fails when building VoiceOSCore AAR
    implementation(files("${rootDir}/vivoka/vsdk-6.0.0.aar"))
    implementation(files("${rootDir}/vivoka/vsdk-csdk-asr-2.0.0.aar"))
    implementation(files("${rootDir}/vivoka/vsdk-csdk-core-1.0.1.aar"))
}
```

**Result:** Build error during AAR packaging.

### 3.3 Solution Architecture

Created `VivokaSDK` wrapper module that unpacks AAR contents:

```
VoiceOS App
    └── VoiceOSCore (library) ✅
            └── VivokaSDK (wrapper library) ✅
                    ├── libs/*.jar (unpacked classes)
                    └── jniLibs/*/*.so (native libraries)
```

---

## 4. Solution: Wrapper Module Pattern

### 4.1 Pattern Overview

**Strategy:** Create an intermediate wrapper library module that:
1. Unpacks AAR files into JAR files and native libraries
2. Exposes these as standard library dependencies
3. Acts as a transparent dependency layer

**Why This Works:**
- Libraries CAN have JAR file dependencies
- Native libraries in `src/main/jniLibs/` are automatically packaged
- No Gradle AAR-in-AAR restriction violation

### 4.2 Wrapper Module Structure

```
Modules/VoiceOS/libraries/{SDK-Name}/
├── build.gradle.kts          (library module config)
├── src/main/
│   ├── AndroidManifest.xml   (minimal manifest)
│   └── jniLibs/              (native libraries)
│       ├── arm64-v8a/        (64-bit ARM)
│       ├── armeabi-v7a/      (32-bit ARM)
│       ├── x86/              (32-bit x86)
│       └── x86_64/           (64-bit x86)
├── libs/                     (extracted JAR files)
│   ├── {sdk}-core.jar
│   ├── {sdk}-asr.jar
│   └── {sdk}-utils.jar
├── proguard-rules.pro        (SDK ProGuard rules)
├── consumer-rules.pro        (rules for consuming modules)
└── README.md                 (update instructions)
```

### 4.3 Benefits

| Benefit | Description |
|---------|-------------|
| **Clean Architecture** | SDK isolation in dedicated wrapper |
| **Standard Build** | No Gradle hacks or workarounds |
| **Reusability** | Other modules can depend on wrapper |
| **Maintainability** | Clear update process documented |
| **Performance** | Native libraries properly packaged for all architectures |

---

## 5. Implementation Guide

### 5.1 Step 1: Identify AAR Contents

Extract each AAR file to examine contents:

```bash
mkdir -p extracted
cd extracted

# AARs are ZIP files
unzip -q ../your-sdk.aar -d your-sdk
ls -la your-sdk/
```

**Typical AAR Contents:**
```
your-sdk/
├── AndroidManifest.xml
├── classes.jar            ← Java/Kotlin classes
├── jni/                   ← Native libraries (optional)
│   ├── arm64-v8a/
│   ├── armeabi-v7a/
│   └── x86_64/
├── R.txt                  ← Resource IDs
├── res/                   ← Resources (optional)
└── proguard.txt          ← ProGuard rules (optional)
```

### 5.2 Step 2: Create Wrapper Module

```bash
# Create wrapper module structure
mkdir -p Modules/VoiceOS/libraries/{SDK-Name}/src/main/jniLibs
mkdir -p Modules/VoiceOS/libraries/{SDK-Name}/libs
```

**Create build.gradle.kts:**

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.augmentalis.{sdkname}"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        targetSdk = 34
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Unpacked SDK JARs
    api(files("libs/{sdk}-core.jar"))
    api(files("libs/{sdk}-asr.jar"))

    // Native libraries in src/main/jniLibs/ (auto-included)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
}
```

**Create AndroidManifest.xml:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Minimal wrapper module manifest -->
</manifest>
```

### 5.3 Step 3: Extract and Copy Files

```bash
# Extract all AAR files
for aar in *.aar; do
    name=$(basename "$aar" .aar)
    unzip -q "$aar" -d "extracted/$name"
done

# Copy classes.jar files
cp extracted/sdk-core/classes.jar \
   Modules/VoiceOS/libraries/{SDK-Name}/libs/sdk-core.jar

cp extracted/sdk-asr/classes.jar \
   Modules/VoiceOS/libraries/{SDK-Name}/libs/sdk-asr.jar

# Copy native libraries
cp -r extracted/sdk-asr/jni/* \
   Modules/VoiceOS/libraries/{SDK-Name}/src/main/jniLibs/

cp -r extracted/sdk-core/jni/* \
   Modules/VoiceOS/libraries/{SDK-Name}/src/main/jniLibs/

# Verify native libraries
ls -la Modules/VoiceOS/libraries/{SDK-Name}/src/main/jniLibs/*/
```

### 5.4 Step 4: Add ProGuard Rules

**proguard-rules.pro:**

```proguard
# {SDK-Name} ProGuard Rules
-keep class com.{vendor}.{sdk}.** { *; }
-keep interface com.{vendor}.{sdk}.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep constructors (if SDK uses reflection)
-keepclassmembers class * {
    public <init>(...);
}
```

**consumer-rules.pro:**

```proguard
# {SDK-Name} Consumer ProGuard Rules
# Automatically applied to consuming modules

-keep class com.{vendor}.{sdk}.** { *; }
-keep interface com.{vendor}.{sdk}.** { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}
```

### 5.5 Step 5: Update Build Configuration

**settings.gradle.kts:**

```kotlin
// Add wrapper module
include(":Modules:VoiceOS:libraries:{SDK-Name}")
```

**VoiceOSCore/build.gradle.kts:**

```kotlin
dependencies {
    // ✅ Use wrapper module instead of direct AARs
    implementation(project(":Modules:VoiceOS:libraries:{SDK-Name}"))

    // ❌ Remove direct AAR references
    // implementation(files("${rootDir}/sdks/your-sdk.aar"))
}
```

### 5.6 Step 6: Create Documentation

**README.md in wrapper module:**

```markdown
# {SDK-Name} Wrapper Module

**Purpose:** Wrapper for {SDK-Name} AAR dependencies to resolve AAR-in-AAR build issues.

## Original AAR Files

Located in: `/{path}/sdks/`

- `{sdk}-core-{version}.aar` ({size})
- `{sdk}-asr-{version}.aar` ({size})

## Updating to New Version

1. Extract new AAR files:
   ```bash
   unzip {sdk}-NEW-VERSION.aar -d extracted/{sdk}-NEW-VERSION
   ```

2. Copy classes and native libraries:
   ```bash
   cp extracted/{sdk}-NEW-VERSION/classes.jar libs/{sdk}-NEW-VERSION.jar
   cp -r extracted/{sdk}-NEW-VERSION/jni/* src/main/jniLibs/
   ```

3. Update `build.gradle.kts` dependencies to reference new version

4. Clean up:
   ```bash
   rm -rf extracted
   ```

## Usage

Other modules should depend on this wrapper:

```kotlin
// ✅ Correct
implementation(project(":Modules:VoiceOS:libraries:{SDK-Name}"))

// ❌ Wrong (causes AAR-in-AAR error)
implementation(files("sdks/{sdk}.aar"))
```
```

### 5.7 Step 7: Verify Build

```bash
# Build wrapper module
./gradlew :Modules:VoiceOS:libraries:{SDK-Name}:assembleDebug

# Build VoiceOSCore
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:assembleDebug

# Build main app
./gradlew :Modules:VoiceOS:apps:VoiceOS:assembleDebug
```

**Expected Results:**
- ✅ All builds succeed without AAR packaging errors
- ✅ Native libraries packaged for all architectures
- ✅ SDK classes accessible from VoiceOSCore

---

## 6. Applying to Other ASR Systems

### 6.1 Common ASR SDKs

| SDK | Format | Complexity | Notes |
|-----|--------|------------|-------|
| **Google Speech** | Maven | Low | Already in Maven Central, no wrapper needed |
| **Azure Cognitive** | Maven | Low | Use standard Maven dependency |
| **Whisper.cpp** | Manual/JNI | High | Requires NDK build + JNI wrapper |
| **Vosk** | Maven | Low | Available in Maven Central |
| **Vivoka VSDK** | AAR | Medium | **Requires wrapper module** (documented here) |
| **Picovoice** | AAR | Medium | Use wrapper pattern if distributed as AAR |
| **CMU Sphinx** | Source | Very High | Build from source + JNI wrapper |

### 6.2 Decision Tree

```
Is SDK distributed as AAR file?
    │
    ├─ No (Maven/JAR) ──────► Use standard Gradle dependency
    │
    └─ Yes (AAR)
           │
           └─ Is VoiceOSCore a library module?
                  │
                  ├─ No ──────► Direct AAR dependency OK
                  │
                  └─ Yes ──────► Use Wrapper Module Pattern
```

### 6.3 Example: Picovoice Porcupine

If Picovoice distributes Porcupine as AAR:

```kotlin
// Create wrapper: Modules/VoiceOS/libraries/PicovoiceSDK/

dependencies {
    // Extracted from picovoice-porcupine-{version}.aar
    api(files("libs/picovoice-core.jar"))
    api(files("libs/porcupine-android.jar"))

    // Native wake word engine libraries auto-included from jniLibs/
}
```

### 6.4 Example: Custom ASR SDK

For proprietary/custom ASR systems distributed as AARs:

```kotlin
// Wrapper module: Modules/VoiceOS/libraries/CustomASR/

dependencies {
    // Unpacked JARs
    api(files("libs/custom-asr-engine.jar"))
    api(files("libs/custom-asr-models.jar"))

    // Native DSP/acoustic model libraries
    // Located in src/main/jniLibs/{abi}/

    implementation(libs.androidx.core.ktx)
}
```

---

## 7. Troubleshooting

### 7.1 Common Issues

| Problem | Cause | Solution |
|---------|-------|----------|
| **Missing native libs** | Not copied to jniLibs/ | Verify `jni/` or `lib{abi}/` dirs in AAR |
| **ClassNotFoundException** | Missing JAR from libs/ | Check all `classes.jar` files extracted |
| **UnsatisfiedLinkError** | Native lib not packaged | Verify jniLibs/ structure matches ABIs |
| **ProGuard removes classes** | Missing keep rules | Add SDK classes to consumer-rules.pro |
| **Build still fails** | Gradle cache issue | `./gradlew clean build --no-build-cache` |

### 7.2 Debugging Steps

**Verify wrapper module contents:**

```bash
# Check JARs
ls -lh Modules/VoiceOS/libraries/{SDK-Name}/libs/

# Check native libraries per ABI
ls -la Modules/VoiceOS/libraries/{SDK-Name}/src/main/jniLibs/*/

# Verify AAR built correctly
unzip -l Modules/VoiceOS/libraries/{SDK-Name}/build/outputs/aar/*.aar
```

**Check if classes are accessible:**

```kotlin
// In VoiceOSCore
import com.vendor.sdk.SomeClass  // Should resolve

fun test() {
    val instance = SomeClass()  // Should compile
}
```

### 7.3 Performance Validation

**Native Library Loading:**

```kotlin
// Monitor native library loading
init {
    try {
        System.loadLibrary("your-sdk-native")
        Log.i(TAG, "✅ Native library loaded")
    } catch (e: UnsatisfiedLinkError) {
        Log.e(TAG, "❌ Failed to load native library", e)
    }
}
```

**APK Inspection:**

```bash
# Check if native libs are in final APK
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep "lib/"

# Expected output:
# lib/arm64-v8a/lib{sdk}.so
# lib/armeabi-v7a/lib{sdk}.so
# lib/x86_64/lib{sdk}.so
```

---

## Appendix A: Alternative Solutions

### A.1 Comparison Table

| Solution | Pros | Cons | Verdict |
|----------|------|------|---------|
| **Wrapper Module (Unpacked)** | ✅ Clean architecture<br>✅ Standard build<br>✅ Reusable | ⚠️ Manual unpacking required | **✅ Recommended** |
| **Local Maven Repository** | ✅ Standard Maven approach<br>✅ Version management | ⚠️ Extra setup scripts<br>⚠️ Local repo management | ✅ Alternative |
| **Change VoiceOSCore to App** | ✅ Simple change | ❌ Breaks architecture<br>❌ Not reusable | ❌ Not viable |
| **Fat AAR Plugin** | ✅ Automated | ❌ Third-party dependency<br>❌ May break with Gradle updates | ⚠️ Risky |
| **Composite Builds** | ✅ Modular | ❌ Complex setup<br>❌ Doesn't solve AAR-in-AAR | ❌ Not applicable |

### A.2 Local Maven Alternative

If you prefer Local Maven approach:

**publish-to-maven.gradle.kts:**

```kotlin
plugins {
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("sdk") {
            groupId = "com.vendor"
            artifactId = "sdk-name"
            version = "1.0.0"
            artifact(file("sdk-name-1.0.0.aar"))
        }
    }

    repositories {
        mavenLocal()
    }
}
```

**Publish:**

```bash
./gradlew -b sdks/publish-to-maven.gradle.kts publishToMavenLocal
```

**Use:**

```kotlin
dependencies {
    implementation("com.vendor:sdk-name:1.0.0")
}
```

**Trade-offs:**
- More automated but requires build scripts
- Version management more complex
- Local Maven repo can become stale

### A.3 When Wrapper Pattern Doesn't Apply

**Use direct dependencies when:**
- SDK available in Maven Central/JCenter
- Module is an application (not library)
- SDK distributed as source code (build from source)

**Example (Maven Central):**

```kotlin
dependencies {
    // ✅ Direct Maven dependency
    implementation("com.microsoft.cognitiveservices.speech:client-sdk:1.38.0")
    implementation("com.alphacephei:vosk-android:0.3.47")
}
```

---

## Summary

**Key Takeaways:**

1. **AAR-in-AAR is forbidden** by Gradle for library modules
2. **Wrapper module pattern** is the cleanest solution
3. **Unpacking AARs** into JARs + native libs works reliably
4. **Pattern applies** to any external SDK distributed as AAR
5. **Document thoroughly** for future SDK updates

**When to Use This Guide:**
- Integrating ASR engines (Vivoka, Picovoice, custom)
- Adding third-party Android SDKs as AARs
- VoiceOSCore remains a library module
- Need clean, maintainable architecture

**Related Documentation:**
- VoiceOS-Infrastructure-Components-Developer-Manual-51222-V1.md
- VoiceOS-Fix-AAR-Packaging-5231202-V1.md (implementation case study)
- Modules/VoiceOS/libraries/VivokaSDK/README.md (reference implementation)

---

**Last Updated:** 2025-12-23
**Maintained By:** VoiceOS Development Team
