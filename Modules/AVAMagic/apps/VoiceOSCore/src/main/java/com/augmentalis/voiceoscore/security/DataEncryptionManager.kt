/**
 * DataEncryptionManager.kt - Data encryption layer for sensitive information
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-09
 * Phase: 3 (Medium Priority)
 * Issue: Data encryption layer for sensitive data
 */
package com.augmentalis.voiceoscore.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Thread-safe data encryption manager using Android Keystore
 *
 * Features:
 * - AES-256-GCM encryption (authenticated encryption)
 * - Keys stored in Android Keystore (hardware-backed if available)
 * - Automatic key generation and management
 * - Base64 encoding for database storage
 * - Thread-safe encryption/decryption
 * - Secure random IV generation per encryption
 *
 * Security properties:
 * - Confidentiality: AES-256 encryption
 * - Integrity: GCM authentication tag
 * - Non-repudiation: Each encryption uses unique IV
 * - Key security: Android Keystore protection
 *
 * Usage:
 * ```kotlin
 * val encryptionManager = DataEncryptionManager(context)
 *
 * // Encrypt sensitive data
 * val plaintext = "User's voice command"
 * val encrypted = encryptionManager.encrypt(plaintext)
 * // Store encrypted in database
 *
 * // Decrypt when needed
 * val decrypted = encryptionManager.decrypt(encrypted)
 * // Use decrypted data
 * ```
 *
 * Thread Safety: All operations are thread-safe
 */
