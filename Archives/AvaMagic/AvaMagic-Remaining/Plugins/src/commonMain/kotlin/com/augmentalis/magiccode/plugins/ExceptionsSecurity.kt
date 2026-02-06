package com.augmentalis.avacode.plugins

/**
 * Exception thrown when encryption operation fails.
 *
 * Wraps underlying encryption failures (keystore unavailable, key invalidated,
 * GCM authentication failure, etc.) with context-specific error messages.
 *
 * ## Common Causes
 * - Android Keystore unavailable (device lacks hardware security)
 * - Encryption key was invalidated (user cleared security credentials)
 * - GCM authentication tag mismatch (data tampered or corrupted)
 * - SharedPreferences file corrupted
 *
 * ## Security Note
 * This exception should trigger fail-secure behavior - deny permission grant
 * rather than fall back to unencrypted storage.
 *
 * @param message Human-readable error description
 * @param cause Underlying exception that caused the encryption failure
 * @since 1.1.0
 */
class EncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when migration operation fails.
 *
 * Indicates that migration from plain-text to encrypted storage failed and
 * should be retried or investigated.
 *
 * ## Recovery Strategy
 * - Migration is idempotent (safe to retry)
 * - Plain-text file is preserved on failure
 * - Check [failedCount] to see how many permissions failed
 * - Review logs for specific permission failures
 *
 * @param message Human-readable error description
 * @param failedCount Number of permissions that failed to migrate
 * @param cause Underlying exception that caused the migration failure
 * @since 1.1.0
 */
class MigrationException(
    message: String,
    val failedCount: Int,
    cause: Throwable? = null
) : Exception(message, cause)
