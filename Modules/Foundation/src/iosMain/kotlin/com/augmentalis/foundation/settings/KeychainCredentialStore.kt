/*
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC.
 * All rights reserved.
 */
package com.augmentalis.foundation.settings

import com.augmentalis.foundation.ICredentialStore
import kotlinx.cinterop.CFBridgingRelease
import kotlinx.cinterop.CFDictionaryRef
import kotlinx.cinterop.CFTypeRefVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFBridgingRelease as CFBridgingReleaseType
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease as FoundationCFBridgingRelease
import platform.Foundation.NSCopying
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class)
class KeychainCredentialStore(
    private val serviceName: String = "com.augmentalis.foundation.credentials"
) : ICredentialStore {

    override suspend fun store(key: String, value: String) {
        delete(key)
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding)
            ?: throw IllegalArgumentException("Could not encode credential value as UTF-8")
        val query = keychainQuery(key).apply {
            setObject(data, forKey = kSecValueData!! as NSCopying)
            setObject(
                kSecAttrAccessibleWhenUnlockedThisDeviceOnly!!,
                forKey = kSecAttrAccessible!! as NSCopying
            )
        }
        @Suppress("UNCHECKED_CAST")
        val status = SecItemAdd(query as CFDictionaryRef, null)
        if (status != errSecSuccess) {
            throw IllegalStateException("Failed to store credential in keychain. Status: $status")
        }
    }

    override suspend fun retrieve(key: String): String? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val searchQuery = keychainQuery(key).apply {
            setObject(kCFBooleanTrue!!, forKey = kSecReturnData!! as NSCopying)
            setObject(kSecMatchLimitOne!!, forKey = kSecMatchLimit!! as NSCopying)
        }
        @Suppress("UNCHECKED_CAST")
        val status = SecItemCopyMatching(searchQuery as CFDictionaryRef, result.ptr)
        if (status == errSecSuccess) {
            val data = FoundationCFBridgingRelease(result.value) as? NSData
            data?.let { NSString.create(data = it, encoding = NSUTF8StringEncoding) as? String }
        } else null
    }

    override suspend fun delete(key: String) {
        @Suppress("UNCHECKED_CAST")
        SecItemDelete(keychainQuery(key) as CFDictionaryRef)
    }

    override suspend fun hasCredential(key: String): Boolean = memScoped {
        val result = alloc<CFTypeRefVar>()
        val searchQuery = keychainQuery(key).apply {
            setObject(kCFBooleanTrue!!, forKey = kSecReturnData!! as NSCopying)
            setObject(kSecMatchLimitOne!!, forKey = kSecMatchLimit!! as NSCopying)
        }
        @Suppress("UNCHECKED_CAST")
        val status = SecItemCopyMatching(searchQuery as CFDictionaryRef, result.ptr)
        if (status == errSecSuccess) {
            FoundationCFBridgingRelease(result.value)
            true
        } else false
    }

    private fun keychainQuery(key: String): NSMutableDictionary = NSMutableDictionary().apply {
        setObject(kSecClassGenericPassword!!, forKey = kSecClass!! as NSCopying)
        setObject(serviceName, forKey = kSecAttrService!! as NSCopying)
        setObject(key, forKey = kSecAttrAccount!! as NSCopying)
    }
}
