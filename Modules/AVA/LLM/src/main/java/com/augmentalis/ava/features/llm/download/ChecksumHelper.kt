/**
 * Checksum Helper for Model Downloads
 *
 * Provides utilities for generating and verifying SHA-256 checksums for model files.
 *
 * Usage:
 * ```
 * // Generate checksum for a downloaded file
 * val checksum = ChecksumHelper.calculateSHA256(File("/path/to/model.bin"))
 *
 * // Verify file integrity
 * val isValid = ChecksumHelper.verifyChecksum(
 *     file = File("/path/to/model.bin"),
 *     expectedChecksum = "abc123..."
 * )
 * ```
 *
 * Created: 2025-11-07
 * Author: AVA AI Team
 */

package com.augmentalis.ava.features.llm.download

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Helper object for checksum operations
 */
object ChecksumHelper {

    /**
     * Calculate SHA-256 checksum for a file
     *
     * @param file File to checksum
     * @return Hex-encoded SHA-256 checksum
     */
    fun calculateSHA256(file: File): String {
        require(file.exists()) { "File does not exist: ${file.absolutePath}" }
        require(file.isFile) { "Not a file: ${file.absolutePath}" }

        return file.inputStream().use { input ->
            calculateSHA256(input)
        }
    }

    /**
     * Calculate SHA-256 checksum for an input stream
     *
     * @param input Input stream to checksum
     * @return Hex-encoded SHA-256 checksum
     */
    fun calculateSHA256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)

        var bytesRead = input.read(buffer)
        while (bytesRead != -1) {
            digest.update(buffer, 0, bytesRead)
            bytesRead = input.read(buffer)
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Verify file checksum
     *
     * @param file File to verify
     * @param expectedChecksum Expected SHA-256 checksum (hex-encoded)
     * @return True if checksums match (case-insensitive)
     */
    fun verifyChecksum(file: File, expectedChecksum: String): Boolean {
        val actualChecksum = calculateSHA256(file)
        return actualChecksum.equals(expectedChecksum, ignoreCase = true)
    }

    /**
     * Generate checksums for all models in a directory
     *
     * Useful for generating checksums during model preparation.
     *
     * @param directory Directory containing model files
     * @return Map of filename to checksum
     */
    fun generateChecksums(directory: File): Map<String, String> {
        require(directory.exists()) { "Directory does not exist: ${directory.absolutePath}" }
        require(directory.isDirectory) { "Not a directory: ${directory.absolutePath}" }

        return directory.listFiles()
            ?.filter { it.isFile }
            ?.associate { file ->
                file.name to calculateSHA256(file)
            } ?: emptyMap()
    }

    /**
     * Print checksums in Kotlin data class format
     *
     * Useful for updating ModelDownloadConfig definitions.
     *
     * @param checksums Map of filename to checksum
     */
    fun printForKotlin(checksums: Map<String, String>) {
        checksums.forEach { (filename, checksum) ->
            println("""// $filename""")
            println("""checksum = "$checksum",""")
            println()
        }
    }

    /**
     * Known model checksums
     *
     * To add new model checksums:
     * 1. Download the model file
     * 2. Run: ChecksumHelper.calculateSHA256(File("/path/to/model"))
     * 3. Add the checksum below
     * 4. Update ModelDownloadConfig with the checksum
     */
    object KnownChecksums {
        /**
         * Gemma 2B IT Q4 (MLC-LLM quantized)
         *
         * Source: https://huggingface.co/mlc-ai/gemma-2b-it-q4f16_1-MLC
         * File: params_shard_0.bin
         * Size: ~1.5GB
         *
         * TODO: Generate checksum after downloading model
         * Command: wget [url] && sha256sum params_shard_0.bin
         */
        const val GEMMA_2B_IT_Q4_PARAMS = "TODO_GENERATE_AFTER_DOWNLOAD"

        /**
         * MobileBERT INT8 (ONNX)
         *
         * Source: https://huggingface.co/onnx-community/mobilebert-uncased-ONNX
         * File: onnx/model_int8.onnx
         * Size: ~25.5MB
         *
         * TODO: Generate checksum after downloading model
         * Command: wget [url] && sha256sum model_int8.onnx
         */
        const val MOBILEBERT_INT8_ONNX = "TODO_GENERATE_AFTER_DOWNLOAD"

        /**
         * MobileBERT Vocabulary
         *
         * Source: https://huggingface.co/onnx-community/mobilebert-uncased-ONNX
         * File: vocab.txt
         * Size: ~460KB
         *
         * TODO: Generate checksum after downloading model
         * Command: wget [url] && sha256sum vocab.txt
         */
        const val MOBILEBERT_VOCAB = "TODO_GENERATE_AFTER_DOWNLOAD"

        /**
         * Example of how to add new checksums:
         *
         * 1. Download the file:
         *    wget https://example.com/model.bin
         *
         * 2. Generate checksum:
         *    sha256sum model.bin
         *    # Output: abc123def456... model.bin
         *
         * 3. Add constant:
         *    const val MY_MODEL = "abc123def456..."
         *
         * 4. Update ModelDownloadConfig:
         *    val MY_MODEL_CONFIG = ModelDownloadConfig(
         *        ...
         *        checksum = KnownChecksums.MY_MODEL,
         *        ...
         *    )
         */
    }
}
