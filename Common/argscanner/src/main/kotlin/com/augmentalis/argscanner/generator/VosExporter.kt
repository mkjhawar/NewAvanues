package com.augmentalis.argscanner.generator

import android.content.Context
import android.os.Environment
import com.augmentalis.argscanner.models.ARScanSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * VosExporter - Exports generated DSL to .vos files
 *
 * Handles file system operations for saving AVAMagic UI DSL:
 * - Creates .vos files
 * - Manages export directories
 * - Supports multiple export formats (VOS, JSON, YAML)
 * - Handles storage permissions
 *
 * Created by: Manoj Jhawar, manoj@ideahq.net
 */
class VosExporter(private val context: Context) {

    companion object {
        private const val DEFAULT_DIR = "ARGScanner"
        private const val VOS_EXTENSION = ".vos"
        private const val JSON_EXTENSION = ".json"
        private const val YAML_EXTENSION = ".yaml"
    }

    /**
     * Export DSL to file
     *
     * @param session Scan session
     * @param dslContent Generated DSL content
     * @param format Export format
     * @return Exported file path
     */
    suspend fun export(
        session: ARScanSession,
        dslContent: String,
        format: ARScanSession.DSLFormat = ARScanSession.DSLFormat.AVAMAGIC
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            // Determine export directory
            val exportDir = getExportDirectory()
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            // Generate filename
            val filename = generateFilename(session, format)
            val file = File(exportDir, filename)

            // Write content
            FileOutputStream(file).use { output ->
                output.write(dslContent.toByteArray())
            }

            ExportResult.Success(
                filePath = file.absolutePath,
                fileSize = file.length()
            )
        } catch (e: Exception) {
            ExportResult.Error(
                message = "Export failed: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Export session with metadata
     *
     * Creates a complete export package with:
     * - DSL file (.vos)
     * - Metadata file (.json)
     * - Thumbnail (if available)
     */
    suspend fun exportWithMetadata(
        session: ARScanSession,
        dslContent: String,
        metadata: Map<String, Any> = emptyMap()
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val exportDir = getExportDirectory()
            val sessionDir = File(exportDir, sanitizeFilename(session.name))
            if (!sessionDir.exists()) {
                sessionDir.mkdirs()
            }

            // Export DSL file
            val dslFile = File(sessionDir, "${sanitizeFilename(session.name)}$VOS_EXTENSION")
            FileOutputStream(dslFile).use { output ->
                output.write(dslContent.toByteArray())
            }

            // Export metadata file
            val metadataFile = File(sessionDir, "metadata.json")
            val metadataJson = buildMetadataJson(session, metadata)
            FileOutputStream(metadataFile).use { output ->
                output.write(metadataJson.toByteArray())
            }

            // Create README
            val readmeFile = File(sessionDir, "README.txt")
            val readme = buildReadme(session)
            FileOutputStream(readmeFile).use { output ->
                output.write(readme.toByteArray())
            }

            ExportResult.Success(
                filePath = sessionDir.absolutePath,
                fileSize = calculateDirectorySize(sessionDir)
            )
        } catch (e: Exception) {
            ExportResult.Error(
                message = "Export with metadata failed: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Get export directory
     *
     * Uses app-specific external storage (Android 10+)
     */
    private fun getExportDirectory(): File {
        return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            // External storage available
            File(context.getExternalFilesDir(null), DEFAULT_DIR)
        } else {
            // Fall back to internal storage
            File(context.filesDir, DEFAULT_DIR)
        }
    }

    /**
     * Generate filename from session
     */
    private fun generateFilename(
        session: ARScanSession,
        format: ARScanSession.DSLFormat
    ): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(session.startTime))
        val baseName = sanitizeFilename(session.name)
        val extension = when (format) {
            ARScanSession.DSLFormat.AVAMAGIC -> VOS_EXTENSION
            ARScanSession.DSLFormat.JSON -> JSON_EXTENSION
            ARScanSession.DSLFormat.YAML -> YAML_EXTENSION
            ARScanSession.DSLFormat.XML -> ".xml"
        }

        return "${baseName}_${timestamp}${extension}"
    }

    /**
     * Sanitize filename (remove invalid characters)
     */
    private fun sanitizeFilename(name: String): String {
        return name
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .replace(Regex("_+"), "_")
            .take(50)  // Limit length
    }

    /**
     * Build metadata JSON
     */
    private fun buildMetadataJson(
        session: ARScanSession,
        additionalMetadata: Map<String, Any>
    ): String {
        return buildString {
            appendLine("{")
            appendLine("  \"sessionId\": \"${session.sessionId}\",")
            appendLine("  \"name\": \"${session.name}\",")
            appendLine("  \"description\": \"${session.description ?: ""}\",")
            appendLine("  \"startTime\": ${session.startTime},")
            appendLine("  \"endTime\": ${session.endTime ?: 0},")
            appendLine("  \"duration\": ${session.duration ?: 0},")
            appendLine("  \"environment\": \"${session.environment}\",")
            appendLine("  \"roomType\": \"${session.roomType ?: "UNKNOWN"}\",")
            appendLine("  \"totalObjects\": ${session.totalObjectsDetected},")
            appendLine("  \"confidentObjects\": ${session.confidentObjects},")
            appendLine("  \"totalRelationships\": ${session.totalRelationships},")
            appendLine("  \"trackingQuality\": \"${session.trackingQuality}\",")
            appendLine("  \"averageFps\": ${session.averageFps ?: 0.0},")
            appendLine("  \"dslFormat\": \"${session.dslFormat}\",")

            if (additionalMetadata.isNotEmpty()) {
                appendLine("  \"additionalMetadata\": {")
                additionalMetadata.entries.forEachIndexed { index, (key, value) ->
                    val comma = if (index < additionalMetadata.size - 1) "," else ""
                    appendLine("    \"$key\": \"$value\"$comma")
                }
                appendLine("  },")
            }

            appendLine("  \"exportedAt\": ${System.currentTimeMillis()}")
            appendLine("}")
        }
    }

    /**
     * Build README file
     */
    private fun buildReadme(session: ARScanSession): String {
        return buildString {
            appendLine("ARGScanner Export")
            appendLine("=================")
            appendLine()
            appendLine("Session: ${session.name}")
            appendLine("ID: ${session.sessionId}")
            appendLine()
            appendLine("Scan Details:")
            appendLine("- Environment: ${session.environment}")
            appendLine("- Room Type: ${session.roomType ?: "Unknown"}")
            appendLine("- Objects Detected: ${session.totalObjectsDetected} (${session.confidentObjects} confident)")
            appendLine("- Spatial Relationships: ${session.totalRelationships}")
            appendLine("- Duration: ${session.getDurationSeconds() ?: 0} seconds")
            appendLine("- Tracking Quality: ${session.trackingQuality}")
            appendLine()
            appendLine("Files:")
            appendLine("- ${sanitizeFilename(session.name)}.vos - AVAMagic UI DSL")
            appendLine("- metadata.json - Session metadata")
            appendLine("- README.txt - This file")
            appendLine()
            appendLine("Generated by: ARGScanner v1.0.0")
            appendLine("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        }
    }

    /**
     * Calculate directory size
     */
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        directory.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    /**
     * Delete exported file/directory
     */
    suspend fun deleteExport(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            file.deleteRecursively()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * List all exports
     */
    suspend fun listExports(): List<ExportInfo> = withContext(Dispatchers.IO) {
        try {
            val exportDir = getExportDirectory()
            if (!exportDir.exists()) {
                return@withContext emptyList()
            }

            exportDir.listFiles()
                ?.filter { it.isDirectory || it.extension in listOf("vos", "json", "yaml") }
                ?.map { file ->
                    ExportInfo(
                        path = file.absolutePath,
                        name = file.name,
                        size = if (file.isDirectory) calculateDirectorySize(file) else file.length(),
                        modifiedAt = file.lastModified(),
                        isDirectory = file.isDirectory
                    )
                }
                ?.sortedByDescending { it.modifiedAt }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Export result
     */
    sealed class ExportResult {
        data class Success(
            val filePath: String,
            val fileSize: Long
        ) : ExportResult()

        data class Error(
            val message: String,
            val exception: Exception
        ) : ExportResult()
    }

    /**
     * Export information
     */
    data class ExportInfo(
        val path: String,
        val name: String,
        val size: Long,
        val modifiedAt: Long,
        val isDirectory: Boolean
    )
}
