// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/embeddings/BatchModelWrapper.kt
// created: 2025-11-24
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.embeddings

import java.io.File

/**
 * Batch Model Wrapping Utility
 *
 * Wraps multiple ONNX embedding models into AVA-AON format for deployment.
 *
 * ## Extensibility
 * To add new models, simply add entries to the MODEL_REGISTRY list.
 *
 * ## Usage:
 * ```kotlin
 * val wrapper = BatchModelWrapper(
 *     inputDir = File("/path/to/onnx-models"),
 *     outputDir = File("/path/to/aon-models"),
 *     strategy = AONPackageManager.DistributionStrategy.AVA_STANDARD
 * )
 *
 * val results = wrapper.wrapAllModels()
 * wrapper.printSummary(results)
 * ```
 *
 * ## Command-line:
 * ```bash
 * # Using Gradle
 * ./gradlew :Universal:AVA:Features:RAG:run \
 *   --args="batch \
 *     --input /path/to/onnx-models \
 *     --output /path/to/aon-models \
 *     --strategy AVA_STANDARD"
 * ```
 */
class BatchModelWrapper(
    private val inputDir: File,
    private val outputDir: File,
    private val strategy: AONPackageManager.DistributionStrategy = AONPackageManager.DistributionStrategy.AVA_STANDARD,
    private val verbose: Boolean = false,
    private val dryRun: Boolean = false
) {

    /**
     * Model Registry Entry
     *
     * Add new models here to include them in batch processing.
     */
    data class ModelRegistryEntry(
        val modelId: String,
        val onnxFilename: String,
        val licenseTier: Int = 0,
        val modelVersion: Int = 1
    )

    companion object {
        /**
         * Model Registry (Extensible)
         *
         * To add a new model:
         * 1. Download ONNX model from HuggingFace
         * 2. Add entry to this list
         * 3. Place ONNX file in input directory
         * 4. Run batch wrapper
         */
        val MODEL_REGISTRY = listOf(
            // FREE TIER
            ModelRegistryEntry(
                modelId = "AVA-384-Base-INT8",
                onnxFilename = "all-MiniLM-L6-v2.onnx",
                licenseTier = 0,
                modelVersion = 1
            ),
            ModelRegistryEntry(
                modelId = "AVA-384-Fast-INT8",
                onnxFilename = "paraphrase-MiniLM-L3-v2.onnx",
                licenseTier = 0,
                modelVersion = 1
            ),

            // PRO TIER
            ModelRegistryEntry(
                modelId = "AVA-768-Qual-INT8",
                onnxFilename = "all-mpnet-base-v2.onnx",
                licenseTier = 1,
                modelVersion = 1
            ),
            ModelRegistryEntry(
                modelId = "AVA-384-Multi-INT8",
                onnxFilename = "paraphrase-multilingual-MiniLM-L12-v2.onnx",
                licenseTier = 1,
                modelVersion = 1
            ),
            ModelRegistryEntry(
                modelId = "AVA-768-Multi-INT8",
                onnxFilename = "paraphrase-multilingual-mpnet-base-v2.onnx",
                licenseTier = 1,
                modelVersion = 1
            ),
            ModelRegistryEntry(
                modelId = "AVA-384-ZH-INT8",
                onnxFilename = "text2vec-base-chinese.onnx",
                licenseTier = 1,
                modelVersion = 1
            ),
            ModelRegistryEntry(
                modelId = "AVA-768-JA-INT8",
                onnxFilename = "sentence-bert-base-ja-mean-tokens-v2.onnx",
                licenseTier = 1,
                modelVersion = 1
            )
        )

        /**
         * Example: Add a new model
         *
         * ```kotlin
         * // Add to MODEL_REGISTRY:
         * ModelRegistryEntry(
         *     modelId = "AVA-1024-Code-INT8",
         *     onnxFilename = "codebert-base.onnx",
         *     licenseTier = 1,
         *     modelVersion = 1
         * )
         * ```
         */
    }

    /**
     * Wrapping result for a single model
     */
    data class WrapResult(
        val modelId: String,
        val status: Status,
        val outputFile: File? = null,
        val error: String? = null
    ) {
        enum class Status {
            SUCCESS,
            SKIPPED,
            FAILED
        }
    }

    /**
     * Wrap all models in registry
     *
     * @return List of results for each model
     */
    fun wrapAllModels(): List<WrapResult> {
        printHeader()

        // Create output directories
        if (!dryRun) {
            outputDir.mkdirs()
            File(outputDir, "free").mkdirs()
            File(outputDir, "pro").mkdirs()
        }

        val results = MODEL_REGISTRY.mapIndexed { index, entry ->
            wrapSingleModel(entry, index + 1, MODEL_REGISTRY.size)
        }

        return results
    }

    /**
     * Wrap a single model from registry
     */
    private fun wrapSingleModel(
        entry: ModelRegistryEntry,
        current: Int,
        total: Int
    ): WrapResult {
        val tierName = if (entry.licenseTier == 0) "free" else "pro"
        val inputFile = File(inputDir, entry.onnxFilename)
        val outputFile = File(outputDir, "$tierName/${entry.modelId}.AON")

        println("\n[$current/$total] ${entry.modelId}")
        println("  ONNX:    ${entry.onnxFilename}")
        println("  Tier:    ${tierName.uppercase()} (${entry.licenseTier})")
        println("  Version: ${entry.modelVersion}")

        // Check if input exists
        if (!inputFile.exists()) {
            println("  ⚠ SKIPPED: Input file not found")
            return WrapResult(
                modelId = entry.modelId,
                status = WrapResult.Status.SKIPPED,
                error = "Input file not found: ${inputFile.absolutePath}"
            )
        }

        val inputSizeMB = inputFile.length() / (1024 * 1024)
        println("  Size:    ${inputSizeMB} MB")

        if (dryRun) {
            println("  ✓ WOULD WRAP → ${outputFile.absolutePath}")
            return WrapResult(
                modelId = entry.modelId,
                status = WrapResult.Status.SUCCESS,
                outputFile = outputFile
            )
        }

        // Perform wrapping
        return try {
            if (verbose) {
                println("  Running wrapper...")
            }

            val packages = AONPackageManager.getPackagesForStrategy(strategy)

            val result = AONFileManager.wrapONNX(
                onnxFile = inputFile,
                outputFile = outputFile,
                modelId = entry.modelId,
                modelVersion = entry.modelVersion,
                allowedPackages = packages,
                expiryTimestamp = 0,
                licenseTier = entry.licenseTier,
                encrypt = false
            )

            val outputSizeMB = result.length() / (1024 * 1024)
            println("  ✓ WRAPPED → ${result.absolutePath} (${outputSizeMB} MB)")

            WrapResult(
                modelId = entry.modelId,
                status = WrapResult.Status.SUCCESS,
                outputFile = result
            )
        } catch (e: Exception) {
            println("  ✗ FAILED: ${e.message}")
            if (verbose) {
                e.printStackTrace()
            }

            WrapResult(
                modelId = entry.modelId,
                status = WrapResult.Status.FAILED,
                error = e.message
            )
        }
    }

    /**
     * Print summary of results
     */
    fun printSummary(results: List<WrapResult>) {
        val successful = results.count { it.status == WrapResult.Status.SUCCESS }
        val skipped = results.count { it.status == WrapResult.Status.SKIPPED }
        val failed = results.count { it.status == WrapResult.Status.FAILED }

        println("\n╔════════════════════════════════════════════════════════════╗")
        println("║                       Summary                              ║")
        println("╚════════════════════════════════════════════════════════════╝\n")

        println("  Total models:     ${results.size}")
        println("  ✓ Wrapped:        $successful")
        println("  ⚠ Skipped:        $skipped")
        if (failed > 0) {
            println("  ✗ Failed:         $failed")
        }
        println()

        if (!dryRun && successful > 0) {
            println("Output directory: ${outputDir.absolutePath}")
            println("  free/  - Free tier models")
            println("  pro/   - Pro tier models")
            println()
        }

        // Print failed models
        if (failed > 0) {
            println("Failed models:")
            results.filter { it.status == WrapResult.Status.FAILED }.forEach {
                println("  - ${it.modelId}: ${it.error}")
            }
            println()
        }
    }

    private fun printHeader() {
        val packages = AONPackageManager.getPackagesForStrategy(strategy).joinToString(", ")

        println("╔════════════════════════════════════════════════════════════╗")
        println("║          AVA RAG Model Batch Wrapper v1.0                  ║")
        println("╚════════════════════════════════════════════════════════════╝\n")

        println("Configuration:")
        println("  Input directory:  ${inputDir.absolutePath}")
        println("  Output directory: ${outputDir.absolutePath}")
        println("  Strategy:         $strategy")
        println("  Packages:         $packages")
        println("  Dry run:          $dryRun")
        println()

        println("Processing ${MODEL_REGISTRY.size} models from registry...")
    }
}

