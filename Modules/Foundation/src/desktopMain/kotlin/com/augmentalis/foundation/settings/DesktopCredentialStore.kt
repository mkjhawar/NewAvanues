/*
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC.
 * All rights reserved.
 */

package com.augmentalis.foundation.settings

import java.io.File
import java.security.SecureRandom
import java.util.Base64
import java.util.prefs.Preferences
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Desktop (JVM) implementation of [ICredentialStore] using AES-256-GCM encryption
 * backed by java.util.prefs.Preferences for storage.
 *
 * Encryption key is stored at `~/.avanues/credential.key` and generated once
 * with SecureRandom on first use. If the key file is missing or corrupt,
 * a new key is generated (existing credentials become unreadable and are
 * silently cleared on next access).
 *
 * Ciphertext format: [12-byte IV][GCM ciphertext+tag] → Base64 → Preferences
 */
class DesktopCredentialStore : ICredentialStore {

    private val prefs: Preferences = Preferences.userRoot().node("/com/augmentalis/foundation/credentials")
    private val secretKey: SecretKey = loadOrCreateKey()

    override suspend fun store(key: String, value: String) {
        val plaintext = value.toByteArray(Charsets.UTF_8)
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(CIPHER_TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)
        val combined = iv + ciphertext
        prefs.put(key, Base64.getEncoder().encodeToString(combined))
        prefs.flush()
    }

    override suspend fun retrieve(key: String): String? {
        val encoded = prefs.get(key, null) ?: return null
        return try {
            val combined = Base64.getDecoder().decode(encoded)
            if (combined.size < GCM_IV_LENGTH + 1) return null
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            // Key changed or data corrupt — remove stale entry
            prefs.remove(key)
            prefs.flush()
            null
        }
    }

    override suspend fun delete(key: String) {
        prefs.remove(key)
        prefs.flush()
    }

    override suspend fun hasCredential(key: String): Boolean {
        return prefs.get(key, null) != null
    }

    private companion object {
        const val CIPHER_TRANSFORM = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_BITS = 128
        const val KEY_LENGTH = 32 // AES-256

        fun loadOrCreateKey(): SecretKey {
            val keyDir = File(System.getProperty("user.home"), ".avanues")
            val keyFile = File(keyDir, "credential.key")

            if (keyFile.exists()) {
                try {
                    val keyBytes = keyFile.readBytes()
                    if (keyBytes.size == KEY_LENGTH) {
                        return SecretKeySpec(keyBytes, "AES")
                    }
                } catch (_: Exception) {
                    // Corrupt key file — regenerate below
                }
            }

            // Generate new key
            keyDir.mkdirs()
            val keyBytes = ByteArray(KEY_LENGTH).also { SecureRandom().nextBytes(it) }
            keyFile.writeBytes(keyBytes)
            // Restrict permissions on Unix-like systems
            try { keyFile.setReadable(false, false); keyFile.setReadable(true, true) } catch (_: Exception) {}
            try { keyFile.setWritable(false, false); keyFile.setWritable(true, true) } catch (_: Exception) {}
            return SecretKeySpec(keyBytes, "AES")
        }
    }
}
