package com.augmentalis.magiccode.plugins.security

import com.augmentalis.magiccode.plugins.core.PluginLog

/**
 * Digital signature verifier for plugin packages.
 *
 * Provides cryptographic signature verification to ensure plugin integrity
 * and authenticity. Uses public-key cryptography to verify that plugins
 * have not been tampered with and come from trusted publishers.
 *
 * ## Security Model
 * Plugins are signed by publishers using their private key. This class
 * verifies signatures using the publisher's public key, ensuring:
 * - **Integrity**: Package has not been modified after signing
 * - **Authenticity**: Package was signed by holder of private key
 * - **Non-repudiation**: Publisher cannot deny signing the package
 *
 * ## Supported Algorithms
 * - **RSA-SHA256**: 2048-bit or 4096-bit RSA with SHA-256 hash
 * - **RSA-SHA512**: 2048-bit or 4096-bit RSA with SHA-512 hash
 * - **ECDSA-SHA256**: ECDSA with P-256 curve and SHA-256 hash
 * - **ECDSA-SHA512**: ECDSA with P-384/P-521 curve and SHA-512 hash
 *
 * ## Signature Formats
 * - **External**: Signature in separate `.sig` file
 * - **Embedded**: Signature embedded in plugin manifest/metadata
 *
 * ## Public Key Formats
 * - PEM (Privacy Enhanced Mail) - Base64 encoded with headers
 * - DER (Distinguished Encoding Rules) - Binary format
 *
 * ## Platform-Specific Implementation
 * This is an `expect` class with platform-specific implementations:
 * - **JVM**: Uses Java Cryptography Architecture (JCA)
 * - **Android**: Uses Android KeyStore and Security Provider
 * - **iOS**: Uses Security framework and CommonCrypto
 * - **Native**: Uses OpenSSL or platform crypto libraries
 *
 * ## Usage Example
 * ```kotlin
 * val verifier = SignatureVerifier()
 * val result = verifier.verify(
 *     packagePath = "/plugins/myplugin.jar",
 *     signaturePath = "/plugins/myplugin.jar.sig",
 *     publicKeyPath = "/keys/publisher.pub",
 *     algorithm = SignatureAlgorithm.RSA_SHA256
 * )
 *
 * when (result) {
 *     is VerificationResult.Valid -> println("Signature valid")
 *     is VerificationResult.Invalid -> println("Signature invalid: ${result.reason}")
 * }
 * ```
 *
 * ## Security Warning
 * Always use signatures in production environments. Never load unsigned
 * plugins from untrusted sources, as they may contain malicious code.
 *
 * @see TrustStore
 * @see SignatureAlgorithm
 * @see VerificationResult
 */
expect class SignatureVerifier() {
    /**
     * Verify digital signature of a plugin package.
     *
     * Reads the plugin package file, reads the external signature file,
     * and verifies the signature using the specified public key.
     *
     * ## Verification Process
     * 1. Loads public key from file
     * 2. Computes hash of package file
     * 3. Decrypts signature using public key
     * 4. Compares decrypted hash with computed hash
     *
     * ## Security Note
     * Verification fails if:
     * - Signature doesn't match package contents
     * - Public key is invalid or malformed
     * - Algorithm mismatch between signature and key
     * - Files cannot be read
     *
     * @param packagePath Absolute path to plugin package file (e.g., `.jar`, `.zip`)
     * @param signaturePath Absolute path to signature file (typically `.sig` extension)
     * @param publicKeyPath Absolute path to public key file (PEM or DER format)
     * @param algorithm Signature algorithm to use (default: RSA-SHA256)
     *
     * @return [VerificationResult.Valid] if signature is valid,
     *         [VerificationResult.Invalid] if signature is invalid or verification fails
     *
     * @see verifyEmbedded
     */
    fun verify(
        packagePath: String,
        signaturePath: String,
        publicKeyPath: String,
        algorithm: SignatureAlgorithm = SignatureAlgorithm.RSA_SHA256
    ): VerificationResult

    /**
     * Verify signature using embedded signature from package metadata.
     *
     * Verifies a signature that is embedded within the plugin package
     * manifest or metadata, rather than stored in a separate file.
     * This is common for JAR files with signatures in META-INF or
     * zip files with signed metadata.
     *
     * ## Use Cases
     * - Plugin packages with self-contained signatures
     * - Single-file distribution (no separate `.sig` file)
     * - Manifest-based signing (similar to JAR signing)
     *
     * ## Signature Format
     * The embedded signature must be Base64-encoded. It is typically
     * extracted from plugin manifest entries like:
     * - `X-Plugin-Signature: <base64-signature>`
     * - JAR manifest: `SHA256-Digest`
     *
     * @param packagePath Absolute path to plugin package file
     * @param embeddedSignature Base64-encoded signature string extracted from metadata
     * @param publicKeyPath Absolute path to public key file (PEM or DER format)
     * @param algorithm Signature algorithm to use (default: RSA-SHA256)
     *
     * @return [VerificationResult.Valid] if signature is valid,
     *         [VerificationResult.Invalid] if signature is invalid or verification fails
     *
     * @see verify
     */
    fun verifyEmbedded(
        packagePath: String,
        embeddedSignature: String,
        publicKeyPath: String,
        algorithm: SignatureAlgorithm = SignatureAlgorithm.RSA_SHA256
    ): VerificationResult

    /**
     * Load public key from file.
     *
     * Loads and parses a public key from PEM or DER format file.
     * This is a utility method for loading keys independently of
     * signature verification.
     *
     * ## Supported Formats
     * - **PEM**: Text-based Base64 encoding with headers
     *   ```
     *   -----BEGIN PUBLIC KEY-----
     *   <base64-encoded-key>
     *   -----END PUBLIC KEY-----
     *   ```
     * - **DER**: Binary ASN.1 encoding
     *
     * ## Algorithm Compatibility
     * The public key must match the specified algorithm:
     * - RSA algorithms require RSA public key
     * - ECDSA algorithms require EC public key with matching curve
     *
     * @param publicKeyPath Absolute path to public key file
     * @param algorithm Signature algorithm (determines expected key type)
     *
     * @return Platform-specific public key object
     *         (e.g., `java.security.PublicKey` on JVM)
     *
     * @throws IllegalArgumentException if key format is invalid or algorithm mismatch
     * @throws IOException if file cannot be read
     */
    fun loadPublicKey(publicKeyPath: String, algorithm: SignatureAlgorithm): Any
}

