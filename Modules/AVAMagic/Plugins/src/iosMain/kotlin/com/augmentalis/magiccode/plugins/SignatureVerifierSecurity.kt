package com.augmentalis.avacode.plugins

import com.augmentalis.avacode.plugins.PluginLog
import platform.Foundation.*
import platform.Security.*
import kotlinx.cinterop.*

/**
 * iOS implementation of SignatureVerifier using Security framework.
 */
@OptIn(ExperimentalForeignApi::class)
actual class SignatureVerifier {
    companion object {
        private const val TAG = "SignatureVerifier"
    }

    actual fun verify(
        packagePath: String,
        signaturePath: String,
        publicKeyPath: String,
        algorithm: SignatureAlgorithm
    ): VerificationResult {
        try {
            // Read package data
            val packageData = NSData.dataWithContentsOfFile(packagePath)
                ?: return VerificationResult.Invalid("Failed to read package file")

            // Read signature
            val signatureData = NSData.dataWithContentsOfFile(signaturePath)
                ?: return VerificationResult.Invalid("Failed to read signature file")

            // Load public key
            val publicKey = loadPublicKey(publicKeyPath, algorithm) as SecKeyRef

            // Verify signature using SecKeyVerifySignature
            val algorithmRef = getSecKeyAlgorithm(algorithm)

            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val isValid = SecKeyVerifySignature(
                    publicKey,
                    algorithmRef,
                    packageData,
                    signatureData,
                    error.ptr
                )

                if (isValid) {
                    PluginLog.i(TAG, "Signature verification succeeded for: $packagePath")
                    return VerificationResult.Valid(algorithm)
                } else {
                    val errorMsg = error.value?.localizedDescription ?: "Unknown error"
                    PluginLog.w(TAG, "Signature verification failed: $errorMsg")
                    return VerificationResult.Invalid("Signature verification failed: $errorMsg")
                }
            }
        } catch (e: Exception) {
            PluginLog.e(TAG, "Signature verification error", e)
            return VerificationResult.Invalid("Verification failed: ${e.message}", e)
        }
    }

    actual fun verifyEmbedded(
        packagePath: String,
        embeddedSignature: String,
        publicKeyPath: String,
        algorithm: SignatureAlgorithm
    ): VerificationResult {
        try {
            // Read package data
            val packageData = NSData.dataWithContentsOfFile(packagePath)
                ?: return VerificationResult.Invalid("Failed to read package file")

            // Decode Base64 signature
            val signatureData = NSData.create(base64EncodedString = embeddedSignature, options = 0u)
                ?: return VerificationResult.Invalid("Failed to decode embedded signature")

            // Load public key
            val publicKey = loadPublicKey(publicKeyPath, algorithm) as SecKeyRef

            // Verify signature
            val algorithmRef = getSecKeyAlgorithm(algorithm)

            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val isValid = SecKeyVerifySignature(
                    publicKey,
                    algorithmRef,
                    packageData,
                    signatureData,
                    error.ptr
                )

                if (isValid) {
                    PluginLog.i(TAG, "Embedded signature verification succeeded for: $packagePath")
                    return VerificationResult.Valid(algorithm)
                } else {
                    val errorMsg = error.value?.localizedDescription ?: "Unknown error"
                    PluginLog.w(TAG, "Embedded signature verification failed: $errorMsg")
                    return VerificationResult.Invalid("Embedded signature verification failed: $errorMsg")
                }
            }
        } catch (e: Exception) {
            PluginLog.e(TAG, "Embedded signature verification error", e)
            return VerificationResult.Invalid("Verification failed: ${e.message}", e)
        }
    }

    actual fun loadPublicKey(publicKeyPath: String, algorithm: SignatureAlgorithm): Any {
        try {
            // Read public key data
            val keyData = NSData.dataWithContentsOfFile(publicKeyPath)
                ?: throw IllegalArgumentException("Failed to read public key file")

            // Determine key type
            val keyType = when (algorithm) {
                SignatureAlgorithm.RSA_SHA256, SignatureAlgorithm.RSA_SHA512 -> kSecAttrKeyTypeRSA
                SignatureAlgorithm.ECDSA_SHA256, SignatureAlgorithm.ECDSA_SHA512 -> kSecAttrKeyTypeECSECPrimeRandom
            }

            // Create key attributes
            val attributes = mapOf(
                kSecAttrKeyType to keyType,
                kSecAttrKeyClass to kSecAttrKeyClassPublic
            )

            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val publicKey = SecKeyCreateWithData(
                    keyData,
                    attributes as CFDictionaryRef,
                    error.ptr
                )

                if (publicKey == null) {
                    val errorMsg = error.value?.localizedDescription ?: "Unknown error"
                    throw IllegalArgumentException("Failed to create public key: $errorMsg")
                }

                return publicKey
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to load public key: ${e.message}", e)
        }
    }

    /**
     * Get SecKeyAlgorithm for signature verification.
     */
    private fun getSecKeyAlgorithm(algorithm: SignatureAlgorithm): SecKeyAlgorithm {
        return when (algorithm) {
            SignatureAlgorithm.RSA_SHA256 -> kSecKeyAlgorithmRSASignatureMessagePKCS1v15SHA256
            SignatureAlgorithm.RSA_SHA512 -> kSecKeyAlgorithmRSASignatureMessagePKCS1v15SHA512
            SignatureAlgorithm.ECDSA_SHA256 -> kSecKeyAlgorithmECDSASignatureMessageX962SHA256
            SignatureAlgorithm.ECDSA_SHA512 -> kSecKeyAlgorithmECDSASignatureMessageX962SHA512
        }
    }
}
