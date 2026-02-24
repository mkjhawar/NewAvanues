package com.augmentalis.netavanue.capability

import kotlinx.cinterop.*
import platform.CommonCrypto.CC_SHA256
import platform.Foundation.*
import platform.Security.*
import platform.UIKit.UIDevice

/**
 * iOS implementation of [DeviceFingerprint].
 *
 * Fingerprint: SHA-256(identifierForVendor + bundle ID)
 * Key pair: ECDSA P-256 stored in iOS Keychain (survives app reinstall
 * as long as the same developer account signs the app).
 */
actual class DeviceFingerprint actual constructor() {
    private val keychainTag = "com.augmentalis.netavanue.devicekey"

    actual val fingerprint: String
    actual val publicKey: String

    init {
        // Generate fingerprint from identifierForVendor + bundle ID
        val vendorId = UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown"
        val bundleId = NSBundle.mainBundle.bundleIdentifier ?: "com.augmentalis.netavanue"
        val input = "$vendorId:$bundleId"

        // SHA-256 using CommonCrypto
        val inputData = input.encodeToByteArray()
        val hashData = sha256(inputData)
        fingerprint = hashData.joinToString("") { "%02x".format(it) }

        // Load or generate ECDSA key pair from Keychain
        val keyPair = loadOrGenerateKeyPair()
        publicKey = keyPair.first
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun sign(data: ByteArray): String {
        val privKeyRef = loadPrivateKeyFromKeychain() ?: throw IllegalStateException("No signing key found")

        val nsData = data.toNSData()
        val errorPtr = nativeHeap.alloc<ObjCObjectVar<NSError?>>()
        val signatureData = SecKeyCreateSignature(
            privKeyRef,
            kSecKeyAlgorithmECDSASignatureMessageX962SHA256,
            nsData as CFDataRef,
            errorPtr.ptr as kotlinx.cinterop.CPointer<kotlinx.cinterop.ObjCObjectVar<platform.CoreFoundation.CFErrorRef?>>
        )

        return if (signatureData != null) {
            (signatureData as NSData).base64EncodedStringWithOptions(0u)
        } else {
            throw IllegalStateException("Signing failed")
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun loadOrGenerateKeyPair(): Pair<String, Unit> {
        // Try to load existing public key
        val existingPub = loadPublicKeyFromKeychain()
        if (existingPub != null) return existingPub to Unit

        // Generate new key pair
        val attributes = mapOf<Any?, Any?>(
            kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
            kSecAttrKeySizeInBits to 256,
            kSecAttrTokenID to kSecAttrTokenIDSecureEnclave,
            kSecPrivateKeyAttrs to mapOf<Any?, Any?>(
                kSecAttrIsPermanent to true,
                kSecAttrApplicationTag to keychainTag.encodeToByteArray().toNSData(),
            ),
        )

        val errorPtr = nativeHeap.alloc<ObjCObjectVar<NSError?>>()
        val privKey = SecKeyCreateRandomKey(
            attributes as CFDictionaryRef,
            errorPtr.ptr as kotlinx.cinterop.CPointer<kotlinx.cinterop.ObjCObjectVar<platform.CoreFoundation.CFErrorRef?>>
        )

        val pubKey = if (privKey != null) SecKeyCopyPublicKey(privKey) else null
        val pubData = if (pubKey != null) {
            SecKeyCopyExternalRepresentation(pubKey, null) as? NSData
        } else null

        val pubBase64 = pubData?.base64EncodedStringWithOptions(0u) ?: ""
        return pubBase64 to Unit
    }

    private fun loadPublicKeyFromKeychain(): String? {
        // Query for existing key
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassKey,
            kSecAttrApplicationTag to keychainTag.encodeToByteArray().toNSData(),
            kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
            kSecReturnRef to true,
        )

        memScoped {
            val result = alloc<ObjCObjectVar<Any?>>()
            val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr as kotlinx.cinterop.CPointer<kotlinx.cinterop.COpaquePointerVar>)
            if (status == errSecSuccess) {
                val keyRef = result.value as? SecKeyRef
                if (keyRef != null) {
                    val pubKey = SecKeyCopyPublicKey(keyRef)
                    val pubData = if (pubKey != null) {
                        SecKeyCopyExternalRepresentation(pubKey, null) as? NSData
                    } else null
                    return pubData?.base64EncodedStringWithOptions(0u)
                }
            }
        }
        return null
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun loadPrivateKeyFromKeychain(): SecKeyRef? {
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassKey,
            kSecAttrApplicationTag to keychainTag.encodeToByteArray().toNSData(),
            kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
            kSecReturnRef to true,
        )

        memScoped {
            val result = alloc<ObjCObjectVar<Any?>>()
            val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr as kotlinx.cinterop.CPointer<kotlinx.cinterop.COpaquePointerVar>)
            return if (status == errSecSuccess) result.value as? SecKeyRef else null
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun sha256(input: ByteArray): ByteArray {
        val digest = ByteArray(32) // CC_SHA256_DIGEST_LENGTH = 32
        input.usePinned { pinnedInput ->
            digest.usePinned { pinnedDigest ->
                platform.CommonCrypto.CC_SHA256(
                    pinnedInput.addressOf(0),
                    input.size.toUInt(),
                    pinnedDigest.addressOf(0).reinterpret()
                )
            }
        }
        return digest
    }

    private fun ByteArray.toNSData(): NSData {
        if (isEmpty()) return NSData()
        return usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
        }
    }
}
