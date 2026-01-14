# Phase 1: KMP Migration Implementation Guide

**Date**: 2025-11-02 01:45 PST
**Duration**: 2 weeks
**Status**: 📋 Ready to Start
**Goal**: Convert AVA from Android-only to Kotlin Multiplatform foundation
**Created by**: Manoj Jhawar, manoj@ideahq.net

---

## Overview

This guide provides step-by-step instructions to migrate AVA's feature modules (NLU, Chat, Overlay) from Android-only to Kotlin Multiplatform (KMP), enabling iOS, macOS, Windows, and Web support.

**What we're doing**:
- ✅ Restructuring source directories for KMP
- ✅ Updating build.gradle.kts for multiplatform targets
- ✅ Moving business logic to commonMain
- ✅ Creating expect/actual declarations for platform code
- ✅ Setting up iOS/macOS/Desktop/JS targets

**What we're NOT doing** (comes in Phase 2+):
- ❌ Implementing iOS/macOS/Windows UI (Phase 3)
- ❌ MagicCode integration (Phase 3)
- ❌ VoiceAvenue integration (Phase 5)
- ❌ Platform-specific testing (Phase 6)

---

## Prerequisites

### Development Environment

**Required**:
- ✅ macOS (for iOS/macOS development)
- ✅ Android Studio or IntelliJ IDEA 2023.3+
- ✅ JDK 17
- ✅ Kotlin 1.9.21
- ✅ Gradle 8.10.2

**For iOS/macOS**:
- ✅ Xcode 15+
- ✅ CocoaPods (for iOS dependencies)
- ✅ Kotlin Multiplatform Mobile (KMM) plugin

**For Windows** (optional, can be done later):
- Windows machine or VM
- Visual Studio 2022

### Knowledge Requirements

**Must understand**:
- Kotlin coroutines and Flow
- Gradle build system
- Android architecture (already familiar)

**Should learn**:
- KMP basics: expect/actual pattern
- Source set hierarchy
- Platform-specific dependencies

**Recommended reading**:
- [Kotlin Multiplatform Docs](https://kotlinlang.org/docs/multiplatform.html)
- [KMP Source Sets](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets)
- [expect/actual](https://kotlinlang.org/docs/multiplatform-expect-actual.html)

---

## Step 1: Update Root Build Configuration

### 1.1 Update gradle/libs.versions.toml

**Current**:
```toml
[versions]
kotlin = "1.9.21"
compose = "1.5.4"

[plugins]
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

**Add**:
```toml
[versions]
kotlin = "1.9.21"
compose = "1.5.4"
ksp = "1.9.21-1.0.15"

[plugins]
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-cocoapods = { id = "org.jetbrains.kotlin.native.cocoapods", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version = "1.5.11" }
sqldelight = { id = "app.cash.sqldelight", version = "2.0.1" }
```

### 1.2 Update root build.gradle.kts

**Add at top**:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false  // ADD THIS
    alias(libs.plugins.compose.multiplatform) apply false // ADD THIS
    alias(libs.plugins.sqldelight) apply false            // ADD THIS
}
```

**Commit**: `git commit -m "build: Add KMP plugins to root build configuration"`

---

## Step 2: Migrate features/nlu Module

### 2.1 Backup Current Structure

```bash
cd /Users/manoj_mbpm14/Coding/ava
cp -r features/nlu features/nlu.backup
```

### 2.2 Update features/nlu/build.gradle.kts

**Replace entire file**:
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    // Android target
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "NLU"
            isStatic = true
        }
    }

    // macOS targets
    listOf(
        macosX64(),
        macosArm64()
    ).forEach { macosTarget ->
        macosTarget.binaries.framework {
            baseName = "NLU"
            isStatic = true
        }
    }

    // JVM target (for Windows/Linux desktop)
    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // JS target (for Web)
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core:common"))
                implementation(project(":core:domain"))

                // Kotlin coroutines (KMP)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        val androidMain by getting {
            dependencies {
                // ONNX Runtime Android
                implementation("ai.onnxruntime:onnxruntime-android:1.17.0")

                // Android coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                // iOS-specific dependencies
                // Note: ONNX Runtime iOS needs native binding
            }
        }

        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

        val macosMain by creating {
            dependsOn(commonMain)
            dependencies {
                // macOS-specific dependencies
            }
        }

        val macosX64Main by getting { dependsOn(macosMain) }
        val macosArm64Main by getting { dependsOn(macosMain) }

        val desktopMain by getting {
            dependencies {
                // ONNX Runtime JVM (for Windows/Linux)
                implementation("com.microsoft.onnxruntime:onnxruntime:1.17.0")
            }
        }

        val jsMain by getting {
            dependencies {
                // TensorFlow.js or ONNX Runtime Web
                // implementation(npm("@tensorflow/tfjs", "4.10.0"))
            }
        }
    }
}

