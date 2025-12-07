# Data Model: Encrypted Permission Storage

**Feature**: 001-encrypted-permission-storage
**Created**: 2025-10-26
**Status**: Design Phase

## Overview

This document defines the data entities and their relationships for the encrypted permission storage feature. The feature replaces plain-text SharedPreferences storage with hardware-backed AES256-GCM encrypted storage for plugin permission grants.

**Key Design Principles:**
- Backward compatibility with existing plain-text storage format
- Transparent encryption (consumers unaware of encryption)
- Hardware-backed keys (Android Keystore TEE/TrustZone)
- Idempotent migration from plain-text to encrypted

---

## Entity 1: Permission Grant

**Purpose:** Represents a single permission granted to a plugin by the user or system.

**Storage Location:** EncryptedSharedPreferences (`plugin_permissions_encrypted`)

**Storage Format:**
- **Key:** `{pluginId}.{permission}` (e.g., `com.example.plugin.ACCESSIBILITY_SERVICES`)
- **Value:** `{status}|{timestamp}|{grantedBy}` (e.g., `GRANTED|1698765432000|user`)

### Fields

| Field | Type | Description | Validation Rules |
|-------|------|-------------|------------------|
| `pluginId` | String | Reverse-domain plugin identifier | Required, non-empty, matches manifest ID |
| `permission` | Permission enum | Type of permission requested | Required, one of Permission enum values |
| `status` | GrantStatus enum | Current grant status | Required, one of GRANTED/DENIED/PENDING/REVOKED |
| `timestamp` | Long | Unix timestamp (ms) when grant occurred | Required, positive number |
| `grantedBy` | String? | User ID or "system" | Optional, nullable |

### Permission Enum

```kotlin
enum class Permission {
    ACCESSIBILITY_SERVICES,    // AccessibilityService access
    CONTACTS,                  // Read/write contacts
    MICROPHONE,                // Audio recording
    CAMERA,                    // Camera access
    LOCATION,                  // GPS location
    STORAGE,                   // File system access
    PHONE,                     // Phone state/calls
    SMS,                       // SMS read/write
    NETWORK,                   // Network access
    BLUETOOTH,                 // Bluetooth access
    NOTIFICATIONS,             // Notification access
    SYSTEM_SETTINGS,           // Modify system settings
    OVERLAY,                   // Draw over other apps
    BACKGROUND_EXECUTION       // Run in background
}
```

### GrantStatus Enum

```kotlin
enum class GrantStatus {
    GRANTED,     // Permission granted
    DENIED,      // Permission explicitly denied
    PENDING,     // Permission requested but not yet decided
    REVOKED      // Previously granted but now revoked
}
```

### Relationships

- **One plugin** → **many permissions** (1:N)
- **One permission** → **one status** per plugin (1:1)
- **One permission grant** → **zero or one grantedBy** (1:0..1)

### Encryption Details

**Before Encryption (Plain-Text):**
```xml
<!-- /data/data/com.augmentalis.vos4/shared_prefs/plugin_permissions.xml -->
<map>
    <string name="com.example.plugin.ACCESSIBILITY_SERVICES">GRANTED|1698765432000|user</string>
    <string name="com.example.plugin.MICROPHONE">DENIED|1698765433000|user</string>
</map>
```

**After Encryption (Encrypted):**
```xml
<!-- /data/data/com.augmentalis.vos4/shared_prefs/plugin_permissions_encrypted.xml -->
<map>
    <string name="AUg7KmP3vXc8JnR...">encrypted_value_1_base64</string>
    <string name="BVh8LnQ4wYd9KoS...">encrypted_value_2_base64</string>
</map>
```

**Key Points:**
- Both keys AND values are encrypted
- Keys encrypted with AES256_SIV (deterministic, allows lookup)
- Values encrypted with AES256_GCM (authenticated encryption)
- GCM authentication tag prevents tampering

### State Transitions

```
PENDING → GRANTED    (user approves permission)
PENDING → DENIED     (user denies permission)
GRANTED → REVOKED    (user revokes previously granted permission)
DENIED → GRANTED     (user changes mind, grants permission)
REVOKED → GRANTED    (user re-grants revoked permission)
```

