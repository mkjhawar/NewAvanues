/**
 * VLMTool.kt - VoiceOS Language Model Encryption CLI
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 *
 * Command-line tool for encrypting/decrypting Whisper model files
 * using the VSM codec (AES-256-CTR + XOR scramble + Fisher-Yates shuffle).
 *
 * Usage:
 *   ./gradlew :Modules:SpeechRecognition:runVlmTool --args="<command> [options]"
 *
 * Commands:
 *   encode <input> <output>           Encrypt a single file
 *   decode <input> <output>           Decrypt a single file
 *   batch-encode <dir> [output-dir]   Encrypt all .bin files in directory
 *   batch-decode <dir> [output-dir]   Decrypt all .vlm files in directory
 *   verify <file>                     Verify a .vlm file integrity
 *   info <file>                       Show .vlm file header info
 */
package com.augmentalis.speechrecognition.cli

import com.augmentalis.speechrecognition.whisper.WhisperModelSize
import com.augmentalis.speechrecognition.whisper.vsm.VSMCodec
import com.augmentalis.speechrecognition.whisper.vsm.VSMFormat
import com.augmentalis.speechrecognition.whisper.vsm.vsmFileName
import java.io.File
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    println()
    println("  VLM Tool v1.0 — VoiceOS Language Model Encryption")
    println("  Copyright (C) Augmentalis Inc, Intelligent Devices LLC")
    println()

    if (args.isEmpty()) {
        printUsage()
        exitProcess(1)
    }

    val command = args[0].lowercase()
    val remainingArgs = args.drop(1)

    try {
        when (command) {
            "encode" -> handleEncode(remainingArgs)
            "decode" -> handleDecode(remainingArgs)
            "batch-encode" -> handleBatchEncode(remainingArgs)
            "batch-decode" -> handleBatchDecode(remainingArgs)
            "verify" -> handleVerify(remainingArgs)
            "info" -> handleInfo(remainingArgs)
            "help", "-h", "--help" -> printUsage()
            else -> {
                println("  Unknown command: $command")
                println()
                printUsage()
                exitProcess(1)
            }
        }
    } catch (e: Exception) {
        System.err.println("  Error: ${e.message}")
        exitProcess(1)
    }
}

// ============================================================================
// Command Handlers
// ============================================================================

private fun handleEncode(args: List<String>) {
    if (args.size < 2) {
        println("  Error: Input and output paths required")
        println("  Usage: vlm-tool encode <input.bin> <output.vlm>")
        exitProcess(1)
    }

    val inputPath = args[0]
    val outputPath = args[1]
    val inputFile = File(inputPath)

    if (!inputFile.exists()) {
        println("  Error: Input file not found: $inputPath")
        exitProcess(1)
    }

    // Build metadata from known model sizes
    val metadata = buildMetadata(inputFile)

    println("  Encrypting...")
    println("    Input:  $inputPath (${formatBytes(inputFile.length())})")
    println("    Output: $outputPath")

    val codec = VSMCodec()
    val elapsedMs = measureTimeMillis {
        val success = codec.encryptFile(inputPath, outputPath, metadata)
        if (!success) {
            println("  Encryption FAILED")
            exitProcess(1)
        }
    }

    val outputFile = File(outputPath)
    println("    Done in ${formatDuration(elapsedMs)}")
    println("    Output size: ${formatBytes(outputFile.length())}")
    println("    Overhead: +${formatBytes(outputFile.length() - inputFile.length())} (header + padding)")
    println()
}

private fun handleDecode(args: List<String>) {
    if (args.size < 2) {
        println("  Error: Input and output paths required")
        println("  Usage: vlm-tool decode <input.vlm> <output.bin>")
        exitProcess(1)
    }

    val inputPath = args[0]
    val outputPath = args[1]
    val inputFile = File(inputPath)

    if (!inputFile.exists()) {
        println("  Error: Input file not found: $inputPath")
        exitProcess(1)
    }

    println("  Decrypting...")
    println("    Input:  $inputPath (${formatBytes(inputFile.length())})")
    println("    Output: $outputPath")

    val codec = VSMCodec()
    val tempDir = File(System.getProperty("java.io.tmpdir"), "vlm_decode_${System.nanoTime()}")

    val elapsedMs = measureTimeMillis {
        val tempFile = codec.decryptToTempFile(inputPath, tempDir)
        if (tempFile == null) {
            println("  Decryption FAILED (invalid format or hash mismatch)")
            tempDir.deleteRecursively()
            exitProcess(1)
        }

        // Move temp file to desired output path
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()
        tempFile.copyTo(outputFile, overwrite = true)
        tempFile.delete()
    }

    tempDir.deleteRecursively()
    val outputFile = File(outputPath)
    println("    Done in ${formatDuration(elapsedMs)}")
    println("    Output size: ${formatBytes(outputFile.length())}")
    println()
}