android {
    namespace = "com.augmentalis.ava.features.nlu"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Configure source sets for KMP
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
}
```

### 2.3 Restructure Source Directories

**Create new KMP structure**:
```bash
cd features/nlu

# Create commonMain structure
mkdir -p src/commonMain/kotlin/com/augmentalis/ava/features/nlu/domain
mkdir -p src/commonMain/kotlin/com/augmentalis/ava/features/nlu/usecase

# Create platform-specific structures
mkdir -p src/androidMain/kotlin/com/augmentalis/ava/features/nlu/platform
mkdir -p src/iosMain/kotlin/com/augmentalis/ava/features/nlu/platform
mkdir -p src/macosMain/kotlin/com/augmentalis/ava/features/nlu/platform
mkdir -p src/desktopMain/kotlin/com/augmentalis/ava/features/nlu/platform
mkdir -p src/jsMain/kotlin/com/augmentalis/ava/features/nlu/platform

# Move AndroidManifest
mkdir -p src/androidMain
mv src/main/AndroidManifest.xml src/androidMain/ 2>/dev/null || true
```

### 2.4 Create Shared Interface in commonMain

**File**: `src/commonMain/kotlin/com/augmentalis/ava/features/nlu/domain/IntentClassifier.kt`

```kotlin
package com.augmentalis.ava.features.nlu.domain

import com.augmentalis.ava.core.common.Result

/**
 * Intent classification data class (shared across all platforms)
 */
data class IntentClassification(
    val intent: String,
    val confidence: Float,
    val inferenceTimeMs: Long = 0,
    val allScores: Map<String, Float> = emptyMap()
)

/**
 * Intent classifier interface (expect declaration for platform implementations)
 */
expect class IntentClassifier {
    suspend fun initialize(modelPath: String): Result<Unit>

    suspend fun classifyIntent(
        text: String,
        candidateIntents: List<String>
    ): Result<IntentClassification>

    fun close()

    companion object {
        fun getInstance(context: Any): IntentClassifier
    }
}
```

### 2.5 Create Android Implementation (actual)

**File**: `src/androidMain/kotlin/com/augmentalis/ava/features/nlu/domain/IntentClassifier.kt`

```kotlin
package com.augmentalis.ava.features.nlu.domain

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.features.nlu.platform.BertTokenizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.LongBuffer

