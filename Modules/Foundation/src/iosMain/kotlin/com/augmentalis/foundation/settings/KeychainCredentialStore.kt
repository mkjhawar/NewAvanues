/*
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC.
 * All rights reserved.
 */
package com.augmentalis.foundation.settings

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.NSCopyingProtocol
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

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class KeychainCredentialStore(
    private val serviceName: String = "com.augmentalis.foundation.credentials"
) : ICredentialStore {

    override suspend fun store(key: String, value: String) {
        delete(key)
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding)
            ?: throw IllegalArgumentException("Could not encode credential value as UTF-8")
        val query = buildQuery(key).apply {
            @Suppress("UNCHECKED_CAST")
            setObject(data, forKey = kSecValueData as NSCopyingProtocol)
            @Suppress("UNCHECKED_CAST")
            setObject(
                kSecAttrAccessibleWhenUnlockedThisDeviceOnly!!,
                forKey = kSecAttrAccessible as NSCopyingProtocol
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
        val searchQuery = buildQuery(key).apply {
            @Suppress("UNCHECKED_CAST")
            setObject(kCFBooleanTrue!!, forKey = kSecReturnData as NSCopyingProtocol)
            @Suppress("UNCHECKED_CAST")
            setObject(kSecMatchLimitOne!!, forKey = kSecMatchLimit as NSCopyingProtocol)
        }
        @Suppress("UNCHECKED_CAST")
        val status = SecItemCopyMatching(searchQuery as CFDictionaryRef, result.ptr)
        if (status == errSecSuccess) {
            @Suppress("UNCHECKED_CAST")
            val data = result.value as? NSData
            data?.let { NSString.create(data = it, encoding = NSUTF8StringEncoding) as? String }
        } else null
    }

    override suspend fun delete(key: String) {
        @Suppress("UNCHECKED_CAST")
        SecItemDelete(buildQuery(key) as CFDictionaryRef)
    }

    override suspend fun hasCredential(key: String): Boolean = memScoped {
        val result = alloc<CFTypeRefVar>()
        val searchQuery = buildQuery(key).apply {
            @Suppress("UNCHECKED_CAST")
            setObject(kCFBooleanTrue!!, forKey = kSecReturnData as NSCopyingProtocol)
            @Suppress("UNCHECKED_CAST")
            setObject(kSecMatchLimitOne!!, forKey = kSecMatchLimit as NSCopyingProtocol)
        }
        @Suppress("UNCHECKED_CAST")
        val status = SecItemCopyMatching(searchQuery as CFDictionaryRef, result.ptr)
        status == errSecSuccess
    }

    private fun buildQuery(key: String): NSMutableDictionary = NSMutableDictionary().apply {
        @Suppress("UNCHECKED_CAST")
        setObject(kSecClassGenericPassword!!, forKey = kSecClass as NSCopyingProtocol)
        @Suppress("UNCHECKED_CAST")
        setObject(serviceName, forKey = kSecAttrService as NSCopyingProtocol)
        @Suppress("UNCHECKED_CAST")
        setObject(key, forKey = kSecAttrAccount as NSCopyingProtocol)
    }
}
