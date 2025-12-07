# Fixing Stub `MyObjectBox` Generation in ObjectBox

When `MyObjectBox` appears only as a **stub** and the `_` entity classes are not generated, the ObjectBox annotation processor has failed.  
This guide provides a **10-step process** to diagnose and fix the issue, while still allowing **KSP for other libraries**.

---

## 1. Ensure KAPT + Plugin in Every Entity Module
Each module with `@Entity` must apply KAPT and the ObjectBox plugin:

```kotlin
plugins {
  id("com.android.library") // or application
  kotlin("android")
  kotlin("kapt")
  id("io.objectbox")
}

dependencies {
  val obx = "4.3.1"
  implementation("io.objectbox:objectbox-kotlin:$obx")
  kapt("io.objectbox:objectbox-processor:$obx")
}

kapt {
  correctErrorTypes = true
  useBuildCache = true
  arguments { arg("objectbox.incremental", "true") }
}
```

You can still use `id("com.google.devtools.ksp")` for other processors — just not for ObjectBox.

---

## 2. Run KAPT Directly & Inspect Logs
```
./gradlew :core:kaptDebugKotlin --info --stacktrace
```
Look for the **first error from `objectbox-processor`**. That’s the real blocker.

---

## 3. Hard Clean & IDE Reset
```
./gradlew clean --no-build-cache
rm -rf ~/.gradle/caches/build-cache-*
File > Invalidate Caches / Restart (Android Studio)
```

---

## 4. Stabilize `namespace` & Package
Ensure `android.namespace` matches your entity package. Mismatches prevent generated imports.

---

## 5. Check Entity Definitions
Avoid patterns that block processing:
- Missing `@Id var id: Long = 0`
- Unsupported field types or unresolved imports
- Entities placed in `src/test` instead of `src/main`
- Using `object`, `sealed`, or `inner` classes as `@Entity`
- Collections/Maps without `@Convert`

---

## 6. Purge Stale Generated Sources
```
rm -rf core/build/generated
./gradlew :core:assembleDebug --info
```

---

## 7. Refresh ObjectBox Model (if corrupted)
If kapt logs mention **model UID conflicts**, delete:
```
rm -rf core/objectbox-models
```
Rebuild to regenerate.

---

## 8. Disable Incremental Kotlin (for diagnosis)
```
echo "kotlin.incremental=false" >> gradle.properties
./gradlew :core:kaptDebugKotlin --info
```
Re-enable afterward.

---

## 9. Isolate Conflicting Processors
Temporarily disable other annotation processors (Room, Dagger, etc.) in the same module. Re-enable once ObjectBox generates correctly.

---

## 10. Add a Smoke Test (CI Guard)
```kotlin
// src/test/kotlin/ObjectBoxGenerationSmokeTest.kt
import org.junit.Test

class ObjectBoxGenerationSmokeTest {
  @Test fun generatedClassesExist() {
    Class.forName("io.objectbox.MyObjectBox")
  }
}
```

---

# If Still Stub
- Focus on the **first kapt error message**.  
- Likely causes: unsupported field type, missing converter, visibility issue, or package mismatch.

---

## Recommended Hardening
- Upgrade to **Kotlin 1.9.25** and ObjectBox **4.3.x**.  
- Add a simple “HelloWorld” entity with 3 fields to confirm generation works at baseline.

---

## TL;DR
- Use **KAPT for ObjectBox**, **KSP for others**.  
- Apply ObjectBox plugin + kapt in **every entity module**.  
- Run `kaptDebugKotlin --info` and fix the **first processor error**.  
- Clean caches, stabilize namespaces, and test with a minimal entity.  

---
© 2025 — Augmentalis Inc | Intelligent Devices LLC