**Forbidden Transitions:**
- `DENIED → REVOKED` (cannot revoke what was never granted)
- `PENDING → REVOKED` (cannot revoke what was never granted)

### Validation Rules

1. **Plugin ID Format:** Must match `^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$` (reverse-domain)
   - Valid: `com.example.plugin`, `com.augmentalis.speech.engine`
   - Invalid: `MyPlugin`, `com.Example.Plugin`, `plugin`

2. **Timestamp:** Must be positive, cannot be future (allow 5-second clock skew)

3. **Granted By:** If provided, must be non-empty string
   - System grants: `"system"`
   - User grants: User ID from auth system

4. **Status Consistency:**
   - If status is `REVOKED`, there must be a previous `GRANTED` entry in logs
   - If status is `DENIED`, plugin must have requested permission

### Examples

**Example 1: Accessibility Service Permission Grant**
```
Key:   com.augmentalis.whatsapp.automation.ACCESSIBILITY_SERVICES
Value: GRANTED|1698765432123|user
```

**Example 2: Microphone Permission Denial**
```
Key:   com.example.voice.recorder.MICROPHONE
Value: DENIED|1698765433456|user
```

**Example 3: System-Granted Storage Permission**
```
Key:   com.augmentalis.core.STORAGE
Value: GRANTED|1698765434789|system
```

---

## Entity 2: Encryption Key

**Purpose:** Master encryption key used to encrypt/decrypt all permission grants.

**Storage Location:** Android Keystore (`AndroidKeyStore` provider)

**Key Alias:** `_plugin_permissions_master_key_`

### Fields

| Field | Type | Description | Value |
|-------|------|-------------|-------|
| `keyAlias` | String | Identifier in Android Keystore | `_plugin_permissions_master_key_` |
| `keyScheme` | MasterKey.KeyScheme | Encryption algorithm | `AES256_GCM` (256-bit AES in GCM mode) |
| `keySize` | Int | Key size in bits | 256 |
| `strongBoxBacked` | Boolean | Hardware security module usage | `true` if available, else `false` |
| `insideSecureHardware` | Boolean | Stored in TEE/TrustZone | `true` on API 28+ |
| `userAuthenticationRequired` | Boolean | Biometric unlock requirement | `false` (permissions need background access) |
| `keyValidityDuration` | Int | Key lifetime in seconds | Unlimited (-1) |
| `blockModes` | String[] | Encryption block modes | `["GCM"]` |
| `encryptionPaddings` | String[] | Padding schemes | `["NoPadding"]` |

### Key Properties

**Device-Bound:**
- Key cannot be exported from device
- Key cannot be backed up to cloud
- Key deleted on factory reset

**Hardware-Backed (API 28+):**
- Stored in Trusted Execution Environment (TEE)
- Or StrongBox Keymaster (hardware security module) if available
- Protected from software extraction even with root access

**Lifecycle:**
- Created on first app launch (or after key invalidation)
- Persists across app updates and device reboots
- Invalidated if user clears security credentials in Settings
- Automatically regenerated if invalidated (data loss)

### Key Generation Code

```kotlin
val masterKey = MasterKey.Builder(context, "_plugin_permissions_master_key_")
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .setUserAuthenticationRequired(false)  // Background access needed
    .setRequestStrongBoxBacked(true)       // Prefer StrongBox if available
    .build()
```

### Key Access Control

**Who Can Access:**
- ✅ VOS4 app process (com.augmentalis.vos4)
- ✅ Android Keystore system service

**Who Cannot Access:**
- ❌ ADB shell (even with root)
- ❌ Other apps (even with same signature)
- ❌ Backup services (keys excluded)
- ❌ Device manufacturer (keys in TEE/TrustZone)

**Exception:** Compromised firmware or OS-level exploit could theoretically access TEE

### Fallback Strategy

If hardware keystore unavailable (rare on API 28+):

```kotlin
try {
    val masterKey = MasterKey.Builder(context, keyAlias)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .setRequestStrongBoxBacked(true)
        .build()
} catch (e: Exception) {
    // Fallback to software keystore (less secure)
    val masterKey = MasterKey.Builder(context, keyAlias)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .setRequestStrongBoxBacked(false)  // Software keystore
        .build()
    PluginLog.w(TAG, "Hardware keystore unavailable, using software fallback")
}
```

