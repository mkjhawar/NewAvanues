package com.augmentalis.netavanue.capability

import java.io.File
import java.net.NetworkInterface
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Desktop (JVM) implementation of [DeviceFingerprint].
 *
 * Fingerprint: SHA-256(MAC addresses + hostname)
 * Key pair: ECDSA P-256 stored in ~/.avanues/device_key.{pub,priv}
 */
@OptIn(ExperimentalEncodingApi::class)
actual class DeviceFingerprint actual constructor() {
    private val keyDir = File(System.getProperty("user.home"), ".avanues")
    private val pubKeyFile = File(keyDir, "device_key.pub")
    private val privKeyFile = File(keyDir, "device_key.priv")

    actual val fingerprint: String
    actual val publicKey: String
    private val privateKey: PrivateKey

    init {
        // Generate fingerprint from MAC addresses + hostname
        val macAddresses = try {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { !it.isLoopback && it.hardwareAddress != null }
                .map { it.hardwareAddress.joinToString(":") { b -> "%02x".format(b) } }
                .sorted()
                .joinToString("|")
        } catch (_: Exception) { "unknown" }

        val hostname = try {
            java.net.InetAddress.getLocalHost().hostName
        } catch (_: Exception) { "unknown" }

        val input = "$macAddresses:$hostname"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        fingerprint = hash.joinToString("") { "%02x".format(it) }

        // Load or generate ECDSA key pair
        val pair = loadOrGenerateKeyPair()
        publicKey = Base64.encode(pair.public.encoded)
        privateKey = pair.private
    }

    actual fun sign(data: ByteArray): String {
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data)
        return Base64.encode(signature.sign())
    }

    private fun loadOrGenerateKeyPair(): KeyPair {
        if (pubKeyFile.exists() && privKeyFile.exists()) {
            try {
                val factory = KeyFactory.getInstance("EC")
                val pubBytes = Base64.decode(pubKeyFile.readText().trim())
                val privBytes = Base64.decode(privKeyFile.readText().trim())
                val publicKey = factory.generatePublic(X509EncodedKeySpec(pubBytes))
                val privateKey = factory.generatePrivate(PKCS8EncodedKeySpec(privBytes))
                return KeyPair(publicKey, privateKey)
            } catch (_: Exception) {
                // Corrupted files — regenerate
            }
        }

        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        val pair = generator.generateKeyPair()

        keyDir.mkdirs()
        pubKeyFile.writeText(Base64.encode(pair.public.encoded))
        privKeyFile.writeText(Base64.encode(pair.private.encoded))
        // Restrict key file permissions to owner-only
        privKeyFile.setReadable(false, false)
        privKeyFile.setReadable(true, true)
        privKeyFile.setWritable(false, false)
        privKeyFile.setWritable(true, true)

        return pair
    }
}
