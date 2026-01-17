/*
 * Copyright (c) 2025 Intelligent Devices LLC / Manoj Jhawar
 * All Rights Reserved - Confidential
 *
 * AVA Model Manager - CLI for AI model management
 * Install, manage, and encode AI models for AVA applications.
 */

package com.augmentalis.alc.cli

import com.augmentalis.alc.ava3.AVA3Decoder
import com.augmentalis.alc.ava3.AVA3Encoder
import com.augmentalis.alc.catalog.AVAModelCatalog
import com.augmentalis.alc.download.ModelRegistry
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

/**
 * AVA Model Manager
 *
 * Command-line tool for managing AVA AI models:
 * - Browse and install models from the AVA catalog
 * - Encode models with AVA3 for secure distribution
 * - Manage installed models
 *
 * Usage:
 *   ava-models <command> [options]
 *
 * Commands:
 *   catalog              Show available models in AVA catalog
 *   install <model>      Install a model from the catalog
 *   uninstall <model>    Uninstall a model
 *   list                 List installed models
 *   info <model>         Show model information
 *   search <query>       Search for models
 *   encode <input> <out> Encode a file with AVA3
 *   decode <input> <out> Decode an AVA3 file
 *   sync <repo-path>     Sync registry to repo
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage()
        exitProcess(1)
    }

    val command = args[0].lowercase()
    val remainingArgs = args.drop(1)

    try {
        when (command) {
            "catalog" -> handleCatalog()
            "install" -> handleInstall(remainingArgs)
            "uninstall" -> handleUninstall(remainingArgs)
            "list" -> handleList()
            "info" -> handleInfo(remainingArgs)
            "search" -> handleSearch(remainingArgs)
            "encode" -> handleEncode(remainingArgs)
            "decode" -> handleDecode(remainingArgs)
            "sync" -> handleSync(remainingArgs)
            "help", "-h", "--help" -> printUsage()
            else -> {
                println("Unknown command: $command")
                printUsage()
                exitProcess(1)
            }
        }
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}

private fun printUsage() {
    println("""
        AVA Model Manager v2.0

        Usage: ava-models <command> [options]

        Commands:
          catalog                      Browse available models in AVA catalog

          install <model-id>           Install a model from the catalog
                                       Example: ava-models install ava-nlu

          uninstall <model-id>         Uninstall an installed model

          list                         List installed models

          info <model-id>              Show detailed model information

          search <query>               Search models by name or capability

          encode <input> <output>      Encode a file with AVA3 format
                                       Input: path to model file
                                       Output: path for encoded file

          decode <input> <output>      Decode an AVA3 file
                                       Input: path to .ava3 file
                                       Output: path for decoded file

          sync <repo-path>             Sync registry to repo documentation

          help                         Show this help message

        Available Models:
          ava-nlu           Natural Language Understanding for voice commands
          ava-embeddings    Semantic embeddings for search and similarity
          ava-chat-lite     Lightweight conversational AI (1.1 GB)
          ava-chat          Full conversational AI with reasoning (2.3 GB)

        Examples:
          ava-models catalog                # Show all available models
          ava-models install ava-nlu        # Install NLU model
          ava-models search "chat"          # Search for chat models
          ava-models list                   # Show installed models
          ava-models info ava-embeddings    # Show model details
    """.trimIndent())
}

private fun handleCatalog() {
    val catalog = AVAModelCatalog()

    println("\nAVA Model Catalog")
    println("=".repeat(60))

    runBlocking {
        val models = catalog.getAvailableModels()

        if (models.isEmpty()) {
            println("No models available in catalog")
            return@runBlocking
        }

        // Group by category
        val byCategory = models.groupBy { it.entry.category }

        for ((category, categoryModels) in byCategory) {
            println("\n${category.name}")
            println("-".repeat(40))

            for (model in categoryModels) {
                val status = when (model.status) {
                    is AVAModelCatalog.InstallStatus.Installed -> "[installed]"
                    is AVAModelCatalog.InstallStatus.Installing -> "[installing]"
                    else -> ""
                }
                println("  ${model.entry.id.padEnd(20)} ${model.entry.sizeEstimate.padEnd(10)} $status")
                println("    ${model.entry.description}")
            }
        }

        println("\n" + "-".repeat(60))
        val installed = models.count { it.status is AVAModelCatalog.InstallStatus.Installed }
        println("Total: ${models.size} models, $installed installed")
        println("\nUse 'ava-models install <model-id>' to install a model")
    }

    catalog.close()
}

private fun handleInstall(args: List<String>) {
    if (args.isEmpty()) {
        println("Error: Model ID required")
        println("Usage: ava-models install <model-id>")
        println("\nUse 'ava-models catalog' to see available models")
        exitProcess(1)
    }

    val modelId = args[0].lowercase()
    val catalog = AVAModelCatalog()

    println("Installing model: $modelId")
    println("Preparing installation...")

    val callback = object : AVAModelCatalog.InstallCallback {
        override fun onProgress(modelId: String, progress: Int, message: String) {
            print("\r  $message ${progress}%".padEnd(60))
        }

        override fun onComplete(modelId: String, path: String) {
            println("\n\nInstallation complete!")
            println("  Model: $modelId")
            println("  Location: $path")
        }

        override fun onError(modelId: String, error: String) {
            println("\n\nInstallation failed: $error")
        }
    }

    runBlocking {
        val job = catalog.installModel(modelId, callback)
        job.join()
    }

    catalog.close()
}

private fun handleUninstall(args: List<String>) {
    if (args.isEmpty()) {
        println("Error: Model ID required")
        println("Usage: ava-models uninstall <model-id>")
        exitProcess(1)
    }

    val modelId = args[0].lowercase()
    val catalog = AVAModelCatalog()

    print("Uninstall model '$modelId'? (y/N): ")
    val confirm = readLine()?.lowercase()

    if (confirm != "y" && confirm != "yes") {
        println("Cancelled")
        catalog.close()
        return
    }

    if (catalog.uninstallModel(modelId)) {
        println("Model uninstalled: $modelId")
    } else {
        println("Model not found: $modelId")
    }

    catalog.close()
}

private fun handleSearch(args: List<String>) {
    if (args.isEmpty()) {
        println("Error: Search query required")
        println("Usage: ava-models search <query>")
        exitProcess(1)
    }

    val query = args.joinToString(" ")
    val catalog = AVAModelCatalog()

    println("Searching for: $query")
    println("-".repeat(40))

    runBlocking {
        val results = catalog.searchModels(query)

        if (results.isEmpty()) {
            println("No models found matching '$query'")
        } else {
            for (model in results) {
                val status = when (model.status) {
                    is AVAModelCatalog.InstallStatus.Installed -> "[installed]"
                    else -> ""
                }
                println("${model.entry.id} $status")
                println("  ${model.entry.name}: ${model.entry.description}")
                println("  Size: ${model.entry.sizeEstimate}")
                println()
            }
            println("Found ${results.size} model(s)")
        }
    }

    catalog.close()
}

private fun handleEncode(args: List<String>) {
    if (args.size < 2) {
        println("Error: Input and output paths required")
        println("Usage: alc-model-tool encode <input> <output>")
        exitProcess(1)
    }

    val inputPath = args[0]
    val outputPath = args[1]

    println("Encoding file...")
    println("  Input: $inputPath")
    println("  Output: $outputPath")

    val encoder = AVA3Encoder()
    val result = encoder.encode(inputPath, outputPath)

    println("Encoding complete!")
    println("  Output: $result")
    println("  Size: ${formatBytes(java.io.File(result).length())}")
}

private fun handleDecode(args: List<String>) {
    if (args.size < 2) {
        println("Error: Input and output paths required")
        println("Usage: alc-model-tool decode <input> <output>")
        exitProcess(1)
    }

    val inputPath = args[0]
    val outputPath = args[1]

    println("Decoding file...")
    println("  Input: $inputPath")
    println("  Output: $outputPath")

    val decoder = AVA3Decoder()
    val result = decoder.decodeToFile(inputPath, outputPath)

    println("Decoding complete!")
    println("  Output: $result")
    println("  Size: ${formatBytes(java.io.File(result).length())}")
}

private fun handleList() {
    val catalog = AVAModelCatalog()
    val models = catalog.getInstalledModels()

    if (models.isEmpty()) {
        println("No models installed")
        println("\nUse 'ava-models catalog' to see available models")
        println("Use 'ava-models install <model-id>' to install a model")
        catalog.close()
        return
    }

    println("Installed Models:")
    println("-".repeat(70))
    println("%-20s %-15s %-10s %-15s".format("ID", "Category", "Size", "Status"))
    println("-".repeat(70))

    for (model in models) {
        val status = when (val s = model.status) {
            is AVAModelCatalog.InstallStatus.Installed -> "v${s.version}"
            else -> "-"
        }
        println("%-20s %-15s %-10s %-15s".format(
            model.entry.id,
            model.entry.category.name,
            model.entry.sizeEstimate,
            status
        ))
    }

    println("-".repeat(70))
    println("Total: ${models.size} model(s) installed")

    catalog.close()
}

private fun handleInfo(args: List<String>) {
    if (args.isEmpty()) {
        println("Error: Model ID required")
        println("Usage: ava-models info <model-id>")
        exitProcess(1)
    }

    val modelId = args[0].lowercase()
    val catalog = AVAModelCatalog()
    val model = catalog.getModelInfo(modelId)

    if (model == null) {
        println("Model not found: $modelId")
        println("\nUse 'ava-models catalog' to see available models")
        catalog.close()
        exitProcess(1)
    }

    println("\nModel Information")
    println("=".repeat(50))
    println("  ID:          ${model.entry.id}")
    println("  Name:        ${model.entry.name}")
    println("  Description: ${model.entry.description}")
    println("  Category:    ${model.entry.category.name}")
    println("  Size:        ${model.entry.sizeEstimate}")
    println("  Version:     ${model.entry.version}")

    println("\n  Capabilities:")
    for (cap in model.entry.capabilities) {
        println("    - $cap")
    }

    println("\n  Status:")
    when (val status = model.status) {
        is AVAModelCatalog.InstallStatus.NotInstalled -> {
            println("    Not installed")
            println("\n    Use 'ava-models install ${model.entry.id}' to install")
        }
        is AVAModelCatalog.InstallStatus.Installing -> {
            println("    Installing... ${status.progress}%")
        }
        is AVAModelCatalog.InstallStatus.Installed -> {
            println("    Installed (v${status.version})")
            println("    Path: ${status.path}")
        }
        is AVAModelCatalog.InstallStatus.Error -> {
            println("    Error: ${status.message}")
        }
    }

    catalog.close()
}

private fun handleSync(args: List<String>) {
    if (args.isEmpty()) {
        println("Error: Repository path required")
        println("Usage: alc-model-tool sync <repo-path>")
        exitProcess(1)
    }

    val repoPath = args[0]
    val registry = ModelRegistry()

    println("Syncing registry to: $repoPath")

    if (registry.syncToRepo(repoPath)) {
        println("Registry synced successfully")
        println("  Output: $repoPath/docs/models/MODEL-REGISTRY.md")
    } else {
        println("Failed to sync registry")
        exitProcess(1)
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
