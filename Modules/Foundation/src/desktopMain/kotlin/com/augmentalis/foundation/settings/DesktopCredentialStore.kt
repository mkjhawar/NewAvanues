/*
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC.
 * All rights reserved.
 */

package com.augmentalis.foundation.settings

import java.util.Base64
import java.util.prefs.Preferences

/**
 * Desktop (JVM) implementation of [ICredentialStore] using java.util.prefs.Preferences.
 *
 * **Security Note:** This implementation uses Base64 encoding for obfuscation only,
 * not cryptographic encryption. For production deployments requiring stronger credential
 * protection, integrate with the OS-native credential store (Keychain on macOS,
 * Credential Manager on Windows, Secret Service on Linux).
 *
 * Credentials are stored in the user's preferences node:
 * `/com/augmentalis/foundation/credentials`
 */
class DesktopCredentialStore : ICredentialStore {

    private val prefs: Preferences = Preferences.userRoot().node("/com/augmentalis/foundation/credentials")

    override suspend fun store(key: String, value: String) {
        val encoded = Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
        prefs.put(key, encoded)
        prefs.flush()
    }

    override suspend fun retrieve(key: String): String? {
        val encoded = prefs.get(key, null)
        return encoded?.let {
            String(Base64.getDecoder().decode(it), Charsets.UTF_8)
        }
    }

    override suspend fun delete(key: String) {
        prefs.remove(key)
        prefs.flush()
    }

    override suspend fun hasCredential(key: String): Boolean {
        return prefs.get(key, null) != null
    }
}
