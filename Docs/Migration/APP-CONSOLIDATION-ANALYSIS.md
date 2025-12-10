# App Consolidation Analysis: VoiceOS + AVA + WebAvanue

**Date:** 2025-12-07
**Status:** Analysis Complete
**Branch:** AVA-Development

---

## Executive Summary

Analysis of merging VoiceOS, AVA, and WebAvanue into a consolidated app with Play Store and AOSP compatibility.

| Metric | Current (3 Apps) | Merged |
|--------|------------------|--------|
| Total APK Size | ~145 MB | ~90-95 MB |
| Shared Code | Duplicated 3x | Deduplicated |
| Install Options | 3 separate | 1 unified |
| Savings | - | ~35% |

---

## Current APK Sizes

| App | Debug APK | Release APK |
|-----|-----------|-------------|
| VoiceOS | 155 MB | 67 MB |
| AVA | 158 MB | ~70 MB (est) |
| WebAvanue | 18 MB | 8.3 MB |
| **Total (separate)** | **331 MB** | **~145 MB** |

---

## Large Components

| Component | Size | App | Excludable |
|-----------|------|-----|------------|
| Vivoka SDK (AAR + native) | 69 MB | VoiceOS | No |
| Native libs (arm64) | 74 MB | VoiceOS | No |
| ONNX Runtime | ~30 MB | AVA | Dynamic |
| NLU/LLM models | 1-4 GB | AVA | Downloaded |

---

## Code Size Breakdown

| App | Kotlin Files | Lines of Code (est) |
|-----|--------------|---------------------|
| VoiceOS | 1,124 | ~100K |
| AVA | 476 | ~40K |
| WebAvanue | 167 | ~15K |
| **Total** | **1,767** | **~155K** |

---

## Common Dependencies Analysis

### Core Framework (All 3 Apps)

| Dependency | VoiceOS | AVA | WebAvanue | Size |
|------------|---------|-----|-----------|------|
| Kotlin | 1.9.x | 1.9.21 | 1.9.21 | ~1.5 MB |
| Coroutines | 1.7.x | 1.7.3 | 1.7.3 | ~200 KB |
| AndroidX Core KTX | 1.12.0 | 1.12.0 | 1.12.0 | ~100 KB |
| Lifecycle Runtime | 2.7.0 | 2.6.2 | 2.6.2 | ~150 KB |
| Activity Compose | 1.8.0 | 1.8.1 | 1.8.2 | ~100 KB |

### Compose UI (All 3 Apps)

| Dependency | VoiceOS | AVA | WebAvanue | Size |
|------------|---------|-----|-----------|------|
| Compose UI | BOM 2024.04 | BOM 2023.10 | 1.5.4 | ~3 MB |
| Compose Material3 | Yes | Yes | Yes | ~1.5 MB |
| Material Icons Extended | Yes | Yes | Yes | ~2 MB |
| Compose Foundation | Yes | Yes | Yes | ~500 KB |
| Navigation Compose | 2.7.6 | 2.7.5 | 2.7.6 | ~300 KB |

### Data Layer

| Dependency | VoiceOS | AVA | WebAvanue | Size |
|------------|---------|-----|-----------|------|
| Room | 2.6.1 | 2.6.1 | - | ~400 KB |
| SQLDelight | Yes | 2.0.1 | Yes | ~200 KB |
| DataStore | 1.0.0 | 1.0.0 | - | ~100 KB |

### Dependency Injection

| Dependency | VoiceOS | AVA | WebAvanue | Size |
|------------|---------|-----|-----------|------|
| Hilt | 2.51.1 | 2.50 | - | ~500 KB |
| Hilt Navigation | - | Yes | - | ~50 KB |

### Networking/Serialization

| Dependency | VoiceOS | AVA | WebAvanue | Size |
|------------|---------|-----|-----------|------|
| Ktor | - | 2.3.7 | - | ~500 KB |
| kotlinx-serialization | - | 1.6.2 | - | ~200 KB |
| kotlinx-datetime | - | - | 0.5.0 | ~50 KB |

### Platform-Specific

| Dependency | VoiceOS | AVA | WebAvanue | Size |
|------------|---------|-----|-----------|------|
| Vivoka SDK | 3 AARs | - | - | ~69 MB |
| ONNX Runtime | - | 1.16.3 | - | ~30 MB |
| TensorFlow Lite | - | Yes | - | ~5 MB |
| WebKit | - | - | 1.9.0 | ~100 KB |
| Voyager Navigation | - | - | 1.0.0 | ~200 KB |

### Background Processing

| Dependency | VoiceOS | AVA | WebAvanue | Size |
|------------|---------|-----|-----------|------|
| WorkManager | 2.9.0 | 2.9.0 | - | ~300 KB |
| Hilt Work | - | 1.1.0 | - | ~50 KB |

