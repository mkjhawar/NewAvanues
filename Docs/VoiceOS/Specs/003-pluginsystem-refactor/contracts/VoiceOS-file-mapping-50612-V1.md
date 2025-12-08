# File Mapping Contract: VOS4 → MagicCode

**Feature**: 003-pluginsystem-refactor
**Purpose**: Exact file paths for copying encrypted storage from VOS4 to MagicCode

---

## Base Directories

**VOS4 PluginSystem**:
```
/Volumes/M Drive/Coding/vos4/modules/libraries/PluginSystem/
```

**MagicCode PluginSystem**:
```
/Volumes/M Drive/Coding/magiccode/runtime/plugin-system/
```

---

## 1. Common Files (4 files)

### 1.1 PermissionStorage.kt (expect class)

**Source**:
```
vos4/modules/libraries/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorage.kt
```

**Target**:
```
magiccode/runtime/plugin-system/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorage.kt
```

**Size**: 193 lines
**Type**: expect class (API definition)

---

### 1.2 EncryptionStatus.kt

**Source**:
```
vos4/modules/libraries/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/EncryptionStatus.kt
```

**Target**:
```
magiccode/runtime/plugin-system/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/EncryptionStatus.kt
```

**Size**: ~30 lines
**Type**: data class

---

### 1.3 MigrationResult.kt

**Source**:
```
vos4/modules/libraries/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/MigrationResult.kt
```

**Target**:
```
magiccode/runtime/plugin-system/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/MigrationResult.kt
```

**Size**: ~40 lines
**Type**: sealed class (3 cases: Success, Failure, AlreadyMigrated)

---

### 1.4 Exceptions.kt

**Source**:
```
vos4/modules/libraries/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/Exceptions.kt
```

**Target**:
```
magiccode/runtime/plugin-system/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/Exceptions.kt
```

**Size**: ~20 lines
**Type**: exception classes (EncryptionException, MigrationException)

---

## 2. Android Files (3 files)

### 2.1 PermissionStorage.kt (actual implementation)

**Source**:
```
vos4/modules/libraries/PluginSystem/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorage.kt
```

**Target**:
```
magiccode/runtime/plugin-system/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorage.kt
```

**Size**: 410 lines
**Type**: actual class (full implementation with encryption + migration)

---

### 2.2 KeyManager.kt

**Source**:
```
vos4/modules/libraries/PluginSystem/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/KeyManager.kt
```

**Target**:
```
magiccode/runtime/plugin-system/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/KeyManager.kt
```

**Size**: 202 lines
**Type**: object singleton (master key management with hardware fallback)

---

### 2.3 EncryptedStorageFactory.kt

**Source**:
```
vos4/modules/libraries/PluginSystem/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/EncryptedStorageFactory.kt
```

**Target**:
```
magiccode/runtime/plugin-system/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/EncryptedStorageFactory.kt
```

**Size**: 282 lines
**Type**: object factory (EncryptedSharedPreferences wrapper)

---

## 3. Test Files (2 files)

### 3.1 PermissionStorageEncryptionTest.kt

**Source**:
```
vos4/modules/libraries/PluginSystem/src/androidInstrumentedTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorageEncryptionTest.kt
```

**Target**:
```
magiccode/runtime/plugin-system/src/androidInstrumentedTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorageEncryptionTest.kt
```

**Size**: 325 lines
**Type**: AndroidJUnit4 test (5 unit tests)
**Tests**:
- testEncryptionRoundTrip
- testMultiplePermissionsEncrypted
- testHardwareKeystoreDetection
- testCorruptedEncryptedDataDetection
- testConcurrentPermissionGrants

---

### 3.2 PermissionStoragePerformanceTest.kt

**Source**:
```
vos4/modules/libraries/PluginSystem/src/androidInstrumentedTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStoragePerformanceTest.kt
```

**Target**:
```
magiccode/runtime/plugin-system/src/androidInstrumentedTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStoragePerformanceTest.kt
```

**Size**: ~200 lines (estimated, file pending in VOS4)
**Type**: AndroidJUnit4 test (3 performance benchmarks)
**Benchmarks**:
- Bulk permission save/query latency
- Concurrent access performance
- Migration performance

**Note**: If this file doesn't exist in VOS4, skip and create placeholder in MagicCode

---

## 4. Modified Files (1 file)

### 4.1 PluginLogger.kt (ADD security() method)

**Source**:
```
vos4/modules/libraries/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/core/PluginLogger.kt
```

**Target**:
```
magiccode/runtime/plugin-system/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/core/PluginLogger.kt
```

**Modification**: Add security() method to interface and implementations

**Changes**:
```kotlin
// Add to PluginLogger interface
interface PluginLogger {
    fun security(tag: String, event: String, throwable: Throwable? = null)
}

// Add to ConsolePluginLogger
override fun security(tag: String, event: String, throwable: Throwable?) {
    println("[SECURITY] [$tag] $event")
    throwable?.printStackTrace()
}

// Add to PluginLog object
object PluginLog {
    fun security(tag: String, event: String, throwable: Throwable? = null) {
        logger.security(tag, event, throwable)
    }
}
```

---

## 5. New Stub Files (2 files)

### 5.1 iOS Stub

**Target**:
```
magiccode/runtime/plugin-system/src/iosMain/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorage.kt
```

**Size**: ~80 lines
**Type**: actual stub (all methods throw UnsupportedOperationException)