actual class IntentClassifier private constructor(
    private val context: Context
) {
    private lateinit var ortEnvironment: OrtEnvironment
    private lateinit var ortSession: OrtSession
    private lateinit var tokenizer: BertTokenizer
    private var isInitialized = false

    actual suspend fun initialize(modelPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isInitialized) {
                return@withContext Result.Success(Unit)
            }

            ortEnvironment = OrtEnvironment.getEnvironment()

            val modelFile = File(context.filesDir, modelPath)
            if (!modelFile.exists()) {
                return@withContext Result.Error(
                    exception = IllegalStateException("Model not found: $modelPath"),
                    message = "ONNX model not found at $modelPath"
                )
            }

            val sessionOptions = OrtSession.SessionOptions().apply {
                addNnapi()
                setIntraOpNumThreads(4)
                setInterOpNumThreads(2)
            }

            ortSession = ortEnvironment.createSession(
                modelFile.absolutePath,
                sessionOptions
            )

            tokenizer = BertTokenizer(context)
            isInitialized = true

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to initialize ONNX Runtime: ${e.message}"
            )
        }
    }

    actual suspend fun classifyIntent(
        text: String,
        candidateIntents: List<String>
    ): Result<IntentClassification> = withContext(Dispatchers.Default) {
        try {
            if (!isInitialized) {
                return@withContext Result.Error(
                    exception = IllegalStateException("Not initialized"),
                    message = "Call initialize() first"
                )
            }

            if (text.isBlank()) {
                return@withContext Result.Error(
                    exception = IllegalArgumentException("Empty text"),
                    message = "Text cannot be empty"
                )
            }

            if (candidateIntents.isEmpty()) {
                return@withContext Result.Error(
                    exception = IllegalArgumentException("No candidates"),
                    message = "At least one candidate intent required"
                )
            }

            // Tokenize input
            val tokens = tokenizer.tokenize(text)

            // Create ONNX tensors
            val inputIdsTensor = OnnxTensor.createTensor(
                ortEnvironment,
                LongBuffer.wrap(tokens.inputIds.map { it.toLong() }.toLongArray()),
                longArrayOf(1, tokens.inputIds.size.toLong())
            )

            val attentionMaskTensor = OnnxTensor.createTensor(
                ortEnvironment,
                LongBuffer.wrap(tokens.attentionMask.map { it.toLong() }.toLongArray()),
                longArrayOf(1, tokens.attentionMask.size.toLong())
            )

            val tokenTypeIdsTensor = OnnxTensor.createTensor(
                ortEnvironment,
                LongBuffer.wrap(tokens.tokenTypeIds.map { it.toLong() }.toLongArray()),
                longArrayOf(1, tokens.tokenTypeIds.size.toLong())
            )

            // Run inference
            val startTime = System.currentTimeMillis()
            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to attentionMaskTensor,
                "token_type_ids" to tokenTypeIdsTensor
            )

            val outputs = ortSession.run(inputs)
            val inferenceTime = System.currentTimeMillis() - startTime

            // Extract and process results
            val logits = outputs[0].value as Array<FloatArray>
            val scores = logits[0]
            val probabilities = softmax(scores)

            val bestIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
            val confidence = probabilities[bestIndex]

            val intent = if (bestIndex < candidateIntents.size) {
                candidateIntents[bestIndex]
            } else {
                "unknown"
            }

            // Cleanup
            inputIdsTensor.close()
            attentionMaskTensor.close()
            tokenTypeIdsTensor.close()
            outputs.close()

            Result.Success(
                IntentClassification(
                    intent = intent,
                    confidence = confidence,
                    inferenceTimeMs = inferenceTime,
                    allScores = candidateIntents.zip(probabilities.take(candidateIntents.size)).toMap()
                )
            )
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Classification failed: ${e.message}"
            )
        }
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val expScores = logits.map { kotlin.math.exp((it - maxLogit).toDouble()).toFloat() }
        val sumExp = expScores.sum()
        return expScores.map { it / sumExp }.toFloatArray()
    }

    actual fun close() {
        if (isInitialized) {
            ortSession.close()
            ortEnvironment.close()
            isInitialized = false
        }
    }

    actual companion object {
        @Volatile
        private var INSTANCE: IntentClassifier? = null

        actual fun getInstance(context: Any): IntentClassifier {
            require(context is Context) { "Android requires android.content.Context" }

            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: IntentClassifier(context.applicationContext as Context).also {
                    INSTANCE = it
                }
            }
        }
    }
}
```

### 2.6 Create iOS Stub Implementation

**File**: `src/iosMain/kotlin/com/augmentalis/ava/features/nlu/domain/IntentClassifier.kt`

```kotlin
package com.augmentalis.ava.features.nlu.domain

