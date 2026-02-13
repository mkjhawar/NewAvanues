/**
 * ICredentialStore.kt - Cross-platform encrypted credential storage interface
 *
 * Abstraction for secure credential persistence. Android implements via
 * EncryptedSharedPreferences (AES256-GCM), iOS via Keychain Services,
 * Desktop via platform credential managers.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.foundation.settings

/**
 * Platform-agnostic secure credential storage interface.
 *
 * All values are stored encrypted at rest. Implementations must use
 * the strongest available platform encryption:
 * - Android: EncryptedSharedPreferences with AES256_GCM
 * - iOS: Keychain Services with kSecAttrAccessibleWhenUnlockedThisDeviceOnly
 * - Desktop: Platform credential manager or encrypted file store
 */
interface ICredentialStore {

    /**
     * Store a credential value securely.
     *
     * @param key Unique identifier for the credential
     * @param value The secret value to store (encrypted at rest)
     */
    suspend fun store(key: String, value: String)

    /**
     * Retrieve a previously stored credential.
     *
     * @param key Unique identifier for the credential
     * @return The decrypted value, or null if not found
     */
    suspend fun retrieve(key: String): String?

    /**
     * Delete a stored credential.
     *
     * @param key Unique identifier for the credential to remove
     */
    suspend fun delete(key: String)

    /**
     * Check whether a credential exists without retrieving it.
     *
     * @param key Unique identifier to check
     * @return true if a value is stored for this key
     */
    suspend fun hasCredential(key: String): Boolean
}