private fun handleBatchEncode(args: List<String>) {
    if (args.isEmpty()) {
        println("  Error: Input directory required")
        println("  Usage: vlm-tool batch-encode <dir> [output-dir]")
        println()
        println("  Encrypts all .bin files in <dir> (recursively) to .vlm files.")
        println("  If output-dir is specified, encrypted files go there; otherwise, alongside originals.")
        exitProcess(1)
    }

    val inputDir = File(args[0])
    val outputDir = if (args.size > 1) File(args[1]) else null

    if (!inputDir.exists() || !inputDir.isDirectory) {
        println("  Error: Directory not found: ${args[0]}")
        exitProcess(1)
    }

    outputDir?.mkdirs()

    // Find all .bin files recursively
    val binFiles = inputDir.walkTopDown()
        .filter { it.isFile && it.extension == "bin" }
        .toList()

    if (binFiles.isEmpty()) {
        println("  No .bin files found in: ${inputDir.absolutePath}")
        exitProcess(0)
    }

    println("  Found ${binFiles.size} .bin file(s) to encrypt")
    println("  ${"─".repeat(60)}")

    val codec = VSMCodec()
    var successCount = 0
    var failCount = 0
    var totalBytes = 0L

    val totalElapsed = measureTimeMillis {
        for ((index, binFile) in binFiles.withIndex()) {
            val vlmName = resolveVlmName(binFile)
            val vlmFile = if (outputDir != null) {
                // Preserve subdirectory structure in output dir
                val relativePath = binFile.relativeTo(inputDir).parent
                val targetDir = if (relativePath != null) File(outputDir, relativePath) else outputDir
                targetDir.mkdirs()
                File(targetDir, vlmName)
            } else {
                File(binFile.parentFile, vlmName)
            }

            print("  [${index + 1}/${binFiles.size}] ${binFile.name} → $vlmName ... ")

            val metadata = buildMetadata(binFile)
            var success = false
            val elapsedMs = measureTimeMillis {
                success = codec.encryptFile(binFile.absolutePath, vlmFile.absolutePath, metadata)
            }
            if (success) {
                successCount++
                totalBytes += vlmFile.length()
                println("OK (${formatBytes(binFile.length())}, ${formatDuration(elapsedMs)})")
            } else {
                failCount++
                println("FAILED")
            }
        }
    }

    println("  ${"─".repeat(60)}")
    println("  Batch complete: $successCount succeeded, $failCount failed")
    println("  Total output: ${formatBytes(totalBytes)}")
    println("  Total time: ${formatDuration(totalElapsed)}")
    println()
}

private fun handleBatchDecode(args: List<String>) {
    if (args.isEmpty()) {
        println("  Error: Input directory required")
        println("  Usage: vlm-tool batch-decode <dir> [output-dir]")
        println()
        println("  Decrypts all .vlm files in <dir> (recursively) to .bin files.")
        exitProcess(1)
    }

    val inputDir = File(args[0])
    val outputDir = if (args.size > 1) File(args[1]) else null

    if (!inputDir.exists() || !inputDir.isDirectory) {
        println("  Error: Directory not found: ${args[0]}")
        exitProcess(1)
    }

    outputDir?.mkdirs()

    val vlmFiles = inputDir.walkTopDown()
        .filter { it.isFile && it.extension == "vlm" }
        .toList()

    if (vlmFiles.isEmpty()) {
        println("  No .vlm files found in: ${inputDir.absolutePath}")
        exitProcess(0)
    }

    println("  Found ${vlmFiles.size} .vlm file(s) to decrypt")
    println("  ${"─".repeat(60)}")

    val codec = VSMCodec()
    val tempDir = File(System.getProperty("java.io.tmpdir"), "vlm_batch_${System.nanoTime()}")
    var successCount = 0
    var failCount = 0

    val totalElapsed = measureTimeMillis {
        for ((index, vlmFile) in vlmFiles.withIndex()) {
            val binName = vlmFile.nameWithoutExtension + ".bin"
            val targetDir = if (outputDir != null) {
                val relativePath = vlmFile.relativeTo(inputDir).parent
                val dir = if (relativePath != null) File(outputDir, relativePath) else outputDir
                dir.mkdirs()
                dir
            } else {
                vlmFile.parentFile
            }
            val binFile = File(targetDir, binName)

            print("  [${index + 1}/${vlmFiles.size}] ${vlmFile.name} → $binName ... ")

            var tempFile: java.io.File? = null
            val elapsedMs = measureTimeMillis {
                tempFile = codec.decryptToTempFile(vlmFile.absolutePath, tempDir)
                tempFile?.let {
                    it.copyTo(binFile, overwrite = true)
                    it.delete()
                }
            }
            if (tempFile != null) {
                successCount++
                println("OK (${formatBytes(binFile.length())}, ${formatDuration(elapsedMs)})")
            } else {
                failCount++
                println("FAILED")
            }
        }
    }

    tempDir.deleteRecursively()
    println("  ${"─".repeat(60)}")
    println("  Batch complete: $successCount succeeded, $failCount failed")
    println("  Total time: ${formatDuration(totalElapsed)}")
    println()
}

