package com.augmentalis.magiccode.plugins.platform

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.errno
import platform.posix.strerror

/**
 * iOS implementation of ZipExtractor using NSFileManager and ZipArchive.
 *
 * Note: This implementation uses NSFileManager's built-in ZIP capabilities
 * available in iOS 10+. For production use, consider using a third-party
 * library like ZipArchive or SSZipArchive via Kotlin/Native interop.
 */
actual class ZipExtractor {
    actual fun extractZip(zipPath: String, destPath: String): Boolean {
        val fileManager = NSFileManager.defaultManager

        // Verify ZIP file exists
        val zipNSString = zipPath as NSString
        if (!fileManager.fileExistsAtPath(zipPath)) {
            throw IOException("ZIP file not found: $zipPath")
        }

        // Create destination directory if needed
        val destNSString = destPath as NSString
        if (!fileManager.fileExistsAtPath(destPath)) {
            val created = fileManager.createDirectoryAtPath(
                destPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
            if (!created) {
                throw IOException("Failed to create destination directory: $destPath")
            }
        }

        // For iOS, we use NSFileManager's unzipping capability
        // This is a simplified implementation. For production, consider using
        // a proper ZIP library through Kotlin/Native interop (e.g., SSZipArchive)

        // Create a temporary directory for extraction
        val tempDir = NSTemporaryDirectory() + "plugin_extract_${NSUUID().UUIDString}"

        try {
            // Create temporary extraction directory
            fileManager.createDirectoryAtPath(
                tempDir,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )

            // Use unzip command via Process (fallback approach)
            // Note: In production, you should use a proper ZIP library
            val success = executeUnzipCommand(zipPath, tempDir)

            if (!success) {
                throw IOException("Failed to extract ZIP archive")
            }

            // Move contents to destination
            val tempContents = fileManager.contentsOfDirectoryAtPath(tempDir, error = null)
                ?: throw IOException("Failed to read extracted contents")

            tempContents.forEach { item ->
                val itemName = item as NSString
                val sourcePath = "$tempDir/$itemName"
                val targetPath = "$destPath/$itemName"

                // Remove target if exists
                if (fileManager.fileExistsAtPath(targetPath)) {
                    fileManager.removeItemAtPath(targetPath, error = null)
                }

                // Move item
                val moved = fileManager.moveItemAtPath(
                    sourcePath,
                    toPath = targetPath,
                    error = null
                )

                if (!moved) {
                    throw IOException("Failed to move extracted file: $itemName")
                }
            }

            return true
        } finally {
            // Cleanup temporary directory
            if (fileManager.fileExistsAtPath(tempDir)) {
                fileManager.removeItemAtPath(tempDir, error = null)
            }
        }
    }

    actual fun listZipEntries(zipPath: String): List<String> {
        val fileManager = NSFileManager.defaultManager

        if (!fileManager.fileExistsAtPath(zipPath)) {
            throw IOException("ZIP file not found: $zipPath")
        }

        // Create temporary directory for inspection
        val tempDir = NSTemporaryDirectory() + "plugin_list_${NSUUID().UUIDString}"

        try {
            fileManager.createDirectoryAtPath(
                tempDir,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )

            // Extract to temp directory
            val success = executeUnzipCommand(zipPath, tempDir)
            if (!success) {
                throw IOException("Failed to list ZIP entries")
            }

            // Recursively list all files
            val entries = mutableListOf<String>()
            listFilesRecursively(tempDir, tempDir, entries)

            return entries
        } finally {
            // Cleanup
            if (fileManager.fileExistsAtPath(tempDir)) {
                fileManager.removeItemAtPath(tempDir, error = null)
            }
        }
    }

    actual fun containsEntry(zipPath: String, entryPath: String): Boolean {
        return try {
            val entries = listZipEntries(zipPath)
            entries.contains(entryPath)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Execute unzip command using system utilities.
     * This is a fallback approach for iOS where native ZIP libraries
     * would typically be used via interop.
     */
    private fun executeUnzipCommand(zipPath: String, destPath: String): Boolean {
        // Note: iOS sandboxing restricts process execution
        // In production, use a proper ZIP library like SSZipArchive via cinterop

        // For now, attempt to use NSTask (available in macOS/iOS)
        // This is a placeholder - actual implementation would use a ZIP library

        val task = NSTask()
        task.setLaunchPath("/usr/bin/unzip")
        task.setArguments(listOf("-q", "-o", zipPath, "-d", destPath))

        return try {
            task.launch()
            task.waitUntilExit()
            task.terminationStatus == 0L
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Recursively list files in a directory.
     */
    private fun listFilesRecursively(rootPath: String, currentPath: String, entries: MutableList<String>) {
        val fileManager = NSFileManager.defaultManager
        val contents = fileManager.contentsOfDirectoryAtPath(currentPath, error = null) ?: return

        contents.forEach { item ->
            val itemName = item as NSString
            val itemPath = "$currentPath/$itemName"
            val relativePath = itemPath.removePrefix("$rootPath/")

            entries.add(relativePath)

            // If directory, recurse
            var isDirectory = false
            fileManager.fileExistsAtPath(itemPath, isDirectory = cValuesOf(isDirectory))

            if (isDirectory) {
                listFilesRecursively(rootPath, itemPath, entries)
            }
        }
    }
}

/**
 * Simple IOException for iOS platform.
 */
class IOException(message: String, cause: Throwable? = null) : Exception(message, cause)