### Relationships

- **One master key** → **encrypts all permission grants** (1:N)
- **One master key** → **one Android Keystore entry** (1:1)
- **One app** → **one master key** (1:1)

---

## Entity 3: Migration State

**Purpose:** Tracks migration status from plain-text to encrypted storage to ensure idempotency.

**Storage Location:** Encrypted SharedPreferences (`plugin_permissions_encrypted`)

**Storage Key:** `_migration_state_`

### Fields

| Field | Type | Description | Default Value |
|-------|------|-------------|---------------|
| `migrationCompleted` | Boolean | Whether migration has finished successfully | `false` |
| `migratedCount` | Int | Number of permissions successfully migrated | `0` |
| `failedCount` | Int | Number of permissions that failed to migrate | `0` |
| `migrationTimestamp` | Long | Unix timestamp (ms) when migration completed | `0` |
| `migrationVersion` | String | Version of migration logic used | `"1.0.0"` |
| `originalFileDeleted` | Boolean | Whether plain-text file was deleted | `false` |

### Storage Format

**Value:** `{completed}|{migratedCount}|{failedCount}|{timestamp}|{version}|{deleted}`

**Example:**
```
Key:   _migration_state_
Value: true|15|0|1698765435000|1.0.0|true
```

This means:
- Migration completed successfully
- 15 permissions migrated
- 0 permissions failed
- Completed on Oct 31, 2023 at 1:30:35 AM UTC
- Using migration logic version 1.0.0
- Original plain-text file deleted

### State Lifecycle

```
1. App Launch (pre-migration)
   migrationCompleted = false

2. Migration Starts
   migratedCount = 0
   failedCount = 0

3. Migration In Progress
   migratedCount increments per successful permission
   failedCount increments per failed permission

4. Migration Completes
   migrationCompleted = true
   migrationTimestamp = System.currentTimeMillis()
   originalFileDeleted = true (if deletion successful)

5. App Restart (post-migration)
   Checks migrationCompleted flag
   Skips migration if true (idempotency)
```

### Idempotency Guarantee

```kotlin
suspend fun migrateToEncrypted(): MigrationResult {
    // Check if already migrated
    val migrationState = getMigrationState()
    if (migrationState.migrationCompleted) {
        PluginLog.i(TAG, "Migration already completed, skipping")
        return MigrationResult.AlreadyMigrated(migrationState.migratedCount)
    }

    // Proceed with migration...
}
```

### Error Handling

**Partial Migration (app crash mid-migration):**
- Migration state NOT saved until completion
- On next launch, migration re-runs from scratch
- Encrypted permissions overwrite any partial progress
- Plain-text file only deleted after full migration success

**Migration Failure:**
- `migrationCompleted` remains `false`
- `failedCount` > 0
- Plain-text file preserved
- User can retry migration manually

### Relationships

- **One app instance** → **one migration state** (1:1)
- **One migration state** → **tracks one migration operation** (1:1)

---

## Entity 4: Encrypted SharedPreferences Instance

**Purpose:** The underlying storage mechanism for encrypted permission data.

**Implementation:** `androidx.security.crypto.EncryptedSharedPreferences`

### Configuration

| Property | Value | Description |
|----------|-------|-------------|
| `fileName` | `plugin_permissions_encrypted` | File name in SharedPreferences directory |
| `masterKey` | MasterKey instance | Encryption key from Android Keystore |
| `prefKeyEncryptionScheme` | `AES256_SIV` | Deterministic key encryption (allows lookup) |
| `prefValueEncryptionScheme` | `AES256_GCM` | Authenticated value encryption (prevents tampering) |

### Creation Code