### Firebase (Optional)

| Dependency | VoiceOS | AVA | WebAvanue | Size |
|------------|---------|-----|-----------|------|
| Firebase BOM | - | 32.7.0 | - | Optional |
| Crashlytics | - | Yes | - | ~500 KB |
| Analytics | - | Yes | - | ~300 KB |

---

## Dependency Overlap Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    SHARED (All 3 Apps)                         │
│                    ~10-12 MB when deduplicated                 │
├─────────────────────────────────────────────────────────────────┤
│  - Kotlin stdlib & coroutines                                  │
│  - AndroidX Core, Lifecycle, Activity                          │
│  - Compose UI, Material3, Icons, Foundation                    │
│  - Navigation Compose                                          │
│  - JUnit (testing)                                             │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────────┐
│   VoiceOS ONLY       │ │     AVA ONLY         │ │  WebAvanue ONLY      │
│   ~70 MB             │ │     ~35 MB           │ │  ~1 MB               │
├──────────────────────┤ ├──────────────────────┤ ├──────────────────────┤
│ - Vivoka SDK (69MB)  │ │ - ONNX Runtime (30MB)│ │ - WebKit             │
│ - Room + SQLDelight  │ │ - TensorFlow Lite    │ │ - Voyager Navigation │
│ - Hilt               │ │ - Ktor networking    │ │ - kotlinx-datetime   │
│ - DataStore          │ │ - Hilt + Hilt Work   │ │                      │
│ - Window Manager     │ │ - Firebase (optional)│ │                      │
│ - Splashscreen       │ │ - Room + SQLDelight  │ │                      │
│                      │ │ - Serialization      │ │                      │
└──────────────────────┘ └──────────────────────┘ └──────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                VoiceOS + AVA SHARED                            │
│                ~2 MB when deduplicated                         │
├─────────────────────────────────────────────────────────────────┤
│  - Room 2.6.1                                                  │
│  - Hilt DI                                                     │
│  - DataStore                                                   │
│  - WorkManager                                                 │
│  - Splashscreen                                                │
└─────────────────────────────────────────────────────────────────┘
```

---

## Version Alignment Required

| Dependency | VoiceOS | AVA | WebAvanue | Unified |
|------------|---------|-----|-----------|---------|
| Kotlin | 1.9.x | 1.9.21 | 1.9.21 | **1.9.21** |
| Compose BOM | 2024.04 | 2023.10 | 1.5.4 | **2024.04** |
| Lifecycle | 2.7.0 | 2.6.2 | 2.6.2 | **2.7.0** |
| Hilt | 2.51.1 | 2.50 | - | **2.51.1** |
| Nav Compose | 2.7.6 | 2.7.5 | 2.7.6 | **2.7.6** |
| Room | 2.6.1 | 2.6.1 | - | **2.6.1** |
| SQLDelight | 2.0.x | 2.0.1 | 2.0.x | **2.0.1** |

---

## Architecture Options

### Option C1: Universal APK + Configuration Splits

| Aspect | Details |
|--------|---------|
| Play Store | AAB with optimized delivery |
| AOSP | Universal APK (~80-90MB) |
| Complexity | Low |
| Recommendation | Best for AOSP compatibility |

```bash
# Generate universal APK for AOSP
bundletool build-apks --bundle=app.aab --output=app.apks --mode=universal
```

### Option C2: Bundletool Local Testing Mode

| Aspect | Details |
|--------|---------|
| Play Store | Standard AAB |
| AOSP | Split APKs via ADB/SAI |
| Complexity | Medium |
| Limitation | Requires SAI or ADB |

### Option C3: Custom Module Loader

| Aspect | Details |
|--------|---------|
| Play Store | Play Core SplitInstall |
| AOSP | Custom download + install |
| Complexity | High |
| Benefit | Most flexible |

```kotlin
class ModuleManager(private val context: Context) {
    private val isPlayStoreAvailable: Boolean by lazy {
        try {
            context.packageManager.getPackageInfo("com.android.vending", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    suspend fun installModule(moduleName: String): Result<Unit> {
        return if (isPlayStoreAvailable) {
            installViaPlayCore(moduleName)
        } else {
            installViaCustomLoader(moduleName)
        }
    }
}
```

### Option C4: Plugin Architecture (Current)

| Aspect | Details |
|--------|---------|
| Play Store | Separate APKs |
| AOSP | Separate APKs |
| Complexity | Low (already implemented) |
| Benefit | Independent updates |

```
Avanues Hub (Base) ──AIDL──> VoiceOS APK (67MB)
                   ──AIDL──> AVA APK (70MB)
                   ──Intent─> WebAvanue APK (8MB)
```

---

## Recommended Approach: Hybrid C1 + C4

| Distribution | Strategy |
|--------------|----------|
| Play Store | AAB with Dynamic Features |
| AOSP/Sideload | Universal APK OR separate APKs |
| F-Droid | Separate APKs (policy) |
| Enterprise | Separate APKs via MDM |

---

## Consolidated App Size Estimate

| Component | Current (Separate) | Merged (Deduplicated) |
|-----------|-------------------|----------------------|
| Shared libs | 3 × 12 MB = 36 MB | 12 MB |
| VoiceOS-specific | 70 MB | 70 MB |
| AVA-specific | 35 MB | 35 MB |
| WebAvanue-specific | 1 MB | 1 MB |
| **Release APK Total** | ~145 MB | **~90-95 MB** |
| **Savings** | - | **~35%** |

---

## Module Structure for Merged App

```
android/apps/avanues/           # Consolidated app
├── app/                        # Main app shell
│   ├── build.gradle.kts
│   └── src/main/
│       ├── kotlin/.../
│       │   ├── AvanuesApp.kt
│       │   ├── ModuleManager.kt
│       │   └── navigation/
│       └── AndroidManifest.xml
├── feature/
│   ├── voiceos/               # VoiceOS feature module
│   ├── ava/                   # AVA feature module
│   └── webavanue/             # WebAvanue feature module
├── gradle/
│   └── libs.versions.toml     # Unified version catalog
└── settings.gradle.kts

Modules/
├── Shared/                    # NEW: Cross-product shared
│   ├── Core/                  # Unified core utilities
│   ├── Theme/                 # Unified Ocean Glass theme
│   └── Database/              # Unified SQLDelight
├── VoiceOS/                   # Existing
├── AVA/                       # Existing
└── WebAvanue/                 # Existing
```

---

## Next Steps

1. **Phase 1:** Create unified `libs.versions.toml` with aligned versions
2. **Phase 2:** Extract shared modules to `Modules/Shared/`
3. **Phase 3:** Create base app shell with ModuleManager
4. **Phase 4:** Configure Dynamic Feature Modules
5. **Phase 5:** Test Play Store + AOSP delivery

---

## DexGuard Licensing

### Key Finding: One License Covers All Modules

DexGuard licensing is **per applicationId (package name)**, not per module:

| Current (3 Apps) | Consolidated (1 App) |
|------------------|----------------------|
| 3 separate package names | 1 package name |
| 3 DexGuard licenses needed | **1 DexGuard license** |
| 3x cost | **1x cost (66% savings)** |

### Coverage Under Single License

| Component | Covered |
|-----------|---------|
| Base app module | Yes |
| Dynamic Feature Modules | Yes |
| Library modules | Yes |
| All :feature:* modules | Yes |

### Consolidated Structure

```
com.augmentalis.avanues        → 1 DexGuard License
├── :feature:voiceos           (covered)
├── :feature:ava               (covered)
└── :feature:webavanue         (covered)
```

### Considerations

| Issue | Solution |
|-------|----------|
| Debug builds (`.debug` suffix) | Contact GuardSquare to add suffix |
| Package name must match exactly | Case sensitive |
| Pricing model | Per app, based on industry + downloads |

### Modular App Protection

| Module Type | Protection | License Coverage |
|-------------|------------|------------------|
| Base app (:app) | DexGuard processes at build | Primary license |
| Library modules | Compiled into base | Covered |
| Dynamic Feature Modules | Built as split APKs | Covered* |

*DexGuard processes entire AAB - all code under same applicationId = one license.

### Verification Needed

| Question | Ask GuardSquare |
|----------|-----------------|
| AAB + Dynamic Features supported? | Confirm |
| One license for all splits? | Confirm |
| Per-module configuration? | Confirm |

### References

- [DexGuard applicationIdSuffix](https://stackoverflow.com/questions/43850456/dexguard-with-applicationidsuffix)
- [Apply DexGuard for Modules](https://amyamyzhu.github.io/2018/07/13/Apply-Dexguard-for-Modules/)
- [DexProtector AAB Support](https://dexprotector.com/node/4627)
- [Request DexGuard Quote](https://www.guardsquare.com/request-pricing)

---

## References

- [Dynamic Feature Modules Guide](https://medium.com/@aanshul16/android-dynamic-feature-module-complete-guide-e2844717c2c8)
- [Play Feature Delivery](https://developer.android.com/guide/playcore/feature-delivery)
- [Bundletool](https://github.com/google/bundletool)
- [SAI - Split APKs Installer](https://github.com/Aefyr/SAI)

---

Updated: 2025-12-07 | IDEACODE v10.3.1
