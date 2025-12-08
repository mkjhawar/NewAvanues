# ObjectBox Issue Analysis — 2025-09-03

## Context
You reported issues with **ObjectBox generated classes** (`MyObjectBox`, `RecognitionLearning_`) not being created, causing build failures.  
Your stack: **Gradle 8.9, AGP 8.5.x, Kotlin 1.9.24**, multi-module, using **KSP elsewhere**.

---

## Root Causes
1. **KSP vs KAPT**  
   - ObjectBox still requires **KAPT** (annotation processor).  
   - Using only KSP prevents `_` class generation.

2. **Plugin placement**  
   - `id("io.objectbox")` must be applied in **every module with @Entity**, not just root.

3. **Multi-module setup**  
   - Entities in `:core` but plugin/kapt only in `:app` → generation skipped.

4. **Namespace/package changes**  
   - If `namespace` differs from `package`, generated classes won’t resolve.

5. **Cache/IDE issues**  
   - Old generated sources not flushed → imports look missing.

---

## Fix — Golden Path

### Root `gradle/libs.versions.toml`
```toml
[versions]
objectbox = "4.3.1"
kotlin = "1.9.25"
agp = "8.5.2"
```

### Module `build.gradle.kts`
```kotlin
plugins {
  id("com.android.library") // or application
  kotlin("android")
  kotlin("kapt")
  id("io.objectbox")
}

android {
  namespace = "voiceos.data"
  compileSdk = 35
  defaultConfig { minSdk = 26 }
}

dependencies {
  implementation("io.objectbox:objectbox-kotlin:${libs.versions.objectbox.get()}")
  kapt("io.objectbox:objectbox-processor:${libs.versions.objectbox.get()}")
}

kapt {
  correctErrorTypes = true
  useBuildCache = true
  arguments { arg("objectbox.incremental", "true") }
}
```

### Entity Example
```kotlin
@Entity
data class RecognitionLearning(
  @Id var id: Long = 0,
  val phrase: String,
  val locale: String,
  val confidence: Double
)
```
→ Generates `RecognitionLearning_` + `MyObjectBox`.

### Init in `Application`
```kotlin
class VoiceOsApp : Application() {
  lateinit var boxStore: BoxStore
  override fun onCreate() {
    super.onCreate()
    boxStore = MyObjectBox.builder()
      .androidContext(this)
      .build()
  }
}
```

---

## Checklist
- [ ] `kotlin("kapt")` in all modules with entities  
- [ ] `id("io.objectbox")` in those modules  
- [ ] Entities in `src/main`, not `test/`  
- [ ] Clean build: `./gradlew clean build --no-build-cache`  
- [ ] Stable namespace/package alignment  
- [ ] Upgrade to **ObjectBox 4.3.1** and Kotlin 1.9.25  

---

## Notes on KSP
- You can safely **use KSP for other processors** (e.g. Room, Moshi).  
- ObjectBox requires **KAPT exclusively**, so mix both in the same project.  
- Gradle supports having both `kapt` and `ksp` plugins active.

---

## Attempted Fix Implementation (2025-09-03)

### What Was Attempted
- **Updated ObjectBox to version 4.3.1** (from previous version)
- **Updated Kotlin to version 1.9.25** (from 1.9.24)
- **Modified settings.gradle.kts** to include proper plugin resolution for ObjectBox
- **Added KAPT configuration to VoiceDataManager module**:
  - Enabled `kotlin("kapt")` plugin
  - Added KAPT dependencies for ObjectBox processor
  - Configured KAPT arguments for incremental processing

### Current Status
- **Entity generation still not working** - `MyObjectBox` and entity `_` classes are not being generated
- Build succeeds but ObjectBox entities remain ungenerated
- Requires further investigation into:
  - Multi-module ObjectBox configuration
  - Proper entity package structure
  - KAPT vs KSP conflicts in mixed environment

### Next Steps Required
1. Verify ObjectBox plugin is applied in correct modules
2. Check entity package alignment with module namespace
3. Investigate multi-module entity generation patterns
4. Consider clean build with cache clearing
5. Review ObjectBox documentation for latest multi-module setup

## Recommendation
Stay with **ObjectBox**, resolve generation via `kapt + plugin per module + clean rebuild`.  
Switch to Room only if you want **SQL migration control** or if kapt becomes a bottleneck in future (Kotlin K2).

**Note**: The attempted fix shows ObjectBox 4.3.1 + Kotlin 1.9.25 + KAPT configuration is not sufficient alone. Entity generation issues persist and require deeper multi-module configuration analysis.

---
© 2025 — Augmentalis Inc | Intelligent Devices LLC
