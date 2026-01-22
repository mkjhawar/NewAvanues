package com.augmentalis.ava.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.posix.memcpy

/**
 * iOS implementation of FileSystem using NSFileManager.
 */
@OptIn(ExperimentalForeignApi::class)
actual class FileSystem actual constructor() {
    private val fileManager = NSFileManager.defaultManager

    actual fun getFilesDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        )
        return paths.firstOrNull() as? String ?: ""
    }

    actual fun getCacheDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSCachesDirectory,
            NSUserDomainMask,
            true
        )
        return paths.firstOrNull() as? String ?: ""
    }

    actual fun getDocumentsDirectory(): String {
        return getFilesDirectory()
    }

    actual fun exists(path: String): Boolean {
        return fileManager.fileExistsAtPath(path)
    }

    actual fun readBytes(path: String): ByteArray {
        val nsData = NSData.dataWithContentsOfFile(path)
            ?: throw IllegalArgumentException("File not found: $path")

        return ByteArray(nsData.length.toInt()).apply {
            usePinned { pinned ->
                memcpy(pinned.addressOf(0), nsData.bytes, nsData.length)
            }
        }
    }

    actual fun readText(path: String): String {
        return NSString.stringWithContentsOfFile(
            path,
            encoding = NSUTF8StringEncoding,
            error = null
        ) ?: throw IllegalArgumentException("File not found: $path")
    }

    actual fun writeBytes(path: String, data: ByteArray) {
        val parentPath = (path as NSString).stringByDeletingLastPathComponent
        if (!exists(parentPath)) {
            mkdirs(parentPath)
        }

        val nsData = data.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = data.size.toULong())
        }

        nsData?.writeToFile(path, atomically = true)
            ?: throw IllegalStateException("Failed to write file: $path")
    }

    actual fun writeText(path: String, text: String) {
        val parentPath = (path as NSString).stringByDeletingLastPathComponent
        if (!exists(parentPath)) {
            mkdirs(parentPath)
        }

        (text as NSString).writeToFile(
            path,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )
    }

    actual fun delete(path: String): Boolean {
        if (!exists(path)) return false
        return fileManager.removeItemAtPath(path, error = null)
    }

    actual fun mkdirs(path: String): Boolean {
        if (exists(path)) return true
        return fileManager.createDirectoryAtPath(
            path,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
    }

    actual fun listFiles(directory: String): List<String> {
        val contents = fileManager.contentsOfDirectoryAtPath(directory, error = null)
            ?: return emptyList()

        return contents.map { it.toString() }
    }

    actual fun getFileSize(path: String): Long {
        if (!exists(path)) return -1L

        val attributes = fileManager.attributesOfItemAtPath(path, error = null)
            ?: return -1L

        return (attributes[NSFileSize] as? NSNumber)?.longValue ?: -1L
    }

    actual fun getAvailableSpace(): Long {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        )
        val documentsDirectory = paths.firstOrNull() as? String ?: return 0L

        val attributes = fileManager.attributesOfFileSystemForPath(documentsDirectory, error = null)
            ?: return 0L

        return (attributes[NSFileSystemFreeSize] as? NSNumber)?.longValue ?: 0L
    }
}

/**
 * iOS factory for FileSystem.
 */
actual object FileSystemFactory {
    actual fun create(): FileSystem {
        return FileSystem()
    }
}