private fun handleVerify(args: List<String>) {
    if (args.isEmpty()) {
        println("  Error: File path required")
        println("  Usage: vlm-tool verify <file.vlm>")
        exitProcess(1)
    }

    val filePath = args[0]
    val file = File(filePath)

    if (!file.exists()) {
        println("  Error: File not found: $filePath")
        exitProcess(1)
    }

    println("  Verifying: $filePath")

    val codec = VSMCodec()
    val header = codec.readHeader(filePath)

    if (header == null) {
        println("  FAIL: Cannot read header (file too small or corrupted)")
        exitProcess(1)
    }

    if (!header.isValid()) {
        println("  FAIL: Invalid magic bytes (expected VSM1 / 0x56534D31)")
        exitProcess(1)
    }

    println("  Header:     VALID (VSM1)")
    println("  Encrypted:  ${if (header.isEncrypted()) "YES" else "NO"}")
    println("  Original:   ${formatBytes(header.originalSize)}")
    println("  Blocks:     ${header.blockCount}")

    // Full decode verification
    print("  Integrity:  ")
    val tempDir = File(System.getProperty("java.io.tmpdir"), "vlm_verify_${System.nanoTime()}")

    val elapsedMs = measureTimeMillis {
        val tempFile = codec.decryptToTempFile(filePath, tempDir)
        if (tempFile != null) {
            println("PASS (hash verified, ${formatBytes(tempFile.length())})")
            tempFile.delete()
        } else {
            println("FAIL (decryption or hash mismatch)")
            tempDir.deleteRecursively()
            exitProcess(1)
        }
    }

    tempDir.deleteRecursively()
    println("  Verified in ${formatDuration(elapsedMs)}")
    println()
}

private fun handleInfo(args: List<String>) {
    if (args.isEmpty()) {
        println("  Error: File path required")
        println("  Usage: vlm-tool info <file.vlm>")
        exitProcess(1)
    }

    val filePath = args[0]
    val file = File(filePath)

    if (!file.exists()) {
        println("  Error: File not found: $filePath")
        exitProcess(1)
    }

    val codec = VSMCodec()
    val header = codec.readHeader(filePath)

    if (header == null || !header.isValid()) {
        println("  Error: Not a valid VLM file")
        exitProcess(1)
    }

    val metadata = codec.readMetadata(filePath)

    println("  File:        ${file.name}")
    println("  File size:   ${formatBytes(file.length())}")
    println("  ${"─".repeat(40)}")
    println("  Magic:       0x${Integer.toHexString(header.magic).uppercase()} (${magicToString(header.magic)})")
    println("  Version:     ${header.version.toInt() shr 8}.${header.version.toInt() and 0xFF}")
    println("  Flags:       0x${header.flags.toString(16).padStart(4, '0')} ${flagsDescription(header.flags)}")
    println("  Original:    ${formatBytes(header.originalSize)}")
    println("  Encoded:     ${formatBytes(header.encodedSize)}")
    println("  Block size:  ${formatBytes(header.blockSize.toLong())}")
    println("  Blocks:      ${header.blockCount}")
    println("  File hash:   ${header.fileHash.joinToString("") { "%02x".format(it) }}")
    println("  Timestamp:   ${header.timestamp} (${java.util.Date(header.timestamp)})")
    println("  Content:     ${contentTypeName(header.contentType)}")

    if (metadata.isNotEmpty()) {
        println("  ${"─".repeat(40)}")
        println("  Metadata:")
        for ((key, value) in metadata) {
            println("    $key: $value")
        }
    }
    println()
}

