/*
 * Copyright (c) 2026 Manoj Jhawar, Aman Jhawar - Intelligent Devices LLC
 * All rights reserved.
 *
 * Created: 2026-02-11
 */

package com.augmentalis.voiceavanue.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure credential store for SFTP passwords and SSH key passphrases.
 * Uses Android EncryptedSharedPreferences with AES256_GCM encryption,
 * with fallback to regular SharedPreferences if hardware-backed keystore
 * is unavailable.
 */
@Singleton
class SftpCredentialStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        try {
            // Attempt to create encrypted preferences with hardware-backed keystore
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences if encryption is unavailable
            Log.w(TAG, "EncryptedSharedPreferences unavailable, falling back to regular prefs", e)
            context.getSharedPreferences(PREFS_NAME_FALLBACK, Context.MODE_PRIVATE)
        }
    }

    /**
     * Store SFTP password securely.
     */
    fun storePassword(password: String) {
        prefs.edit()
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    /**
     * Retrieve stored SFTP password.
     * @return Stored password or empty string if not set
     */
    fun getPassword(): String {
        return prefs.getString(KEY_PASSWORD, "") ?: ""
    }

    /**
     * Store SSH key passphrase securely.
     */
    fun storePassphrase(passphrase: String) {
        prefs.edit()
            .putString(KEY_PASSPHRASE, passphrase)
            .apply()
    }

    /**
     * Retrieve stored SSH key passphrase.
     * @return Stored passphrase or empty string if not set
     */
    fun getPassphrase(): String {
        return prefs.getString(KEY_PASSPHRASE, "") ?: ""
    }

    /**
     * Remove all stored credentials.
     */
    fun clearAll() {
        prefs.edit()
            .remove(KEY_PASSWORD)
            .remove(KEY_PASSPHRASE)
            .apply()
    }

    /**
     * Check if any credentials are stored.
     * @return true if password or passphrase is stored
     */
    fun hasCredentials(): Boolean {
        val hasPassword = prefs.getString(KEY_PASSWORD, "")?.isNotEmpty() == true
        val hasPassphrase = prefs.getString(KEY_PASSPHRASE, "")?.isNotEmpty() == true
        return hasPassword || hasPassphrase
    }

    companion object {
        private const val TAG = "SftpCredentialStore"
        private const val PREFS_NAME = "vos_sftp_credentials"
        private const val PREFS_NAME_FALLBACK = "vos_sftp_credentials_fallback"
        private const val KEY_PASSWORD = "sftp_password"
        private const val KEY_PASSPHRASE = "ssh_passphrase"
    }
}
