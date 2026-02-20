/**
 * IosVSMCodec.kt - iOS VSM encryption/decryption via CommonCrypto
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 *
 * iOS implementation of the VSM codec using Apple's CommonCrypto framework.
 * Produces files byte-identical to the jvmMain VSMCodec — same algorithms,
 * same constants, same block pipeline (XOR → shuffle → AES-CTR).
 *
 * Uses:
 *   CCKeyDerivationPBKDF (PBKDF2-HMAC-SHA256) for key derivation
 *   CCCryptorCreateWithMode (AES-256-CTR) for block encryption
 *   CC_SHA256 for file hash, CC_SHA512 for scramble pattern, CC_MD5 for nonce
 */
package com.augmentalis.speechrecognition.whisper.vsm

import com.augmentalis.speechrecognition.logError
import com.augmentalis.speechrecognition.logInfo
import com.augmentalis.speechrecognition.native.crypto.*
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.*
import kotlin.random.Random as KRandom

@OptIn(ExperimentalForeignApi::class)
class IosVSMCodec {

    companion object {
        private const val TAG = "IosVSMCodec"
    }

    /**
     * Encrypt a raw model file (.bin) to VSM format (.vsm).
     */
    fun encryptFile(
        inputPath: String,
        outputPath: String,
        metadata: Map<String, String> = emptyMap()
    ): Boolean {
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(inputPath)) return false