/**
 * Supported signature algorithms for plugin verification.
 *
 * Defines the cryptographic algorithms that can be used to sign and
 * verify plugin packages. Each algorithm combines a hash function
 * with a public-key signature scheme.
 *
 * ## Algorithm Selection
 * - **RSA-SHA256**: Standard choice, widely supported, good security
 * - **RSA-SHA512**: Higher security, slightly larger signatures
 * - **ECDSA-SHA256**: Smaller signatures, faster verification, modern
 * - **ECDSA-SHA512**: Highest security, very small signatures
 *
 * ## Key Size Recommendations
 * - **RSA**: Minimum 2048 bits, recommended 4096 bits
 * - **ECDSA**: P-256 curve (256 bits) or P-384 curve (384 bits)
 *
 * @property algorithmName Platform-specific algorithm identifier
 *                         (e.g., for Java Cryptography Architecture)
 */
enum class SignatureAlgorithm(val algorithmName: String) {
    /** RSA signature with SHA-256 hash (2048+ bit keys) */
    RSA_SHA256("SHA256withRSA"),

    /** RSA signature with SHA-512 hash (2048+ bit keys, higher security) */
    RSA_SHA512("SHA512withRSA"),

    /** ECDSA signature with SHA-256 hash (P-256 curve, smaller signatures) */
    ECDSA_SHA256("SHA256withECDSA"),

    /** ECDSA signature with SHA-512 hash (P-384/P-521 curve, highest security) */
    ECDSA_SHA512("SHA512withECDSA")
}

/**
 * Result of signature verification operation.
 *
 * Sealed class representing the outcome of verifying a plugin signature.
 * Use pattern matching to handle success and failure cases.
 *
 * ## Usage Example
 * ```kotlin
 * when (val result = verifier.verify(...)) {
 *     is VerificationResult.Valid -> {
 *         println("Verified with ${result.algorithm}")
 *         loadPlugin()
 *     }
 *     is VerificationResult.Invalid -> {
 *         println("Verification failed: ${result.reason}")
 *         result.exception?.printStackTrace()
 *         rejectPlugin()
 *     }
 * }
 * ```
 *
 * @see SignatureVerifier.verify
 * @see SignatureVerifier.verifyEmbedded
 */
sealed class VerificationResult {
    /**
     * Signature verification succeeded.
     *
     * The signature is cryptographically valid and the package has not
     * been tampered with. The plugin can be safely loaded.
     *
     * ## Security Note
     * A valid signature means the package matches the signature, but
     * does NOT necessarily mean the publisher is trusted. Always check
     * the publisher's identity using a [TrustStore] before loading.
     *
     * @property algorithm The signature algorithm that was successfully verified
     */
    data class Valid(val algorithm: SignatureAlgorithm) : VerificationResult()