import com.augmentalis.ava.core.common.Result

actual class IntentClassifier {
    actual suspend fun initialize(modelPath: String): Result<Unit> {
        // TODO: Implement ONNX Runtime iOS or CoreML
        return Result.Error(
            exception = NotImplementedError("iOS implementation pending"),
            message = "iOS NLU not yet implemented"
        )
    }

    actual suspend fun classifyIntent(
        text: String,
        candidateIntents: List<String>
    ): Result<IntentClassification> {
        // TODO: Implement iOS classification
        return Result.Error(
            exception = NotImplementedError("iOS implementation pending"),
            message = "iOS NLU not yet implemented"
        )
    }

    actual fun close() {
        // TODO: Cleanup iOS resources
    }

    actual companion object {
        actual fun getInstance(context: Any): IntentClassifier {
            return IntentClassifier()
        }
    }
}
```

### 2.7 Move Existing Android Code

**Move Android-specific files**:
```bash
cd features/nlu

# Move existing implementation files to androidMain/platform
mv src/main/kotlin/com/augmentalis/ava/features/nlu/BertTokenizer.kt \
   src/androidMain/kotlin/com/augmentalis/ava/features/nlu/platform/

mv src/main/kotlin/com/augmentalis/ava/features/nlu/ModelManager.kt \
   src/androidMain/kotlin/com/augmentalis/ava/features/nlu/platform/

mv src/main/kotlin/com/augmentalis/ava/features/nlu/NLUInitializer.kt \
   src/androidMain/kotlin/com/augmentalis/ava/features/nlu/platform/

