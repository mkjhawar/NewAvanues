package com.augmentalis.magiccode.plugins.security

/**
 * Mock implementation of PermissionStorage for unit testing.
 *
 * Provides in-memory storage without actual encryption, suitable for testing
 * business logic without Android dependencies.
 */
class MockPermissionStorage(
    private val isEncrypted: Boolean = true,
    private val isHardwareBacked: Boolean = true
) {
    private val storage = mutableMapOf<String, MutableSet<String>>()
    private var migrated = false

    fun savePermission(pluginId: String, permission: String) {
        require(pluginId.isNotBlank()) { "pluginId cannot be blank" }
        require(permission.isNotBlank()) { "permission cannot be blank" }

        storage.getOrPut(pluginId) { mutableSetOf() }.add(permission)
    }

    fun hasPermission(pluginId: String, permission: String): Boolean {
        return storage[pluginId]?.contains(permission) ?: false
    }

    fun getAllPermissions(pluginId: String): Set<String> {
        return storage[pluginId]?.toSet() ?: emptySet()
    }

    fun revokePermission(pluginId: String, permission: String) {
        storage[pluginId]?.remove(permission)
    }

    fun clearAllPermissions(pluginId: String) {
        storage.remove(pluginId)
    }

    fun isEncrypted(): Boolean = isEncrypted

    fun getEncryptionStatus(): EncryptionStatus {
        return EncryptionStatus(
            isEncrypted = isEncrypted,
            isHardwareBacked = isHardwareBacked,
            keyAlgorithm = if (isEncrypted) "AES256-GCM" else "NONE",
            migrationComplete = migrated
        )
    }

    suspend fun migrateToEncrypted(): MigrationResult {
        val count = storage.values.sumOf { it.size }
        return if (migrated) {
            MigrationResult.AlreadyMigrated(
                migratedCount = count,
                migrationTimestamp = System.currentTimeMillis()
            )
        } else {
            migrated = true
            MigrationResult.Success(migratedCount = count)
        }
    }
}

/**
 * Encryption status information for mock testing.
 */
data class EncryptionStatus(
    val isEncrypted: Boolean,
    val isHardwareBacked: Boolean,
    val keyAlgorithm: String,
    val migrationComplete: Boolean
)
