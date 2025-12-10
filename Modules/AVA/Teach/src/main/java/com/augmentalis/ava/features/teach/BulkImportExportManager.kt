package com.augmentalis.ava.features.teach

import android.content.Context
import android.net.Uri
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.TrainExample
import com.augmentalis.ava.core.domain.model.TrainExampleSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.BufferedReader
import java.io.BufferedWriter
import java.security.MessageDigest

/**
 * Phase 1.1: Bulk Import/Export Manager
 *
 * Handles importing and exporting training examples in JSON and CSV formats.
 * Includes validation and duplicate detection using existing MD5 hash system.
 */
class BulkImportExportManager(
    private val context: Context
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Export all training examples to JSON
     * @param examples List of training examples to export
     * @param uri Output file URI
     * @return Result with success/error
     */
    suspend fun exportToJson(
        examples: List<TrainExample>,
        uri: Uri
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val exportData = examples.map { it.toExportData() }
            val jsonString = json.encodeToString(exportData)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(outputStream.writer()).use { writer ->
                    writer.write(jsonString)
                }
            } ?: return@withContext Result.Error(Exception("Failed to open output stream"))

            Result.Success(examples.size)
        } catch (e: Exception) {
            Result.Error(e, "Export failed: ${e.message}")
        }
    }

    /**
     * Export all training examples to CSV
     * @param examples List of training examples to export
     * @param uri Output file URI
     * @return Result with success/error
     */
    suspend fun exportToCsv(
        examples: List<TrainExample>,
        uri: Uri
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(outputStream.writer()).use { writer ->
                    // Write CSV header
                    writer.write("utterance,intent,locale,source,created_at,usage_count\n")

                    // Write data rows
                    examples.forEach { example ->
                        val escapedUtterance = escapeCsvField(example.utterance)
                        val escapedIntent = escapeCsvField(example.intent)
                        writer.write("$escapedUtterance,$escapedIntent,${example.locale},${example.source},${example.createdAt},${example.usageCount}\n")
                    }
                }
            } ?: return@withContext Result.Error(Exception("Failed to open output stream"))

            Result.Success(examples.size)
        } catch (e: Exception) {
            Result.Error(e, "CSV export failed: ${e.message}")
        }
    }

    /**
     * Import training examples from JSON
     * @param uri Input file URI
     * @param existingHashes Set of existing example hashes for duplicate detection
     * @return Result with imported examples and stats
     */
    suspend fun importFromJson(
        uri: Uri,
        existingHashes: Set<String>
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(inputStream.reader()).use { it.readText() }
            } ?: return@withContext Result.Error(Exception("Failed to open input stream"))

            val exportDataList = json.decodeFromString<List<TrainExampleExportData>>(jsonString)

            val validationResult = validateAndDeduplicateImport(exportDataList, existingHashes)

            Result.Success(validationResult)
        } catch (e: Exception) {
            Result.Error(e, "JSON import failed: ${e.message}")
        }
    }

    /**
     * Import training examples from CSV
     * @param uri Input file URI
     * @param existingHashes Set of existing example hashes for duplicate detection
     * @return Result with imported examples and stats
     */
    suspend fun importFromCsv(
        uri: Uri,
        existingHashes: Set<String>
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            val exportDataList = mutableListOf<TrainExampleExportData>()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(inputStream.reader()).use { reader ->
                    // Skip header
                    reader.readLine()

                    // Read data rows
                    var line: String? = reader.readLine()
                    while (line != null) {
                        val fields = parseCsvLine(line)
                        if (fields.size >= 4) {
                            val utterance = fields[0]
                            val intent = fields[1]
                            val locale = fields.getOrNull(2) ?: "en-US"
                            val source = fields.getOrNull(3) ?: "MANUAL"
                            val createdAt = fields.getOrNull(4)?.toLongOrNull() ?: System.currentTimeMillis()
                            val usageCount = fields.getOrNull(5)?.toIntOrNull() ?: 0

                            exportDataList.add(
                                TrainExampleExportData(
                                    utterance = utterance,
                                    intent = intent,
                                    locale = locale,
                                    source = source,
                                    createdAt = createdAt,
                                    usageCount = usageCount
                                )
                            )
                        }
                        line = reader.readLine()
                    }
                }
            } ?: return@withContext Result.Error(Exception("Failed to open input stream"))

            val validationResult = validateAndDeduplicateImport(exportDataList, existingHashes)

            Result.Success(validationResult)
        } catch (e: Exception) {
            Result.Error(e, "CSV import failed: ${e.message}")
        }
    }

    /**
     * Validate imported data and detect duplicates
     */
    private fun validateAndDeduplicateImport(
        exportDataList: List<TrainExampleExportData>,
        existingHashes: Set<String>
    ): ImportResult {
        val validExamples = mutableListOf<TrainExample>()
        val duplicates = mutableListOf<TrainExampleExportData>()
        val invalid = mutableListOf<Pair<TrainExampleExportData, String>>()

        exportDataList.forEach { exportData ->
            // Validate fields
            val validationError = validateExportData(exportData)
            if (validationError != null) {
                invalid.add(exportData to validationError)
                return@forEach
            }

            // Calculate hash
            val hash = calculateMD5Hash("${exportData.utterance}${exportData.intent}")

            // Check for duplicates
            if (hash in existingHashes) {
                duplicates.add(exportData)
                return@forEach
            }

            // Create TrainExample
            val example = TrainExample(
                id = 0, // Will be auto-generated
                exampleHash = hash,
                utterance = exportData.utterance,
                intent = exportData.intent,
                locale = exportData.locale,
                source = parseSource(exportData.source),
                createdAt = exportData.createdAt,
                usageCount = exportData.usageCount,
                lastUsed = null
            )

            validExamples.add(example)
        }

        return ImportResult(
            validExamples = validExamples,
            duplicateCount = duplicates.size,
            invalidCount = invalid.size,
            totalProcessed = exportDataList.size,
            invalidExamples = invalid
        )
    }

    /**
     * Validate export data fields
     */
    private fun validateExportData(data: TrainExampleExportData): String? {
        return when {
            data.utterance.isBlank() -> "Utterance cannot be empty"
            data.intent.isBlank() -> "Intent cannot be empty"
            data.utterance.length > 500 -> "Utterance too long (max 500 characters)"
            data.intent.length > 100 -> "Intent too long (max 100 characters)"
            !isValidLocale(data.locale) -> "Invalid locale format: ${data.locale}"
            else -> null
        }
    }

    /**
     * Validate locale format (basic check)
     */
    private fun isValidLocale(locale: String): Boolean {
        return locale.matches(Regex("[a-z]{2}-[A-Z]{2}"))
    }

    /**
     * Parse source string to enum
     */
    private fun parseSource(source: String): TrainExampleSource {
        return try {
            TrainExampleSource.valueOf(source)
        } catch (e: IllegalArgumentException) {
            TrainExampleSource.MANUAL
        }
    }

    /**
     * Calculate MD5 hash (same as existing system)
     */
    private fun calculateMD5Hash(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Escape CSV field (handle commas and quotes)
     */
    private fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }

    /**
     * Parse CSV line (handle quoted fields)
     */
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        var currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        // Escaped quote
                        currentField.append('"')
                        i++
                    } else {
                        // Toggle quote mode
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    // Field separator
                    fields.add(currentField.toString())
                    currentField = StringBuilder()
                }
                else -> {
                    currentField.append(char)
                }
            }
            i++
        }

        // Add last field
        fields.add(currentField.toString())

        return fields
    }

    /**
     * Convert TrainExample to export data
     */
    private fun TrainExample.toExportData(): TrainExampleExportData {
        return TrainExampleExportData(
            utterance = utterance,
            intent = intent,
            locale = locale,
            source = source.name,
            createdAt = createdAt,
            usageCount = usageCount
        )
    }
}

/**
 * Serializable export data format
 */
@Serializable
data class TrainExampleExportData(
    val utterance: String,
    val intent: String,
    val locale: String = "en-US",
    val source: String = "MANUAL",
    val createdAt: Long = System.currentTimeMillis(),
    val usageCount: Int = 0
)

/**
 * Import result with statistics
 */
data class ImportResult(
    val validExamples: List<TrainExample>,
    val duplicateCount: Int,
    val invalidCount: Int,
    val totalProcessed: Int,
    val invalidExamples: List<Pair<TrainExampleExportData, String>>
) {
    val successCount: Int get() = validExamples.size
    val successRate: Float get() = if (totalProcessed > 0) successCount.toFloat() / totalProcessed else 0f
}
