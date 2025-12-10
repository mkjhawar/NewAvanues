# Quick Start: PluginSystem Synchronization

**Feature**: 003-pluginsystem-refactor
**Audience**: Developers implementing the synchronization
**Time**: 4 hours total

---

## Overview

This guide walks through synchronizing VOS4's encrypted permission storage to MagicCode PluginSystem, establishing MagicCode as the canonical library.

**Goals**:
1. Copy 7 encryption files + 2 tests from VOS4 → MagicCode
2. Verify repository equality (core APIs match)
3. Update VOS4 to depend on MagicCode library
4. Synchronize documentation

---

## Prerequisites

**Before starting**:
- [ ] VOS4 on branch `003-pluginsystem-refactor`
- [ ] MagicCode repository available at `/Volumes/M Drive/Coding/magiccode`
- [ ] Both projects build successfully (baseline verification)
- [ ] Android SDK with API 29+ installed
- [ ] JUnit 4 test dependencies (no JUnit 5)

**Verify baselines**:
```bash
# VOS4 tests
cd "/Volumes/M Drive/Coding/vos4"
./gradlew :modules:libraries:PluginSystem:test

# MagicCode tests (should have 282 tests passing)
cd "/Volumes/M Drive/Coding/magiccode"
./gradlew :runtime:plugin-system:test
```

---

## Phase 2.1: Pre-Merge Verification (30 min)

### Step 1: Run VOS4 Encryption Tests

```bash
cd "/Volumes/M Drive/Coding/vos4"
./gradlew :modules:libraries:PluginSystem:connectedAndroidTest
```

**Expected**: 8 tests pass (5 unit + 3 performance)

**If failed**: Fix tests before proceeding

---

### Step 2: Run MagicCode Baseline Tests

```bash
cd "/Volumes/M Drive/Coding/magiccode"
./gradlew :runtime:plugin-system:test
```

**Expected**: 282 tests pass

**If failed**: Fix MagicCode tests before proceeding

---

### Step 3: Quick Diff Check (Optional)

```bash
# Compare PluginManager.kt
diff "/Volumes/M Drive/Coding/vos4/modules/libraries/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/core/PluginManager.kt" \
     "/Volumes/M Drive/Coding/magiccode/runtime/plugin-system/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/core/PluginManager.kt"
```

**Expected**: Minimal differences (cosmetic only)

**If major differences**: Review and document in research.md

---

## Phase 2.2: Copy Encryption Stack to MagicCode (1.5 hours)

### Step 1: Create Directories

```bash
cd "/Volumes/M Drive/Coding/magiccode/runtime/plugin-system"

mkdir -p src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security
mkdir -p src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security
mkdir -p src/iosMain/kotlin/com/augmentalis/magiccode/plugins/security
mkdir -p src/jvmMain/kotlin/com/augmentalis/magiccode/plugins/security
mkdir -p src/androidInstrumentedTest/kotlin/com/augmentalis/magiccode/plugins/security
```

---

### Step 2: Copy Files (Use Script)

```bash
# Save this as copy-encryption-files.sh
VOS4="/Volumes/M Drive/Coding/vos4/modules/libraries/PluginSystem"
MC="/Volumes/M Drive/Coding/magiccode/runtime/plugin-system"

# Common files (4 files)
cp "$VOS4/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorage.kt" \
   "$MC/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/"

cp "$VOS4/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/EncryptionStatus.kt" \
   "$MC/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/"

cp "$VOS4/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/MigrationResult.kt" \
   "$MC/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/"

cp "$VOS4/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/Exceptions.kt" \
   "$MC/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/"

# Android files (3 files)
cp "$VOS4/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorage.kt" \
   "$MC/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/"

cp "$VOS4/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/KeyManager.kt" \
   "$MC/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/"

cp "$VOS4/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/EncryptedStorageFactory.kt" \
   "$MC/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/"

echo "7 encryption files copied!"
```

**Run**:
```bash
chmod +x copy-encryption-files.sh
./copy-encryption-files.sh
```

---

### Step 3: Update PluginLogger.kt

**File**: `src/commonMain/kotlin/com/augmentalis/magiccode/plugins/core/PluginLogger.kt`

**Add to interface**:
```kotlin
interface PluginLogger {
    fun security(tag: String, event: String, throwable: Throwable? = null)
}
```

**Add to ConsolePluginLogger**:
```kotlin
override fun security(tag: String, event: String, throwable: Throwable?) {
    println("[SECURITY] [$tag] $event")
    throwable?.printStackTrace()
}
```

**Add to PluginLog object**:
```kotlin
object PluginLog {
    fun security(tag: String, event: String, throwable: Throwable? = null) {
        logger.security(tag, event, throwable)
    }
}
```

---

### Step 4: Create iOS/JVM Stubs