```kotlin
val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "plugin_permissions_encrypted",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

### Performance Characteristics

**From Research (research.md):**
- First creation: 100-300ms (one-time cost)
- Subsequent reads: 3-10ms per operation (2-5x plain SharedPreferences)
- Writes: 5-15ms per operation
- Batch operations: Amortized cost improves performance

**Mitigation:**
- Singleton instance (create once, reuse)
- In-memory caching of frequently accessed permissions
- Background writes using Dispatchers.IO

### File Location

**Plain-Text (old):**
```
/data/data/com.augmentalis.vos4/shared_prefs/plugin_permissions.xml
```

**Encrypted (new):**
```
/data/data/com.augmentalis.vos4/shared_prefs/plugin_permissions_encrypted.xml
```

**Keystore:**
```
Android Keystore Provider (system-managed, not accessible as file)
```

### Backup Exclusion

**Critical Security Requirement:**

The encrypted SharedPreferences file CAN be backed up (it's just encrypted data), but the master key MUST NOT be backed up.

**Gradle Configuration:**
```xml
<!-- AndroidManifest.xml -->
<application
    android:allowBackup="false"
    android:fullBackupContent="@xml/backup_rules"
    ...>
</application>
```

**Backup Rules (`res/xml/backup_rules.xml`):**
```xml
<full-backup-content>
    <exclude domain="sharedpref" path="plugin_permissions_encrypted.xml"/>
</full-backup-content>
```

**Rationale:**
- If encrypted file AND key are backed up together, attacker with backup access can decrypt
- By excluding encrypted file from backup, even if backup is compromised, no permission data leaks
- Alternative: Include encrypted file in backup, but keys are never backed up (Android Keystore property)

### Relationships

- **One encrypted SharedPreferences** → **stores all permission grants** (1:N)
- **One encrypted SharedPreferences** → **uses one master key** (N:1)
- **One app** → **one encrypted SharedPreferences instance** (1:1, singleton)

---

## Data Model Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                   Android Keystore                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Master Key (_plugin_permissions_master_key_)        │  │
│  │  - AES256_GCM                                        │  │
│  │  - Hardware-backed (TEE/TrustZone/StrongBox)        │  │
│  │  - Non-exportable, device-bound                      │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ▲
                              │ encrypts/decrypts
                              │
┌─────────────────────────────────────────────────────────────┐
│      EncryptedSharedPreferences Instance (Singleton)        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  File: plugin_permissions_encrypted.xml              │  │
│  │  - PrefKeyEncryptionScheme: AES256_SIV               │  │
│  │  - PrefValueEncryptionScheme: AES256_GCM             │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ▲
                              │ stores
                              │
         ┌────────────────────┴───────────────────┐
         │                                        │
┌────────────────────┐                 ┌─────────────────────┐
│ Permission Grants  │                 │  Migration State    │
│ (Multiple Entries) │                 │  (Single Entry)     │
├────────────────────┤                 ├─────────────────────┤
│ Key: {plugin}.{p}  │                 │ Key: _migration_... │
│ Value: {s}|{t}|{g} │                 │ Value: {c}|{m}|...  │
│                    │                 │                     │
│ Example:           │                 │ Example:            │
│ com.ex.ACCES...    │                 │ true|15|0|1698...   │
│ → GRANTED|169...   │                 │                     │
└────────────────────┘                 └─────────────────────┘
         │
         │ references
         ▼
┌────────────────────────────────────────┐
│  Permission Enum                       │
│  - ACCESSIBILITY_SERVICES              │
│  - CONTACTS, MICROPHONE, CAMERA, ...   │
└────────────────────────────────────────┘
         │
         │ has status
         ▼
┌────────────────────────────────────────┐
│  GrantStatus Enum                      │
│  - GRANTED, DENIED, PENDING, REVOKED   │
└────────────────────────────────────────┘
```

---

## Data Access Patterns

### Pattern 1: Grant Permission

```kotlin
// Transparent encryption - caller unaware
fun grantPermission(pluginId: String, permission: Permission) {
    permissionStorage.savePermission(
        pluginId = "com.example.plugin",
        permission = Permission.ACCESSIBILITY_SERVICES,
        status = GrantStatus.GRANTED
    )
    // Internally encrypts and stores in EncryptedSharedPreferences
}
```

**Data Flow:**
1. Caller invokes savePermission()
2. PermissionStorage formats value: `GRANTED|{timestamp}|user`
3. EncryptedSharedPreferences encrypts key: `com.example.plugin.ACCESSIBILITY_SERVICES` → `AUg7KmP3...`
4. EncryptedSharedPreferences encrypts value: `GRANTED|...` → `encrypted_base64`
5. Writes to XML file on disk
6. Returns to caller (synchronous operation)