        return try {
            val inputData = NSData.dataWithContentsOfFile(inputPath) ?: return false
            val originalSize = inputData.length.toLong()
            if (originalSize == 0L) return false

            val timestamp = NSDate().timeIntervalSince1970.toLong() * 1000 // ms since epoch

            // Compute SHA-256 hash of original file
            val originalBytes = inputData.toByteArray()
            val fileHash = sha256Truncated(originalBytes)

            // Derive encryption key
            val key = deriveKey(fileHash, timestamp)

            // Calculate block count
            val blockCount = ((originalSize + VSMFormat.BLOCK_SIZE - 1) / VSMFormat.BLOCK_SIZE).toInt()

            // Serialize metadata
            val metadataBytes = serializeMetadata(metadata)

            // Build header
            val headerBytes = buildHeader(
                originalSize, originalSize, blockCount,
                fileHash, timestamp, VSMFormat.FLAG_ENCRYPTED
            )

            // Create output directory
            val outputUrl = NSURL.fileURLWithPath(outputPath)
            val parentDir = outputUrl.URLByDeletingLastPathComponent?.path
            if (parentDir != null && !fileManager.fileExistsAtPath(parentDir)) {
                fileManager.createDirectoryAtPath(parentDir, withIntermediateDirectories = true, attributes = null, error = null)
            }

            // Write output
            val outputHandle = NSFileHandle.fileHandleForWritingAtPath(outputPath)
                ?: run {
                    fileManager.createFileAtPath(outputPath, contents = null, attributes = null)
                    NSFileHandle.fileHandleForWritingAtPath(outputPath) ?: return false
                }

            outputHandle.writeData(headerBytes.toNSData())

            // Write metadata length (4 bytes LE) + metadata
            outputHandle.writeData(intToLEBytes(metadataBytes.size).toNSData())
            if (metadataBytes.isNotEmpty()) {
                outputHandle.writeData(metadataBytes.toNSData())
            }

            // Encrypt blocks
            for (i in 0 until blockCount) {
                val start = i * VSMFormat.BLOCK_SIZE
                val end = minOf(start + VSMFormat.BLOCK_SIZE, originalSize.toInt())
                val blockData = originalBytes.copyOfRange(start, end)

                // Pad last block
                val paddedBlock = if (blockData.size < VSMFormat.BLOCK_SIZE) {
                    ByteArray(VSMFormat.BLOCK_SIZE).also { padded ->
                        blockData.copyInto(padded)
                    }
                } else {
                    blockData
                }

                val encrypted = encryptBlock(paddedBlock, i, key)
                outputHandle.writeData(encrypted.toNSData())
            }

            outputHandle.closeFile()
            logInfo(TAG, "Encrypted $inputPath -> $outputPath")
            true
        } catch (e: Exception) {
            logError(TAG, "Encryption failed: ${e.message}")
            try { fileManager.removeItemAtPath(outputPath, error = null) } catch (_: Exception) {}
            false
        }
    }

    /**
     * Decrypt a VSM file to a temporary file for loading.
     * Returns the temp file path, or null on failure.
     */
    fun decryptToTempFile(vsmPath: String, tempDir: String): String? {
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(vsmPath)) return null

        // Ensure temp dir exists
        if (!fileManager.fileExistsAtPath(tempDir)) {
            fileManager.createDirectoryAtPath(tempDir, withIntermediateDirectories = true, attributes = null, error = null)
        }

        val tempPath = "$tempDir/vsm_${NSDate().timeIntervalSince1970.toLong()}.bin"

        return try {
            val vsmData = NSData.dataWithContentsOfFile(vsmPath) ?: return null
            val vsmBytes = vsmData.toByteArray()

            if (vsmBytes.size < VSMFormat.HEADER_SIZE) return null

            // Parse header
            val header = parseHeader(vsmBytes) ?: return null
            if (header.magic != VSMFormat.MAGIC) return null
            if (!header.isEncrypted()) return null

            // Skip header + metadata
            var offset = VSMFormat.HEADER_SIZE
            val metaLen = leBytesToInt(vsmBytes, offset)
            offset += 4 + metaLen

            // Derive key
            val key = deriveKey(header.fileHash, header.timestamp)

            // Decrypt blocks
            val decryptedData = ByteArray(header.originalSize.toInt())
            var bytesWritten = 0

            for (i in 0 until header.blockCount) {
                val blockStart = offset + (i * VSMFormat.BLOCK_SIZE)
                val blockEnd = minOf(blockStart + VSMFormat.BLOCK_SIZE, vsmBytes.size)
                val blockData = vsmBytes.copyOfRange(blockStart, blockEnd)

                // Pad if needed
                val block = if (blockData.size < VSMFormat.BLOCK_SIZE) {
                    ByteArray(VSMFormat.BLOCK_SIZE).also { blockData.copyInto(it) }
                } else {
                    blockData
                }

                val decrypted = decryptBlock(block, i, key)

                // Only copy up to originalSize
                val remaining = header.originalSize.toInt() - bytesWritten
                val copyLen = minOf(remaining, decrypted.size)
                decrypted.copyInto(decryptedData, bytesWritten, 0, copyLen)
                bytesWritten += copyLen
            }

            // Verify hash
            val decryptedHash = sha256Truncated(decryptedData)
            if (!decryptedHash.contentEquals(header.fileHash)) {
                logError(TAG, "Hash verification failed after decryption")
                return null
            }

            // Write to temp file
            val nsData = decryptedData.toNSData()
            if (!nsData.writeToFile(tempPath, atomically = true)) {
                logError(TAG, "Failed to write decrypted temp file")
                return null
            }

            logInfo(TAG, "Decrypted $vsmPath -> $tempPath (${bytesWritten / (1024 * 1024)}MB)")
            tempPath
        } catch (e: Exception) {
            logError(TAG, "Decryption failed: ${e.message}")
            try { fileManager.removeItemAtPath(tempPath, error = null) } catch (_: Exception) {}
            null
        }
    }

    // ========================================================================
    // Key Derivation (PBKDF2-HMAC-SHA256 via CommonCrypto)
    // ========================================================================

    private fun deriveKey(fileHash: ByteArray, timestamp: Long): ByteArray {
        val keyMaterial = VSMFormat.MASTER_SEED + fileHash.copyOf(16) + longToLEBytes(timestamp)
        val salt = VSMFormat.SALT.toByteArray(Charsets.UTF_8)
        val derivedKey = ByteArray(32) // 256 bits

        // Convert keyMaterial to ISO-8859-1 string (1:1 byte-to-char mapping) to match JVM behavior
        val password = keyMaterial.map { (it.toInt() and 0xFF).toChar() }.toCharArray()
        val passwordString = String(password)
        val passwordBytes = passwordString.encodeToByteArray()

        memScoped {
            passwordBytes.usePinned { pinnedPwd ->
                salt.usePinned { pinnedSalt ->
                    derivedKey.usePinned { pinnedKey ->
                        CCKeyDerivationPBKDF(
                            kCCPBKDF2,
                            pinnedPwd.addressOf(0).reinterpret(),
                            passwordBytes.size.convert(),
                            pinnedSalt.addressOf(0).reinterpret(),
                            salt.size.convert(),
                            kCCPRFHmacAlgSHA256,
                            VSMFormat.PBKDF2_ITERATIONS.convert(),
                            pinnedKey.addressOf(0).reinterpret(),
                            derivedKey.size.convert()
                        )
                    }
                }
            }
        }

        return derivedKey
    }

    // ========================================================================
    // Block Encryption / Decryption
    // ========================================================================

    private fun encryptBlock(data: ByteArray, blockIndex: Int, key: ByteArray): ByteArray {
        val pattern = deriveScramblePattern(key, blockIndex)
        val scrambled = xorData(data, pattern)
        val shuffled = fisherYatesShuffle(scrambled, blockIndex, key)
        return aesCTR(shuffled, key, deriveNonce(key, blockIndex))
    }

    private fun decryptBlock(data: ByteArray, blockIndex: Int, key: ByteArray): ByteArray {
        val decrypted = aesCTR(data, key, deriveNonce(key, blockIndex))
        val unshuffled = fisherYatesUnshuffle(decrypted, blockIndex, key)
        val pattern = deriveScramblePattern(key, blockIndex)
        return xorData(unshuffled, pattern)
    }

    // ========================================================================
    // AES-256-CTR via CommonCrypto
    // ========================================================================

    private fun aesCTR(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val result = ByteArray(data.size)

        memScoped {
            val cryptorRef = alloc<CCCryptorRefVar>()

            key.usePinned { pinnedKey ->
                iv.usePinned { pinnedIV ->
                    val status = CCCryptorCreateWithMode(
                        kCCEncrypt, // CTR mode: encrypt == decrypt
                        kCCModeCTR,
                        kCCAlgorithmAES,
                        ccNoPadding,
                        pinnedIV.addressOf(0),
                        pinnedKey.addressOf(0),
                        key.size.convert(),
                        null, 0u,
                        0, // numRounds (0 = default)
                        kCCModeOptionCTR_BE.convert(), // big-endian counter (matches Java)
                        cryptorRef.ptr
                    )
                    if (status != kCCSuccess.toInt()) {
                        logError(TAG, "CCCryptorCreateWithMode failed: $status")
                        return data // fallback: return unchanged
                    }
                }
            }

            val dataOutMoved = alloc<size_tVar>()
            data.usePinned { pinnedData ->
                result.usePinned { pinnedResult ->
                    CCCryptorUpdate(
                        cryptorRef.value,
                        pinnedData.addressOf(0),
                        data.size.convert(),
                        pinnedResult.addressOf(0),
                        result.size.convert(),
                        dataOutMoved.ptr
                    )
                }
            }

            CCCryptorRelease(cryptorRef.value)
        }

        return result
    }

    // ========================================================================
    // Hash Functions via CommonCrypto
    // ========================================================================

    /** SHA-256 hash truncated to first 16 bytes (matches JVM VSMCodec) */
    private fun sha256Truncated(data: ByteArray): ByteArray {
        val hash = ByteArray(CC_SHA256_DIGEST_LENGTH)
        data.usePinned { pinnedData ->
            hash.usePinned { pinnedHash ->
                CC_SHA256(pinnedData.addressOf(0), data.size.convert(), pinnedHash.addressOf(0).reinterpret())
            }
        }
        return hash.copyOf(16)
    }

    /** SHA-512 hash for scramble pattern derivation */
    private fun sha512(data: ByteArray): ByteArray {
        val hash = ByteArray(CC_SHA512_DIGEST_LENGTH)
        data.usePinned { pinnedData ->
            hash.usePinned { pinnedHash ->
                CC_SHA512(pinnedData.addressOf(0), data.size.convert(), pinnedHash.addressOf(0).reinterpret())
            }
        }
        return hash
    }

    /** MD5 hash for per-block nonce derivation */
    private fun md5(data: ByteArray): ByteArray {
        val hash = ByteArray(CC_MD5_DIGEST_LENGTH)
        data.usePinned { pinnedData ->
            hash.usePinned { pinnedHash ->
                CC_MD5(pinnedData.addressOf(0), data.size.convert(), pinnedHash.addressOf(0).reinterpret())
            }
        }
        return hash
    }

    // ========================================================================
    // Crypto Primitives (pure Kotlin — matches JVM exactly)
    // ========================================================================

    private fun deriveScramblePattern(key: ByteArray, blockIndex: Int): ByteArray {
        val seed = key + intToLEBytes(blockIndex)
        val pattern = sha512(seed) // 64 bytes

        val repeats = (VSMFormat.BLOCK_SIZE / 64) + 1
        val fullPattern = ByteArray(repeats * 64)
        for (i in 0 until repeats) {
            pattern.copyInto(fullPattern, i * 64, 0, 64)
        }
        return fullPattern.copyOf(VSMFormat.BLOCK_SIZE)
    }

    private fun deriveNonce(key: ByteArray, blockIndex: Int): ByteArray {
        val nonceMaterial = key.copyOf(8) + intToLEBytes(blockIndex)
        return md5(nonceMaterial).copyOf(16)
    }

    private fun xorData(data: ByteArray, pattern: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        for (i in data.indices) {
            result[i] = (data[i].toInt() xor pattern[i % pattern.size].toInt()).toByte()
        }
        return result
    }

    private fun fisherYatesShuffle(data: ByteArray, blockIndex: Int, key: ByteArray): ByteArray {
        val permutation = buildPermutation(data.size, blockIndex, key)
        val result = ByteArray(data.size)
        for (i in permutation.indices) {
            result[permutation[i]] = data[i]
        }
        return result
    }

    private fun fisherYatesUnshuffle(data: ByteArray, blockIndex: Int, key: ByteArray): ByteArray {
        val permutation = buildPermutation(data.size, blockIndex, key)
        val result = ByteArray(data.size)
        for (i in permutation.indices) {
            result[i] = data[permutation[i]]
        }
        return result
    }

    /**
     * Build Fisher-Yates permutation using java.util.Random-compatible PRNG.
     * CRITICAL: Must produce identical permutation to java.util.Random(seed.toLong())
     * Java's LCG: seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L shl 48) - 1)
     */
    private fun buildPermutation(size: Int, blockIndex: Int, key: ByteArray): IntArray {
        val seed = bytesToInt(key.copyOf(4)) xor blockIndex
        val indices = IntArray(size) { it }

        // Use java.util.Random-compatible LCG
        val rng = JavaCompatRandom(seed.toLong())
        for (i in size - 1 downTo 1) {
            val j = rng.nextInt(i + 1)
            val temp = indices[i]
            indices[i] = indices[j]
            indices[j] = temp
        }
        return indices
    }

    // ========================================================================
    // Header I/O
    // ========================================================================

    private fun buildHeader(
        originalSize: Long,
        encodedSize: Long,
        blockCount: Int,
        fileHash: ByteArray,
        timestamp: Long,
        flags: Short
    ): ByteArray {
        val header = ByteArray(VSMFormat.HEADER_SIZE)
        intToLEBytes(VSMFormat.MAGIC).copyInto(header, 0)
        shortToLEBytes(VSMFormat.VERSION).copyInto(header, 4)
        shortToLEBytes(flags).copyInto(header, 6)
        longToLEBytes(originalSize).copyInto(header, 8)
        longToLEBytes(encodedSize).copyInto(header, 16)
        intToLEBytes(VSMFormat.BLOCK_SIZE).copyInto(header, 24)
        intToLEBytes(blockCount).copyInto(header, 28)
        fileHash.copyInto(header, 32, 0, 16)
        longToLEBytes(timestamp).copyInto(header, 48)
        intToLEBytes(0).copyInto(header, 56) // contentType
        intToLEBytes(0).copyInto(header, 60) // reserved
        return header
    }

    private fun parseHeader(bytes: ByteArray): VSMHeader? {
        if (bytes.size < VSMFormat.HEADER_SIZE) return null
        return VSMHeader(
            magic = leBytesToInt(bytes, 0),
            version = leBytesToShort(bytes, 4),
            flags = leBytesToShort(bytes, 6),
            originalSize = leBytesToLong(bytes, 8),
            encodedSize = leBytesToLong(bytes, 16),
            blockSize = leBytesToInt(bytes, 24),
            blockCount = leBytesToInt(bytes, 28),
            fileHash = bytes.copyOfRange(32, 48),
            timestamp = leBytesToLong(bytes, 48),
            contentType = leBytesToInt(bytes, 56),
            reserved = leBytesToInt(bytes, 60)
        )
    }

    // ========================================================================
    // Metadata
    // ========================================================================

    private fun serializeMetadata(metadata: Map<String, String>): ByteArray {
        if (metadata.isEmpty()) return ByteArray(0)
        val json = metadata.entries.joinToString(",") { (k, v) ->
            "\"${k.replace("\"", "\\\"")}\":\"${v.replace("\"", "\\\"")}\""
        }.let { "{$it}" }
        return json.encodeToByteArray()
    }

    // ========================================================================
    // Byte Helpers
    // ========================================================================

    private fun bytesToInt(bytes: ByteArray): Int {
        if (bytes.size < 4) return 0
        return (bytes[0].toInt() and 0xFF) or
            ((bytes[1].toInt() and 0xFF) shl 8) or
            ((bytes[2].toInt() and 0xFF) shl 16) or
            ((bytes[3].toInt() and 0xFF) shl 24)
    }

    // ========================================================================
    // NSData <-> ByteArray conversion
    // ========================================================================

    private fun ByteArray.toNSData(): NSData {
        if (isEmpty()) return NSData()
        return usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), size.convert())
        }
    }

    private fun NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        if (size == 0) return ByteArray(0)
        val result = ByteArray(size)
        result.usePinned { pinned ->
            getBytes(pinned.addressOf(0), length)
        }
        return result
    }
}

/**
 * java.util.Random-compatible linear congruential generator for iOS.
 * MUST produce identical output to java.util.Random(seed) to ensure
 * cross-platform byte shuffle compatibility.
 *
 * LCG formula: seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L shl 48) - 1)
 */
private class JavaCompatRandom(seed: Long) {
    private var state: Long = (seed xor 0x5DEECE66DL) and MASK

    companion object {
        private const val MULTIPLIER = 0x5DEECE66DL
        private const val INCREMENT = 0xBL
        private const val MASK = (1L shl 48) - 1
    }

    private fun next(bits: Int): Int {
        state = (state * MULTIPLIER + INCREMENT) and MASK
        return (state ushr (48 - bits)).toInt()
    }

    fun nextInt(bound: Int): Int {
        if (bound <= 0) throw IllegalArgumentException("bound must be positive")
        if (bound and (bound - 1) == 0) {
            // Power of two
            return ((bound.toLong() * next(31).toLong()) shr 31).toInt()
        }
        var bits: Int
        var value: Int
        do {
            bits = next(31)
            value = bits % bound
        } while (bits - value + (bound - 1) < 0)
        return value
    }
}
