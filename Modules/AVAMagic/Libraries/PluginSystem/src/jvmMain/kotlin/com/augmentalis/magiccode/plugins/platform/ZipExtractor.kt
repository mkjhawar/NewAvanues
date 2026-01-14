package com.augmentalis.avacode.plugins.platform

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.io.FileInputStream

/**
 * JVM implementation of ZipExtractor using java.util.zip.
 */
actual class ZipExtractor {
    actual fun extractZip(zipPath: String, destPath: String): Boolean {
        val zipFile = File(zipPath)
        if (!zipFile.exists() || !zipFile.isFile) {
            throw IOException("ZIP file not found: $zipPath")
        }

        val destDir = File(destPath)
        if (!destDir.exists()) {
            destDir.mkdirs()
        }

        try {
            ZipFile(zipFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    val entryFile = File(destDir, entry.name)

                    // Security check: prevent zip slip vulnerability
                    if (!entryFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                        throw IOException("ZIP entry is outside target directory: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        entryFile.mkdirs()
                    } else {
                        // Ensure parent directories exist
                        entryFile.parentFile?.mkdirs()

                        // Extract file
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(entryFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            throw IOException("Failed to extract ZIP: $zipPath", e)
        }
    }

    actual fun listZipEntries(zipPath: String): List<String> {
        val zipFile = File(zipPath)
        if (!zipFile.exists() || !zipFile.isFile) {
            throw IOException("ZIP file not found: $zipPath")
        }

        try {
            ZipFile(zipFile).use { zip ->
                return zip.entries().asSequence()
                    .map { it.name }
                    .toList()
            }
        } catch (e: Exception) {
            throw IOException("Failed to list ZIP entries: $zipPath", e)
        }
    }

    actual fun containsEntry(zipPath: String, entryPath: String): Boolean {
        val zipFile = File(zipPath)
        if (!zipFile.exists() || !zipFile.isFile) {
            return false
        }

        try {
            ZipFile(zipFile).use { zip ->
                return zip.getEntry(entryPath) != null
            }
        } catch (e: Exception) {
            return false
        }
    }
}