**iOS Stub** (`src/iosMain/kotlin/.../security/PermissionStorage.kt`):
```kotlin
package com.augmentalis.magiccode.plugins.security

actual class PermissionStorage private constructor() {
    actual companion object {
        actual fun create(context: Any): PermissionStorage {
            throw UnsupportedOperationException(
                "Encrypted permission storage is only available on Android"
            )
        }
    }

    actual fun savePermission(pluginId: String, permission: String) {
        throw UnsupportedOperationException("Not available on iOS")
    }

    actual fun hasPermission(pluginId: String, permission: String): Boolean {
        throw UnsupportedOperationException("Not available on iOS")
    }

    actual fun getAllPermissions(pluginId: String): Set<String> {
        throw UnsupportedOperationException("Not available on iOS")
    }

    actual fun revokePermission(pluginId: String, permission: String) {
        throw UnsupportedOperationException("Not available on iOS")
    }

    actual fun clearAllPermissions(pluginId: String) {
        throw UnsupportedOperationException("Not available on iOS")
    }

    actual fun isEncrypted(): Boolean {
        throw UnsupportedOperationException("Not available on iOS")
    }

    actual fun getEncryptionStatus(): EncryptionStatus {
        throw UnsupportedOperationException("Not available on iOS")
    }

    actual suspend fun migrateToEncrypted(): MigrationResult {
        throw UnsupportedOperationException("Not available on iOS")
    }
}
```

**JVM Stub**: Copy iOS stub, change "iOS" → "JVM" in error messages

---

### Step 5: Update build.gradle.kts

**Add to androidMain**:
```kotlin
val androidMain by getting {
    dependencies {
        implementation("androidx.security:security-crypto:1.1.0-alpha06")
    }
}
```

**Add androidInstrumentedTest source set** (if not exists):
```kotlin
val androidInstrumentedTest by getting {
    dependencies {
        implementation("androidx.test:core:1.5.0")
        implementation("androidx.test.ext:junit:1.1.5")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    }
}
```

---

### Step 6: Verify Compilation

```bash
cd "/Volumes/M Drive/Coding/magiccode"
./gradlew :runtime:plugin-system:compileKotlinAndroid
```

**Expected**: 0 errors

**If errors**: Check package names, file paths, imports

---

## Phase 2.3: Copy Tests to MagicCode (45 min)

### Step 1: Copy Test Files

```bash
VOS4="/Volumes/M Drive/Coding/vos4/modules/libraries/PluginSystem"
MC="/Volumes/M Drive/Coding/magiccode/runtime/plugin-system"

cp "$VOS4/src/androidInstrumentedTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorageEncryptionTest.kt" \
   "$MC/src/androidInstrumentedTest/kotlin/com/augmentalis/magiccode/plugins/security/"

# Copy performance test (if exists)
if [ -f "$VOS4/src/androidInstrumentedTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStoragePerformanceTest.kt" ]; then
    cp "$VOS4/src/androidInstrumentedTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStoragePerformanceTest.kt" \
       "$MC/src/androidInstrumentedTest/kotlin/com/augmentalis/magiccode/plugins/security/"
fi
```

---

### Step 2: Run All Tests

```bash
cd "/Volumes/M Drive/Coding/magiccode"

# Run existing tests (282)
./gradlew :runtime:plugin-system:test

# Run new encryption tests (8)
./gradlew :runtime:plugin-system:connectedAndroidTest
```

**Expected**:
- Existing: 282/282 passing
- New: 8/8 passing
- **Total**: 290 tests

**If failures**: Review test errors, fix issues

---

## Phase 2.4: Verify Repository Equality (30 min)

### Quick Manual Verification

```bash
# Compare PluginManager API
diff "/Volumes/M Drive/Coding/vos4/modules/libraries/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/core/PluginManager.kt" \
     "/Volumes/M Drive/Coding/magiccode/runtime/plugin-system/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/core/PluginManager.kt"

# Compare PluginLoader API
diff "/Volumes/M Drive/Coding/vos4/modules/libraries/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/core/PluginLoader.kt" \
     "/Volumes/M Drive/Coding/magiccode/runtime/plugin-system/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/core/PluginLoader.kt"
```

**Expected**: Minimal cosmetic differences only

**Document findings** in `contracts/equality-report.md`

---

## Phase 2.5: Update VOS4 Dependencies (1 hour)

### Step 1: Configure Composite Build

**File**: `/Volumes/M Drive/Coding/vos4/settings.gradle.kts`

**Add**:
```kotlin
includeBuild("/Volumes/M Drive/Coding/magiccode") {
    dependencySubstitution {
        substitute(module("com.augmentalis.magiccode:plugin-system"))
            .using(project(":runtime:plugin-system"))
    }
}
```

---

### Step 2: Update VOS4 PluginSystem Dependencies

**File**: `/Volumes/M Drive/Coding/vos4/modules/libraries/PluginSystem/build.gradle.kts`

