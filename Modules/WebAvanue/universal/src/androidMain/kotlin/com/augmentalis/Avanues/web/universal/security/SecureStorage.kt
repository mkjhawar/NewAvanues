package com.augmentalis.Avanues.web.universal.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.augmentalis.Avanues.web.universal.presentation.ui.security.HttpAuthCredentials
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.SecureStorageProvider

/**
 * SecureStorage - Android implementation of encrypted credential storage
 *
 * Security Features:
 * - AES256-GCM encryption for values
 * - AES256-SIV encryption for keys
 * - Master key stored in Android Keystore (hardware-backed if available)
 * - No plaintext credentials in memory or storage
 *
 * Phase 1 Security Fix:
 * - CWE-311: Missing encryption for sensitive data
 * - CWE-798: Use of hardcoded credentials
 *
 * Usage:
 * ```kotlin
 * val storage = SecureStorage(context)
 * storage.storeCredential("https://example.com", "user", "pass")
 * val creds = storage.getCredential("https://example.com")
 * ```
 *
 * @param context Android context (must be Application context for lifecycle safety)
 */
class SecureStorage(context: Context) : SecureStorageProvider {

    companion object {
        private const val PREFS_NAME = "webavanue_secure_prefs"
        private const val MASTER_KEY_ALIAS = "webavanue_master_key"

        // Key suffixes for credential storage
        private const val USERNAME_SUFFIX = ":username"
        private const val PASSWORD_SUFFIX = ":password"
        private const val REMEMBER_SUFFIX = ":remember"
    }

    // Master key for encryption (stored in Android Keystore)
    private val masterKey: MasterKey = MasterKey.Builder(context.applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // Encrypted SharedPreferences
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Store HTTP authentication credentials securely
     *
     * @param url URL/host for which credentials are stored
     * @param username Username
     * @param password Password (will be encrypted)
     * @param remember Whether to remember credentials (default: false)
     */
    override fun storeCredential(url: String, username: String, password: String, remember: Boolean) {
        try {
            // Normalize URL (remove trailing slashes, lowercase)
            val normalizedUrl = normalizeUrl(url)

            encryptedPrefs.edit().apply {
                putString(normalizedUrl + USERNAME_SUFFIX, username)
                putString(normalizedUrl + PASSWORD_SUFFIX, password)
                putBoolean(normalizedUrl + REMEMBER_SUFFIX, remember)
                apply()
            }

            println("✅ SecureStorage: Credentials stored for $normalizedUrl (remember: $remember)")
        } catch (e: Exception) {
            println("⚠️  SecureStorage: Failed to store credentials: ${e.message}")
            throw SecureStorageException("Failed to store credentials", e)
        }
    }

    /**
     * Retrieve HTTP authentication credentials
     *
     * @param url URL/host for which to retrieve credentials
     * @return Pair of (username, password) or null if not found
     */
    override fun getCredential(url: String): HttpAuthCredentials? {
        return try {
            val normalizedUrl = normalizeUrl(url)

            val username = encryptedPrefs.getString(normalizedUrl + USERNAME_SUFFIX, null)
            val password = encryptedPrefs.getString(normalizedUrl + PASSWORD_SUFFIX, null)
            val remember = encryptedPrefs.getBoolean(normalizedUrl + REMEMBER_SUFFIX, false)

            if (username != null && password != null) {
                println("✅ SecureStorage: Credentials retrieved for $normalizedUrl")
                HttpAuthCredentials(username, password, remember)
            } else {
                null
            }
        } catch (e: Exception) {
            println("⚠️  SecureStorage: Failed to retrieve credentials: ${e.message}")
            null
        }
    }

    /**
     * Check if credentials exist for a URL
     *
     * @param url URL/host to check
     * @return true if credentials exist
     */
    override fun hasCredential(url: String): Boolean {
        return try {
            val normalizedUrl = normalizeUrl(url)
            encryptedPrefs.contains(normalizedUrl + USERNAME_SUFFIX)
        } catch (e: Exception) {
            println("⚠️  SecureStorage: Failed to check credentials: ${e.message}")
            false
        }
    }

    /**
     * Remove stored credentials
     *
     * @param url URL/host for which to remove credentials
     */
    override fun removeCredential(url: String) {
        try {
            val normalizedUrl = normalizeUrl(url)

            encryptedPrefs.edit().apply {
                remove(normalizedUrl + USERNAME_SUFFIX)
                remove(normalizedUrl + PASSWORD_SUFFIX)
                remove(normalizedUrl + REMEMBER_SUFFIX)
                apply()
            }

            println("✅ SecureStorage: Credentials removed for $normalizedUrl")
        } catch (e: Exception) {
            println("⚠️  SecureStorage: Failed to remove credentials: ${e.message}")
            throw SecureStorageException("Failed to remove credentials", e)
        }
    }

    /**
     * Clear all stored credentials
     */
    override fun clearAll() {
        try {
            encryptedPrefs.edit().clear().apply()
            println("✅ SecureStorage: All credentials cleared")
        } catch (e: Exception) {
            println("⚠️  SecureStorage: Failed to clear credentials: ${e.message}")
            throw SecureStorageException("Failed to clear credentials", e)
        }
    }

    /**
     * Get all stored URLs
     *
     * @return List of URLs that have stored credentials
     */
    override fun getStoredUrls(): List<String> {
        return try {
            encryptedPrefs.all.keys
                .filter { it.endsWith(USERNAME_SUFFIX) }
                .map { it.removeSuffix(USERNAME_SUFFIX) }
        } catch (e: Exception) {
            println("⚠️  SecureStorage: Failed to list URLs: ${e.message}")
            emptyList()
        }
    }

    /**
     * Normalize URL for consistent storage keys
     * - Removes protocol (http://, https://)
     * - Removes trailing slashes
     * - Converts to lowercase
     * - Preserves port numbers
     *
     * @param url URL to normalize
     * @return Normalized URL
     */
    private fun normalizeUrl(url: String): String {
        return url
            .replace(Regex("^https?://"), "") // Remove protocol
            .trimEnd('/') // Remove trailing slashes
            .lowercase() // Lowercase for consistency
    }
}

/**
 * Exception thrown by SecureStorage operations
 */
class SecureStorageException(message: String, cause: Throwable? = null) : Exception(message, cause)
