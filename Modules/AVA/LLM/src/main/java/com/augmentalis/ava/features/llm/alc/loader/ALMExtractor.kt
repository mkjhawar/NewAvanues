/**
 * AVA Model Archive Extractor
 *
 * Single Responsibility: Extract AVA model archive files
 *
 * Supports AVA 3-character extension scheme (v2.0):
 * - .amm = MLC-LLM models (TVM compiled)
 * - .amg = GGUF models (llama.cpp)
 * - .amr = LiteRT models (Google AI Edge)
 * - .ALM = Legacy format (backward compatibility)
 *
 * Automatically detects and extracts archives to make models
 * ready for runtime loading. Runs on app startup or when model
 * directories are accessed.
 *
 * Created: 2025-11-24
 * Updated: 2025-12-01 (v2.0 extension scheme)
 */

package com.augmentalis.ava.features.llm.alc.loader

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.utils.IOUtils

/**
 * Utility for extracting AVA model archive files
 *
 * AVA 3-character extension scheme (v2.0):
 * - .amm files (MLC) contain: AVA-{MODEL}.adm, AVALibrary.adm, config, tokenizer, weights
 * - .amg files (GGUF) contain: model.gguf, manifest.json, tokenizer.ats
 * - .amr files (LiteRT) contain: model.task, manifest.json, tokenizer.ats
 * - .ALM files (legacy) are still supported for backward compatibility
 */
