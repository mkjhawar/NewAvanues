// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/embeddings/AONWrapperTool.kt
// created: 2025-11-23
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.embeddings

import java.io.File

/**
 * Command-line tool for wrapping ONNX models into AVA-AON format
 *
 * Usage:
 * ```bash
 * # Wrap a single model
 * ./gradlew :Universal:AVA:Features:RAG:runAONWrapper \
 *   --args="wrap \
 *   --input=/path/to/model.onnx \
 *   --output=/path/to/model.aon \
 *   --model-id=AVA-384-Base-INT8 \
 *   --version=1"
 *
 * # Batch wrap all models in directory
 * ./gradlew :Universal:AVA:Features:RAG:runAONWrapper \
 *   --args="batch \
 *   --input-dir=/path/to/onnx/models \
 *   --output-dir=/path/to/aon/models"
 *
 * # Verify AON file integrity
 * ./gradlew :Universal:AVA:Features:RAG:runAONWrapper \
 *   --args="verify --file=/path/to/model.aon"
 * ```
 */
object AONWrapperTool {

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            printUsage()
            return
        }

        when (args[0]) {
            "wrap" -> wrapSingle(args)
            "batch" -> wrapBatch(args)
            "verify" -> verifyFile(args)
            "unwrap" -> unwrapFile(args)
            else -> {
                println("Unknown command: ${args[0]}")
                printUsage()
            }
        }
    }

    private fun wrapSingle(args: Array<String>) {
        val params = parseArgs(args)

        val inputFile = File(params["input"] ?: error("--input required"))
        val outputFile = File(params["output"] ?: error("--output required"))
        val modelId = params["model-id"] ?: error("--model-id required")
        val modelVersion = params["version"]?.toIntOrNull() ?: 1
        val expiryDays = params["expiry-days"]?.toIntOrNull() ?: 0
        val licenseTier = params["license"]?.toIntOrNull() ?: 0
        val encrypt = params["encrypt"]?.toBoolean() ?: false

        val expiryTimestamp = if (expiryDays > 0) {
            (System.currentTimeMillis() / 1000) + (expiryDays * 86400L)
        } else {
            0L
        }

        println("Wrapping ONNX model:")
        println("  Input:        ${inputFile.absolutePath}")
        println("  Output:       ${outputFile.absolutePath}")
        println("  Model ID:     $modelId")
        println("  Version:      $modelVersion")
        println("  License:      $licenseTier")
        println("  Expiry:       ${if (expiryDays > 0) "$expiryDays days" else "Never"}")
        println("  Encryption:   ${if (encrypt) "Yes" else "No"}")
        println()

        val result = AONFileManager.wrapONNX(
            onnxFile = inputFile,
            outputFile = outputFile,
            modelId = modelId,
            modelVersion = modelVersion,
            expiryTimestamp = expiryTimestamp,
            licenseTier = licenseTier,
            encrypt = encrypt
        )

        val sizeDiff = result.length() - inputFile.length()
        val overhead = HEADER_SIZE + FOOTER_SIZE

        println("✓ Successfully created AON file")
        println("  Original size: ${inputFile.length() / 1024} KB")
        println("  AON size:      ${result.length() / 1024} KB")
        println("  Overhead:      $overhead bytes (header + footer)")
        println()
    }

    private fun wrapBatch(args: Array<String>) {
        val params = parseArgs(args)

        val inputDir = File(params["input-dir"] ?: error("--input-dir required"))
        val outputDir = File(params["output-dir"] ?: error("--output-dir required"))

        if (!inputDir.exists() || !inputDir.isDirectory) {
            error("Input directory does not exist: ${inputDir.path}")
        }

        outputDir.mkdirs()

        val onnxFiles = inputDir.listFiles { file ->
            file.extension.lowercase() == "onnx"
        } ?: emptyArray()

        println("Found ${onnxFiles.size} ONNX files in ${inputDir.path}")
        println()

        onnxFiles.forEachIndexed { index, onnxFile ->
            val modelId = onnxFile.nameWithoutExtension
            val outputFile = File(outputDir, "${onnxFile.nameWithoutExtension}.aon")

            println("[${index + 1}/${onnxFiles.size}] Processing: ${onnxFile.name}")

            try {
                AONFileManager.wrapONNX(
                    onnxFile = onnxFile,
                    outputFile = outputFile,
                    modelId = modelId
                )
                println("  ✓ Created: ${outputFile.name}")
            } catch (e: Exception) {
                println("  ✗ Failed: ${e.message}")
            }
            println()
        }

        println("Batch wrapping complete!")
    }

    private fun verifyFile(args: Array<String>) {
        val params = parseArgs(args)
        val file = File(params["file"] ?: error("--file required"))

        if (!file.exists()) {
            error("File does not exist: ${file.path}")
        }

        println("Verifying AON file: ${file.name}")
        println()

        try {
            val isAON = AONFileManager.isAONFile(file)
            if (!isAON) {
                println("✗ Not a valid AON file (bad magic bytes)")
                return
            }

            println("✓ Valid AON file")
            println("  File size:    ${file.length() / 1024} KB")
            println()

            // Note: Full unwrap requires Android Context, so we can only do basic checks here
            println("⚠ Full integrity check requires Android context")
            println("  Use unwrap command in Android app to fully verify")

        } catch (e: Exception) {
            println("✗ Verification failed: ${e.message}")
        }
    }

    private fun unwrapFile(args: Array<String>) {
        val params = parseArgs(args)
        val inputFile = File(params["input"] ?: error("--input required"))
        val outputFile = File(params["output"] ?: error("--output required"))

        println("⚠ Unwrap requires Android context for package verification")
        println("  This command only extracts ONNX data without security checks")
        println()

        if (!inputFile.exists()) {
            error("File does not exist: ${inputFile.path}")
        }

        try {
            // Read raw ONNX data (skip header/footer)
            val fileBytes = inputFile.readBytes()
            val onnxDataOffset = HEADER_SIZE
            val onnxDataSize = fileBytes.size - HEADER_SIZE - FOOTER_SIZE

            if (onnxDataSize <= 0) {
                error("Invalid AON file (too small)")
            }

            val onnxData = fileBytes.copyOfRange(onnxDataOffset, onnxDataOffset + onnxDataSize)
            outputFile.writeBytes(onnxData)

            println("✓ Extracted ONNX data")
            println("  Output:       ${outputFile.absolutePath}")
            println("  Size:         ${outputFile.length() / 1024} KB")
            println()
            println("⚠ This ONNX file has NOT been verified!")
            println("  Use only for testing/debugging")

        } catch (e: Exception) {
            println("✗ Unwrap failed: ${e.message}")
        }
    }

    private fun parseArgs(args: Array<String>): Map<String, String> {
        val params = mutableMapOf<String, String>()

        args.drop(1).forEach { arg ->
            if (arg.startsWith("--")) {
                val parts = arg.substring(2).split("=", limit = 2)
                if (parts.size == 2) {
                    params[parts[0]] = parts[1]
                }
            }
        }

        return params
    }

    private fun printUsage() {
        println("""
        |AVA-AON Wrapper Tool v1.0
        |
        |Commands:
        |  wrap        Wrap a single ONNX file
        |  batch       Batch wrap all ONNX files in directory
        |  verify      Verify AON file integrity
        |  unwrap      Extract ONNX data (for debugging)
        |
        |Usage:
        |  wrap --input=<file> --output=<file> --model-id=<id> [options]
        |    Options:
        |      --version=<int>         Model version (default: 1)
        |      --expiry-days=<int>     Expiry in days (default: 0 = never)
        |      --license=<0|1|2>       License tier (default: 0)
        |      --encrypt=<true|false>  Enable encryption (default: false)
        |
        |  batch --input-dir=<dir> --output-dir=<dir>
        |
        |  verify --file=<file>
        |
        |  unwrap --input=<file> --output=<file>
        |
        |Examples:
        |  # Wrap model with 1-year expiry
        |  wrap --input=model.onnx --output=model.aon \
        |       --model-id=AVA-384-Base-INT8 --expiry-days=365
        |
        |  # Batch wrap all models
        |  batch --input-dir=./onnx-models --output-dir=./aon-models
        |
        |  # Verify AON file
        |  verify --file=model.aon
        """.trimMargin())
    }

    // Constants from AONFileManager
    private const val HEADER_SIZE = 256
    private const val FOOTER_SIZE = 128
}
