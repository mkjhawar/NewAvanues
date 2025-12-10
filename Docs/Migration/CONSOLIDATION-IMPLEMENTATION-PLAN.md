# Avanues Consolidated App - Implementation Plan

**Date:** 2025-12-07
**Status:** Planning
**Target:** Play Store + AOSP Compatible

---

## Overview

Consolidate VoiceOS, AVA, and WebAvanue into a single modular app using Dynamic Feature Modules with dual distribution support.

---

## Phase 1: Unified Version Catalog

**Duration:** 1 day
**Branch:** `feature/unified-versions`

### Tasks

| Task | File | Action |
|------|------|--------|
| 1.1 | Create unified `libs.versions.toml` | Align all versions |
| 1.2 | Update VoiceOS versions | Compose BOM 2024.04 |
| 1.3 | Update AVA versions | Lifecycle 2.7.0, Hilt 2.51.1 |
| 1.4 | Update WebAvanue versions | Add Hilt, align Compose |

### Unified versions.toml

```toml
[versions]
kotlin = "1.9.21"
agp = "8.2.0"
composeBom = "2024.04.01"
lifecycle = "2.7.0"
hilt = "2.51.1"
room = "2.6.1"
sqldelight = "2.0.1"
coroutines = "1.7.3"
navigation = "2.7.6"
ktor = "2.3.7"
onnxruntime = "1.16.3"
```

---

## Phase 2: Shared Modules Extraction

**Duration:** 2-3 days
**Branch:** `feature/shared-modules`

### New Structure

```
Modules/
├── Shared/                         # NEW
│   ├── Core/                       # Unified utilities
│   │   ├── build.gradle.kts
│   │   └── src/commonMain/kotlin/
│   │       └── com/augmentalis/shared/
│   │           ├── logging/
│   │           ├── utils/
│   │           └── extensions/
│   ├── Theme/                      # Ocean Glass unified
│   │   └── src/commonMain/kotlin/
│   │       └── com/augmentalis/shared/theme/
│   ├── Database/                   # SQLDelight schemas
│   │   └── src/commonMain/sqldelight/
│   └── Navigation/                 # Shared navigation
│       └── src/commonMain/kotlin/
├── VoiceOS/                        # Existing (refactored)
├── AVA/                            # Existing (refactored)
└── WebAvanue/                      # Existing (refactored)
```

### Tasks

| Task | Action |
|------|--------|
| 2.1 | Create `Modules/Shared/Core` with logging, utils |
| 2.2 | Extract Ocean Glass theme to `Modules/Shared/Theme` |
| 2.3 | Create unified SQLDelight schema in `Modules/Shared/Database` |
| 2.4 | Update VoiceOS to depend on Shared modules |
| 2.5 | Update AVA to depend on Shared modules |
| 2.6 | Update WebAvanue to depend on Shared modules |

---

## Phase 3: Base App Shell

**Duration:** 2 days
**Branch:** `feature/avanues-base`

### New App Structure

```
android/apps/avanues/
├── app/                            # Base app shell
│   ├── build.gradle.kts
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── kotlin/com/augmentalis/avanues/
│   │       ├── AvanuesApplication.kt
│   │       ├── MainActivity.kt
│   │       ├── navigation/
│   │       │   └── AvanuesNavHost.kt
│   │       ├── module/
│   │       │   ├── ModuleManager.kt
│   │       │   └── ModuleInstaller.kt
│   │       └── ui/
│   │           ├── HomeScreen.kt
│   │           └── ModuleSelectionScreen.kt
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml
├── settings.gradle.kts
├── build.gradle.kts
└── gradle.properties
```

### ModuleManager Implementation

```kotlin
// ModuleManager.kt
class ModuleManager @Inject constructor(
    private val context: Context
) {
    private val splitInstallManager: SplitInstallManager? by lazy {
        if (isPlayStoreAvailable()) {
            SplitInstallManagerFactory.create(context)
        } else null
    }

    private fun isPlayStoreAvailable(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.android.vending", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isModuleInstalled(moduleName: String): Boolean {
        return splitInstallManager?.installedModules?.contains(moduleName)
            ?: isModuleBundled(moduleName)
    }

    private fun isModuleBundled(moduleName: String): Boolean {
        // For universal APK, all modules are bundled
        return try {
            Class.forName(getModuleEntryClass(moduleName))
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    suspend fun installModule(moduleName: String): Result<Unit> {
        return if (splitInstallManager != null) {
            installViaPlayCore(moduleName)
        } else {
            // Universal APK - module already included
            Result.success(Unit)
        }
    }

    private suspend fun installViaPlayCore(moduleName: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            val request = SplitInstallRequest.newBuilder()
                .addModule(moduleName)
                .build()

            splitInstallManager?.startInstall(request)
                ?.addOnSuccessListener { continuation.resume(Result.success(Unit)) }
                ?.addOnFailureListener { continuation.resume(Result.failure(it)) }
        }
    }
}
```

---

## Phase 4: Dynamic Feature Modules

**Duration:** 3-4 days
**Branch:** `feature/dynamic-modules`

### Feature Module Structure

