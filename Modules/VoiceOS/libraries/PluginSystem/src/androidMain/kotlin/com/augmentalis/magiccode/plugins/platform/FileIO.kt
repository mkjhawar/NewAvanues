package com.augmentalis.magiccode.plugins.platform

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Android implementation of FileIO using java.io.File.
 */
actual class FileIO {
    actual fun readFileAsString(path: String): String {
        val file = File(path)
        if (!file.exists()) {
            throw FileNotFoundException("File not found: $path")
        }
        try {
            return file.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            throw IOException("Failed to read file: $path", e)
        }
    }

    actual fun readFileAsBytes(path: String): ByteArray {
        val file = File(path)
        if (!file.exists()) {
            throw FileNotFoundException("File not found: $path")
        }
        try {
            return file.readBytes()
        } catch (e: Exception) {
            throw IOException("Failed to read file: $path", e)
        }
    }

    actual fun fileExists(path: String): Boolean {
        return File(path).isFile
    }

    actual fun directoryExists(path: String): Boolean {
        return File(path).isDirectory
    }

    actual fun getParentDirectory(path: String): String {
        return File(path).parent ?: ""
    }

    actual fun listFiles(path: String): List<String> {
        val dir = File(path)
        if (!dir.isDirectory) {
            return emptyList()
        }
        return dir.list()?.toList() ?: emptyList()
    }

    actual fun createDirectory(path: String): Boolean {
        val dir = File(path)
        return if (dir.exists()) {
            dir.isDirectory
        } else {
            dir.mkdirs()
        }
    }

    actual fun getFileSize(path: String): Long {
        val file = File(path)
        return if (file.exists() && file.isFile) {
            file.length()
        } else {
            -1L
        }
    }

    actual fun delete(path: String): Boolean {
        return try {
            File(path).deleteRecursively()
        } catch (e: Exception) {
            false
        }
    }

    actual fun writeFileAsString(path: String, content: String) {
        try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
        } catch (e: Exception) {
            throw IOException("Failed to write file: $path", e)
        }
    }

    actual fun writeFileAsBytes(path: String, content: ByteArray) {
        try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeBytes(content)
        } catch (e: Exception) {
            throw IOException("Failed to write file: $path", e)
        }
    }

    actual fun copy(sourcePath: String, destPath: String): Boolean {
        return try {
            val source = File(sourcePath)
            val dest = File(destPath)
            if (source.isDirectory) {
                source.copyRecursively(dest, overwrite = true)
            } else {
                dest.parentFile?.mkdirs()
                source.copyTo(dest, overwrite = true)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    actual fun getLastModified(path: String): Long {
        val file = File(path)
        return if (file.exists()) {
            file.lastModified()
        } else {
            -1L
        }
    }

    actual fun listFilesRecursive(path: String): List<String> {
        val dir = File(path)
        if (!dir.isDirectory) {
            return emptyList()
        }
        return dir.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(dir).path }
            .toList()
    }

    actual fun getAvailableDiskSpace(path: String): Long {
        return try {
            val file = File(path)
            val root = if (file.exists()) {
                file
            } else {
                file.parentFile ?: File("/")
            }
            root.usableSpace
        } catch (e: Exception) {
            -1L
        }
    }
}