/**
 * Command-line entry point
 *
 * Usage:
 * ```bash
 * ./gradlew :Universal:AVA:Features:RAG:run \
 *   --args="batch \
 *     --input /path/to/onnx \
 *     --output /path/to/aon \
 *     --strategy AVA_STANDARD \
 *     --verbose"
 * ```
 */
fun main(args: Array<String>) {
    if (args.isEmpty() || args[0] != "batch") {
        println("Usage: batch --input DIR --output DIR [OPTIONS]")
        println()
        println("OPTIONS:")
        println("  --input DIR      Input directory with ONNX models (required)")
        println("  --output DIR     Output directory for AON files (required)")
        println("  --strategy STR   Distribution strategy (default: AVA_STANDARD)")
        println("                   Options: AVA_STANDARD, AVANUES_PLATFORM, DEVELOPMENT, ALL_AVA")
        println("  --verbose        Enable verbose output")
        println("  --dry-run        Show what would be wrapped without doing it")
        println()
        println("EXAMPLES:")
        println("  # Wrap all models for standard AVA apps")
        println("  batch --input /models --output /wrapped")
        println()
        println("  # Wrap for Avanues platform with verbose output")
        println("  batch --input /models --output /wrapped --strategy AVANUES_PLATFORM --verbose")
        return
    }

    var inputDir: File? = null
    var outputDir: File? = null
    var strategy = AONPackageManager.DistributionStrategy.AVA_STANDARD
    var verbose = false
    var dryRun = false

    var i = 1
    while (i < args.size) {
        when (args[i]) {
            "--input" -> {
                inputDir = File(args[++i])
            }
            "--output" -> {
                outputDir = File(args[++i])
            }
            "--strategy" -> {
                strategy = AONPackageManager.DistributionStrategy.valueOf(args[++i])
            }
            "--verbose" -> {
                verbose = true
            }
            "--dry-run" -> {
                dryRun = true
            }
        }
        i++
    }

    if (inputDir == null || outputDir == null) {
        println("ERROR: Both --input and --output are required")
        return
    }

    if (!inputDir.exists()) {
        println("ERROR: Input directory does not exist: ${inputDir.absolutePath}")
        return
    }

    val wrapper = BatchModelWrapper(
        inputDir = inputDir,
        outputDir = outputDir,
        strategy = strategy,
        verbose = verbose,
        dryRun = dryRun
    )

    val results = wrapper.wrapAllModels()
    wrapper.printSummary(results)

    // Exit with error code if any failed
    val failed = results.count { it.status == BatchModelWrapper.WrapResult.Status.FAILED }
    if (failed > 0) {
        System.exit(1)
    }
}
