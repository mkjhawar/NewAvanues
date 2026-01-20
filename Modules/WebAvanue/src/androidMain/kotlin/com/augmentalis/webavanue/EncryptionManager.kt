package com.augmentalis.webavanue

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages database encryption keys using Android Keystore.
 *
 * Security Features:
 * - AES-256-GCM encryption
 * - Keys stored in hardware-backed Android Keystore (when available)
 * - Automatic key generation and rotation
 * - Secure key derivation for SQLCipher passphrase
 *
 * CWE-311 Mitigation: Encrypts sensitive browser data at rest
 */
class EncryptionManager(private val context: Context) {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "webavanue_db_master_key"
        private const val PREFS_NAME = "webavanue_encryption"
        private const val PREFS_ENCRYPTED_PASSPHRASE = "encrypted_passphrase"
        private const val PREFS_IV = "passphrase_iv"
        private const val AES_KEY_SIZE = 256
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val PASSPHRASE_LENGTH = 32 // 256 bits for SQLCipher
    }

    /**
     * Gets or creates the database encryption passphrase.
     *
     * Flow:
     * 1. Check if passphrase exists in SharedPreferences (encrypted)
     * 2. If not, generate new random passphrase
     * 3. Encrypt passphrase using Android Keystore key
     * 4. Store encrypted passphrase in SharedPreferences
     * 5. Return decrypted passphrase for SQLCipher
     *
     * @return Byte array passphrase for SQLCipher (32 bytes)
     */
    fun getOrCreateDatabasePassphrase(): ByteArray {
        // Check if passphrase already exists
        val encryptedPassphrase = sharedPrefs.getString(PREFS_ENCRYPTED_PASSPHRASE, null)
        val iv = sharedPrefs.getString(PREFS_IV, null)

        return if (encryptedPassphrase != null && iv != null) {
            // Decrypt existing passphrase
            decryptPassphrase(encryptedPassphrase, iv)
        } else {
            // Generate new passphrase
            val newPassphrase = generateRandomPassphrase()

            // Encrypt and store it
            val (encrypted, ivBytes) = encryptPassphrase(newPassphrase)
            sharedPrefs.edit()
                .putString(PREFS_ENCRYPTED_PASSPHRASE, encrypted)
                .putString(PREFS_IV, ivBytes)
                .apply()

            newPassphrase
        }
    }

    /**
     * Generates a cryptographically secure random passphrase.
     */
    private fun generateRandomPassphrase(): ByteArray {
        return ByteArray(PASSPHRASE_LENGTH).apply {
            java.security.SecureRandom().nextBytes(this)
        }
    }

    /**
     * Gets or creates the master encryption key in Android Keystore.
     */
    private fun getOrCreateMasterKey(): SecretKey {
        // Check if key already exists
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }

        // Generate new key
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_SIZE)
            .setUserAuthenticationRequired(false) // Don't require biometric for each access
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts the database passphrase using Android Keystore key.
     *
     * @param passphrase The plaintext passphrase to encrypt
     * @return Pair of (base64 encrypted data, base64 IV)
     */
    private fun encryptPassphrase(passphrase: ByteArray): Pair<String, String> {
        val key = getOrCreateMasterKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val encrypted = cipher.doFinal(passphrase)

        return Pair(
            Base64.encodeToString(encrypted, Base64.NO_WRAP),
            Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    /**
     * Decrypts the database passphrase using Android Keystore key.
     *
     * @param encryptedPassphrase Base64 encoded encrypted passphrase
     * @param ivString Base64 encoded IV
     * @return Decrypted passphrase bytes
     */
    private fun decryptPassphrase(encryptedPassphrase: String, ivString: String): ByteArray {
        val key = getOrCreateMasterKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        val iv = Base64.decode(ivString, Base64.NO_WRAP)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val encrypted = Base64.decode(encryptedPassphrase, Base64.NO_WRAP)
        return cipher.doFinal(encrypted)
    }

    /**
     * Rotates the database encryption key.
     *
     * IMPORTANT: This requires re-encrypting the entire database.
     * Should only be called during maintenance windows.
     *
     * Process:
     * 1. Generate new passphrase
     * 2. Export database with old key
     * 3. Re-encrypt with new key
     * 4. Replace old database
     * 5. Clean up old passphrase
     *
     * @return New passphrase for database re-encryption
     */
    fun rotateEncryptionKey(): ByteArray {
        // Generate new passphrase
        val newPassphrase = generateRandomPassphrase()

        // Encrypt and store new passphrase
        val (encrypted, iv) = encryptPassphrase(newPassphrase)
        sharedPrefs.edit()
            .putString(PREFS_ENCRYPTED_PASSPHRASE, encrypted)
            .putString(PREFS_IV, iv)
            .apply()

        return newPassphrase
    }

    /**
     * Checks if encryption key exists (database is already encrypted).
     */
    fun hasEncryptionKey(): Boolean {
        return sharedPrefs.contains(PREFS_ENCRYPTED_PASSPHRASE)
    }

    /**
     * Deletes encryption key (for testing or reset scenarios).
     * WARNING: This will make the encrypted database unreadable.
     */
    fun deleteEncryptionKey() {
        // Delete from SharedPreferences
        sharedPrefs.edit()
            .remove(PREFS_ENCRYPTED_PASSPHRASE)
            .remove(PREFS_IV)
            .apply()

        // Delete from Keystore
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }
}
