/*
 * Copyright (c) 2025 Intelligent Devices LLC / Manoj Jhawar
 * All Rights Reserved - Confidential
 *
 * AVA 3.0 Decoder for Desktop (JVM)
 * Decodes proprietary AVA 3.0 encoded files for runtime use.
 */

package com.augmentalis.alc.ava3

import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.Random
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.util.zip.Inflater

/**
 * AVA 3.0 File Decoder for Desktop
 *
 * Decodes proprietary AVA 3.0 encoded files (.ava3) for runtime use.
 * Only AVA applications with the correct decoder can read these files.
 *
 * Features:
 * - AES-256-CTR decryption
 * - XOR scramble + byte shuffle reversal
 * - Automatic decompression (if compressed)
 * - File integrity verification
 *
 * @see docs/AVA3-ENCODING-SPEC.md for format specification
 */
class AVA3Decoder {

    private val logger = LoggerFactory.getLogger(AVA3Decoder::class.java)

    companion object {
        // AVA 3.0 Constants
        private const val AVA3_MAGIC = 0x41564133  // "AVA3"
        private const val AVA3_VERSION = 0x0300.toShort()  // 3.0
        private const val DEFAULT_BLOCK_SIZE = 65536  // 64 KB

        // Flags
        private const val FLAG_ENCRYPTED: Short = 0x0001
        private const val FLAG_COMPRESSED: Short = 0x0002
        private const val FLAG_CHUNKED: Short = 0x0004

        // Master seed (obfuscated - matches encoder)
        private val AVA_MASTER_SEED = byteArrayOf(
            0x41, 0x56, 0x41, 0x2D, 0x41, 0x49, 0x2D, 0x33,
            0x2E, 0x30, 0x2D, 0x4D, 0x41, 0x4E, 0x4F, 0x4A,
            0x2D, 0x4A, 0x48, 0x41, 0x57, 0x41, 0x52, 0x2D,
            0x32, 0x30, 0x32, 0x35, 0x2D, 0x49, 0x44, 0x4C.toByte()
        )

        private const val SALT = "AVA-3.0-SALT-2025"
        private const val PBKDF2_ITERATIONS = 10000
    }

    /**
     * AVA 3.0 Header (64 bytes)
     */
    data class AVA3Header(
        val magic: Int,
        val version: Short,
        val flags: Short,
        val originalSize: Long,
        val encodedSize: Long,
        val blockSize: Int,
        val blockCount: Int,
        val fileHash: ByteArray,
        val timestamp: Long,
        val contentType: Int,
        val reserved: Int
    ) {
        companion object {
            fun parse(buffer: ByteBuffer): AVA3Header {
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                return AVA3Header(
                    magic = buffer.int,
                    version = buffer.short,
                    flags = buffer.short,
                    originalSize = buffer.long,
                    encodedSize = buffer.long,
                    blockSize = buffer.int,
                    blockCount = buffer.int,
                    fileHash = ByteArray(16).also { buffer.get(it) },
                    timestamp = buffer.long,
                    contentType = buffer.int,
                    reserved = buffer.int
                )
            }
        }
    }

    /**
     * Decode AVA 3.0 file to memory
     *
     * @param encodedPath Path to .ava3 file
     * @return Decoded file contents
     * @throws AVA3Exception if decoding fails
     */
    fun decode(encodedPath: String): ByteArray {
        logger.debug("Decoding $encodedPath")
        val startTime = System.currentTimeMillis()

        val file = File(encodedPath)
        if (!file.exists()) {
            throw AVA3Exception("File not found: $encodedPath")
        }

        FileInputStream(file).use { fis ->
            // Read header
            val headerBytes = ByteArray(64)
            fis.read(headerBytes)
            val header = AVA3Header.parse(ByteBuffer.wrap(headerBytes))

            // Validate header
            if (header.magic != AVA3_MAGIC) {
                throw AVA3Exception("Invalid magic: 0x${header.magic.toString(16)}")
            }
            if (header.version != AVA3_VERSION) {
                throw AVA3Exception("Unsupported version: 0x${header.version.toString(16)}")
            }

            logger.debug("AVA 3.0 file, ${header.blockCount} blocks, ${header.originalSize} bytes original")

            // Derive key
            val key = deriveKey(header.fileHash, header.timestamp)

            // Read and decode metadata (skip for now)
            val metaLenBytes = ByteArray(4)
            fis.read(metaLenBytes)
            val metaLen = ByteBuffer.wrap(metaLenBytes).order(ByteOrder.LITTLE_ENDIAN).int
            fis.skip(metaLen.toLong())

            // Decode data blocks
            val decodedData = ByteArray(header.blockCount * header.blockSize)
            var decodedOffset = 0

            for (i in 0 until header.blockCount) {
                val blockData = ByteArray(header.blockSize)
                fis.read(blockData)

                val decoded = decodeBlock(blockData, i, key)
                System.arraycopy(decoded, 0, decodedData, decodedOffset, decoded.size)
                decodedOffset += decoded.size

                if ((i + 1) % 100 == 0) {
                    logger.trace("Decoded block ${i + 1}/${header.blockCount}")
                }
            }

            // Trim to original size
            val trimmedData = decodedData.copyOf(header.originalSize.toInt())

            // Decompress if needed
            val originalData = if ((header.flags.toInt() and FLAG_COMPRESSED.toInt()) != 0) {
                decompress(trimmedData)
            } else {
                trimmedData
            }

            // Verify hash
            val computedHash = computeFileHash(originalData)
            if (!computedHash.contentEquals(header.fileHash)) {
                logger.warn("File hash mismatch - file may be corrupted")
            }

            val elapsed = System.currentTimeMillis() - startTime
            logger.info("Decoded ${originalData.size} bytes in ${elapsed}ms")

            return originalData
        }
    }

