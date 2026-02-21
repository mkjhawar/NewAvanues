/**
 * VSMCodec.kt - VoiceOS Speech Model encryption/decryption codec
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 *
 * Self-contained AES-256-CTR encryption for Whisper ggml model files.
 * Same crypto algorithms as AVA3 (ALC module) but with a unique master seed
 * and placed in jvmMain so Android + Desktop share a single implementation.
 *
 * Crypto stack per 64 KB block:
 *   Encrypt: XOR scramble (SHA-512) -> Fisher-Yates byte shuffle -> AES-256-CTR
 *   Decrypt: AES-256-CTR -> Fisher-Yates unshuffle -> XOR unscramble
 *
 * Key derivation: PBKDF2-HMAC-SHA256 with VSM-specific master seed + file hash + timestamp.
 * File hash: SHA-256 truncated to 16 bytes stored in header for integrity verification.
 *
 * Uses streaming I/O — processes one 64 KB block at a time, safe for models up to 1.5 GB.
 */
package com.augmentalis.speechrecognition.whisper.vsm

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.Random
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class VSMCodec {

    /**
     * Encrypt a raw model file (.bin) to VSM format (.vsm).
     *
     * Two-pass streaming: first pass computes SHA-256 file hash (for header + key derivation),
     * second pass reads 64 KB blocks, encrypts each, and writes to output.
     *
     * @param inputPath  Path to source ggml model file (.bin)
     * @param outputPath Path for encrypted output (.vsm)
     * @param metadata   Optional metadata map (model name, language, etc.) — stored as JSON after header
     * @return true on success, false on failure
     */
    fun encryptFile(
        inputPath: String,
        outputPath: String,
        metadata: Map<String, String> = emptyMap()
    ): Boolean {
        return try {
            val inputFile = File(inputPath)
            if (!inputFile.exists()) return false

            val originalSize = inputFile.length()
            if (originalSize == 0L) return false

            val timestamp = System.currentTimeMillis()

            // Pass 1: Compute SHA-256 hash of original file (streaming, 8 KB buffer)
            val fileHash = computeFileHash(inputFile)

            // Derive encryption key from master seed + file hash + timestamp
            val key = deriveKey(fileHash, timestamp)

            // Calculate block count (ceil division)
            val blockCount = ((originalSize + VSMFormat.BLOCK_SIZE - 1) / VSMFormat.BLOCK_SIZE).toInt()

            // Serialize metadata to JSON bytes
            val metadataBytes = serializeMetadata(metadata)

            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()

            // Pass 2: Read blocks, encrypt, write
            FileInputStream(inputFile).use { fis ->
                FileOutputStream(outputFile).use { fos ->
                    // Write 64-byte header
                    writeHeader(
                        fos, originalSize, originalSize, blockCount,
                        fileHash, timestamp, VSMFormat.FLAG_ENCRYPTED
                    )

                    // Write metadata length (4 bytes LE) + metadata JSON
                    writeInt(fos, metadataBytes.size)
                    if (metadataBytes.isNotEmpty()) {
                        fos.write(metadataBytes)
                    }

                    // Encrypt and write blocks
                    val readBuffer = ByteArray(VSMFormat.BLOCK_SIZE)
                    for (i in 0 until blockCount) {
                        val bytesRead = readFully(fis, readBuffer)

                        // Pad last block to full block size if needed
                        val block = if (bytesRead < VSMFormat.BLOCK_SIZE) {
                            ByteArray(VSMFormat.BLOCK_SIZE).also { padded ->
                                System.arraycopy(readBuffer, 0, padded, 0, bytesRead)
                                // Remaining bytes are zero-filled by default
                            }
                        } else {
                            readBuffer.copyOf(VSMFormat.BLOCK_SIZE)
                        }

                        val encrypted = encryptBlock(block, i, key)
                        fos.write(encrypted)
                    }
                }
            }

            true
        } catch (e: Exception) {
            // Clean up partial output on failure
            try { File(outputPath).delete() } catch (_: Exception) {}
            false
        }
    }

    /**
     * Decrypt a VSM file to a temporary file for loading by whisper.cpp.
     *
     * Reads header, derives key, decrypts blocks to temp file, verifies hash.
     * Caller is responsible for deleting the returned temp file after use.
     *
     * @param vsmPath Path to encrypted .vsm file
     * @param tempDir Directory for temporary decrypted file (e.g. app cache dir)
     * @return Decrypted temp file, or null on failure (invalid format, hash mismatch, etc.)
     */
    fun decryptToTempFile(vsmPath: String, tempDir: File): File? {
        val vsmFile = File(vsmPath)
        if (!vsmFile.exists()) return null

        tempDir.mkdirs()
        var tempFile: File? = null

        try {
            FileInputStream(vsmFile).use { fis ->
                // Read and parse 64-byte header
                val headerBytes = ByteArray(VSMFormat.HEADER_SIZE)
                if (readFully(fis, headerBytes) != VSMFormat.HEADER_SIZE) return null

                val header = parseHeader(headerBytes) ?: return null
                if (!header.isValid()) return null
                if (!header.isEncrypted()) return null

                // Read metadata length and skip metadata
                val metaLenBytes = ByteArray(4)
                if (readFully(fis, metaLenBytes) != 4) return null
                val metaLen = leBytesToInt(metaLenBytes)
                if (metaLen > 0) {
                    skipFully(fis, metaLen.toLong())
                }

                // Derive decryption key (same derivation as encryption)
                val key = deriveKey(header.fileHash, header.timestamp)

                // Create temp file for decrypted output
                tempFile = File(tempDir, "vsm_${System.nanoTime()}.bin")

                FileOutputStream(tempFile!!).use { fos ->
                    val blockBuffer = ByteArray(VSMFormat.BLOCK_SIZE)
                    var totalWritten = 0L

                    for (i in 0 until header.blockCount) {
                        val bytesRead = readFully(fis, blockBuffer)
                        if (bytesRead == 0) break

                        // Pad to full block if file was truncated
                        val block = if (bytesRead < VSMFormat.BLOCK_SIZE) {
                            ByteArray(VSMFormat.BLOCK_SIZE).also { padded ->
                                System.arraycopy(blockBuffer, 0, padded, 0, bytesRead)
                            }
                        } else {
                            blockBuffer.copyOf(VSMFormat.BLOCK_SIZE)
                        }

                        val decrypted = decryptBlock(block, i, key)

                        // Only write up to originalSize bytes (last block has padding)
                        val remaining = header.originalSize - totalWritten
                        val writeLen = minOf(remaining, decrypted.size.toLong()).toInt()
                        fos.write(decrypted, 0, writeLen)
                        totalWritten += writeLen
                    }
                }

                // Verify integrity: hash of decrypted file must match header hash
                val decryptedHash = computeFileHash(tempFile!!)
                if (!decryptedHash.contentEquals(header.fileHash)) {
                    tempFile?.delete()
                    return null
                }

                return tempFile
            }
        } catch (e: Exception) {
            tempFile?.delete()
            return null
        }
    }

    /**
     * Read VSM header metadata without decrypting the file.
     * Useful for checking model identity, version, or size before loading.
     */
    fun readHeader(vsmPath: String): VSMHeader? {
        val file = File(vsmPath)
        if (!file.exists() || file.length() < VSMFormat.HEADER_SIZE) return null

        return try {
            FileInputStream(file).use { fis ->
                val headerBytes = ByteArray(VSMFormat.HEADER_SIZE)
                if (readFully(fis, headerBytes) != VSMFormat.HEADER_SIZE) return null
                parseHeader(headerBytes)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Read metadata JSON from a VSM file.
     * Returns the raw metadata map, or empty map if no metadata.
     */
    fun readMetadata(vsmPath: String): Map<String, String> {
        val file = File(vsmPath)
        if (!file.exists()) return emptyMap()

        return try {
            FileInputStream(file).use { fis ->
                // Skip header
                skipFully(fis, VSMFormat.HEADER_SIZE.toLong())

                // Read metadata length
                val metaLenBytes = ByteArray(4)
                if (readFully(fis, metaLenBytes) != 4) return emptyMap()
                val metaLen = leBytesToInt(metaLenBytes)
                if (metaLen <= 0) return emptyMap()

                // Read metadata JSON
                val metaBytes = ByteArray(metaLen)
                if (readFully(fis, metaBytes) != metaLen) return emptyMap()
                val json = String(metaBytes, Charsets.UTF_8)

                // Simple JSON parsing (no library dependency)
                parseSimpleJson(json)
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // ========================================================================
    // Key Derivation
    // ========================================================================

    /**
     * Derive AES-256 key from master seed, file hash, and timestamp.
     *
     * keyMaterial = MASTER_SEED || fileHash[0:16] || timestamp(8 bytes LE)
     * key = PBKDF2-HMAC-SHA256(keyMaterial as char[], salt, 10000 iterations, 256 bits)
     *
     * Note: keyMaterial is converted to chars via ISO-8859-1 (1:1 byte-to-char mapping)
     * to match AVA3's key derivation exactly.
     */
    private fun deriveKey(fileHash: ByteArray, timestamp: Long): ByteArray {
        val keyMaterial = VSMFormat.MASTER_SEED + fileHash.copyOf(16) + longToBytes(timestamp)
        val salt = VSMFormat.SALT.toByteArray(Charsets.UTF_8)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(
            String(keyMaterial, Charsets.ISO_8859_1).toCharArray(),
            salt,
            VSMFormat.PBKDF2_ITERATIONS,
            256
        )
        return factory.generateSecret(spec).encoded
    }

    // ========================================================================
    // Block Encryption / Decryption
    // ========================================================================

    /**
     * Encrypt a single 64 KB block.
     * Pipeline: XOR scramble -> Fisher-Yates shuffle -> AES-256-CTR
     */
    private fun encryptBlock(data: ByteArray, blockIndex: Int, key: ByteArray): ByteArray {
        // Step 1: XOR with SHA-512-derived scramble pattern
        val pattern = deriveScramblePattern(key, blockIndex)
        val scrambled = xorData(data, pattern)

        // Step 2: Fisher-Yates byte shuffle with seeded RNG
        val shuffled = fisherYatesShuffle(scrambled, blockIndex, key)

        // Step 3: AES-256-CTR encryption with per-block nonce
        val nonce = deriveNonce(key, blockIndex)
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(nonce))
        return cipher.doFinal(shuffled)
    }

    /**
     * Decrypt a single 64 KB block.
     * Pipeline: AES-256-CTR -> Fisher-Yates unshuffle -> XOR unscramble
     */
    private fun decryptBlock(data: ByteArray, blockIndex: Int, key: ByteArray): ByteArray {
        // Step 1: AES-256-CTR decryption (symmetric — same algo, DECRYPT_MODE)
        val nonce = deriveNonce(key, blockIndex)
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(nonce))
        val decrypted = cipher.doFinal(data)

        // Step 2: Reverse Fisher-Yates shuffle
        val unshuffled = fisherYatesUnshuffle(decrypted, blockIndex, key)

        // Step 3: XOR unscramble (XOR is self-inverse)
        val pattern = deriveScramblePattern(key, blockIndex)
        return xorData(unshuffled, pattern)
    }

    // ========================================================================
    // Crypto Primitives
    // ========================================================================

    /**
     * Derive XOR scramble pattern from SHA-512.
     * SHA-512 produces 64 bytes per hash — repeated to fill 64 KB block.
     */
    private fun deriveScramblePattern(key: ByteArray, blockIndex: Int): ByteArray {
        val seed = key + intToBytes(blockIndex)
        val digest = MessageDigest.getInstance("SHA-512")
        val pattern = digest.digest(seed) // 64 bytes

        val repeats = (VSMFormat.BLOCK_SIZE / 64) + 1
        val fullPattern = ByteArray(repeats * 64)
        for (i in 0 until repeats) {
            System.arraycopy(pattern, 0, fullPattern, i * 64, 64)
        }
        return fullPattern.copyOf(VSMFormat.BLOCK_SIZE)
    }

    /**
     * Derive per-block nonce (16 bytes) from MD5.
     * nonceMaterial = key[0:8] || blockIndex(4 bytes LE)
     */
    private fun deriveNonce(key: ByteArray, blockIndex: Int): ByteArray {
        val nonceMaterial = key.copyOf(8) + intToBytes(blockIndex)
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(nonceMaterial).copyOf(16)
    }

    /** XOR each byte of data with the corresponding pattern byte. Self-inverse. */
    private fun xorData(data: ByteArray, pattern: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        for (i in data.indices) {
            result[i] = (data[i].toInt() xor pattern[i % pattern.size].toInt()).toByte()
        }
        return result
    }

    /**
     * Fisher-Yates shuffle: permute bytes using a seeded RNG.
     * Seed = int(key[0:4]) XOR blockIndex → java.util.Random
     * Mapping: result[permuted_index] = data[original_index]
     */
    private fun fisherYatesShuffle(data: ByteArray, blockIndex: Int, key: ByteArray): ByteArray {
        val permutation = buildPermutation(data.size, blockIndex, key)
        val result = ByteArray(data.size)
        for (i in permutation.indices) {
            result[permutation[i]] = data[i]
        }
        return result
    }

    /**
     * Reverse Fisher-Yates shuffle: unpermute bytes.
     * Uses the same permutation but reverses the mapping direction.
     * Mapping: result[original_index] = data[permuted_index]
     */
    private fun fisherYatesUnshuffle(data: ByteArray, blockIndex: Int, key: ByteArray): ByteArray {
        val permutation = buildPermutation(data.size, blockIndex, key)
        val result = ByteArray(data.size)
        for (i in permutation.indices) {
            result[i] = data[permutation[i]]
        }
        return result
    }

    /**
     * Build the Fisher-Yates permutation array (shared by shuffle and unshuffle).
     * The permutation maps original index → shuffled index.
     */
    private fun buildPermutation(size: Int, blockIndex: Int, key: ByteArray): IntArray {
        val seed = bytesToInt(key.copyOf(4)) xor blockIndex
        val indices = IntArray(size) { it }

        val rng = Random(seed.toLong())
        for (i in size - 1 downTo 1) {
            val j = rng.nextInt(i + 1)
            val temp = indices[i]
            indices[i] = indices[j]
            indices[j] = temp
        }
        return indices
    }

    // ========================================================================
    // File Hash
    // ========================================================================

    /** Compute SHA-256 of file, truncated to first 16 bytes. Streaming (8 KB buffer). */
    private fun computeFileHash(file: File): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().copyOf(16)
    }

    // ========================================================================
    // Header I/O
    // ========================================================================

    /** Write 64-byte VSM header to output stream (little-endian). */
    private fun writeHeader(
        fos: FileOutputStream,
        originalSize: Long,
        encodedSize: Long,
        blockCount: Int,
        fileHash: ByteArray,
        timestamp: Long,
        flags: Short
    ) {
        val header = ByteBuffer.allocate(VSMFormat.HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        header.putInt(VSMFormat.MAGIC)           // bytes 0-3
        header.putShort(VSMFormat.VERSION)        // bytes 4-5
        header.putShort(flags)                    // bytes 6-7
        header.putLong(originalSize)              // bytes 8-15
        header.putLong(encodedSize)               // bytes 16-23
        header.putInt(VSMFormat.BLOCK_SIZE)       // bytes 24-27
        header.putInt(blockCount)                 // bytes 28-31
        header.put(fileHash)                      // bytes 32-47 (16 bytes)
        header.putLong(timestamp)                 // bytes 48-55
        header.putInt(0)                          // bytes 56-59: contentType (0 = speech)
        header.putInt(0)                          // bytes 60-63: reserved
        fos.write(header.array())
    }

    /** Parse 64-byte header from raw bytes. */
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
    // I/O Helpers
    // ========================================================================

    /** Write a single int as 4 bytes little-endian. */
    private fun writeInt(fos: FileOutputStream, value: Int) {
        val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
        fos.write(buf)
    }

    /**
     * Read up to buffer.size bytes from the stream, retrying until buffer is full or EOF.
     * Unlike InputStream.read(), this handles partial reads correctly.
     * Returns the number of bytes actually read.
     */
    private fun readFully(input: InputStream, buffer: ByteArray): Int {
        var offset = 0
        while (offset < buffer.size) {
            val bytesRead = input.read(buffer, offset, buffer.size - offset)
            if (bytesRead == -1) break
            offset += bytesRead
        }
        return offset
    }

    /** Skip exactly n bytes from the stream. */
    private fun skipFully(input: InputStream, n: Long) {
        var remaining = n
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped <= 0) {
                // skip() returned 0 — read and discard instead
                if (input.read() == -1) break
                remaining--
            } else {
                remaining -= skipped
            }
        }
    }

    // ========================================================================
    // Byte Conversion (JVM optimized — uses ByteBuffer)
    // ========================================================================

    private fun longToBytes(value: Long): ByteArray =
        ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array()

    private fun intToBytes(value: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()

    private fun bytesToInt(bytes: ByteArray): Int =
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int

    // ========================================================================
    // Metadata
    // ========================================================================

    /** Serialize metadata map to JSON bytes. Minimal JSON — no library dependency. */
    private fun serializeMetadata(metadata: Map<String, String>): ByteArray {
        if (metadata.isEmpty()) return ByteArray(0)
        val json = metadata.entries.joinToString(",") { (k, v) ->
            "\"${escapeJson(k)}\":\"${escapeJson(v)}\""
        }.let { "{$it}" }
        return json.toByteArray(Charsets.UTF_8)
    }

    /** Parse simple flat JSON object {"key":"value",...} without library dependency. */
    private fun parseSimpleJson(json: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val trimmed = json.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return result

        val inner = trimmed.substring(1, trimmed.length - 1).trim()
        if (inner.isEmpty()) return result

        // Split by comma outside quotes, then parse key:value pairs
        var inQuotes = false
        var escaped = false
        val parts = mutableListOf<String>()
        val current = StringBuilder()

        for (ch in inner) {
            when {
                escaped -> { current.append(ch); escaped = false }
                ch == '\\' -> { current.append(ch); escaped = true }
                ch == '"' -> { current.append(ch); inQuotes = !inQuotes }
                ch == ',' && !inQuotes -> { parts.add(current.toString()); current.clear() }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) parts.add(current.toString())

        for (part in parts) {
            val colonIndex = findUnquotedColon(part)
            if (colonIndex < 0) continue
            val key = unquoteJson(part.substring(0, colonIndex).trim())
            val value = unquoteJson(part.substring(colonIndex + 1).trim())
            if (key.isNotEmpty()) result[key] = value
        }

        return result
    }

    private fun findUnquotedColon(s: String): Int {
        var inQuotes = false
        for (i in s.indices) {
            when (s[i]) {
                '"' -> inQuotes = !inQuotes
                ':' -> if (!inQuotes) return i
            }
        }
        return -1
    }

    private fun unquoteJson(s: String): String {
        val trimmed = s.trim()
        if (trimmed.length >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
        }
        return trimmed
    }

    private fun escapeJson(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")
}
