package com.augmentalis.magiccode.plugins.security

/**
 * Detailed encryption status information for permission storage.
 *
 * Contains diagnostic information about the current state of permission
 * encryption, useful for logging, security audits, and troubleshooting.
 *
 * ## Security Implications
 * - **isHardwareBacked = true**: Keys stored in TEE/TrustZone (best security)
 * - **isHardwareBacked = false**: Keys stored in software keystore (degraded security)
 * - **isEncrypted = false**: Permissions stored in plain-text (INSECURE)
 *
 * ## Usage Example
 * ```kotlin
 * val status = permissionStorage.getEncryptionStatus()
 *
 * if (!status.isHardwareBacked) {
 *     Log.w(TAG, "WARNING: Encryption NOT hardware-backed!")
 * }
 *
 * if (!status.migrationCompleted) {
 *     Log.w(TAG, "Migration pending - permissions not encrypted")
 * }
 * ```
 *
 * @property isEncrypted Whether permissions are currently encrypted
 * @property isHardwareBacked Whether encryption keys are stored in hardware (TEE/TrustZone/StrongBox)
 * @property migrationCompleted Whether migration from plain-text to encrypted has completed
 * @property keyAlias Alias of the master key in Android Keystore
 * @property encryptionScheme Description of encryption scheme (e.g., "AES256_GCM")
 * @property migratedPermissionCount Number of permissions migrated (0 if not migrated)
 * @since 1.1.0
 */
data class EncryptionStatus(
    val isEncrypted: Boolean,
    val isHardwareBacked: Boolean,
    val migrationCompleted: Boolean,
    val keyAlias: String,
    val encryptionScheme: String = "AES256_GCM",
    val migratedPermissionCount: Int = 0
)