class DataEncryptionManager(
    private val context: Context,
    private val keyAlias: String = "VoiceOS_Master_Key"
) {
    companion object {
        private const val TAG = "DataEncryptionManager"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12 // 96 bits
        private const val GCM_TAG_LENGTH = 128 // 128 bits
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        // Ensure encryption key exists
        ensureKeyExists()
    }

    /**
     * Encrypt plaintext string to Base64-encoded ciphertext
     *
     * Uses AES-256-GCM with random IV for each encryption.
     * Output format: Base64(IV || Ciphertext || AuthTag)
     *
     * @param plaintext String to encrypt (can be null)
     * @return Base64-encoded encrypted string, or null if input is null
     * @throws EncryptionException if encryption fails
     */
    fun encrypt(plaintext: String?): String? {
        if (plaintext == null) return null
        if (plaintext.isEmpty()) return ""

        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getKey())

            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            // Combine IV + Ciphertext for storage
            val combined = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

            // Base64 encode for database storage
            Base64.encodeToString(combined, Base64.NO_WRAP)

        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            throw EncryptionException("Encryption failed: ${e.message}", e)
        }
    }

    /**
     * Decrypt Base64-encoded ciphertext to plaintext string
     *
     * Expects format: Base64(IV || Ciphertext || AuthTag)
     *
     * @param ciphertext Base64-encoded encrypted string (can be null)
     * @return Decrypted plaintext string, or null if input is null
     * @throws DecryptionException if decryption fails (wrong key, corrupted data, etc.)
     */
    fun decrypt(ciphertext: String?): String? {
        if (ciphertext == null) return null
        if (ciphertext.isEmpty()) return ""

        return try {
            // Base64 decode
            val combined = Base64.decode(ciphertext, Base64.NO_WRAP)

            // Extract IV and ciphertext
            val iv = ByteArray(GCM_IV_LENGTH)
            val encrypted = ByteArray(combined.size - GCM_IV_LENGTH)

            System.arraycopy(combined, 0, iv, 0, iv.size)
            System.arraycopy(combined, iv.size, encrypted, 0, encrypted.size)

            // Decrypt
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)

            val plaintext = cipher.doFinal(encrypted)
            String(plaintext, Charsets.UTF_8)

        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            throw DecryptionException("Decryption failed: ${e.message}", e)
        }
    }

    /**
     * Encrypt byte array to Base64-encoded ciphertext
     *
     * @param data Byte array to encrypt (can be null)
     * @return Base64-encoded encrypted string, or null if input is null
     * @throws EncryptionException if encryption fails
     */
    fun encryptBytes(data: ByteArray?): String? {
        if (data == null) return null
        if (data.isEmpty()) return ""

        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getKey())

            val iv = cipher.iv
            val ciphertext = cipher.doFinal(data)

            // Combine IV + Ciphertext
            val combined = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)

        } catch (e: Exception) {
            Log.e(TAG, "Byte encryption failed", e)
            throw EncryptionException("Byte encryption failed: ${e.message}", e)
        }
    }

    /**
     * Decrypt Base64-encoded ciphertext to byte array
     *
     * @param ciphertext Base64-encoded encrypted string (can be null)
     * @return Decrypted byte array, or null if input is null
     * @throws DecryptionException if decryption fails
     */
    fun decryptBytes(ciphertext: String?): ByteArray? {
        if (ciphertext == null) return null
        if (ciphertext.isEmpty()) return ByteArray(0)

        return try {
            val combined = Base64.decode(ciphertext, Base64.NO_WRAP)

            val iv = ByteArray(GCM_IV_LENGTH)
            val encrypted = ByteArray(combined.size - GCM_IV_LENGTH)

            System.arraycopy(combined, 0, iv, 0, iv.size)
            System.arraycopy(combined, iv.size, encrypted, 0, encrypted.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)

            cipher.doFinal(encrypted)

        } catch (e: Exception) {
            Log.e(TAG, "Byte decryption failed", e)
            throw DecryptionException("Byte decryption failed: ${e.message}", e)
        }
    }

    /**
     * Check if data is encrypted (heuristic check)
     *
     * Checks if string is valid Base64 and has expected length.
     * Not 100% reliable but useful for migration scenarios.
     *
     * @param data String to check
     * @return true if data appears to be encrypted
     */
    fun isEncrypted(data: String?): Boolean {
        if (data == null || data.isEmpty()) return false

        return try {
            val decoded = Base64.decode(data, Base64.NO_WRAP)
            // Encrypted data should have at least IV + some ciphertext
            decoded.size > GCM_IV_LENGTH
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Re-encrypt data with new key
     *
     * Useful for key rotation scenarios.
     *
     * @param oldCiphertext Existing encrypted data
     * @param newKeyAlias New key alias to use
     * @return Re-encrypted data with new key
     */
    fun reencrypt(oldCiphertext: String, newKeyAlias: String): String {
        // Decrypt with current key
        val plaintext = decrypt(oldCiphertext)

        // Encrypt with new key
        val newManager = DataEncryptionManager(context, newKeyAlias)
        return newManager.encrypt(plaintext)
            ?: throw EncryptionException(
                "Re-encryption failed: encryption with new key '$newKeyAlias' returned null. " +
                "Expected: encrypted ciphertext. Actual: null. " +
                "Verify: (1) new key '$newKeyAlias' exists in Android Keystore, " +
                "(2) key generation succeeded, (3) plaintext is valid, (4) sufficient memory available. " +
                "Check Keystore state and retry key migration."
            )
    }

    /**
     * Delete encryption key
     *
     * WARNING: All data encrypted with this key will become unrecoverable.
     * Only use when intentionally destroying encrypted data.
     */
    fun deleteKey() {
        try {
            keyStore.deleteEntry(keyAlias)
            Log.i(TAG, "Deleted encryption key: $keyAlias")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete key", e)
            throw SecurityException(
                "Failed to delete encryption key '$keyAlias': ${e.message}. " +
                "Expected: key successfully removed from Android Keystore. Actual: deletion failed. " +
                "Possible causes: (1) key does not exist, (2) Keystore locked or unavailable, " +
                "(3) insufficient permissions, (4) OS denies key deletion. " +
                "Ensure all data encrypted with this key is backed up before retry.",
                e
            )
        }
    }

    /**
     * Check if encryption key exists
     *
     * @return true if key exists in keystore
     */
    fun keyExists(): Boolean {
        return try {
            keyStore.containsAlias(keyAlias)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get or create encryption key
     */
    private fun getKey(): SecretKey {
        ensureKeyExists()
        return (keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Ensure encryption key exists, create if missing
     */
    private fun ensureKeyExists() {
        if (!keyStore.containsAlias(keyAlias)) {
            generateKey()
        }
    }

    /**
     * Generate new AES-256 key in Android Keystore
     */
    private fun generateKey() {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true) // Force unique IV per encryption
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()

            Log.i(TAG, "Generated new encryption key: $keyAlias")

        } catch (e: Exception) {
            Log.e(TAG, "Key generation failed", e)
            throw SecurityException(
                "Failed to generate encryption key '$keyAlias': ${e.message}. " +
                "Expected: new AES-256 key created in Android Keystore. Actual: generation failed. " +
                "Possible causes: (1) Android Keystore unavailable, (2) device incompatibility, " +
                "(3) insufficient memory, (4) OS version too old (API < 21), (5) Keystore locked. " +
                "Verify API level, Keystore status, and memory availability.",
                e
            )
        }
    }
}

/**
 * Exception thrown when encryption operation fails
 */
class EncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when decryption operation fails
 *
 * Common causes:
 * - Wrong decryption key
 * - Corrupted ciphertext
 * - Tampered authentication tag
 * - Invalid IV
 */
class DecryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)
