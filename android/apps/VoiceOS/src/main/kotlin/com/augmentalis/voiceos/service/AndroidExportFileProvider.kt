package com.augmentalis.voiceos.service

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.augmentalis.voiceoscore.IExportFileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Android implementation of [IExportFileProvider] using Storage Access Framework.
 *
 * For export: Creates files in app's external files directory or uses SAF for user-selected location.
 * For import: Reads from URIs provided by document picker.
 *
 * @property context Android context for file operations
 */
class AndroidExportFileProvider(
    private val context: Context
) : IExportFileProvider {

    override suspend fun writeExport(
        data: String,
        suggestedName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Write to app's external files directory
            val exportDir = getExportDirectory()
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val file = File(exportDir, suggestedName)
            file.writeText(data, Charsets.UTF_8)

            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun readImport(fileUri: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(fileUri)

            val content = if (uri.scheme == "content") {
                // Content URI from document picker
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                        reader.readText()
                    }
                } ?: throw IllegalStateException("Could not open input stream for $fileUri")
            } else {
                // File path
                File(fileUri).readText(Charsets.UTF_8)
            }

            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getDefaultExportDirectory(): String {
        return getExportDirectory().absolutePath
    }

    /**
     * Write export to a URI provided by SAF document picker.
     * Call this when user selects a location via Intent.ACTION_CREATE_DOCUMENT.
     *
     * @param uri URI from SAF
     * @param data JSON data to write
     * @return Result indicating success or failure
     */
    suspend fun writeToUri(uri: Uri, data: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(data)
                }
            } ?: throw IllegalStateException("Could not open output stream for $uri")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * List existing export files in the default directory.
     * Useful for showing recent exports to the user.
     */
    fun listExportFiles(): List<ExportFileInfo> {
        val exportDir = getExportDirectory()
        if (!exportDir.exists()) return emptyList()

        return exportDir.listFiles { file ->
            file.isFile && file.name.endsWith(".json")
        }?.map { file ->
            ExportFileInfo(
                name = file.name,
                path = file.absolutePath,
                size = file.length(),
                lastModified = file.lastModified()
            )
        }?.sortedByDescending { it.lastModified } ?: emptyList()
    }

    /**
     * Delete an export file.
     */
    fun deleteExportFile(path: String): Boolean {
        return try {
            File(path).delete()
        } catch (e: Exception) {
            false
        }
    }

    private fun getExportDirectory(): File {
        // Use app's external files directory for exports
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        return File(externalDir, "voiceos_exports")
    }
}

/**
 * Information about an export file.
 */
data class ExportFileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long
)
