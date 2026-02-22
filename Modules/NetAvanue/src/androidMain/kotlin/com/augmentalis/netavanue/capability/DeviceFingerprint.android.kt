package com.augmentalis.netavanue.capability

import android.content.Context
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.*
import java.security.spec.ECGenParameterSpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Android implementation of [DeviceFingerprint].
 *
 * Fingerprint: SHA-256(ANDROID_ID + package name)
 * Key pair: ECDSA P-256 stored in Android Keystore (survives app reinstall,
 * wiped only on factory reset).
 *
 * Note: Ed25519 is not supported in Android Keystore before API 33.
 * We use ECDSA P-256 (secp256r1) as a compatible alternative. The server
 * can verify with any public key format — it stores the base64 public key.
 */
actual class DeviceFingerprint {
    private var _fingerprint: String = ""
    private var _publicKey: String = ""
    private var keyPair: KeyPair? = null

    actual val fingerprint: String get() = _fingerprint
    actual val publicKey: String get() = _publicKey

    /** Must be called with application context before use. */
    @OptIn(ExperimentalEncodingApi::class)
    fun initialize(context: Context) {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        val packageName = context.packageName
        val input = "$androidId:$packageName"

        // SHA-256 fingerprint
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        _fingerprint = hash.joinToString("") { "%02x".format(it) }

        // Load or generate ECDSA key pair from Android Keystore
        val alias = "netavanue_device_key"
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)

        if (ks.containsAlias(alias)) {
            val entry = ks.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
            keyPair = entry?.let { KeyPair(it.certificate.publicKey, it.privateKey) }
        }

        if (keyPair == null) {
            val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
            val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
            generator.initialize(spec)
            keyPair = generator.generateKeyPair()
        }

        _publicKey = Base64.encode(keyPair!!.public.encoded)
    }

    @OptIn(ExperimentalEncodingApi::class)
    actual fun sign(data: ByteArray): String {
        val privateKey = keyPair?.private ?: throw IllegalStateException("DeviceFingerprint not initialized")
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data)
        return Base64.encode(signature.sign())
    }
}