# Move use cases to commonMain (they're platform-agnostic)
mv src/main/kotlin/com/augmentalis/ava/features/nlu/usecase/*.kt \
   src/commonMain/kotlin/com/augmentalis/ava/features/nlu/usecase/

# Remove old main directory
rm -rf src/main/kotlin
```

### 2.8 Build and Verify

```bash
cd /Users/manoj_mbpm14/Coding/ava

# Clean build
./gradlew clean

# Build NLU module for all targets
./gradlew :features:nlu:build

# Verify Android target
./gradlew :features:nlu:compileDebugKotlinAndroid

# Verify iOS targets (requires macOS)
./gradlew :features:nlu:compileKotlinIosX64
./gradlew :features:nlu:compileKotlinIosArm64

# Verify Desktop target
./gradlew :features:nlu:compileKotlinDesktop

# Verify JS target
./gradlew :features:nlu:compileKotlinJs
```

**Expected**: All targets compile successfully

**Commit**:
```bash
git add features/nlu
git commit -m "feat(nlu): Migrate to Kotlin Multiplatform

Converted features/nlu from Android-only to KMP with support for:
- Android (ONNX Runtime implementation)
- iOS (stub, implementation pending)
- macOS (stub, implementation pending)
- Desktop/JVM (Windows/Linux, stub pending)
- JS/Web (stub, implementation pending)

Changes:
- Updated build.gradle.kts for multiplatform targets
- Restructured to commonMain/androidMain/iosMain/etc
- Created expect/actual IntentClassifier interface
- Moved Android ONNX implementation to androidMain
- Moved platform-agnostic use cases to commonMain
- Created iOS/macOS/Desktop/JS stubs

Build Status: ✅ All targets compile

Created by Manoj Jhawar, manoj@ideahq.net"
```

---

## Step 3: Migrate features/chat Module

### 3.1 Similar Process

Follow the same steps as NLU:
1. Update build.gradle.kts
2. Create KMP source structure
3. Move ViewModels to commonMain
4. Create expect/actual for platform-specific code
5. Move Android-specific code to androidMain

### 3.2 Key Differences for Chat

**What goes in commonMain**:
- ✅ ChatViewModel (business logic)
- ✅ Message models
- ✅ Use cases (SendMessageUseCase, etc.)
- ✅ Repository interfaces

**What needs expect/actual**:
- ⚠️ Text-to-Speech (different per platform)
- ⚠️ Speech-to-Text (different per platform)
- ⚠️ Platform context/lifecycle

**Example expect/actual for TTS**:
```kotlin
// commonMain
expect class TextToSpeech {
    fun speak(text: String)
    fun stop()
}

// androidMain
actual class TextToSpeech actual constructor() {
    private val tts = android.speech.tts.TextToSpeech(...)
    actual fun speak(text: String) { tts.speak(text, ...) }
    actual fun stop() { tts.stop() }
}

// iosMain
actual class TextToSpeech actual constructor() {
    private val synthesizer = platform.AVFoundation.AVSpeechSynthesizer()
    actual fun speak(text: String) {
        val utterance = platform.AVFoundation.AVSpeechUtterance(text)
        synthesizer.speakUtterance(utterance)
    }
    actual fun stop() { synthesizer.stopSpeaking(...) }
}
```

---

## Step 4: Migrate features/overlay Module

### 4.1 Overlay-Specific Challenges

**Problem**: Overlay system requires system-level permissions that vary by platform

**Android**:
- ✅ Uses SYSTEM_ALERT_WINDOW
- ✅ Accessibility Service
- ✅ Foreground Service

**iOS**:
- ❌ No system overlays allowed
- ✅ Alternative: Siri Shortcuts
- ✅ Alternative: Share Sheet extension
- ✅ Alternative: Notification-based suggestions

**macOS**:
- ✅ Overlay windows allowed
- ✅ Accessibility API

**Windows**:
- ✅ Overlay windows allowed
- ✅ WinAPI or Compose Desktop

### 4.2 Expect/Actual for Overlay

```kotlin
// commonMain
expect class OverlayManager {
    suspend fun requestPermission(): Boolean
    suspend fun showOverlay(config: OverlayConfig)
    suspend fun hideOverlay()
    fun registerContextListener(listener: (AppContext) -> Unit)
}

// androidMain
actual class OverlayManager actual constructor() {
    actual suspend fun requestPermission(): Boolean {
        // Request SYSTEM_ALERT_WINDOW
    }

    actual suspend fun showOverlay(config: OverlayConfig) {
        // Create overlay window
    }
}

// iosMain
actual class OverlayManager actual constructor() {
    actual suspend fun requestPermission(): Boolean {
        // iOS: Always return true, use alternative methods
        return true
    }

    actual suspend fun showOverlay(config: OverlayConfig) {
        // iOS: Show as notification or Siri suggestion
        // Cannot show true overlay
    }
}
```

---

## Step 5: Update Dependent Modules

### 5.1 Update overlay/NluConnector

**File**: `features/overlay/src/main/java/.../NluConnector.kt`

**Before**:
```kotlin
import com.augmentalis.ava.features.nlu.IntentClassifier
```

**After**:
```kotlin
import com.augmentalis.ava.features.nlu.domain.IntentClassifier
```

Update package imports to point to new `domain` package in commonMain.

### 5.2 Update Tests

Tests need to handle KMP structure:
```kotlin
// androidTest (stays in androidTest)
class IntentClassifierAndroidTest {
    @Test
    fun testONNXInference() {
        // Tests actual Android ONNX implementation
    }
}

// commonTest (new, for shared logic)
class IntentClassificationTest {
    @Test
    fun testDataClassEquals() {
        // Tests shared data structures
    }
}
```

---

## Step 6: Verification & Testing

### 6.1 Build Verification Checklist

```bash
# Clean everything
./gradlew clean

# Build all modules for Android
./gradlew :features:nlu:assembleDebug
./gradlew :features:chat:assembleDebug
./gradlew :features:overlay:assembleDebug

# Build for iOS (requires macOS)
./gradlew :features:nlu:linkDebugFrameworkIosArm64
./gradlew :features:chat:linkDebugFrameworkIosArm64

# Build for Desktop
./gradlew :features:nlu:desktopJar
./gradlew :features:chat:desktopJar

# Build for JS
./gradlew :features:nlu:jsBrowserDevelopmentWebpack
./gradlew :features:chat:jsBrowserDevelopmentWebpack

# Run tests
./gradlew :features:nlu:testDebugUnitTest
./gradlew :features:chat:testDebugUnitTest
./gradlew :features:overlay:testDebugUnitTest
```

### 6.2 Success Criteria

**Must have**:
- ✅ All modules compile for all targets
- ✅ Android app still works (no regression)
- ✅ All unit tests pass
- ✅ No unresolved references

**Nice to have** (Phase 2):
- ⏳ iOS stubs replaced with implementations
- ⏳ Desktop implementations complete
- ⏳ JS implementations complete

---

## Step 7: Documentation

### 7.1 Update Architecture Docs

Create: `docs/architecture/KMP-Architecture.md`

Document:
- Source set hierarchy
- expect/actual patterns used
- Platform-specific implementations
- Shared vs platform code percentages

### 7.2 Update Developer Guide

Create: `docs/guides/Building-For-Platforms.md`

Include:
- How to build for each platform
- Platform-specific requirements
- Testing procedures
- Troubleshooting

---

## Common Issues & Solutions

### Issue 1: "Cannot access class from commonMain"

**Problem**: Android code trying to access Android-specific types from commonMain

**Solution**: Use expect/actual pattern
```kotlin
// BAD - Android type in commonMain
fun foo(context: android.content.Context) { }

// GOOD - expect/actual
expect class PlatformContext
actual typealias PlatformContext = android.content.Context // androidMain
```

### Issue 2: "Unresolved reference: androidMain"

**Problem**: Gradle cache issues

**Solution**:
```bash
./gradlew clean
rm -rf .gradle
./gradlew build
```

### Issue 3: "Source set not found"

**Problem**: Source set hierarchy not configured

**Solution**: Check `build.gradle.kts` - ensure `iosMain by creating` uses `creating`, not `getting`

### Issue 4: iOS build fails with "Framework not found"

**Problem**: CocoaPods dependencies not installed

**Solution**:
```bash
cd iosApp
pod install
```

---

## Next Steps After Phase 1

Once Phase 1 is complete:

**Phase 2** (Week 3-4): Implement platform-specific NLU
- Replace iOS stubs with CoreML or ONNX Runtime iOS
- Implement Desktop ONNX Runtime
- Implement Web TensorFlow.js

**Phase 3** (Week 5-6): MagicCode UI migration
- Define UI in MagicCode DSL
- Generate platform-specific UI
- Test on all platforms

**Phase 4** (Week 7-8): Platform-specific features
- iOS: Siri, Widgets
- macOS: Menu bar
- Windows: System tray

---

## Rollback Plan

If migration fails:

```bash
# Restore from backup
rm -rf features/nlu features/chat features/overlay
mv features/nlu.backup features/nlu
mv features/chat.backup features/chat
mv features/overlay.backup features/overlay

# Restore build files
git checkout features/nlu/build.gradle.kts
git checkout features/chat/build.gradle.kts
git checkout features/overlay/build.gradle.kts

# Clean and rebuild
./gradlew clean build
```

---

## Summary

**What you accomplished in Phase 1**:
- ✅ Converted 3 feature modules to KMP
- ✅ Setup all platform targets (iOS, macOS, Desktop, JS)
- ✅ Created expect/actual architecture
- ✅ Maintained Android functionality (no regression)
- ✅ Established foundation for cross-platform development

**Code reuse achieved**:
- Domain models: 100% shared
- Business logic: 70-80% shared
- Platform code: 20-30% per platform

**Ready for**:
- Phase 2: Platform-specific implementations
- Phase 3: MagicCode UI generation
- Phase 4: Platform-specific features
- Phase 5: VoiceAvenue integration

---

**Created by**: Manoj Jhawar, manoj@ideahq.net
**Last Updated**: 2025-11-02 01:45 PST