// ============================================================================
// Helpers
// ============================================================================

/**
 * Determine the correct .vlm filename for a .bin file.
 * Tries enum lookup first (exact match by ggmlFileName),
 * then checks if the file is already named as a VoiceOS-*.bin,
 * and falls back to simple extension swap.
 */
private fun resolveVlmName(binFile: File): String {
    val name = binFile.name

    // 1. Match by ggml filename (e.g. "ggml-base.en.bin")
    val byGgml = WhisperModelSize.entries.firstOrNull { it.ggmlFileName == name }
    if (byGgml != null) return byGgml.vsmName

    // 2. Match by current VoiceOS-*.bin pattern (e.g. "VoiceOS-Bas-EN.bin")
    val byVsmBin = WhisperModelSize.entries.firstOrNull {
        it.vsmName.replace(".vlm", ".bin") == name
    }
    if (byVsmBin != null) return byVsmBin.vsmName

    // 3. Fallback: swap extension
    return name.replace(".bin", VSMFormat.VSM_EXTENSION)
}

/** Build metadata map from file, detecting known model sizes. */
private fun buildMetadata(file: File): Map<String, String> {
    val metadata = mutableMapOf<String, String>()

    // Try to identify the model
    val model = WhisperModelSize.entries.firstOrNull {
        it.ggmlFileName == file.name ||
        it.vsmName.replace(".vlm", ".bin") == file.name
    }

    if (model != null) {
        metadata["model"] = model.displayName
        metadata["size_mb"] = model.approxSizeMB.toString()
        metadata["lang"] = if (model.isEnglishOnly) "en" else "multilingual"
        metadata["vlm_name"] = model.vsmName
    }

    metadata["source_name"] = file.name
    metadata["source_size"] = file.length().toString()
    metadata["tool"] = "VLMTool/1.0"

    return metadata
}

private fun magicToString(magic: Int): String {
    val bytes = ByteArray(4)
    bytes[0] = (magic shr 24).toByte()
    bytes[1] = ((magic shr 16) and 0xFF).toByte()
    bytes[2] = ((magic shr 8) and 0xFF).toByte()
    bytes[3] = (magic and 0xFF).toByte()
    return String(bytes, Charsets.US_ASCII)
}

private fun flagsDescription(flags: Short): String {
    val parts = mutableListOf<String>()
    if (flags.toInt() and VSMFormat.FLAG_ENCRYPTED.toInt() != 0) parts.add("ENCRYPTED")
    if (flags.toInt() and VSMFormat.FLAG_COMPRESSED.toInt() != 0) parts.add("COMPRESSED")
    return if (parts.isEmpty()) "(none)" else parts.joinToString(" | ")
}

private fun contentTypeName(type: Int): String = when (type) {
    0 -> "Speech Model (Whisper/ggml)"
    else -> "Unknown ($type)"
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
}

private fun formatDuration(ms: Long): String = when {
    ms < 1000 -> "${ms}ms"
    ms < 60_000 -> "%.1fs".format(ms / 1000.0)
    else -> "%dm %ds".format(ms / 60_000, (ms % 60_000) / 1000)
}

private fun printUsage() {
    println("""
  Usage: vlm-tool <command> [options]

  Commands:
    encode <input.bin> <output.vlm>    Encrypt a model file to VLM format
    decode <input.vlm> <output.bin>    Decrypt a VLM file back to raw model
    batch-encode <dir> [output-dir]    Encrypt all .bin files in a directory
    batch-decode <dir> [output-dir]    Decrypt all .vlm files in a directory
    verify <file.vlm>                  Verify VLM file integrity (full decode + hash check)
    info <file.vlm>                    Show VLM file header and metadata
    help                               Show this help message

  Examples:
    vlm-tool encode VLMFiles/EN/VoiceOS-Bas-EN.bin VLMFiles/EN/VoiceOS-Bas-EN.vlm
    vlm-tool batch-encode VLMFiles/
    vlm-tool batch-encode VLMFiles/ /sdcard/ava-ai-models/vlm/
    vlm-tool verify VLMFiles/EN/VoiceOS-Bas-EN.vlm
    vlm-tool info VLMFiles/MUL/VoiceOS-Tin-MUL.vlm

  Gradle shortcut:
    ./gradlew :Modules:SpeechRecognition:runVlmTool --args="batch-encode VLMFiles/"
    """.trimIndent())
}