    /**
     * Signature verification failed.
     *
     * The signature is invalid, the package may have been tampered with,
     * or verification could not be completed due to an error.
     *
     * ## Security Warning
     * NEVER load a plugin with an invalid signature. It may have been
     * modified by an attacker or corrupted during transmission.
     *
     * ## Common Failure Reasons
     * - "Signature mismatch": Package was modified after signing
     * - "Invalid public key": Public key file is malformed
     * - "Algorithm mismatch": Wrong algorithm specified for key type
     * - "File not found": Package or signature file missing
     * - "Signature format error": Signature file is corrupted
     *
     * @property reason Human-readable explanation of verification failure
     * @property exception Optional exception that caused the failure (for debugging)
     */
    data class Invalid(val reason: String, val exception: Throwable? = null) : VerificationResult()
}

/**
 * Trust store for managing trusted plugin publisher public keys.
 *
 * Maintains a registry of trusted publishers and their public keys.
 * Used to ensure that only plugins from known, trusted sources are loaded.
 *
 * ## Security Model
 * The trust store implements a "trust on first use" or "explicit trust"
 * model. Publishers must be explicitly added to the trust store before
 * their plugins can be loaded. This prevents loading plugins from
 * unknown or untrusted sources.
 *
 * ## Publisher Identity
 * Publishers are identified by reverse domain notation (e.g.,
 * "com.example.publisher"). This provides a globally unique namespace
 * similar to Java package names.
 *
 * ## Use Cases
 * - Pre-populate with official publisher keys on installation
 * - Allow users to add trusted publishers manually
 * - Enterprise: Distribute organization's trust store
 * - Verify publisher identity before loading plugins
 *
 * ## Thread Safety
 * This class is NOT thread-safe. Wrap in synchronized block or use
 * from a single thread if concurrent access is needed.
 *
 * ## Usage Example
 * ```kotlin
 * val trustStore = TrustStore()
 *
 * // Add official publisher
 * trustStore.addTrustedKey(
 *     publisherId = "com.magiccode.official",
 *     publicKeyPath = "/keys/official.pub"
 * )
 *
 * // Verify plugin publisher before loading
 * if (trustStore.isTrusted("com.example.plugin")) {
 *     val keyPath = trustStore.getPublicKeyPath("com.example.plugin")
 *     // Verify signature with key...
 * } else {
 *     println("Untrusted publisher, rejecting plugin")
 * }
 * ```
 *
 * @see SignatureVerifier
 */
class TrustStore {
    private val trustedKeys = mutableMapOf<String, String>()

    companion object {
        private const val TAG = "TrustStore"
    }

    /**
     * Add a trusted publisher to the trust store.
     *
     * Registers a publisher's public key, allowing plugins from this
     * publisher to be verified and loaded.
     *
     * ## Publisher ID Format
     * Use reverse domain notation (e.g., "com.company.team").
     * This ensures global uniqueness and prevents conflicts.
     *
     * ## Security Note
     * Only add publishers you trust. Adding a malicious publisher's
     * key allows them to sign and distribute harmful plugins.
     *
     * @param publisherId Publisher identifier in reverse domain notation
     * @param publicKeyPath Absolute path to publisher's public key file
     *
     * @see removeTrustedKey
     * @see isTrusted
     */
    fun addTrustedKey(publisherId: String, publicKeyPath: String) {
        trustedKeys[publisherId] = publicKeyPath
        PluginLog.i(TAG, "Added trusted key for publisher: $publisherId")
    }

    /**
     * Remove a trusted publisher.
     *
     * @param publisherId Publisher identifier
     * @return true if removed, false if not found
     */
    fun removeTrustedKey(publisherId: String): Boolean {
        val removed = trustedKeys.remove(publisherId) != null
        if (removed) {
            PluginLog.i(TAG, "Removed trusted key for publisher: $publisherId")
        }
        return removed
    }

    /**
     * Get public key path for a trusted publisher.
     *
     * @param publisherId Publisher identifier
     * @return Public key path, or null if not trusted
     */
    fun getPublicKeyPath(publisherId: String): String? {
        return trustedKeys[publisherId]
    }

    /**
     * Check if publisher is trusted.
     *
     * @param publisherId Publisher identifier
     * @return true if trusted
     */
    fun isTrusted(publisherId: String): Boolean {
        return trustedKeys.containsKey(publisherId)
    }

    /**
     * Get all trusted publishers.
     *
     * @return Set of publisher IDs
     */
    fun getTrustedPublishers(): Set<String> {
        return trustedKeys.keys.toSet()
    }

    /**
     * Clear all trusted keys.
     */
    fun clear() {
        val count = trustedKeys.size
        trustedKeys.clear()
        PluginLog.i(TAG, "Cleared $count trusted keys")
    }
}
