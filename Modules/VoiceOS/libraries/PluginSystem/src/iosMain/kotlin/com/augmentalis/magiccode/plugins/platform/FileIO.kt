package com.augmentalis.magiccode.plugins.platform

import platform.Foundation.*
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * iOS implementation of FileIO using Foundation.NSFileManager.
 */
@OptIn(ExperimentalForeignApi::class)
actual class FileIO {
    private val fileManager = NSFileManager.defaultManager

    actual fun readFileAsString(path: String): String {
        val fileExists = fileManager.fileExistsAtPath(path)
        if (!fileExists) {
            throw Exception("File not found: $path")
        }

        val data = NSData.dataWithContentsOfFile(path)
            ?: throw Exception("Failed to read file: $path")

        return NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
            ?: throw Exception("Failed to decode file as UTF-8: $path")
    }

    actual fun readFileAsBytes(path: String): ByteArray {
        val fileExists = fileManager.fileExistsAtPath(path)
        if (!fileExists) {
            throw Exception("File not found: $path")
        }

        val data = NSData.dataWithContentsOfFile(path)
            ?: throw Exception("Failed to read file: $path")

        val bytes = data.bytes
            ?: throw Exception("No data available in file: $path")

        return ByteArray(data.length.toInt()) { index ->
            bytes[index].toByte()
        }
    }

    actual fun fileExists(path: String): Boolean {
        var isDirectory = false
        val exists = fileManager.fileExistsAtPath(path, isDirectory = null)
        return exists && !isDirectory
    }

    actual fun directoryExists(path: String): Boolean {
        var isDirectory: Boolean = false
        val exists = fileManager.fileExistsAtPath(path, isDirectory = null)
        return exists && isDirectory
    }

    actual fun getParentDirectory(path: String): String {
        val nsPath = path as NSString
        return nsPath.stringByDeletingLastPathComponent
    }

    actual fun listFiles(path: String): List<String> {
        val contents = fileManager.contentsOfDirectoryAtPath(path, error = null)
            ?: return emptyList()

        return (0 until contents.count).mapNotNull { index ->
            contents.objectAtIndex(index) as? String
        }
    }

    actual fun createDirectory(path: String): Boolean {
        if (directoryExists(path)) {
            return true
        }

        return fileManager.createDirectoryAtPath(
            path = path,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
    }

    actual fun getFileSize(path: String): Long {
        if (!fileExists(path)) {
            return -1L
        }

        val attributes = fileManager.attributesOfItemAtPath(path, error = null)
            ?: return -1L

        val fileSize = attributes[NSFileSize] as? NSNumber
        return fileSize?.longValue ?: -1L
    }

    actual fun delete(path: String): Boolean {
        return fileManager.removeItemAtPath(path, error = null)
    }

    actual fun writeFileAsString(path: String, content: String) {
        val nsString = content as NSString
        val data = nsString.dataUsingEncoding(NSUTF8StringEncoding)
            ?: throw Exception("Failed to encode string as UTF-8")

        // Create parent directory if needed
        val parentDir = getParentDirectory(path)
        if (parentDir.isNotEmpty()) {
            createDirectory(parentDir)
        }

        val written = data.writeToFile(path, atomically = true)
        if (!written) {
            throw Exception("Failed to write file: $path")
        }
    }

    actual fun writeFileAsBytes(path: String, content: ByteArray) {
        val data = content.toNSData()

        // Create parent directory if needed
        val parentDir = getParentDirectory(path)
        if (parentDir.isNotEmpty()) {
            createDirectory(parentDir)
        }

        val written = data.writeToFile(path, atomically = true)
        if (!written) {
            throw Exception("Failed to write file: $path")
        }
    }

    actual fun copy(sourcePath: String, destPath: String): Boolean {
        // Create parent directory for destination
        val parentDir = getParentDirectory(destPath)
        if (parentDir.isNotEmpty()) {
            createDirectory(parentDir)
        }

        // Remove destination if it exists
        if (fileManager.fileExistsAtPath(destPath)) {
            fileManager.removeItemAtPath(destPath, error = null)
        }

        return fileManager.copyItemAtPath(sourcePath, toPath = destPath, error = null)
    }

    actual fun getLastModified(path: String): Long {
        val attributes = fileManager.attributesOfItemAtPath(path, error = null)
            ?: return -1L

        val modificationDate = attributes[NSFileModificationDate] as? NSDate
        return modificationDate?.timeIntervalSince1970?.toLong()?.times(1000) ?: -1L
    }

    actual fun listFilesRecursive(path: String): List<String> {
        if (!directoryExists(path)) {
            return emptyList()
        }

        val result = mutableListOf<String>()
        val enumerator = fileManager.enumeratorAtPath(path) ?: return emptyList()

        while (true) {
            val item = enumerator.nextObject() as? String ?: break
            val fullPath = "$path/$item"
            if (fileExists(fullPath)) {
                result.add(item)
            }
        }

        return result
    }

    actual fun getAvailableDiskSpace(path: String): Long {
        val attributes = try {
            fileManager.attributesOfFileSystemForPath(path, error = null)
        } catch (e: Exception) {
            null
        } ?: return -1L

        val freeSpace = attributes[NSFileSystemFreeSize] as? NSNumber
        return freeSpace?.longValue ?: -1L
    }

    /**
     * Helper function to convert ByteArray to NSData.
     */
    private fun ByteArray.toNSData(): NSData {
        return NSData.create(
            bytes = this.refTo(0),
            length = this.size.toULong()
        )
    }
}
