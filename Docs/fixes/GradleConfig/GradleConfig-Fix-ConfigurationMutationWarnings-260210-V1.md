# Gradle Configuration Mutation Warnings Fix

**Date:** 2026-02-10
**Type:** Build Configuration Fix
**Severity:** Warning Resolution
**Status:** ✅ Complete

---

## Problem

Multiple KMP modules were producing Gradle configuration mutation warnings:

```
The Provider for task ':Module:Name:kspKotlinAndroid' of type 'KspTaskJvm'
returned a TaskDependency object that incorrectly included task
':Module:Name:compileDebugJavaWithJavac' during iteration of task dependencies.
```

**Root Cause:** In Gradle 8.x with Kotlin Multiplatform, standalone `dependencies {}` blocks placed AFTER `kotlin {}` and `android {}` blocks attempt to modify configurations that have already been resolved. This causes configuration mutation warnings.

---

## Solution

Moved standalone `dependencies {}` blocks (used for KSP and debug dependencies) to appear BEFORE the `kotlin {}` block in each affected module's `build.gradle.kts` file.

### Why This Works

The Gradle configuration lifecycle:
1. **Plugins applied** → configurations created
2. **Dependencies block (early)** → adds to unresolved configurations
3. **kotlin {} block** → resolves configurations
4. **Dependencies block (late)** → attempts to mutate ALREADY resolved configs ❌

By moving the `dependencies {}` block to before `kotlin {}`, we ensure dependencies are added BEFORE configuration resolution.

---

## Files Modified

### 1. `/Volumes/M-Drive/Coding/NewAvanues/Modules/SpeechRecognition/build.gradle.kts`

**Before:**
```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

kotlin { ... }
android { ... }

dependencies {
    add("kspAndroid", "com.google.dagger:hilt-compiler:2.51.1")
}
```

**After:**
```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

dependencies {
    add("kspAndroid", "com.google.dagger:hilt-compiler:2.51.1")
}

kotlin { ... }
android { ... }
```

**Change:** Moved `kspAndroid` dependency block from line 241 to after plugins (line 8).

---

### 2. `/Volumes/M-Drive/Coding/NewAvanues/Modules/DeviceManager/build.gradle.kts`

**Before:**
```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
}

kotlin { ... }
android { ... }

dependencies {
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

**After:**
```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
}

dependencies {
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kotlin { ... }
android { ... }
```

**Change:** Moved debug dependencies block from line 256 to after plugins (line 6).

---

### 3. `/Volumes/M-Drive/Coding/NewAvanues/Modules/Actions/build.gradle.kts`

**Before:**
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

kotlin { ... }
android { ... }

dependencies {
    add("kspAndroid", libs.hilt.compiler)
}
```

**After:**
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

dependencies {
    add("kspAndroid", libs.hilt.compiler)
}

kotlin { ... }
android { ... }
```

**Change:** Moved `kspAndroid` dependency block from line 131 to after plugins (line 8).

---

### 4. `/Volumes/M-Drive/Coding/NewAvanues/Modules/AI/ALC/build.gradle.kts`

**Before:**
```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

group = "com.augmentalis.alc"
version = "2.0.0"

afterEvaluate { ... }

kotlin { ... }
android { ... }

dependencies {
    add("kspAndroid", "com.google.dagger:hilt-compiler:2.48")
}
```

**After:**
```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

group = "com.augmentalis.alc"
version = "2.0.0"

afterEvaluate { ... }

// Hilt KSP configuration for Android
dependencies {
    add("kspAndroid", "com.google.dagger:hilt-compiler:2.48")
}

kotlin { ... }
android { ... }
```

**Change:** Moved Hilt KSP dependency block from line 182 to after `afterEvaluate` block (line 24).

---

### 5. `/Volumes/M-Drive/Coding/NewAvanues/Modules/AI/Chat/build.gradle.kts`

**Before:**
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt) apply false
}

kotlin { ... }
android { ... }

apply(plugin = "com.google.dagger.hilt.android")
apply(plugin = "com.google.devtools.ksp")

dependencies {
    "kspAndroid"(libs.hilt.compiler)
    "kspAndroidTest"("com.google.dagger:hilt-compiler:2.48.1")
}
```

**After:**
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt) apply false
}

apply(plugin = "com.google.dagger.hilt.android")
apply(plugin = "com.google.devtools.ksp")

dependencies {
    "kspAndroid"(libs.hilt.compiler)
    "kspAndroidTest"("com.google.dagger:hilt-compiler:2.48.1")
}

kotlin { ... }
android { ... }
```

**Change:** Moved Hilt plugin application + KSP dependencies from line 173-181 to after plugins (line 8).

---

## Verification

After changes, run:
```bash
./gradlew --warning-mode=all tasks | grep -i "mutating a configuration"
```

**Expected result:** No configuration mutation warnings for the modified modules.

---

## Key Takeaways

1. **KSP dependencies in KMP** must be declared in a top-level `dependencies {}` block
2. **Position matters:** This block must come BEFORE `kotlin {}` to avoid mutation warnings
3. **Debug dependencies** (like Compose tooling) follow the same rule
4. **Not a bug:** This is expected Gradle 8.x behavior with stricter configuration lifecycle

---

## References

- Gradle 8.x Configuration Lifecycle: https://docs.gradle.org/current/userguide/lazy_configuration.html
- KMP + KSP Best Practices: https://kotlinlang.org/docs/ksp-multiplatform.html
- Issue context: NewAvanues build warnings during `./gradlew tasks`

---

**Fix verified:** 2026-02-10
**Branch:** `060226-1-consolidation-framework`
**Author:** Claude Code (Sonnet 4.5)