```
android/apps/avanues/
├── feature/
│   ├── voiceos/
│   │   ├── build.gradle.kts
│   │   ├── src/main/
│   │   │   ├── AndroidManifest.xml
│   │   │   └── kotlin/.../voiceos/
│   │   │       └── VoiceOSFeature.kt
│   │   └── proguard-rules.pro
│   ├── ava/
│   │   ├── build.gradle.kts
│   │   ├── src/main/
│   │   │   ├── AndroidManifest.xml
│   │   │   └── kotlin/.../ava/
│   │   │       └── AvaFeature.kt
│   │   └── proguard-rules.pro
│   └── webavanue/
│       ├── build.gradle.kts
│       ├── src/main/
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/.../webavanue/
│       │       └── WebAvanueFeature.kt
│       └── proguard-rules.pro
```

### Feature Module build.gradle.kts

```kotlin
// feature/voiceos/build.gradle.kts
plugins {
    alias(libs.plugins.android.dynamic.feature)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.augmentalis.avanues.feature.voiceos"
    compileSdk = 34

    defaultConfig {
        minSdk = 28
    }
}

dependencies {
    implementation(project(":app"))
    implementation(project(":Modules:Shared:Core"))
    implementation(project(":Modules:Shared:Theme"))
    implementation(project(":Modules:VoiceOS:apps:VoiceOSCore"))
    implementation(project(":Modules:VoiceOS:apps:VoiceCursor"))
    // ... other VoiceOS modules
}
```

### AndroidManifest.xml (Feature Module)

```xml
<!-- feature/voiceos/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:dist="http://schemas.android.com/apk/distribution">

    <dist:module
        dist:instant="false"
        dist:title="@string/feature_voiceos">
        <dist:delivery>
            <dist:on-demand />
        </dist:delivery>
        <dist:fusing dist:include="true" />
    </dist:module>

    <application>
        <activity
            android:name=".VoiceOSActivity"
            android:exported="false" />
    </application>
</manifest>
```

---

## Phase 5: AOSP Compatibility Layer

**Duration:** 1-2 days
**Branch:** `feature/aosp-compat`

### Universal APK Generation

```bash
# Build AAB
./gradlew bundleRelease

# Generate Universal APK for AOSP
bundletool build-apks \
    --bundle=app/build/outputs/bundle/release/app-release.aab \
    --output=app-release.apks \
    --mode=universal

# Extract universal APK
unzip app-release.apks -d output/
mv output/universal.apk avanues-universal-release.apk
```

### Build Variants

```kotlin
// app/build.gradle.kts
android {
    flavorDimensions += "distribution"

    productFlavors {
        create("playstore") {
            dimension = "distribution"
            // Dynamic delivery enabled
        }
        create("aosp") {
            dimension = "distribution"
            // All modules fused
        }
    }

    bundle {
        language { enableSplit = false }
        density { enableSplit = true }
        abi { enableSplit = true }
    }
}
```

---

## Phase 6: Build & Test

**Duration:** 2-3 days
**Branch:** `feature/consolidation-test`

### Test Matrix

| Scenario | Play Store | AOSP |
|----------|------------|------|
| Fresh install | Dynamic download | Universal APK |
| Module install | On-demand | Already bundled |
| Update | Delta updates | Full APK |
| Offline | Cached modules | Full functionality |

### Build Commands

```bash
# Play Store AAB
./gradlew bundlePlaystoreRelease

# AOSP Universal APK
./gradlew bundleAospRelease
bundletool build-apks --mode=universal ...

# Debug builds
./gradlew assemblePlaystoreDebug
./gradlew assembleAospDebug
```

---

## Phase 7: DexGuard Integration

**Duration:** 1 day
**Branch:** `feature/dexguard`

### Configuration

```kotlin
// app/build.gradle.kts
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "dexguard-rules.pro"
            )
        }
    }
}
```

### DexGuard Rules

```proguard
# dexguard-rules.pro
-keep class com.augmentalis.avanues.** { *; }
-keep class com.augmentalis.shared.** { *; }

# Feature modules
-keep class com.augmentalis.avanues.feature.voiceos.** { *; }
-keep class com.augmentalis.avanues.feature.ava.** { *; }
-keep class com.augmentalis.avanues.feature.webavanue.** { *; }
```

---

## Timeline Summary

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| 1. Unified Versions | 1 day | None |
| 2. Shared Modules | 2-3 days | Phase 1 |
| 3. Base App Shell | 2 days | Phase 2 |
| 4. Dynamic Features | 3-4 days | Phase 3 |
| 5. AOSP Compat | 1-2 days | Phase 4 |
| 6. Build & Test | 2-3 days | Phase 5 |
| 7. DexGuard | 1 day | Phase 6 |
| **Total** | **12-16 days** | |

---

## Deliverables

| Deliverable | Format | Distribution |
|-------------|--------|--------------|
| `avanues-release.aab` | Android App Bundle | Play Store |
| `avanues-universal.apk` | Universal APK | AOSP/Sideload |
| `avanues-voiceos.apk` | Standalone | F-Droid (optional) |
| `avanues-ava.apk` | Standalone | F-Droid (optional) |

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Module dependency conflicts | Unified version catalog |
| Play Core unavailable on AOSP | ModuleManager fallback |
| DexGuard AAB support | Confirm with GuardSquare |
| Large universal APK | ABI splits, on-demand assets |

---

## Success Criteria

- [ ] Single codebase builds for Play Store + AOSP
- [ ] One DexGuard license covers all modules
- [ ] APK size < 100MB (universal)
- [ ] All existing functionality preserved
- [ ] Module install works on Play Store
- [ ] Universal APK works on AOSP

---

Updated: 2025-12-07 | IDEACODE v10.3.1