**Replace local implementation with**:
```kotlin
dependencies {
    implementation("com.augmentalis.magiccode:plugin-system")
}
```

---

### Step 3: Archive VOS4's Local PluginSystem (Optional)

```bash
cd "/Volumes/M Drive/Coding/vos4/modules/libraries"
mkdir -p _archived
mv PluginSystem _archived/PluginSystem-$(date +%Y%m%d)
```

**OR**: Keep PluginSystem module but remove src/ directory

---

### Step 4: Build VOS4 with MagicCode Dependency

```bash
cd "/Volumes/M Drive/Coding/vos4"
./gradlew :app:assembleDebug
```

**Expected**: Successful build

**If errors**: Check composite build configuration, dependency paths

---

### Step 5: Run VOS4 Tests

```bash
./gradlew :app:test
```

**Expected**: All tests pass (including 8 encryption tests)

---

## Phase 2.6: Documentation (1 hour)

**Skip in YOLO mode** - Document after implementation works

Files to update later:
- MagicCode PLUGIN_DEVELOPER_GUIDE.md
- MagicCode ARCHITECTURE.md
- MagicCode TESTING_GUIDE.md
- MagicCode README.md

---

## Phase 2.7: Final Verification & Commit (30 min)

### Step 1: Run Full Test Suites

```bash
# MagicCode
cd "/Volumes/M Drive/Coding/magiccode"
./gradlew check

# VOS4
cd "/Volumes/M Drive/Coding/vos4"
./gradlew check
```

**Expected**: All tests passing in both repos

---

### Step 2: Commit MagicCode Changes

```bash
cd "/Volumes/M Drive/Coding/magiccode"

# Stage docs first
git add docs/

# Stage code
git add runtime/plugin-system/src/

# Stage build config
git add runtime/plugin-system/build.gradle.kts

# Commit
git commit -m "feat(PluginSystem): Add hardware-backed encrypted permission storage

Added encrypted permission storage using AndroidX Security library:
- KeyManager with StrongBox/TEE/Software fallback
- EncryptedStorageFactory for EncryptedSharedPreferences wrapper
- PermissionStorage with migration from plain-text
- 8 comprehensive tests (5 unit + 3 performance)
- iOS/JVM stubs with helpful error messages

Security features:
- AES256-GCM authenticated encryption
- Hardware-backed keystore (when available)
- GCM tamper detection
- Backup exclusion rules
- Security audit logging

Tests: 290 total (282 existing + 8 new)
Coverage: 80%+

Synchronized from VOS4 PluginSystem implementation.
"
```

---

### Step 3: Commit VOS4 Changes

```bash
cd "/Volumes/M Drive/Coding/vos4"

# Stage build config
git add settings.gradle.kts
git add modules/libraries/PluginSystem/build.gradle.kts

# Commit
git commit -m "refactor(PluginSystem): Migrate to MagicCode library dependency

Replaced local PluginSystem module with MagicCode library:
- Configured Gradle composite build
- Updated dependencies to use com.augmentalis.magiccode:plugin-system
- Archived local PluginSystem module (now in _archived/)

Benefits:
- Single source of truth (MagicCode is canonical)
- Automatic updates from MagicCode improvements
- Reduced duplication (105 local files → library dependency)
- Proper library-consumer architecture

All tests passing with MagicCode dependency (8 encryption tests + existing).
"
```

---

## Troubleshooting

### Issue: Compilation errors in MagicCode

**Solution**: Check package names match exactly:
```
com.augmentalis.magiccode.plugins.security
```

---

### Issue: Tests fail after copy

**Solution**:
1. Check JUnit 4 dependencies present
2. Verify AndroidManifest.xml includes test runner
3. Check test file paths match package structure

---

### Issue: Composite build not resolving

**Solution**: Try local Maven publish:
```bash
cd "/Volumes/M Drive/Coding/magiccode"
./gradlew :runtime:plugin-system:publishToMavenLocal

# In VOS4 build.gradle.kts
repositories {
    mavenLocal()
}
dependencies {
    implementation("com.augmentalis.magiccode:plugin-system:1.0.0")
}
```

---

## Success Checklist

- [ ] All 290 MagicCode tests pass (282 + 8)
- [ ] All VOS4 tests pass with MagicCode dependency
- [ ] VOS4 app builds and launches
- [ ] PluginSystem functionality works in VOS4
- [ ] Commits created in both repositories
- [ ] Branch `003-pluginsystem-refactor` updated

---

## Next Steps

After synchronization complete:
1. Run `/idea.analyze` to verify constitution compliance
2. Create pull requests in both repositories
3. Update living documentation (notes.md, progress.md)
4. Mark feature as complete in backlog.md

---

**Estimated Time**: 4 hours total
**Actual Time**: _______ (fill in after completion)
