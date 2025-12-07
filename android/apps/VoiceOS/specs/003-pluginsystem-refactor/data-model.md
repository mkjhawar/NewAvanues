# Data Model: PluginSystem Repository Synchronization

**Feature**: 003-pluginsystem-refactor
**Date**: 2025-10-26

---

## Overview

This synchronization task **does not introduce new data models**. All entities already exist in VOS4's encrypted permission storage implementation and will be copied to MagicCode as-is.

---

## Existing Entities (Being Copied)

### 1. EncryptionStatus

**Purpose**: Diagnostic information about encryption state

**Definition**:
```kotlin
data class EncryptionStatus(
    val isEncrypted: Boolean,
    val isHardwareBacked: Boolean,
    val encryptionScheme: String,
    val keyAlias: String,
    val migrationCompleted: Boolean,
    val migratedPermissionCount: Int
)
```

**Fields**:
- `isEncrypted`: Always true for Android implementation (EncryptedSharedPreferences)
- `isHardwareBacked`: True if StrongBox/TEE available, false if software keystore
- `encryptionScheme`: "AES256-SIV/AES256-GCM" (key/value encryption schemes)
- `keyAlias`: Master key alias (should be "_plugin_permissions_master_key_")
- `migrationCompleted`: True if plain-text → encrypted migration completed
- `migratedPermissionCount`: Number of permissions migrated from plain-text

**Validation Rules**:
- `keyAlias` must match `KeyManager.MASTER_KEY_ALIAS`
- `encryptionScheme` must be "AES256-SIV/AES256-GCM"
- `migratedPermissionCount` >= 0

**Usage**:
```kotlin
val status = permissionStorage.getEncryptionStatus()
println("Hardware-backed: ${status.isHardwareBacked}")
println("Migration complete: ${status.migrationCompleted}")
```

---

### 2. MigrationResult

**Purpose**: Result of plain-text → encrypted migration

**Definition**:
```kotlin
sealed class MigrationResult {
    data class Success(
        val migratedCount: Int,
        val timestamp: Long
    ) : MigrationResult()

    data class Failure(
        val reason: String,
        val failedCount: Int,
        val exception: Throwable? = null
    ) : MigrationResult()

    data class AlreadyMigrated(
        val count: Int,
        val timestamp: Long
    ) : MigrationResult()
}
```

**Success Case**:
- `migratedCount`: Number of permissions successfully migrated
- `timestamp`: Migration completion timestamp (System.currentTimeMillis())

**Failure Case**:
- `reason`: Human-readable error message
- `failedCount`: Number of permissions that failed to migrate (-1 if unknown)
- `exception`: Optional underlying exception

**AlreadyMigrated Case**:
- `count`: Number of permissions previously migrated
- `timestamp`: Original migration timestamp

**Validation Rules**:
- `Success.migratedCount` >= 0
- `Failure.failedCount` >= -1 (where -1 = unknown)
- `AlreadyMigrated.count` >= 0

**Usage**:
```kotlin
when (val result = permissionStorage.migrateToEncrypted()) {
    is MigrationResult.Success -> println("Migrated ${result.migratedCount} permissions")
    is MigrationResult.Failure -> println("Migration failed: ${result.reason}")
    is MigrationResult.AlreadyMigrated -> println("Already migrated ${result.count} permissions")
}
```

---

### 3. EncryptionException

**Purpose**: Wrapper for encryption-related errors

**Definition**:
```kotlin
class EncryptionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
```

**Use Cases**:
- Master key creation failure
- EncryptedSharedPreferences initialization failure
- GCM authentication tag verification failure (tampered data)
- KeyStore unavailable

**Usage**:
```kotlin
try {
    permissionStorage.savePermission(pluginId, permission)
} catch (e: EncryptionException) {
    Log.e(TAG, "Encryption failed: ${e.message}", e)
    // Handle encryption failure (e.g., fallback to plain-text in development)
}
```

---

### 4. MigrationException

**Purpose**: Wrapper for migration-specific errors

**Definition**:
```kotlin
class MigrationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
```

**Use Cases**:
- Plain-text file corrupted
- Encrypted storage initialization failure during migration
- Partial migration failures
- Rollback failures

**Usage**:
```kotlin
try {
    val result = permissionStorage.migrateToEncrypted()
} catch (e: MigrationException) {
    Log.e(TAG, "Migration failed: ${e.message}", e)
    // Handle migration failure (e.g., retry, manual intervention)
}
```

---

## State Transitions

### Permission Storage Lifecycle

```
┌─────────────────┐
│  Fresh Install  │
└────────┬────────┘
         │
         ▼
┌─────────────────────────┐
│ Encrypted Storage Empty │
│ (migration_completed=F) │
└────────┬────────────────┘
         │
         │ savePermission()
         ▼
┌──────────────────────────┐
│ Encrypted Storage Active │
│ (migration_completed=T)  │
└──────────────────────────┘

┌──────────────────────┐
│  Upgrade from Plain  │
│  (legacy install)    │
└────────┬─────────────┘
         │
         │ migrateToEncrypted()
         ▼
┌──────────────────────────┐
│  Migration In Progress   │
│  (mutex locked)          │
└────────┬─────────────────┘
         │
         ▼
┌──────────────────────────┐
│ Encrypted Storage Active │
│ (plain-text deleted)     │
└──────────────────────────┘
```

### Migration State Machine

```
START
  │
  ├─ Already migrated? → AlreadyMigrated(count, timestamp)
  │
  ├─ No plain-text file? → Success(0, timestamp)
  │
  └─ Plain-text exists
       │
       ├─ Read all permissions
       │
       ├─ Migrate each permission
       │  ├─ Success? → Continue
       │  └─ Failure? → Log error, increment failedCount
       │
       ├─ All succeeded? → Success(count, timestamp)
       │                   + Delete plain-text file
       │
       └─ Some failed? → Failure(reason, failedCount)
                         + Keep plain-text file (rollback)
```

---

## Relationships

**No relationships** - These are independent value objects and sealed classes:

- `EncryptionStatus`: Diagnostic snapshot (no persistence)
- `MigrationResult`: One-time migration result (no persistence)
- `EncryptionException`: Error wrapper (no persistence)
- `MigrationException`: Error wrapper (no persistence)

**Actual permissions** are stored in EncryptedSharedPreferences as:
```
Key: "{pluginId}:permissions"
Value: "CAMERA,RECORD_AUDIO,ACCESS_FINE_LOCATION" (comma-separated)
```

---

## Platform Differences

### Android (Actual Implementation)

All entities fully implemented with:
- EncryptedSharedPreferences backing
- Hardware keystore integration
- GCM authentication
- Migration logic

### iOS/JVM (Stub Implementation)

All entities defined but methods throw `UnsupportedOperationException`:
```kotlin
actual class PermissionStorage {
    actual fun getEncryptionStatus(): EncryptionStatus {
        throw UnsupportedOperationException(
            "Encrypted permission storage not available on iOS/JVM"
        )
    }
}
```

**Rationale**: EncryptedSharedPreferences is Android-specific API

---

## Summary

**Total Entities**: 4 (all existing, no new models)

1. `EncryptionStatus` - Diagnostic data class
2. `MigrationResult` - Sealed class with 3 cases
3. `EncryptionException` - Exception wrapper
4. `MigrationException` - Exception wrapper

**Validation**: All entities already tested in VOS4 (8 tests, Phase 1-3 evaluation complete)

**Copy Strategy**: Direct copy from VOS4 to MagicCode with no modifications

---

**Next**: Create contracts/file-mapping.md for exact file paths
