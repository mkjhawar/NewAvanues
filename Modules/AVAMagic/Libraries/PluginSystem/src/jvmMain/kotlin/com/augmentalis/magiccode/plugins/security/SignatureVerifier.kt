package com.augmentalis.avacode.plugins.security

import com.augmentalis.avacode.plugins.core.PluginLog
import java.io.File
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * JVM implementation of SignatureVerifier using java.security.
 */
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
            val packageData = File(packagePath).readBytes()

            // Read signature
            val signatureBytes = File(signaturePath).readBytes()

            // Load public key
            val publicKey = loadPublicKey(publicKeyPath, algorithm) as PublicKey

            // Verify signature
            val signature = Signature.getInstance(algorithm.algorithmName)
            signature.initVerify(publicKey)
            signature.update(packageData)

            val isValid = signature.verify(signatureBytes)

            return if (isValid) {
                PluginLog.i(TAG, "Signature verification succeeded for: $packagePath")
                VerificationResult.Valid(algorithm)
            } else {
                PluginLog.w(TAG, "Signature verification failed for: $packagePath")
                VerificationResult.Invalid("Signature does not match package")
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
            val packageData = File(packagePath).readBytes()

            // Decode Base64 signature
            val signatureBytes = Base64.getDecoder().decode(embeddedSignature)

            // Load public key
            val publicKey = loadPublicKey(publicKeyPath, algorithm) as PublicKey

            // Verify signature
            val signature = Signature.getInstance(algorithm.algorithmName)
            signature.initVerify(publicKey)
            signature.update(packageData)

            val isValid = signature.verify(signatureBytes)

            return if (isValid) {
                PluginLog.i(TAG, "Embedded signature verification succeeded for: $packagePath")
                VerificationResult.Valid(algorithm)
            } else {
                PluginLog.w(TAG, "Embedded signature verification failed for: $packagePath")
                VerificationResult.Invalid("Embedded signature does not match package")
            }
        } catch (e: Exception) {
            PluginLog.e(TAG, "Embedded signature verification error", e)
            return VerificationResult.Invalid("Verification failed: ${e.message}", e)
        }
    }

    actual fun loadPublicKey(publicKeyPath: String, algorithm: SignatureAlgorithm): Any {
        try {
            val keyBytes = File(publicKeyPath).readBytes()

            // Determine key algorithm (RSA or EC)
            val keyAlgorithm = when (algorithm) {
                SignatureAlgorithm.RSA_SHA256, SignatureAlgorithm.RSA_SHA512 -> "RSA"
                SignatureAlgorithm.ECDSA_SHA256, SignatureAlgorithm.ECDSA_SHA512 -> "EC"
            }

            // Parse public key (assumes X.509 DER format)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance(keyAlgorithm)
            return keyFactory.generatePublic(keySpec)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to load public key: ${e.message}", e)
        }
    }
}
