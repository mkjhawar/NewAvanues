package com.augmentalis.ava.features.nlu.ava.io

import android.util.Log
import com.augmentalis.ava.core.common.AVAException
import com.augmentalis.ava.features.nlu.ava.model.AvaFile
import com.augmentalis.ava.features.nlu.ava.model.AvaIntent
import com.augmentalis.ava.features.nlu.ava.parser.AvaFileParser
import java.io.File

/**
 * File I/O operations for .ava and .vos files
 *
 * Reads .ava (AVA) and .vos (VoiceOS) files from file system,
 * parses them, returns data objects.
 * Uses AvaFileParser for parsing logic.
 *
 * Supported formats:
 * - Universal Format v2.0 (.ava, .vos) - VCM text format
 * - VoiceOS JSON Format v1.0 (.vos) - Old JSON format with "commands" array
 */
class AvaFileReader {

    companion object {
        private const val TAG = "AvaFileReader"
        private const val AVA_ROOT = "/.ava"
        private const val CORE_DIR = "$AVA_ROOT/core"
        private const val VOICEOS_DIR = "$AVA_ROOT/voiceos"
        private const val USER_DIR = "$AVA_ROOT/user"

        /** Supported file extensions */
        private val SUPPORTED_EXTENSIONS = listOf("ava", "vos")
    }

    /**
     * Parse .ava file from JSON string
     *
     * @param jsonString JSON content of .ava file
     * @param source Source identifier (e.g., "ASSETS", "CORE", "USER")
     * @return List of intents with source set
     */
    fun parseAvaFile(jsonString: String, source: String): List<AvaIntent> {
        val avaFile = AvaFileParser.parse(jsonString)
        return avaFile.intents.map { intent ->
            intent.copy(source = source)
        }
    }

    /**
     * Load single .ava file
     */
    fun loadAvaFile(filePath: String): AvaFile {
        val file = File(filePath)
        if (!file.exists()) {
            throw AVAException.ResourceNotFoundException("File not found: $filePath")
        }

        val jsonString = file.readText()
        return AvaFileParser.parse(jsonString)
    }

    /**
     * Load all intents from a directory
     */
    fun loadIntentsFromDirectory(directoryPath: String, source: String): List<AvaIntent> {
        val directory = File(directoryPath)

        if (!directory.exists() || !directory.isDirectory) {
            Log.d(TAG, "Directory not found: $directoryPath")
            return emptyList()
        }

        val allFiles = directory.listFiles()
        if (allFiles == null) {
            Log.d(TAG, "Cannot list files in: $directoryPath")
            return emptyList()
        }

        val avaFiles = allFiles.filter { it.extension in SUPPORTED_EXTENSIONS }
        Log.d(TAG, "Found ${avaFiles.size} .ava/.vos files in $directoryPath")

        val allIntents = mutableListOf<AvaIntent>()

        for (file in avaFiles) {
            try {
                val avaFile = loadAvaFile(file.absolutePath)
                val intents = avaFile.intents.map { intent ->
                    intent.copy(source = source)
                }
                allIntents.addAll(intents)
                Log.d(TAG, "Loaded ${intents.size} intents from ${file.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load ${file.name}: ${e.message}", e)
            }
        }

        return allIntents
    }

    /**
     * Get list of available locales from core directory
     */
    fun getAvailableLocales(): List<String> {
        val coreDir = File(CORE_DIR)
        if (!coreDir.exists() || !coreDir.isDirectory) {
            return listOf("en-US")
        }

        val allFiles = coreDir.listFiles()
        if (allFiles == null) {
            return listOf("en-US")
        }

        val localeDirs = allFiles.filter { file ->
            file.isDirectory && file.name.matches(Regex("[a-z]{2}-[A-Z]{2}"))
        }

        val locales = localeDirs.map { it.name }
        return if (locales.isEmpty()) listOf("en-US") else locales
    }
}