class ALMExtractor(
    private val context: Context
) {

    companion object {
        // Supported archive extensions (v2.0 + legacy)
        private val ARCHIVE_EXTENSIONS = listOf(
            ".amm",   // Ava Model MLC
            ".amg",   // Ava Model GGUF
            ".amr",   // Ava Model liteRT
            ".ALM"    // Legacy format
        )
        private const val EXTRACTION_MARKER = ".ava_extracted"
        private const val ALM_EXTENSION = ".ALM"

        // Standard locations to scan for model archives
        private val SCAN_LOCATIONS = listOf(
            "/sdcard/ava-ai-models/llm",        // MLC models
            "/sdcard/ava-ai-models/llm-gguf",   // GGUF models
            "/sdcard/ava-ai-models/llm-litert", // LiteRT models
            "ava-ai-models/llm",                // Relative to external files dir
            "ava-ai-models/llm-gguf",
            "ava-ai-models/llm-litert",
            "models/llm"                        // Fallback location
        )
    }

    /**
     * Scan and extract all model archives in standard locations
     *
     * @return List of extracted model directories
     */
    suspend fun extractAllALMFiles(): List<File> = withContext(Dispatchers.IO) {
        val extractedDirs = mutableListOf<File>()

        for (location in getScanLocations()) {
            if (location.exists() && location.isDirectory) {
                Timber.d("Scanning for model archives in: ${location.absolutePath}")
                val extracted = scanAndExtractDirectory(location)
                extractedDirs.addAll(extracted)
            }
        }

        if (extractedDirs.isEmpty()) {
            Timber.i("No model archives found to extract")
        } else {
            Timber.i("Extracted ${extractedDirs.size} model archive(s)")
        }

        return@withContext extractedDirs
    }

    /**
     * Check if a file is a supported archive format
     */
    private fun isArchiveFile(file: File): Boolean {
        return ARCHIVE_EXTENSIONS.any { ext ->
            file.name.endsWith(ext, ignoreCase = true)
        }
    }

    /**
     * Extract a specific model archive file (.amm, .amg, .amr, .ALM)
     *
     * @param almFile The model archive file
     * @return The extracted directory, or null if extraction failed
     */
    suspend fun extractALMFile(almFile: File): File? = withContext(Dispatchers.IO) {
        try {
            if (!almFile.exists()) {
                Timber.w("Archive file does not exist: ${almFile.absolutePath}")
                return@withContext null
            }

            if (!isArchiveFile(almFile)) {
                Timber.w("Not a supported archive format: ${almFile.name}")
                return@withContext null
            }

            // Determine extraction directory (same name without extension)
            val modelName = almFile.nameWithoutExtension
            val extractDir = File(almFile.parent, modelName)

            // Check if already extracted
            if (isAlreadyExtracted(almFile, extractDir)) {
                Timber.d("Archive already extracted: ${almFile.name} -> ${extractDir.name}")
                return@withContext extractDir
            }

            // Create extraction directory
            if (extractDir.exists()) {
                Timber.w("Extraction directory exists, cleaning: ${extractDir.absolutePath}")
                extractDir.deleteRecursively()
            }
            extractDir.mkdirs()

            // Extract tar archive
            Timber.i("Extracting ALM: ${almFile.name} -> ${extractDir.name}")
            extractTarArchive(almFile, extractDir)

            // Create extraction marker
            createExtractionMarker(almFile, extractDir)

            // Verify extraction
            if (verifyExtraction(extractDir)) {
                Timber.i("Successfully extracted ALM: ${almFile.name}")
                return@withContext extractDir
            } else {
                Timber.e("Extraction verification failed: ${almFile.name}")
                extractDir.deleteRecursively()
                return@withContext null
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to extract ALM file: ${almFile.absolutePath}")
            return@withContext null
        }
    }

    /**
     * Check if an .ALM file needs to be extracted
     *
     * @param modelDir The directory to check
     * @return True if .ALM file exists and needs extraction
     */
    fun needsExtraction(modelDir: File): Boolean {
        val almFile = File(modelDir.parent, "${modelDir.name}$ALM_EXTENSION")
        if (!almFile.exists()) return false

        // Check if already extracted
        return !isAlreadyExtracted(almFile, modelDir)
    }

    /**
     * Get list of locations to scan for .ALM files
     */
    private fun getScanLocations(): List<File> {
        val locations = mutableListOf<File>()

        // Absolute paths
        for (path in SCAN_LOCATIONS) {
            if (path.startsWith("/")) {
                locations.add(File(path))
            }
        }

        // Relative paths (to external files dir)
        val externalDir = context.getExternalFilesDir(null)
        val baseDir = externalDir ?: context.filesDir

        for (path in SCAN_LOCATIONS) {
            if (!path.startsWith("/")) {
                locations.add(File(baseDir, path))
            }
        }

        return locations
    }

    /**
     * Scan directory and extract all model archives
     */
    private suspend fun scanAndExtractDirectory(directory: File): List<File> {
        val extractedDirs = mutableListOf<File>()

        directory.listFiles()?.forEach { file ->
            if (file.isFile && isArchiveFile(file)) {
                Timber.d("Found model archive: ${file.name}")
                val extractedDir = extractALMFile(file)
                if (extractedDir != null) {
                    extractedDirs.add(extractedDir)
                }
            }
        }

        return extractedDirs
    }

    /**
     * Extract tar archive to directory
     */
    private fun extractTarArchive(tarFile: File, destDir: File) {
        FileInputStream(tarFile).use { fis ->
            TarArchiveInputStream(fis).use { tarIn ->
                var entry = tarIn.nextTarEntry

                while (entry != null) {
                    val outputFile = File(destDir, entry.name)

                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        // Ensure parent directory exists
                        outputFile.parentFile?.mkdirs()

                        // Extract file
                        FileOutputStream(outputFile).use { fos ->
                            IOUtils.copy(tarIn, fos)
                        }

                        // Preserve executable permissions if needed
                        if (entry.mode and 0x40 != 0) { // Owner execute bit
                            outputFile.setExecutable(true)
                        }
                    }

                    entry = tarIn.nextTarEntry
                }
            }
        }
    }

    /**
     * Check if ALM file is already extracted
     */
    private fun isAlreadyExtracted(almFile: File, extractDir: File): Boolean {
        if (!extractDir.exists()) return false

        // Check for extraction marker
        val markerFile = File(extractDir, EXTRACTION_MARKER)
        if (!markerFile.exists()) return false

        // Verify marker content matches ALM file timestamp
        try {
            val markerContent = markerFile.readText().trim()
            val expectedContent = "${almFile.lastModified()}"
            return markerContent == expectedContent
        } catch (e: Exception) {
            Timber.w(e, "Failed to read extraction marker")
            return false
        }
    }

    /**
     * Create extraction marker file
     */
    private fun createExtractionMarker(almFile: File, extractDir: File) {
        try {
            val markerFile = File(extractDir, EXTRACTION_MARKER)
            markerFile.writeText("${almFile.lastModified()}")
        } catch (e: Exception) {
            Timber.w(e, "Failed to create extraction marker")
        }
    }

    /**
     * Verify that extraction was successful
     *
     * Supports multiple formats:
     * - MLC (.adm, .ADco legacy)
     * - GGUF (.gguf files)
     * - LiteRT (.task files)
     */
    private fun verifyExtraction(extractDir: File): Boolean {
        if (!extractDir.exists() || !extractDir.isDirectory) return false

        // Check for MLC format files (.adm or .ADco legacy)
        val hasMlcDeviceCode = extractDir.listFiles()?.any {
            it.name.endsWith(".adm", ignoreCase = true) ||
            it.name.endsWith(".ADco", ignoreCase = true)
        } ?: false

        val hasMlcLibrary = File(extractDir, "AVALibrary.adm").exists() ||
            File(extractDir, "AVALibrary.ADco").exists()

        // Check for GGUF format files
        val hasGgufModel = extractDir.listFiles()?.any {
            it.name.endsWith(".gguf", ignoreCase = true)
        } ?: false

        // Check for LiteRT format files
        val hasLiteRTModel = extractDir.listFiles()?.any {
            it.name.endsWith(".task", ignoreCase = true) ||
            it.name.endsWith(".tflite", ignoreCase = true)
        } ?: false

        // Check for tokenizer (any format)
        val hasTokenizer = extractDir.listFiles()?.any {
            it.name.startsWith("tokenizer") ||
            it.name.endsWith(".ats", ignoreCase = true) ||
            it.name.endsWith(".ath", ignoreCase = true)
        } ?: false

        // Check for manifest (GGUF/LiteRT)
        val hasManifest = File(extractDir, "manifest.json").exists()

        // MLC format verification
        if (hasMlcDeviceCode && hasMlcLibrary) {
            if (!hasTokenizer) {
                Timber.w("MLC verification: No tokenizer files found")
                return false
            }
            return true
        }

        // GGUF format verification
        if (hasGgufModel) {
            if (!hasManifest) {
                Timber.w("GGUF verification: No manifest.json found (may be raw GGUF)")
            }
            return true  // Raw GGUF files are valid
        }

        // LiteRT format verification
        if (hasLiteRTModel) {
            if (!hasManifest) {
                Timber.w("LiteRT verification: No manifest.json found")
            }
            return true  // Task files are valid
        }

        Timber.w("Verification failed: No recognized model format found")
        return false
    }

    /**
     * Clean up model archives after successful extraction
     *
     * @param deleteAfterExtraction If true, delete archive files after extraction
     */
    suspend fun cleanupALMFiles(deleteAfterExtraction: Boolean = false) = withContext(Dispatchers.IO) {
        if (!deleteAfterExtraction) return@withContext

        for (location in getScanLocations()) {
            if (location.exists() && location.isDirectory) {
                location.listFiles()?.forEach { file ->
                    if (file.isFile && isArchiveFile(file)) {
                        val modelName = file.nameWithoutExtension
                        val extractDir = File(file.parent, modelName)

                        if (extractDir.exists() && isAlreadyExtracted(file, extractDir)) {
                            Timber.i("Deleting extracted archive: ${file.name}")
                            file.delete()
                        }
                    }
                }
            }
        }
    }

    /**
     * Get extraction status for a model
     */
    fun getExtractionStatus(modelName: String): ExtractionStatus {
        for (location in getScanLocations()) {
            if (!location.exists()) continue

            // Check all supported archive extensions
            for (ext in ARCHIVE_EXTENSIONS) {
                val archiveFile = File(location, "$modelName$ext")
                if (archiveFile.exists()) {
                    val extractDir = File(location, modelName)
                    if (isAlreadyExtracted(archiveFile, extractDir)) {
                        return ExtractionStatus.Extracted(extractDir)
                    } else {
                        return ExtractionStatus.NeedsExtraction(archiveFile)
                    }
                }
            }

            // Check if extracted directory exists without archive
            val extractDir = File(location, modelName)
            if (extractDir.exists()) {
                return ExtractionStatus.DirectoryExists(extractDir)
            }
        }

        return ExtractionStatus.NotFound
    }

    /**
     * Status of model extraction
     */
    sealed class ExtractionStatus {
        /** Model directory exists (no archive file) */
        data class DirectoryExists(val directory: File) : ExtractionStatus()

        /** Archive file exists and is already extracted */
        data class Extracted(val directory: File) : ExtractionStatus()

        /** Archive file exists but needs extraction */
        data class NeedsExtraction(val archiveFile: File) : ExtractionStatus()

        /** Neither archive file nor directory found */
        object NotFound : ExtractionStatus()
    }
}