    /**
     * Decode AVA 3.0 file to output file
     */
    fun decodeToFile(encodedPath: String, outputPath: String): String {
        val decoded = decode(encodedPath)

        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        FileOutputStream(outputFile).use { fos ->
            fos.write(decoded)
        }

        logger.info("Decoded to $outputPath")
        return outputPath
    }

    /**
     * Decode AVA 3.0 file to cache directory
     *
     * @param encodedPath Path to .ava3 file
     * @param cacheDir Cache directory (default: ~/.augmentalis/cache/ava3)
     * @return Path to decoded file in cache
     */
    fun decodeToCache(encodedPath: String, cacheDir: String? = null): String {
        val inputFile = File(encodedPath)
        var outputName = inputFile.name
        if (outputName.endsWith(".ava3")) {
            outputName = outputName.dropLast(5)
        }

        val cachePath = cacheDir ?: "${System.getProperty("user.home")}/.augmentalis/cache/ava3"
        val cache = File(cachePath)
        cache.mkdirs()

        val outputPath = File(cache, outputName).absolutePath
        return decodeToFile(encodedPath, outputPath)
    }

    /**
     * Verify AVA 3.0 file without full decoding
     */
    fun verify(encodedPath: String): Boolean {
        return try {
            val file = File(encodedPath)
            if (!file.exists()) return false

            FileInputStream(file).use { fis ->
                val headerBytes = ByteArray(64)
                fis.read(headerBytes)
                val header = AVA3Header.parse(ByteBuffer.wrap(headerBytes))

                header.magic == AVA3_MAGIC && header.version == AVA3_VERSION
            }
        } catch (e: Exception) {
            logger.error("Verification failed", e)
            false
        }
    }

    /**
     * Check if file is AVA 3.0 encoded
     */
    fun isAVA3Encoded(path: String): Boolean {
        if (path.endsWith(".ava3")) return true

        return try {
            val file = File(path)
            if (!file.exists() || file.length() < 4) return false

            FileInputStream(file).use { fis ->
                val magic = ByteArray(4)
                fis.read(magic)
                val magicInt = ByteBuffer.wrap(magic).order(ByteOrder.LITTLE_ENDIAN).int
                magicInt == AVA3_MAGIC
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun computeFileHash(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val fullHash = digest.digest(data)
        return fullHash.copyOf(16)
    }

    private fun deriveKey(fileHash: ByteArray, timestamp: Long): ByteArray {
        val keyMaterial = AVA_MASTER_SEED + fileHash.copyOf(16) + longToBytes(timestamp)
        val salt = SALT.toByteArray(Charsets.UTF_8)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(
            String(keyMaterial, Charsets.ISO_8859_1).toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            256
        )
        return factory.generateSecret(spec).encoded
    }

    private fun deriveScramblePattern(key: ByteArray, blockIndex: Int): ByteArray {
        val seed = key + intToBytes(blockIndex)
        val digest = MessageDigest.getInstance("SHA-512")
        val pattern = digest.digest(seed)

        val repeats = (DEFAULT_BLOCK_SIZE / 64) + 1
        val fullPattern = ByteArray(repeats * 64)
        for (i in 0 until repeats) {
            System.arraycopy(pattern, 0, fullPattern, i * 64, 64)
        }
        return fullPattern.copyOf(DEFAULT_BLOCK_SIZE)
    }

    private fun deriveNonce(key: ByteArray, blockIndex: Int): ByteArray {
        val nonceMaterial = key.copyOf(8) + intToBytes(blockIndex)
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(nonceMaterial).copyOf(16)
    }

    private fun byteUnshuffle(data: ByteArray, blockIndex: Int, key: ByteArray): ByteArray {
        val seed = bytesToInt(key.copyOf(4)) xor blockIndex
        val indices = (0 until data.size).toMutableList()

        val rng = Random(seed.toLong())
        for (i in indices.size - 1 downTo 1) {
            val j = rng.nextInt(i + 1)
            val temp = indices[i]
            indices[i] = indices[j]
            indices[j] = temp
        }

        val result = ByteArray(data.size)
        for (i in indices.indices) {
            result[i] = data[indices[i]]
        }
        return result
    }

    private fun xorData(data: ByteArray, pattern: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        for (i in data.indices) {
            result[i] = (data[i].toInt() xor pattern[i % pattern.size].toInt()).toByte()
        }
        return result
    }

    private fun decodeBlock(data: ByteArray, blockIndex: Int, key: ByteArray): ByteArray {
        val nonce = deriveNonce(key, blockIndex)
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(nonce)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val decrypted = cipher.doFinal(data)

        val unshuffled = byteUnshuffle(decrypted, blockIndex, key)
        val pattern = deriveScramblePattern(key, blockIndex)
        return xorData(unshuffled, pattern)
    }

    private fun decompress(data: ByteArray): ByteArray {
        return try {
            Inflater().run {
                setInput(data)
                val output = ByteArray(data.size * 4)
                val size = inflate(output)
                end()
                output.copyOf(size)
            }
        } catch (e: Exception) {
            logger.warn("Decompression failed, returning raw data")
            data
        }
    }

    private fun longToBytes(value: Long): ByteArray {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array()
    }

    private fun intToBytes(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun bytesToInt(bytes: ByteArray): Int {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int
    }
}

/**
 * Exception thrown by AVA3Decoder
 */
class AVA3Exception(message: String, cause: Throwable? = null) : Exception(message, cause)