### Pattern 2: Query Permission

```kotlin
fun checkPermission(pluginId: String, permission: Permission): Boolean {
    val status = permissionStorage.getPermission(
        pluginId = "com.example.plugin",
        permission = Permission.ACCESSIBILITY_SERVICES
    )
    return status == GrantStatus.GRANTED
}
```

**Data Flow:**
1. Caller invokes getPermission()
2. PermissionStorage encrypts lookup key: `com.example.plugin.ACCESSIBILITY_SERVICES` → `AUg7KmP3...`
3. EncryptedSharedPreferences reads encrypted value from XML
4. Decrypts value: `encrypted_base64` → `GRANTED|1698765432000|user`
5. PermissionStorage parses status: `GRANTED`
6. Returns to caller

**Performance:** 3-10ms (includes decryption + disk read)

### Pattern 3: Migration (One-Time)

```kotlin
suspend fun migrate() {
    val migrationResult = permissionStorage.migrateToEncrypted()
    when (migrationResult) {
        is MigrationResult.Success -> {
            PluginLog.i(TAG, "Migrated ${migrationResult.migratedCount} permissions")
        }
        is MigrationResult.Failure -> {
            PluginLog.e(TAG, "Migration failed: ${migrationResult.reason}")
        }
    }
}
```

**Data Flow:**
1. Check migration state (if already migrated, skip)
2. Open plain-text SharedPreferences (`plugin_permissions.xml`)
3. For each entry:
   - Read plain-text key/value
   - Parse into (pluginId, permission, status, timestamp)
   - Write to encrypted SharedPreferences (auto-encrypts)
4. Save migration state to encrypted SharedPreferences
5. Delete plain-text SharedPreferences file
6. Return MigrationResult

**Performance:** 50-100ms for 10 permissions (one-time cost)

### Pattern 4: Bulk Query

```kotlin
fun getAllPluginPermissions(pluginId: String): Map<Permission, GrantStatus> {
    return permissionStorage.getAllPermissions("com.example.plugin")
}
```

**Data Flow:**
1. EncryptedSharedPreferences.getAll() retrieves all encrypted entries
2. Filter entries matching `com.example.plugin.*` prefix
3. Decrypt each matching entry
4. Parse into Map<Permission, GrantStatus>
5. Return to caller

**Optimization:** Cache result in memory for repeated queries

---

## Security Properties

### Threat Model

**Protected Against:**
- ✅ ADB access without device unlock
- ✅ Rooted devices (unless bootloader unlocked and custom recovery installed)
- ✅ Malicious apps reading SharedPreferences files
- ✅ Physical theft (data encrypted at rest)
- ✅ Cloud backup extraction (keys not backed up)
- ✅ Data tampering (GCM authentication tag detects modifications)

**Not Protected Against:**
- ❌ Compromised OS/firmware (attacker controls TEE)
- ❌ Physical attacks on hardware (chip decapping, side-channel)
- ❌ Unlocked device in attacker's hands (keys accessible when device unlocked)
- ❌ App process memory dump while running (decrypted data in RAM)

### Encryption Strength

**Algorithm:** AES-256-GCM
- 256-bit key length (2^256 possible keys)
- Galois/Counter Mode (authenticated encryption)
- NIST-approved, industry standard
- Hardware-accelerated on Android 9+

**Key Security:**
- Hardware-bound (cannot export from device)
- Requires device unlock for key generation (one-time)
- Protected by TEE/TrustZone or StrongBox
- Automatically regenerated if invalidated (data loss)

---

## Conclusion

This data model provides:
- **3 core entities:** Permission Grant, Encryption Key, Migration State
- **1 infrastructure component:** EncryptedSharedPreferences instance
- **Backward compatibility:** Automatic migration from plain-text
- **Performance:** <5ms overhead per operation (with caching)
- **Security:** Hardware-backed encryption, tamper detection, backup exclusion

**Next Steps:**
1. Review data model for completeness
2. Proceed to API contracts (contracts/PermissionStorage.kt)
3. Create quickstart guide (quickstart.md)
4. Generate tasks (tasks.md)
