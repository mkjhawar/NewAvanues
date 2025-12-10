# Quickstart Guide: Encrypted Permission Storage

**Feature**: 001-encrypted-permission-storage
**Created**: 2025-10-26
**Target Audience**: VOS4 developers, plugin developers

## Table of Contents

1. [Overview](#overview)
2. [Setup](#setup)
3. [Basic Usage](#basic-usage)
4. [Migration](#migration)
5. [Advanced Usage](#advanced-usage)
6. [Troubleshooting](#troubleshooting)
7. [Security Best Practices](#security-best-practices)

---

## Overview

This guide shows you how to use encrypted permission storage in VOS4's PluginSystem. All permissions are automatically encrypted using hardware-backed AES256-GCM encryption.

**Key Benefits:**
- 🔒 Hardware-backed encryption (TEE/TrustZone)
- 🚀 <5ms latency overhead
- 🔄 Automatic migration from plain-text
- 🛡️ Tamper detection (GCM authentication)
- 📦 100% API backward compatible

**What's Encrypted:**
- Plugin permission grants (GRANTED/DENIED/PENDING/REVOKED)
- Permission timestamps
- Granted-by information

**What's NOT Encrypted:**
- Plugin metadata (stored separately in Room database)
- Plugin manifest files (public information)
- Encryption keys (stored in Android Keystore, not SharedPreferences)

---

## Setup

### 1. Add Dependency

Edit `modules/libraries/PluginSystem/build.gradle.kts`:

```kotlin
dependencies {
    // AndroidX Security for encrypted storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Existing dependencies...
    implementation("androidx.hilt:hilt-common:2.51.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
```

### 2. Exclude from Backup (CRITICAL for security)

Edit `app/src/main/res/xml/backup_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <!-- Exclude encrypted permissions from backup (keys cannot be backed up) -->
    <exclude domain="sharedpref" path="plugin_permissions_encrypted.xml"/>
</full-backup-content>
```

Edit `app/src/main/AndroidManifest.xml`:

```xml
<application
    android:allowBackup="true"
    android:fullBackupContent="@xml/backup_rules"
    ...>
</application>
```

### 3. Initialize PermissionStorage

In your Application class or Hilt module:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object PluginModule {
    @Provides
    @Singleton
    fun providePermissionStorage(
        @ApplicationContext context: Context
    ): PermissionStorage {
        return PermissionStorage(context)
    }
}
```

---

## Basic Usage

### Granting a Permission

```kotlin
@Inject
lateinit var permissionStorage: PermissionStorage

fun grantAccessibilityPermission(pluginId: String) {
    // Automatically encrypted before storage
    permissionStorage.savePermission(
        pluginId = "com.augmentalis.whatsapp.automation",
        permission = Permission.ACCESSIBILITY_SERVICES,
        status = GrantStatus.GRANTED,
        grantedBy = "user"  // Optional, defaults to "user"
    )

    Log.i(TAG, "Granted ACCESSIBILITY_SERVICES to $pluginId")
}
```

**Performance:** 5-15ms (includes encryption + disk write)

### Checking a Permission

```kotlin
fun hasPermission(pluginId: String, permission: Permission): Boolean {
    // Automatically decrypted after retrieval
    val status = permissionStorage.getPermission(
        pluginId = pluginId,
        permission = permission
    )

    return status == GrantStatus.GRANTED
}

// Example usage
if (hasPermission("com.example.plugin", Permission.MICROPHONE)) {
    // Plugin can access microphone
    startVoiceRecording()
} else {
    // Show permission request dialog
    showPermissionRequestDialog()
}
```

**Performance:** 3-10ms (includes disk read + decryption)

### Getting All Permissions for a Plugin

```kotlin
fun getPluginPermissions(pluginId: String): Map<Permission, GrantStatus> {
    return permissionStorage.getAllPermissions(pluginId)
}

// Example usage
val permissions = getPluginPermissions("com.example.plugin")

permissions.forEach { (permission, status) ->
    when (status) {
        GrantStatus.GRANTED -> Log.d(TAG, "$permission: ✅ Granted")
        GrantStatus.DENIED -> Log.d(TAG, "$permission: ❌ Denied")
        GrantStatus.PENDING -> Log.d(TAG, "$permission: ⏳ Pending")
        GrantStatus.REVOKED -> Log.d(TAG, "$permission: 🚫 Revoked")
    }
}
```

**Performance:** 10-50ms for 10 permissions

### Revoking a Permission

```kotlin
fun revokePermission(pluginId: String, permission: Permission) {
    permissionStorage.revokePermission(
        pluginId = pluginId,
        permission = permission
    )

    Log.i(TAG, "Revoked $permission from $pluginId")

    // Notify plugin that permission was revoked
    pluginManager.notifyPermissionRevoked(pluginId, permission)
}
```

### Clearing All Permissions (Uninstall)

```kotlin
fun uninstallPlugin(pluginId: String) {
    // Clear all permissions when uninstalling plugin
    permissionStorage.clearAllPermissions(pluginId)

    // Remove plugin from registry
    pluginRegistry.unregisterPlugin(pluginId)

    Log.i(TAG, "Cleared all permissions for $pluginId")
}
```

---

## Migration

### Automatic Migration on App Launch

Migration from plain-text to encrypted storage happens automatically on first app launch after upgrading. No user intervention required.

```kotlin
class VoiceOSApplication : Application() {
    @Inject
    lateinit var permissionStorage: PermissionStorage

    override fun onCreate() {
        super.onCreate()

        // Trigger automatic migration
        lifecycleScope.launch {
            migratePermissionsIfNeeded()
        }
    }

    private suspend fun migratePermissionsIfNeeded() {
        when (val result = permissionStorage.migrateToEncrypted()) {
            is MigrationResult.Success -> {
                Log.i(TAG, "✅ Migration successful: ${result.migratedCount} permissions migrated")
            }
            is MigrationResult.Failure -> {
                Log.e(TAG, "❌ Migration failed: ${result.reason}")
                Log.e(TAG, "   Failed count: ${result.failedCount}")
                // Optionally notify user or retry
            }
            is MigrationResult.AlreadyMigrated -> {
                Log.d(TAG, "✓ Migration already completed (${result.migratedCount} permissions)")
            }
        }
    }
}
```

**Performance:** 50-100ms for 10 permissions (one-time cost)

### Manual Migration Trigger

If you need to manually trigger migration (e.g., in settings):

```kotlin
suspend fun manuallyMigratePermissions() {
    val progressDialog = showProgressDialog("Migrating permissions...")

    try {
        val result = permissionStorage.migrateToEncrypted()

        when (result) {
            is MigrationResult.Success -> {
                showSuccessToast("Migrated ${result.migratedCount} permissions")
            }
            is MigrationResult.Failure -> {
                showErrorDialog("Migration failed: ${result.reason}")
            }
            is MigrationResult.AlreadyMigrated -> {
                showInfoToast("Migration already completed")
            }
        }
    } finally {
        progressDialog.dismiss()
    }
}
```

### Checking Migration Status

```kotlin
fun checkMigrationStatus() {
    val encryptionStatus = permissionStorage.getEncryptionStatus()

    Log.i(TAG, """
        Encryption Status:
        - Encrypted: ${encryptionStatus.isEncrypted}
        - Hardware-Backed: ${encryptionStatus.isHardwareBacked}
        - Migration Complete: ${encryptionStatus.migrationCompleted}
        - Migrated Count: ${encryptionStatus.migratedPermissionCount}
        - Key Alias: ${encryptionStatus.keyAlias}
    """.trimIndent())

    if (!encryptionStatus.migrationCompleted) {
        Log.w(TAG, "⚠️  Migration not yet completed - permissions not encrypted!")
    }

    if (!encryptionStatus.isHardwareBacked) {
        Log.w(TAG, "⚠️  Encryption NOT hardware-backed - using software keystore!")
    }
}
```

---

## Advanced Usage

### Custom Dependency Injection (Manual)

If not using Hilt, create PermissionStorage manually:

```kotlin
class PluginRepository(context: Context) {
    private val permissionStorage = PermissionStorage(context.applicationContext)

    fun grantPermission(pluginId: String, permission: Permission) {
        permissionStorage.savePermission(
            pluginId = pluginId,
            permission = permission,
            status = GrantStatus.GRANTED
        )
    }
}
```

### Background Permission Queries

For long-running operations, use coroutines:

```kotlin
suspend fun checkPermissionsInBackground(pluginIds: List<String>) {
    withContext(Dispatchers.IO) {
        pluginIds.forEach { pluginId ->
            val permissions = permissionStorage.getAllPermissions(pluginId)

            if (permissions[Permission.ACCESSIBILITY_SERVICES] != GrantStatus.GRANTED) {
                Log.w(TAG, "$pluginId missing ACCESSIBILITY_SERVICES permission")
            }
        }
    }
}
```

### Batch Permission Grants

```kotlin
fun grantMultiplePermissions(
    pluginId: String,
    permissions: List<Permission>,
    grantedBy: String = "system"
) {
    permissions.forEach { permission ->
        permissionStorage.savePermission(
            pluginId = pluginId,
            permission = permission,
            status = GrantStatus.GRANTED,
            grantedBy = grantedBy
        )
    }

    Log.i(TAG, "Granted ${permissions.size} permissions to $pluginId")
}

// Example: Grant all required permissions for a core plugin
grantMultiplePermissions(
    pluginId = "com.augmentalis.core.accessibility",
    permissions = listOf(
        Permission.ACCESSIBILITY_SERVICES,
        Permission.OVERLAY,
        Permission.SYSTEM_SETTINGS
    ),
    grantedBy = "system"
)
```

### Observing Permission Changes

```kotlin
class PermissionRepository @Inject constructor(
    private val permissionStorage: PermissionStorage
) {
    private val _permissionChanges = MutableSharedFlow<PermissionChange>()
    val permissionChanges: SharedFlow<PermissionChange> = _permissionChanges.asSharedFlow()

    fun savePermission(
        pluginId: String,
        permission: Permission,
        status: GrantStatus
    ) {
        permissionStorage.savePermission(pluginId, permission, status)

        // Emit change event
        lifecycleScope.launch {
            _permissionChanges.emit(PermissionChange(pluginId, permission, status))
        }
    }
}

data class PermissionChange(
    val pluginId: String,
    val permission: Permission,
    val newStatus: GrantStatus
)

// Consumer
permissionRepository.permissionChanges.collect { change ->
    Log.i(TAG, "Permission changed: ${change.pluginId}.${change.permission} → ${change.newStatus}")
    updateUI(change)
}
```

---

## Troubleshooting

### Issue: "Keystore unavailable" error

**Symptom:** EncryptionException with message "AndroidKeyStore unavailable"

**Cause:** Device lacks hardware keystore (rare on API 28+)

**Solution:**
1. Check device API level: Must be Android 9.0+ (API 28+)
2. Verify device has TEE/TrustZone support
3. Fallback to software keystore if hardware unavailable:

```kotlin
try {
    val masterKey = MasterKey.Builder(context, keyAlias)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .setRequestStrongBoxBacked(true)  // Prefer hardware
        .build()
} catch (e: Exception) {
    Log.w(TAG, "Hardware keystore unavailable, using software fallback", e)
    val masterKey = MasterKey.Builder(context, keyAlias)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .setRequestStrongBoxBacked(false)  // Software fallback
        .build()
}
```

### Issue: "Key permanently invalidated" error

**Symptom:** EncryptionException with message "Key was invalidated"

**Cause:** User cleared security credentials in Settings, or device was factory reset

**Solution:** Regenerate encryption key (data loss):

```kotlin
fun handleKeyInvalidation() {
    Log.e(TAG, "Encryption key invalidated - regenerating (data loss)")

    // Delete old encrypted file
    context.deleteSharedPreferences("plugin_permissions_encrypted")

    // Create new PermissionStorage (generates new key)
    permissionStorage = PermissionStorage(context)

    // Notify user that permissions need to be re-granted
    showNotification("Permissions cleared due to security credential reset")
}
```

### Issue: Migration fails with "corrupted data"

**Symptom:** MigrationResult.Failure with failedCount > 0

**Cause:** Corrupted plain-text SharedPreferences file

**Solution:** Skip corrupted entries, migrate valid ones:

```kotlin
when (val result = permissionStorage.migrateToEncrypted()) {
    is MigrationResult.Failure -> {
        if (result.failedCount > 0) {
            Log.w(TAG, "${result.failedCount} permissions failed to migrate (corrupted)")
            // Migration continues with valid entries
            // Corrupted entries are logged and skipped
        }
    }
    else -> { /* ... */ }
}
```

### Issue: Slow permission queries (>10ms)

**Symptom:** getPermission() takes >10ms consistently

**Cause:** EncryptedSharedPreferences creation overhead or disk I/O bottleneck

**Solution:** Implement in-memory caching:

```kotlin
class CachedPermissionStorage(
    private val delegate: PermissionStorage
) : PermissionStorage by delegate {

    private val cache = ConcurrentHashMap<String, GrantStatus>()

    override fun getPermission(pluginId: String, permission: Permission): GrantStatus? {
        val cacheKey = "$pluginId.$permission"

        return cache.getOrPut(cacheKey) {
            delegate.getPermission(pluginId, permission) ?: return@getOrPut null
        }
    }

    override fun savePermission(
        pluginId: String,
        permission: Permission,
        status: GrantStatus,
        grantedBy: String
    ) {
        delegate.savePermission(pluginId, permission, status, grantedBy)

        // Invalidate cache
        val cacheKey = "$pluginId.$permission"
        cache[cacheKey] = status
    }
}
```

### Issue: "Permission data unreadable" after device restore

**Symptom:** getPermission() returns null for all permissions after restoring Android backup

**Cause:** Encryption keys are device-bound and not backed up

**Solution:** This is **expected behavior** (security feature). Users must re-grant permissions:

```kotlin
fun handlePostRestorePermissions() {
    val encryptionStatus = permissionStorage.getEncryptionStatus()

    if (!encryptionStatus.migrationCompleted || encryptionStatus.migratedPermissionCount == 0) {
        Log.i(TAG, "No permissions found post-restore (expected)")
        showNotification("Please re-grant plugin permissions")

        // Optionally: Prompt user to re-grant permissions for installed plugins
        promptPermissionReGrant()
    }
}
```

---

## Security Best Practices

### 1. Always Exclude Encrypted Data from Backups

**Critical:** Ensure encrypted SharedPreferences file is excluded from Android Auto Backup.

```xml
<!-- res/xml/backup_rules.xml -->
<full-backup-content>
    <exclude domain="sharedpref" path="plugin_permissions_encrypted.xml"/>
</full-backup-content>
```

**Why:** Even though data is encrypted, if both encrypted file AND encryption keys were backed up together, an attacker with backup access could decrypt. By excluding the encrypted file, backups contain NO permission data at all.

### 2. Verify Hardware-Backed Encryption

```kotlin
fun verifyHardwareEncryption() {
    val status = permissionStorage.getEncryptionStatus()

    if (!status.isHardwareBacked) {
        Log.w(TAG, "⚠️  SECURITY WARNING: Encryption NOT hardware-backed!")
        Log.w(TAG, "Keys stored in software keystore (less secure)")

        // Optionally notify security-conscious users
        if (BuildConfig.DEBUG) {
            throw SecurityException("Hardware encryption required in production")
        }
    } else {
        Log.i(TAG, "✓ Encryption is hardware-backed (TEE/TrustZone/StrongBox)")
    }
}
```

### 3. Monitor Encryption Failures

```kotlin
fun monitorEncryptionHealth() {
    try {
        permissionStorage.getPermission("test.plugin", Permission.STORAGE)
    } catch (e: EncryptionException) {
        Log.e(TAG, "Encryption failure detected", e)

        // Report to crash analytics
        crashlytics.recordException(e)

        // Notify security team
        securityMonitor.reportEncryptionFailure(e)
    }
}
```

### 4. Audit Permission Access

```kotlin
fun auditPermissionAccess(pluginId: String, permission: Permission) {
    val status = permissionStorage.getPermission(pluginId, permission)

    // Log all permission checks for security audit
    securityLog.log(
        event = "PERMISSION_CHECK",
        pluginId = pluginId,
        permission = permission.name,
        status = status?.name ?: "NOT_FOUND",
        timestamp = System.currentTimeMillis()
    )

    if (status == GrantStatus.GRANTED) {
        // Additional logging for granted permissions
        securityLog.log(
            event = "PERMISSION_USED",
            pluginId = pluginId,
            permission = permission.name
        )
    }
}
```

### 5. Implement Permission Expiry (Optional)

For enhanced security, implement time-based permission expiry:

```kotlin
fun checkPermissionWithExpiry(
    pluginId: String,
    permission: Permission,
    expiryDurationMs: Long = 30 * 24 * 60 * 60 * 1000  // 30 days
): Boolean {
    val status = permissionStorage.getPermission(pluginId, permission)

    if (status != GrantStatus.GRANTED) {
        return false
    }

    // Check if permission has expired (requires timestamp tracking)
    val grantTimestamp = getPermissionTimestamp(pluginId, permission)
    val currentTime = System.currentTimeMillis()

    if (currentTime - grantTimestamp > expiryDurationMs) {
        Log.i(TAG, "Permission expired for $pluginId.$permission")
        permissionStorage.revokePermission(pluginId, permission)
        return false
    }

    return true
}
```

---

## Next Steps

1. **Read the Full Specification:** See `spec.md` for detailed requirements
2. **Review Data Model:** See `data-model.md` for entity relationships
3. **Check API Contract:** See `contracts/PermissionStorage.kt` for complete API
4. **Run Tests:** See Phase 3 in `plan.md` for comprehensive test strategy
5. **Implement:** Follow tasks in `tasks.md` (generated by `/idea.tasks`)

---

## Resources

- **AndroidX Security Library:** https://developer.android.com/topic/security/data
- **Android Keystore System:** https://developer.android.com/training/articles/keystore
- **EncryptedSharedPreferences:** https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences
- **VOS4 Constitution:** `.ideacode/memory/principles.md`
- **VOS4 Documentation:** `docs/modules/PluginSystem/`

---

**Version:** 1.0.0
**Last Updated:** 2025-10-26
**Status:** Design Phase
