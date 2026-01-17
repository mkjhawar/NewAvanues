/*
 * Copyright (c) 2025 Intelligent Devices LLC / Manoj Jhawar
 * All Rights Reserved - Confidential
 *
 * AVA 3.0 Encoder for Desktop (JVM)
 * Encodes model files into proprietary AVA 3.0 format for secure distribution.
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
import java.util.zip.Deflater
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AVA 3.0 File Encoder for Desktop
 *
 * Encodes files into proprietary AVA 3.0 format (.ava3) for secure distribution.
 * Only AVA applications with the correct decoder can read these files.
 *
 * Features:
 * - AES-256-CTR encryption
 * - XOR scramble + byte shuffle
 * - Optional compression
 * - File integrity verification via hash
 *
 * Supported output formats:
 * - .amm (MLC-LLM models)
 * - .amg (GGUF models)
 * - .amr (LiteRT models)
 * - .ava3 (generic)
 */
class AVA3Encoder {

    private val logger = LoggerFactory.getLogger(AVA3Encoder::class.java)

    companion object {
        // AVA 3.0 Constants (must match decoder)
        private const val AVA3_MAGIC = 0x41564133  // "AVA3"
        private const val AVA3_VERSION = 0x0300.toShort()  // 3.0
        private const val DEFAULT_BLOCK_SIZE = 65536  // 64 KB

        // Flags
        private const val FLAG_ENCRYPTED: Short = 0x0001
        private const val FLAG_COMPRESSED: Short = 0x0002
        private const val FLAG_CHUNKED: Short = 0x0004

        // Content types
        const val CONTENT_TYPE_GENERIC = 0
        const val CONTENT_TYPE_MLC = 1      // .amm
        const val CONTENT_TYPE_GGUF = 2     // .amg
        const val CONTENT_TYPE_LITERT = 3   // .amr
        const val CONTENT_TYPE_ONNX = 4     // ONNX models
        const val CONTENT_TYPE_TOKENIZER = 5

        // Master seed (must match decoder exactly)
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
     * Encoding options
     */
    data class EncodeOptions(
        val compress: Boolean = true,
        val contentType: Int = CONTENT_TYPE_GENERIC,
        val metadata: Map<String, String> = emptyMap()
    )

    /**
     * Encode a file to AVA 3.0 format
     *
     * @param inputPath Path to source file
     * @param outputPath Path for encoded output (should end with .ava3, .amm, .amg, or .amr)
     * @param options Encoding options
     * @return Path to encoded file
     */
    fun encode(inputPath: String, outputPath: String, options: EncodeOptions = EncodeOptions()): String {
        logger.info("Encoding $inputPath -> $outputPath")
        val startTime = System.currentTimeMillis()

        val inputFile = File(inputPath)
        if (!inputFile.exists()) {
            throw AVA3Exception("Input file not found: $inputPath")
        }

        // Read input file
        val originalData = inputFile.readBytes()
        val originalSize = originalData.size.toLong()

        // Compress if requested
        val dataToEncode = if (options.compress) {
            compress(originalData)
        } else {
            originalData
        }

        // Compute file hash (of original uncompressed data)
        val fileHash = computeFileHash(originalData)

        // Generate timestamp
        val timestamp = System.currentTimeMillis()

        // Derive encryption key
        val key = deriveKey(fileHash, timestamp)

        // Calculate blocks
        val blockSize = DEFAULT_BLOCK_SIZE
        val blockCount = (dataToEncode.size + blockSize - 1) / blockSize

        // Encode blocks
        val encodedBlocks = mutableListOf<ByteArray>()
        for (i in 0 until blockCount) {
            val start = i * blockSize
            val end = minOf(start + blockSize, dataToEncode.size)
            val blockData = dataToEncode.copyOfRange(start, end)

            // Pad last block if needed
            val paddedBlock = if (blockData.size < blockSize) {
                blockData.copyOf(blockSize)
            } else {
                blockData
            }

            val encodedBlock = encodeBlock(paddedBlock, i, key)
            encodedBlocks.add(encodedBlock)
        }

        // Build flags
        var flags: Short = FLAG_ENCRYPTED
        if (options.compress) {
            flags = (flags.toInt() or FLAG_COMPRESSED.toInt()).toShort()
        }

        // Serialize metadata
        val metadataJson = if (options.metadata.isNotEmpty()) {
            options.metadata.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }
                .let { "{$it}" }
                .toByteArray(Charsets.UTF_8)
        } else {
            ByteArray(0)
        }

        // Write output file
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        FileOutputStream(outputFile).use { fos ->
            // Write header (64 bytes)
            val header = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
            header.putInt(AVA3_MAGIC)
            header.putShort(AVA3_VERSION)
            header.putShort(flags)
            header.putLong(originalSize)
            header.putLong(dataToEncode.size.toLong())
            header.putInt(blockSize)
            header.putInt(blockCount)
            header.put(fileHash)
            header.putLong(timestamp)
            header.putInt(options.contentType)
            header.putInt(0) // reserved
            fos.write(header.array())

            // Write metadata length + data
            val metaLenBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
            metaLenBuffer.putInt(metadataJson.size)
            fos.write(metaLenBuffer.array())
            fos.write(metadataJson)

            // Write encoded blocks
            for (block in encodedBlocks) {
                fos.write(block)
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        val ratio = (outputFile.length().toDouble() / originalSize * 100).toInt()
        logger.info("Encoded ${originalData.size} bytes -> ${outputFile.length()} bytes ($ratio%) in ${elapsed}ms")

        return outputPath
    }

    /**
     * Encode multiple files into a single archive
     */
    fun encodeDirectory(inputDir: String, outputPath: String, options: EncodeOptions = EncodeOptions()): String {
        // For directory encoding, we first create a tar archive, then encode it
        // This is a simplified version - full implementation would use Apache Commons Compress
        throw UnsupportedOperationException("Directory encoding not yet implemented - use tar first")
    }

    /**
     * Get appropriate file extension based on content type
     */
    fun getExtension(contentType: Int): String {
        return when (contentType) {
            CONTENT_TYPE_MLC -> ".amm"
            CONTENT_TYPE_GGUF -> ".amg"
            CONTENT_TYPE_LITERT -> ".amr"
            else -> ".ava3"
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

    private fun byteShuffle(data: ByteArray, blockIndex: Int, key: ByteArray): ByteArray {
        val seed = bytesToInt(key.copyOf(4)) xor blockIndex
        val indices = (0 until data.size).toMutableList()

        // Fisher-Yates shuffle with seeded RNG
        val rng = Random(seed.toLong())
        for (i in indices.size - 1 downTo 1) {
            val j = rng.nextInt(i + 1)
            val temp = indices[i]
            indices[i] = indices[j]
            indices[j] = temp
        }

        // Apply shuffle
        val result = ByteArray(data.size)
        for (i in indices.indices) {
            result[indices[i]] = data[i]
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

    private fun encodeBlock(data: ByteArray, blockIndex: Int, key: ByteArray): ByteArray {
        // Step 1: XOR scramble
        val pattern = deriveScramblePattern(key, blockIndex)
        val scrambled = xorData(data, pattern)

        // Step 2: Byte shuffle
        val shuffled = byteShuffle(scrambled, blockIndex, key)

        // Step 3: AES-256-CTR encryption
        val nonce = deriveNonce(key, blockIndex)
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(nonce)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

        return cipher.doFinal(shuffled)
    }

    private fun compress(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(data)
        deflater.finish()

        val output = ByteArray(data.size + 1024)
        val compressedSize = deflater.deflate(output)
        deflater.end()

        // Only use compressed if it's smaller
        return if (compressedSize < data.size) {
            output.copyOf(compressedSize)
        } else {
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