**Implementation**:
```kotlin
package com.augmentalis.magiccode.plugins.security

actual class PermissionStorage private constructor() {
    actual companion object {
        actual fun create(context: Any): PermissionStorage {
            throw UnsupportedOperationException(
                "Encrypted permission storage is only available on Android. " +
                "This feature requires EncryptedSharedPreferences which is Android-specific. " +
                "Please use Android platform for encrypted permission storage."
            )
        }
    }

    actual fun savePermission(pluginId: String, permission: String) {
        throw UnsupportedOperationException("Encrypted storage not available on iOS")
    }

    actual fun hasPermission(pluginId: String, permission: String): Boolean {
        throw UnsupportedOperationException("Encrypted storage not available on iOS")
    }

    // ... all other methods throw UnsupportedOperationException
}
```

---

### 5.2 JVM Stub

**Target**:
```
magiccode/runtime/plugin-system/src/jvmMain/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorage.kt
```

**Size**: ~80 lines
**Type**: actual stub (identical to iOS stub, message says "JVM")

---

## 6. Build Configuration Files

### 6.1 MagicCode build.gradle.kts

**Target**:
```
magiccode/runtime/plugin-system/build.gradle.kts
```

**Changes**:
```kotlin
val androidMain by getting {
    dependencies {
        implementation("androidx.security:security-crypto:1.1.0-alpha06")  // ADD THIS
    }
}

val androidInstrumentedTest by getting {  // ADD THIS SOURCE SET if not exists
    dependencies {
        implementation("androidx.test:core:1.5.0")
        implementation("androidx.test.ext:junit:1.1.5")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    }
}
```

---

### 6.2 VOS4 settings.gradle.kts (Phase 2.5)

**Target**:
```
vos4/settings.gradle.kts
```

**Changes**:
```kotlin
// ADD THIS for composite build
includeBuild("/Volumes/M Drive/Coding/magiccode") {
    dependencySubstitution {
        substitute(module("com.augmentalis.magiccode:plugin-system"))
            .using(project(":runtime:plugin-system"))
    }
}
```

---

### 6.3 VOS4 PluginSystem build.gradle.kts (Phase 2.5)

**Target**:
```
vos4/modules/libraries/PluginSystem/build.gradle.kts
```

**Changes**:
```kotlin
// REPLACE local implementation with MagicCode dependency
dependencies {
    implementation("com.augmentalis.magiccode:plugin-system")  // Resolved via composite build
}
```

---

## 7. Backup Rules

### 7.1 MagicCode backup_rules.xml

**Target**:
```
magiccode/runtime/plugin-system/src/androidMain/res/xml/backup_rules.xml
```

**Note**: May need to create res/xml/ directory first

**Content**:
```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <!-- Exclude encrypted plugin permissions from backup (FR-010) -->
    <exclude domain="sharedpref" path="plugin_permissions_encrypted.xml"/>
</full-backup-content>
```

---

## Summary Statistics

**Total Files to Copy**: 7 encryption files + 2 tests = 9 files
**Total Files to Create**: 2 stubs (iOS, JVM) = 2 files
**Total Files to Modify**: 1 file (PluginLogger.kt)
**Total Build Files to Update**: 3 files (MagicCode build.gradle.kts, VOS4 settings.gradle.kts, VOS4 PluginSystem build.gradle.kts)

**Grand Total**: 15 file operations

**Total Lines of Code**:
- Copied: ~1,700 lines
- Created stubs: ~160 lines
- Modified: ~20 lines (security() method)
- **Total**: ~1,880 lines

---

## Copy Script

```bash
#!/bin/bash
# Quick copy script for Phase 2.2

VOS4="/Volumes/M Drive/Coding/vos4/modules/libraries/PluginSystem"
MC="/Volumes/M Drive/Coding/magiccode/runtime/plugin-system"

# Create directories
mkdir -p "$MC/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security"
mkdir -p "$MC/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security"
mkdir -p "$MC/src/iosMain/kotlin/com/augmentalis/magiccode/plugins/security"
mkdir -p "$MC/src/jvmMain/kotlin/com/augmentalis/magiccode/plugins/security"
mkdir -p "$MC/src/androidInstrumentedTest/kotlin/com/augmentalis/magiccode/plugins/security"

# Copy common files
cp "$VOS4/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorage.kt" \
   "$MC/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/"

cp "$VOS4/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/EncryptionStatus.kt" \
   "$MC/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/"

cp "$VOS4/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/MigrationResult.kt" \
   "$MC/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/"

cp "$VOS4/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/Exceptions.kt" \
   "$MC/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/"

# Copy Android files
cp "$VOS4/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorage.kt" \
   "$MC/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/"

cp "$VOS4/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/KeyManager.kt" \
   "$MC/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/"

cp "$VOS4/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/EncryptedStorageFactory.kt" \
   "$MC/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/"

# Copy tests
cp "$VOS4/src/androidInstrumentedTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorageEncryptionTest.kt" \
   "$MC/src/androidInstrumentedTest/kotlin/com/augmentalis/magiccode/plugins/security/"

# Copy performance test (if exists)
if [ -f "$VOS4/src/androidInstrumentedTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStoragePerformanceTest.kt" ]; then
    cp "$VOS4/src/androidInstrumentedTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStoragePerformanceTest.kt" \
       "$MC/src/androidInstrumentedTest/kotlin/com/augmentalis/magiccode/plugins/security/"
fi

echo "Files copied successfully!"
```

---

**Next**: Create quickstart.md for developers using the synchronized PluginSystem
